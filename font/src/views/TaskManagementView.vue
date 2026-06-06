<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArchiveBoxIcon, FolderIcon, PencilSquareIcon, QueueListIcon, TrashIcon } from '@heroicons/vue/24/outline'
import {
  batchBindTaskGroup,
  createTaskGroup,
  deleteTaskGroup,
  fetchAllTaskGroupBindings,
  fetchExportRecords,
  fetchTaskGroups,
  fetchTaskNotes,
  fetchTasks,
  unbindTaskGroup,
  updateTaskGroup,
} from '@/api/api'
import type { ExportRecord, Task, TaskGroup, TaskGroupBinding, TaskNote } from '@/types/entity'
import { detectSite, siteLabel, statusTagType, statusText } from '@/utils/task'

const loading = ref(false)
const tasks = ref<Task[]>([])
const taskGroups = ref<TaskGroup[]>([])
const exportRecords = ref<ExportRecord[]>([])
const taskNotesMap = ref<Record<number, TaskNote[]>>({})
const allBindings = ref<TaskGroupBinding[]>([])
const statusFilter = ref('ALL')
const siteFilter = ref('ALL')
const archiveFilter = ref('ALL')
const selectedGroupId = ref<number | 'ALL'>('ALL')
const keywordFilter = ref('')
const groupDetailVisible = ref(false)
const activeGroup = ref<TaskGroup | null>(null)
const selectedTaskIds = ref<Set<number>>(new Set())

// Group form
const groupDialogVisible = ref(false)
const editingGroup = ref<TaskGroup | null>(null)
const groupForm = ref({ groupName: '', groupColor: '#3b82f6', description: '' })

async function loadData() {
  loading.value = true
  try {
    const taskData = await fetchTasks()
    tasks.value = taskData
    const taskIds = taskData.map(item => item.taskId)
    const [groups, exports, noteEntries, bindings] = await Promise.all([
      fetchTaskGroups(),
      fetchExportRecords(),
      Promise.all(taskIds.map(async id => [id, await fetchTaskNotes(id)] as const)),
      fetchAllTaskGroupBindings(),
    ])
    taskGroups.value = groups
    exportRecords.value = exports
    taskNotesMap.value = Object.fromEntries(noteEntries)
    allBindings.value = bindings
    selectedTaskIds.value.clear()
  } catch {
    ElMessage.error('任务管理中心数据加载失败')
  } finally {
    loading.value = false
  }
}

const groupMap = computed(() => new Map(taskGroups.value.map(g => [g.groupId, g])))

function getTaskBindings(taskId: number) {
  return allBindings.value.filter(b => b.taskId === taskId)
}

