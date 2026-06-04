export interface UserInfo {
  userId?: number
  username: string
  email: string
  role?: string
  status?: string
  lastLoginAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface TaskRuntime {
  taskId: number
  status: string
  assignedNodeId?: string | null
  progressPercent: number
  expectedPages?: number | null
  completedPages: number
  successPages: number
  failedPages: number
  retryCount?: number
  lastErrorCode?: string | null
  lastErrorMessage?: string | null
  queuedAt?: string | null
  startedAt?: string | null
  finishedAt?: string | null
  updatedAt?: string
}

export interface Task {
  taskId: number
  userId?: number | null
  nodeId?: string
  batchId?: string | null
  url: string
  keyword: string
  siteType?: string | null
  taskStatus: string
  taskProgress: number
  totalPages: number
  maxLinksPerLevel?: number
  priority?: number
  source?: string
  idempotencyKey?: string | null
  retryCount?: number
  cancelRequested?: boolean
  lastErrorMessage?: string | null
  archived?: boolean
  archivedAt?: string | null
  createdAt?: string
  updatedAt?: string
  startedAt?: string | null
  finishedAt?: string | null
  runtime?: TaskRuntime | null
  tagIds?: number[]
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
  tags?: ResultTag[]
}

export interface TaskEvent {
  eventId: number
  taskId: number
  nodeId?: string | null
  eventType: string
  eventLevel: string
  eventMessage: string
  payloadJson?: string | null
  createdAt?: string
}

export interface TaskFile {
  fileId: number
  taskId: number
  pageResultId?: number | null
  fileType: string
  storageType?: string
  mimeType?: string
  filePath?: string | null
  contentSha256?: string | null
  sizeBytes?: number | null
  createdAt?: string
  updatedAt?: string
}

export interface TaskDetail {
  task: Task
  runtime?: TaskRuntime | null
  events: TaskEvent[]
  files: TaskFile[]
  pageResults: CrawlerPageResult[]
}

export interface CrawlerNode {
  nodeId: string
  nodeName?: string
  status: string
  version?: string
  capabilitiesJson?: string | null
  tagsJson?: string | null
  capabilities?: string[]
  tags?: string[]
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
  batchId?: string
  idempotencyKey?: string
  tagIds?: number[]
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

export interface TaskBatch {
  batchId: string
  batchName?: string | null
  createdBy?: number | null
  taskCount: number
  status: string
  notes?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface TaskBatchDetail {
  batch: TaskBatch
  tasks: Task[]
}

export interface ResultTag {
  tagId?: number
  tagName: string
  tagColor?: string | null
  categoryName?: string | null
  description?: string | null
  bindingCount?: number | null
  createdAt?: string
  updatedAt?: string
  pageResultIds?: number[]
}

export interface ExportRecord {
  exportId?: number
  userId?: number | null
  taskId?: number | null
  exportScope: string
  exportType: string
  fileName?: string | null
  recordCount?: number | null
  status?: string | null
  createdAt?: string
}

export interface TaskNote {
  noteId?: number
  taskId: number
  userId?: number | null
  noteContent: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskGroup {
  groupId?: number
  userId?: number | null
  groupName: string
  groupColor?: string | null
  description?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface TaskGroupBinding {
  bindingId?: number
  taskId: number
  groupId: number
  createdBy?: number | null
  createdAt?: string
}

export interface Notification {
  notificationId?: number
  userId?: number
  type: string
  level?: string
  title: string
  content?: string
  link?: string
  isRead?: boolean
  createdAt?: string
}

export interface TaskTemplate {
  templateId?: number
  userId?: number
  templateName: string
  keyword?: string
  url?: string
  siteType?: string
  maxLinksPerLevel?: number
  tagIdsJson?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface SystemConfig {
  configKey: string
  configValue: string
  description?: string
  category?: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskSchedule {
  scheduleId?: number
  userId?: number
  scheduleName: string
  keyword: string
  url?: string
  siteType?: string
  maxLinksPerLevel?: number
  cronExpression: string
  enabled?: boolean
  lastRunAt?: string | null
  nextRunAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface HealthInfo {
  mysql: { status: string; error?: string }
  rabbitmq: { status: string; error?: string }
  jvm: { totalMemoryMB: number; freeMemoryMB: number; usedMemoryMB: number; maxMemoryMB: number; processors: number }
  disk: { totalGB: number; freeGB: number; usableGB: number }
}

export interface CleanupStats {
  totalTasks: number
  totalPageResults: number
  totalLogs: number
  totalEvents: number
  totalNotifications: number
  archivedTasks: number
}
