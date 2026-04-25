<script setup lang="ts">
import {
  HomeIcon,
  RectangleStackIcon,
  ArchiveBoxIcon,
  MagnifyingGlassIcon,
  BellIcon,
  RocketLaunchIcon,
  SparklesIcon,
} from '@heroicons/vue/24/outline'
import { BugAntIcon } from '@heroicons/vue/24/solid'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPageResults, fetchTasks } from '@/api/api'
import type { CrawlerPageResult, Task } from '@/types/entity'
import { clearAuthSession, getCurrentUserProfile } from '@/utils/auth'
import { normalizeTaskStatus } from '@/utils/task'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/', label: '工作台概览', icon: HomeIcon },
  { path: '/tasks', label: '我的任务', icon: RectangleStackIcon },
  { path: '/results', label: '结果中心', icon: ArchiveBoxIcon },
]

function isPathActive(path: string) {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path === path || route.path.startsWith(path + '/')
}

const searchQuery = ref('')
const searchOpen = ref(false)
const notificationsOpen = ref(false)
const userMenuOpen = ref(false)
const isLoadingSearch = ref(false)

const tasks = ref<Task[]>([])
const pageResults = ref<CrawlerPageResult[]>([])
const lastSeenPageResultId = ref(Number(localStorage.getItem('user.lastSeenPageResultId') || 0))

// 进行中任务数量，支持 taskStatus 为 '进行中' 或 'running'（兼容中英文）
const runningTaskCount = computed(() => {
  return tasks.value.filter(t => normalizeTaskStatus(t.taskStatus) === 'RUNNING').length
})

const userName = computed(() => {
  const storedName = localStorage.getItem('profileName')
  if (storedName) {
    return storedName
  }
  const profile = getCurrentUserProfile()
  return profile.username || profile.email || '用户'
})

const searchResults = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  if (keyword.length < 2) {
    return { tasks: [], pages: [] }
  }
  const taskMatches = tasks.value.filter((task) => {
    return (
      String(task.taskId).includes(keyword) ||
      task.keyword?.toLowerCase().includes(keyword) ||
      task.url?.toLowerCase().includes(keyword)
    )
  })
  const pageMatches = pageResults.value.filter((page) => {
    return (
      String(page.pageResultId).includes(keyword) ||
      page.pageUrl?.toLowerCase().includes(keyword) ||
      page.pageTitle?.toLowerCase().includes(keyword)
    )
  })
  return { tasks: taskMatches.slice(0, 6), pages: pageMatches.slice(0, 6) }
})

const notificationItems = computed(() => {
  const sorted = [...pageResults.value].sort((a, b) => (b.pageResultId ?? 0) - (a.pageResultId ?? 0))
  return sorted.slice(0, 5)
})

const unreadNotificationCount = computed(() => {
  return notificationItems.value.filter((item) => (item.pageResultId ?? 0) > lastSeenPageResultId.value).length
})

async function loadSearchData() {
  if (isLoadingSearch.value) {
    return
  }
  isLoadingSearch.value = true
  try {
    const [taskData, pageData] = await Promise.all([fetchTasks(), fetchPageResults()])
    tasks.value = taskData
    pageResults.value = pageData
  } finally {
    isLoadingSearch.value = false
  }
}

