import request, { API_BASE_URL } from '@/api/axiosConfig'
import type { AxiosResponse } from 'axios'
import type {
  AuthResponse,
  CrawlerNode,
  CrawlerPageResult,
  DispatchTaskPayload,
  Task,
  TaskDetail,
  TaskEvent,
  TaskLog,
  TaskRuntime,
  TaskBatch,
  TaskBatchDetail,
  ResultTag,
  UserInfo,
} from '@/types/entity'
import type { ApiResponse } from '@/types/apiResponse'

type RawTask = Partial<Task> & { node_id?: string }
type RawTaskRuntime = Partial<TaskRuntime> & { assigned_node_id?: string }
type RawCrawlerPageResult = Partial<CrawlerPageResult> & { node_id?: string }
type RawCrawlerNode = Partial<CrawlerNode>
type RawTaskLog = Partial<TaskLog> & { node_key?: string }
type RawTaskEvent = Partial<TaskEvent> & { node_id?: string; event_type?: string; event_level?: string; event_message?: string; payload_json?: string }
type RawTaskBatch = Partial<TaskBatch> & { batch_id?: string; batch_name?: string; created_by?: number; task_count?: number }
type RawResultTag = Partial<ResultTag> & { tag_id?: number; tag_name?: string; tag_color?: string; category_name?: string; binding_count?: number; page_result_ids?: number[] }
type RawTaskDetail = {
  task?: RawTask
  runtime?: RawTaskRuntime | null
  events?: RawTaskEvent[]
  files?: TaskDetail['files']
  pageResults?: RawCrawlerPageResult[]
}
type RawTaskBatchDetail = {
  batch?: RawTaskBatch
  tasks?: RawTask[]
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

function tryParseJsonArray(value?: string | null) {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(item => String(item)) : []
  } catch {
    return []
  }
}

function mapTaskRuntime(raw?: RawTaskRuntime | null): TaskRuntime | null {
  if (!raw) {
    return null
  }
  return {
    taskId: Number(raw.taskId ?? 0),
    status: String(raw.status ?? 'PENDING').toUpperCase(),
    assignedNodeId: String(raw.assignedNodeId ?? raw.assigned_node_id ?? '').trim() || null,
    progressPercent: Number(raw.progressPercent ?? 0),
    expectedPages: raw.expectedPages ?? null,
    completedPages: Number(raw.completedPages ?? 0),
    successPages: Number(raw.successPages ?? 0),
    failedPages: Number(raw.failedPages ?? 0),
    retryCount: Number(raw.retryCount ?? 0),
    lastErrorCode: raw.lastErrorCode ?? null,
    lastErrorMessage: raw.lastErrorMessage ?? null,
    queuedAt: raw.queuedAt ?? null,
    startedAt: raw.startedAt ?? null,
    finishedAt: raw.finishedAt ?? null,
    updatedAt: raw.updatedAt,
  }
}

