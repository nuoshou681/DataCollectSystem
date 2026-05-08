<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { fetchStatsOverview, fetchStatsTrend, fetchStatsSiteDistribution, fetchStatsHourlyActivity, fetchStatsNodeLoad } from '@/api/api'

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
  chart.setOption({ backgroundColor: 'transparent', ...option })
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

watch(trend, () => initChart(trendChartRef.value!, {
  tooltip: { trigger: 'axis' },
  legend: { data: ['创建', '完成', '失败'], textStyle: { color: '#94a3b8' }, top: 0 },
  grid: { left: 40, right: 16, top: 32, bottom: 24 },
  xAxis: { type: 'category', data: trend.value.map(d => d.date.substring(5)), axisLabel: { color: '#94a3b8' } },
  yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } } },
  series: [
    { name: '创建', type: 'bar', data: trend.value.map(d => d.created), itemStyle: { color: '#60a5fa' }, barMaxWidth: 24 },
    { name: '完成', type: 'bar', data: trend.value.map(d => d.finished), itemStyle: { color: '#34d399' }, barMaxWidth: 24 },
    { name: '失败', type: 'bar', data: trend.value.map(d => d.failed), itemStyle: { color: '#f87171' }, barMaxWidth: 24 },
  ],
}), { deep: true })

watch(siteDist, () => initChart(siteChartRef.value!, {
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
    data: siteDist.value.map(d => ({ name: d.name, value: d.value })),
    label: { color: '#94a3b8' },
  }],
}), { deep: true })

watch(hourly, () => initChart(hourlyChartRef.value!, {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 12, bottom: 24 },
  xAxis: { type: 'category', data: hourly.value.map(d => d.hour), axisLabel: { color: '#94a3b8' } },
  yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } } },
  series: [{
    type: 'line', data: hourly.value.map(d => d.count), smooth: true,
    itemStyle: { color: '#f472b6' }, areaStyle: { color: 'rgba(244,114,182,0.08)' },
  }],
}), { deep: true })

watch(nodeLoad, () => initChart(nodeChartRef.value!, {
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 12, bottom: 24 },
  xAxis: { type: 'category', data: nodeLoad.value.map(d => d.name), axisLabel: { color: '#94a3b8' } },
  yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(148,163,184,0.15)' } } },
  series: [{ type: 'bar', data: nodeLoad.value.map(d => d.value), itemStyle: { color: '#a78bfa' }, barMaxWidth: 32 }],
}), { deep: true })

onMounted(load)
</script>

<template>
  <div class="p-6 space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-sm text-gray-400">全系统任务数</div>
        <div class="text-2xl font-semibold mt-2 text-blue-400">{{ overview.totalTasks ?? '-' }}</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-sm text-gray-400">运行中</div>
        <div class="text-2xl font-semibold mt-2 text-emerald-400">{{ overview.runningTasks ?? '-' }}</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-sm text-gray-400">今日完成</div>
        <div class="text-2xl font-semibold mt-2 text-violet-400">{{ overview.finishedToday ?? '-' }}</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-sm text-gray-400">成功率</div>
        <div class="text-2xl font-semibold mt-2 text-amber-400">{{ overview.successRate ?? '-' }}%</div>
      </div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
        <h3 class="text-sm font-medium text-gray-300 mb-3">7日趋势</h3>
        <div ref="trendChartRef" style="height:300px"></div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
        <h3 class="text-sm font-medium text-gray-300 mb-3">站点分布</h3>
        <div ref="siteChartRef" style="height:300px"></div>
      </div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
        <h3 class="text-sm font-medium text-gray-300 mb-3">今日采集时段</h3>
        <div ref="hourlyChartRef" style="height:220px"></div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-sm font-medium text-gray-300">节点负载</h3>
          <button class="text-xs text-blue-400 hover:underline" :disabled="loading" @click="load">刷新</button>
        </div>
        <div ref="nodeChartRef" style="height:220px"></div>
      </div>
    </section>
  </div>
</template>
