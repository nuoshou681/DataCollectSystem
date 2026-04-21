<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchCrawlerNodes, fetchPageResults, fetchTaskLogs, fetchTasks } from '@/api/api'
import type { CrawlerNode, CrawlerPageResult, Task, TaskLog } from '@/types/entity'

const loading = ref(false)
const nodes = ref<CrawlerNode[]>([])
const tasks = ref<Task[]>([])
const logs = ref<TaskLog[]>([])
const pageResults = ref<CrawlerPageResult[]>([])

const errorRate = computed(() => {
  if (!logs.value.length) {
    return 0
  }
  const errorCount = logs.value.filter(log => log.logLevel === 'ERROR').length
  return Math.round((errorCount / logs.value.length) * 100)
})

const highlights = computed(() => [
  {
    label: '任务总量',
    value: String(tasks.value.length),
    note: '当前任务数',
    tone: 'text-emerald-600',
  },
  {
    label: '在线节点',
    value: String(nodes.value.filter(node => node.status === 'ONLINE').length),
    note: '实时节点状态',
    tone: 'text-sky-600',
  },
  {
    label: '错误率',
    value: `${errorRate.value}%`,
    note: '基于审计日志',
    tone: 'text-rose-600',
  },
  {
    label: '排队任务',
    value: String(tasks.value.filter(task => task.taskStatus === 'PENDING' || !task.taskStatus).length),
    note: '等待分配',
    tone: 'text-amber-600',
  },
])

const alerts = computed(() => {
  const items = logs.value
    .filter(log => log.logLevel === 'ERROR' || log.logLevel === 'WARN')
    .slice(0, 3)
    .map(log => ({
      title: `${log.logLevel} · 任务 #${log.taskId}`,
      detail: log.logMessage,
      severity: log.logLevel === 'ERROR' ? 'danger' : 'warning',
    }))
  if (!items.length) {
    return [{ title: '当前无活跃告警', detail: '系统运行稳定', severity: 'info' }]
  }
  return items
})

const activity = computed(() => {
  return logs.value.slice(0, 4).map(log => ({
    time: `#${log.logId}`,
    action: log.logMessage,
    actor: `节点 ${log.nodeId ?? '-'}`,
  }))
})

const queueBands = computed(() => {
  const running = tasks.value.filter(task => task.taskStatus === 'RUNNING').length
  const pending = tasks.value.filter(task => task.taskStatus === 'PENDING' || !task.taskStatus).length
  const failed = tasks.value.filter(task => task.taskStatus === 'FAILED' || task.taskStatus === 'PARTIAL_FAILED').length
  const total = Math.max(tasks.value.length, 1)
  return [
    { label: '执行中', value: Math.round((running / total) * 100), color: 'bg-sky-500' },
    { label: '排队中', value: Math.round((pending / total) * 100), color: 'bg-emerald-500' },
    { label: '异常', value: Math.round((failed / total) * 100), color: 'bg-rose-500' },
  ]
})

async function loadDashboard() {
  loading.value = true
  try {
    const [nodeData, taskData, logData, pageData] = await Promise.all([
      fetchCrawlerNodes(),
      fetchTasks(),
      fetchTaskLogs(),
      fetchPageResults(),
    ])
    nodes.value = nodeData
    tasks.value = taskData
    logs.value = logData
    pageResults.value = pageData
  } catch {
    ElMessage.error('控制台数据加载失败')
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
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card v-for="item in highlights" :key="item.label" class="shadow-sm" v-loading="loading">
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
                <li>• 当前排队任务 {{ tasks.filter(task => task.taskStatus === 'PENDING' || !task.taskStatus).length }} 个。</li>
                <li>• 在线节点 {{ nodes.filter(node => node.status === 'ONLINE').length }} 个。</li>
                <li>• 最近错误日志 {{ logs.filter(log => log.logLevel === 'ERROR').length }} 条。</li>
              </ul>
            </div>
            <div class="rounded-xl border border-slate-100 p-4">
              <div class="text-sm font-semibold">容量分布</div>
              <div class="grid grid-cols-2 gap-3 mt-3 text-xs text-gray-500">
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">页面结果</div>
                  <div class="mt-2">总量: {{ pageResults.length }}</div>
                </div>
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">任务失败</div>
                  <div class="mt-2">数量: {{ tasks.filter(task => task.taskStatus === 'FAILED' || task.taskStatus === 'PARTIAL_FAILED').length }}</div>
                </div>
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">排队任务</div>
                  <div class="mt-2">数量: {{ tasks.filter(task => task.taskStatus === 'PENDING' || !task.taskStatus).length }}</div>
                </div>
                <div class="rounded-lg bg-slate-50 p-3">
                  <div class="font-medium text-gray-700">在线节点</div>
                  <div class="mt-2">数量: {{ nodes.filter(node => node.status === 'ONLINE').length }}</div>
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
