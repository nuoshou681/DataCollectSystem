<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCrawlerNodes } from '@/api/api'
import type { CrawlerNode } from '@/types/entity'
import { deriveNodeStatus } from '@/utils/task'

const loading = ref(false)
const nodes = ref<CrawlerNode[]>([])
let refreshTimer: number | null = null

async function loadNodes() {
  loading.value = true
  try {
    nodes.value = await fetchCrawlerNodes()
  } catch {
    ElMessage.error('节点状态加载失败')
  } finally {
    loading.value = false
  }
}

const stats = computed(() => {
  const derived = nodes.value.map(node => deriveNodeStatus(node))
  return {
    total: nodes.value.length,
    online: derived.filter(node => ['ONLINE', 'IDLE'].includes(node.displayStatus)).length,
    busy: derived.filter(node => node.displayStatus === 'BUSY').length,
    offline: derived.filter(node => node.displayStatus === 'OFFLINE').length,
  }
})

const capacityStats = computed(() => {
  const maxConcurrency = nodes.value.reduce((sum, node) => sum + (node.maxConcurrency ?? 0), 0)
  const currentLoad = nodes.value.reduce((sum, node) => sum + (node.currentLoad ?? 0), 0)
  const usagePercent = maxConcurrency > 0 ? Math.round((currentLoad / maxConcurrency) * 100) : 0
  return { maxConcurrency, currentLoad, usagePercent }
})

const riskNodes = computed(() => {
  return nodes.value
    .map(node => ({ node, status: deriveNodeStatus(node) }))
    .filter(item => item.status.displayStatus === 'OFFLINE' || item.status.displayStatus === 'BUSY')
    .slice(0, 6)
})

onMounted(() => {
  void loadNodes()
  refreshTimer = window.setInterval(() => void loadNodes(), 5000)
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
      <el-card><div class="text-sm text-gray-500">节点总数</div><div class="text-2xl font-semibold mt-2">{{ stats.total }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">在线</div><div class="text-2xl font-semibold mt-2 text-emerald-600">{{ stats.online }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">繁忙</div><div class="text-2xl font-semibold mt-2 text-amber-600">{{ stats.busy }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">离线</div><div class="text-2xl font-semibold mt-2 text-red-600">{{ stats.offline }}</div></el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[1fr_0.95fr] gap-6">
      <el-card>
        <template #header><span class="font-semibold">节点容量概览</span></template>
        <div class="grid grid-cols-3 gap-4">
          <div class="rounded-xl border border-slate-100 p-4">
            <div class="text-xs text-gray-500">总并发能力</div>
            <div class="mt-2 text-2xl font-semibold">{{ capacityStats.maxConcurrency }}</div>
          </div>
          <div class="rounded-xl border border-slate-100 p-4">
            <div class="text-xs text-gray-500">当前负载</div>
            <div class="mt-2 text-2xl font-semibold">{{ capacityStats.currentLoad }}</div>
          </div>
          <div class="rounded-xl border border-slate-100 p-4">
            <div class="text-xs text-gray-500">负载使用率</div>
            <div class="mt-2 text-2xl font-semibold">{{ capacityStats.usagePercent }}%</div>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">重点关注节点</span></template>
        <div class="space-y-3">
          <div v-for="item in riskNodes" :key="item.node.nodeId" class="rounded-xl border border-slate-200 p-4">
            <div class="flex items-center justify-between gap-3">
              <div class="font-medium">{{ item.node.nodeId }}</div>
              <el-tag :type="item.status.statusType">{{ item.status.displayStatus }}</el-tag>
            </div>
            <div class="mt-2 text-xs text-slate-500">
              负载 {{ item.node.currentLoad ?? 0 }}/{{ item.node.maxConcurrency ?? 0 }} · 最后心跳 {{ item.node.lastHeartbeat || '-' }}
            </div>
          </div>
          <el-empty v-if="!riskNodes.length" description="当前没有高风险节点" />
        </div>
      </el-card>
    </section>

    <el-card>
      <template #header><div class="flex items-center justify-between"><span class="font-semibold">节点能力与心跳</span><span class="text-sm text-gray-500">自动刷新 5 秒</span></div></template>
      <el-table :data="nodes" border stripe v-loading="loading">
        <el-table-column prop="nodeId" label="节点ID" min-width="160" />
        <el-table-column prop="nodeName" label="名称" width="140" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="deriveNodeStatus(scope.row).statusType">{{ deriveNodeStatus(scope.row).displayStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="maxConcurrency" label="最大并发" width="120" />
        <el-table-column prop="currentLoad" label="当前负载" width="120" />
        <el-table-column prop="heartbeatTimeoutSec" label="心跳超时" width="120" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="180" />
        <el-table-column prop="lastOnlineAt" label="最后在线" min-width="180" />
        <el-table-column label="标签" min-width="180">
          <template #default="scope">
            <el-tag v-for="tag in scope.row.tags || []" :key="tag" size="small" class="mr-1">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="能力" min-width="220">
          <template #default="scope">
            <el-tag v-for="cap in scope.row.capabilities || []" :key="cap" size="small" type="info" class="mr-1">{{ cap }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
