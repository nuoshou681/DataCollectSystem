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

// Group by
const groupBy = ref<'flat' | 'node' | 'site' | 'user' | 'batch'>('flat')

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
    const nodeSet = new Set(taskData.map(t => t.runtime?.assignedNodeId ?? t.nodeId).filter(Boolean) as string[])
    nodeColorMap.value = new Map(
      Array.from(nodeSet).map((n, i) => [n, NODE_COLORS[i % NODE_COLORS.length] ?? '#94a3b8']),
    )
    selectedIds.value.clear()
    tableRef.value?.clearSelection()
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(rows: Task[]) {
  selectedIds.value = new Set(rows.map(r => r.taskId))
}

const stats = computed(() => ({
  total: tasks.value.length,
  running: tasks.value.filter(t => (t.runtime?.status ?? t.taskStatus) === 'RUNNING').length,
  failed: tasks.value.filter(t => ['FAILED', 'PARTIAL_FAILED'].includes(t.runtime?.status ?? t.taskStatus ?? '')).length,
  archived: tasks.value.filter(t => t.archived).length,
}))

const selectableTasks = computed(() => tasks.value.filter(t => !t.archived))

const allSelected = computed({
  get: () => selectableTasks.value.length > 0 && selectableTasks.value.every(t => selectedIds.value.has(t.taskId)),
  set: (val: boolean) => {
    if (val) { selectedIds.value = new Set(selectableTasks.value.map(t => t.taskId)) }
    else { selectedIds.value.clear() }
  },
})

const isIndeterminate = computed(() => selectedIds.value.size > 0 && selectedIds.value.size < selectableTasks.value.length)

const selectedFailedIds = computed(() =>
  Array.from(selectedIds.value).filter(id => {
    const t = tasks.value.find(task => task.taskId === id)
    return t && ['FAILED', 'PARTIAL_FAILED'].includes(t.runtime?.status ?? t.taskStatus ?? '')
  }),
)

// ── Grouping ──
interface TaskGroup {
  key: string
  label: string
  color?: string
  total: number
  running: number
  finished: number
  failed: number
  tasks: Task[]
}

function groupTasks(getKey: (t: Task) => string, getLabel: (key: string) => string, getColor?: (key: string) => string): TaskGroup[] {
  const map = new Map<string, { running: number; finished: number; failed: number; tasks: Task[] }>()
  for (const t of tasks.value) {
    const key = getKey(t)
    if (!map.has(key)) map.set(key, { running: 0, finished: 0, failed: 0, tasks: [] })
    const entry = map.get(key)!
    entry.tasks.push(t)
    const st = t.runtime?.status ?? t.taskStatus ?? ''
    if (st === 'RUNNING') entry.running++
    else if (st === 'FINISHED') entry.finished++
    else if (['FAILED', 'PARTIAL_FAILED'].includes(st)) entry.failed++
  }
  return Array.from(map.entries())
    .map(([key, d]) => ({
      key,
      label: getLabel(key),
      color: getColor?.(key),
      total: d.tasks.length,
      ...d,
    }))
    .sort((a, b) => b.total - a.total)
}

const nodeGroups = computed(() =>
  groupTasks(
    t => t.runtime?.assignedNodeId ?? t.nodeId ?? '未分配',
    k => k,
    k => nodeColorMap.value.get(k) ?? '#94a3b8',
  ),
)

const siteGroups = computed(() =>
  groupTasks(
    t => siteLabel(detectSite(t.url)),
    k => k,
  ),
)

const userGroups = computed(() =>
  groupTasks(
    t => String(t.userId ?? 0),
    k => userNameMap.value.get(Number(k)) ?? `用户${k}`,
  ),
)

const batchGroups = computed(() =>
  groupTasks(
    t => t.batchId || '无批次',
    k => k,
  ),
)

const currentGroups = computed<TaskGroup[]>(() => {
  if (groupBy.value === 'node') return nodeGroups.value
  if (groupBy.value === 'site') return siteGroups.value
  if (groupBy.value === 'user') return userGroups.value
  if (groupBy.value === 'batch') return batchGroups.value
  return []
})