function openSearch() {
  searchOpen.value = true
  if (!tasks.value.length && !pageResults.value.length) {
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
  if (notificationsOpen.value && !pageResults.value.length) {
    void loadSearchData()
  }
  if (notificationsOpen.value && notificationItems.value.length) {
    const latestId = notificationItems.value[0]?.pageResultId ?? 0
    if (latestId > lastSeenPageResultId.value) {
      lastSeenPageResultId.value = latestId
      localStorage.setItem('user.lastSeenPageResultId', String(latestId))
    }
  }
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function handleTaskClick() {
  const keyword = searchQuery.value.trim()
  router.push({ path: '/tasks', query: keyword ? { q: keyword } : {} })
  searchOpen.value = false
}

function handlePageResultClick(url?: string) {
  if (!url) {
    return
  }
  window.open(url, '_blank', 'noopener')
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
  <div class="app-shell user-shell">
    <aside class="side-panel">
      <div class="brand">
        <BugAntIcon class="brand-icon" />
        <div>
          <div class="brand-title">采集工作台</div>
          <div class="brand-sub">用户空间</div>
        </div>
      </div>

      <div class="nav-section">
        <div class="nav-title">导航</div>
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
        <div class="nav-title">快捷操作</div>
        <div class="quick-actions">
          <button class="chip">
            <RocketLaunchIcon class="chip-icon" />
            新建任务批次
          </button>
          <button class="chip">
            <SparklesIcon class="chip-icon" />
            保存关键词组
          </button>
        </div>
      </div>

      <div class="side-card">
        <div class="side-card-title">今日关注</div>
        <div class="side-card-body">
          <div class="metric">
            <span>进行中任务</span>
            <strong>{{ runningTaskCount }}</strong>
          </div>
          <div class="metric">
            <span>下次刷新</span>
            <strong>2 min</strong>
          </div>
        </div>
      </div>
    </aside>

    <div class="main-panel">
      <header class="topbar">
        <div class="search">
          <MagnifyingGlassIcon class="search-icon" />
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="搜索任务、关键词、链接"
            @focus="openSearch"
            @blur="closeSearch"
            @keydown.enter.prevent="handleTaskClick"
          />
          <div v-if="searchOpen" class="search-panel">
            <div class="search-header">
              <span>搜索结果</span>
              <button type="button" class="ghost-button" @click="loadSearchData">
                刷新
              </button>
            </div>
            <div v-if="isLoadingSearch" class="search-empty">正在加载任务与结果…</div>
            <template v-else>
              <div class="search-section">
                <div class="search-label">任务</div>
                <div v-if="!searchResults.tasks.length" class="search-empty">未找到匹配任务</div>
                <button
                  v-for="task in searchResults.tasks"
                  :key="task.taskId"
                  type="button"
                  class="search-item"
                  @click="handleTaskClick"
                >
                  <div>任务 #{{ task.taskId }}</div>
                  <div class="search-meta">{{ task.keyword || task.url }}</div>
                </button>
              </div>
              <div class="search-section">
                <div class="search-label">页面结果</div>
                <div v-if="!searchResults.pages.length" class="search-empty">未找到匹配页面</div>
                <button
                  v-for="page in searchResults.pages"
                  :key="page.pageResultId"
                  type="button"
                  class="search-item"
                  @click="handlePageResultClick(page.pageUrl)"
                >
                  <div>结果 #{{ page.pageResultId }}</div>
                  <div class="search-meta">{{ page.pageUrl }}</div>
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
              <div class="dropdown-title">最新通知</div>
              <div v-if="!notificationItems.length" class="dropdown-empty">暂无新的页面结果</div>
              <button
                v-for="notice in notificationItems"
                :key="notice.pageResultId"
                type="button"
                class="dropdown-item"
                @click="handlePageResultClick(notice.pageUrl)"
              >
                <div>任务 #{{ notice.taskId }} · 结果 #{{ notice.pageResultId }}</div>
                <div class="dropdown-meta">{{ notice.pageUrl }}</div>
              </button>
            </div>
          </div>
          <div class="dropdown-wrap">
            <button type="button" class="user-pill" @click="toggleUserMenu">
            <img src="https://randomuser.me/api/portraits/men/32.jpg" alt="user" />
            <div>
              <div class="user-name">{{ userName }}</div>
              <div class="user-role">用户</div>
            </div>
            </button>
            <div v-if="userMenuOpen" class="dropdown-panel">
              <div class="dropdown-title">账号设置</div>
              <button type="button" class="dropdown-item" @click="router.push('/profile')">
                个人资料
              </button>
              <button type="button" class="dropdown-item" @click="router.push('/tasks')">
                进入任务中心
              </button>
              <button type="button" class="dropdown-item" @click="router.push('/')">
                返回工作台
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
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Newsreader:wght@400;600&display=swap');

:global(body) {
  font-family: 'Space Grotesk', 'Newsreader', sans-serif;
}

.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  background: radial-gradient(circle at 12% 12%, #f0f7ff 0%, #f8fbff 40%, #ffffff 100%);
  color: #0f172a;
}

.user-shell .side-panel {
  border-right: 1px solid #e2e8f0;
  padding: 24px 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f7f9fc 100%);
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
  color: #1d4ed8;
}

.brand-title {
  font-weight: 700;
  font-size: 18px;
}

.brand-sub {
  font-size: 12px;
  color: #64748b;
}

.nav-section {
  margin-bottom: 24px;
}

.nav-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #94a3b8;
  margin-bottom: 12px;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  font-weight: 500;
  color: #334155;
  transition: all 0.2s ease;
  background: transparent;
}

.nav-item.active {
  background: #e0edff;
  color: #1d4ed8;
  box-shadow: 0 8px 20px rgba(29, 78, 216, 0.12);
}

.nav-item:hover {
  background: #f1f5f9;
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.quick-actions {
  display: grid;
  gap: 8px;
}

.chip {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e2e8f0;
  padding: 8px 10px;
  border-radius: 999px;
  font-size: 13px;
  color: #1e293b;
  background: #ffffff;
}

.chip-icon {
  width: 16px;
  height: 16px;
  color: #0ea5e9;
}

.side-card {
  margin-top: 28px;
  border-radius: 16px;
  padding: 16px;
  background: #0f172a;
  color: #f8fafc;
}

.side-card-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  opacity: 0.7;
}

.side-card-body {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.metric {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.main-panel {
  display: flex;
  flex-direction: column;
}

.topbar {
  padding: 20px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f1f5f9;
  padding: 8px 12px;
  border-radius: 999px;
  width: min(420px, 60vw);
  position: relative;
}

.search-input {
  border: none;
  outline: none;
  background: transparent;
  width: 100%;
  font-size: 14px;
}

.search-icon {
  width: 18px;
  height: 18px;
  color: #64748b;
}

.search-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  width: min(420px, 70vw);
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
  padding: 14px;
  z-index: 10;
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #475569;
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
  background: #f8fafc;
  border: none;
  border-radius: 12px;
  padding: 8px 10px;
  display: grid;
  gap: 4px;
  margin-bottom: 8px;
  cursor: pointer;
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

.ghost-button {
  border: none;
  background: #e2e8f0;
  color: #1e293b;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dropdown-wrap {
  position: relative;
}

.icon-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: #f8fafc;
  position: relative;
  cursor: pointer;
}

.icon {
  width: 18px;
  height: 18px;
  color: #0f172a;
}

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
  color: #cbd5f5;
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
  background: #ffffff;
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 20px 36px rgba(15, 23, 42, 0.14);
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
  background: #f8fafc;
  border: none;
  border-radius: 12px;
  padding: 8px 10px;
  margin-bottom: 8px;
  display: grid;
  gap: 4px;
  cursor: pointer;
  font-size: 13px;
  word-break: break-all;
}

.dropdown-item.danger {
  background: #fee2e2;
  color: #991b1b;
}

.dropdown-meta {
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.dropdown-empty {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 4px;
}

.content {
  padding: 28px;
}

@media (max-width: 1024px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: none;
  }
}
</style>
