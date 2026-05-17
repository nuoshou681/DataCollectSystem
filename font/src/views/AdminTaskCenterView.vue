<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArchiveBoxIcon,
  ArrowPathIcon,
  BoltIcon,
  ExclamationTriangleIcon,
  GlobeAltIcon,
  QueueListIcon,
  ServerStackIcon,
  UsersIcon,
} from '@heroicons/vue/24/outline'
import {
  batchArchiveTasks,
  batchRetryTasks,
  fetchTaskBatchDetail,
  fetchTaskDetail,
  fetchTasks,
  fetchUsers,
  retryTask,
  updateTaskArchived,
} from '@/api/api'
import type { Task, TaskBatch, TaskDetail } from '@/types/entity'
import { detectSite, siteLabel, statusText } from '@/utils/task'

const NODE_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#ec4899']

const loading = ref(false)
const tasks = ref<Task[]>([])
const selectedIds = ref<Set<number>>(new Set())
const tableRef = ref<any>(null)

// Detail drawer
const detailDrawer = ref(false)
const detailLoading = ref(false)
const activeDetail = ref<TaskDetail | null>(null)

// Filters
const filterNode = ref<string | null>(null)
const filterUser = ref<number | null>(null)
const filterBatch = ref<string | null>(null)
const filterSite = ref<string | null>(null)
const batchDetail = ref<TaskBatch | null>(null)

watch(filterBatch, async (bid) => {
  if (bid && bid !== '无批次') {
    try {
      batchDetail.value = await fetchTaskBatchDetail(bid).then(d => d?.batch ?? null)
    } catch { batchDetail.value = null }
  } else {
    batchDetail.value = null
  }
})

const userNameMap = ref<Map<number, string>>(new Map())
const nodeColorMap = ref<Map<string, string>>(new Map())

