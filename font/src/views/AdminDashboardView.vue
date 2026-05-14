<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  BoltIcon,
  CircleStackIcon,
  CloudIcon,
  CpuChipIcon,
  QueueListIcon,
  ServerIcon,
  ServerStackIcon,
  ShieldCheckIcon,
  XCircleIcon,
} from '@heroicons/vue/24/outline'
import {
  fetchCrawlerNodes,
  fetchHealth,
  fetchStatsHourlyActivity,
  fetchStatsNodeLoad,
  fetchStatsOverview,
  fetchStatsTrend,
  fetchTasks,
  getTaskRuntimeStreamUrl,
} from '@/api/api'
import type { CrawlerNode, HealthInfo, Task, TaskRuntime } from '@/types/entity'
import { deriveNodeStatus } from '@/utils/task'

const CHART_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#64748b']
const AXIS_COLOR = '#94a3b8'
const SPLIT_COLOR = '#f1f5f9'

const SITE_NAME_MAP: Record<string, string> = {
  SOHU: '搜狐新闻', BING: 'Bing 搜索', BAIDU_BAIKE: '百度百科',
  TENCENT_NEWS: '腾讯新闻', SINA_NEWS: '新浪新闻', THEPAPER: '澎湃新闻',
  HUANQIU: '环球网', CHINANEWS: '中国新闻网', CCTV_NEWS: '央视网新闻',
  GUANCHA: '观察者网', WIKIPEDIA: '维基百科',
  UNKNOWN: '其他', OTHER: '其他',
}

const loading = ref(false)
const nodes = ref<CrawlerNode[]>([])
const tasks = ref<Task[]>([])
const health = ref<HealthInfo | null>(null)

// Stats data
const trend = ref<Array<{ date: string; created: number; finished: number; failed: number }>>([])
const hourly = ref<Array<{ hour: string; count: number }>>([])
const nodeLoad = ref<Array<{ name: string; value: number }>>([])

// Chart refs
const siteChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
const statusChartRef = ref<HTMLDivElement>()
const hourlyChartRef = ref<HTMLDivElement>()
const nodeChartRef = ref<HTMLDivElement>()

let runtimeStream: EventSource | null = null

function initChart(el: HTMLDivElement | undefined, option: Record<string, unknown>) {
  if (!el) return
  const chart = echarts.init(el, undefined, { renderer: 'svg' })
  chart.setOption(option)
}

async function loadAll() {
  loading.value = true
  try {
    const [nodeData, taskData, statsData, healthData, trendData, hourlyData, nodeLoadData] = await Promise.all([
      fetchCrawlerNodes(),
      fetchTasks(),
      fetchStatsOverview(),
      fetchHealth(),
      fetchStatsTrend(),
      fetchStatsHourlyActivity(),
      fetchStatsNodeLoad(),
    ])
    nodes.value = nodeData
    tasks.value = taskData
    if (statsData) overviewStats.value = {
      totalTasks: Number(statsData.totalTasks ?? 0),
      runningTasks: Number(statsData.runningTasks ?? 0),
      finishedToday: Number(statsData.finishedToday ?? 0),
      successRate: Number(statsData.successRate ?? 0),
      finishedTasks: Number(statsData.finishedTasks ?? 0),
      failedTasks: Number(statsData.failedTasks ?? 0),
      pendingTasks: Number(statsData.pendingTasks ?? 0),
    }
    health.value = healthData
    trend.value = trendData
    hourly.value = hourlyData
    nodeLoad.value = nodeLoadData
  } finally {
    loading.value = false
  }
}

const overviewStats = ref({
  totalTasks: 0, runningTasks: 0, finishedToday: 0, successRate: 0,
  finishedTasks: 0, failedTasks: 0, pendingTasks: 0,
})

const onlineNodes = computed(() =>
  nodes.value.filter(n => ['ONLINE', 'IDLE'].includes(deriveNodeStatus(n).displayStatus)),
)

const offlineNodes = computed(() =>
  nodes.value.filter(n => deriveNodeStatus(n).displayStatus === 'OFFLINE'),
)

const failedTaskCount = computed(() =>
  tasks.value.filter(t => ['FAILED', 'PARTIAL_FAILED'].includes(t.runtime?.status ?? t.taskStatus ?? '')).length,
)

const hasAlerts = computed(() =>
  offlineNodes.value.length > 0 || failedTaskCount.value > 0,
)

const healthScore = computed(() => {
  let score = 100
  if (health.value) {
    if (health.value.mysql?.status !== 'UP') score -= 25
    if (health.value.rabbitmq?.status !== 'UP') score -= 25
  }
  const total = nodes.value.length
  if (total > 0) {
    const offlineCount = offlineNodes.value.length
    const offlineRatio = offlineCount / total
    if (offlineRatio >= 0.5) score -= 40
    else score -= Math.round(offlineRatio * 40)
  }
  if (failedTaskCount.value > 10) score -= 10
  else if (failedTaskCount.value > 0) score -= failedTaskCount.value
  return Math.max(0, score)
})

