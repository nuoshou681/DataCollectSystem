import request from '@/api/axiosConfig'
import type {
  UserInfo,
  Task,
  SubTask,
  CrawlerPageResult,
  CrawlerNode,
  DispatchTaskPayload,
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

export async function login(email: string, password: string) {
  return request.post<ApiResponse<UserInfo>>('/login', { email, password })
}

export async function register(email: string, password: string) {
  return request.post<ApiResponse<UserInfo>>('/register', { email, password })
}

export async function dispatchTask(payload: DispatchTaskPayload) {
  return request.post<ApiResponse<number>>('/task/dispatch', payload)
}

export async function fetchTasks() {
  const res = await request.get<ApiResponse<Task[]>>('/task/task')
  return unwrapResponse<Task[]>(res) ?? []
}

export async function fetchSubTasks(taskId?: number) {
  const params = taskId ? { taskId } : undefined
  const res = await request.get<ApiResponse<SubTask[]>>('/task/sub', { params })
  return unwrapResponse<SubTask[]>(res) ?? []
}

export async function fetchPageResults(taskId?: number, subTaskId?: number) {
  const params: { taskId?: number; subTaskId?: number } = {}
  if (taskId) {
    params.taskId = taskId
  }
  if (subTaskId) {
    params.subTaskId = subTaskId
  }
  const res = await request.get<ApiResponse<CrawlerPageResult[]>>('/task/page-results', { params })
  return unwrapResponse<CrawlerPageResult[]>(res) ?? []
}

export async function fetchCrawlerNodes() {
  const res = await request.get<ApiResponse<CrawlerNode[]>>('/client')
  return unwrapResponse<CrawlerNode[]>(res) ?? []
}
