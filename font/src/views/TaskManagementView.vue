<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchExportRecords,
  fetchTaskGroups,
  fetchTaskGroupBindings,
  fetchTaskNotes,
  fetchTasks,
} from '@/api/api'
import type { ExportRecord, Task, TaskGroup, TaskGroupBinding, TaskNote } from '@/types/entity'
import { detectSite, siteLabel, statusTagType, statusText } from '@/utils/task'

const loading = ref(false)
const tasks = ref<Task[]>([])
const taskGroups = ref<TaskGroup[]>([])
const exportRecords = ref<ExportRecord[]>([])
const taskNotesMap = ref<Record<number, TaskNote[]>>({})
const taskGroupBindingMap = ref<Record<number, TaskGroupBinding[]>>({})
const statusFilter = ref('ALL')
const siteFilter = ref('ALL')
const archiveFilter = ref('ALL')
const selectedGroupId = ref<number | 'ALL'>('ALL')
const keywordFilter = ref('')
const groupDetailVisible = ref(false)
const activeGroup = ref<TaskGroup | null>(null)

async function loadData() {
  loading.value = true
  try {
    const taskData = await fetchTasks()
    tasks.value = taskData
    const taskIds = taskData.map(item => item.taskId)
    const [groups, exports, noteEntries, bindingEntries] = await Promise.all([
      fetchTaskGroups(),
      fetchExportRecords(),
      Promise.all(taskIds.map(async taskId => [taskId, await fetchTaskNotes(taskId)] as const)),
      Promise.all(taskIds.map(async taskId => [taskId, await fetchTaskGroupBindings(taskId)] as const)),
    ])
    taskGroups.value = groups
    exportRecords.value = exports
    taskNotesMap.value = Object.fromEntries(noteEntries)
    taskGroupBindingMap.value = Object.fromEntries(bindingEntries)
  } catch {
    ElMessage.error('任务管理中心数据加载失败')
  } finally {
    loading.value = false
  }
}

const groupMap = computed(() => new Map(taskGroups.value.map(group => [group.groupId, group])))

function resolveTaskGroups(taskId: number) {
  const bindings = taskGroupBindingMap.value[taskId] ?? []
  return bindings
    .map(binding => groupMap.value.get(binding.groupId))
    .filter((group): group is TaskGroup => Boolean(group))
}

const filteredTasks = computed(() => {
  const keyword = keywordFilter.value.trim().toLowerCase()
  return tasks.value.filter(task => {
    const runtimeStatus = task.runtime?.status ?? task.taskStatus
    const currentSite = siteLabel(detectSite(task.url))
    const groups = resolveTaskGroups(task.taskId)
    const matchesStatus = statusFilter.value === 'ALL' || runtimeStatus === statusFilter.value
    const matchesSite = siteFilter.value === 'ALL' || currentSite === siteFilter.value
    const matchesArchive = archiveFilter.value === 'ALL'
      || (archiveFilter.value === 'ARCHIVED' && task.archived)
      || (archiveFilter.value === 'ACTIVE' && !task.archived)
    const matchesGroup = selectedGroupId.value === 'ALL' || groups.some(group => group.groupId === selectedGroupId.value)
    const matchesKeyword = !keyword
      || String(task.taskId).includes(keyword)
      || task.keyword.toLowerCase().includes(keyword)
      || task.url.toLowerCase().includes(keyword)
    return matchesStatus && matchesSite && matchesArchive && matchesGroup && matchesKeyword
  })
})

const stats = computed(() => ({
  total: tasks.value.length,
  archived: tasks.value.filter(item => item.archived).length,
  grouped: tasks.value.filter(item => resolveTaskGroups(item.taskId).length > 0).length,
  noted: tasks.value.filter(item => (taskNotesMap.value[item.taskId] ?? []).length > 0).length,
}))