const scoreColor = computed(() => {
  if (healthScore.value >= 80) return 'text-emerald-600'
  if (healthScore.value >= 60) return 'text-amber-600'
  return 'text-red-600'
})

const kpiCards = computed(() => [
  { label: '任务总量', value: overviewStats.value.totalTasks, icon: QueueListIcon, color: 'border-l-blue-500', textColor: 'text-blue-600', iconColor: 'text-blue-500' },
  { label: '运行中', value: overviewStats.value.runningTasks, icon: BoltIcon, color: 'border-l-sky-500', textColor: 'text-sky-600', iconColor: 'text-sky-500' },
  { label: '在线节点', value: `${onlineNodes.value.length}/${nodes.value.length}`, icon: ServerStackIcon, color: 'border-l-emerald-500', textColor: 'text-emerald-600', iconColor: 'text-emerald-500' },
  { label: '健康度', value: healthScore.value, suffix: '分', icon: ShieldCheckIcon, color: 'border-l-violet-500', iconColor: 'text-violet-500', valueClass: scoreColor.value },
])

// Site distribution
const siteStats = computed(() => {
  const map = new Map<string, number>()
  for (const t of tasks.value) {
    const site = t.siteType || 'OTHER'
    map.set(site, (map.get(site) ?? 0) + 1)
  }
  return Array.from(map.entries())
    .map(([name, value]) => ({ name: SITE_NAME_MAP[name] || name, value }))
    .sort((a, b) => b.value - a.value)
})

// Status breakdown for donut
const statusBreakdown = computed(() => ({
  pending: overviewStats.value.pendingTasks,
  running: overviewStats.value.runningTasks,
  finished: overviewStats.value.finishedTasks,
  failed: overviewStats.value.failedTasks,
}))

function renderSiteChart() {
  if (!siteChartRef.value || !siteStats.value.length) return
  const chart = echarts.init(siteChartRef.value, undefined, { renderer: 'svg' })
  chart.setOption({
    color: CHART_COLORS,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['48%', '78%'], center: ['50%', '52%'],
      data: siteStats.value,
      label: { formatter: '{b}\n{c}', fontSize: 11 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
    }],
  })
  const onResize = () => chart.resize()
  window.addEventListener('resize', onResize)
}

// 7-day trend chart
watch(trend, () => initChart(trendChartRef.value!, {
  tooltip: { trigger: 'axis' },
  legend: { data: ['创建', '完成', '失败'], textStyle: { color: AXIS_COLOR }, top: 0 },
  grid: { left: 40, right: 16, top: 32, bottom: 24 },
  xAxis: { type: 'category', data: trend.value.map(d => d.date.substring(5)), axisLabel: { color: AXIS_COLOR } },
  yAxis: { type: 'value', axisLabel: { color: AXIS_COLOR }, splitLine: { lineStyle: { color: SPLIT_COLOR } } },
  series: [
    { name: '创建', type: 'bar', data: trend.value.map(d => d.created), itemStyle: { color: '#60a5fa' }, barMaxWidth: 24 },
    { name: '完成', type: 'bar', data: trend.value.map(d => d.finished), itemStyle: { color: '#34d399' }, barMaxWidth: 24 },
    { name: '失败', type: 'bar', data: trend.value.map(d => d.failed), itemStyle: { color: '#f87171' }, barMaxWidth: 24 },
  ],
}), { deep: true })

// Status breakdown donut
watch(statusBreakdown, () => {
  const { pending, running, finished, failed } = statusBreakdown.value
  initChart(statusChartRef.value!, {
    color: ['#94a3b8', '#3b82f6', '#10b981', '#ef4444'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['42%', '72%'], center: ['50%', '50%'],
      data: [
        { name: '排队中', value: pending },
        { name: '运行中', value: running },
        { name: '已完成', value: finished },
        { name: '失败', value: failed },
      ],
      label: { color: AXIS_COLOR, fontSize: 11 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
    }],
  })
}, { deep: true })

// Hourly activity
watch(hourly, () => initChart(hourlyChartRef.value!, {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 12, bottom: 24 },
  xAxis: { type: 'category', data: hourly.value.map(d => d.hour), axisLabel: { color: AXIS_COLOR } },
  yAxis: { type: 'value', axisLabel: { color: AXIS_COLOR }, splitLine: { lineStyle: { color: SPLIT_COLOR } } },
  series: [{
    type: 'line', data: hourly.value.map(d => d.count), smooth: true,
    itemStyle: { color: '#f472b6' }, areaStyle: { color: 'rgba(244,114,182,0.08)' },
  }],
}), { deep: true })

