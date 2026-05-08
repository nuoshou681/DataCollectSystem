<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  addBookmark, bindTaskGroup, cachePageResultMhtml, createTaskGroup, createTaskNote,
  deleteTaskNote, downloadPageResultMhtml, exportPageResults, fetchBookmarkIds,
  fetchResultTags, fetchTaskDetail, fetchTaskGroupBindings, fetchTaskGroups,
  fetchTaskNotes, getPageResultStreamUrl, getTaskRuntimeStreamUrl,
  removeBookmark, updateTaskArchived, updateTaskNote,
} from '@/api/api'
import type { CrawlerPageResult, ResultTag, Task, TaskDetail, TaskEvent, TaskGroup, TaskGroupBinding, TaskNote, TaskRuntime } from '@/types/entity'
import { detectSite, siteLabel, statusTagType, statusText } from '@/utils/task'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => Number(route.params.id))
const detail = ref<TaskDetail | null>(null)
const bookmarkIds = ref<Set<number>>(new Set())
const resultTags = ref<ResultTag[]>([])
const taskNotes = ref<TaskNote[]>([])
const taskGroups = ref<TaskGroup[]>([])
const groupBindings = ref<TaskGroupBinding[]>([])
const loading = ref(false)
const exporting = ref(false)
const cacheLoadingMap = ref<Record<number, boolean>>({})
const downloadLoadingMap = ref<Record<number, boolean>>({})
const noteDraft = ref('')
const editingNoteId = ref<number | null>(null)
const selectedGroupId = ref<number | null>(null)
const groupDialogVisible = ref(false)
const groupForm = ref<TaskGroup>({ groupName: '', groupColor: '#2563eb', description: '' })

let pageStream: EventSource | null = null
let runtimeStream: EventSource | null = null

const task = computed(() => detail.value?.task)
const pageResults = computed(() => detail.value?.pageResults ?? [])
const events = computed(() => detail.value?.events ?? [])
const files = computed(() => detail.value?.files ?? [])
const runtime = computed(() => detail.value?.runtime)
const runtimeStatus = computed(() => runtime.value?.status ?? task.value?.taskStatus)
const activeTaskGroups = computed(() => {
  const groupMap = new Map(taskGroups.value.map(g => [g.groupId, g]))
  return groupBindings.value.map(b => groupMap.get(b.groupId)).filter((g): g is TaskGroup => Boolean(g))
})

async function load() {
  loading.value = true
  try {
    const [d, bm, tags, notes, groups, bindings] = await Promise.all([
      fetchTaskDetail(taskId.value),
      fetchBookmarkIds(),
      fetchResultTags(),
      fetchTaskNotes(taskId.value),
      fetchTaskGroups(),
      fetchTaskGroupBindings(taskId.value),
    ])
    detail.value = d
    bookmarkIds.value = bm
    resultTags.value = tags
    taskNotes.value = notes
    taskGroups.value = groups
    groupBindings.value = bindings
  } catch { ElMessage.error('任务详情加载失败') } finally { loading.value = false }
}

function connectStreams() {
  pageStream?.close()
  pageStream = new EventSource(getPageResultStreamUrl())
  pageStream.addEventListener('crawler-page-result', event => {
    try {
      const incoming = JSON.parse((event as MessageEvent).data) as CrawlerPageResult
      if (!incoming?.taskId || incoming.taskId !== taskId.value) return
      const pages = detail.value?.pageResults
      if (!pages) return
      const idx = pages.findIndex(p => p.pageResultId && p.pageResultId === incoming.pageResultId)
      if (idx >= 0) pages[idx] = { ...pages[idx], ...incoming }
      else pages.push(incoming)
    } catch { /* noop */ }
  })
  runtimeStream?.close()
  runtimeStream = new EventSource(getTaskRuntimeStreamUrl())
  runtimeStream.addEventListener('task-runtime', event => {
    try {
      const incoming = JSON.parse((event as MessageEvent).data) as TaskRuntime
      if (!incoming?.taskId || incoming.taskId !== taskId.value) return
      if (detail.value) detail.value.runtime = { ...(detail.value.runtime ?? {} as TaskRuntime), ...incoming }
    } catch { /* noop */ }
  })
}

