<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchCleanupStats, runCleanup } from '@/api/api'
import type { CleanupStats } from '@/types/entity'

const stats = ref<CleanupStats | null>(null)
const days = ref(30)
const loading = ref(false)
const cleaning = ref(false)

async function load() {
  loading.value = true
  try {
    stats.value = await fetchCleanupStats()
  } catch {
    ElMessage.error('获取统计失败')
  } finally {
    loading.value = false
  }
}

async function handleCleanup() {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${days.value} 天前的归档任务、日志、事件和通知吗？此操作不可恢复！`,
      '数据清理确认',
      { type: 'warning', confirmButtonText: '确认清理', cancelButtonText: '取消' }
    )
    cleaning.value = true
    const result = await runCleanup(days.value)
    if (result) {
      ElMessage.success(`清理完成: 归档任务${result.deletedArchivedTasks ?? 0}条, 日志${result.deletedLogs ?? 0}条, 事件${result.deletedEvents ?? 0}条, 通知${result.deletedNotifications ?? 0}条`)
    }
    load()
  } catch {
    // cancelled
  } finally {
    cleaning.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-white">数据清理</h1>

    <div v-if="stats" class="grid grid-cols-6 gap-3">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4 text-center">
        <div class="text-xl font-bold text-blue-400">{{ stats.totalTasks }}</div>
        <div class="text-gray-500 text-xs mt-1">总任务</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4 text-center">
        <div class="text-xl font-bold text-violet-400">{{ stats.totalPageResults }}</div>
        <div class="text-gray-500 text-xs mt-1">页面结果</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4 text-center">
        <div class="text-xl font-bold text-amber-400">{{ stats.totalLogs }}</div>
        <div class="text-gray-500 text-xs mt-1">日志</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4 text-center">
        <div class="text-xl font-bold text-emerald-400">{{ stats.totalEvents }}</div>
        <div class="text-gray-500 text-xs mt-1">事件</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4 text-center">
        <div class="text-xl font-bold text-pink-400">{{ stats.totalNotifications }}</div>
        <div class="text-gray-500 text-xs mt-1">通知</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4 text-center">
        <div class="text-xl font-bold text-red-400">{{ stats.archivedTasks }}</div>
        <div class="text-gray-500 text-xs mt-1">已归档</div>
      </div>
    </div>

    <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-6">
      <h3 class="text-sm font-medium text-gray-300 mb-4">清理设置</h3>
      <div class="flex items-end gap-4">
        <div>
          <label class="text-gray-400 text-sm block mb-1">清理 {{ days }} 天前的数据</label>
          <input v-model.number="days" type="range" min="1" max="365" class="w-64">
        </div>
        <button
          class="bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white px-6 py-2 rounded-lg text-sm"
          :disabled="cleaning"
          @click="handleCleanup"
        >
          {{ cleaning ? '清理中...' : '执行清理' }}
        </button>
      </div>
      <p class="text-gray-500 text-xs mt-3">将删除 {{ days }} 天前的已归档任务、系统日志、任务事件和旧通知。活跃任务不受影响。</p>
    </div>
  </div>
</template>
