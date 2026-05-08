<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  addBookmark,
  bindTaskGroup,
  cachePageResultMhtml,
  createTaskGroup,
  createTaskNote,
  deleteTaskNote,
  dispatchBatchTask,
  dispatchTask,
  downloadPageResultMhtml,
  exportPageResults,
  fetchResultTags,
  fetchBookmarkIds,
  fetchTaskGroupBindings,
  fetchTaskGroups,
  fetchTaskNotes,
  fetchTaskDetail,
  fetchTasks,
  getPageResultStreamUrl,
  getTaskRuntimeStreamUrl,
  importTasksCsv,
  removeBookmark,
  updateTaskArchived,
  updateTaskNote,
} from '@/api/api'
import type { CrawlerPageResult, DispatchTaskPayload, ResultTag, Task, TaskDetail, TaskEvent, TaskGroup, TaskGroupBinding, TaskNote, TaskRuntime } from '@/types/entity'
import { getCurrentUserProfile } from '@/utils/auth'
import { detectSite, siteLabel, statusTagType, statusText } from '@/utils/task'

type SiteKey = 'sohu' | 'bing' | 'baike' | 'tencent_news' | 'sina_news' | 'thepaper' | 'huanqiu' | 'chinanews' | 'cctv_news' | 'guancha' | 'wikipedia'
type StatusFilter = 'ALL' | 'PENDING' | 'RUNNING' | 'FINISHED' | 'PARTIAL_FAILED' | 'FAILED'

interface SiteOption {
  key: SiteKey
  label: string
  seedUrl: string
  siteType: string
  category: string
  icon: string
  domain: string
  description: string
  method: 'search' | 'scrape'
}

const siteOptions: SiteOption[] = [
  { key: 'sohu', label: '搜狐新闻', seedUrl: 'https://search.sohu.com/?keyword=', siteType: 'SOHU', category: '新闻资讯', icon: '📰', domain: 'search.sohu.com', description: '搜狐新闻搜索引擎，覆盖全网新闻资讯', method: 'scrape' },
  { key: 'bing', label: 'Bing 搜索', seedUrl: 'https://www.bing.com/search?q=', siteType: 'BING', category: '搜索引擎', icon: '🔍', domain: 'www.bing.com', description: '微软 Bing 全网搜索，结果多样覆盖广', method: 'search' },
  { key: 'baike', label: '百度百科', seedUrl: 'https://baike.baidu.com/item/', siteType: 'BAIDU_BAIKE', category: '百科知识', icon: '📚', domain: 'baike.baidu.com', description: '全球最大中文百科全书，权威知识库', method: 'scrape' },
  { key: 'tencent_news', label: '腾讯新闻', seedUrl: 'https://news.qq.com', siteType: 'TENCENT_NEWS', category: '新闻资讯', icon: '🐧', domain: 'news.qq.com', description: '腾讯新闻门户，实时热点资讯平台', method: 'search' },
  { key: 'sina_news', label: '新浪新闻', seedUrl: 'https://news.sina.com.cn', siteType: 'SINA_NEWS', category: '新闻资讯', icon: '📡', domain: 'news.sina.com.cn', description: '新浪新闻中心，全方位时事报道', method: 'search' },
  { key: 'thepaper', label: '澎湃新闻', seedUrl: 'https://www.thepaper.cn', siteType: 'THEPAPER', category: '新闻资讯', icon: '🗞️', domain: 'www.thepaper.cn', description: '澎湃新闻，专注时政与深度报道', method: 'search' },
  { key: 'huanqiu', label: '环球网', seedUrl: 'https://www.huanqiu.com', siteType: 'HUANQIU', category: '新闻资讯', icon: '🌐', domain: 'www.huanqiu.com', description: '环球网，全球视角的国际资讯平台', method: 'search' },
  { key: 'chinanews', label: '中国新闻网', seedUrl: 'https://www.chinanews.com.cn', siteType: 'CHINANEWS', category: '新闻资讯', icon: '🏛️', domain: 'www.chinanews.com.cn', description: '中国新闻网，权威国家新闻通讯社', method: 'search' },
  { key: 'cctv_news', label: '央视网新闻', seedUrl: 'https://news.cctv.com', siteType: 'CCTV_NEWS', category: '新闻资讯', icon: '📺', domain: 'news.cctv.com', description: '央视网新闻频道，官方主流媒体', method: 'search' },
  { key: 'guancha', label: '观察者网', seedUrl: 'https://www.guancha.cn', siteType: 'GUANCHA', category: '新闻资讯', icon: '👁️', domain: 'www.guancha.cn', description: '观察者网，深度评论与时事分析', method: 'search' },
  { key: 'wikipedia', label: '维基百科', seedUrl: 'https://en.wikipedia.org/w/index.php?search=', siteType: 'WIKIPEDIA', category: '百科知识', icon: '📖', domain: 'en.wikipedia.org', description: '英文维基百科，全球最大开放百科', method: 'scrape' },
]

