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

export async function login(email: string, password: string) {
  return request.post<ApiResponse<AuthResponse>>('/login', { email, password })
}

export async function register(email: string, password: string) {
  return request.post<ApiResponse<UserInfo>>('/register', { email, password })
}

export async function dispatchTask(payload: DispatchTaskPayload) {
  return request.post<ApiResponse<Task[]>>('/task/dispatch', payload)
}

export async function fetchTasks() {
  const res = await request.get<ApiResponse<Task[]>>('/task/task')
  return unwrapResponse<Task[]>(res) ?? []
}

export async function fetchPageResults(taskId?: number) {
  const params: { taskId?: number } = {}
  if (taskId) {
    params.taskId = taskId
  }
  const res = await request.get<ApiResponse<CrawlerPageResult[]>>('/task/page-results', { params })
  return unwrapResponse<CrawlerPageResult[]>(res) ?? []
}

export async function cachePageResultMhtml(pageResultId: number) {
  const res = await request.post<ApiResponse<CrawlerPageResult>>('/task/cache-mhtml', null, {
    params: { pageResultId },
  })
  return unwrapResponse<CrawlerPageResult>(res)
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
  return unwrapResponse<CrawlerNode[]>(res) ?? []
}

export async function fetchTaskLogs() {
  const res = await request.get<ApiResponse<TaskLog[]>>('/log')
  return unwrapResponse<TaskLog[]>(res) ?? []
}

export function getPageResultStreamUrl() {
  return `${API_BASE_URL}/task/page-results/stream`
}
