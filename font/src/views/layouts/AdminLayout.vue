<script setup lang="ts">
import {
  ArrowDownTrayIcon,
  Bars3Icon,
  BellIcon,
  BoltIcon,
  ClipboardDocumentCheckIcon,
  Cog6ToothIcon,
  DocumentIcon,
  ServerStackIcon,
  ShieldCheckIcon,
  Squares2X2Icon,
  UsersIcon,
  XMarkIcon,
} from '@heroicons/vue/24/outline'
import { BugAntIcon } from '@heroicons/vue/24/solid'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchCrawlerNodes,
  fetchNotifications,
  fetchStatsOverview,
  fetchTaskLogs,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/api'
import type { CrawlerNode, Notification, TaskLog } from '@/types/entity'
import { clearAuthSession, getCurrentUserProfile } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/admin', label: '控制台', icon: Squares2X2Icon },

  { path: '/admin/task-center', label: '任务治理', icon: ClipboardDocumentCheckIcon },
  { path: '/admin/exports', label: '导出中心', icon: ArrowDownTrayIcon },
  { path: '/admin/nodes', label: '节点管理', icon: ServerStackIcon },


  { path: '/admin/users', label: '用户管理', icon: UsersIcon },
  { path: '/admin/logs', label: '审计日志', icon: DocumentIcon },
  { path: '/admin/notifications', label: '通知中心', icon: BellIcon },
  { path: '/admin/config', label: '系统配置', icon: Cog6ToothIcon },
]

function isPathActive(path: string) {
  if (path === '/admin') return route.path === '/admin'
  return route.path === path || route.path.startsWith(path + '/')
}

const searchQuery = ref('')
const searchOpen = ref(false)
const notificationsOpen = ref(false)
const isLoadingSearch = ref(false)
const sidebarOpen = ref(false)
const nodes = ref<CrawlerNode[]>([])
const logs = ref<TaskLog[]>([])

const overviewStats = ref({
  totalTasks: 0,
  runningTasks: 0,
  finishedToday: 0,
  successRate: 0,
})

const breadcrumbMap: Record<string, string> = {
  '/admin': '控制台',
  '/admin/task-center': '任务治理',
  '/admin/exports': '导出中心',
  '/admin/nodes': '节点管理',
  '/admin/users': '用户管理',
  '/admin/logs': '审计日志',
  '/admin/notifications': '通知中心',
  '/admin/config': '系统配置',
  '/admin/profile': '个人设置',
}

const breadcrumbs = computed(() => {
  const items: { label: string; path?: string }[] = []
  if (route.path !== '/admin') {
    items.push({ label: '控制台', path: '/admin' })
  }
  const label = breadcrumbMap[route.path]
  if (label && label !== '控制台') {
    items.push({ label })
  }
  return items
})

const userInitials = computed(() => {
  const name = userName.value
  return name ? name.charAt(0).toUpperCase() : 'A'
})

const userName = computed(() => {
  const storedName = localStorage.getItem('profileName')
  if (storedName) return storedName
  const profile = getCurrentUserProfile()
  return profile.username || profile.email || '管理员'
})

const onlineNodeCount = computed(() =>
  nodes.value.filter(n => n.status === 'ONLINE').length,
)

const queueLoad = computed(() => {
  const online = nodes.value.filter(n => n.status === 'ONLINE')
  if (!online.length) return 0
  const totalLoad = online.reduce((sum, n) => sum + (n.currentLoad ?? 0), 0)
  const totalMax = online.reduce((sum, n) => sum + (n.maxConcurrency ?? 1), 0)
  return totalMax > 0 ? Math.round((totalLoad / totalMax) * 100) : 0
})

