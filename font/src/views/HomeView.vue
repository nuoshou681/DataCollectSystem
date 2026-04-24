<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchPageResults, fetchTasks } from '@/api/api'
import type { CrawlerPageResult, Task } from '@/types/entity'

type SiteKey = 'sohu' | 'bing' | 'baike' | 'other'

const tasks = ref<Task[]>([])
const pageResults = ref<CrawlerPageResult[]>([])
const loading = ref(false)

function detectSite(url: string): SiteKey {
  if (url.includes('search.sohu.com')) {
    return 'sohu'
  }
  if (url.includes('www.bing.com')) {
    return 'bing'
  }
  if (url.includes('baike.baidu.com')) {
    return 'baike'
  }
  return 'other'
}

function siteLabel(site: SiteKey) {
  if (site === 'sohu') {
    return '搜狐'
  }
  if (site === 'bing') {
    return '必应'
  }
  if (site === 'baike') {
    return '百科'
  }
  return '其他'
}

const taskStats = computed(() => {
  const total = tasks.value.length
  const running = tasks.value.filter(task => task.taskStatus === 'RUNNING').length
  const pending = tasks.value.filter(task => task.taskStatus === 'PENDING' || !task.taskStatus).length
  const failed = tasks.value.filter(task => task.taskStatus === 'FAILED' || task.taskStatus === 'PARTIAL_FAILED').length
  return { total, running, pending, failed }
})

const successRate = computed(() => {
  const total = pageResults.value.length
  if (!total) {
    return 0
  }
  const success = pageResults.value.filter(page => page.success).length
  return Math.round((success / total) * 100)
})

const highlights = computed(() => [
  { label: '任务总数', value: String(taskStats.value.total), note: '累计任务' },
  { label: '进行中任务', value: String(taskStats.value.running), note: '执行中' },
  { label: '采集成功率', value: `${successRate.value}%`, note: '基于页面结果' },
  { label: '新增链接', value: String(pageResults.value.length), note: '当前结果量' },
])

const focusKeywords = computed(() => {
  const counter = new Map<string, { count: number; sites: Set<string> }>()
  for (const task of tasks.value) {
    const keyword = (task.keyword || '').trim()
    if (!keyword) {
      continue
    }
    const site = siteLabel(detectSite(task.url))
    const entry = counter.get(keyword) || { count: 0, sites: new Set<string>() }
    entry.count += 1
    entry.sites.add(site)
    counter.set(keyword, entry)
  }

  return [...counter.entries()]
    .sort((a, b) => b[1].count - a[1].count)
    .slice(0, 3)
    .map(([name, meta]) => ({ name, tags: [...meta.sites], count: meta.count }))
})

const suggestions = computed(() => {
  const tips: string[] = []
  if (taskStats.value.failed > 0) {
    tips.push('存在失败任务，建议优先检查失败原因并重试。')
  }
  if (successRate.value < 70) {
    tips.push('当前成功率偏低，可减少任务并发或调整站点策略。')
  }
  if (taskStats.value.pending > 0 && taskStats.value.running === 0) {
    tips.push('任务仍在排队，确认节点在线状态和队列负载。')
  }
  if (tips.length === 0) {
    tips.push('采集运行稳定，可继续扩展关键词覆盖。')
  }
  return tips
})

const nextSteps = computed(() => {
  const steps: string[] = []
  if (taskStats.value.failed > 0) {
    steps.push('优先处理失败任务并检查节点异常。')
  }
  if (taskStats.value.pending > 0) {
    steps.push('确认排队任务已分配到可用节点。')
  }
  steps.push('定期下载高价值页面并归档。')
  return steps
})

const recentLinks = computed(() => {
  return [...pageResults.value]
    .sort((a, b) => (b.pageResultId ?? 0) - (a.pageResultId ?? 0))
    .slice(0, 3)
    .map(page => ({
      title: page.pageTitle || page.pageUrl,
      source: siteLabel(detectSite(page.pageUrl)),
      meta: `结果ID #${page.pageResultId ?? '-'} · 任务 #${page.taskId}`,
    }))
})

