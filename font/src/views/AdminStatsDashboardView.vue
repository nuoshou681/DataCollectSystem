<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ArrowTrendingUpIcon, BoltIcon, CheckBadgeIcon, QueueListIcon } from '@heroicons/vue/24/outline'
import { fetchStatsOverview, fetchStatsTrend, fetchStatsSiteDistribution, fetchStatsHourlyActivity, fetchStatsNodeLoad } from '@/api/api'

const CHART_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#64748b']

const overview = ref<Record<string, number>>({})
const trend = ref<Array<{ date: string; created: number; finished: number; failed: number }>>([])
const siteDist = ref<Array<{ name: string; value: number }>>([])
const hourly = ref<Array<{ hour: string; count: number }>>([])
const nodeLoad = ref<Array<{ name: string; value: number }>>([])
const loading = ref(false)

const trendChartRef = ref<HTMLDivElement>()
const siteChartRef = ref<HTMLDivElement>()
const hourlyChartRef = ref<HTMLDivElement>()
const nodeChartRef = ref<HTMLDivElement>()

function initChart(el: HTMLDivElement | undefined, option: Record<string, unknown>) {
  if (!el) return
  const chart = echarts.init(el, undefined, { renderer: 'svg' })
  chart.setOption(option)
}

async function load() {
  loading.value = true
  try {
    const [ov, tr, sd, ha, nl] = await Promise.all([
      fetchStatsOverview(), fetchStatsTrend(), fetchStatsSiteDistribution(), fetchStatsHourlyActivity(), fetchStatsNodeLoad(),
    ])
    overview.value = ov; trend.value = tr; siteDist.value = sd; hourly.value = ha; nodeLoad.value = nl
  } finally {
    loading.value = false
  }
}

const AXIS_COLOR = '#94a3b8'
const SPLIT_COLOR = '#f1f5f9'

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

watch(siteDist, () => initChart(siteChartRef.value!, {
  color: CHART_COLORS,
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
    data: siteDist.value.map(d => ({ name: d.name, value: d.value })),
    label: { color: AXIS_COLOR },
  }],
}), { deep: true })

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

watch(nodeLoad, () => initChart(nodeChartRef.value!, {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 12, bottom: 24 },
  xAxis: { type: 'category', data: nodeLoad.value.map(d => d.name), axisLabel: { color: AXIS_COLOR } },
  yAxis: { type: 'value', axisLabel: { color: AXIS_COLOR }, splitLine: { lineStyle: { color: SPLIT_COLOR } } },
  series: [{ type: 'bar', data: nodeLoad.value.map(d => d.value), itemStyle: { color: '#a78bfa' }, barMaxWidth: 32 }],
}), { deep: true })

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">全系统数据统计</h2>
        <p class="text-sm text-slate-500 mt-1">全系统任务趋势、站点分布与节点负载</p>
      </div>
      <el-button text type="primary" :loading="loading" @click="load">刷新</el-button>
    </div>

    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500">
        <div class="flex items-center gap-3"><QueueListIcon class="w-6 h-6 text-blue-500" /><div><div class="text-sm text-slate-500">全系统任务数</div><div class="text-2xl font-bold text-blue-600">{{ overview.totalTasks ?? '-' }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500">
        <div class="flex items-center gap-3"><BoltIcon class="w-6 h-6 text-emerald-500" /><div><div class="text-sm text-slate-500">运行中</div><div class="text-2xl font-bold text-emerald-600">{{ overview.runningTasks ?? '-' }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-violet-500">
        <div class="flex items-center gap-3"><CheckBadgeIcon class="w-6 h-6 text-violet-500" /><div><div class="text-sm text-slate-500">今日完成</div><div class="text-2xl font-bold text-violet-600">{{ overview.finishedToday ?? '-' }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500">
        <div class="flex items-center gap-3"><ArrowTrendingUpIcon class="w-6 h-6 text-amber-500" /><div><div class="text-sm text-slate-500">成功率</div><div class="text-2xl font-bold text-amber-600">{{ overview.successRate ?? '-' }}%</div></div></div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header><span class="font-semibold">7日趋势</span></template>
        <div ref="trendChartRef" style="height:300px"></div>
      </el-card>
      <el-card>
        <template #header><span class="font-semibold">站点分布</span></template>
        <div ref="siteChartRef" style="height:300px"></div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header><span class="font-semibold">今日采集时段</span></template>
        <div ref="hourlyChartRef" style="height:220px"></div>
      </el-card>
      <el-card>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">节点负载</span>
            <el-button text type="primary" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>
        <div ref="nodeChartRef" style="height:220px"></div>
      </el-card>
    </section>
  </div>
</template>