const searchResults = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (keyword.length < 2) return { nodes: [] as CrawlerNode[], logs: [] as TaskLog[] }
  const nodeMatches = nodes.value.filter(n =>
    n.nodeId.toLowerCase().includes(keyword) || n.status.toLowerCase().includes(keyword),
  )
  const logMatches = logs.value.filter(l =>
    String(l.taskId).includes(keyword) ||
    String(l.nodeId ?? '').includes(keyword) ||
    l.logMessage?.toLowerCase().includes(keyword) ||
    l.logLevel?.toLowerCase().includes(keyword),
  )
  return { nodes: nodeMatches.slice(0, 6), logs: logMatches.slice(0, 6) }
})

const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const notificationItems = computed(() => notifications.value.slice(0, 5))
const unreadNotificationCount = computed(() => unreadCount.value)

async function loadNotifications() {
  try {
    const [data, count] = await Promise.all([fetchNotifications(), fetchUnreadCount()])
    notifications.value = data
    unreadCount.value = count
  } catch { /* ignore */ }
}

async function loadSearchData() {
  if (isLoadingSearch.value) return
  isLoadingSearch.value = true
  try {
    const [nodeData, logResult, stats] = await Promise.all([
      fetchCrawlerNodes(),
      fetchTaskLogs(1, 50),
      fetchStatsOverview(),
    ])
    nodes.value = nodeData
    logs.value = logResult.records
    if (stats) {
      overviewStats.value = {
        totalTasks: Number(stats.totalTasks ?? 0),
        runningTasks: Number(stats.runningTasks ?? 0),
        finishedToday: Number(stats.finishedToday ?? 0),
        successRate: Number(stats.successRate ?? 0),
      }
    }
  } finally {
    isLoadingSearch.value = false
  }
}

function openSearch() {
  searchOpen.value = true
  if (!nodes.value.length && !logs.value.length) {
    void loadSearchData()
  }
}

function closeSearch() {
  setTimeout(() => { searchOpen.value = false }, 150)
}

function toggleNotifications() {
  notificationsOpen.value = !notificationsOpen.value
  if (notificationsOpen.value) void loadNotifications()
}

async function readNotification(id: number) {
  await markNotificationRead(id)
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  notifications.value = notifications.value.map(n =>
    n.notificationId === id ? { ...n, isRead: true } : n,
  )
}

async function readAllNotifications() {
  await markAllNotificationsRead()
  unreadCount.value = 0
  notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
}

function handleLogClick(log?: TaskLog) {
  if (log) {
    router.push({ path: '/admin/logs', query: { taskId: String(log.taskId) } })
  } else {
    router.push('/admin/logs')
  }
  searchOpen.value = false
}

function handleNodeClick(node?: CrawlerNode) {
  router.push(node ? `/admin/nodes?highlight=${node.nodeId}` : '/admin/nodes')
  searchOpen.value = false
}

function logout() {
  clearAuthSession()
  router.push('/login')
}

function closeSidebar() {
  sidebarOpen.value = false
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (notificationsOpen.value && !target.closest('.notification-area')) {
    notificationsOpen.value = false
  }
}