async function loadDashboard() {
  loading.value = true
  try {
    const [taskData, pageData] = await Promise.all([fetchTasks(), fetchPageResults()])
    tasks.value = taskData
    pageResults.value = pageData
  } catch {
    ElMessage.error('仪表盘数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="space-y-8">
    <section class="hero">
      <div class="hero-text">
        <div class="hero-label">今日概览</div>
        <h1 class="hero-title">数据采集正在加速。</h1>
        <p class="hero-sub">
          保持关键词策略清晰，实时关注节点状态，确保链接命中率稳定。
        </p>
        <div class="hero-actions">
          <RouterLink to="/tasks" class="hero-primary">创建新任务</RouterLink>
          <button class="hero-secondary">查看任务模板</button>
        </div>
      </div>
      <div class="hero-panel">
        <div class="panel-title">实时评分</div>
        <div class="panel-metric">88</div>
        <div class="panel-sub">任务健康指数</div>
        <div class="panel-bars">
          <div class="panel-bar"><span style="width: 76%"></span></div>
          <div class="panel-bar"><span style="width: 62%"></span></div>
          <div class="panel-bar"><span style="width: 84%"></span></div>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card v-for="item in highlights" :key="item.label" class="shadow-sm" v-loading="loading">
        <div class="text-sm text-gray-500">{{ item.label }}</div>
        <div class="text-2xl font-semibold mt-2 text-slate-800">{{ item.value }}</div>
        <div class="text-xs text-gray-400 mt-1">{{ item.note }}</div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <el-card class="xl:col-span-2">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">重点关键词</span>
            <span class="text-xs text-gray-400">关键词组合</span>
          </div>
        </template>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div v-for="item in focusKeywords" :key="item.name" class="keyword-card">
            <div class="keyword-title">{{ item.name }}</div>
            <div class="keyword-tags">
              <el-tag v-for="tag in item.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
            </div>
            <div class="keyword-note">关联任务 {{ item.count }} 个</div>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <span class="font-semibold">智能建议</span>
        </template>
        <ul class="text-sm text-gray-600 space-y-3">
          <li v-for="item in suggestions" :key="item">• {{ item }}</li>
        </ul>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header>
          <span class="font-semibold">最新链接</span>
        </template>
        <div class="space-y-3">
          <div v-for="link in recentLinks" :key="link.title" class="link-row">
            <div>
              <div class="font-medium text-gray-700">{{ link.title }}</div>
              <div class="text-xs text-gray-400">{{ link.source }} · {{ link.meta }}</div>
            </div>
            <div class="text-xs text-gray-400">最新</div>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <span class="font-semibold">下一步建议</span>
        </template>
        <ol class="text-sm text-gray-600 space-y-3">
          <li v-for="(item, index) in nextSteps" :key="item">{{ index + 1 }}. {{ item }}</li>
        </ol>
      </el-card>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  padding: 24px;
  border-radius: 24px;
  background: linear-gradient(120deg, #dbeafe 0%, #fef9c3 50%, #ede9fe 100%);
}

.hero-label {
  text-transform: uppercase;
  letter-spacing: 0.2em;
  font-size: 11px;
  color: #64748b;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
  color: #0f172a;
}

.hero-sub {
  margin-top: 10px;
  color: #475569;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.hero-primary {
  padding: 10px 16px;
  border-radius: 999px;
  background: #1d4ed8;
  color: #fff;
  font-size: 14px;
}

.hero-secondary {
  padding: 10px 16px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #e2e8f0;
  font-size: 14px;
}

.hero-panel {
  border-radius: 20px;
  background: #0f172a;
  color: #f8fafc;
  padding: 20px;
  display: grid;
  gap: 8px;
}

.panel-title {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.2em;
  color: #94a3b8;
}

.panel-metric {
  font-size: 40px;
  font-weight: 700;
}

.panel-sub {
  font-size: 12px;
  color: #cbd5f5;
}

.panel-bars {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.panel-bar {
  height: 6px;
  background: rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  overflow: hidden;
}

.panel-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #38bdf8, #6366f1);
}

.keyword-card {
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  display: grid;
  gap: 8px;
}

.keyword-title {
  font-weight: 600;
  color: #1e293b;
}

.keyword-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.keyword-note {
  font-size: 12px;
  color: #64748b;
}

.link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
}
</style>
