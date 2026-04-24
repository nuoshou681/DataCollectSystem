export interface UserInfo {
  userId?: number
  username: string
  email: string
  role?: string
}

export interface Task {
  taskId: number
  userId?: number | null
  nodeId?: string
  url: string
  keyword: string
  siteType?: string | null
  taskStatus: string
  taskProgress: number
  totalPages: number
  maxLinksPerLevel?: number
  priority?: number
  source?: string
  retryCount?: number
  cancelRequested?: boolean
  lastErrorMessage?: string | null
  createdAt?: string
  updatedAt?: string
  startedAt?: string | null
  finishedAt?: string | null
}

export interface CrawlerPageResult {
  pageResultId?: number
  taskId: number
  nodeId?: string
  pageUrl: string
  pageTitle?: string
  pageIndex: number
  totalPages?: number
  success: boolean
  siteType?: string | null
  filePath?: string
  storageType?: string
  mimeType?: string
  fileSizeBytes?: number | null
  contentSha256?: string | null
  errorCode?: string | null
  errorMessage?: string
  mhtmlCached?: boolean
  mhtmlCachedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface CrawlerNode {
  nodeId: string
  nodeName?: string
  status: string
  version?: string
  maxConcurrency?: number
  currentLoad?: number
  heartbeatTimeoutSec?: number
  lastHeartbeat?: string
  lastOnlineAt?: string
}

export interface DispatchTaskPayload {
  userId?: number | null
  keyword: string
  url: string
  maxLinksPerLevel?: number
  siteType?: string | null
  priority?: number
  source?: string
}

export interface AuthResponse {
  token: string
  role: string
}

export interface TaskLog {
  logId: number
  taskId: number
  nodeId?: string | number | null
  nodeKey?: string | null
  logMessage: string
  logLevel: string
  createdAt?: string
  updatedAt?: string
}
