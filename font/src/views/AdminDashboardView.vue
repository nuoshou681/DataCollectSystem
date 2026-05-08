<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { ArchiveBoxIcon, BoltIcon, QueueListIcon, ServerStackIcon } from '@heroicons/vue/24/outline'
import { fetchCrawlerNodes, fetchTaskLogs, fetchTasks, getTaskRuntimeStreamUrl } from '@/api/api'
import type { CrawlerNode, Task, TaskLog, TaskRuntime } from '@/types/entity'
import { deriveNodeStatus } from '@/utils/task'

const loading = ref(false)
const nodes = ref<CrawlerNode[]>([])
const tasks = ref<Task[]>([])
const logs = ref<TaskLog[]>([])
const siteChart = ref<HTMLDivElement>()
let runtimeStream: EventSource | null = null

const CHART_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#64748b']

const SITE_NAME_MAP: Record<string, string> = {
  'SOHU': '搜狐新闻', 'BING': 'Bing 搜索', 'BAIDU_BAIKE': '百度百科',
  'TENCENT_NEWS': '腾讯新闻', 'SINA_NEWS': '新浪新闻', 'THEPAPER': '澎湃新闻',
  'HUANQIU': '环球网', 'CHINANEWS': '中国新闻网', 'CCTV_NEWS': '央视网新闻',
  'GUANCHA': '观察者网', 'WIKIPEDIA': '维基百科',
  'UNKNOWN': '其他', 'OTHER': '其他',
}

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
    } catch { /* ignore */ }
  })
}

const highlights = computed(() => {
  const runtimes = tasks.value.map(task => task.runtime).filter(Boolean)
  const onlineNodes = nodes.value.filter(node => ['ONLINE', 'IDLE'].includes(deriveNodeStatus(node).displayStatus)).length
  return [
    { label: '任务总量', value: tasks.value.length, color: 'border-l-blue-500', textColor: 'text-blue-600', icon: QueueListIcon, iconColor: 'text-blue-500' },
    { label: '运行中', value: runtimes.filter(r => r?.status === 'RUNNING').length, color: 'border-l-amber-500', textColor: 'text-amber-600', icon: BoltIcon, iconColor: 'text-amber-500' },
    { label: '已归档', value: tasks.value.filter(task => task.archived).length, color: 'border-l-emerald-500', textColor: 'text-emerald-600', icon: ArchiveBoxIcon, iconColor: 'text-emerald-500' },
    { label: '在线节点', value: onlineNodes, color: 'border-l-violet-500', textColor: 'text-violet-600', icon: ServerStackIcon, iconColor: 'text-violet-500' },
  ]
})

const queueBands = computed(() => {
  const total = Math.max(tasks.value.length, 1)
  const pending = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'PENDING').length
  const running = tasks.value.filter(task => (task.runtime?.status ?? task.taskStatus) === 'RUNNING').length
  const failed = tasks.value.filter(task => ['FAILED', 'PARTIAL_FAILED'].includes(task.runtime?.status ?? task.taskStatus)).length
  return [
    { label: '排队中', value: Math.round((pending / total) * 100), color: 'bg-slate-400', barColor: '#94a3b8' },
    { label: '执行中', value: Math.round((running / total) * 100), color: 'bg-sky-500', barColor: '#0ea5e9' },
    { label: '异常', value: Math.round((failed / total) * 100), color: 'bg-rose-500', barColor: '#f43f5e' },
  ]
})

const siteStats = computed(() => {
  const map = new Map<string, number>()
  for (const task of tasks.value) {
    const site = task.siteType || 'OTHER'
    map.set(site, (map.get(site) ?? 0) + 1)
  }
  return Array.from(map.entries())
    .map(([name, value]) => ({ name: SITE_NAME_MAP[name] || name, value }))
    .sort((a, b) => b.value - a.value)
})

function renderSiteChart() {
  if (!siteChart.value || !siteStats.value.length) return
  const chart = echarts.init(siteChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    color: CHART_COLORS,
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['45%', '75%'], center: ['50%', '50%'],
      data: siteStats.value,
      label: { formatter: '{b}\n{c}', fontSize: 11 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
    }],
  })
  window.addEventListener('resize', () => chart.resize())
}

watch([siteStats], renderSiteChart, { deep: true })

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
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card v-for="item in highlights" :key="item.label" shadow="hover" class="border-l-4" :class="item.color" v-loading="loading">
        <div class="flex items-center gap-3">
          <component :is="item.icon" class="w-6 h-6" :class="item.iconColor" />
          <div>
            <div class="text-sm text-slate-500">{{ item.label }}</div>
            <div class="text-2xl font-bold mt-0.5" :class="item.textColor">{{ item.value }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <el-card class="xl:col-span-2">
        <template #header><div class="flex items-center justify-between"><span class="font-semibold">运行态健康度</span><el-button text type="primary" @click="loadDashboard">刷新</el-button></div></template>
        <div class="grid grid-cols-3 gap-4">
          <div v-for="band in queueBands" :key="band.label" class="rounded-xl border border-slate-100 p-5 text-center">
            <div class="text-sm text-slate-500">{{ band.label }}</div>
            <div class="text-3xl font-bold mt-2" :style="{ color: band.barColor }">{{ band.value }}%</div>
            <div class="mt-4 h-2.5 rounded-full bg-slate-100"><div class="h-2.5 rounded-full transition-all duration-700" :class="band.color" :style="{ width: `${band.value}%` }" /></div>
          </div>
        </div>
      </el-card>
      <el-card>
        <template #header><span class="font-semibold">站点任务分布</span></template>
        <div ref="siteChart" style="height:240px" v-if="siteStats.length"></div>
        <div v-else class="flex items-center justify-center h-60 text-slate-400">暂无数据</div>
      </el-card>
    </section>
  </div>
</template>
