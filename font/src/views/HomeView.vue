<script setup lang="ts">
const highlights = [
  { label: '进行中任务', value: '6', note: '过去 24 小时' },
  { label: '采集成功率', value: '92%', note: '最近 7 天' },
  { label: '页面平均耗时', value: '2.8s', note: '中位数' },
  { label: '新增链接', value: '214', note: '今日捕获' },
]

const focusKeywords = [
  { name: '古汉语', tags: ['搜狐', '必应'] },
  { name: '新能源', tags: ['百科', '必应'] },
  { name: '风险治理', tags: ['搜狐'] },
]

const suggestions = [
  '新增任务建议错峰投放，以减少节点拥塞。',
  '今天百度百科命中率较高，推荐优先爬取。',
  '失败任务多集中在 Bing 的深度翻页。',
]

const recentLinks = [
  { title: '新能源产业发展趋势', source: '搜狐', time: '10:24' },
  { title: '量子计算研究进展', source: '必应', time: '10:18' },
  { title: '古汉语词义演化', source: '百科', time: '09:57' },
]
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
      <el-card v-for="item in highlights" :key="item.label" class="shadow-sm">
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
            <div class="keyword-note">建议优先投放夜间节点</div>
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
              <div class="text-xs text-gray-400">{{ link.source }}</div>
            </div>
            <div class="text-xs text-gray-400">{{ link.time }}</div>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <span class="font-semibold">下一步建议</span>
        </template>
        <ol class="text-sm text-gray-600 space-y-3">
          <li>1. 确认关键词策略并分配到高命中站点。</li>
          <li>2. 检查异常任务并进行缓存补录。</li>
          <li>3. 将高价值链接加入下载列表。</li>
          <li>4. 在任务列表中标记优先级。</li>
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
