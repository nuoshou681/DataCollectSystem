<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BoltIcon, CheckBadgeIcon, QueueListIcon, ServerStackIcon, XCircleIcon } from '@heroicons/vue/24/outline'
import { fetchCrawlerNodes, fetchNodeTasks } from '@/api/api'
import type { CrawlerNode, Task } from '@/types/entity'
import { deriveNodeStatus, siteLabel, detectSite, statusText } from '@/utils/task'

const route = useRoute()
const loading = ref(false)
const nodes = ref<CrawlerNode[]>([])
let refreshTimer: number | null = null

// Task drawer
const drawerVisible = ref(false)
const drawerNodeId = ref('')
const drawerTasks = ref<Task[]>([])
const drawerLoading = ref(false)

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

async function viewNodeTasks(node: CrawlerNode) {
  drawerNodeId.value = node.nodeId
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    const result = await fetchNodeTasks(node.nodeId)
    drawerTasks.value = result.tasks
  } catch {
    ElMessage.error('节点任务加载失败')
  } finally {
    drawerLoading.value = false
  }
}

const stats = computed(() => {
  const derived = nodes.value.map(n => deriveNodeStatus(n))
  return {
    total: nodes.value.length,
    online: derived.filter(n => ['ONLINE', 'IDLE'].includes(n.displayStatus)).length,
    busy: derived.filter(n => n.displayStatus === 'BUSY').length,
    offline: derived.filter(n => n.displayStatus === 'OFFLINE').length,
  }
})

const capacityStats = computed(() => {
  const maxConcurrency = nodes.value.reduce((s, n) => s + (n.maxConcurrency ?? 0), 0)
  const currentLoad = nodes.value.reduce((s, n) => s + (n.currentLoad ?? 0), 0)
  const usagePercent = maxConcurrency > 0 ? Math.round((currentLoad / maxConcurrency) * 100) : 0
  return { maxConcurrency, currentLoad, usagePercent }
})

const recentNodes = computed(() =>
  [...nodes.value]
    .sort((a, b) => {
      const ta = a.lastHeartbeat ? new Date(a.lastHeartbeat).getTime() : 0
      const tb = b.lastHeartbeat ? new Date(b.lastHeartbeat).getTime() : 0
      return tb - ta
    })
    .slice(0, 6),
)

// Auto-refresh every 5 seconds
onMounted(() => {
  void loadNodes()
  refreshTimer = window.setInterval(() => void loadNodes(), 5000)
})

onBeforeUnmount(() => {
  if (refreshTimer !== null) window.clearInterval(refreshTimer)
})
</script>