const selectedSites = ref<SiteOption['key'][]>(['sohu'])
const siteKeywordFilter = ref('')
const commonKeyword = ref('')
const batchKeywordsText = ref('')
const batchName = ref('')
const batchNotes = ref('')
const batchMode = ref(false)
const selectedTaskTagIds = ref<number[]>([])
const keywordFilter = ref('')
const statusFilter = ref<StatusFilter>('ALL')
const loading = ref(false)
const submitting = ref(false)
const exporting = ref(false)
const tasks = ref<Task[]>([])
const detailMap = ref<Record<number, TaskDetail | undefined>>({})
const bookmarkIds = ref<Set<number>>(new Set())
const resultTags = ref<ResultTag[]>([])
const taskNotesMap = ref<Record<number, TaskNote[]>>({})
const taskGroups = ref<TaskGroup[]>([])
const taskGroupBindingMap = ref<Record<number, TaskGroupBinding[]>>({})
const activeTaskId = ref<number | null>(null)
const streamConnected = ref(false)
const runtimeStreamConnected = ref(false)
const streamStatusText = ref('正在连接实时结果流...')
const runtimeStreamText = ref('正在连接运行态流...')
let resultStream: EventSource | null = null
let runtimeStream: EventSource | null = null

const cacheLoadingMap = ref<Record<number, boolean>>({})
const downloadLoadingMap = ref<Record<number, boolean>>({})
const noteDraft = ref('')
const editingNoteId = ref<number | null>(null)
const selectedGroupId = ref<number | null>(null)
const taskGroupDialogVisible = ref(false)
const groupForm = ref<TaskGroup>({
  groupName: '',
  groupColor: '#2563eb',
  description: '',
})
const siteOptionMap = new Map(siteOptions.map(option => [option.key, option]))

const csvInputRef = ref<HTMLInputElement | null>(null)
const importingCsv = ref(false)

function toggleSite(key: SiteKey) {
  const idx = selectedSites.value.indexOf(key)
  if (idx >= 0) {
    selectedSites.value.splice(idx, 1)
  } else {
    selectedSites.value.push(key)
  }
}

const siteCategories = computed(() => {
  const order = ['搜索引擎', '新闻资讯', '百科知识']
  const groups = new Map<string, SiteOption[]>()
  const filtered = siteKeywordFilter.value
    ? siteOptions.filter(s => s.label.includes(siteKeywordFilter.value) || s.domain.includes(siteKeywordFilter.value) || s.description.includes(siteKeywordFilter.value))
    : siteOptions
  for (const s of filtered) {
    const list = groups.get(s.category) || []
    list.push(s)
    groups.set(s.category, list)
  }
  return order.filter(cat => groups.has(cat)).map(cat => ({ name: cat, sites: groups.get(cat)! }))
})

function triggerCsvImport() {
  csvInputRef.value?.click()
}

async function handleCsvFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importingCsv.value = true
  try {
    const result = await importTasksCsv(file, 10)
    if (result) {
      ElMessage.success(`CSV导入完成: 成功 ${result.success ?? 0} 条，失败 ${result.failed ?? 0} 条`)
    }
    await loadTaskData()
    await loadTaskMetadata()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : 'CSV导入失败')
  } finally {
    importingCsv.value = false
    input.value = ''
  }
}

