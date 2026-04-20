<script lang="ts" setup>
import { ref } from 'vue'

const filters = ref({ level: 'ALL', keyword: '' })

const logs = ref([
  { time: '2026-04-20 10:22', level: 'INFO', source: 'Scheduler', taskId: 42, nodeId: 'node-1', message: 'queued 8 tasks for Sohu' },
  { time: '2026-04-20 10:21', level: 'WARN', source: 'NodeMonitor', taskId: 37, nodeId: 'node-3', message: 'heartbeat delay 18s' },
  { time: '2026-04-20 10:18', level: 'ERROR', source: 'Crawler', taskId: 35, nodeId: 'node-2', message: 'timeout on page index 5' },
  { time: '2026-04-20 10:15', level: 'INFO', source: 'Cache', taskId: 31, nodeId: 'node-4', message: 'mhtml cached' },
])
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <el-card>
        <div class="text-sm text-gray-500">日志总数</div>
        <div class="text-2xl font-semibold mt-2">248</div>
        <div class="text-xs text-gray-400 mt-1">过去 24 小时</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">错误事件</div>
        <div class="text-2xl font-semibold mt-2 text-rose-600">12</div>
        <div class="text-xs text-gray-400 mt-1">需要复盘</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">队列写入</div>
        <div class="text-2xl font-semibold mt-2 text-emerald-600">+86</div>
        <div class="text-xs text-gray-400 mt-1">最近 1 小时</div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">审计日志</span>
          <div class="flex items-center gap-3">
            <el-select v-model="filters.level" size="small" placeholder="等级">
              <el-option label="ALL" value="ALL" />
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
            </el-select>
            <el-input v-model="filters.keyword" size="small" placeholder="搜索关键词" />
          </div>
        </div>
      </template>

      <el-table :data="logs" border stripe>
        <el-table-column prop="time" label="时间" width="180" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.level === 'ERROR' ? 'danger' : scope.row.level === 'WARN' ? 'warning' : 'info'"
              size="small"
            >
              {{ scope.row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="140" />
        <el-table-column prop="taskId" label="任务ID" width="120" />
        <el-table-column prop="nodeId" label="节点" width="140" />
        <el-table-column prop="message" label="内容" min-width="260" />
      </el-table>
    </el-card>
  </div>
</template>