async function toggleBookmark(prId?: number) {
  if (!prId) return
  try {
    if (bookmarkIds.value.has(prId)) { await removeBookmark(prId); bookmarkIds.value.delete(prId); ElMessage.success('已取消收藏') }
    else { await addBookmark(prId); bookmarkIds.value.add(prId); ElMessage.success('已收藏') }
  } catch { ElMessage.error('操作失败') }
}

async function cacheMhtml(prId?: number) {
  if (!prId) return
  cacheLoadingMap.value[prId] = true
  try {
    const result = await cachePageResultMhtml(prId)
    if (result && detail.value) {
      const idx = pageResults.value.findIndex(p => p.pageResultId === prId)
      if (idx >= 0) detail.value.pageResults[idx] = result
    }
  } finally { delete cacheLoadingMap.value[prId] }
}

async function downloadMhtml(prId?: number) {
  if (!prId) return
  downloadLoadingMap.value[prId] = true
  try {
    const result = await downloadPageResultMhtml(prId)
    const url = window.URL.createObjectURL(result.blob)
    const a = document.createElement('a'); a.href = url; a.download = result.fileName; a.click(); a.remove()
    window.URL.revokeObjectURL(url)
  } finally { delete downloadLoadingMap.value[prId] }
}

async function submitNote() {
  if (!noteDraft.value.trim()) { ElMessage.error('请输入备注内容'); return }
  try {
    if (editingNoteId.value) await updateTaskNote(editingNoteId.value, noteDraft.value.trim())
    else await createTaskNote(taskId.value, noteDraft.value.trim())
    taskNotes.value = await fetchTaskNotes(taskId.value)
    noteDraft.value = ''; editingNoteId.value = null
    ElMessage.success('备注已保存')
  } catch { ElMessage.error('保存失败') }
}

function editNote(note: TaskNote) { editingNoteId.value = note.noteId ?? null; noteDraft.value = note.noteContent }
async function removeNote(note: TaskNote) {
  if (!note.noteId) return
  try {
    await deleteTaskNote(note.noteId)
    taskNotes.value = await fetchTaskNotes(taskId.value)
    if (editingNoteId.value === note.noteId) { editingNoteId.value = null; noteDraft.value = '' }
    ElMessage.success('备注已删除')
  } catch { ElMessage.error('删除失败') }
}

async function bindGroup() {
  if (!selectedGroupId.value) { ElMessage.error('请选择分组'); return }
  try {
    await bindTaskGroup(taskId.value, selectedGroupId.value)
    groupBindings.value = await fetchTaskGroupBindings(taskId.value)
    ElMessage.success('已加入分组')
  } catch { ElMessage.error('绑定失败') }
}

async function submitGroup() {
  if (!groupForm.value.groupName.trim()) { ElMessage.error('分组名称不能为空'); return }
  try {
    await createTaskGroup(groupForm.value)
    taskGroups.value = await fetchTaskGroups()
    groupForm.value = { groupName: '', groupColor: '#2563eb', description: '' }
    groupDialogVisible.value = false
    ElMessage.success('分组创建成功')
  } catch { ElMessage.error('创建失败') }
}

async function handleToggleArchive() {
  const t = task.value
  if (!t) return
  try {
    await updateTaskArchived(t.taskId, !t.archived)
    if (detail.value) { detail.value.task.archived = !t.archived; detail.value.task.archivedAt = t.archived ? new Date().toISOString() : null }
    ElMessage.success(t.archived ? '已归档' : '已取消归档')
  } catch { ElMessage.error('操作失败') }
}