onMounted(() => {
  void loadSearchData()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="app-shell">
    <!-- Mobile overlay -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="closeSidebar" />

    <!-- Sidebar -->
    <aside class="side-panel" :class="{ open: sidebarOpen }">
      <div class="brand">
        <BugAntIcon class="brand-icon" />
        <div>
          <div class="brand-title">采集控制台</div>
          <div class="brand-sub">管理员空间</div>
        </div>
        <button class="sidebar-close-btn" @click="closeSidebar">
          <XMarkIcon class="w-5 h-5" />
        </button>
      </div>

      <div class="nav-section">
        <div class="nav-title">运营</div>
        <nav class="nav-list">
          <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isPathActive(item.path) }"
            @click="closeSidebar"
          >
            <component :is="item.icon" class="nav-icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>
      </div>

      <div class="nav-section">
        <div class="nav-title">系统信号</div>
        <div class="signal-grid">
          <div class="signal">
            <ShieldCheckIcon class="signal-icon" />
            <div>
              <div class="signal-label">在线节点</div>
              <div class="signal-value">{{ onlineNodeCount }} / {{ nodes.length }}</div>
            </div>
          </div>
          <div class="signal">
            <BoltIcon class="signal-icon" />
            <div>
              <div class="signal-label">队列负载</div>
              <div class="signal-value">{{ queueLoad }}%</div>
            </div>
          </div>
        </div>
      </div>

      <div class="side-card">
        <div class="side-card-title">运行概览</div>
        <div class="side-card-body">
          <div class="metric">
            <span>进行中任务</span>
            <strong>{{ overviewStats.runningTasks }}</strong>
          </div>
          <div class="metric">
            <span>今日完成</span>
            <strong>{{ overviewStats.finishedToday }}</strong>
          </div>
          <div class="metric">
            <span>成功率</span>
            <strong>{{ overviewStats.successRate }}%</strong>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main panel -->
    <div class="main-panel">
      <header class="topbar">
        <div class="topbar-left">
          <button class="hamburger-btn" @click="sidebarOpen = true">
            <Bars3Icon class="w-5 h-5" />
          </button>
          <div class="headline">
            <div class="headline-title">管理员运维中心</div>
            <div class="headline-sub">实时系统态势与节点调度</div>
          </div>
        </div>

        <!-- Search -->
        <el-popover
          :visible="searchOpen"
          trigger="manual"
          placement="bottom-start"
          :width="420"
          :offset="8"
          :show-arrow="false"
          popper-class="search-popover"
        >
          <template #reference>
            <div class="search-wrapper">
              <el-input
                v-model="searchQuery"
                placeholder="搜索节点、任务或日志"
                :prefix-icon="undefined"
                size="default"
                @focus="openSearch"
              />
            </div>
          </template>
          <div class="search-panel-content">
            <div class="search-header">
              <span>搜索结果</span>
              <el-button size="small" text @click="loadSearchData">刷新</el-button>
            </div>
            <div v-if="isLoadingSearch" class="search-empty">正在加载节点与日志…</div>
            <template v-else>
              <div class="search-section">
                <div class="search-label">节点</div>
                <div v-if="!searchResults.nodes.length" class="search-empty">未找到匹配节点</div>
                <button
                  v-for="node in searchResults.nodes"
                  :key="node.nodeId"
                  type="button"
                  class="search-item"
                  @click="handleNodeClick(node)"
                >
                  <div>{{ node.nodeId }}</div>
                  <div class="search-meta">状态: {{ node.status }}</div>
                </button>
              </div>
              <div class="search-section">
                <div class="search-label">日志</div>
                <div v-if="!searchResults.logs.length" class="search-empty">未找到匹配日志</div>
                <button
                  v-for="log in searchResults.logs"
                  :key="log.logId"
                  type="button"
                  class="search-item"
                  @click="handleLogClick(log)"
                >
                  <div>{{ log.logLevel }} · 任务 #{{ log.taskId }}</div>
                  <div class="search-meta">{{ log.logMessage }}</div>
                </button>
              </div>
            </template>
          </div>
        </el-popover>

        <div class="top-actions">
          <!-- Notifications -->
          <div class="notification-area">
            <el-popover
              :visible="notificationsOpen"
              trigger="manual"
              placement="bottom-end"
              :width="260"
              :offset="8"
              :show-arrow="false"
              popper-class="notify-popover"
            >
              <template #reference>
                <el-badge :value="unreadNotificationCount" :hidden="!unreadNotificationCount">
                  <button type="button" class="icon-button" @click="toggleNotifications">
                    <BellIcon class="icon" />
                  </button>
                </el-badge>
              </template>
              <div class="dropdown-title">
                通知中心
                <button
                  v-if="unreadCount > 0"
                  class="text-xs text-blue-500 hover:underline ml-2"
                  @click="readAllNotifications"
                >
                  全部已读
                </button>
              </div>
              <div v-if="!notificationItems.length" class="dropdown-empty">暂无消息</div>
              <button
                v-for="notice in notificationItems"
                :key="notice.notificationId"
                type="button"
                class="dropdown-item"
                :class="{ 'opacity-60': notice.isRead }"
                @click="readNotification(notice.notificationId!)"
              >
                <div class="flex items-center gap-1">
                  <span v-if="!notice.isRead" class="unread-dot" />
                  {{ notice.title }}
                </div>
                <div class="dropdown-meta">{{ notice.content }}</div>
              </button>
              <div class="dropdown-footer">
                <RouterLink to="/admin/notifications" class="text-xs text-blue-600 hover:underline"
                  @click="notificationsOpen = false"
                >
                  查看全部
                </RouterLink>
              </div>
            </el-popover>
          </div>

          <!-- User menu -->
          <el-dropdown trigger="click" placement="bottom-end" popper-class="user-dropdown">
            <button type="button" class="user-pill">
              <span class="avatar-initial">{{ userInitials }}</span>
              <div>
                <div class="user-name">{{ userName }}</div>
                <div class="user-role">管理员</div>
              </div>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/admin')">返回控制台</el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/nodes')">节点管理</el-dropdown-item>
                <el-dropdown-item @click="router.push('/admin/config')">系统配置</el-dropdown-item>
                <el-dropdown-item divided class="logout-item" @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Breadcrumbs -->
      <div v-if="breadcrumbs.length" class="breadcrumb-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item
            v-for="(item, idx) in breadcrumbs"
            :key="idx"
            :to="item.path ? { path: item.path } : undefined"
          >
            {{ item.label }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Space+Mono:wght@400;700&display=swap');

:global(body) {
  font-family: 'Space Grotesk', 'Space Mono', monospace;
}

.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  background: #f8fafc;
  color: #0f172a;
}