function resolveTaskGroups(taskId: number) {
  return getTaskBindings(taskId)
    .map(b => groupMap.value.get(b.groupId))
    .filter((g): g is TaskGroup => Boolean(g))
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
    const matchesGroup = selectedGroupId.value === 'ALL' || groups.some(g => g.groupId === selectedGroupId.value)
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

const groupRanking = computed(() => {
  return taskGroups.value
    .map(group => ({
      ...group,
      taskCount: tasks.value.filter(task => resolveTaskGroups(task.taskId).some(g => g.groupId === group.groupId)).length,
    }))
    .sort((a, b) => b.taskCount - a.taskCount)
})

const activeGroupTasks = computed(() => {
  if (!activeGroup.value?.groupId) return []
  return tasks.value.filter(task => resolveTaskGroups(task.taskId).some(g => g.groupId === activeGroup.value?.groupId))
})

const activeGroupNoteCount = computed(() =>
  activeGroupTasks.value.reduce((sum, task) => sum + (taskNotesMap.value[task.taskId]?.length ?? 0), 0))

const activeGroupExportCount = computed(() => {
  const ids = new Set(activeGroupTasks.value.map(t => t.taskId))
  return exportRecords.value.filter(r => r.taskId && ids.has(r.taskId)).length
})

function handleSelectionChange(rows: Task[]) {
  selectedTaskIds.value = new Set(rows.map(r => r.taskId))
}

// ── Group CRUD ──
function openCreateGroup() {
  editingGroup.value = null
  groupForm.value = { groupName: '', groupColor: '#3b82f6', description: '' }
  groupDialogVisible.value = true
}

function openEditGroup(group: TaskGroup) {
  editingGroup.value = group
  groupForm.value = {
    groupName: group.groupName,
    groupColor: group.groupColor || '#3b82f6',
    description: group.description || '',
  }
  groupDialogVisible.value = true
}

async function submitGroup() {
  if (!groupForm.value.groupName.trim()) {
    ElMessage.warning('分组名称不能为空')
    return
  }
  try {
    if (editingGroup.value) {
      await updateTaskGroup(editingGroup.value.groupId!, groupForm.value)
      ElMessage.success('分组已更新')
    } else {
      await createTaskGroup(groupForm.value as TaskGroup)
      ElMessage.success('分组创建成功')
    }
    groupDialogVisible.value = false
    await loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDeleteGroup(group: TaskGroup) {
  try {
    await ElMessageBox.confirm(`确定删除分组「${group.groupName}」吗？分组内的任务不会被删除。`, '删除分组', { type: 'warning' })
    await deleteTaskGroup(group.groupId!)
    ElMessage.success('分组已删除')
    await loadData()
  } catch { /* cancelled */ }
}

// ── Batch bind ──
const batchGroupId = ref<number | null>(null)

async function handleBatchBind() {
  if (!selectedTaskIds.value.size) { ElMessage.warning('请先选择任务'); return }
  if (!batchGroupId.value) { ElMessage.warning('请选择目标分组'); return }
  try {
    await batchBindTaskGroup(Array.from(selectedTaskIds.value), batchGroupId.value)
    ElMessage.success(`已将 ${selectedTaskIds.value.size} 个任务加入分组`)
    batchGroupId.value = null
    await loadData()
  } catch {
    ElMessage.error('批量加入分组失败')
  }
}

// ── Unbind ──
async function handleUnbind(taskId: number, groupId: number, groupName: string) {
  try {
    await unbindTaskGroup(taskId, groupId)
    ElMessage.success(`已从「${groupName}」移出`)
    await loadData()
  } catch {
    ElMessage.error('移出分组失败')
  }
}

// ── Group detail ──
function openGroupDetail(group: TaskGroup) {
  activeGroup.value = group
  groupDetailVisible.value = true
}

onMounted(() => { void loadData() })
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500">
        <div class="flex items-center gap-3"><QueueListIcon class="w-6 h-6 text-blue-500" /><div><div class="text-sm text-slate-500">任务总量</div><div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-sky-500">
        <div class="flex items-center gap-3"><ArchiveBoxIcon class="w-6 h-6 text-sky-500" /><div><div class="text-sm text-slate-500">已归档任务</div><div class="text-2xl font-bold text-sky-600">{{ stats.archived }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500">
        <div class="flex items-center gap-3"><FolderIcon class="w-6 h-6 text-emerald-500" /><div><div class="text-sm text-slate-500">已分组任务</div><div class="text-2xl font-bold text-emerald-600">{{ stats.grouped }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500">
        <div class="flex items-center gap-3"><PencilSquareIcon class="w-6 h-6 text-amber-500" /><div><div class="text-sm text-slate-500">已有备注任务</div><div class="text-2xl font-bold text-amber-600">{{ stats.noted }}</div></div></div>
      </el-card>
    </section>

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
                <el-option label="搜狐新闻" value="搜狐新闻" />
                <el-option label="Bing 搜索" value="Bing 搜索" />
                <el-option label="百度百科" value="百度百科" />
                <el-option label="腾讯新闻" value="腾讯新闻" />
                <el-option label="新浪新闻" value="新浪新闻" />
                <el-option label="澎湃新闻" value="澎湃新闻" />
                <el-option label="环球网" value="环球网" />
                <el-option label="中国新闻网" value="中国新闻网" />
                <el-option label="央视网新闻" value="央视网新闻" />
                <el-option label="观察者网" value="观察者网" />
                <el-option label="维基百科" value="维基百科" />
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

        <!-- Batch bar -->
        <div v-if="selectedTaskIds.size" class="flex items-center gap-3 mb-3 px-3 py-2 bg-blue-50 border border-blue-200 rounded-lg text-sm">
          <span class="text-blue-700 font-medium">已选 {{ selectedTaskIds.size }} 个任务</span>
          <el-select v-model="batchGroupId" size="small" placeholder="选择分组" style="width: 160px">
            <el-option v-for="g in taskGroups" :key="g.groupId" :label="g.groupName" :value="g.groupId!" />
          </el-select>
          <el-button size="small" type="primary" :disabled="!batchGroupId" @click="handleBatchBind">加入分组</el-button>
        </div>

        <el-table :data="filteredTasks" border stripe v-loading="loading" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" />
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
              <div class="flex flex-wrap gap-1">
                <el-tag
                  v-for="group in resolveTaskGroups(scope.row.taskId)"
                  :key="group.groupId"
                  size="small"
                  effect="dark"
                  :color="group.groupColor || undefined"
                  closable
                  @close="handleUnbind(scope.row.taskId, group.groupId!, group.groupName)"
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
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">任务分组</span>
            <el-button size="small" type="primary" text @click="openCreateGroup">新建分组</el-button>
          </div>
        </template>
        <div class="space-y-3">
          <button
            v-for="group in groupRanking" :key="group.groupId" type="button"
            class="w-full text-left rounded-xl border border-slate-200 p-4 hover:border-sky-300 transition group"
            @click="openGroupDetail(group)"
          >
            <div class="flex items-center justify-between gap-4">
              <div class="flex items-center gap-3 min-w-0">
                <span class="inline-block h-3 w-3 rounded-full flex-shrink-0" :style="{ backgroundColor: group.groupColor || '#94a3b8' }" />
                <div class="min-w-0">
                  <div class="font-medium truncate">{{ group.groupName }}</div>
                  <div class="text-xs text-slate-500 truncate">{{ group.description || '未填写说明' }}</div>
                </div>
              </div>
              <div class="flex items-center gap-2 flex-shrink-0">
                <el-tag type="info" size="small">{{ group.taskCount }} 个</el-tag>
                <el-button size="small" text @click.stop="openEditGroup(group)" class="opacity-0 group-hover:opacity-100 transition">
                  <PencilSquareIcon class="w-4 h-4" />
                </el-button>
                <el-button size="small" text type="danger" @click.stop="handleDeleteGroup(group)" class="opacity-0 group-hover:opacity-100 transition">
                  <TrashIcon class="w-4 h-4" />
                </el-button>
              </div>
            </div>
          </button>
          <el-empty v-if="!groupRanking.length" description="还没有任务分组，点击上方按钮创建" />
        </div>
      </el-card>
    </section>

    <!-- Group detail drawer -->
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
          <el-table-column type="selection" width="42" />
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
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button size="small" text type="danger" @click="handleUnbind(scope.row.taskId, activeGroup!.groupId!, activeGroup!.groupName)">移出</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- Group create/edit dialog -->
    <el-dialog v-model="groupDialogVisible" :title="editingGroup ? '编辑分组' : '新建分组'" width="420px">
      <div class="space-y-4">
        <div>
          <label class="text-sm text-slate-600 mb-1 block">分组名称</label>
          <el-input v-model="groupForm.groupName" maxlength="40" placeholder="输入分组名称" />
        </div>
        <div>
          <label class="text-sm text-slate-600 mb-1 block">颜色</label>
          <el-color-picker v-model="groupForm.groupColor" />
        </div>
        <div>
          <label class="text-sm text-slate-600 mb-1 block">说明</label>
          <el-input v-model="groupForm.description" type="textarea" :rows="3" maxlength="200" placeholder="可选，分组说明" />
        </div>
      </div>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">{{ editingGroup ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
