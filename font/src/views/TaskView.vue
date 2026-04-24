<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  cachePageResultMhtml,
  dispatchTask,
  downloadPageResultMhtml,
  fetchPageResults,
  getPageResultStreamUrl,
  fetchTasks,
} from '@/api/api'
import type { CrawlerPageResult, DispatchTaskPayload, Task } from '@/types/entity'

type SiteKey = 'sohu' | 'bing' | 'baike' | 'other'

interface SiteOption {
  key: Exclude<SiteKey, 'other'>
  label: string
  seedUrl: string
}

interface LinkRow {
  pageResultId?: number
  pageUrl: string
  pageTitle?: string
  pageIndex: number
  success: boolean
  filePath?: string
  errorMessage?: string
  mhtmlCached?: boolean
  mhtmlCachedAt?: string
  isSeed?: boolean
}

interface SiteTaskRow {
  taskId: number
  keyword: string
  nodeId: string
  taskStatus: string
  taskProgress: number
  completedPages: number
  expectedPages: number
  links: LinkRow[]
}

const siteOptions: SiteOption[] = [
  {
    key: 'sohu',
    label: '搜狐新闻',
    seedUrl: 'https://search.sohu.com/?keyword=',
  },
  {
    key: 'bing',
    label: 'Bing',
    seedUrl: 'https://www.bing.com/search?q=',
  },
  {
    key: 'baike',
    label: '百度百科',
    seedUrl: 'https://baike.baidu.com/item/',
  },
]

const EXPECTED_PAGE_RESULTS_PER_TASK = 10

const route = useRoute()
const router = useRouter()

const selectedSites = ref<SiteOption['key'][]>(['sohu'])
const usePerSiteKeyword = ref(false)
const commonKeyword = ref('')
const siteKeywords = ref<Record<SiteOption['key'], string>>({
  sohu: '',
  bing: '',
  baike: '',
})

const loading = ref(false)
const submitting = ref(false)
const tasks = ref<Task[]>([])
const pageResults = ref<CrawlerPageResult[]>([])
const activeSitePanels = ref<string[]>(['sohu'])
const streamConnected = ref(false)
const streamStatusText = ref('正在连接实时结果流...')
let resultStream: EventSource | null = null

const cacheLoadingMap = ref<Record<number, boolean>>({})
const downloadLoadingMap = ref<Record<number, boolean>>({})

const siteOptionMap = new Map(siteOptions.map(option => [option.key, option]))

function detectSite(url: string): SiteKey {
  if (url.includes('search.sohu.com')) {
    return 'sohu'
  }
  if (url.includes('www.bing.com')) {
    return 'bing'
  }
  if (url.includes('baike.baidu.com')) {
    return 'baike'
  }
  return 'other'
}

function siteLabel(key: SiteKey) {
  if (key === 'other') {
    return '其他站点'
  }
  return siteOptionMap.get(key)?.label ?? key
}

function statusTagType(status?: string): 'success' | 'danger' | 'warning' | 'info' {
  if (!status) {
    return 'info'
  }
  if (status === 'FINISHED') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  if (status === 'PARTIAL_FAILED') {
    return 'warning'
  }
  if (status === 'RUNNING') {
    return 'warning'
  }
  if (status === 'PENDING') {
    return 'info'
  }
  return 'info'
}

function statusText(status?: string) {
  if (!status) {
    return '排队中'
  }
  if (status === 'FINISHED') {
    return '已完成'
  }
  if (status === 'FAILED') {
    return '失败'
  }
  if (status === 'PARTIAL_FAILED') {
    return '部分失败'
  }
  if (status === 'RUNNING') {
    return '执行中'
  }
  if (status === 'PENDING') {
    return '排队中'
  }
  return status
}

function buildSeedLink(url: string, keyword: string) {
  return `${url}${encodeURIComponent(keyword)}`
}

