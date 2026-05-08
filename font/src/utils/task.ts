import type { CrawlerNode, CrawlerPageResult, Task } from '@/types/entity'

export type SiteKey = 'sohu' | 'bing' | 'baike' | 'tencent_news' | 'sina_news' | 'thepaper' | 'huanqiu' | 'chinanews' | 'cctv_news' | 'guancha' | 'wikipedia' | 'other'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'FINISHED' | 'FAILED' | 'PARTIAL_FAILED' | 'CANCELLED'

export function detectSite(url: string): SiteKey {
  if (url.includes('search.sohu.com')) {
    return 'sohu'
  }
  if (url.includes('www.bing.com')) {
    return 'bing'
  }
  if (url.includes('baike.baidu.com')) {
    return 'baike'
  }
  if (url.includes('chinanews.com.cn')) {
    return 'chinanews'
  }
  if (url.includes('cctv.com')) {
    return 'cctv_news'
  }
  if (url.includes('guancha.cn')) {
    return 'guancha'
  }
  if (url.includes('news.qq.com')) {
    return 'tencent_news'
  }
  if (url.includes('news.sina.com.cn') || url.includes('sina.com.cn')) {
    return 'sina_news'
  }
  if (url.includes('thepaper.cn')) {
    return 'thepaper'
  }
  if (url.includes('huanqiu.com')) {
    return 'huanqiu'
  }
  if (url.includes('en.wikipedia.org')) {
    return 'wikipedia'
  }
  return 'other'
}

export function siteLabel(key: SiteKey) {
  if (key === 'sohu') {
    return '搜狐'
  }
  if (key === 'bing') {
    return 'Bing'
  }
  if (key === 'baike') {
    return '百度百科'
  }
  if (key === 'tencent_news') {
    return '腾讯新闻'
  }
  if (key === 'sina_news') {
    return '新浪新闻'
  }
  if (key === 'thepaper') {
    return '澎湃新闻'
  }
  if (key === 'huanqiu') {
    return '环球网'
  }
  if (key === 'chinanews') {
    return '中国新闻网'
  }
  if (key === 'cctv_news') {
    return '央视网'
  }
  if (key === 'guancha') {
    return '观察者网'
  }
  if (key === 'wikipedia') {
    return '维基百科'
  }
  return '其他站点'
}

export function normalizeTaskStatus(status?: string | null): TaskStatus {
  const normalized = String(status || 'PENDING').trim().toUpperCase()
  if (normalized === 'RUNNING') {
    return 'RUNNING'
  }
  if (normalized === 'FINISHED') {
    return 'FINISHED'
  }
  if (normalized === 'FAILED') {
    return 'FAILED'
  }
  if (normalized === 'PARTIAL_FAILED') {
    return 'PARTIAL_FAILED'
  }
  if (normalized === 'CANCELLED') {
    return 'CANCELLED'
  }
  return 'PENDING'
}

export function statusText(status?: string | null) {
  const normalized = normalizeTaskStatus(status)
  if (normalized === 'FINISHED') {
    return '已完成'
  }
  if (normalized === 'FAILED') {
    return '失败'
  }
  if (normalized === 'PARTIAL_FAILED') {
    return '部分失败'
  }
  if (normalized === 'RUNNING') {
    return '执行中'
  }
  if (normalized === 'CANCELLED') {
    return '已取消'
  }
  return '排队中'
}

export function statusTagType(status?: string | null): 'success' | 'danger' | 'warning' | 'info' {
  const normalized = normalizeTaskStatus(status)
  if (normalized === 'FINISHED') {
    return 'success'
  }
  if (normalized === 'FAILED') {
    return 'danger'
  }
  if (normalized === 'PARTIAL_FAILED' || normalized === 'RUNNING') {
    return 'warning'
  }
  return 'info'
}

export function normalizeNodeId(value?: string | number | null) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

export function buildSeedLink(url: string, keyword: string) {
  return `${url}${encodeURIComponent(keyword)}`
}