async function loadTaskData(taskIdToOpen?: number) {
  loading.value = true
  try {
    const [taskData, bookmarkedIds] = await Promise.all([fetchTasks(), fetchBookmarkIds()])
    tasks.value = taskData
    bookmarkIds.value = bookmarkedIds

    const detailEntries = await Promise.all(taskData.map(async task => [task.taskId, await fetchTaskDetail(task.taskId)] as const))
    detailMap.value = Object.fromEntries(
      detailEntries
        .filter((entry): entry is readonly [number, TaskDetail] => Boolean(entry[1]))
        .map(([taskId, detail]) => [taskId, detail]),
    )

    for (const task of tasks.value) {
      const detail = detailMap.value[task.taskId]
      if (detail?.runtime) {
        task.runtime = detail.runtime
      }
    }

    await loadTaskMetadata(taskData.map(task => task.taskId))

    if (taskIdToOpen) {
      activeTaskId.value = taskIdToOpen
    } else if (!activeTaskId.value && taskData.length > 0) {
      const firstTask = taskData[0]
      if (firstTask) {
        activeTaskId.value = firstTask.taskId
      }
    }
  } catch {
    ElMessage.error('任务数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadResultTags() {
  try {
    resultTags.value = await fetchResultTags()
  } catch {
    ElMessage.error('任务标签加载失败')
  }
}

async function loadTaskMetadata(taskIds?: number[]) {
  const ids = taskIds ?? tasks.value.map(task => task.taskId)
  if (!ids.length) {
    return
  }
  try {
    const [groups, noteEntries, bindingEntries] = await Promise.all([
      fetchTaskGroups(),
      Promise.all(ids.map(async taskId => [taskId, await fetchTaskNotes(taskId)] as const)),
      Promise.all(ids.map(async taskId => [taskId, await fetchTaskGroupBindings(taskId)] as const)),
    ])
    taskGroups.value = groups
    taskNotesMap.value = Object.fromEntries(noteEntries)
    taskGroupBindingMap.value = Object.fromEntries(bindingEntries)
  } catch {
    ElMessage.error('任务元数据加载失败')
  }
}

function createPayload(option: SiteOption, keyword: string): DispatchTaskPayload {
  const profile = getCurrentUserProfile()
  return {
    userId: profile.userId ?? null,
    keyword,
    url: option.seedUrl,
    source: 'manual',
    siteType: option.siteType,
    maxLinksPerLevel: 10,
    tagIds: selectedTaskTagIds.value,
  }
}

async function submitTask() {
  if (selectedSites.value.length === 0) {
    ElMessage.error('至少选择一个站点')
    return
  }

  if (batchMode.value) {
    if (selectedSites.value.length !== 1) {
      ElMessage.error('批量创建任务时请只选择一个站点')
      return
    }
    if (!batchKeywordsText.value.trim()) {
      ElMessage.error('请填写批量关键词')
      return
    }
  } else if (!commonKeyword.value.trim()) {
    ElMessage.error('请填写关键词')
    return
  }

  if (batchMode.value) {
    submitting.value = true
    try {
      const firstSelectedSite = selectedSites.value[0]
      if (!firstSelectedSite) {
        throw new Error('请选择站点')
      }
      const selectedSite = siteOptionMap.get(firstSelectedSite)
      if (!selectedSite) {
        throw new Error('站点配置不存在')
      }
      await dispatchBatchTask({
        userId: getCurrentUserProfile().userId ?? null,
        url: selectedSite.seedUrl,
        siteType: selectedSite.siteType,
        source: 'manual',
        maxLinksPerLevel: 10,
        keywordsText: batchKeywordsText.value,
        batchName: batchName.value.trim(),
        batchNotes: batchNotes.value.trim(),
        keyword: '',
        tagIds: selectedTaskTagIds.value,
      })
      ElMessage.success('批量任务创建成功')
      await loadTaskData()
      await loadTaskMetadata()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '批量任务创建失败')
    } finally {
      submitting.value = false
    }
    return
  }

  submitting.value = true
  try {
    const requests = selectedSites.value.map(site => {
      const option = siteOptionMap.get(site)
      if (!option) {
        return Promise.resolve(null)
      }
      return dispatchTask(createPayload(option, commonKeyword.value.trim()))
    })
    await Promise.all(requests)
    ElMessage.success('任务创建成功')
    await loadTaskData()
    await loadTaskMetadata()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务创建失败')
  } finally {
    submitting.value = false
  }
}

function parsePageResultEvent(raw: string) {
  try {
    const parsed = JSON.parse(raw) as CrawlerPageResult
    return parsed && parsed.taskId !== undefined ? parsed : null
  } catch {
    return null
  }
}

function parseRuntimeEvent(raw: string) {
  try {
    const parsed = JSON.parse(raw) as TaskRuntime
    return parsed && parsed.taskId !== undefined ? parsed : null
  } catch {
    return null
  }
}

function upsertPageResult(incoming: CrawlerPageResult) {
  const detail = detailMap.value[incoming.taskId]
  if (!detail) {
    return
  }
  const index = detail.pageResults.findIndex(item => {
    if (incoming.pageResultId && item.pageResultId) {
      return item.pageResultId === incoming.pageResultId
    }
    return item.taskId === incoming.taskId && item.pageIndex === incoming.pageIndex && item.pageUrl === incoming.pageUrl
  })

  if (index >= 0) {
    detail.pageResults[index] = { ...detail.pageResults[index], ...incoming }
  } else {
    detail.pageResults.push(incoming)
  }
}

function upsertRuntime(incoming: TaskRuntime) {
  const task = tasks.value.find(item => item.taskId === incoming.taskId)
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
    detail.task.totalPages = incoming.expectedPages ?? detail.task.totalPages
    const alreadyFinished = detail.events.some(event => event.eventType === 'TASK_FINISHED')
    if (incoming.finishedAt && !alreadyFinished) {
      detail.events.push({
        eventId: Date.now(),
        taskId: incoming.taskId,
        eventType: 'TASK_RUNTIME_FINISHED',
        eventLevel: incoming.status === 'FAILED' || incoming.status === 'PARTIAL_FAILED' ? 'ERROR' : 'INFO',
        eventMessage: `任务运行态已更新为 ${incoming.status}`,
        createdAt: incoming.finishedAt,
        nodeId: incoming.assignedNodeId ?? null,
      })
    }
  }
}

