<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BellAlertIcon, BellIcon, CheckBadgeIcon, EnvelopeIcon, EnvelopeOpenIcon } from '@heroicons/vue/24/outline'
import {
  deleteNotification,
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/api'
import type { Notification } from '@/types/entity'

const notifications = ref<Notification[]>([])
const loading = ref(false)
const filterRead = ref<'ALL' | 'UNREAD' | 'READ'>('ALL')
const totalUnread = ref(0)

async function loadAll() {
  loading.value = true
  try {
    const [data, unread] = await Promise.all([fetchNotifications(), fetchUnreadCount()])
    notifications.value = Array.isArray(data) ? data : []
    totalUnread.value = unread ?? 0
  } catch {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const filteredNotifications = computed(() => {
  if (filterRead.value === 'UNREAD') return notifications.value.filter(n => !n.isRead)
  if (filterRead.value === 'READ') return notifications.value.filter(n => n.isRead)
  return notifications.value
})

const stats = computed(() => ({
  total: notifications.value.length,
  unread: notifications.value.filter(n => !n.isRead).length,
  read: notifications.value.filter(n => n.isRead).length,
}))

async function handleRead(notificationId: number) {
  await markNotificationRead(notificationId)
  const n = notifications.value.find(item => item.notificationId === notificationId)
  if (n) { n.isRead = true }
  if (totalUnread.value > 0) totalUnread.value--
}

async function handleReadAll() {
  await markAllNotificationsRead()
  notifications.value.forEach(n => { n.isRead = true })
  totalUnread.value = 0
  ElMessage.success('已全部标记为已读')
}

async function handleDelete(notificationId: number) {
  try {
    await ElMessageBox.confirm('确定删除该通知吗？', '删除确认', { type: 'warning' })
    await deleteNotification(notificationId)
    notifications.value = notifications.value.filter(n => n.notificationId !== notificationId)
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

function levelTagType(level?: string) {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN' || level === 'WARNING') return 'warning'
  return 'info'
}

function formatTime(raw?: string) {
  if (!raw) return '-'
  const d = new Date(raw)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(loadAll)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">通知中心</h2>
        <p class="text-sm text-slate-500 mt-1">系统通知、告警与运维消息</p>
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" :disabled="!stats.unread" @click="handleReadAll">
          <EnvelopeOpenIcon class="w-4 h-4 mr-1" />
          全部已读
        </el-button>
        <el-button size="small" text type="primary" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- Stats -->
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500 cursor-pointer"
        :class="filterRead === 'ALL' ? 'bg-blue-50 ring-1 ring-blue-200' : ''"
        @click="filterRead = 'ALL'"
      >
        <div class="flex items-center gap-3">
          <BellIcon class="w-5 h-5 text-blue-500" />
          <div>
            <div class="text-xs text-slate-400">全部通知</div>
            <div class="text-xl font-bold text-blue-600">{{ stats.total }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500 cursor-pointer"
        :class="filterRead === 'UNREAD' ? 'bg-amber-50 ring-1 ring-amber-200' : ''"
        @click="filterRead = filterRead === 'UNREAD' ? 'ALL' : 'UNREAD'"
      >
        <div class="flex items-center gap-3">
          <BellAlertIcon class="w-5 h-5 text-amber-500" />
          <div>
            <div class="text-xs text-slate-400">未读</div>
            <div class="text-xl font-bold text-amber-600">{{ stats.unread }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500 cursor-pointer"
        :class="filterRead === 'READ' ? 'bg-emerald-50 ring-1 ring-emerald-200' : ''"
        @click="filterRead = filterRead === 'READ' ? 'ALL' : 'READ'"
      >
        <div class="flex items-center gap-3">
          <EnvelopeOpenIcon class="w-5 h-5 text-emerald-500" />
          <div>
            <div class="text-xs text-slate-400">已读</div>
            <div class="text-xl font-bold text-emerald-600">{{ stats.read }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <!-- Notification list -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="font-semibold">通知列表</span>
            <el-tag size="small" type="info">{{ filteredNotifications.length }} 条</el-tag>
          </div>
          <div class="flex items-center gap-2">
            <el-tag
              v-for="opt in [{ label: '全部', value: 'ALL' }, { label: '未读', value: 'UNREAD' }, { label: '已读', value: 'READ' }]"
              :key="opt.value"
              size="small"
              class="cursor-pointer"
              :type="filterRead === opt.value ? '' : 'info'"
              :effect="filterRead === opt.value ? 'dark' : 'plain'"
              @click="filterRead = opt.value as any"
            >
              {{ opt.label }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-table :data="filteredNotifications" border stripe v-loading="loading" highlight-current-row
        :row-class-name="({ row }: { row: Notification }) => row.isRead ? '' : 'unread-row'"
      >
        <el-table-column prop="notificationId" label="ID" width="80" align="center" />
        <el-table-column label="" width="36">
          <template #default="scope">
            <span v-if="!scope.row.isRead" class="w-2 h-2 bg-blue-500 rounded-full inline-block" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'font-semibold': !scope.row.isRead }">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip>
          <template #default="scope">
            <span class="text-sm" :class="scope.row.isRead ? 'text-slate-500' : 'text-slate-800'">
              {{ scope.row.content }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="90" align="center">
          <template #default="scope">
            <el-tag :type="levelTagType(scope.row.level)" size="small" effect="dark">
              {{ scope.row.level || 'INFO' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="scope">
            <span class="text-xs text-slate-500">{{ scope.row.type || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="150">
          <template #default="scope">
            <span class="text-sm text-slate-600">{{ formatTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="scope">
            <div class="flex items-center gap-1 justify-center">
              <el-button
                v-if="!scope.row.isRead"
                size="small" text type="primary"
                @click="handleRead(scope.row.notificationId!)"
              >
                已读
              </el-button>
              <el-button
                size="small" text type="danger"
                @click="handleDelete(scope.row.notificationId!)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无通知" :image-size="80" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
:deep(.unread-row) {
  background-color: #f0f9ff !important;
}
</style>