// Node load
watch(nodeLoad, () => initChart(nodeChartRef.value!, {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 12, bottom: 24 },
  xAxis: { type: 'category', data: nodeLoad.value.map(d => d.name), axisLabel: { color: AXIS_COLOR } },
  yAxis: { type: 'value', axisLabel: { color: AXIS_COLOR }, splitLine: { lineStyle: { color: SPLIT_COLOR } } },
  series: [{ type: 'bar', data: nodeLoad.value.map(d => d.value), itemStyle: { color: '#a78bfa' }, barMaxWidth: 32 }],
}), { deep: true })

watch([siteStats], () => setTimeout(renderSiteChart, 0), { deep: true })

// SSE
function connectRuntimeStream() {
  runtimeStream?.close()
  runtimeStream = new EventSource(getTaskRuntimeStreamUrl())
  runtimeStream.addEventListener('task-runtime', event => {
    try {
      const runtime = JSON.parse((event as MessageEvent).data) as TaskRuntime
      const task = tasks.value.find(t => t.taskId === runtime.taskId)
      if (task) {
        task.runtime = { ...(task.runtime ?? {} as TaskRuntime), ...runtime }
        task.taskStatus = runtime.status
      }
    } catch { /* ignore */ }
  })
}

const nodeSummaries = computed(() =>
  nodes.value.map(n => {
    const status = deriveNodeStatus(n)
    return {
      node: n,
      displayStatus: status.displayStatus,
      statusType: status.statusType,
      loadPercent: (n.maxConcurrency ?? 1) > 0
        ? Math.round(((n.currentLoad ?? 0) / (n.maxConcurrency ?? 1)) * 100)
        : 0,
    }
  }),
)

onMounted(() => {
  void loadAll()
  connectRuntimeStream()
})

onBeforeUnmount(() => { runtimeStream?.close() })
</script>

