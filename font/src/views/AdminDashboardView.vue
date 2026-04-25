<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCrawlerNodes, fetchTaskLogs, fetchTasks } from '@/api/api'
import type { CrawlerNode, Task, TaskLog } from '@/types/entity'
import { deriveNodeStatus } from '@/utils/task'

const loading = ref(false)
const nodes = ref<CrawlerNode[]>([])
const tasks = ref<Task[]>([])
const logs = ref<TaskLog[]>([])

async function loadDashboard() {
  loading.value = true
  try {
    const [nodeData, taskData, logData] = await Promise.all([fetchCrawlerNodes(), fetchTasks(), fetchTaskLogs()])
    nodes.value = nodeData
    tasks.value = taskData
    logs.value = logData
  } catch {
    ElMessage.error('控制台数据加载失败')
  } finally {
    loading.value = false
  }
}

const highlights = computed(() => {
  const runtimes = tasks.value.map(task => task.runtime).filter(Boolean)
  return [
    { label: '任务总量', value: String(tasks.value.length), note: '主任务表' },
    { label: '运行中', value: String(runtimes.filter(runtime => runtime?.status === 'RUNNING').length), note: 'task_runtime' },
    { label: '在线节点', value: String(nodes.value.filter(node => ['ONLINE', 'IDLE'].includes(deriveNodeStatus(node).displayStatus)).length), note: 'crawler' },
    { label: '错误事件', value: String(logs.value.filter(log => log.logLevel === 'ERROR').length), note: 'task_event/task_log 镜像' },
  ]
})

const queueBands = computed(() => {
  const total = Math.max(tasks.value.length, 1)
  const pending = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'PENDING').length
  const running = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'RUNNING').length
  const failed = tasks.value.filter(task => ['FAILED', 'PARTIAL_FAILED'].includes(task.runtime?.status ?? task.taskStatus)).length
  return [
    { label: '排队中', value: Math.round((pending / total) * 100), color: 'bg-slate-500' },
    { label: '执行中', value: Math.round((running / total) * 100), color: 'bg-sky-500' },
    { label: '异常', value: Math.round((failed / total) * 100), color: 'bg-rose-500' },
  ]
})

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="space-y-8">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card v-for="item in highlights" :key="item.label" class="shadow-sm" v-loading="loading">
        <div class="text-sm text-gray-500">{{ item.label }}</div>
        <div class="text-2xl font-semibold mt-2 text-slate-800">{{ item.value }}</div>
        <div class="text-xs text-gray-400 mt-1">{{ item.note }}</div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <el-card class="xl:col-span-2">
        <template #header><div class="flex items-center justify-between"><span class="font-semibold">运行态健康度</span><el-button text type="primary" @click="loadDashboard">刷新</el-button></div></template>
        <div class="grid grid-cols-3 gap-4">
          <div v-for="band in queueBands" :key="band.label" class="rounded-xl border border-slate-100 p-4">
            <div class="text-xs text-gray-500">{{ band.label }}</div>
            <div class="text-xl font-semibold mt-2">{{ band.value }}%</div>
            <div class="mt-3 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full" :class="band.color" :style="{ width: `${band.value}%` }" /></div>
          </div>
        </div>
      </el-card>
      <el-card>
        <template #header><span class="font-semibold">最近异常</span></template>
        <div class="space-y-3">
          <div v-for="log in logs.slice(0, 4)" :key="log.logId" class="rounded-xl border border-slate-100 p-3">
            <div class="flex items-center justify-between">
              <div class="font-medium">任务 #{{ log.taskId }}</div>
              <el-tag :type="log.logLevel === 'ERROR' ? 'danger' : log.logLevel === 'WARN' ? 'warning' : 'info'" size="small">{{ log.logLevel }}</el-tag>
            </div>
            <div class="text-xs text-gray-500 mt-2">{{ log.logMessage }}</div>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>
