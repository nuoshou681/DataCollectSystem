<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { BoltIcon, CheckBadgeIcon, ExclamationTriangleIcon, QueueListIcon, InboxIcon } from '@heroicons/vue/24/outline'
import { fetchTasks, getTaskRuntimeStreamUrl } from '@/api/api'
import type { Task, TaskRuntime } from '@/types/entity'

const tasks = ref<Task[]>([])
const loading = ref(false)
let runtimeStream: EventSource | null = null

async function loadDashboard() {
  loading.value = true
  try {
    tasks.value = await fetchTasks()
  } catch {
    ElMessage.error('仪表盘数据加载失败')
  } finally {
    loading.value = false
  }
}

function connectRuntimeStream() {
  runtimeStream?.close()
  runtimeStream = new EventSource(getTaskRuntimeStreamUrl())
  runtimeStream.addEventListener('task-runtime', event => {
    try {
      const runtime = JSON.parse((event as MessageEvent).data) as TaskRuntime
      const task = tasks.value.find(item => item.taskId === runtime.taskId)
      if (task) {
        task.runtime = { ...(task.runtime ?? {} as TaskRuntime), ...runtime }
        task.taskStatus = runtime.status
      }
    } catch {
      // ignore malformed events
    }
  })
}

const highlights = computed(() => {
  const runtimes = tasks.value.map(task => task.runtime).filter(Boolean)
  const running = runtimes.filter(r => r?.status === 'RUNNING').length
  const successPages = runtimes.reduce((sum, runtime) => sum + (runtime?.successPages ?? 0), 0)
  const failedPages = runtimes.reduce((sum, runtime) => sum + (runtime?.failedPages ?? 0), 0)
  return [
    { label: '任务总数', value: tasks.value.length, icon: QueueListIcon, color: 'border-l-blue-500', iconColor: 'text-blue-500' },
    { label: '运行中', value: running, icon: BoltIcon, color: 'border-l-amber-500', iconColor: 'text-amber-500' },
    { label: '成功页面', value: successPages, icon: CheckBadgeIcon, color: 'border-l-emerald-500', iconColor: 'text-emerald-500' },
    { label: '失败页面', value: failedPages, icon: ExclamationTriangleIcon, color: 'border-l-rose-500', iconColor: 'text-rose-500' },
  ]
})

onMounted(() => {
  void loadDashboard()
  connectRuntimeStream()
})

onBeforeUnmount(() => {
  runtimeStream?.close()
})
</script>

<template>
  <div class="space-y-8">
    <section class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <el-card v-for="item in highlights" :key="item.label" shadow="hover" class="border-l-4" :class="item.color" v-loading="loading">
        <div class="flex items-center gap-3">
          <component :is="item.icon" class="w-6 h-6" :class="item.iconColor" />
          <div>
            <div class="text-sm text-slate-500">{{ item.label }}</div>
            <div class="text-2xl font-bold mt-0.5 text-slate-800">{{ item.value }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <el-card>
      <template #header><span class="font-semibold">运行态摘要</span></template>
      <div v-if="tasks.length" class="space-y-3">
        <div v-for="task in tasks.slice(0, 6)" :key="task.taskId" class="rounded-xl border border-slate-100 p-3 hover:border-slate-200 transition-colors cursor-pointer" @click="$router.push(`/tasks/${task.taskId}`)">
          <div class="flex items-center justify-between">
            <div class="font-medium text-sm">#{{ task.taskId }} · {{ task.keyword }}</div>
            <el-tag size="small" :type="task.runtime?.status === 'FINISHED' ? 'success' : task.runtime?.status === 'FAILED' ? 'danger' : 'info'">{{ task.runtime?.status ?? task.taskStatus }}</el-tag>
          </div>
          <div class="text-xs text-slate-500 mt-2">
            node={{ task.runtime?.assignedNodeId || task.nodeId || '-' }}
            · 进度 {{ task.runtime?.progressPercent ?? task.taskProgress }}%
            · 成功 {{ task.runtime?.successPages ?? 0 }} / 失败 {{ task.runtime?.failedPages ?? 0 }}
          </div>
        </div>
      </div>
      <div v-else class="text-center py-12">
        <InboxIcon class="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <div class="text-slate-500">还没有采集任务</div>
        <el-button type="primary" class="mt-4" @click="$router.push('/tasks/create')">创建第一个任务</el-button>
      </div>
    </el-card>
  </div>
</template>
