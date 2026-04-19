export interface UserInfo {
  password:string,
  email:string,
  username:string
}

export interface Task {
  taskId: number
  userId?: number
  keyword: string
  url: string
  status?: string
  progress?: number
}

export interface SubTask {
  subtaskId: number
  taskId: number
  nodeId?: string
  url: string
  keyword?: string
}

export interface CrawlerPageResult {
  taskId: number
  subTaskId: number
  nodeId?: string
  pageUrl: string
  pageTitle?: string
  pageIndex: number
  success: boolean
  filePath?: string
  errorMessage?: string
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
