<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ArrowTrendingUpIcon, BoltIcon, CheckBadgeIcon, QueueListIcon } from '@heroicons/vue/24/outline'
import { fetchStatsOverview, fetchStatsTrend, fetchStatsSiteDistribution, fetchStatsHourlyActivity } from '@/api/api'

const CHART_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#64748b']

const overview = ref<Record<string, number>>({})
const trend = ref<Array<{ date: string; created: number; finished: number; failed: number }>>([])
const siteDist = ref<Array<{ name: string; value: number }>>([])
const hourly = ref<Array<{ hour: string; count: number }>>([])
const loading = ref(false)

const trendChart = ref<HTMLDivElement>()
const siteChart = ref<HTMLDivElement>()
const hourlyChart = ref<HTMLDivElement>()

async function load() {
  loading.value = true
  try {
    const [ov, tr, sd, ha] = await Promise.all([
      fetchStatsOverview(), fetchStatsTrend(), fetchStatsSiteDistribution(), fetchStatsHourlyActivity(),
    ])
    overview.value = ov; trend.value = tr; siteDist.value = sd; hourly.value = ha
  } finally {
    loading.value = false
  }
}

function renderTrendChart() {
  if (!trendChart.value || !trend.value.length) return
  const chart = echarts.init(trendChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['创建', '完成', '失败'], top: 0 },
    grid: { left: 40, right: 16, top: 32, bottom: 24 },
    xAxis: { type: 'category', data: trend.value.map(d => d.date.substring(5)) },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [
      { name: '创建', type: 'bar', data: trend.value.map(d => d.created), itemStyle: { color: '#60a5fa' }, barMaxWidth: 24 },
      { name: '完成', type: 'bar', data: trend.value.map(d => d.finished), itemStyle: { color: '#34d399' }, barMaxWidth: 24 },
      { name: '失败', type: 'bar', data: trend.value.map(d => d.failed), itemStyle: { color: '#f87171' }, barMaxWidth: 24 },
    ],
  })
}

const SITE_NAME_MAP: Record<string, string> = {
  'SOHU': '搜狐新闻', 'BING': 'Bing 搜索', 'BAIDU_BAIKE': '百度百科',
  'TENCENT_NEWS': '腾讯新闻', 'SINA_NEWS': '新浪新闻', 'THEPAPER': '澎湃新闻',
  'HUANQIU': '环球网', 'CHINANEWS': '中国新闻网', 'CCTV_NEWS': '央视网新闻',
  'GUANCHA': '观察者网', 'WIKIPEDIA': '维基百科(EN)',
  'UNKNOWN': '其他站点', '未知': '未分类',
  'BAIDU_NEWS': '百度新闻(已停用)', 'BAIDU_SEARCH': '百度搜索(已停用)',
  'SO360': '360搜索(已停用)', 'SOGOU_WEIXIN': '搜狗微信(已停用)',
  'ZHIHU': '知乎(已停用)', 'NETEASE_NEWS': '网易新闻(已停用)',
}

function renderSiteChart() {
  if (!siteChart.value || !siteDist.value.length) return
  const chart = echarts.init(siteChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    color: CHART_COLORS,
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
      data: siteDist.value.map(d => ({ name: SITE_NAME_MAP[d.name] || d.name, value: d.value })),
      label: { formatter: '{b}: {c}' },
    }],
  })
}

function renderHourlyChart() {
  if (!hourlyChart.value || !hourly.value.length) return
  const chart = echarts.init(hourlyChart.value, undefined, { renderer: 'svg' })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 12, bottom: 24 },
    xAxis: { type: 'category', data: hourly.value.map(d => d.hour) },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [{
      type: 'line', data: hourly.value.map(d => d.count), smooth: true,
      itemStyle: { color: '#818cf8' }, areaStyle: { color: 'rgba(129,140,248,0.08)' },
    }],
  })
}

watch([trend], renderTrendChart, { deep: true })
watch([siteDist], renderSiteChart, { deep: true })
watch([hourly], renderHourlyChart, { deep: true })

onMounted(load)
</script>

<template>
  <div class="p-6 space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500">
        <div class="flex items-center gap-3"><QueueListIcon class="w-6 h-6 text-blue-500" /><div><div class="text-sm text-slate-500">总任务数</div><div class="text-2xl font-bold text-blue-600">{{ overview.totalTasks ?? '-' }}</div></div></div>
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
        <div ref="trendChart" style="height:300px"></div>
      </el-card>
      <el-card>
        <template #header><span class="font-semibold">站点分布</span></template>
        <div ref="siteChart" style="height:300px"></div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">今日采集时段分布</span>
          <el-button text type="primary" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <div ref="hourlyChart" style="height:220px"></div>
    </el-card>
  </div>
</template>
