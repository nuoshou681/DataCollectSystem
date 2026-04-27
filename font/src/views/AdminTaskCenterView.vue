<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchExportRecords,
  fetchTaskGroups,
  fetchTasks,
} from '@/api/api'
import type { ExportRecord, Task, TaskGroup } from '@/types/entity'
import { detectSite, siteLabel, statusText } from '@/utils/task'

const loading = ref(false)
const tasks = ref<Task[]>([])
const exportsData = ref<ExportRecord[]>([])
const groups = ref<TaskGroup[]>([])

async function loadCenter() {
  loading.value = true
  try {
    const [taskData, exportData, groupData] = await Promise.all([
      fetchTasks(),
      fetchExportRecords(),
      fetchTaskGroups(),
    ])
    tasks.value = taskData
    exportsData.value = exportData
    groups.value = groupData
  } catch {
    ElMessage.error('任务治理中心加载失败')
  } finally {
    loading.value = false
  }
}

const stats = computed(() => ({
  total: tasks.value.length,
  archived: tasks.value.filter(item => item.archived).length,
  running: tasks.value.filter(item => (item.runtime?.status ?? item.taskStatus) === 'RUNNING').length,
  exported: exportsData.value.length,
}))

const siteDist = computed(() => {
  const map = new Map<string, number>()
  for (const task of tasks.value) {
    const label = siteLabel(detectSite(task.url))
    map.set(label, (map.get(label) ?? 0) + 1)
  }
  return Array.from(map.entries()).map(([label, value]) => ({ label, value }))
})

const recentTasks = computed(() => tasks.value.slice(0, 10))

function formatDayLabel(offset: number) {
  const date = new Date()
  date.setDate(date.getDate() - offset)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

function sameDay(dateText?: string | null, offset = 0) {
  if (!dateText) {
    return false
  }
  const target = new Date(dateText)
  const current = new Date()
  current.setDate(current.getDate() - offset)
  return target.getFullYear() === current.getFullYear()
    && target.getMonth() === current.getMonth()
    && target.getDate() === current.getDate()
}

const trendStats = computed(() => {
  return Array.from({ length: 7 }, (_, index) => {
    const offset = 6 - index
    const created = tasks.value.filter(task => sameDay(task.createdAt, offset)).length
    const archived = tasks.value.filter(task => sameDay(task.archivedAt, offset)).length
    const exported = exportsData.value.filter(record => sameDay(record.createdAt, offset)).length
    return {
      label: formatDayLabel(offset),
      created,
      archived,
      exported,
      peak: Math.max(created, archived, exported, 1),
    }
  })
})

onMounted(() => {
  void loadCenter()
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card v-loading="loading"><div class="text-sm text-slate-500">任务总量</div><div class="mt-2 text-2xl font-semibold">{{ stats.total }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">运行中任务</div><div class="mt-2 text-2xl font-semibold text-sky-600">{{ stats.running }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">已归档任务</div><div class="mt-2 text-2xl font-semibold text-emerald-600">{{ stats.archived }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">导出记录</div><div class="mt-2 text-2xl font-semibold text-amber-600">{{ stats.exported }}</div></el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[0.9fr_1.1fr] gap-6">
      <el-card>
        <template #header><div class="flex items-center justify-between"><span class="font-semibold">站点任务分布</span><el-button text type="primary" @click="loadCenter">刷新</el-button></div></template>
        <div class="space-y-4">
          <div v-for="item in siteDist" :key="item.label" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span>{{ item.label }}</span>
              <span class="text-slate-500">{{ item.value }}</span>
            </div>
            <el-progress :percentage="stats.total ? Math.round((item.value / stats.total) * 100) : 0" :stroke-width="12" />
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">最新任务治理视图</span></template>
        <el-table :data="recentTasks" border stripe v-loading="loading">
          <el-table-column prop="taskId" label="任务ID" width="90" />
          <el-table-column label="站点" width="110">
            <template #default="scope">{{ siteLabel(detectSite(scope.row.url)) }}</template>
          </el-table-column>
          <el-table-column prop="keyword" label="关键词" min-width="140" />
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.archived ? 'info' : 'success'">
                {{ scope.row.archived ? '已归档' : statusText(scope.row.runtime?.status ?? scope.row.taskStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="batchId" label="批次" min-width="160" />
          <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        </el-table>
      </el-card>
    </section>

    <el-card>
      <template #header><span class="font-semibold">最近 7 天任务治理趋势</span></template>
      <div class="grid grid-cols-1 md:grid-cols-7 gap-3">
        <div v-for="item in trendStats" :key="item.label" class="rounded-xl border border-slate-200 p-4 bg-white">
          <div class="text-xs text-slate-500">{{ item.label }}</div>
          <div class="mt-4 space-y-3">
            <div>
              <div class="flex items-center justify-between text-xs"><span>新建</span><span>{{ item.created }}</span></div>
              <div class="mt-1 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-sky-500" :style="{ width: `${Math.round((item.created / item.peak) * 100)}%` }" /></div>
            </div>
            <div>
              <div class="flex items-center justify-between text-xs"><span>归档</span><span>{{ item.archived }}</span></div>
              <div class="mt-1 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-emerald-500" :style="{ width: `${Math.round((item.archived / item.peak) * 100)}%` }" /></div>
            </div>
            <div>
              <div class="flex items-center justify-between text-xs"><span>导出</span><span>{{ item.exported }}</span></div>
              <div class="mt-1 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-amber-500" :style="{ width: `${Math.round((item.exported / item.peak) * 100)}%` }" /></div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>