function formatDayLabel(offset: number) {
  const date = new Date()
  date.setDate(date.getDate() - offset)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function sameDay(dateText?: string | null, offset = 0) {
  if (!dateText) {
    return false
  }
  const target = new Date(dateText)
  const current = new Date()
  current.setDate(current.getDate() - offset)
  return target.getFullYear() === current.getFullYear()
    && target.getMonth() === current.getMonth()
    && target.getDate() === current.getDate()
}

const siteStats = computed(() => {
  const map = new Map<string, number>()
  for (const task of tasks.value) {
    const label = siteLabel(detectSite(task.url))
    map.set(label, (map.get(label) ?? 0) + 1)
  }
  return Array.from(map.entries()).map(([label, value]) => ({ label, value }))
})

const statusStats = computed(() => {
  const map = new Map<string, number>()
  for (const task of tasks.value) {
    const status = task.runtime?.status ?? task.taskStatus
    map.set(statusText(status), (map.get(statusText(status)) ?? 0) + 1)
  }
  return Array.from(map.entries()).map(([label, value]) => ({ label, value }))
})

const trendStats = computed(() => {
  return Array.from({ length: 7 }, (_, index) => {
    const offset = 6 - index
    const created = tasks.value.filter(task => sameDay(task.createdAt, offset)).length
    const archived = tasks.value.filter(task => sameDay(task.archivedAt, offset)).length
    const exported = exportRecords.value.filter(record => sameDay(record.createdAt, offset)).length
    return {
      label: formatDayLabel(offset),
      created,
      archived,
      exported,
      peak: Math.max(created, archived, exported, 1),
    }
  })
})

const groupRanking = computed(() => {
  return taskGroups.value
    .map(group => ({
      ...group,
      taskCount: tasks.value.filter(task => resolveTaskGroups(task.taskId).some(item => item.groupId === group.groupId)).length,
    }))
    .sort((a, b) => b.taskCount - a.taskCount)
})

const activeGroupTasks = computed(() => {
  if (!activeGroup.value?.groupId) {
    return []
  }
  return tasks.value.filter(task => resolveTaskGroups(task.taskId).some(group => group.groupId === activeGroup.value?.groupId))
})

const activeGroupNoteCount = computed(() => {
  return activeGroupTasks.value.reduce((sum, task) => sum + (taskNotesMap.value[task.taskId]?.length ?? 0), 0)
})

const activeGroupExportCount = computed(() => {
  const taskIds = new Set(activeGroupTasks.value.map(task => task.taskId))
  return exportRecords.value.filter(record => record.taskId && taskIds.has(record.taskId)).length
})

function openGroupDetail(group: TaskGroup) {
  activeGroup.value = group
  groupDetailVisible.value = true
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card><div class="text-sm text-slate-500">任务总量</div><div class="mt-2 text-2xl font-semibold">{{ stats.total }}</div></el-card>
      <el-card><div class="text-sm text-slate-500">已归档任务</div><div class="mt-2 text-2xl font-semibold text-sky-600">{{ stats.archived }}</div></el-card>
      <el-card><div class="text-sm text-slate-500">已分组任务</div><div class="mt-2 text-2xl font-semibold text-emerald-600">{{ stats.grouped }}</div></el-card>
      <el-card><div class="text-sm text-slate-500">已有备注任务</div><div class="mt-2 text-2xl font-semibold text-amber-600">{{ stats.noted }}</div></el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[1.1fr_1fr] gap-6">
      <el-card>
        <template #header><span class="font-semibold">任务状态分布</span></template>
        <div class="space-y-4">
          <div v-for="item in statusStats" :key="item.label" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span>{{ item.label }}</span>
              <span class="text-slate-500">{{ item.value }}</span>
            </div>
            <el-progress :percentage="stats.total ? Math.round((item.value / stats.total) * 100) : 0" :stroke-width="12" />
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">站点采集分布</span></template>
        <div class="space-y-4">
          <div v-for="item in siteStats" :key="item.label" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span>{{ item.label }}</span>
              <span class="text-slate-500">{{ item.value }}</span>
            </div>
            <el-progress :percentage="stats.total ? Math.round((item.value / stats.total) * 100) : 0" status="success" :stroke-width="12" />
          </div>
        </div>
      </el-card>
    </section>

    <el-card>
      <template #header><span class="font-semibold">最近 7 天任务趋势</span></template>
      <div class="grid grid-cols-1 md:grid-cols-7 gap-3">
        <div v-for="item in trendStats" :key="item.label" class="rounded-xl border border-slate-200 p-4 bg-white">
          <div class="text-xs text-slate-500">{{ item.label }}</div>
          <div class="mt-4 space-y-3">
            <div>
              <div class="flex items-center justify-between text-xs"><span>新建</span><span>{{ item.created }}</span></div>
              <div class="mt-1 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-sky-500" :style="{ width: `${Math.round((item.created / item.peak) * 100)}%` }" /></div>
            </div>
            <div>
              <div class="flex items-center justify-between text-xs"><span>归档</span><span>{{ item.archived }}</span></div>
              <div class="mt-1 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-emerald-500" :style="{ width: `${Math.round((item.archived / item.peak) * 100)}%` }" /></div>
            </div>
            <div>
              <div class="flex items-center justify-between text-xs"><span>导出</span><span>{{ item.exported }}</span></div>
              <div class="mt-1 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-amber-500" :style="{ width: `${Math.round((item.exported / item.peak) * 100)}%` }" /></div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <section class="grid grid-cols-1 xl:grid-cols-[1.35fr_0.95fr] gap-6">
      <el-card>
        <template #header>
          <div class="flex items-center justify-between gap-4 flex-wrap">
            <span class="font-semibold">任务管理中心</span>
            <div class="flex items-center gap-3 flex-wrap">
              <el-input v-model="keywordFilter" size="small" placeholder="搜索任务/关键词/URL" style="width: 220px" />
              <el-select v-model="statusFilter" size="small" style="width: 150px">
                <el-option label="全部状态" value="ALL" />
                <el-option label="排队中" value="PENDING" />
                <el-option label="执行中" value="RUNNING" />
                <el-option label="已完成" value="FINISHED" />
                <el-option label="部分失败" value="PARTIAL_FAILED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
              <el-select v-model="siteFilter" size="small" style="width: 140px">
                <el-option label="全部站点" value="ALL" />
                <el-option label="搜狐" value="搜狐" />
                <el-option label="Bing" value="Bing" />
                <el-option label="百度百科" value="百度百科" />
              </el-select>
              <el-select v-model="archiveFilter" size="small" style="width: 140px">
                <el-option label="全部归档" value="ALL" />
                <el-option label="仅活跃" value="ACTIVE" />
                <el-option label="仅归档" value="ARCHIVED" />
              </el-select>
              <el-select v-model="selectedGroupId" size="small" style="width: 180px">
                <el-option label="全部分组" value="ALL" />
                <el-option v-for="group in taskGroups" :key="group.groupId" :label="group.groupName" :value="group.groupId!" />
              </el-select>
              <el-button text type="primary" @click="loadData">刷新</el-button>
            </div>
          </div>
        </template>

        <el-table :data="filteredTasks" border stripe v-loading="loading">
          <el-table-column prop="taskId" label="任务ID" width="100" />
          <el-table-column label="站点" width="110">
            <template #default="scope">{{ siteLabel(detectSite(scope.row.url)) }}</template>
          </el-table-column>
          <el-table-column prop="keyword" label="关键词" min-width="130" />
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.runtime?.status ?? scope.row.taskStatus)">
                {{ statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="归档" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.archived ? 'info' : 'success'">{{ scope.row.archived ? '已归档' : '活跃' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分组" min-width="200">
            <template #default="scope">
              <div class="flex flex-wrap gap-2">
                <el-tag
                  v-for="group in resolveTaskGroups(scope.row.taskId)"
                  :key="group.groupId"
                  effect="dark"
                  :color="group.groupColor || undefined"
                >
                  {{ group.groupName }}
                </el-tag>
                <span v-if="!resolveTaskGroups(scope.row.taskId).length" class="text-xs text-slate-400">未分组</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="备注数" width="100">
            <template #default="scope">{{ taskNotesMap[scope.row.taskId]?.length ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        </el-table>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">任务分组热度</span></template>
        <div class="space-y-3">
          <button v-for="group in groupRanking" :key="group.groupId" type="button" class="w-full text-left rounded-xl border border-slate-200 p-4 hover:border-sky-300 transition" @click="openGroupDetail(group)">
            <div class="flex items-center justify-between gap-4">
              <div class="flex items-center gap-3 min-w-0">
                <span class="inline-block h-3 w-3 rounded-full" :style="{ backgroundColor: group.groupColor || '#94a3b8' }" />
                <div class="min-w-0">
                  <div class="font-medium truncate">{{ group.groupName }}</div>
                  <div class="text-xs text-slate-500 truncate">{{ group.description || '未填写说明' }}</div>
                </div>
              </div>
              <el-tag type="info">{{ group.taskCount }} 个任务</el-tag>
            </div>
          </button>
          <el-empty v-if="!groupRanking.length" description="还没有任务分组" />
        </div>
      </el-card>
    </section>

    <el-drawer v-model="groupDetailVisible" :title="activeGroup ? `${activeGroup.groupName} · 分组详情` : '分组详情'" size="48%">
      <div v-if="activeGroup" class="space-y-5">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="分组名称">{{ activeGroup.groupName }}</el-descriptions-item>
          <el-descriptions-item label="颜色">
            <span class="inline-flex items-center gap-2">
              <span class="inline-block h-3 w-3 rounded-full" :style="{ backgroundColor: activeGroup.groupColor || '#94a3b8' }" />
              {{ activeGroup.groupColor || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="任务数量">{{ activeGroupTasks.length }}</el-descriptions-item>
          <el-descriptions-item label="备注数量">{{ activeGroupNoteCount }}</el-descriptions-item>
          <el-descriptions-item label="导出记录">{{ activeGroupExportCount }}</el-descriptions-item>
          <el-descriptions-item label="说明">{{ activeGroup.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="activeGroupTasks" border stripe>
          <el-table-column prop="taskId" label="任务ID" width="90" />
          <el-table-column label="站点" width="110">
            <template #default="scope">{{ siteLabel(detectSite(scope.row.url)) }}</template>
          </el-table-column>
          <el-table-column prop="keyword" label="关键词" min-width="130" />
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.runtime?.status ?? scope.row.taskStatus)">
                {{ statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" width="90">
            <template #default="scope">{{ taskNotesMap[scope.row.taskId]?.length ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="归档" width="100">
            <template #default="scope">{{ scope.row.archived ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>
