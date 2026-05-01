<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { fetchStatsOverview, fetchStatsTrend, fetchStatsSiteDistribution, fetchStatsHourlyActivity, fetchStatsNodeLoad } from '@/api/api'

const overview = ref<Record<string, number>>({})
const trend = ref<Array<{ date: string; created: number; finished: number; failed: number }>>([])
const siteDist = ref<Array<{ name: string; value: number }>>([])
const hourly = ref<Array<{ hour: string; count: number }>>([])
const nodeLoadRef = ref<Array<{ name: string; value: number }>>([])

const trendChartRef = ref<HTMLDivElement>()
const siteChartRef = ref<HTMLDivElement>()
const hourlyChartRef = ref<HTMLDivElement>()
const nodeChartRef = ref<HTMLDivElement>()

function initChart(ref: typeof trendChartRef, option: any) {
  if (!ref.value) return
  const chart = echarts.init(ref.value, undefined, { renderer: 'svg' })
  chart.setOption(option)
}

async function load() {
  const [ov, tr, sd, ha, nl] = await Promise.all([
    fetchStatsOverview(), fetchStatsTrend(), fetchStatsSiteDistribution(), fetchStatsHourlyActivity(), fetchStatsNodeLoad(),
  ])
  overview.value = ov; trend.value = tr; siteDist.value = sd; hourly.value = ha; nodeLoadRef.value = nl
}

watch(trend, () => initChart(trendChartRef, {
  tooltip: { trigger: 'axis' },
  legend: { data: ['创建', '完成', '失败'], textStyle: { color: '#9ca3af' } },
  xAxis: { type: 'category', data: trend.value.map(d => d.date.substring(5)), axisLabel: { color: '#9ca3af' } },
  yAxis: { type: 'value', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#374151' } } },
  series: [
    { name: '创建', type: 'bar', data: trend.value.map(d => d.created), itemStyle: { color: '#60a5fa' } },
    { name: '完成', type: 'bar', data: trend.value.map(d => d.finished), itemStyle: { color: '#34d399' } },
    { name: '失败', type: 'bar', data: trend.value.map(d => d.failed), itemStyle: { color: '#f87171' } },
  ],
}), { deep: true })

watch(siteDist, () => initChart(siteChartRef, {
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['40%', '70%'], data: siteDist.value.map(d => ({ name: d.name, value: d.value })), label: { color: '#9ca3af' } }],
}), { deep: true })

watch(hourly, () => initChart(hourlyChartRef, {
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: hourly.value.map(d => d.hour), axisLabel: { color: '#9ca3af' } },
  yAxis: { type: 'value', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#374151' } } },
  series: [{ type: 'line', data: hourly.value.map(d => d.count), smooth: true, itemStyle: { color: '#f472b6' }, areaStyle: { color: 'rgba(244,114,182,0.1)' } }],
}), { deep: true })

watch(nodeLoadRef, () => initChart(nodeChartRef, {
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: nodeLoadRef.value.map(d => d.name), axisLabel: { color: '#9ca3af' } },
  yAxis: { type: 'value', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#374151' } } },
  series: [{ type: 'bar', data: nodeLoadRef.value.map(d => d.value), itemStyle: { color: '#a78bfa' } }],
}), { deep: true })

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-white">数据统计大屏</h1>

    <div class="grid grid-cols-4 gap-4">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-blue-400">{{ overview.totalTasks ?? '-' }}</div>
        <div class="text-gray-400 text-sm">全系统任务数</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-emerald-400">{{ overview.runningTasks ?? '-' }}</div>
        <div class="text-gray-400 text-sm">运行中</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-violet-400">{{ overview.finishedToday ?? '-' }}</div>
        <div class="text-gray-400 text-sm">今日完成</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-amber-400">{{ overview.successRate ?? '-' }}%</div>
        <div class="text-gray-400 text-sm">成功率</div>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <h3 class="text-sm font-medium text-gray-400 mb-3">7日趋势</h3>
        <div ref="trendChartRef" style="height:280px"></div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <h3 class="text-sm font-medium text-gray-400 mb-3">站点分布</h3>
        <div ref="siteChartRef" style="height:280px"></div>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <h3 class="text-sm font-medium text-gray-400 mb-3">今日采集时段</h3>
        <div ref="hourlyChartRef" style="height:200px"></div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <h3 class="text-sm font-medium text-gray-400 mb-3">节点负载</h3>
        <div ref="nodeChartRef" style="height:200px"></div>
      </div>
    </div>
  </div>
</template>
