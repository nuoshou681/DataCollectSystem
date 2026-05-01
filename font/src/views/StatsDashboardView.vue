<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { fetchStatsOverview, fetchStatsTrend, fetchStatsSiteDistribution, fetchStatsHourlyActivity } from '@/api/api'

const overview = ref<Record<string, number>>({})
const trend = ref<Array<{ date: string; created: number; finished: number; failed: number }>>([])
const siteDist = ref<Array<{ name: string; value: number }>>([])
const hourly = ref<Array<{ hour: string; count: number }>>([])

const trendChart = ref<HTMLDivElement>()
const siteChart = ref<HTMLDivElement>()
const hourlyChart = ref<HTMLDivElement>()

async function load() {
  const [ov, tr, sd, ha] = await Promise.all([
    fetchStatsOverview(), fetchStatsTrend(), fetchStatsSiteDistribution(), fetchStatsHourlyActivity(),
  ])
  overview.value = ov; trend.value = tr; siteDist.value = sd; hourly.value = ha
}

function renderTrendChart() {
  if (!trendChart.value || !trend.value.length) return
  const chart = echarts.init(trendChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['创建', '完成', '失败'], textStyle: { color: '#9ca3af' } },
    xAxis: { type: 'category', data: trend.value.map(d => d.date.substring(5)), axisLabel: { color: '#9ca3af' } },
    yAxis: { type: 'value', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#374151' } } },
    series: [
      { name: '创建', type: 'bar', data: trend.value.map(d => d.created), itemStyle: { color: '#60a5fa' } },
      { name: '完成', type: 'bar', data: trend.value.map(d => d.finished), itemStyle: { color: '#34d399' } },
      { name: '失败', type: 'bar', data: trend.value.map(d => d.failed), itemStyle: { color: '#f87171' } },
    ],
  })
}

function renderSiteChart() {
  if (!siteChart.value || !siteDist.value.length) return
  const chart = echarts.init(siteChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: siteDist.value.map(d => ({ name: d.name, value: d.value })),
      label: { color: '#9ca3af' },
    }],
  })
}

function renderHourlyChart() {
  if (!hourlyChart.value || !hourly.value.length) return
  const chart = echarts.init(hourlyChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hourly.value.map(d => d.hour), axisLabel: { color: '#9ca3af' } },
    yAxis: { type: 'value', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#374151' } } },
    series: [{
      type: 'line', data: hourly.value.map(d => d.count), smooth: true,
      itemStyle: { color: '#818cf8' }, areaStyle: { color: 'rgba(129,140,248,0.1)' },
    }],
  })
}

watch([trend], renderTrendChart, { deep: true })
watch([siteDist], renderSiteChart, { deep: true })
watch([hourly], renderHourlyChart, { deep: true })

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-gray-900">数据统计</h1>

    <div class="grid grid-cols-4 gap-4">
      <div class="bg-white border rounded-xl p-4 shadow-sm">
        <div class="text-2xl font-bold text-blue-600">{{ overview.totalTasks ?? '-' }}</div>
        <div class="text-gray-500 text-sm">总任务数</div>
      </div>
      <div class="bg-white border rounded-xl p-4 shadow-sm">
        <div class="text-2xl font-bold text-emerald-500">{{ overview.runningTasks ?? '-' }}</div>
        <div class="text-gray-500 text-sm">运行中</div>
      </div>
      <div class="bg-white border rounded-xl p-4 shadow-sm">
        <div class="text-2xl font-bold text-violet-500">{{ overview.finishedToday ?? '-' }}</div>
        <div class="text-gray-500 text-sm">今日完成</div>
      </div>
      <div class="bg-white border rounded-xl p-4 shadow-sm">
        <div class="text-2xl font-bold text-amber-500">{{ overview.successRate ?? '-' }}%</div>
        <div class="text-gray-500 text-sm">成功率</div>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-4">
      <div class="bg-white border rounded-xl p-4 shadow-sm">
        <h3 class="text-sm font-medium text-gray-600 mb-3">7日趋势</h3>
        <div ref="trendChart" style="height:280px"></div>
      </div>
      <div class="bg-white border rounded-xl p-4 shadow-sm">
        <h3 class="text-sm font-medium text-gray-600 mb-3">站点分布</h3>
        <div ref="siteChart" style="height:280px"></div>
      </div>
    </div>

    <div class="bg-white border rounded-xl p-4 shadow-sm">
      <h3 class="text-sm font-medium text-gray-600 mb-3">今日采集时段分布</h3>
      <div ref="hourlyChart" style="height:200px"></div>
    </div>
  </div>
</template>