function connectPageResultStream() {
  resultStream?.close()
  resultStream = new EventSource(getPageResultStreamUrl())
  resultStream.onopen = () => {
    streamConnected.value = true
    streamStatusText.value = '实时结果流已连接'
  }
  resultStream.addEventListener('crawler-page-result', event => {
    const payload = parsePageResultEvent((event as MessageEvent).data)
    if (payload) {
      upsertPageResult(payload)
    }
  })
  resultStream.onerror = () => {
    streamConnected.value = false
    streamStatusText.value = '实时结果流重连中...'
  }
}

function connectRuntimeStream() {
  runtimeStream?.close()
  runtimeStream = new EventSource(getTaskRuntimeStreamUrl())
  runtimeStream.onopen = () => {
    runtimeStreamConnected.value = true
    runtimeStreamText.value = '运行态流已连接'
  }
  runtimeStream.addEventListener('task-runtime', event => {
    const payload = parseRuntimeEvent((event as MessageEvent).data)
    if (payload) {
      upsertRuntime(payload)
    }
  })
  runtimeStream.onerror = () => {
    runtimeStreamConnected.value = false
    runtimeStreamText.value = '运行态流重连中...'
  }
}

function triggerBrowserDownload(blob: Blob, fileName: string) {
  const objectUrl = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(objectUrl)
}

async function cacheMhtml(pageResultId?: number) {
  if (!pageResultId) {
    return
  }
  cacheLoadingMap.value[pageResultId] = true
  try {
    const result = await cachePageResultMhtml(pageResultId)
    if (result) {
      upsertPageResult(result)
      ElMessage.success('MHTML 已缓存')
    }
  } finally {
    delete cacheLoadingMap.value[pageResultId]
  }
}

async function downloadMhtml(pageResultId?: number) {
  if (!pageResultId) {
    return
  }
  downloadLoadingMap.value[pageResultId] = true
  try {
    const result = await downloadPageResultMhtml(pageResultId)
    triggerBrowserDownload(result.blob, result.fileName)
  } finally {
    delete downloadLoadingMap.value[pageResultId]
  }
}

async function toggleBookmark(pageResultId?: number) {
  if (!pageResultId) {
    return
  }
  try {
    if (bookmarkIds.value.has(pageResultId)) {
      await removeBookmark(pageResultId)
      bookmarkIds.value.delete(pageResultId)
      ElMessage.success('已取消收藏')
    } else {
      await addBookmark(pageResultId)
      bookmarkIds.value.add(pageResultId)
      ElMessage.success('已加入收藏')
    }
  } catch {
    ElMessage.error('收藏操作失败')
  }
}