/* ── Sidebar ── */

.side-panel {
  border-right: 1px solid #e2e8f0;
  padding: 24px 18px;
  background: #ffffff;
  overflow-y: auto;
}

.sidebar-close-btn {
  display: none;
  border: none;
  background: none;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
  margin-left: auto;
}

.sidebar-overlay {
  display: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand-icon {
  width: 34px;
  height: 34px;
  color: #0e7490;
}

.brand-title {
  font-weight: 700;
  font-size: 17px;
}

.brand-sub {
  font-size: 12px;
  color: #64748b;
}

.nav-section {
  margin-bottom: 24px;
}

.nav-title {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #94a3b8;
  margin-bottom: 10px;
  padding-left: 4px;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  color: #475569;
  transition: all 0.15s ease;
}

.nav-item.active {
  background: #e0f2fe;
  color: #0e7490;
  box-shadow: 0 4px 12px rgba(14, 116, 144, 0.1);
}

.nav-item:hover {
  background: #f1f5f9;
}

.nav-icon {
  width: 19px;
  height: 19px;
}

.signal-grid {
  display: grid;
  gap: 8px;
}

.signal {
  display: flex;
  gap: 10px;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
}

.signal-icon {
  width: 18px;
  height: 18px;
  color: #0ea5e9;
}

.signal-label {
  font-size: 11px;
  color: #94a3b8;
}

.signal-value {
  font-size: 14px;
  font-weight: 600;
}

.side-card {
  margin-top: 20px;
  border-radius: 14px;
  padding: 16px;
  background: #0f172a;
  color: #f8fafc;
  font-size: 13px;
}

.side-card-title {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  opacity: 0.7;
  margin-bottom: 12px;
}

.side-card-body {
  display: grid;
  gap: 8px;
}

.metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric strong {
  font-weight: 600;
}

/* ── Main panel ── */

.main-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.hamburger-btn {
  display: none;
  border: none;
  background: none;
  color: #334155;
  cursor: pointer;
  padding: 4px;
}

/* ── Topbar ── */

.topbar {
  padding: 16px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
  gap: 20px;
  flex-wrap: wrap;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.headline-title {
  font-size: 18px;
  font-weight: 700;
}

.headline-sub {
  font-size: 12px;
  color: #94a3b8;
}

/* ── Search ── */

.search-wrapper {
  width: 360px;
  max-width: 100%;
}

.search-panel-content {
  padding: 4px 0;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #475569;
  margin-bottom: 8px;
  padding: 0 4px;
}

.search-section {
  margin-bottom: 10px;
}

.search-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #94a3b8;
  margin-bottom: 6px;
  padding: 0 4px;
}

.search-item {
  width: 100%;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 8px 10px;
  display: grid;
  gap: 4px;
  margin-bottom: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #1e293b;
  transition: background 0.15s;
}

.search-item:hover {
  background: #f1f5f9;
}

.search-meta {
  font-size: 12px;
  color: #64748b;
}

.search-empty {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 4px;
}

/* ── Top actions ── */

.top-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.notification-area {
  position: relative;
}

.icon-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.icon-button:hover {
  background: #f8fafc;
}

.icon {
  width: 18px;
  height: 18px;
  color: #475569;
}

/* ── User pill ── */

.user-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #0f172a;
  color: #f8fafc;
  padding: 6px 12px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-family: inherit;
}