<template>
  <div class="space-y-6">
    <!-- KPI Cards -->
    <section class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
      <el-card
        v-for="card in kpiCards" :key="card.label"
        shadow="hover" class="border-l-4" :class="card.color"
        v-loading="loading"
      >
        <div class="flex items-center gap-3">
          <component :is="card.icon" class="w-6 h-6" :class="card.iconColor" />
          <div>
            <div class="text-sm text-slate-500">{{ card.label }}</div>
            <div class="text-2xl font-bold mt-0.5" :class="card.valueClass ?? card.textColor">
              {{ card.value }}<span v-if="card.suffix" class="text-base font-normal ml-0.5">{{ card.suffix }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </section>

    <!-- Alert bar -->
    <div v-if="hasAlerts" class="flex flex-wrap gap-3 items-center rounded-xl bg-amber-50 border border-amber-200 px-5 py-3">
      <XCircleIcon class="w-5 h-5 text-amber-600 flex-shrink-0" />
      <span class="text-sm text-amber-800 font-medium">系统告警：</span>
      <span v-if="offlineNodes.length" class="text-sm text-amber-700">
        离线节点 {{ offlineNodes.map(n => n.nodeId).join('、') }}
      </span>
      <span v-if="failedTaskCount > 0" class="text-sm text-amber-700">
        {{ offlineNodes.length ? '；' : '' }}失败任务 {{ failedTaskCount }} 个
      </span>
      <el-button size="small" type="warning" text @click="loadAll">刷新状态</el-button>
    </div>

    <!-- Row 1: Site pie + Health panels -->
    <section class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <el-card class="xl:col-span-1">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">站点任务分布</span>
            <el-button size="small" text type="primary" @click="loadAll">刷新</el-button>
          </div>
        </template>
        <div ref="siteChartRef" style="height:280px" v-if="siteStats.length" />
        <el-empty v-else description="暂无数据" :image-size="80" />
      </el-card>

      <div class="xl:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-4">
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center gap-2">
              <CircleStackIcon class="w-5 h-5 text-blue-500" />
              <span class="font-semibold text-sm">MySQL</span>
              <el-tag :type="health?.mysql?.status === 'UP' ? 'success' : 'danger'" size="small" class="ml-auto">
                {{ health?.mysql?.status === 'UP' ? '正常' : '异常' }}
              </el-tag>
            </div>
          </template>
          <div v-if="health?.mysql?.error" class="text-xs text-red-500 bg-red-50 rounded-lg p-2">{{ health.mysql.error }}</div>
          <div v-else class="text-sm text-slate-500">数据库连接正常</div>
        </el-card>
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center gap-2">
              <CloudIcon class="w-5 h-5 text-violet-500" />
              <span class="font-semibold text-sm">RabbitMQ</span>
              <el-tag :type="health?.rabbitmq?.status === 'UP' ? 'success' : 'danger'" size="small" class="ml-auto">
                {{ health?.rabbitmq?.status === 'UP' ? '正常' : '异常' }}
              </el-tag>
            </div>
          </template>
          <div v-if="health?.rabbitmq?.error" class="text-xs text-red-500 bg-red-50 rounded-lg p-2">{{ health.rabbitmq.error }}</div>
          <div v-else class="text-sm text-slate-500">消息队列连接正常</div>
        </el-card>
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center gap-2">
              <CpuChipIcon class="w-5 h-5 text-emerald-500" />
              <span class="font-semibold text-sm">JVM 内存</span>
            </div>
          </template>
          <div v-if="health?.jvm" class="space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">已用</span>
              <span class="font-medium">{{ ((health.jvm.usedMemoryMB ?? 0) / 1024).toFixed(1) }} / {{ ((health.jvm.maxMemoryMB ?? 0) / 1024).toFixed(1) }} GB</span>
            </div>
            <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
              <div class="h-full rounded-full bg-gradient-to-r from-emerald-400 to-emerald-600 transition-all"
                :style="{ width: ((health.jvm.usedMemoryMB ?? 0) / (health.jvm.maxMemoryMB ?? 1) * 100).toFixed(1) + '%' }" />
            </div>
          </div>
        </el-card>
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center gap-2">
              <ServerIcon class="w-5 h-5 text-amber-500" />
              <span class="font-semibold text-sm">磁盘</span>
            </div>
          </template>
          <div v-if="health?.disk" class="space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-slate-500">可用</span>
              <span class="font-medium">{{ health.disk.freeGB }} / {{ health.disk.totalGB }} GB</span>
            </div>
            <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
              <div class="h-full rounded-full bg-gradient-to-r from-amber-400 to-amber-600 transition-all"
                :style="{ width: ((1 - health.disk.freeGB / (health.disk.totalGB || 1)) * 100).toFixed(1) + '%' }" />
            </div>
          </div>
        </el-card>
      </div>
    </section>

    <!-- Row 2: 7-day trend + Status breakdown -->
    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header><span class="font-semibold">7日任务趋势</span></template>
        <div ref="trendChartRef" style="height:300px" />
      </el-card>
      <el-card>
        <template #header><span class="font-semibold">任务状态分布</span></template>
        <div ref="statusChartRef" style="height:300px" />
      </el-card>
    </section>

    <!-- Row 3: Hourly activity + Node load -->
    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header><span class="font-semibold">今日采集时段</span></template>
        <div ref="hourlyChartRef" style="height:220px" />
      </el-card>
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">节点负载</span>
            <el-button text type="primary" :loading="loading" @click="loadAll">刷新</el-button>
          </div>
        </template>
        <div ref="nodeChartRef" style="height:220px" />
      </el-card>
    </section>

    <!-- Node summary table -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">爬虫节点运行状态</span>
          <span class="text-xs text-slate-400">共 {{ nodes.length }} 个节点</span>
        </div>
      </template>
      <el-table :data="nodeSummaries" border stripe v-loading="loading" empty-text="暂无节点注册">
        <el-table-column prop="node.nodeId" label="节点 ID" min-width="140" />
        <el-table-column prop="node.nodeName" label="名称" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.statusType" size="small">{{ scope.row.displayStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负载" min-width="160">
          <template #default="scope">
            <div class="flex items-center gap-2">
              <div class="flex-1 h-2 rounded-full bg-slate-100 overflow-hidden">
                <div class="h-full rounded-full transition-all"
                  :class="scope.row.loadPercent > 80 ? 'bg-red-500' : scope.row.loadPercent > 50 ? 'bg-amber-500' : 'bg-emerald-500'"
                  :style="{ width: scope.row.loadPercent + '%' }" />
              </div>
              <span class="text-xs text-slate-500 w-12 text-right">{{ scope.row.node.currentLoad ?? 0 }}/{{ scope.row.node.maxConcurrency ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="node.version" label="版本" width="90" />
        <el-table-column label="最后心跳" min-width="160">
          <template #default="scope"><span class="text-sm">{{ scope.row.node.lastHeartbeat || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="能力" min-width="200">
          <template #default="scope">
            <el-tag v-for="cap in scope.row.node.capabilities || []" :key="cap" size="small" type="info" class="mr-1">{{ cap }}</el-tag>
            <span v-if="!(scope.row.node.capabilities?.length)" class="text-slate-400 text-sm">-</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="140">
          <template #default="scope">
            <el-tag v-for="tag in scope.row.node.tags || []" :key="tag" size="small" class="mr-1">{{ tag }}</el-tag>
            <span v-if="!(scope.row.node.tags?.length)" class="text-slate-400 text-sm">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