async function loadTaskData(silent = false) {
  if (!silent) {
    loading.value = true
  }
  try {
    const [taskRes, pageResultRes] = await Promise.all([
      fetchTasks(),
      fetchPageResults(),
    ])

    tasks.value = taskRes
    pageResults.value = pageResultRes
  } catch {
    ElMessage.error('任务数据加载失败')
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

function upsertPageResult(incoming: CrawlerPageResult) {
  const index = pageResults.value.findIndex(item => {
    if (incoming.pageResultId && item.pageResultId) {
      return item.pageResultId === incoming.pageResultId
    }
    return (
      item.taskId === incoming.taskId
      && item.pageIndex === incoming.pageIndex
      && item.pageUrl === incoming.pageUrl
    )
  })

  if (index >= 0) {
    pageResults.value[index] = {
      ...pageResults.value[index],
      ...incoming,
    }
    return
  }

  pageResults.value.push(incoming)
}

function parsePageResultEvent(raw: string) {
  try {
    const parsed = JSON.parse(raw) as CrawlerPageResult
    if (!parsed || parsed.taskId === undefined || parsed.pageUrl === undefined || parsed.pageIndex === undefined) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function connectPageResultStream() {
  if (resultStream) {
    resultStream.close()
    resultStream = null
  }

  const streamUrl = getPageResultStreamUrl()
  resultStream = new EventSource(streamUrl)

  resultStream.onopen = () => {
    streamConnected.value = true
    streamStatusText.value = '实时结果流已连接'
  }

  resultStream.addEventListener('crawler-page-result', event => {
    const payload = parsePageResultEvent((event as MessageEvent).data)
    if (!payload) {
      return
    }
    upsertPageResult(payload)
  })

  resultStream.onerror = () => {
    streamConnected.value = false
    streamStatusText.value = '实时结果流重连中...'
  }
}

function createPayload(keyword: string, urls: string[]): DispatchTaskPayload {
  return {
    userId: 1,
    keyword,
    url: urls.join('\n'),
    status: 'PENDING',
    progress: 0,
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

  if (usePerSiteKeyword.value) {
    const emptySite = selectedSites.value.find(site => !siteKeywords.value[site].trim())
    if (emptySite) {
      ElMessage.error(`${siteLabel(emptySite)} 的关键词不能为空`)
      return
    }
  }

  submitting.value = true
  try {
    if (usePerSiteKeyword.value) {
      const requests = selectedSites.value.map(site => {
        const option = siteOptionMap.get(site)
        if (!option) {
          return Promise.resolve(null)
        }
        return dispatchTask(createPayload(siteKeywords.value[site].trim(), [option.seedUrl]))
      })
      await Promise.all(requests)
    } else {
      const urls = selectedSites.value
        .map(site => siteOptionMap.get(site)?.seedUrl)
        .filter((url): url is string => Boolean(url))
      await dispatchTask(createPayload(commonKeyword.value.trim(), urls))
    }

    ElMessage.success('任务创建成功，已进入调度队列')
    await loadTaskData()
  } catch {
    ElMessage.error('任务创建失败')
  } finally {
    submitting.value = false
  }
}

function isCaching(pageResultId?: number) {
  if (!pageResultId) {
    return false
  }
  return Boolean(cacheLoadingMap.value[pageResultId])
}

async function cacheLinkMhtml(link: LinkRow) {
  if (!link.pageResultId) {
    ElMessage.warning('该链接没有可缓存的页面结果记录')
    return
  }
  if (!link.filePath) {
    ElMessage.warning('该链接没有可用的 MHTML 文件路径')
    return
  }

  cacheLoadingMap.value[link.pageResultId] = true
  try {
    const result = await cachePageResultMhtml(link.pageResultId)
    if (result) {
      ElMessage.success('MHTML 已缓存到数据库')
      upsertPageResult(result)
      return
    }
    ElMessage.error('MHTML 缓存失败')
  } catch {
    ElMessage.error('MHTML 缓存失败')
  } finally {
    delete cacheLoadingMap.value[link.pageResultId]
  }
}

function normalizeNodeId(value?: string | number | null) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

function resolveNodeId(task: Task, pageList: CrawlerPageResult[]) {
  const taskNodeId = normalizeNodeId(task.nodeId ?? task.node_id)
  if (taskNodeId && taskNodeId !== '-1') {
    return taskNodeId
  }

  const pageNodeId = pageList
    .map(page => normalizeNodeId(page.nodeId ?? page.node_id))
    .find(nodeId => nodeId && nodeId !== '-1')
  if (pageNodeId) {
    return pageNodeId
  }

  return 'node_id'
}

function resolveExpectedPages(pageList: CrawlerPageResult[]) {
  const candidates = pageList
    .map(page => page.totalPages ?? 0)
    .filter(value => value && value > 0)
  if (candidates.length > 0) {
    return Math.max(...candidates)
  }
  return EXPECTED_PAGE_RESULTS_PER_TASK
}

function resolveTaskStatus(task: Task, runtimeStatus: string) {
  if (task.taskStatus) {
    return task.taskStatus
  }
  return runtimeStatus
}

function resolveTaskProgress(task: Task, runtimeProgress: number) {
  if (typeof task.taskProgress === 'number') {
    return task.taskProgress
  }
  return runtimeProgress
}

function calculateTaskRuntime(pageList: CrawlerPageResult[], expectedPages: number) {
  const completedPages = pageList.length
  const totalPages = expectedPages > 0 ? expectedPages : EXPECTED_PAGE_RESULTS_PER_TASK
  const pageBasedProgress = Math.min(
    100,
    Math.round((completedPages / totalPages) * 100),
  )
  const failedPages = pageList.filter(page => !page.success).length

  if (completedPages === 0) {
    return {
      status: 'PENDING',
      progress: 0,
      completedPages,
      expectedPages: totalPages,
    }
  }

  if (completedPages < totalPages) {
    return {
      status: 'RUNNING',
      progress: pageBasedProgress,
      completedPages,
      expectedPages: totalPages,
    }
  }

  if (failedPages === 0) {
    return {
      status: 'FINISHED',
      progress: 100,
      completedPages,
      expectedPages: totalPages,
    }
  }

  if (failedPages >= completedPages) {
    return {
      status: 'FAILED',
      progress: 100,
      completedPages,
      expectedPages: totalPages,
    }
  }

  return {
    status: 'PARTIAL_FAILED',
    progress: 100,
    completedPages,
    expectedPages: totalPages,
  }
}

function isDownloading(pageResultId?: number) {
  if (!pageResultId) {
    return false
  }
  return Boolean(downloadLoadingMap.value[pageResultId])
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

async function downloadLinkMhtml(link: LinkRow) {
  if (!link.pageResultId) {
    ElMessage.warning('该链接没有可下载的页面结果记录')
    return
  }

  downloadLoadingMap.value[link.pageResultId] = true
  try {
    const result = await downloadPageResultMhtml(link.pageResultId)
    if (!result?.blob) {
      ElMessage.error('下载失败')
      return
    }

    triggerBrowserDownload(result.blob, result.fileName)
    ElMessage.success('已开始下载 MHTML 文件')
  } catch {
    ElMessage.error('下载失败')
  } finally {
    delete downloadLoadingMap.value[link.pageResultId]
  }
}

const taskStats = computed(() => {
  const total = filteredTasks.value.length
  let running = 0
  let pending = 0
  let finished = 0
  let failed = 0

  const pageMap = new Map<number, CrawlerPageResult[]>()
  for (const pageResult of filteredPageResults.value) {
    if (!pageMap.has(pageResult.taskId)) {
      pageMap.set(pageResult.taskId, [])
    }
    pageMap.get(pageResult.taskId)?.push(pageResult)
  }

  for (const task of filteredTasks.value) {
    const pageList = pageMap.get(task.taskId) ?? []
    const expectedPages = resolveExpectedPages(pageList)
    const runtime = calculateTaskRuntime(pageList, expectedPages)
    const status = resolveTaskStatus(task, runtime.status)
    if (status === 'FINISHED') {
      finished += 1
      continue
    }
    if (status === 'FAILED' || status === 'PARTIAL_FAILED') {
      failed += 1
      continue
    }
    if (status === 'PENDING') {
      pending += 1
      continue
    }
    if (status === 'RUNNING') {
      running += 1
    }
  }

  return { total, pending, running, finished, failed }
})

const filterKeyword = computed(() => {
  const queryValue = route.query.q
  if (typeof queryValue !== 'string') {
    return ''
  }
  return queryValue.trim().toLowerCase()
})

const filteredTasks = computed(() => {
  if (!filterKeyword.value) {
    return tasks.value
  }
  return tasks.value.filter((task) => {
    return (
      String(task.taskId).includes(filterKeyword.value) ||
      task.keyword?.toLowerCase().includes(filterKeyword.value) ||
      task.url?.toLowerCase().includes(filterKeyword.value)
    )
  })
})

const filteredPageResults = computed(() => {
  if (!filterKeyword.value) {
    return pageResults.value
  }
  return pageResults.value.filter((page) => {
    return (
      String(page.taskId).includes(filterKeyword.value) ||
      String(page.pageResultId ?? '').includes(filterKeyword.value) ||
      page.pageUrl?.toLowerCase().includes(filterKeyword.value) ||
      page.pageTitle?.toLowerCase().includes(filterKeyword.value)
    )
  })
})

function clearFilter() {
  router.replace({ path: '/tasks' })
}

const groupedSiteTasks = computed(() => {
  const pageMap = new Map<number, CrawlerPageResult[]>()

  for (const pageResult of filteredPageResults.value) {
    if (!pageMap.has(pageResult.taskId)) {
      pageMap.set(pageResult.taskId, [])
    }
    pageMap.get(pageResult.taskId)?.push(pageResult)
  }

  for (const pageList of pageMap.values()) {
    pageList.sort((left, right) => left.pageIndex - right.pageIndex)
  }

  const grouped = new Map<SiteKey, SiteTaskRow[]>()

  for (const task of filteredTasks.value) {
    const site = detectSite(task.url)
    const keyword = task.keyword ?? ''

    const pageList = pageMap.get(task.taskId) ?? []
    const expectedPages = resolveExpectedPages(pageList)
    const runtime = calculateTaskRuntime(pageList, expectedPages)
    const status = resolveTaskStatus(task, runtime.status)
    const progress = resolveTaskProgress(task, runtime.progress)
    const expected = typeof task.totalPages === 'number' ? task.totalPages : runtime.expectedPages
    const links: LinkRow[] =
      pageList.length > 0
        ? pageList.map(page => ({
          pageResultId: page.pageResultId,
          pageUrl: page.pageUrl,
          pageTitle: page.pageTitle,
          pageIndex: page.pageIndex,
          success: page.success,
          filePath: page.filePath,
          errorMessage: page.errorMessage,
          mhtmlCached: page.mhtmlCached,
          mhtmlCachedAt: page.mhtmlCachedAt,
          isSeed: false,
        }))
        : [
          {
            pageUrl: buildSeedLink(task.url, keyword),
            pageTitle: '种子链接',
            pageIndex: 0,
            success: true,
            isSeed: true,
          },
        ]

    const row: SiteTaskRow = {
      taskId: task.taskId,
      keyword,
      nodeId: resolveNodeId(task, pageList),
      taskStatus: status,
      taskProgress: progress,
      completedPages: runtime.completedPages,
      expectedPages: expected,
      links,
    }

    if (!grouped.has(site)) {
      grouped.set(site, [])
    }
    grouped.get(site)?.push(row)
  }

  const orderedKeys: SiteKey[] = ['sohu', 'bing', 'baike', 'other']
  return orderedKeys.map(key => ({
    key,
    label: siteLabel(key),
    rows: grouped.get(key) ?? [],
  }))
})

onMounted(() => {
  loadTaskData()
  connectPageResultStream()
})

onBeforeUnmount(() => {
  if (resultStream) {
    resultStream.close()
    resultStream = null
  }
})
</script>

<template>
  <div class="p-6 space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card>
        <div class="text-sm text-gray-500">任务总数</div>
        <div class="text-2xl font-semibold mt-2">{{ taskStats.total }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">待开始</div>
        <div class="text-2xl font-semibold mt-2 text-slate-600">{{ taskStats.pending }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">执行中</div>
        <div class="text-2xl font-semibold mt-2 text-blue-600">{{ taskStats.running }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">完成/异常</div>
        <div class="text-2xl font-semibold mt-2 text-emerald-600">{{ taskStats.finished }}</div>
        <div class="text-xs text-red-500 mt-1">异常 {{ taskStats.failed }}</div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">创建用户任务</span>
          <el-button type="primary" :loading="submitting" @click="submitTask">提交任务</el-button>
        </div>
      </template>

      <el-form label-width="130px" class="max-w-4xl">
        <el-form-item label="选择爬取网站">
          <el-checkbox-group v-model="selectedSites">
            <el-checkbox v-for="site in siteOptions" :key="site.key" :label="site.key">
              {{ site.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="分别指定关键词">
          <el-switch v-model="usePerSiteKeyword" />
        </el-form-item>

        <el-form-item v-if="!usePerSiteKeyword" label="关键词">
          <el-input v-model="commonKeyword" maxlength="80" show-word-limit placeholder="输入一个统一关键词" />
        </el-form-item>

        <template v-else>
          <el-form-item v-for="site in siteOptions" :key="site.key" :label="`${site.label}关键词`">
            <el-input v-model="siteKeywords[site.key]" :disabled="!selectedSites.includes(site.key)" maxlength="80"
              show-word-limit :placeholder="`输入 ${site.label} 的关键词`" />
          </el-form-item>
        </template>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">按站点查看任务与网页链接</span>
          <div class="flex items-center gap-3">
            <el-tag v-if="filterKeyword" size="small" type="info">
              当前过滤: {{ filterKeyword }}
            </el-tag>
            <el-button v-if="filterKeyword" text type="primary" @click="clearFilter">
              清除过滤
            </el-button>
            <el-tag :type="streamConnected ? 'success' : 'warning'" size="small">
              {{ streamStatusText }}
            </el-tag>
            <el-button text type="primary" :loading="loading" @click="loadTaskData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <el-empty v-if="groupedSiteTasks.every(group => group.rows.length === 0)" description="还没有任务，先在上方表单创建一个任务" />

          <el-collapse v-else v-model="activeSitePanels">
            <el-collapse-item v-for="group in groupedSiteTasks" :key="group.key" :name="group.key"
              :disabled="group.rows.length === 0">
              <template #title>
                <div class="flex items-center gap-2">
                  <span>{{ group.label }}</span>
                  <el-tag size="small" type="info">{{ group.rows.length }} 个任务</el-tag>
                </div>
              </template>

              <el-table :data="group.rows" border stripe>
                <el-table-column type="expand">
                  <template #default="scope">
                    <div class="px-4 py-2">
                      <el-timeline>
                        <el-timeline-item v-for="link in scope.row.links"
                          :key="`${scope.row.taskId}-${link.pageIndex}-${link.pageUrl}`"
                          :type="link.success ? 'success' : 'danger'" :timestamp="`序号 ${link.pageIndex}`">
                          <div class="font-medium">{{ link.pageTitle || '网页链接' }}</div>
                          <a class="text-blue-600 break-all" :href="link.pageUrl" target="_blank" rel="noopener">
                            {{ link.pageUrl }}
                          </a>
                          <div v-if="link.filePath" class="text-xs text-gray-500 break-all mt-1">
                            MHTML路径: {{ link.filePath }}
                          </div>
                          <div class="flex items-center gap-2 mt-2">
                            <el-tag v-if="link.mhtmlCached" type="success" size="small">已缓存MHTML</el-tag>
                            <el-tag v-else-if="link.filePath" type="info" size="small">可缓存MHTML</el-tag>
                            <el-button
                              size="small"
                              type="primary"
                              plain
                              :disabled="!link.pageResultId || !link.filePath || Boolean(link.mhtmlCached)"
                              :loading="isCaching(link.pageResultId)"
                              @click="cacheLinkMhtml(link)"
                            >
                              {{ link.mhtmlCached ? '已缓存' : '缓存MHTML' }}
                            </el-button>
                            <el-button
                              size="small"
                              type="success"
                              plain
                              :disabled="!link.pageResultId || (!link.filePath && !link.mhtmlCached)"
                              :loading="isDownloading(link.pageResultId)"
                              @click="downloadLinkMhtml(link)"
                            >
                              下载 MHTML 文件
                            </el-button>
                          </div>
                          <div v-if="link.errorMessage" class="text-red-500 mt-1">{{ link.errorMessage }}</div>
                        </el-timeline-item>
                      </el-timeline>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="taskId" label="任务ID" width="110" />
                <el-table-column prop="keyword" label="关键词" min-width="160" />
                <el-table-column label="任务状态" min-width="160">
                  <template #default="scope">
                    <el-tag :type="statusTagType(scope.row.taskStatus)" class="status-tag">
                      {{ statusText(scope.row.taskStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="进度" width="230">
                  <template #default="scope">
                    <div class="w-full">
                      <el-progress :percentage="scope.row.taskProgress" :stroke-width="10" />
                      <div class="text-xs text-gray-500 mt-1">
                        已完成 {{ scope.row.completedPages }}/{{ scope.row.expectedPages }}
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="nodeId" label="节点ID" min-width="140" />
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </template>
      </el-skeleton>
    </el-card>
  </div>
</template>

<style scoped>
.status-tag {
  max-width: 140px;
  white-space: normal;
}
</style>