.avatar-initial {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #38bdf8, #0e7490);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  text-align: left;
}

.user-role {
  font-size: 11px;
  color: #a5f3fc;
  text-align: left;
}

/* ── Dropdown shared ── */

.dropdown-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #94a3b8;
  margin-bottom: 8px;
  padding: 4px 4px 0;
}

.dropdown-item {
  width: 100%;
  text-align: left;
  background: #f8fafc;
  border: none;
  border-radius: 10px;
  padding: 8px 10px;
  margin-bottom: 6px;
  display: grid;
  gap: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #1e293b;
  transition: background 0.15s;
  font-family: inherit;
}

.dropdown-item:hover {
  background: #f1f5f9;
}

.dropdown-meta {
  font-size: 12px;
  color: #64748b;
}

.dropdown-empty {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 4px;
}

.dropdown-footer {
  text-align: center;
  padding: 8px 4px 4px;
  border-top: 1px solid #f1f5f9;
  margin-top: 4px;
}

.unread-dot {
  width: 6px;
  height: 6px;
  background: #3b82f6;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ── Logout item ── */

:deep(.logout-item) {
  color: #dc2626 !important;
}

/* ── Breadcrumb ── */

.breadcrumb-bar {
  padding: 12px 28px 0;
  background: #ffffff;
}

.breadcrumb-bar :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: #64748b;
}

.breadcrumb-bar :deep(.el-breadcrumb__inner.is-link) {
  color: #0e7490;
}

.breadcrumb-bar :deep(.el-breadcrumb__separator) {
  color: #cbd5e1;
}

/* ── Content ── */

.content {
  padding: 28px;
  min-width: 0;
}

/* ── Responsive ── */

@media (max-width: 1024px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .side-panel {
    position: fixed;
    inset: 0;
    z-index: 50;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    width: 280px;
  }

  .side-panel.open {
    transform: translateX(0);
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 49;
    background: rgba(0, 0, 0, 0.3);
  }

  .sidebar-close-btn {
    display: flex;
  }

  .hamburger-btn {
    display: flex;
  }

  .topbar {
    padding: 14px 20px;
  }

  .content {
    padding: 20px;
  }

  .search-wrapper {
    width: 220px;
  }
}

@media (max-width: 768px) {
  .topbar {
    gap: 12px;
  }

  .top-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .search-wrapper {
    flex: 1;
    width: auto;
  }

  .content {
    padding: 16px;
  }
}
</style>

<style>
/* Element Plus popper overrides — teleported outside scoped component */
.search-popover,
.notify-popover,
.user-dropdown {
  border-radius: 14px !important;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.12) !important;
  border: 1px solid #e2e8f0 !important;
  padding: 12px !important;
}

.notify-popover {
  max-height: 320px;
  overflow-y: auto;
}

.el-dropdown-menu {
  border-radius: 12px !important;
}
</style>
