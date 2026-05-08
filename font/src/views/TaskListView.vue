<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BoltIcon, CheckBadgeIcon, QueueListIcon, XCircleIcon } from '@heroicons/vue/24/outline'
import {
  fetchBookmarkIds,
  fetchTaskDetail,
  fetchTasks,
  getTaskRuntimeStreamUrl,
  updateTaskArchived,
} from '@/api/api'
import type { Task, TaskDetail, TaskRuntime } from '@/types/entity'
import { detectSite, siteLabel, statusTagType, statusText } from '@/utils/task'

type StatusFilter = 'ALL' | 'PENDING' | 'RUNNING' | 'FINISHED' | 'PARTIAL_FAILED' | 'FAILED'

const router = useRouter()
const tasks = ref<Task[]>([])
const detailMap = ref<Record<number, TaskDetail | undefined>>({})
const loading = ref(false)
const keywordFilter = ref('')
const statusFilter = ref<StatusFilter>('ALL')

let runtimeStream: EventSource | null = null

function connectRuntimeStream() {
  runtimeStream?.close()
  runtimeStream = new EventSource(getTaskRuntimeStreamUrl())
  runtimeStream.addEventListener('task-runtime', event => {
    try {
      const incoming = JSON.parse((event as MessageEvent).data) as TaskRuntime
      if (!incoming?.taskId) return
      const task = tasks.value.find(t => t.taskId === incoming.taskId)
      if (task) {
        task.runtime = { ...(task.runtime ?? {} as TaskRuntime), ...incoming }
        task.taskStatus = incoming.status
        task.taskProgress = incoming.progressPercent
        task.totalPages = incoming.expectedPages ?? task.totalPages
      }
      const detail = detailMap.value[incoming.taskId]
      if (detail) {
        detail.runtime = { ...(detail.runtime ?? {} as TaskRuntime), ...incoming }
        detail.task.runtime = detail.runtime
        detail.task.taskStatus = incoming.status
        detail.task.taskProgress = incoming.progressPercent
      }
    } catch { /* noop */ }
  })
}

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await fetchTasks()
    const details = await Promise.all(tasks.value.map(async t => [t.taskId, await fetchTaskDetail(t.taskId)] as const))
    detailMap.value = Object.fromEntries(details.filter((e): e is [number, TaskDetail] => Boolean(e[1])))
    for (const task of tasks.value) {
      const detail = detailMap.value[task.taskId]
      if (detail?.runtime) task.runtime = detail.runtime
    }
  } catch { ElMessage.error('任务数据加载失败') } finally { loading.value = false }
}

async function handleToggleArchive(task: Task) {
  try {
    await updateTaskArchived(task.taskId, !task.archived)
    task.archived = !task.archived
    ElMessage.success(task.archived ? '已归档' : '已取消归档')
  } catch { ElMessage.error('操作失败') }
}

const filteredTasks = computed(() => {
  const keyword = keywordFilter.value.trim().toLowerCase()
  return tasks.value.filter(task => {
    const status = task.runtime?.status ?? task.taskStatus
    if (statusFilter.value !== 'ALL' && status !== statusFilter.value) return false
    if (!keyword) return true
    return String(task.taskId).includes(keyword)
      || task.keyword.toLowerCase().includes(keyword)
      || task.url.toLowerCase().includes(keyword)
  })
})

const taskStats = computed(() => {
  const total = tasks.value.length
  const running = tasks.value.filter(t => (t.runtime?.status ?? t.taskStatus) === 'RUNNING').length
  const finished = tasks.value.filter(t => (t.runtime?.status ?? t.taskStatus) === 'FINISHED').length
  const failed = tasks.value.filter(t => {
    const s = t.runtime?.status ?? t.taskStatus
    return s === 'FAILED' || s === 'PARTIAL_FAILED'
  }).length
  return { total, running, finished, failed }
})

