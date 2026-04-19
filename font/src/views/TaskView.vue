<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dispatchTask, fetchPageResults, fetchSubTasks, fetchTasks } from '@/api/api'
import type { CrawlerPageResult, DispatchTaskPayload, SubTask, Task } from '@/types/entity'

type SiteKey = 'sohu' | 'bing' | 'baike' | 'other'

interface SiteOption {
  key: Exclude<SiteKey, 'other'>
  label: string
  seedUrl: string
}

interface LinkRow {
  pageUrl: string
  pageTitle?: string
  pageIndex: number
  success: boolean
  errorMessage?: string
}

interface SiteTaskRow {
  subtaskId: number
  taskId: number
  keyword: string
  nodeId: string
  taskStatus: string
  taskProgress: number
  links: LinkRow[]
}

const siteOptions: SiteOption[] = [
  {
    key: 'sohu',
    label: '搜狐新闻',
    seedUrl: 'https://search.sohu.com/search?query=',
  },
  {
    key: 'bing',
    label: 'Bing',
    seedUrl: 'https://www.bing.com/search?q=',
  },
  {
    key: 'baike',
    label: '百度百科',
    seedUrl: 'https://baike.baidu.com/search?word=',
  },
]

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
const subTasks = ref<SubTask[]>([])
const pageResults = ref<CrawlerPageResult[]>([])
const activeSitePanels = ref<string[]>(['sohu'])

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
  if (status === 'FAILED' || status === 'PARTIAL_FAILED') {
    return 'danger'
  }
  if (status === 'RUNNING' || status === 'PENDING') {
    return 'warning'
  }
  return 'info'
}

function statusText(status?: string) {
  if (!status) {
    return 'PENDING'
  }
  return status
}

function buildSeedLink(url: string, keyword: string) {
  return `${url}${encodeURIComponent(keyword)}`
}

async function loadTaskData() {
  loading.value = true
  try {
    const [taskRes, subTaskRes, pageResultRes] = await Promise.all([
      fetchTasks(),
      fetchSubTasks(),
      fetchPageResults(),
    ])

    tasks.value = taskRes
    subTasks.value = subTaskRes
    pageResults.value = pageResultRes
  } catch {
    ElMessage.error('任务数据加载失败')
  } finally {
    loading.value = false
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

const taskStats = computed(() => {
  const total = tasks.value.length
  let running = 0
  let finished = 0
  let failed = 0

  for (const task of tasks.value) {
    if (task.status === 'FINISHED') {
      finished += 1
      continue
    }
    if (task.status === 'FAILED' || task.status === 'PARTIAL_FAILED') {
      failed += 1
      continue
    }
    running += 1
  }

  return { total, running, finished, failed }
})

const groupedSiteTasks = computed(() => {
  const taskMap = new Map(tasks.value.map(task => [task.taskId, task]))
  const pageMap = new Map<number, CrawlerPageResult[]>()

  for (const pageResult of pageResults.value) {
    if (!pageMap.has(pageResult.subTaskId)) {
      pageMap.set(pageResult.subTaskId, [])
    }
    pageMap.get(pageResult.subTaskId)?.push(pageResult)
  }

  for (const pageList of pageMap.values()) {
    pageList.sort((left, right) => left.pageIndex - right.pageIndex)
  }

  const grouped = new Map<SiteKey, SiteTaskRow[]>()

  for (const subTask of subTasks.value) {
    const site = detectSite(subTask.url)
    const task = taskMap.get(subTask.taskId)
    const keyword = task?.keyword ?? subTask.keyword ?? ''

    const pageList = pageMap.get(subTask.subtaskId) ?? []
    const links: LinkRow[] =
      pageList.length > 0
        ? pageList.map(page => ({
            pageUrl: page.pageUrl,
            pageTitle: page.pageTitle,
            pageIndex: page.pageIndex,
            success: page.success,
            errorMessage: page.errorMessage,
          }))
        : [
            {
              pageUrl: buildSeedLink(subTask.url, keyword),
              pageTitle: '种子链接',
              pageIndex: 0,
              success: true,
            },
          ]

    const row: SiteTaskRow = {
      subtaskId: subTask.subtaskId,
      taskId: subTask.taskId,
      keyword,
      nodeId: subTask.nodeId || '-',
      taskStatus: task?.status || 'PENDING',
      taskProgress: task?.progress ?? 0,
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
        <div class="text-sm text-gray-500">进行中</div>
        <div class="text-2xl font-semibold mt-2 text-blue-600">{{ taskStats.running }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">已完成</div>
        <div class="text-2xl font-semibold mt-2 text-emerald-600">{{ taskStats.finished }}</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">失败/部分失败</div>
        <div class="text-2xl font-semibold mt-2 text-red-600">{{ taskStats.failed }}</div>
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
            <el-input
              v-model="siteKeywords[site.key]"
              :disabled="!selectedSites.includes(site.key)"
              maxlength="80"
              show-word-limit
              :placeholder="`输入 ${site.label} 的关键词`"
            />
          </el-form-item>
        </template>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">按站点查看任务与网页链接</span>
          <el-button text type="primary" :loading="loading" @click="loadTaskData">刷新</el-button>
        </div>
      </template>

      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <el-empty
            v-if="groupedSiteTasks.every(group => group.rows.length === 0)"
            description="还没有任务，先在上方表单创建一个任务"
          />

          <el-collapse v-else v-model="activeSitePanels">
            <el-collapse-item
              v-for="group in groupedSiteTasks"
              :key="group.key"
              :name="group.key"
              :disabled="group.rows.length === 0"
            >
              <template #title>
                <div class="flex items-center gap-2">
                  <span>{{ group.label }}</span>
                  <el-tag size="small" type="info">{{ group.rows.length }} 个子任务</el-tag>
                </div>
              </template>

              <el-table :data="group.rows" border stripe>
                <el-table-column type="expand">
                  <template #default="scope">
                    <div class="px-4 py-2">
                      <el-timeline>
                        <el-timeline-item
                          v-for="link in scope.row.links"
                          :key="`${scope.row.subtaskId}-${link.pageIndex}-${link.pageUrl}`"
                          :type="link.success ? 'success' : 'danger'"
                          :timestamp="`序号 ${link.pageIndex}`"
                        >
                          <div class="font-medium">{{ link.pageTitle || '网页链接' }}</div>
                          <a class="text-blue-600 break-all" :href="link.pageUrl" target="_blank" rel="noopener">
                            {{ link.pageUrl }}
                          </a>
                          <div v-if="link.errorMessage" class="text-red-500 mt-1">{{ link.errorMessage }}</div>
                        </el-timeline-item>
                      </el-timeline>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="subtaskId" label="子任务ID" width="110" />
                <el-table-column prop="taskId" label="任务ID" width="100" />
                <el-table-column prop="keyword" label="关键词" min-width="160" />
                <el-table-column label="任务状态" width="130">
                  <template #default="scope">
                    <el-tag :type="statusTagType(scope.row.taskStatus)">{{ statusText(scope.row.taskStatus) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="进度" width="180">
                  <template #default="scope">
                    <el-progress :percentage="scope.row.taskProgress" :stroke-width="10" />
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