// ── Batch detail ──
const batchDetail = ref<TaskBatch | null>(null)
const activeBatchId = ref<string | null>(null)

watch([groupBy, activeBatchId], async () => {
  if (groupBy.value === 'batch' && activeBatchId.value && activeBatchId.value !== '无批次') {
    try { batchDetail.value = await fetchTaskBatchDetail(activeBatchId.value).then(d => d?.batch ?? null) }
    catch { batchDetail.value = null }
  } else {
    batchDetail.value = null
  }
})

// ── Task actions ──
async function handleRetry(taskId: number) {
  try {
    await ElMessageBox.confirm('确定要重试该任务吗？将重新加入队列。', '重试任务', { type: 'warning' })
    const ok = await retryTask(taskId)
    if (ok) { ElMessage.success('任务已重新加入队列'); await loadAll() }
    else { ElMessage.error('重试失败，任务状态不允许') }
  } catch { /* cancelled */ }
}

async function handleBatchRetry() {
  const ids = selectedFailedIds.value
  if (!ids.length) { ElMessage.warning('选中的任务中没有失败状态的任务'); return }
  try {
    await ElMessageBox.confirm(`确定要重试 ${ids.length} 个失败任务吗？`, '批量重试', { type: 'warning' })
    const result = await batchRetryTasks(ids)
    ElMessage.success(`已重新入队 ${result?.count ?? ids.length} 个任务`)
    await loadAll()
  } catch { /* cancelled */ }
}

async function handleArchive(taskId: number, archived: boolean) {
  try {
    await ElMessageBox.confirm(archived ? '确定归档该任务吗？' : '确定取消归档吗？', archived ? '归档任务' : '取消归档', { type: 'warning' })
    await updateTaskArchived(taskId, archived)
    ElMessage.success(archived ? '已归档' : '已取消归档')
    await loadAll()
  } catch { /* cancelled */ }
}

async function handleBatchArchive(archived: boolean) {
  if (!selectedIds.value.size) { ElMessage.warning('请先选择任务'); return }
  try {
    await ElMessageBox.confirm(`确定${archived ? '归档' : '取消归档'} ${selectedIds.value.size} 个任务吗？`, archived ? '批量归档' : '批量取消归档', { type: 'warning' })
    const result = await batchArchiveTasks(Array.from(selectedIds.value), archived)
    ElMessage.success(`成功${archived ? '归档' : '取消归档'} ${result?.count ?? selectedIds.value.size} 个任务`)
    await loadAll()
  } catch { /* cancelled */ }
}

async function openDetail(taskId: number) {
  detailDrawer.value = true; detailLoading.value = true
  try { activeDetail.value = await fetchTaskDetail(taskId) }
  catch { ElMessage.error('加载任务详情失败') }
  finally { detailLoading.value = false }
}

// ── Shared table columns ──
const groupTabItems = [
  { name: 'flat' as const, label: '全部任务', icon: QueueListIcon },
  { name: 'node' as const, label: '按节点', icon: ServerStackIcon },
  { name: 'site' as const, label: '按站点', icon: GlobeAltIcon },
  { name: 'user' as const, label: '按用户', icon: UsersIcon },
  { name: 'batch' as const, label: '按批次', icon: QueueListIcon },
]

onMounted(() => { void loadAll() })
</script>