export function resolveExpectedPages(pageList: CrawlerPageResult[], fallback = 10) {
  const candidates = pageList
    .map(page => page.totalPages ?? 0)
    .filter(value => value && value > 0)

  if (candidates.length > 0) {
    return Math.max(...candidates)
  }

  return fallback
}

export function calculateTaskRuntime(pageList: CrawlerPageResult[], expectedPages: number) {
  const completedPages = pageList.length
  const totalPages = expectedPages > 0 ? expectedPages : 10
  const failedPages = pageList.filter(page => !page.success).length
  const progress = Math.min(100, Math.round((completedPages / totalPages) * 100))

  if (completedPages === 0) {
    return { status: 'PENDING' as TaskStatus, progress: 0, completedPages, expectedPages: totalPages }
  }

  if (completedPages < totalPages) {
    return { status: 'RUNNING' as TaskStatus, progress, completedPages, expectedPages: totalPages }
  }

  if (failedPages === 0) {
    return { status: 'FINISHED' as TaskStatus, progress: 100, completedPages, expectedPages: totalPages }
  }

  if (failedPages >= completedPages) {
    return { status: 'FAILED' as TaskStatus, progress: 100, completedPages, expectedPages: totalPages }
  }

  return { status: 'PARTIAL_FAILED' as TaskStatus, progress: 100, completedPages, expectedPages: totalPages }
}

export function resolveTaskStatus(task: Task, pageList: CrawlerPageResult[], fallbackExpectedPages = 10) {
  if (task.runtime?.status) {
    return normalizeTaskStatus(task.runtime.status)
  }
  const normalized = normalizeTaskStatus(task.taskStatus)
  if (normalized !== 'PENDING' || pageList.length === 0) {
    return normalized
  }

  return calculateTaskRuntime(pageList, resolveExpectedPages(pageList, fallbackExpectedPages)).status
}

export function resolveTaskProgress(task: Task, pageList: CrawlerPageResult[], fallbackExpectedPages = 10) {
  if (typeof task.runtime?.progressPercent === 'number') {
    return task.runtime.progressPercent
  }
  if (typeof task.taskProgress === 'number' && task.taskProgress > 0) {
    return task.taskProgress
  }

  return calculateTaskRuntime(pageList, resolveExpectedPages(pageList, fallbackExpectedPages)).progress
}

export function resolveTaskNodeId(task: Task, pageList: CrawlerPageResult[]) {
  const runtimeNodeId = normalizeNodeId(task.runtime?.assignedNodeId)
  if (runtimeNodeId) {
    return runtimeNodeId
  }
  const taskNodeId = normalizeNodeId(task.nodeId)
  if (taskNodeId && taskNodeId !== '-1') {
    return taskNodeId
  }

  const pageNodeId = pageList
    .map(page => normalizeNodeId(page.nodeId))
    .find(nodeId => nodeId && nodeId !== '-1')

  return pageNodeId || ''
}

export function deriveNodeStatus(node: CrawlerNode, now = Date.now()) {
  const heartbeat = node.lastHeartbeat ? Date.parse(node.lastHeartbeat) : 0
  const timeoutSeconds = node.heartbeatTimeoutSec ?? 15
  const delaySeconds = heartbeat ? Math.floor((now - heartbeat) / 1000) : Number.POSITIVE_INFINITY
  const rawStatus = String(node.status || 'UNKNOWN').toUpperCase()
  const displayStatus = !heartbeat || delaySeconds > timeoutSeconds ? 'OFFLINE' : rawStatus

  let statusType: 'success' | 'warning' | 'danger' | 'info' = 'info'
  if (displayStatus === 'ONLINE' || displayStatus === 'IDLE') {
    statusType = 'success'
  } else if (displayStatus === 'BUSY') {
    statusType = 'warning'
  } else if (displayStatus === 'OFFLINE') {
    statusType = 'danger'
  }

  return {
    rawStatus,
    displayStatus,
    statusType,
    delaySeconds: Number.isFinite(delaySeconds) ? delaySeconds : -1,
    heartbeat,
  }
}
