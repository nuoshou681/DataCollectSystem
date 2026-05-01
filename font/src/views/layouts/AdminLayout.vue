<script setup lang="ts">
import {
  Squares2X2Icon,
  ServerStackIcon,
  DocumentIcon,
  QueueListIcon,
  ArrowDownTrayIcon,
  ChartBarIcon,
  HeartIcon,
  TrashIcon,
  ShieldCheckIcon,
  BellIcon,
  ClipboardDocumentCheckIcon,
  UsersIcon,
  Cog6ToothIcon,
} from '@heroicons/vue/24/outline'
import { BugAntIcon } from '@heroicons/vue/24/solid'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCrawlerNodes, fetchTaskLogs, fetchNotifications, fetchUnreadCount, markNotificationRead, markAllNotificationsRead } from '@/api/api'
import type { CrawlerNode, TaskLog, Notification } from '@/types/entity'
import { clearAuthSession, getCurrentUserProfile } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/admin', label: '控制台', icon: Squares2X2Icon },
  { path: '/admin/stats', label: '数据统计', icon: ChartBarIcon },
  { path: '/admin/task-center', label: '任务治理', icon: ClipboardDocumentCheckIcon },
  { path: '/admin/exports', label: '导出中心', icon: ArrowDownTrayIcon },
  { path: '/admin/nodes', label: '节点管理', icon: ServerStackIcon },
  { path: '/admin/batches', label: '任务批次', icon: QueueListIcon },
  { path: '/admin/health', label: '系统健康', icon: HeartIcon },
  { path: '/admin/cleanup', label: '数据清理', icon: TrashIcon },
  { path: '/admin/users', label: '用户管理', icon: UsersIcon },
  { path: '/admin/logs', label: '审计日志', icon: DocumentIcon },
  { path: '/admin/config', label: '系统配置', icon: Cog6ToothIcon },
]

function isPathActive(path: string) {
  if (path === '/admin') {
    return route.path === '/admin'
  }
  return route.path === path || route.path.startsWith(path + '/')
}

const searchQuery = ref('')
const searchOpen = ref(false)
const notificationsOpen = ref(false)
const userMenuOpen = ref(false)
const isLoadingSearch = ref(false)
const nodes = ref<CrawlerNode[]>([])
const logs = ref<TaskLog[]>([])
const lastSeenLogId = ref(Number(localStorage.getItem('admin.lastSeenLogId') || 0))

const userName = computed(() => {
  const storedName = localStorage.getItem('profileName')
  if (storedName) {
    return storedName
  }
  const profile = getCurrentUserProfile()
  return profile.username || profile.email || '管理员'
})

const searchResults = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (keyword.length < 2) {
    return { nodes: [], logs: [] }
  }
  const nodeMatches = nodes.value.filter((node) => {
    return node.nodeId.toLowerCase().includes(keyword) || node.status.toLowerCase().includes(keyword)
  })
  const logMatches = logs.value.filter((log) => {
    return (
      String(log.taskId).includes(keyword) ||
      String(log.nodeId ?? '').includes(keyword) ||
      log.logMessage?.toLowerCase().includes(keyword) ||
      log.logLevel?.toLowerCase().includes(keyword)
    )
  })
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
  if (isLoadingSearch.value) {
    return
  }
  isLoadingSearch.value = true
  try {
    const [nodeData, logData] = await Promise.all([fetchCrawlerNodes(), fetchTaskLogs()])
    nodes.value = nodeData
    logs.value = logData
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
  window.setTimeout(() => {
    searchOpen.value = false
  }, 120)
}

function toggleNotifications() {
  notificationsOpen.value = !notificationsOpen.value
  if (notificationsOpen.value) {
    void loadNotifications()
  }
}

async function readNotification(id: number) {
  await markNotificationRead(id)
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  notifications.value = notifications.value.map(n => n.notificationId === id ? { ...n, isRead: true } : n)
}