<template>
  <div class="space-y-6">
    <!-- KPI cards -->
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <QueueListIcon class="w-6 h-6 text-blue-500" />
          <div><div class="text-sm text-slate-500">任务总量</div><div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div></div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-sky-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <BoltIcon class="w-6 h-6 text-sky-500" />
          <div><div class="text-sm text-slate-500">运行中</div><div class="text-2xl font-bold text-sky-600">{{ stats.running }}</div></div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-red-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <ExclamationTriangleIcon class="w-6 h-6 text-red-500" />
          <div><div class="text-sm text-slate-500">失败/部分失败</div><div class="text-2xl font-bold text-red-600">{{ stats.failed }}</div></div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <ArchiveBoxIcon class="w-6 h-6 text-emerald-500" />
          <div><div class="text-sm text-slate-500">已归档</div><div class="text-2xl font-bold text-emerald-600">{{ stats.archived }}</div></div>
        </div>
      </el-card>
    </section>

    <!-- Batch info panel -->
    <div v-if="batchDetail && groupBy === 'batch' && activeBatchId" class="grid grid-cols-2 md:grid-cols-4 gap-3 px-4 py-3 bg-slate-50 border border-slate-200 rounded-lg text-sm">
      <div><div class="text-xs text-slate-400">批次名称</div><div class="font-medium text-slate-800">{{ batchDetail.batchName || batchDetail.batchId }}</div></div>
      <div><div class="text-xs text-slate-400">状态</div><el-tag size="small" :type="batchDetail.status === 'COMPLETED' ? 'success' : batchDetail.status === 'FAILED' ? 'danger' : 'info'">{{ batchDetail.status }}</el-tag></div>
      <div><div class="text-xs text-slate-400">任务数 / 创建者</div><div class="font-medium text-slate-800">{{ batchDetail.taskCount }} 个 · 用户{{ batchDetail.createdBy }}</div></div>
      <div><div class="text-xs text-slate-400">备注</div><div class="font-medium text-slate-800 truncate">{{ batchDetail.notes || '-' }}</div></div>
    </div>

    <!-- Group tabs + actions -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between flex-wrap gap-3">
          <div class="flex items-center gap-4">
            <span
              v-for="tab in groupTabItems" :key="tab.name"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium cursor-pointer transition-all select-none"
              :class="groupBy === tab.name ? 'bg-cyan-600 text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'"
              @click="groupBy = tab.name"
            >
              <component :is="tab.icon" class="w-4 h-4" />
              {{ tab.label }}
            </span>
          </div>
          <div class="flex items-center gap-2">
            <el-button size="small" :disabled="!selectedIds.size" @click="handleBatchArchive(true)">批量归档</el-button>
            <el-button size="small" type="warning" :disabled="!selectedFailedIds.length" @click="handleBatchRetry">
              <ArrowPathIcon class="w-4 h-4 mr-1" />批量重试 ({{ selectedFailedIds.length }})
            </el-button>
            <el-button size="small" text type="primary" @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- ── Flat table ── -->
      <template v-if="groupBy === 'flat'">
        <el-table :data="tasks" border stripe v-loading="loading" @selection-change="handleSelectionChange" ref="tableRef">
          <el-table-column type="selection" width="42" :selectable="(row: Task) => !row.archived" />
          <el-table-column prop="taskId" label="ID" width="72" />
          <el-table-column label="站点" width="100">
            <template #default="scope">{{ siteLabel(detectSite(scope.row.url)) }}</template>
          </el-table-column>
          <el-table-column label="关键词" min-width="110" show-overflow-tooltip>
            <template #default="scope"><span>{{ scope.row.keyword }}</span></template>
          </el-table-column>
          <el-table-column label="节点" width="130">
            <template #default="scope">
              <el-tag v-if="scope.row.runtime?.assignedNodeId ?? scope.row.nodeId" size="small" effect="dark"
                :color="nodeColorMap.get(scope.row.runtime?.assignedNodeId ?? scope.row.nodeId ?? '') ?? '#94a3b8'">
                {{ scope.row.runtime?.assignedNodeId ?? scope.row.nodeId }}
              </el-tag>
              <span v-else class="text-xs text-slate-400">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" width="100">
            <template #default="scope">
              <span v-if="scope.row.userId" class="text-sm">{{ userNameMap.get(scope.row.userId) ?? `用户${scope.row.userId}` }}</span>
              <span v-else class="text-xs text-slate-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag size="small"
                :type="scope.row.archived ? 'info' : ['FAILED','PARTIAL_FAILED'].includes(scope.row.runtime?.status ?? '') ? 'danger' : scope.row.runtime?.status === 'FINISHED' ? 'success' : scope.row.runtime?.status === 'RUNNING' ? 'warning' : ''">
                {{ scope.row.archived ? '已归档' : statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" min-width="120">
            <template #default="scope">
              <div class="flex items-center gap-2">
                <el-progress :percentage="scope.row.runtime?.progressPercent ?? scope.row.taskProgress ?? 0" :stroke-width="6" :show-text="false" />
                <span class="text-xs text-slate-500 w-14 flex-shrink-0">{{ scope.row.runtime?.completedPages ?? 0 }}/{{ scope.row.runtime?.expectedPages ?? scope.row.totalPages ?? 0 }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="150" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <div class="flex items-center gap-1">
                <el-button size="small" text type="primary" @click="openDetail(scope.row.taskId)">详情</el-button>
                <el-button v-if="['FAILED','PARTIAL_FAILED'].includes(scope.row.runtime?.status ?? scope.row.taskStatus ?? '')" size="small" text type="warning" @click="handleRetry(scope.row.taskId)">重试</el-button>
                <el-button size="small" text :type="scope.row.archived ? 'success' : 'info'" @click="handleArchive(scope.row.taskId, !scope.row.archived)">{{ scope.row.archived ? '取消归档' : '归档' }}</el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无任务" :image-size="80" /></template>
        </el-table>
      </template>

      <!-- ── Grouped views ── -->
      <template v-else>
        <div v-if="!currentGroups.length" class="py-12">
          <el-empty description="暂无数据" :image-size="80" />
        </div>
        <el-collapse v-model="activeBatchId" v-else>
          <el-collapse-item v-for="g in currentGroups" :key="g.key" :name="g.key">
            <template #title>
              <div class="flex items-center gap-3 w-full pr-4">
                <!-- color dot for node -->
                <span v-if="g.color" class="w-3 h-3 rounded-full flex-shrink-0" :style="{ background: g.color }" />
                <span class="font-semibold text-sm text-slate-800">{{ g.label }}</span>
                <el-tag size="small" type="info">{{ g.total }} 个任务</el-tag>
                <div class="flex items-center gap-2 text-xs ml-auto">
                  <span v-if="g.running" class="text-sky-600 font-medium">{{ g.running }} 运行</span>
                  <span v-if="g.finished" class="text-emerald-600 font-medium">{{ g.finished }} 完成</span>
                  <span v-if="g.failed" class="text-red-500 font-medium">{{ g.failed }} 失败</span>
                  <span v-if="!g.running && !g.finished && !g.failed" class="text-slate-400">其他</span>
                </div>
              </div>
            </template>
            <el-table :data="g.tasks" border stripe size="small">
              <el-table-column prop="taskId" label="ID" width="72" />
              <el-table-column label="站点" width="100">
                <template #default="scope">{{ siteLabel(detectSite(scope.row.url)) }}</template>
              </el-table-column>
              <el-table-column label="关键词" min-width="110" show-overflow-tooltip>
                <template #default="scope"><span>{{ scope.row.keyword }}</span></template>
              </el-table-column>
              <el-table-column label="节点" width="130">
                <template #default="scope">
                  <el-tag v-if="scope.row.runtime?.assignedNodeId ?? scope.row.nodeId" size="small" effect="dark"
                    :color="nodeColorMap.get(scope.row.runtime?.assignedNodeId ?? scope.row.nodeId ?? '') ?? '#94a3b8'">
                    {{ scope.row.runtime?.assignedNodeId ?? scope.row.nodeId }}
                  </el-tag>
                  <span v-else class="text-xs text-slate-400">未分配</span>
                </template>
              </el-table-column>
              <el-table-column label="用户" width="100">
                <template #default="scope">
                  <span v-if="scope.row.userId" class="text-sm">{{ userNameMap.get(scope.row.userId) ?? `用户${scope.row.userId}` }}</span>
                  <span v-else class="text-xs text-slate-400">-</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag size="small"
                    :type="scope.row.archived ? 'info' : ['FAILED','PARTIAL_FAILED'].includes(scope.row.runtime?.status ?? '') ? 'danger' : scope.row.runtime?.status === 'FINISHED' ? 'success' : scope.row.runtime?.status === 'RUNNING' ? 'warning' : ''">
                    {{ scope.row.archived ? '已归档' : statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="进度" min-width="120">
                <template #default="scope">
                  <div class="flex items-center gap-2">
                    <el-progress :percentage="scope.row.runtime?.progressPercent ?? scope.row.taskProgress ?? 0" :stroke-width="5" :show-text="false" />
                    <span class="text-xs text-slate-500">{{ scope.row.runtime?.completedPages ?? 0 }}/{{ scope.row.runtime?.expectedPages ?? scope.row.totalPages ?? 0 }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="150" />
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="scope">
                  <div class="flex items-center gap-1">
                    <el-button size="small" text type="primary" @click="openDetail(scope.row.taskId)">详情</el-button>
                    <el-button v-if="['FAILED','PARTIAL_FAILED'].includes(scope.row.runtime?.status ?? scope.row.taskStatus ?? '')" size="small" text type="warning" @click="handleRetry(scope.row.taskId)">重试</el-button>
                    <el-button size="small" text :type="scope.row.archived ? 'success' : 'info'" @click="handleArchive(scope.row.taskId, !scope.row.archived)">{{ scope.row.archived ? '取消归档' : '归档' }}</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </template>
    </el-card>

    <!-- Detail drawer -->
    <el-drawer v-model="detailDrawer" title="任务详情" size="520px">
      <div v-loading="detailLoading" class="space-y-4">
        <template v-if="activeDetail">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="任务 ID">{{ activeDetail.task.taskId }}</el-descriptions-item>
            <el-descriptions-item label="关键词">{{ activeDetail.task.keyword || '-' }}</el-descriptions-item>
            <el-descriptions-item label="目标 URL"><a :href="activeDetail.task.url" target="_blank" class="text-blue-600 underline text-sm">{{ activeDetail.task.url }}</a></el-descriptions-item>
            <el-descriptions-item label="站点">{{ siteLabel(detectSite(activeDetail.task.url)) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusText(activeDetail.runtime?.status ?? activeDetail.task.taskStatus) }}</el-descriptions-item>
            <el-descriptions-item label="进度">{{ activeDetail.runtime?.completedPages ?? 0 }} / {{ activeDetail.runtime?.expectedPages ?? activeDetail.task.totalPages ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="节点">{{ activeDetail.runtime?.assignedNodeId || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ activeDetail.task.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="错误信息" v-if="activeDetail.runtime?.lastErrorMessage"><span class="text-red-600 text-sm">{{ activeDetail.runtime.lastErrorMessage }}</span></el-descriptions-item>
          </el-descriptions>
          <div v-if="activeDetail.pageResults?.length">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">页面采集结果 ({{ activeDetail.pageResults.length }})</h4>
            <el-table :data="activeDetail.pageResults" border stripe size="small" max-height="300">
              <el-table-column prop="pageIndex" label="#" width="50" />
              <el-table-column prop="pageUrl" label="URL" min-width="200" show-overflow-tooltip />
              <el-table-column prop="pageTitle" label="标题" min-width="140" show-overflow-tooltip />
              <el-table-column label="状态" width="70"><template #default="scope"><el-tag :type="scope.row.success ? 'success' : 'danger'" size="small">{{ scope.row.success ? '成功' : '失败' }}</el-tag></template></el-table-column>
            </el-table>
          </div>
          <div v-if="activeDetail.events?.length">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">任务事件 ({{ activeDetail.events.length }})</h4>
            <el-table :data="activeDetail.events" border stripe size="small" max-height="240">
              <el-table-column prop="eventType" label="类型" min-width="140" />
              <el-table-column prop="eventLevel" label="级别" width="70"><template #default="scope"><el-tag :type="scope.row.eventLevel === 'ERROR' ? 'danger' : scope.row.eventLevel === 'WARN' ? 'warning' : 'info'" size="small">{{ scope.row.eventLevel }}</el-tag></template></el-table-column>
              <el-table-column prop="eventMessage" label="消息" min-width="200" show-overflow-tooltip />
              <el-table-column prop="createdAt" label="时间" min-width="160" />
            </el-table>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>