async function exportCurrentResults() {
  exporting.value = true
  try {
    const result = await exportPageResults(activeTaskId.value ?? undefined)
    triggerBrowserDownload(result.blob, result.fileName)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

async function submitTaskNote() {
  if (!activeTaskId.value || !noteDraft.value.trim()) {
    ElMessage.error('请输入备注内容')
    return
  }
  try {
    if (editingNoteId.value) {
      await updateTaskNote(editingNoteId.value, noteDraft.value.trim())
    } else {
      await createTaskNote(activeTaskId.value, noteDraft.value.trim())
    }
    taskNotesMap.value[activeTaskId.value] = await fetchTaskNotes(activeTaskId.value)
    noteDraft.value = ''
    editingNoteId.value = null
    ElMessage.success('任务备注已保存')
  } catch {
    ElMessage.error('任务备注保存失败')
  }
}

function editTaskNote(note: TaskNote) {
  editingNoteId.value = note.noteId ?? null
  noteDraft.value = note.noteContent
}

async function removeTaskNote(note: TaskNote) {
  if (!note.noteId || !activeTaskId.value) {
    return
  }
  try {
    await deleteTaskNote(note.noteId)
    taskNotesMap.value[activeTaskId.value] = await fetchTaskNotes(activeTaskId.value)
    if (editingNoteId.value === note.noteId) {
      editingNoteId.value = null
      noteDraft.value = ''
    }
    ElMessage.success('任务备注已删除')
  } catch {
    ElMessage.error('任务备注删除失败')
  }
}

async function submitTaskGroup() {
  if (!groupForm.value.groupName.trim()) {
    ElMessage.error('分组名称不能为空')
    return
  }
  try {
    await createTaskGroup(groupForm.value)
    taskGroups.value = await fetchTaskGroups()
    groupForm.value = {
      groupName: '',
      groupColor: '#2563eb',
      description: '',
    }
    taskGroupDialogVisible.value = false
    ElMessage.success('任务分组创建成功')
  } catch {
    ElMessage.error('任务分组创建失败')
  }
}

async function bindCurrentTaskGroup() {
  if (!activeTaskId.value || !selectedGroupId.value) {
    ElMessage.error('请选择任务分组')
    return
  }
  try {
    await bindTaskGroup(activeTaskId.value, selectedGroupId.value)
    taskGroupBindingMap.value[activeTaskId.value] = await fetchTaskGroupBindings(activeTaskId.value)
    ElMessage.success('任务已加入分组')
  } catch {
    ElMessage.error('任务分组绑定失败')
  }
}

async function toggleTaskArchive(task: Task) {
  try {
    await updateTaskArchived(task.taskId, !task.archived)
    task.archived = !task.archived
    task.archivedAt = task.archived ? new Date().toISOString() : null
    const detail = detailMap.value[task.taskId]
    if (detail) {
      detail.task.archived = task.archived
      detail.task.archivedAt = task.archivedAt
    }
    ElMessage.success(task.archived ? '任务已归档' : '任务已取消归档')
  } catch {
    ElMessage.error('任务归档操作失败')
  }
}

const filteredTasks = computed(() => {
  const keyword = keywordFilter.value.trim().toLowerCase()
  return tasks.value.filter(task => {
    const status = task.runtime?.status ?? task.taskStatus
    const matchesStatus = statusFilter.value === 'ALL' || status === statusFilter.value
    const matchesKeyword = !keyword
      || String(task.taskId).includes(keyword)
      || task.keyword.toLowerCase().includes(keyword)
      || task.url.toLowerCase().includes(keyword)
    return matchesStatus && matchesKeyword
  })
})

const taskStats = computed(() => {
  const total = tasks.value.length
  const pending = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'PENDING').length
  const running = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'RUNNING').length
  const finished = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'FINISHED').length
  const partialFailed = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'PARTIAL_FAILED').length
  const failed = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'FAILED').length
  return { total, pending, running, finished, partialFailed, failed }
})

const taskRows = computed(() => {
  return filteredTasks.value.map(task => {
    const detail = detailMap.value[task.taskId]
    const runtime = detail?.runtime ?? task.runtime
    return {
      ...task,
      runtime,
      pageResults: detail?.pageResults ?? [],
      events: detail?.events ?? ([] as TaskEvent[]),
      files: detail?.files ?? [],
      siteLabel: siteLabel(detectSite(task.url)),
    }
  })
})

const activeTaskDetail = computed(() => {
  return activeTaskId.value ? detailMap.value[activeTaskId.value] ?? null : null
})

const activeTaskNotes = computed(() => {
  if (!activeTaskId.value) {
    return []
  }
  return taskNotesMap.value[activeTaskId.value] ?? []
})

const activeTaskGroups = computed(() => {
  if (!activeTaskId.value) {
    return []
  }
  const bindings = taskGroupBindingMap.value[activeTaskId.value] ?? []
  const groupMap = new Map(taskGroups.value.map(group => [group.groupId, group]))
  return bindings.map(binding => groupMap.get(binding.groupId)).filter((group): group is TaskGroup => Boolean(group))
})

onMounted(() => {
  void loadResultTags()
  void loadTaskData()
  connectPageResultStream()
  connectRuntimeStream()
})

onBeforeUnmount(() => {
  resultStream?.close()
  runtimeStream?.close()
})
</script>