async function readAllNotifications() {
  await markAllNotificationsRead()
  unreadCount.value = 0
  notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function handleLogClick() {
  router.push('/admin/logs')
  searchOpen.value = false
}

function handleNodeClick() {
  router.push('/admin/nodes')
  searchOpen.value = false
}

function logout() {
  clearAuthSession()
  router.push('/login')
}

onMounted(() => {
  void loadSearchData()
})
</script>

<template>
  <div class="app-shell admin-shell">
    <aside class="side-panel">
      <div class="brand">
        <BugAntIcon class="brand-icon" />
        <div>
          <div class="brand-title">采集控制台</div>
          <div class="brand-sub">管理员空间</div>
        </div>
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
              <div class="signal-label">安全状态</div>
              <div class="signal-value">稳定</div>
            </div>
          </div>
          <div class="signal">
            <BoltIcon class="signal-icon" />
            <div>
              <div class="signal-label">队列负载</div>
              <div class="signal-value">72%</div>
            </div>
          </div>
        </div>
      </div>

      <div class="side-card">
        <div class="side-card-title">值班备忘</div>
        <div class="side-card-body">
          <div>• 高优先级任务: 2</div>
          <div>• 重试队列待处理: 5</div>
          <div>• 节点轮换倒计时: 45 分钟</div>
        </div>
      </div>
    </aside>

    <div class="main-panel">
      <header class="topbar">
        <div class="headline">
          <div class="headline-title">管理员运维中心</div>
          <div class="headline-sub">实时系统态势与节点调度</div>
        </div>
        <div class="search">
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索节点、任务或日志"
            @focus="openSearch"
            @blur="closeSearch"
            @keydown.enter.prevent="handleLogClick"
          />
          <div v-if="searchOpen" class="search-panel">
            <div class="search-header">
              <span>搜索结果</span>
              <button type="button" class="ghost-button" @click="loadSearchData">
                刷新
              </button>
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
                  @click="handleNodeClick"
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
                  @click="handleLogClick"
                >
                  <div>{{ log.logLevel }} · 任务 #{{ log.taskId }}</div>
                  <div class="search-meta">{{ log.logMessage }}</div>
                </button>
              </div>
            </template>
          </div>
        </div>
        <div class="top-actions">
          <div class="dropdown-wrap">
            <button type="button" class="icon-button" @click="toggleNotifications">
              <BellIcon class="icon" />
              <span v-if="unreadNotificationCount" class="badge">{{ unreadNotificationCount }}</span>
            </button>
            <div v-if="notificationsOpen" class="dropdown-panel">
              <div class="dropdown-title">
                通知中心
                <button v-if="unreadCount > 0" class="text-blue-400 text-xs ml-2 hover:underline" @click="readAllNotifications">全部已读</button>
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
                  <span v-if="!notice.isRead" class="w-1.5 h-1.5 bg-blue-400 rounded-full flex-shrink-0"></span>
                  {{ notice.title }}
                </div>
                <div class="dropdown-meta">{{ notice.content }}</div>
              </button>
            </div>
          </div>
          <div class="dropdown-wrap">
            <button type="button" class="user-pill" @click="toggleUserMenu">
            <img src="https://randomuser.me/api/portraits/men/32.jpg" alt="admin" />
            <div>
              <div class="user-name">{{ userName }}</div>
              <div class="user-role">管理员</div>
            </div>
            </button>
            <div v-if="userMenuOpen" class="dropdown-panel">
              <div class="dropdown-title">账号设置</div>
              <button type="button" class="dropdown-item" @click="router.push('/admin/profile')">
                个人资料
              </button>
              <button type="button" class="dropdown-item" @click="router.push('/admin')">
                返回控制台
              </button>
              <button type="button" class="dropdown-item" @click="router.push('/admin/nodes')">
                节点管理
              </button>
              <button type="button" class="dropdown-item danger" @click="logout">
                退出登录
              </button>
            </div>
          </div>
        </div>
      </header>

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
  grid-template-columns: 280px 1fr;
  background: radial-gradient(circle at 20% 0%, #111827 0%, #0b1120 38%, #05070f 100%);
  color: #e2e8f0;
}

.admin-shell .side-panel {
  border-right: 1px solid rgba(148, 163, 184, 0.2);
  padding: 28px 22px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.9) 0%, rgba(2, 6, 23, 0.95) 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 30px;
}

.brand-icon {
  width: 36px;
  height: 36px;
  color: #38bdf8;
}

.brand-title {
  font-weight: 700;
  font-size: 18px;
}

.brand-sub {
  font-size: 12px;
  color: #94a3b8;
}

.nav-section {
  margin-bottom: 24px;
}