async function handleExport() {
  exporting.value = true
  try {
    const result = await exportPageResults(taskId.value)
    const url = window.URL.createObjectURL(result.blob)
    const a = document.createElement('a'); a.href = url; a.download = result.fileName; a.click(); a.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally { exporting.value = false }
}

watch(taskId, load)
onMounted(() => { load(); connectStreams() })
onBeforeUnmount(() => { pageStream?.close(); runtimeStream?.close() })
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <div class="flex items-center gap-3">
          <el-button text @click="router.push('/tasks')">← 返回</el-button>
          <h2 class="text-xl font-semibold text-slate-800">任务详情 #{{ taskId }}</h2>
          <el-tag v-if="task" :type="statusTagType(runtimeStatus)">{{ statusText(runtimeStatus) }}</el-tag>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <el-button plain :loading="exporting" @click="handleExport">导出结果</el-button>
        <el-button plain type="warning" @click="handleToggleArchive">{{ task?.archived ? '取消归档' : '归档任务' }}</el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <template #header><span class="font-semibold">基本信息</span></template>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm" v-if="task">
        <div><span class="text-slate-400">关键词</span><div class="font-medium mt-0.5">{{ task.keyword }}</div></div>
        <div><span class="text-slate-400">站点</span><div class="font-medium mt-0.5">{{ siteLabel(detectSite(task.url)) }}</div></div>
        <div><span class="text-slate-400">URL</span><div class="font-medium mt-0.5 truncate max-w-xs">{{ task.url }}</div></div>
        <div><span class="text-slate-400">节点</span><div class="font-medium mt-0.5">{{ runtime?.assignedNodeId || task.nodeId || '未分配' }}</div></div>
        <div><span class="text-slate-400">进度</span><div class="font-medium mt-0.5">{{ runtime?.completedPages ?? 0 }}/{{ runtime?.expectedPages ?? task.totalPages ?? 0 }} · 成功 {{ runtime?.successPages ?? 0 }} · 失败 {{ runtime?.failedPages ?? 0 }}</div></div>
        <div><span class="text-slate-400">创建时间</span><div class="font-medium mt-0.5">{{ task.createdAt || '-' }}</div></div>
        <div><span class="text-slate-400">开始时间</span><div class="font-medium mt-0.5">{{ runtime?.startedAt || '-' }}</div></div>
        <div><span class="text-slate-400">结束时间</span><div class="font-medium mt-0.5">{{ runtime?.finishedAt || '-' }}</div></div>
      </div>
      <el-progress v-if="task" :percentage="runtime?.progressPercent ?? task.taskProgress ?? 0" :stroke-width="12" class="mt-4" />
      <div v-if="runtime?.lastErrorMessage" class="mt-3 text-sm text-rose-600 bg-rose-50 rounded-lg p-3">{{ runtime.lastErrorMessage }}</div>
    </el-card>

    <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header><span class="font-semibold">页面结果 ({{ pageResults.length }})</span></template>
        <div class="max-h-96 overflow-auto space-y-3">
          <div v-for="page in pageResults" :key="page.pageResultId || `${page.taskId}-${page.pageIndex}`" class="rounded-lg border border-slate-200 p-3">
            <div class="flex items-center justify-between gap-3">
              <div class="font-medium text-sm truncate">{{ page.pageTitle || `页面 ${page.pageIndex}` }}</div>
              <el-tag :type="page.success ? 'success' : 'danger'" size="small">{{ page.success ? 'SUCCESS' : 'FAILED' }}</el-tag>
            </div>
            <div class="text-xs text-slate-500 break-all mt-1">{{ page.pageUrl }}</div>
            <div class="text-xs text-slate-400 mt-1">siteType={{ page.siteType || '-' }} · {{ page.mimeType || '-' }} · {{ page.fileSizeBytes ?? '-' }} bytes</div>
            <div class="text-xs text-rose-500 mt-1" v-if="page.errorMessage">{{ page.errorMessage }}</div>
            <div class="flex items-center gap-2 mt-2">
              <el-button size="small" text :type="page.pageResultId && bookmarkIds.has(page.pageResultId) ? 'warning' : 'primary'" @click="toggleBookmark(page.pageResultId)">
                {{ page.pageResultId && bookmarkIds.has(page.pageResultId) ? '取消收藏' : '收藏' }}
              </el-button>
              <el-button size="small" text type="primary" :loading="Boolean(cacheLoadingMap[page.pageResultId || 0])" @click="cacheMhtml(page.pageResultId)">{{ page.mhtmlCached ? '已缓存' : '缓存MHTML' }}</el-button>
              <el-button size="small" text type="success" :loading="Boolean(downloadLoadingMap[page.pageResultId || 0])" @click="downloadMhtml(page.pageResultId)">下载</el-button>
            </div>
          </div>
          <div v-if="!pageResults.length" class="text-center py-8 text-slate-400">暂无页面结果</div>
        </div>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">事件时间线</span></template>
        <div class="max-h-96 overflow-auto">
          <el-timeline v-if="events.length">
            <el-timeline-item v-for="event in events" :key="event.eventId" :timestamp="event.createdAt || ''" :type="event.eventLevel === 'ERROR' ? 'danger' : event.eventLevel === 'WARN' ? 'warning' : 'primary'" size="small">
              <div class="font-medium text-sm">{{ event.eventType }}</div>
              <div class="text-xs text-slate-500 mt-1">{{ event.eventMessage }}</div>
              <div class="text-xs text-slate-400">node={{ event.nodeId || '-' }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="text-center py-8 text-slate-400">暂无事件</div>
        </div>
      </el-card>
    </div>

    <el-card>
      <template #header><span class="font-semibold">任务整理</span></template>
      <div class="space-y-4">
        <div class="flex items-center gap-3 flex-wrap">
          <el-select v-model="selectedGroupId" clearable placeholder="选择任务分组" style="width: 220px">
            <el-option v-for="group in taskGroups" :key="group.groupId" :label="group.groupName" :value="group.groupId!" />
          </el-select>
          <el-button type="primary" plain @click="bindGroup">加入分组</el-button>
          <el-button plain @click="groupDialogVisible = true">新建分组</el-button>
        </div>
        <div class="flex flex-wrap gap-2">
          <el-tag v-for="group in activeTaskGroups" :key="group.groupId" effect="dark" :color="group.groupColor || undefined">{{ group.groupName }}</el-tag>
          <span v-if="!activeTaskGroups.length" class="text-sm text-slate-400">当前未加入分组</span>
        </div>
        <el-divider />
        <el-input v-model="noteDraft" type="textarea" :rows="3" placeholder="补充任务备注、异常说明、结果整理结论" />
        <div class="flex items-center justify-between">
          <span class="text-xs text-slate-400">备注用于记录任务异常、结果整理结论和后续说明</span>
          <div class="flex items-center gap-2">
            <el-button v-if="editingNoteId" @click="editingNoteId = null; noteDraft = ''">取消编辑</el-button>
            <el-button type="primary" @click="submitNote">{{ editingNoteId ? '保存修改' : '添加备注' }}</el-button>
          </div>
        </div>
        <div class="space-y-2 max-h-48 overflow-auto">
          <div v-for="note in taskNotes" :key="note.noteId" class="rounded-lg border border-slate-200 p-3">
            <div class="text-sm text-slate-700">{{ note.noteContent }}</div>
            <div class="mt-2 flex items-center justify-between">
              <span class="text-xs text-slate-400">{{ note.updatedAt || note.createdAt || '-' }}</span>
              <div class="flex items-center gap-2">
                <el-button size="small" text type="primary" @click="editNote(note)">编辑</el-button>
                <el-button size="small" text type="danger" @click="removeNote(note)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-if="!taskNotes.length" class="text-center py-4 text-slate-400">还没有备注</div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="groupDialogVisible" title="新建任务分组" width="480px">
      <el-form label-width="80px">
        <el-form-item label="分组名称"><el-input v-model="groupForm.groupName" maxlength="40" /></el-form-item>
        <el-form-item label="分组颜色"><el-color-picker v-model="groupForm.groupColor" /></el-form-item>
        <el-form-item label="分组说明"><el-input v-model="groupForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