function mapTask(raw: RawTask, runtime?: TaskRuntime | null): Task {
  return {
    taskId: Number(raw.taskId ?? 0),
    userId: raw.userId ?? null,
    nodeId: String(raw.nodeId ?? raw.node_id ?? '').trim() || undefined,
    batchId: raw.batchId ?? null,
    url: String(raw.url ?? ''),
    keyword: String(raw.keyword ?? ''),
    siteType: raw.siteType ?? null,
    taskStatus: String(raw.taskStatus ?? runtime?.status ?? 'PENDING').toUpperCase(),
    taskProgress: Number(raw.taskProgress ?? runtime?.progressPercent ?? 0),
    totalPages: Number(raw.totalPages ?? runtime?.expectedPages ?? 0),
    maxLinksPerLevel: raw.maxLinksPerLevel,
    priority: raw.priority,
    source: raw.source,
    idempotencyKey: raw.idempotencyKey ?? null,
    retryCount: raw.retryCount,
    cancelRequested: Boolean(raw.cancelRequested),
    lastErrorMessage: raw.lastErrorMessage ?? runtime?.lastErrorMessage ?? null,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
    startedAt: raw.startedAt ?? runtime?.startedAt ?? null,
    finishedAt: raw.finishedAt ?? runtime?.finishedAt ?? null,
    runtime: runtime ?? null,
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
  const capabilities = raw.capabilities?.length ? raw.capabilities : tryParseJsonArray(raw.capabilitiesJson)
  const tags = raw.tags?.length ? raw.tags : tryParseJsonArray(raw.tagsJson)
  return {
    nodeId: String(raw.nodeId ?? ''),
    nodeName: raw.nodeName,
    status: String(raw.status ?? 'UNKNOWN').toUpperCase(),
    version: raw.version,
    capabilitiesJson: raw.capabilitiesJson ?? null,
    tagsJson: raw.tagsJson ?? null,
    capabilities,
    tags,
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

function mapTaskEvent(raw: RawTaskEvent): TaskEvent {
  return {
    eventId: Number(raw.eventId ?? 0),
    taskId: Number(raw.taskId ?? 0),
    nodeId: String(raw.nodeId ?? raw.node_id ?? '').trim() || null,
    eventType: String(raw.eventType ?? raw.event_type ?? 'UNKNOWN'),
    eventLevel: String(raw.eventLevel ?? raw.event_level ?? 'INFO').toUpperCase(),
    eventMessage: String(raw.eventMessage ?? raw.event_message ?? ''),
    payloadJson: raw.payloadJson ?? raw.payload_json ?? null,
    createdAt: raw.createdAt,
  }
}

function mapTaskBatch(raw: RawTaskBatch): TaskBatch {
  return {
    batchId: String(raw.batchId ?? raw.batch_id ?? ''),
    batchName: raw.batchName ?? raw.batch_name ?? null,
    createdBy: raw.createdBy ?? raw.created_by ?? null,
    taskCount: Number(raw.taskCount ?? raw.task_count ?? 0),
    status: String(raw.status ?? 'PENDING'),
    notes: raw.notes ?? null,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
  }
}

function mapResultTag(raw: RawResultTag): ResultTag {
  return {
    tagId: Number(raw.tagId ?? raw.tag_id ?? 0),
    tagName: String(raw.tagName ?? raw.tag_name ?? ''),
    tagColor: raw.tagColor ?? raw.tag_color ?? null,
    categoryName: raw.categoryName ?? raw.category_name ?? null,
    description: raw.description ?? null,
    bindingCount: raw.bindingCount ?? raw.binding_count ?? null,
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
    pageResultIds: raw.pageResultIds ?? raw.page_result_ids ?? [],
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

export async function dispatchBatchTask(payload: DispatchTaskPayload & { keywordsText: string; batchName?: string; batchNotes?: string }) {
  return request.post<ApiResponse<Task[]>>('/task/dispatch-batch', payload)
}

export async function fetchTasks() {
  const res = await request.get<ApiResponse<Task[]>>('/task')
  return (unwrapResponse<RawTask[]>(res) ?? []).map(raw => mapTask(raw))
}

export async function fetchTaskDetail(taskId: number) {
  const res = await request.get<ApiResponse<TaskDetail>>(`/task/${taskId}`)
  const raw = unwrapResponse<RawTaskDetail>(res)
  if (!raw?.task) {
    return null
  }
  const runtime = mapTaskRuntime(raw.runtime)
  return {
    task: mapTask(raw.task, runtime),
    runtime,
    events: (raw.events ?? []).map(mapTaskEvent),
    files: raw.files ?? [],
    pageResults: (raw.pageResults ?? []).map(mapPageResult),
  } as TaskDetail
}

export async function fetchTaskEvents(taskId: number) {
  const res = await request.get<ApiResponse<TaskEvent[]>>('/task/events', { params: { taskId } })
  return (unwrapResponse<RawTaskEvent[]>(res) ?? []).map(mapTaskEvent)
}

export async function fetchPageResults(taskId?: number) {
  const params: { taskId?: number } = {}
  if (taskId) {
    params.taskId = taskId
  }
  const res = await request.get<ApiResponse<CrawlerPageResult[]>>('/task/page-results', { params })
  return (unwrapResponse<RawCrawlerPageResult[]>(res) ?? []).map(mapPageResult)
}

export async function exportPageResults(taskId?: number) {
  const response = await request.get('/task/page-results/export', {
    params: taskId ? { taskId } : {},
    responseType: 'blob',
  })
  const blobResponse = response as AxiosResponse<Blob>
  const fileName = parseDownloadFileName(
    blobResponse.headers?.['content-disposition'] as string | undefined,
  )
  return {
    blob: blobResponse.data,
    fileName: fileName || 'page-results.csv',
  }
}

export async function exportPageResultMhtml(taskId?: number) {
  const response = await request.get('/task/page-results/export-mhtml', {
    params: taskId ? { taskId } : {},
    responseType: 'blob',
  })
  const blobResponse = response as AxiosResponse<Blob>
  const fileName = parseDownloadFileName(
    blobResponse.headers?.['content-disposition'] as string | undefined,
  )
  return {
    blob: blobResponse.data,
    fileName: fileName || 'page-results-mhtml.zip',
  }
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
  const fileName = parseDownloadFileName(blobResponse.headers?.['content-disposition'] as string | undefined)
  return { blob: blobResponse.data, fileName }
}

export async function fetchCrawlerNodes() {
  const res = await request.get<ApiResponse<CrawlerNode[]>>('/client')
  return (unwrapResponse<RawCrawlerNode[]>(res) ?? []).map(mapCrawlerNode)
}

export async function fetchTaskLogs() {
  const res = await request.get<ApiResponse<TaskLog[]>>('/log')
  return (unwrapResponse<RawTaskLog[]>(res) ?? []).map(mapTaskLog)
}

export async function fetchTaskBatches() {
  const res = await request.get<ApiResponse<TaskBatch[]>>('/task/batches')
  return (unwrapResponse<RawTaskBatch[]>(res) ?? []).map(mapTaskBatch)
}

export async function fetchTaskBatchDetail(batchId: string) {
  const res = await request.get<ApiResponse<TaskBatchDetail>>(`/task/batches/${batchId}`)
  const raw = unwrapResponse<RawTaskBatchDetail>(res)
  if (!raw?.batch) {
    return null
  }
  return {
    batch: mapTaskBatch(raw.batch),
    tasks: (raw.tasks ?? []).map(task => mapTask(task)),
  } as TaskBatchDetail
}

export async function fetchBookmarks() {
  const res = await request.get<ApiResponse<CrawlerPageResult[]>>('/task/bookmarks')
  return (unwrapResponse<RawCrawlerPageResult[]>(res) ?? []).map(mapPageResult)
}

export async function fetchBookmarkIds() {
  const res = await request.get<ApiResponse<number[]>>('/task/bookmarks/ids')
  return new Set(unwrapResponse<number[]>(res) ?? [])
}

export async function addBookmark(pageResultId: number) {
  const res = await request.post<ApiResponse<boolean>>('/task/bookmarks', null, {
    params: { pageResultId },
  })
  return unwrapResponse<boolean>(res)
}

export async function removeBookmark(pageResultId: number) {
  const res = await request.delete<ApiResponse<boolean>>('/task/bookmarks', {
    params: { pageResultId },
  })
  return unwrapResponse<boolean>(res)
}

export async function fetchResultTags() {
  const res = await request.get<ApiResponse<ResultTag[]>>('/task/result-tags')
  return (unwrapResponse<RawResultTag[]>(res) ?? []).map(mapResultTag)
}

export async function createResultTag(payload: ResultTag) {
  const res = await request.post<ApiResponse<ResultTag>>('/task/result-tags', payload)
  const raw = unwrapResponse<RawResultTag>(res)
  return raw ? mapResultTag(raw) : null
}

export async function updateResultTag(tagId: number, payload: ResultTag) {
  const res = await request.put<ApiResponse<ResultTag>>(`/task/result-tags/${tagId}`, payload)
  const raw = unwrapResponse<RawResultTag>(res)
  return raw ? mapResultTag(raw) : null
}

export async function deleteResultTag(tagId: number) {
  const res = await request.delete<ApiResponse<boolean>>(`/task/result-tags/${tagId}`)
  return unwrapResponse<boolean>(res)
}

export async function bindResultTag(tagId: number, pageResultId: number) {
  const res = await request.post<ApiResponse<boolean>>(`/task/result-tags/${tagId}/bindings`, null, {
    params: { pageResultId },
  })
  return unwrapResponse<boolean>(res)
}

export async function unbindResultTag(tagId: number, pageResultId: number) {
  const res = await request.delete<ApiResponse<boolean>>(`/task/result-tags/${tagId}/bindings`, {
    params: { pageResultId },
  })
  return unwrapResponse<boolean>(res)
}

export async function fetchPageResultTagBindings(pageResultIds: number[]) {
  const res = await request.get<ApiResponse<Record<number, ResultTag[]>>>('/task/result-tags/bindings', {
    params: { pageResultIds },
    paramsSerializer: {
      serialize: params => (params.pageResultIds as number[]).map(id => `pageResultIds=${id}`).join('&'),
    },
  })
  const raw = unwrapResponse<Record<string, RawResultTag[]>>(res) ?? {}
  return Object.fromEntries(
    Object.entries(raw).map(([key, value]) => [Number(key), (value ?? []).map(mapResultTag)]),
  ) as Record<number, ResultTag[]>
}

export function getPageResultStreamUrl() {
  const token = localStorage.getItem('token')
  if (!token) {
    return `${API_BASE_URL}/task/page-results/stream`
  }
  return `${API_BASE_URL}/task/page-results/stream?token=${encodeURIComponent(token)}`
}

export function getTaskRuntimeStreamUrl() {
  const token = localStorage.getItem('token')
  if (!token) {
    return `${API_BASE_URL}/task/runtime/stream`
  }
  return `${API_BASE_URL}/task/runtime/stream?token=${encodeURIComponent(token)}`
}