.nav-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: #64748b;
  margin-bottom: 12px;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(30, 41, 59, 0.35);
  color: #e2e8f0;
  transition: all 0.2s ease;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.28), rgba(14, 116, 144, 0.5));
  box-shadow: 0 12px 24px rgba(14, 116, 144, 0.35);
}

.nav-item:hover {
  transform: translateX(2px);
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.signal-grid {
  display: grid;
  gap: 12px;
}

.signal {
  display: flex;
  gap: 10px;
  align-items: center;
  background: rgba(15, 23, 42, 0.8);
  border-radius: 12px;
  padding: 10px 12px;
}

.signal-icon {
  width: 20px;
  height: 20px;
  color: #22d3ee;
}

.signal-label {
  font-size: 12px;
  color: #94a3b8;
}

.signal-value {
  font-size: 14px;
  font-weight: 600;
}

.side-card {
  margin-top: 24px;
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(140deg, rgba(30, 64, 175, 0.6), rgba(14, 116, 144, 0.7));
  color: #f8fafc;
  font-size: 13px;
}

.side-card-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  margin-bottom: 10px;
}

.side-card-body {
  display: grid;
  gap: 6px;
}

.main-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  padding: 20px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(10px);
  gap: 20px;
  flex-wrap: wrap;
}

.headline-title {
  font-size: 20px;
  font-weight: 700;
}

.headline-sub {
  font-size: 13px;
  color: #94a3b8;
}

.search {
  flex: 1;
  max-width: 420px;
  position: relative;
}

.search-input {
  width: 100%;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  padding: 10px 16px;
  background: rgba(30, 41, 59, 0.8);
  color: #e2e8f0;
  font-size: 14px;
}

.search-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: min(420px, 70vw);
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(2, 6, 23, 0.6);
  padding: 14px;
  z-index: 10;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #cbd5f5;
  margin-bottom: 10px;
}

.search-section {
  margin-bottom: 12px;
}

.search-label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #94a3b8;
  margin-bottom: 6px;
}

.search-item {
  width: 100%;
  text-align: left;
  background: rgba(30, 41, 59, 0.7);
  border: none;
  border-radius: 12px;
  padding: 8px 10px;
  display: grid;
  gap: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  color: #e2e8f0;
}

.search-meta {
  font-size: 12px;
  color: #94a3b8;
}

.search-empty {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 4px;
}

.ghost-button {
  border: none;
  background: rgba(148, 163, 184, 0.2);
  color: #e2e8f0;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.dropdown-wrap {
  position: relative;
}

.icon-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(30, 41, 59, 0.8);
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  width: 18px;
  height: 18px;
  color: #e2e8f0;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(30, 41, 59, 0.9);
  color: #f8fafc;
  padding: 6px 12px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
}

.user-pill img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
}

.user-role {
  font-size: 11px;
  color: #7dd3fc;
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #ffffff;
  font-size: 10px;
  border-radius: 999px;
  padding: 2px 6px;
}

.dropdown-panel {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
  width: 240px;
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 20px 36px rgba(2, 6, 23, 0.6);
  z-index: 10;
  max-height: 320px;
  overflow-y: auto;
}

.dropdown-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #94a3b8;
  margin-bottom: 8px;
}

.dropdown-item {
  width: 100%;
  text-align: left;
  background: rgba(30, 41, 59, 0.7);
  border: none;
  border-radius: 12px;
  padding: 8px 10px;
  margin-bottom: 8px;
  display: grid;
  gap: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #e2e8f0;
  word-break: break-all;
}

.dropdown-item.danger {
  background: rgba(248, 113, 113, 0.2);
  color: #fecaca;
}

.dropdown-meta {
  font-size: 12px;
  color: #94a3b8;
  word-break: break-all;
}

.dropdown-empty {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 4px;
}

.content {
  padding: 28px;
  min-width: 0;
}

@media (max-width: 1024px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: none;
  }

  .topbar {
    padding: 18px 20px;
  }

  .search {
    max-width: 100%;
  }

  .content {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .top-actions {
    width: 100%;
    justify-content: space-between;
  }

  .search {
    width: 100%;
  }

  .search-panel {
    width: min(100%, 92vw);
  }

  .content {
    padding: 16px;
  }
}
</style>
