<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchTasks } from '@/api/api'
import type { Task } from '@/types/entity'

const tasks = ref<Task[]>([])
const loading = ref(false)

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

const highlights = computed(() => {
  const runtimes = tasks.value.map(task => task.runtime).filter(Boolean)
  const successPages = runtimes.reduce((sum, runtime) => sum + (runtime?.successPages ?? 0), 0)
  const failedPages = runtimes.reduce((sum, runtime) => sum + (runtime?.failedPages ?? 0), 0)
  return [
    { label: '任务总数', value: String(tasks.value.length), note: '累计任务' },
    { label: '运行中任务', value: String(runtimes.filter(runtime => runtime?.status === 'RUNNING').length), note: '实时运行态' },
    { label: '成功页面', value: String(successPages), note: '累计抓取成功' },
    { label: '失败页面', value: String(failedPages), note: '累计抓取失败' },
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

    <el-card>
      <template #header><span class="font-semibold">运行态摘要</span></template>
      <div class="space-y-3">
        <div v-for="task in tasks.slice(0, 6)" :key="task.taskId" class="rounded-xl border border-slate-100 p-3">
          <div class="flex items-center justify-between">
            <div class="font-medium">任务 #{{ task.taskId }} · {{ task.keyword }}</div>
            <el-tag size="small">{{ task.runtime?.status ?? task.taskStatus }}</el-tag>
          </div>
          <div class="text-xs text-gray-500 mt-2">
            node={{ task.runtime?.assignedNodeId || task.nodeId || '-' }} ·
            progress={{ task.runtime?.progressPercent ?? task.taskProgress }}% ·
            success={{ task.runtime?.successPages ?? 0 }} ·
            failed={{ task.runtime?.failedPages ?? 0 }}
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>
