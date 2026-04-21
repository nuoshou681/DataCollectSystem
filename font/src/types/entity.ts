export interface UserInfo {
  password:string,
  email:string,
  username:string
}

export interface Task {
  taskId: number
  nodeId?: string
  node_id?: string
  url: string
  keyword?: string
  taskStatus?: string
  taskProgress?: number
  totalPages?: number
}

export interface CrawlerPageResult {
  pageResultId?: number
  taskId: number
  nodeId?: string
  node_id?: string
  pageUrl: string
  pageTitle?: string
  pageIndex: number
  totalPages?: number
  success: boolean
  filePath?: string
  errorMessage?: string
  mhtmlCached?: boolean
  mhtmlCachedAt?: string
}

export interface CrawlerNode {
  nodeId: string
  status: string
  lastHeartbeat?: string
}

export interface DispatchTaskPayload {
  userId?: number
  keyword: string
  url: string
  status?: string
  progress?: number
}

export interface AuthResponse {
  token: string
  role: string
}

export interface TaskLog {
  logId: number
  taskId: number
  nodeId?: string | number
  logMessage: string
  logLevel: string
}
