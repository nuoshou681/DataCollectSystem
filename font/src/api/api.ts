import request, { API_BASE_URL } from '@/api/axiosConfig'
import type { AxiosResponse } from 'axios'
import type {
  UserInfo,
  Task,
  CrawlerPageResult,
  CrawlerNode,
  DispatchTaskPayload,
  AuthResponse,
  TaskLog,
} from '@/types/entity'
import type { ApiResponse  } from '@/types/apiResponse'

type RawTask = Partial<Task> & {
  node_id?: string
}

type RawCrawlerPageResult = Partial<CrawlerPageResult> & {
  node_id?: string
}

type RawCrawlerNode = Partial<CrawlerNode>

type RawTaskLog = Partial<TaskLog> & {
  node_key?: string
}

function unwrapResponse<T>(res: unknown): T | null {
  if (!res || typeof res !== 'object') {
    return null
  }

  if ('code' in res && 'data' in res) {
    return (res as ApiResponse<T>).data
  }

  if ('data' in res) {
    const nested = (res as { data?: unknown }).data
    if (nested && typeof nested === 'object' && 'data' in nested) {
      return (nested as ApiResponse<T>).data
    }
  }

  return null
}

function parseDownloadFileName(contentDisposition?: string) {
  if (!contentDisposition) {
    return 'crawler-page.mhtml'
  }

  const encodedMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(encodedMatch[1])
    } catch {
      return encodedMatch[1]
    }
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (plainMatch?.[1]) {
    return plainMatch[1]
  }

  return 'crawler-page.mhtml'
}

function mapTask(raw: RawTask): Task {
  return {
    taskId: Number(raw.taskId ?? 0),
    userId: raw.userId ?? null,
    nodeId: String(raw.nodeId ?? raw.node_id ?? '').trim() || undefined,
    url: String(raw.url ?? ''),
    keyword: String(raw.keyword ?? ''),
    siteType: raw.siteType ?? null,
    taskStatus: String(raw.taskStatus ?? 'PENDING').toUpperCase(),
    taskProgress: Number(raw.taskProgress ?? 0),
    totalPages: Number(raw.totalPages ?? 0),
    maxLinksPerLevel: raw.maxLinksPerLevel,
    priority: raw.priority,
    source: raw.source,
    retryCount: raw.retryCount,
    cancelRequested: Boolean(raw.cancelRequested),
    lastErrorMessage: raw.lastErrorMessage ?? null,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
    startedAt: raw.startedAt ?? null,
    finishedAt: raw.finishedAt ?? null,
  }
}

function mapPageResult(raw: RawCrawlerPageResult): CrawlerPageResult {
  return {
    pageResultId: raw.pageResultId,
    taskId: Number(raw.taskId ?? 0),
    nodeId: String(raw.nodeId ?? raw.node_id ?? '').trim() || undefined,
    pageUrl: String(raw.pageUrl ?? ''),
    pageTitle: raw.pageTitle,
    pageIndex: Number(raw.pageIndex ?? 0),
    totalPages: raw.totalPages ? Number(raw.totalPages) : undefined,
    success: Boolean(raw.success),
    siteType: raw.siteType ?? null,
    filePath: raw.filePath,
    storageType: raw.storageType,
    mimeType: raw.mimeType,
    fileSizeBytes: raw.fileSizeBytes ?? null,
    contentSha256: raw.contentSha256 ?? null,
    errorCode: raw.errorCode ?? null,
    errorMessage: raw.errorMessage,
    mhtmlCached: Boolean(raw.mhtmlCached),
    mhtmlCachedAt: raw.mhtmlCachedAt,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
  }
}

function mapCrawlerNode(raw: RawCrawlerNode): CrawlerNode {
  return {
    nodeId: String(raw.nodeId ?? ''),
    nodeName: raw.nodeName,
    status: String(raw.status ?? 'UNKNOWN').toUpperCase(),
    version: raw.version,
    maxConcurrency: raw.maxConcurrency,
    currentLoad: raw.currentLoad,
    heartbeatTimeoutSec: raw.heartbeatTimeoutSec,
    lastHeartbeat: raw.lastHeartbeat,
    lastOnlineAt: raw.lastOnlineAt,
  }
}

function mapTaskLog(raw: RawTaskLog): TaskLog {
  return {
    logId: Number(raw.logId ?? 0),
    taskId: Number(raw.taskId ?? 0),
    nodeId: raw.nodeId ?? null,
    nodeKey: raw.nodeKey ?? raw.node_key ?? null,
    logMessage: String(raw.logMessage ?? ''),
    logLevel: String(raw.logLevel ?? 'INFO').toUpperCase(),
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
  }
}

export async function login(email: string, password: string) {
  const res = await request.post<ApiResponse<AuthResponse>>('/login', { email, password })
  return unwrapResponse<AuthResponse>(res)
}

export async function register(username: string, email: string, password: string) {
  const res = await request.post<ApiResponse<UserInfo>>('/register', { username, email, password })
  return unwrapResponse<UserInfo>(res)
}

export async function dispatchTask(payload: DispatchTaskPayload) {
  return request.post<ApiResponse<Task[]>>('/task/dispatch', payload)
}

export async function fetchTasks() {
  const res = await request.get<ApiResponse<Task[]>>('/task/task')
  return (unwrapResponse<RawTask[]>(res) ?? []).map(mapTask)
}

export async function fetchPageResults(taskId?: number) {
  const params: { taskId?: number } = {}
  if (taskId) {
    params.taskId = taskId
  }
  const res = await request.get<ApiResponse<CrawlerPageResult[]>>('/task/page-results', { params })
  return (unwrapResponse<RawCrawlerPageResult[]>(res) ?? []).map(mapPageResult)
}

export async function cachePageResultMhtml(pageResultId: number) {
  const res = await request.post<ApiResponse<CrawlerPageResult>>('/task/cache-mhtml', null, {
    params: { pageResultId },
  })
  const raw = unwrapResponse<RawCrawlerPageResult>(res)
  return raw ? mapPageResult(raw) : null
}

export async function downloadPageResultMhtml(pageResultId: number) {
  const response = await request.get('/task/download-mhtml', {
    params: { pageResultId },
    responseType: 'blob',
  })

  const blobResponse = response as AxiosResponse<Blob>
  const fileName = parseDownloadFileName(
    blobResponse.headers?.['content-disposition'] as string | undefined,
  )

  return {
    blob: blobResponse.data,
    fileName,
  }
}

export async function fetchCrawlerNodes() {
  const res = await request.get<ApiResponse<CrawlerNode[]>>('/client')
  return (unwrapResponse<RawCrawlerNode[]>(res) ?? []).map(mapCrawlerNode)
}

export async function fetchTaskLogs() {
  const res = await request.get<ApiResponse<TaskLog[]>>('/log')
  return (unwrapResponse<RawTaskLog[]>(res) ?? []).map(mapTaskLog)
}

export function getPageResultStreamUrl() {
  const token = localStorage.getItem('token')
  if (!token) {
    return `${API_BASE_URL}/task/page-results/stream`
  }
  return `${API_BASE_URL}/task/page-results/stream?token=${encodeURIComponent(token)}`
}
