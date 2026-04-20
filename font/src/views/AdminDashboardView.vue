<script setup lang="ts">
import { computed } from 'vue'

const highlights = [
  { label: '任务总量', value: '128', note: '过去 24 小时', tone: 'text-emerald-600' },
  { label: '平均爬取耗时', value: '3m 42s', note: '最近 1 小时', tone: 'text-sky-600' },
  { label: '错误率', value: '2.4%', note: '最近 6 小时', tone: 'text-rose-600' },
  { label: '队列深度', value: '42', note: '等待中', tone: 'text-amber-600' },
]

const alerts = [
  {
    title: '节点 node-3 心跳延迟',
    detail: '上次心跳 28 秒前，建议分流或暂时降载。',
    severity: 'warning',
  },
  {
    title: '关键词热度突增',
    detail: '搜狐关键词量在 30 分钟内上涨 38%。',
    severity: 'info',
  },
  {
    title: '失败任务聚集',
    detail: '必应出现 5 个超时任务。',
    severity: 'danger',
  },
]

const activity = [
  { time: '09:12', action: 'node-2 标记为 BUSY', actor: '自动监控' },
  { time: '09:18', action: '为搜狐排队 14 个任务', actor: '调度器' },
  { time: '09:24', action: 'node-4 完成重启', actor: '运维' },
  { time: '09:30', action: '缓存清理完成', actor: '清理器' },
]

const queueBands = computed(() => [
  { label: '高优先级', value: 18, color: 'bg-sky-500' },
  { label: '普通', value: 54, color: 'bg-emerald-500' },
  { label: '后台', value: 28, color: 'bg-slate-400' },
])
</script>

<template>
  <div class="space-y-8">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card v-for="item in highlights" :key="item.label" class="shadow-sm">
        <div class="text-sm text-gray-500">{{ item.label }}</div>
        <div class="text-2xl font-semibold mt-2" :class="item.tone">{{ item.value }}</div>
        <div class="text-xs text-gray-400 mt-1">{{ item.note }}</div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <el-card class="xl:col-span-2">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">队列健康度</span>
            <span class="text-xs text-gray-400">实时快照</span>
          </div>
        </template>
        <div class="space-y-4">
          <div class="grid grid-cols-3 gap-4">
            <div v-for="band in queueBands" :key="band.label" class="rounded-xl border border-slate-100 p-4">
              <div class="text-xs text-gray-500">{{ band.label }}</div>
              <div class="text-xl font-semibold mt-2">{{ band.value }}</div>
              <div class="mt-3 h-2 rounded-full bg-slate-100">
                <div class="h-2 rounded-full" :class="band.color" :style="{ width: band.value + '%' }" />
              </div>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="rounded-xl border border-slate-100 p-4">
              <div class="text-sm font-semibold">路由建议</div>
              <ul class="text-xs text-gray-500 mt-2 space-y-1">
                <li>• 必应任务优先分配到 node-1/node-4 至 11:00。</li>
                <li>• 搜狐任务优先使用缓存查询。</li>
                <li>• 高峰期对百科任务进行 10% 限流。</li>
              </ul>
            </div>
            <div class="rounded-xl border border-slate-100 p-4">
              <div class="text-sm font-semibold">容量分布</div>
              <div class="grid grid-cols-2 gap-3 mt-3 text-xs text-gray-500">
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">北区集群</div>
                  <div class="mt-2">利用率: 64%</div>
                </div>
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">东区集群</div>
                  <div class="mt-2">利用率: 81%</div>
                </div>
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">沙盒区</div>
                  <div class="mt-2">利用率: 33%</div>
                </div>
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">恢复区</div>
                  <div class="mt-2">利用率: 12%</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <span class="font-semibold">活跃告警</span>
        </template>
        <div class="space-y-3">
          <div
            v-for="alert in alerts"
            :key="alert.title"
            class="rounded-xl border border-slate-100 p-3"
          >
            <div class="flex items-center justify-between">
              <div class="font-medium">{{ alert.title }}</div>
              <el-tag :type="alert.severity === 'danger' ? 'danger' : alert.severity === 'warning' ? 'warning' : 'info'" size="small">
                {{ alert.severity.toUpperCase() }}
              </el-tag>
            </div>
            <div class="text-xs text-gray-500 mt-2">{{ alert.detail }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-2 gap-6">
      <el-card>
        <template #header>
          <span class="font-semibold">运维时间线</span>
        </template>
        <div class="space-y-3">
          <div v-for="item in activity" :key="item.time" class="flex items-start gap-3">
            <div class="w-12 text-xs text-gray-400">{{ item.time }}</div>
            <div class="flex-1 rounded-lg bg-slate-50 p-3">
              <div class="text-sm font-medium text-gray-700">{{ item.action }}</div>
              <div class="text-xs text-gray-500 mt-1">{{ item.actor }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header>
          <span class="font-semibold">管理员操作指引</span>
        </template>
        <ol class="text-sm text-gray-600 space-y-3">
          <li>1. 观察队列波动并将任务分流到健康节点。</li>
          <li>2. 检查节点心跳并更新维护状态。</li>
          <li>3. 定位错误聚集并触发重试。</li>
          <li>4. 导出每日采集汇总用于审计。</li>
        </ol>
      </el-card>
    </section>
  </div>
</template>
