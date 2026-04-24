<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCrawlerNodes } from '@/api/api'
import type { CrawlerNode } from '@/types/entity'
import { deriveNodeStatus } from '@/utils/task'

interface NodeRow {
  nodeId: string
  rawStatus: string
  displayStatus: string
  statusType: 'success' | 'warning' | 'danger' | 'info'
  lastHeartbeatText: string
  delaySeconds: number
}

const loading = ref(false)
const nodes = ref<NodeRow[]>([])
let refreshTimer: number | null = null

function parseHeartbeat(value?: string) {
  if (!value) {
    return 0
  }
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? 0 : parsed
}

function formatTime(value: number) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function mapNodeStatus(node: CrawlerNode): NodeRow {
  const derived = deriveNodeStatus(node)
  const heartbeat = parseHeartbeat(node.lastHeartbeat)

  return {
    nodeId: node.nodeId,
    rawStatus: derived.rawStatus,
    displayStatus: derived.displayStatus,
    statusType: derived.statusType,
    lastHeartbeatText: formatTime(heartbeat),
    delaySeconds: derived.delaySeconds,
  }
}

async function loadNodes() {
  loading.value = true
  try {
    const data = await fetchCrawlerNodes()
    nodes.value = data.map(mapNodeStatus)
  } catch {
    ElMessage.error('节点状态加载失败')
  } finally {
    loading.value = false
  }
}

const stats = computed(() => {
  const total = nodes.value.length
  const online = nodes.value.filter(node => node.displayStatus === 'ONLINE').length
  const busy = nodes.value.filter(node => node.displayStatus === 'BUSY').length
  const offline = nodes.value.filter(node => node.displayStatus === 'OFFLINE').length
  return { total, online, busy, offline }
})

const suggestions = computed(() => {
  const tips: string[] = []
  if (stats.value.offline > 0) {
    tips.push(`离线节点 ${stats.value.offline} 个，建议检查网络或重启。`)
  }
  if (stats.value.busy > stats.value.online / 2) {
    tips.push('繁忙节点占比较高，建议临时扩容或限流。')
  }
  if (!tips.length) {
    tips.push('节点运行稳定，可继续提升任务吞吐。')
  }
  return tips
})

onMounted(() => {
  loadNodes()
  refreshTimer = window.setInterval(() => {
    loadNodes()
  }, 5000)
})

onBeforeUnmount(() => {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div class="p-6 space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card>
        <div class="text-sm text-gray-500">节点总数</div>
        <div class="text-2xl font-semibold mt-2">{{ stats.total }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">在线</div>
        <div class="text-2xl font-semibold mt-2 text-emerald-600">{{ stats.online }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">繁忙</div>
        <div class="text-2xl font-semibold mt-2 text-amber-600">{{ stats.busy }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">离线</div>
        <div class="text-2xl font-semibold mt-2 text-red-600">{{ stats.offline }}</div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">爬虫节点监控</span>
          <div class="text-sm text-gray-500">自动刷新间隔: 5 秒</div>
        </div>
      </template>

      <el-table :data="nodes" border stripe v-loading="loading">
        <el-table-column prop="nodeId" label="节点ID" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.statusType">{{ scope.row.displayStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeatText" label="最后心跳" min-width="200" />
        <el-table-column label="心跳延迟(秒)" width="140">
          <template #default="scope">
            <span v-if="scope.row.delaySeconds >= 0">{{ scope.row.delaySeconds }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="rawStatus" label="原始状态" width="140" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <span class="font-semibold">节点运维建议</span>
      </template>
      <ul class="list-disc pl-6 text-sm text-gray-600 space-y-2">
        <li v-for="item in suggestions" :key="item">{{ item }}</li>
      </ul>
    </el-card>
  </div>
</template>