async function loadAll() {
  loading.value = true
  try {
    const [taskData, userData] = await Promise.all([fetchTasks(), fetchUsers()])
    tasks.value = taskData
    userNameMap.value = new Map(
      (Array.isArray(userData) ? userData : []).map((u: any) => [u.userId, u.username ?? u.email ?? `用户${u.userId}`]),
    )
    // Assign consistent colors to nodes
    const nodeSet = new Set(taskData.map(t => t.runtime?.assignedNodeId ?? t.nodeId).filter(Boolean) as string[])
    nodeColorMap.value = new Map(
      Array.from(nodeSet).map((n, i) => [n, NODE_COLORS[i % NODE_COLORS.length] ?? '#94a3b8']),
    )
    selectedIds.value.clear()
    tableRef.value?.clearSelection()
    filterNode.value = null
    filterUser.value = null
    filterBatch.value = null
    filterSite.value = null
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(rows: Task[]) {
  selectedIds.value = new Set(rows.map(r => r.taskId))
}

const filteredTasks = computed(() => {
  let result = tasks.value
  if (filterNode.value) {
    result = result.filter(t => (t.runtime?.assignedNodeId ?? t.nodeId ?? '未分配') === filterNode.value)
  }
  if (filterUser.value) {
    result = result.filter(t => t.userId === filterUser.value)
  }
  if (filterBatch.value) {
    result = result.filter(t => (t.batchId || '无批次') === filterBatch.value)
  }
  if (filterSite.value) {
    result = result.filter(t => siteLabel(detectSite(t.url)) === filterSite.value)
  }
  return result
})

const stats = computed(() => ({
  total: filteredTasks.value.length,
  running: filteredTasks.value.filter(t => (t.runtime?.status ?? t.taskStatus) === 'RUNNING').length,
  failed: filteredTasks.value.filter(t => ['FAILED', 'PARTIAL_FAILED'].includes(t.runtime?.status ?? t.taskStatus ?? '')).length,
  archived: filteredTasks.value.filter(t => t.archived).length,
}))

const selectableTasks = computed(() =>
  filteredTasks.value.filter(t => !t.archived),
)

const allSelected = computed({
  get: () => selectableTasks.value.length > 0 && selectableTasks.value.every(t => selectedIds.value.has(t.taskId)),
  set: (val: boolean) => {
    if (val) {
      selectedIds.value = new Set(selectableTasks.value.map(t => t.taskId))
    } else {
      selectedIds.value.clear()
    }
  },
})

const isIndeterminate = computed(() => {
  const sel = selectedIds.value.size
  return sel > 0 && sel < selectableTasks.value.length
})

const selectedFailedIds = computed(() =>
  Array.from(selectedIds.value).filter(id => {
    const t = tasks.value.find(task => task.taskId === id)
    return t && ['FAILED', 'PARTIAL_FAILED'].includes(t.runtime?.status ?? t.taskStatus ?? '')
  }),
)

// Node distribution (for sidebar)
interface NodeDistItem {
  nodeId: string
  color: string
  total: number
  running: number
  failed: number
  finished: number
}

const nodeDistribution = computed<NodeDistItem[]>(() => {
  const map = new Map<string, { total: number; running: number; failed: number; finished: number }>()
  for (const t of tasks.value) {
    const nid = t.runtime?.assignedNodeId ?? t.nodeId ?? '未分配'
    if (!map.has(nid)) map.set(nid, { total: 0, running: 0, failed: 0, finished: 0 })
    const entry = map.get(nid)!
    entry.total++
    const st = t.runtime?.status ?? t.taskStatus ?? ''
    if (st === 'RUNNING') entry.running++
    else if (st === 'FINISHED') entry.finished++
    else if (['FAILED', 'PARTIAL_FAILED'].includes(st)) entry.failed++
  }
  return Array.from(map.entries())
    .map(([nodeId, d]) => ({
      nodeId,
      color: nodeColorMap.value.get(nodeId) ?? '#94a3b8',
      ...d,
    }))
    .sort((a, b) => b.total - a.total)
})

// User distribution (for sidebar)
interface UserDistItem {
  userId: number
  username: string
  total: number
  running: number
  failed: number
  finished: number
}

const userDistribution = computed<UserDistItem[]>(() => {
  const map = new Map<number, { total: number; running: number; failed: number; finished: number }>()
  for (const t of tasks.value) {
    const uid = t.userId ?? 0
    if (!map.has(uid)) map.set(uid, { total: 0, running: 0, failed: 0, finished: 0 })
    const entry = map.get(uid)!
    entry.total++
    const st = t.runtime?.status ?? t.taskStatus ?? ''
    if (st === 'RUNNING') entry.running++
    else if (st === 'FINISHED') entry.finished++
    else if (['FAILED', 'PARTIAL_FAILED'].includes(st)) entry.failed++
  }
  return Array.from(map.entries())
    .map(([userId, d]) => ({
      userId,
      username: userNameMap.value.get(userId) ?? `用户${userId}`,
      ...d,
    }))
    .sort((a, b) => b.total - a.total)
})

// Batch distribution (for sidebar)
interface BatchDistItem {
  batchId: string
  total: number
  running: number
  failed: number
  finished: number
}

const batchDistribution = computed<BatchDistItem[]>(() => {
  const map = new Map<string, { total: number; running: number; failed: number; finished: number }>()
  for (const t of tasks.value) {
    const bid = t.batchId || '无批次'
    if (!map.has(bid)) map.set(bid, { total: 0, running: 0, failed: 0, finished: 0 })
    const entry = map.get(bid)!
    entry.total++
    const st = t.runtime?.status ?? t.taskStatus ?? ''
    if (st === 'RUNNING') entry.running++
    else if (st === 'FINISHED') entry.finished++
    else if (['FAILED', 'PARTIAL_FAILED'].includes(st)) entry.failed++
  }
  return Array.from(map.entries())
    .map(([batchId, d]) => ({ batchId, ...d }))
    .sort((a, b) => b.total - a.total)
})

function handleNodeFilterClick(nodeId: string) {
  filterNode.value = filterNode.value === nodeId ? null : nodeId
}

function handleUserFilterClick(userId: number) {
  filterUser.value = filterUser.value === userId ? null : userId
}

function handleBatchFilterClick(batchId: string) {
  filterBatch.value = filterBatch.value === batchId ? null : batchId
}

function clearFilters() {
  filterNode.value = null
  filterUser.value = null
  filterBatch.value = null
  filterSite.value = null
}

const activeFilterLabel = computed(() => {
  const parts: string[] = []
  if (filterNode.value) parts.push(`节点: ${filterNode.value}`)
  if (filterUser.value) parts.push(`用户: ${userNameMap.value.get(filterUser.value) ?? filterUser.value}`)
  if (filterBatch.value) parts.push(`批次: ${filterBatch.value}`)
  if (filterSite.value) parts.push(`站点: ${filterSite.value}`)
  return parts.join(' · ')
})

// Task actions
async function handleRetry(taskId: number) {
  try {
    await ElMessageBox.confirm('确定要重试该任务吗？将重新加入队列。', '重试任务', { type: 'warning' })
    const ok = await retryTask(taskId)
    if (ok) {
      ElMessage.success('任务已重新加入队列')
      await loadAll()
    } else {
      ElMessage.error('重试失败，任务状态不允许')
    }
  } catch { /* cancelled */ }
}

async function handleBatchRetry() {
  const ids = selectedFailedIds.value
  if (!ids.length) {
    ElMessage.warning('选中的任务中没有失败状态的任务')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要重试 ${ids.length} 个失败任务吗？`, '批量重试', { type: 'warning' })
    const result = await batchRetryTasks(ids)
    ElMessage.success(`已重新入队 ${result?.count ?? ids.length} 个任务`)
    await loadAll()
  } catch { /* cancelled */ }
}

async function handleArchive(taskId: number, archived: boolean) {
  try {
    await ElMessageBox.confirm(
      archived ? '确定归档该任务吗？' : '确定取消归档吗？',
      archived ? '归档任务' : '取消归档',
      { type: 'warning' },
    )
    await updateTaskArchived(taskId, archived)
    ElMessage.success(archived ? '已归档' : '已取消归档')
    await loadAll()
  } catch { /* cancelled */ }
}

async function handleBatchArchive(archived: boolean) {
  if (!selectedIds.value.size) {
    ElMessage.warning('请先选择任务')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定${archived ? '归档' : '取消归档'} ${selectedIds.value.size} 个任务吗？`,
      archived ? '批量归档' : '批量取消归档',
      { type: 'warning' },
    )
    const result = await batchArchiveTasks(Array.from(selectedIds.value), archived)
    ElMessage.success(`成功${archived ? '归档' : '取消归档'} ${result?.count ?? selectedIds.value.size} 个任务`)
    await loadAll()
  } catch { /* cancelled */ }
}

// Detail
async function openDetail(taskId: number) {
  detailDrawer.value = true
  detailLoading.value = true
  try {
    activeDetail.value = await fetchTaskDetail(taskId)
  } catch {
    ElMessage.error('加载任务详情失败')
  } finally {
    detailLoading.value = false
  }
}

const siteDist = computed(() => {
  const map = new Map<string, number>()
  for (const t of tasks.value) {
    const site = detectSite(t.url)
    const label = siteLabel(site)
    map.set(label, (map.get(label) ?? 0) + 1)
  }
  return Array.from(map.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
})

onMounted(() => { void loadAll() })
</script>

<template>
  <div class="space-y-6">
    <!-- KPI cards -->
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <QueueListIcon class="w-6 h-6 text-blue-500" />
          <div>
            <div class="text-sm text-slate-500">任务总量</div>
            <div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-sky-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <BoltIcon class="w-6 h-6 text-sky-500" />
          <div>
            <div class="text-sm text-slate-500">运行中</div>
            <div class="text-2xl font-bold text-sky-600">{{ stats.running }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-red-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <ExclamationTriangleIcon class="w-6 h-6 text-red-500" />
          <div>
            <div class="text-sm text-slate-500">失败/部分失败</div>
            <div class="text-2xl font-bold text-red-600">{{ stats.failed }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <ArchiveBoxIcon class="w-6 h-6 text-emerald-500" />
          <div>
            <div class="text-sm text-slate-500">已归档</div>
            <div class="text-2xl font-bold text-emerald-600">{{ stats.archived }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <!-- Active filter bar -->
    <div v-if="filterNode || filterUser || filterBatch || filterSite" class="flex items-center gap-3 px-4 py-2 bg-amber-50 border border-amber-200 rounded-lg text-sm">
      <span class="text-amber-700 font-medium">筛选: {{ activeFilterLabel }}</span>
      <el-button size="small" text type="warning" @click="clearFilters">清除筛选</el-button>
    </div>

    <!-- Batch info panel -->
    <div v-if="batchDetail && filterBatch" class="grid grid-cols-2 md:grid-cols-4 gap-3 px-4 py-3 bg-slate-50 border border-slate-200 rounded-lg text-sm">
      <div>
        <div class="text-xs text-slate-400">批次名称</div>
        <div class="font-medium text-slate-800">{{ batchDetail.batchName || batchDetail.batchId }}</div>
      </div>
      <div>
        <div class="text-xs text-slate-400">状态</div>
        <el-tag size="small" :type="batchDetail.status === 'COMPLETED' ? 'success' : batchDetail.status === 'FAILED' ? 'danger' : 'info'">
          {{ batchDetail.status }}
        </el-tag>
      </div>
      <div>
        <div class="text-xs text-slate-400">任务数 / 创建者</div>
        <div class="font-medium text-slate-800">{{ batchDetail.taskCount }} 个 · 用户{{ batchDetail.createdBy }}</div>
      </div>
      <div>
        <div class="text-xs text-slate-400">备注</div>
        <div class="font-medium text-slate-800 truncate">{{ batchDetail.notes || '-' }}</div>
      </div>
    </div>

    <section class="grid grid-cols-1 xl:grid-cols-[1fr_320px] gap-6">
      <!-- Task table with operations -->
      <el-card>
        <template #header>
          <div class="flex items-center justify-between flex-wrap gap-3">
            <span class="font-semibold">任务列表</span>
            <div class="flex items-center gap-2 flex-wrap">
              <el-button
                size="small"
                :disabled="!selectedIds.size"
                @click="handleBatchArchive(true)"
              >
                批量归档
              </el-button>
              <el-button
                size="small"
                type="warning"
                :disabled="!selectedFailedIds.length"
                @click="handleBatchRetry"
              >
                <ArrowPathIcon class="w-4 h-4 mr-1" />
                批量重试 ({{ selectedFailedIds.length }})
              </el-button>
              <el-button size="small" text type="primary" @click="loadAll">刷新</el-button>
            </div>
          </div>
        </template>

        <el-table
          :data="filteredTasks" border stripe v-loading="loading"
          @selection-change="handleSelectionChange"
          ref="tableRef"
        >
          <el-table-column type="selection" width="42" :selectable="(row: Task) => !row.archived" />
          <el-table-column prop="taskId" label="ID" width="72" />
          <el-table-column label="站点" width="100">
            <template #default="scope">
              <span
                class="cursor-pointer hover:text-blue-600 transition-colors"
                :class="{ 'text-blue-600 underline font-medium': filterSite === siteLabel(detectSite(scope.row.url)) }"
                @click="filterSite = filterSite === siteLabel(detectSite(scope.row.url)) ? null : siteLabel(detectSite(scope.row.url))"
              >{{ siteLabel(detectSite(scope.row.url)) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="关键词" min-width="110" show-overflow-tooltip>
            <template #default="scope">
              <span>{{ scope.row.keyword }}</span>
            </template>
          </el-table-column>
          <el-table-column label="节点" width="120">
            <template #default="scope">
              <el-tag
                v-if="scope.row.runtime?.assignedNodeId ?? scope.row.nodeId"
                size="small"
                :color="nodeColorMap.get(scope.row.runtime?.assignedNodeId ?? scope.row.nodeId ?? '') ?? '#94a3b8'"
                effect="dark"
                class="cursor-pointer"
                @click="handleNodeFilterClick(scope.row.runtime?.assignedNodeId ?? scope.row.nodeId ?? '')"
              >
                {{ scope.row.runtime?.assignedNodeId ?? scope.row.nodeId }}
              </el-tag>
              <span v-else class="text-xs text-slate-400">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" width="100">
            <template #default="scope">
              <span
                v-if="scope.row.userId"
                class="text-sm font-medium cursor-pointer hover:text-blue-600 transition-colors"
                :class="{ 'text-blue-600 underline': filterUser === scope.row.userId }"
                @click="handleUserFilterClick(scope.row.userId)"
              >
                {{ userNameMap.get(scope.row.userId) ?? `用户${scope.row.userId}` }}
              </span>
              <span v-else class="text-xs text-slate-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag
                :type="scope.row.archived ? 'info' :
                  ['FAILED', 'PARTIAL_FAILED'].includes(scope.row.runtime?.status ?? '') ? 'danger' :
                  scope.row.runtime?.status === 'FINISHED' ? 'success' :
                  scope.row.runtime?.status === 'RUNNING' ? 'warning' : ''"
                size="small"
              >
                {{ scope.row.archived ? '已归档' : statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" min-width="120">
            <template #default="scope">
              <div class="flex items-center gap-2">
                <el-progress
                  :percentage="scope.row.runtime?.progressPercent ?? scope.row.taskProgress ?? 0"
                  :stroke-width="6"
                  :show-text="false"
                />
                <span class="text-xs text-slate-500 w-14 flex-shrink-0">
                  {{ scope.row.runtime?.completedPages ?? 0 }}/{{ scope.row.runtime?.expectedPages ?? scope.row.totalPages ?? 0 }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="150" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <div class="flex items-center gap-1">
                <el-button size="small" text type="primary" @click="openDetail(scope.row.taskId)">
                  详情
                </el-button>
                <el-button
                  v-if="['FAILED', 'PARTIAL_FAILED'].includes(scope.row.runtime?.status ?? scope.row.taskStatus ?? '')"
                  size="small" text type="warning"
                  @click="handleRetry(scope.row.taskId)"
                >
                  重试
                </el-button>
                <el-button
                  size="small" text :type="scope.row.archived ? 'success' : 'info'"
                  @click="handleArchive(scope.row.taskId, !scope.row.archived)"
                >
                  {{ scope.row.archived ? '取消归档' : '归档' }}
                </el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无任务" :image-size="80" /></template>
        </el-table>
      </el-card>

      <!-- Right sidebar: distribution views -->
      <div class="space-y-6">
        <!-- Node distribution -->
        <el-card>
          <template #header>
            <div class="flex items-center gap-2">
              <ServerStackIcon class="w-4 h-4 text-slate-500" />
              <span class="font-semibold">节点分布</span>
            </div>
          </template>
          <div class="space-y-3">
            <div v-for="item in nodeDistribution" :key="item.nodeId">
              <button
                type="button"
                class="w-full text-left rounded-lg p-2 transition-colors hover:bg-slate-50 border"
                :class="filterNode === item.nodeId ? 'border-blue-300 bg-blue-50' : 'border-transparent'"
                @click="handleNodeFilterClick(item.nodeId)"
              >
                <div class="flex items-center justify-between mb-1.5">
                  <div class="flex items-center gap-2">
                    <span class="w-3 h-3 rounded-full flex-shrink-0" :style="{ background: item.color }" />
                    <span class="text-sm font-medium truncate max-w-[140px]">{{ item.nodeId }}</span>
                  </div>
                  <span class="text-sm font-bold text-slate-700 flex-shrink-0">{{ item.total }}</span>
                </div>
                <div class="flex items-center gap-1 text-xs">
                  <span v-if="item.running" class="text-sky-600 font-medium mr-1">{{ item.running }} 运行</span>
                  <span v-if="item.finished" class="text-emerald-600 font-medium mr-1">{{ item.finished }} 完成</span>
                  <span v-if="item.failed" class="text-red-500 font-medium">{{ item.failed }} 失败</span>
                  <span v-if="!item.running && !item.finished && !item.failed" class="text-slate-400">空闲</span>
                </div>
              </button>
            </div>
            <el-empty v-if="!nodeDistribution.length" description="暂无节点数据" :image-size="60" />
          </div>
        </el-card>

        <!-- Site distribution -->
        <el-card>
          <template #header>
            <div class="flex items-center gap-2">
              <GlobeAltIcon class="w-4 h-4 text-slate-500" />
              <span class="font-semibold">站点分布</span>
            </div>
          </template>
          <div v-if="siteDist.length" class="space-y-3">
            <div v-for="item in siteDist" :key="item.label">
              <button
                type="button"
                class="w-full text-left rounded-lg p-2 transition-colors hover:bg-slate-50 border"
                :class="filterSite === item.label ? 'border-blue-300 bg-blue-50' : 'border-transparent'"
                @click="filterSite = filterSite === item.label ? null : item.label"
              >
                <div class="flex items-center justify-between">
                  <span class="text-sm font-medium truncate max-w-[140px]">{{ item.label }}</span>
                  <span class="text-sm font-bold text-slate-700">{{ item.value }}</span>
                </div>
              </button>
            </div>
          </div>
          <el-empty v-else description="暂无站点数据" :image-size="60" />
        </el-card>

        <!-- User distribution -->
        <el-card>
          <template #header>
            <div class="flex items-center gap-2">
              <UsersIcon class="w-4 h-4 text-slate-500" />
              <span class="font-semibold">用户分布</span>
            </div>
          </template>
          <div class="space-y-3">
            <div v-for="item in userDistribution" :key="item.userId">
              <button
                type="button"
                class="w-full text-left rounded-lg p-2 transition-colors hover:bg-slate-50 border"
                :class="filterUser === item.userId ? 'border-blue-300 bg-blue-50' : 'border-transparent'"
                @click="handleUserFilterClick(item.userId)"
              >
                <div class="flex items-center justify-between mb-1.5">
                  <span class="text-sm font-medium truncate max-w-[160px]">{{ item.username }}</span>
                  <span class="text-sm font-bold text-slate-700 flex-shrink-0">{{ item.total }}</span>
                </div>
                <div class="flex items-center gap-1 text-xs">
                  <span v-if="item.running" class="text-sky-600 font-medium mr-1">{{ item.running }} 运行</span>
                  <span v-if="item.finished" class="text-emerald-600 font-medium mr-1">{{ item.finished }} 完成</span>
                  <span v-if="item.failed" class="text-red-500 font-medium">{{ item.failed }} 失败</span>
                  <span v-if="!item.running && !item.finished && !item.failed" class="text-slate-400">其他</span>
                </div>
              </button>
            </div>
            <el-empty v-if="!userDistribution.length" description="暂无用户数据" :image-size="60" />
          </div>
        </el-card>

        <!-- Batch distribution -->
        <el-card>
          <template #header>
            <div class="flex items-center gap-2">
              <QueueListIcon class="w-4 h-4 text-slate-500" />
              <span class="font-semibold">批次分布</span>
            </div>
          </template>
          <div class="space-y-3">
            <div v-for="item in batchDistribution" :key="item.batchId">
              <button
                type="button"
                class="w-full text-left rounded-lg p-2 transition-colors hover:bg-slate-50 border"
                :class="filterBatch === item.batchId ? 'border-blue-300 bg-blue-50' : 'border-transparent'"
                @click="handleBatchFilterClick(item.batchId)"
              >
                <div class="flex items-center justify-between mb-1.5">
                  <span class="text-sm font-medium truncate max-w-[160px]">{{ item.batchId }}</span>
                  <span class="text-sm font-bold text-slate-700 flex-shrink-0">{{ item.total }}</span>
                </div>
                <div class="flex items-center gap-1 text-xs">
                  <span v-if="item.running" class="text-sky-600 font-medium mr-1">{{ item.running }} 运行</span>
                  <span v-if="item.finished" class="text-emerald-600 font-medium mr-1">{{ item.finished }} 完成</span>
                  <span v-if="item.failed" class="text-red-500 font-medium">{{ item.failed }} 失败</span>
                  <span v-if="!item.running && !item.finished && !item.failed" class="text-slate-400">其他</span>
                </div>
              </button>
            </div>
            <el-empty v-if="!batchDistribution.length" description="暂无批次数据" :image-size="60" />
          </div>
        </el-card>
      </div>
    </section>

    <!-- Detail drawer -->
    <el-drawer v-model="detailDrawer" title="任务详情" size="520px">
      <div v-loading="detailLoading" class="space-y-4">
        <template v-if="activeDetail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="任务 ID">{{ activeDetail.task.taskId }}</el-descriptions-item>
            <el-descriptions-item label="关键词">{{ activeDetail.task.keyword || '-' }}</el-descriptions-item>
            <el-descriptions-item label="目标 URL">
              <a :href="activeDetail.task.url" target="_blank" class="text-blue-600 underline text-sm">{{ activeDetail.task.url }}</a>
            </el-descriptions-item>
            <el-descriptions-item label="站点">{{ siteLabel(detectSite(activeDetail.task.url)) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusText(activeDetail.runtime?.status ?? activeDetail.task.taskStatus) }}</el-descriptions-item>
            <el-descriptions-item label="进度">
              {{ activeDetail.runtime?.completedPages ?? 0 }} / {{ activeDetail.runtime?.expectedPages ?? activeDetail.task.totalPages ?? 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="节点">{{ activeDetail.runtime?.assignedNodeId || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ activeDetail.task.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="错误信息" v-if="activeDetail.runtime?.lastErrorMessage">
              <span class="text-red-600 text-sm">{{ activeDetail.runtime.lastErrorMessage }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- Page results -->
          <div v-if="activeDetail.pageResults?.length">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">页面采集结果 ({{ activeDetail.pageResults.length }})</h4>
            <el-table :data="activeDetail.pageResults" border stripe size="small" max-height="300">
              <el-table-column prop="pageIndex" label="#" width="50" />
              <el-table-column prop="pageUrl" label="URL" min-width="200" show-overflow-tooltip />
              <el-table-column prop="pageTitle" label="标题" min-width="140" show-overflow-tooltip />
              <el-table-column label="状态" width="70">
                <template #default="scope">
                  <el-tag :type="scope.row.success ? 'success' : 'danger'" size="small">
                    {{ scope.row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- Events -->
          <div v-if="activeDetail.events?.length">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">任务事件 ({{ activeDetail.events.length }})</h4>
            <el-table :data="activeDetail.events" border stripe size="small" max-height="240">
              <el-table-column prop="eventType" label="类型" min-width="140" />
              <el-table-column prop="eventLevel" label="级别" width="70">
                <template #default="scope">
                  <el-tag :type="scope.row.eventLevel === 'ERROR' ? 'danger' : scope.row.eventLevel === 'WARN' ? 'warning' : 'info'" size="small">
                    {{ scope.row.eventLevel }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="eventMessage" label="消息" min-width="200" show-overflow-tooltip />
              <el-table-column prop="createdAt" label="时间" min-width="160" />
            </el-table>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>
