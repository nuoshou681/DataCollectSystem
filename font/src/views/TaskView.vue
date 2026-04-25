<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  cachePageResultMhtml,
  dispatchTask,
  downloadPageResultMhtml,
  fetchTaskDetail,
  fetchTasks,
  getPageResultStreamUrl,
  getTaskRuntimeStreamUrl,
} from '@/api/api'
import type { CrawlerPageResult, DispatchTaskPayload, Task, TaskDetail, TaskEvent, TaskRuntime } from '@/types/entity'
import { getCurrentUserProfile } from '@/utils/auth'
import { detectSite, siteLabel, statusTagType, statusText } from '@/utils/task'

type SiteKey = 'sohu' | 'bing' | 'baike'
type StatusFilter = 'ALL' | 'PENDING' | 'RUNNING' | 'FINISHED' | 'PARTIAL_FAILED' | 'FAILED'

interface SiteOption {
  key: SiteKey
  label: string
  seedUrl: string
  siteType: string
}

const siteOptions: SiteOption[] = [
  { key: 'sohu', label: '搜狐新闻', seedUrl: 'https://search.sohu.com/?keyword=', siteType: 'SOHU' },
  { key: 'bing', label: 'Bing', seedUrl: 'https://www.bing.com/search?q=', siteType: 'BING' },
  { key: 'baike', label: '百度百科', seedUrl: 'https://baike.baidu.com/item/', siteType: 'BAIDU_BAIKE' },
]

const selectedSites = ref<SiteOption['key'][]>(['sohu'])
const usePerSiteKeyword = ref(false)
const commonKeyword = ref('')
const siteKeywords = ref<Record<SiteOption['key'], string>>({ sohu: '', bing: '', baike: '' })
const keywordFilter = ref('')
const statusFilter = ref<StatusFilter>('ALL')
const loading = ref(false)
const submitting = ref(false)
const tasks = ref<Task[]>([])
const detailMap = ref<Record<number, TaskDetail | undefined>>({})
const activeTaskId = ref<number | null>(null)
const streamConnected = ref(false)
const runtimeStreamConnected = ref(false)
const streamStatusText = ref('正在连接实时结果流...')
const runtimeStreamText = ref('正在连接运行态流...')
let resultStream: EventSource | null = null
let runtimeStream: EventSource | null = null

const cacheLoadingMap = ref<Record<number, boolean>>({})
const downloadLoadingMap = ref<Record<number, boolean>>({})
const siteOptionMap = new Map(siteOptions.map(option => [option.key, option]))

async function loadTaskData(taskIdToOpen?: number) {
  loading.value = true
  try {
    const taskData = await fetchTasks()
    tasks.value = taskData

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

function createPayload(option: SiteOption, keyword: string): DispatchTaskPayload {
  const profile = getCurrentUserProfile()
  return {
    userId: profile.userId ?? null,
    keyword,
    url: option.seedUrl,
    source: 'manual',
    siteType: option.siteType,
    maxLinksPerLevel: 10,
  }
}

async function submitTask() {
  if (selectedSites.value.length === 0) {
    ElMessage.error('至少选择一个站点')
    return
  }

  if (!usePerSiteKeyword.value && !commonKeyword.value.trim()) {
    ElMessage.error('请填写关键词')
    return
  }

  submitting.value = true
  try {
    const requests = selectedSites.value.map(site => {
      const option = siteOptionMap.get(site)
      if (!option) {
        return Promise.resolve(null)
      }
      const keyword = usePerSiteKeyword.value ? siteKeywords.value[site].trim() : commonKeyword.value.trim()
      if (!keyword) {
        throw new Error(`${option.label} 的关键词不能为空`)
      }
      return dispatchTask(createPayload(option, keyword))
    })
    await Promise.all(requests)
    ElMessage.success('任务创建成功')
    await loadTaskData()
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
  if (resultStream) {
    resultStream.close()
  }
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
  if (runtimeStream) {
    runtimeStream.close()
  }
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

onMounted(() => {
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
          </div>
        </div>
      </template>
      <el-form label-width="130px" class="max-w-4xl">
        <el-form-item label="选择站点">
          <el-checkbox-group v-model="selectedSites">
            <el-checkbox v-for="site in siteOptions" :key="site.key" :label="site.key">{{ site.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="分别指定关键词"><el-switch v-model="usePerSiteKeyword" /></el-form-item>
        <el-form-item v-if="!usePerSiteKeyword" label="关键词">
          <el-input v-model="commonKeyword" maxlength="80" show-word-limit placeholder="输入统一关键词" />
        </el-form-item>
        <template v-else>
          <el-form-item v-for="site in siteOptions" :key="site.key" :label="`${site.label}关键词`">
            <el-input v-model="siteKeywords[site.key]" :disabled="!selectedSites.includes(site.key)" maxlength="80" show-word-limit />
          </el-form-item>
        </template>
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
            <div class="col-span-2"><span class="text-gray-500">最后错误:</span> {{ activeTaskDetail.runtime?.lastErrorMessage || activeTaskDetail.task.lastErrorMessage || '-' }}</div>
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
  </div>
</template>