<template>
  <div class="space-y-6">
    <!-- KPI Cards -->
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500">
        <div class="flex items-center gap-3">
          <ServerStackIcon class="w-6 h-6 text-blue-500" />
          <div>
            <div class="text-sm text-slate-500">节点总数</div>
            <div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500">
        <div class="flex items-center gap-3">
          <CheckBadgeIcon class="w-6 h-6 text-emerald-500" />
          <div>
            <div class="text-sm text-slate-500">在线</div>
            <div class="text-2xl font-bold text-emerald-600">{{ stats.online }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500">
        <div class="flex items-center gap-3">
          <BoltIcon class="w-6 h-6 text-amber-500" />
          <div>
            <div class="text-sm text-slate-500">繁忙</div>
            <div class="text-2xl font-bold text-amber-600">{{ stats.busy }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-rose-500">
        <div class="flex items-center gap-3">
          <XCircleIcon class="w-6 h-6 text-rose-500" />
          <div>
            <div class="text-sm text-slate-500">离线</div>
            <div class="text-2xl font-bold text-rose-600">{{ stats.offline }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[1fr_0.95fr] gap-6">
      <!-- Capacity overview -->
      <el-card>
        <template #header><span class="font-semibold">集群容量概览</span></template>
        <div class="grid grid-cols-3 gap-4">
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-xs text-slate-500">总并发能力</div>
            <div class="mt-2 text-2xl font-semibold">{{ capacityStats.maxConcurrency }}</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-xs text-slate-500">当前负载</div>
            <div class="mt-2 text-2xl font-semibold">{{ capacityStats.currentLoad }}</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-xs text-slate-500">使用率</div>
            <div class="mt-2 text-2xl font-semibold">{{ capacityStats.usagePercent }}%</div>
          </div>
        </div>
      </el-card>

      <!-- Recent active nodes -->
      <el-card>
        <template #header><span class="font-semibold">最近活跃节点</span></template>
        <div class="space-y-3">
          <div
            v-for="item in recentNodes"
            :key="item.nodeId"
            class="rounded-xl border border-slate-200 p-4 cursor-pointer hover:bg-slate-50 transition-colors"
            @click="viewNodeTasks(item)"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="font-medium">{{ item.nodeId }}</div>
              <el-tag :type="deriveNodeStatus(item).statusType" size="small">
                {{ deriveNodeStatus(item).displayStatus }}
              </el-tag>
            </div>
            <div class="mt-2 text-xs text-slate-500">
              负载 {{ item.currentLoad ?? 0 }}/{{ item.maxConcurrency ?? 0 }}
              · 最后心跳 {{ item.lastHeartbeat || '-' }}
            </div>
          </div>
          <el-empty v-if="!recentNodes.length" description="暂无节点注册" :image-size="60" />
        </div>
      </el-card>
    </section>

    <!-- Full node table -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">全部节点</span>
          <span class="text-xs text-slate-400">自动刷新 5s · 点击行查看任务</span>
        </div>
      </template>
      <el-table :data="nodes" border stripe v-loading="loading" @row-click="viewNodeTasks">
        <el-table-column prop="nodeId" label="节点 ID" min-width="140" />
        <el-table-column prop="nodeName" label="名称" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="deriveNodeStatus(scope.row).statusType" size="small">
              {{ deriveNodeStatus(scope.row).displayStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负载" min-width="140">
          <template #default="scope">
            <div class="flex items-center gap-2">
              <el-progress
                :percentage="(scope.row.maxConcurrency ?? 1) > 0 ? Math.round(((scope.row.currentLoad ?? 0) / (scope.row.maxConcurrency ?? 1)) * 100) : 0"
                :stroke-width="6"
                :show-text="false"
              />
              <span class="text-xs text-slate-500">{{ scope.row.currentLoad ?? 0 }}/{{ scope.row.maxConcurrency ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="170" />
        <el-table-column label="能力" min-width="180">
          <template #default="scope">
            <el-tag v-for="cap in scope.row.capabilities || []" :key="cap" size="small" type="info" class="mr-1">
              {{ cap }}
            </el-tag>
            <span v-if="!(scope.row.capabilities?.length)" class="text-slate-400 text-sm">-</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="140">
          <template #default="scope">
            <el-tag v-for="tag in scope.row.tags || []" :key="tag" size="small" class="mr-1">{{ tag }}</el-tag>
            <span v-if="!(scope.row.tags?.length)" class="text-slate-400 text-sm">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button size="small" text type="primary" @click.stop="viewNodeTasks(scope.row)">
              查看任务
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无节点注册" :image-size="60" /></template>
      </el-table>
    </el-card>

    <!-- Per-node task drawer -->
    <el-drawer v-model="drawerVisible" :title="`${drawerNodeId} 任务列表`" size="640px">
      <div v-loading="drawerLoading" class="space-y-4">
        <div class="text-sm text-slate-500">共 {{ drawerTasks.length }} 个任务</div>
        <el-table :data="drawerTasks" border stripe size="small" max-height="calc(100vh - 200px)">
          <el-table-column prop="taskId" label="ID" width="80" />
          <el-table-column label="站点" width="100">
            <template #default="scope">{{ siteLabel(detectSite(scope.row.url)) }}</template>
          </el-table-column>
          <el-table-column prop="keyword" label="关键词" min-width="120" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="['FAILED', 'PARTIAL_FAILED'].includes(scope.row.taskStatus ?? '') ? 'danger' : scope.row.taskStatus === 'FINISHED' ? 'success' : scope.row.taskStatus === 'RUNNING' ? 'warning' : ''" size="small">
                {{ statusText(scope.row.taskStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" min-width="130">
            <template #default="scope">
              <div class="flex items-center gap-2">
                <el-progress :percentage="scope.row.taskProgress ?? 0" :stroke-width="5" :show-text="false" />
                <span class="text-xs text-slate-500">{{ scope.row.taskProgress ?? 0 }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="160" />
          <template #empty><el-empty description="该节点暂无任务" :image-size="60" /></template>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>