<template>
  <div class="p-6 space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-5 gap-4">
      <el-card><div class="text-sm text-gray-500">任务总数</div><div class="text-2xl font-semibold mt-2">{{ taskStats.total }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">待开始</div><div class="text-2xl font-semibold mt-2 text-slate-600">{{ taskStats.pending }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">执行中</div><div class="text-2xl font-semibold mt-2 text-blue-600">{{ taskStats.running }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">已完成</div><div class="text-2xl font-semibold mt-2 text-emerald-600">{{ taskStats.finished }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">部分失败/失败</div><div class="text-2xl font-semibold mt-2 text-rose-600">{{ taskStats.partialFailed + taskStats.failed }}</div></el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">创建采集任务</span>
          <div class="flex items-center gap-3">
            <el-tag :type="streamConnected ? 'success' : 'warning'" size="small">{{ streamStatusText }}</el-tag>
            <el-tag :type="runtimeStreamConnected ? 'success' : 'warning'" size="small">{{ runtimeStreamText }}</el-tag>
            <el-button type="primary" :loading="submitting" @click="submitTask">提交任务</el-button>
            <input ref="csvInputRef" type="file" accept=".csv" style="display:none" @change="handleCsvFileChange" />
            <el-button plain :loading="importingCsv" @click="triggerCsvImport">
              {{ importingCsv ? '导入中...' : '批量导入CSV' }}
            </el-button>
          </div>
        </div>
      </template>
      <el-form label-width="130px" class="max-w-5xl">
        <el-form-item label="采集站点">
          <div class="space-y-4 w-full">
            <div class="flex items-center gap-3">
              <el-input v-model="siteKeywordFilter" size="small" placeholder="搜索站点..." clearable style="width: 260px" />
              <span class="text-xs text-slate-400">已选 <b class="text-blue-600">{{ selectedSites.length }}</b> 个站点</span>
              <el-button v-if="selectedSites.length" text size="small" type="danger" @click="selectedSites = []">清空选择</el-button>
            </div>
            <div v-for="cat in siteCategories" :key="cat.name" class="space-y-2">
              <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">{{ cat.name }}</div>
              <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                <div
                  v-for="site in cat.sites"
                  :key="site.key"
                  class="relative rounded-xl border-2 p-4 cursor-pointer transition-all duration-200 select-none"
                  :class="selectedSites.includes(site.key) ? 'border-blue-500 bg-blue-50 shadow-md shadow-blue-100' : 'border-slate-200 hover:border-slate-300 hover:shadow-sm bg-white'"
                  @click="toggleSite(site.key)"
                >
                  <div v-if="selectedSites.includes(site.key)" class="absolute top-2 right-2 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center">
                    <span class="text-white text-xs leading-none">&#10003;</span>
                  </div>
                  <div class="flex items-center gap-3 mb-2">
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center text-xl flex-shrink-0" :class="selectedSites.includes(site.key) ? 'bg-blue-100' : 'bg-slate-100'">
                      {{ site.icon }}
                    </div>
                    <div class="min-w-0">
                      <div class="font-semibold text-sm truncate">{{ site.label }}</div>
                      <el-tag size="small" :type="site.method === 'search' ? 'success' : 'warning'" class="mt-0.5" style="font-size:10px;height:18px;line-height:18px;padding:0 4px">
                        {{ site.method === 'search' ? 'Firecrawl /search' : '页面提取' }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="text-xs text-slate-500 leading-relaxed line-clamp-2">{{ site.description }}</div>
                  <div class="text-xs text-slate-400 mt-2 truncate font-mono">{{ site.domain }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-divider />
        <el-form-item label="采集模式">
          <div class="flex items-center gap-4">
            <el-switch v-model="batchMode" active-text="批量" inactive-text="单关键词" />
            <span class="text-xs text-slate-400">{{ batchMode ? '输入多个关键词，系统为每个关键词创建独立任务' : '所有选中站点使用同一个关键词' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="任务标签">
          <el-select v-model="selectedTaskTagIds" multiple collapse-tags collapse-tags-tooltip clearable placeholder="给本次任务预设标签" style="width: 100%">
            <el-option
              v-for="tag in resultTags"
              :key="tag.tagId"
              :label="tag.categoryName ? `${tag.categoryName} / ${tag.tagName}` : tag.tagName"
              :value="tag.tagId!"
            />
          </el-select>
        </el-form-item>
        <template v-if="batchMode">
          <el-form-item label="批次名称"><el-input v-model="batchName" placeholder="例如：4月热点人物批次" /></el-form-item>
          <el-form-item label="批次备注"><el-input v-model="batchNotes" placeholder="可选" /></el-form-item>
          <el-form-item label="批量关键词">
            <el-input v-model="batchKeywordsText" type="textarea" :rows="6" placeholder="每行一个关键词，或用逗号分隔" />
          </el-form-item>
        </template>
        <el-form-item v-else label="关键词">
          <el-input v-model="commonKeyword" maxlength="80" show-word-limit placeholder="输入统一关键词" />
        </el-form-item>
      </el-form>
    </el-card>

    <section class="grid grid-cols-1 xl:grid-cols-[1.2fr_1fr] gap-6">
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">任务概览</span>
            <div class="flex items-center gap-3">
              <el-input v-model="keywordFilter" size="small" placeholder="搜索任务/关键词/URL" style="width: 220px" />
              <el-select v-model="statusFilter" size="small" style="width: 160px">
                <el-option label="全部状态" value="ALL" />
                <el-option label="排队中" value="PENDING" />
                <el-option label="执行中" value="RUNNING" />
                <el-option label="已完成" value="FINISHED" />
                <el-option label="部分失败" value="PARTIAL_FAILED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
              <el-button size="small" type="success" :loading="exporting" @click="exportCurrentResults">导出结果</el-button>
              <el-button size="small" plain type="warning" :disabled="!activeTaskId" @click="activeTaskDetail && toggleTaskArchive(activeTaskDetail.task)">
                {{ activeTaskDetail?.task.archived ? '取消归档' : '归档任务' }}
              </el-button>
              <el-button text type="primary" :loading="loading" @click="loadTaskData(activeTaskId ?? undefined)">刷新</el-button>
            </div>
          </div>
        </template>
        <el-table :data="taskRows" border stripe highlight-current-row @current-change="(row: { taskId?: number } | undefined) => activeTaskId = row?.taskId ?? null">
          <el-table-column prop="taskId" label="任务ID" width="100" />
          <el-table-column prop="siteLabel" label="站点" width="120" />
          <el-table-column prop="keyword" label="关键词" min-width="140" />
          <el-table-column label="状态" width="120">
            <template #default="scope"><el-tag :type="statusTagType(scope.row.runtime?.status ?? scope.row.taskStatus)">{{ statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="进度" min-width="230">
            <template #default="scope">
              <el-progress :percentage="scope.row.runtime?.progressPercent ?? scope.row.taskProgress" :stroke-width="10" />
              <div class="text-xs text-gray-500 mt-1">
                {{ scope.row.runtime?.completedPages ?? 0 }}/{{ scope.row.runtime?.expectedPages ?? scope.row.totalPages ?? 0 }}
                成功 {{ scope.row.runtime?.successPages ?? 0 }} / 失败 {{ scope.row.runtime?.failedPages ?? 0 }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="节点" min-width="160">
            <template #default="scope">{{ scope.row.runtime?.assignedNodeId || scope.row.nodeId || '未分配' }}</template>
          </el-table-column>
          <el-table-column label="归档" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.archived ? 'info' : 'success'">{{ scope.row.archived ? '已归档' : '活跃' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="activeTaskDetail">
        <template #header><span class="font-semibold">任务详情 #{{ activeTaskDetail.task.taskId }}</span></template>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div><span class="text-gray-500">关键词:</span> {{ activeTaskDetail.task.keyword }}</div>
            <div><span class="text-gray-500">站点:</span> {{ siteLabel(detectSite(activeTaskDetail.task.url)) }}</div>
            <div><span class="text-gray-500">状态:</span> {{ statusText(activeTaskDetail.runtime?.status ?? activeTaskDetail.task.taskStatus) }}</div>
            <div><span class="text-gray-500">节点:</span> {{ activeTaskDetail.runtime?.assignedNodeId || activeTaskDetail.task.nodeId || '未分配' }}</div>
            <div><span class="text-gray-500">创建时间:</span> {{ activeTaskDetail.task.createdAt || '-' }}</div>
            <div><span class="text-gray-500">开始时间:</span> {{ activeTaskDetail.runtime?.startedAt || '-' }}</div>
            <div><span class="text-gray-500">结束时间:</span> {{ activeTaskDetail.runtime?.finishedAt || '-' }}</div>
            <div><span class="text-gray-500">文件数:</span> {{ activeTaskDetail.files.length }}</div>
            <div><span class="text-gray-500">归档状态:</span> {{ activeTaskDetail.task.archived ? '已归档' : '未归档' }}</div>
            <div><span class="text-gray-500">归档时间:</span> {{ activeTaskDetail.task.archivedAt || '-' }}</div>
            <div class="col-span-2"><span class="text-gray-500">最后错误:</span> {{ activeTaskDetail.runtime?.lastErrorMessage || activeTaskDetail.task.lastErrorMessage || '-' }}</div>
          </div>

          <el-divider>任务整理</el-divider>
          <div class="space-y-4">
            <div class="flex items-center gap-3 flex-wrap">
              <el-select v-model="selectedGroupId" clearable placeholder="选择任务分组" style="width: 220px">
                <el-option v-for="group in taskGroups" :key="group.groupId" :label="group.groupName" :value="group.groupId!" />
              </el-select>
              <el-button type="primary" plain @click="bindCurrentTaskGroup">加入分组</el-button>
              <el-button plain @click="taskGroupDialogVisible = true">新建分组</el-button>
            </div>
            <div class="flex flex-wrap gap-2">
              <el-tag
                v-for="group in activeTaskGroups"
                :key="group.groupId"
                effect="dark"
                :color="group.groupColor || undefined"
              >
                {{ group.groupName }}
              </el-tag>
              <span v-if="!activeTaskGroups.length" class="text-sm text-slate-500">当前任务尚未加入分组</span>
            </div>
            <el-input v-model="noteDraft" type="textarea" :rows="3" placeholder="补充任务备注、异常说明、结果整理结论" />
            <div class="flex items-center justify-between gap-3 flex-wrap">
              <div class="text-sm text-slate-500">备注用于记录任务异常、结果整理结论和后续说明。</div>
              <div class="flex items-center gap-2">
                <el-button v-if="editingNoteId" @click="editingNoteId = null; noteDraft = ''">取消编辑</el-button>
                <el-button type="primary" @click="submitTaskNote">{{ editingNoteId ? '保存修改' : '添加备注' }}</el-button>
              </div>
            </div>
            <div class="space-y-2 max-h-48 overflow-auto">
              <div v-for="note in activeTaskNotes" :key="note.noteId" class="rounded-lg border border-slate-200 p-3">
                <div class="text-sm text-slate-700">{{ note.noteContent }}</div>
                <div class="mt-2 flex items-center justify-between gap-3">
                  <div class="text-xs text-slate-400">{{ note.updatedAt || note.createdAt || '-' }}</div>
                  <div class="flex items-center gap-2">
                    <el-button size="small" text type="primary" @click="editTaskNote(note)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="removeTaskNote(note)">删除</el-button>
                  </div>
                </div>
              </div>
              <span v-if="!activeTaskNotes.length" class="text-sm text-slate-500">还没有任务备注</span>
            </div>
          </div>

          <el-divider>页面结果</el-divider>
          <div class="max-h-72 overflow-auto space-y-3">
            <div v-for="page in activeTaskDetail.pageResults" :key="page.pageResultId || `${page.taskId}-${page.pageIndex}`" class="rounded-lg border border-slate-200 p-3">
              <div class="flex items-center justify-between gap-3">
                <div class="font-medium">{{ page.pageTitle || `页面 ${page.pageIndex}` }}</div>
                <el-tag :type="page.success ? 'success' : 'danger'" size="small">{{ page.success ? 'SUCCESS' : 'FAILED' }}</el-tag>
              </div>
              <div class="text-xs text-gray-500 break-all mt-1">{{ page.pageUrl }}</div>
              <div class="text-xs text-gray-500 mt-2">
                siteType={{ page.siteType || '-' }} · mime={{ page.mimeType || '-' }} · size={{ page.fileSizeBytes ?? '-' }}
              </div>
              <div class="text-xs text-red-500 mt-1" v-if="page.errorCode || page.errorMessage">{{ page.errorCode || '-' }} {{ page.errorMessage || '' }}</div>
              <div class="flex items-center gap-2 mt-2">
                <el-button size="small" plain type="warning" :disabled="!page.pageResultId" @click="toggleBookmark(page.pageResultId)">
                  {{ page.pageResultId && bookmarkIds.has(page.pageResultId) ? '取消收藏' : '收藏结果' }}
                </el-button>
                <el-button size="small" plain type="primary" :disabled="!page.pageResultId || !page.filePath || Boolean(page.mhtmlCached)" :loading="Boolean(cacheLoadingMap[page.pageResultId || 0])" @click="cacheMhtml(page.pageResultId)">
                  {{ page.mhtmlCached ? '已缓存' : '缓存MHTML' }}
                </el-button>
                <el-button size="small" plain type="success" :disabled="!page.pageResultId || (!page.filePath && !page.mhtmlCached)" :loading="Boolean(downloadLoadingMap[page.pageResultId || 0])" @click="downloadMhtml(page.pageResultId)">
                  下载MHTML
                </el-button>
              </div>
            </div>
          </div>

          <el-divider>事件时间线</el-divider>
          <el-timeline>
            <el-timeline-item v-for="event in activeTaskDetail.events" :key="event.eventId" :timestamp="event.createdAt || ''" :type="event.eventLevel === 'ERROR' ? 'danger' : event.eventLevel === 'WARN' ? 'warning' : 'primary'">
              <div class="font-medium">{{ event.eventType }}</div>
              <div class="text-sm text-gray-600 mt-1">{{ event.eventMessage }}</div>
              <div class="text-xs text-gray-400 mt-1">node={{ event.nodeId || '-' }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>
    </section>

    <el-dialog v-model="taskGroupDialogVisible" title="新建任务分组" width="520px">
      <el-form label-width="90px">
        <el-form-item label="分组名称">
          <el-input v-model="groupForm.groupName" maxlength="40" />
        </el-form-item>
        <el-form-item label="分组颜色">
          <el-color-picker v-model="groupForm.groupColor" />
        </el-form-item>
        <el-form-item label="分组说明">
          <el-input v-model="groupForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="taskGroupDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTaskGroup">创建</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