const tableRows = computed(() => {
  return filteredTasks.value.map(task => ({
    ...task,
    siteLabel: siteLabel(detectSite(task.url)),
    runtimeStatus: task.runtime?.status ?? task.taskStatus,
    runtimeProgress: task.runtime?.progressPercent ?? task.taskProgress ?? 0,
    completedPages: task.runtime?.completedPages ?? 0,
    expectedPages: task.runtime?.expectedPages ?? task.totalPages ?? 0,
    successPages: task.runtime?.successPages ?? 0,
    failedPages: task.runtime?.failedPages ?? 0,
  }))
})

onMounted(() => { loadTasks(); connectRuntimeStream() })
onBeforeUnmount(() => { runtimeStream?.close() })
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">我的任务</h2>
        <p class="text-sm text-slate-500 mt-1">管理所有采集任务，查看执行进度和结果</p>
      </div>
      <el-button type="primary" size="large" @click="router.push('/tasks/create')">+ 新建采集任务</el-button>
    </div>

    <section class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500">
        <div class="flex items-center gap-3"><QueueListIcon class="w-6 h-6 text-blue-500" /><div><div class="text-sm text-slate-500">任务总数</div><div class="text-2xl font-bold text-blue-600">{{ taskStats.total }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500">
        <div class="flex items-center gap-3"><CheckBadgeIcon class="w-6 h-6 text-emerald-500" /><div><div class="text-sm text-slate-500">已完成</div><div class="text-2xl font-bold text-emerald-600">{{ taskStats.finished }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500">
        <div class="flex items-center gap-3"><BoltIcon class="w-6 h-6 text-amber-500" /><div><div class="text-sm text-slate-500">执行中</div><div class="text-2xl font-bold text-amber-600">{{ taskStats.running }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-rose-500">
        <div class="flex items-center gap-3"><XCircleIcon class="w-6 h-6 text-rose-500" /><div><div class="text-sm text-slate-500">失败/部分失败</div><div class="text-2xl font-bold text-rose-600">{{ taskStats.failed }}</div></div></div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between flex-wrap gap-3">
          <span class="font-semibold">任务列表</span>
          <div class="flex items-center gap-3">
            <el-input v-model="keywordFilter" size="small" placeholder="搜索任务ID / 关键词 / URL" clearable style="width: 240px" />
            <el-select v-model="statusFilter" size="small" style="width: 140px">
              <el-option label="全部状态" value="ALL" />
              <el-option label="排队中" value="PENDING" />
              <el-option label="执行中" value="RUNNING" />
              <el-option label="已完成" value="FINISHED" />
              <el-option label="部分失败" value="PARTIAL_FAILED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-button text type="primary" :loading="loading" @click="loadTasks">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableRows" border stripe highlight-current-row @row-click="(row: { taskId: number }) => router.push(`/tasks/${row.taskId}`)" class="cursor-pointer">
        <el-table-column prop="taskId" label="ID" width="80" />
        <el-table-column prop="siteLabel" label="站点" width="110" />
        <el-table-column prop="keyword" label="关键词" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.runtimeStatus)" size="small">{{ statusText(scope.row.runtimeStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="200">
          <template #default="scope">
            <div class="flex items-center gap-2">
              <el-progress :percentage="scope.row.runtimeProgress" :stroke-width="8" :show-text="false" style="flex:1" />
              <span class="text-xs text-slate-500 whitespace-nowrap">{{ scope.row.completedPages }}/{{ scope.row.expectedPages }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="节点" width="120">
          <template #default="scope">{{ scope.row.runtime?.assignedNodeId || scope.row.nodeId || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="归档" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.archived ? 'info' : 'success'" size="small">{{ scope.row.archived ? '已归档' : '活跃' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="scope">
            <el-button size="small" text type="primary" @click.stop="handleToggleArchive(scope.row)">
              {{ scope.row.archived ? '取消归档' : '归档' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="py-12 text-center">
            <QueueListIcon class="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <div class="text-slate-500">还没有采集任务</div>
            <el-button type="primary" class="mt-4" @click="router.push('/tasks/create')">创建第一个任务</el-button>
          </div>
        </template>
      </el-table>
    </el-card>
  </div>
</template>
