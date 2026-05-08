<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArchiveBoxIcon, BellIcon, BoltIcon, DocumentIcon, DocumentTextIcon, QueueListIcon } from '@heroicons/vue/24/outline'
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
    <div>
      <h2 class="text-xl font-semibold text-slate-800">数据清理</h2>
      <p class="text-sm text-slate-500 mt-1">清理过期归档任务、日志、事件和通知</p>
    </div>

    <section class="grid grid-cols-3 md:grid-cols-6 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500" v-if="stats">
        <div class="flex items-center gap-3"><QueueListIcon class="w-5 h-5 text-blue-500" /><div><div class="text-sm text-slate-500">总任务</div><div class="text-xl font-bold text-blue-600">{{ stats.totalTasks }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-violet-500" v-if="stats">
        <div class="flex items-center gap-3"><DocumentTextIcon class="w-5 h-5 text-violet-500" /><div><div class="text-sm text-slate-500">页面结果</div><div class="text-xl font-bold text-violet-600">{{ stats.totalPageResults }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500" v-if="stats">
        <div class="flex items-center gap-3"><DocumentIcon class="w-5 h-5 text-amber-500" /><div><div class="text-sm text-slate-500">日志</div><div class="text-xl font-bold text-amber-600">{{ stats.totalLogs }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500" v-if="stats">
        <div class="flex items-center gap-3"><BoltIcon class="w-5 h-5 text-emerald-500" /><div><div class="text-sm text-slate-500">事件</div><div class="text-xl font-bold text-emerald-600">{{ stats.totalEvents }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-pink-400" v-if="stats">
        <div class="flex items-center gap-3"><BellIcon class="w-5 h-5 text-pink-400" /><div><div class="text-sm text-slate-500">通知</div><div class="text-xl font-bold" style="color:#f472b6">{{ stats.totalNotifications }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-red-500" v-if="stats">
        <div class="flex items-center gap-3"><ArchiveBoxIcon class="w-5 h-5 text-red-500" /><div><div class="text-sm text-slate-500">已归档</div><div class="text-xl font-bold text-red-600">{{ stats.archivedTasks }}</div></div></div>
      </el-card>
    </section>

    <el-card>
      <template #header><span class="font-semibold">清理设置</span></template>
      <div class="flex flex-wrap items-end gap-4">
        <div>
          <div class="text-sm text-slate-500 mb-2">清理 {{ days }} 天前的数据</div>
          <el-slider v-model="days" :min="1" :max="365" show-input style="width:320px" />
        </div>
        <el-button type="danger" :loading="cleaning" @click="handleCleanup">
          {{ cleaning ? '清理中...' : '执行清理' }}
        </el-button>
      </div>
      <p class="text-xs text-slate-400 mt-3">将删除 {{ days }} 天前的已归档任务、系统日志、任务事件和旧通知。活跃任务不受影响。</p>
    </el-card>
  </div>
</template>
