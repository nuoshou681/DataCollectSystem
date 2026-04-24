<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchTaskLogs } from '@/api/api'
import type { TaskLog } from '@/types/entity'

const filters = ref({ level: 'ALL', keyword: '' })
const logs = ref<TaskLog[]>([])
const loading = ref(false)

const filteredLogs = computed(() => {
  const keyword = filters.value.keyword.trim().toLowerCase()
  return logs.value.filter(log => {
    const matchesLevel = filters.value.level === 'ALL' || log.logLevel === filters.value.level
    const matchesKeyword =
      !keyword ||
      String(log.taskId).includes(keyword) ||
      String(log.nodeId ?? '').includes(keyword) ||
      log.logMessage.toLowerCase().includes(keyword)
    return matchesLevel && matchesKeyword
  })
})

const stats = computed(() => {
  const total = logs.value.length
  const errors = logs.value.filter(log => log.logLevel === 'ERROR').length
  const warns = logs.value.filter(log => log.logLevel === 'WARN').length
  return { total, errors, warns }
})

async function loadLogs() {
  loading.value = true
  try {
    logs.value = await fetchTaskLogs()
  } catch {
    ElMessage.error('日志加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadLogs()
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <el-card>
        <div class="text-sm text-gray-500">日志总数</div>
        <div class="text-2xl font-semibold mt-2">{{ stats.total }}</div>
        <div class="text-xs text-gray-400 mt-1">实时统计</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">错误事件</div>
        <div class="text-2xl font-semibold mt-2 text-rose-600">{{ stats.errors }}</div>
        <div class="text-xs text-gray-400 mt-1">需要复盘</div>
      </el-card>
      <el-card>
        <div class="text-sm text-gray-500">警告事件</div>
        <div class="text-2xl font-semibold mt-2 text-amber-600">{{ stats.warns }}</div>
        <div class="text-xs text-gray-400 mt-1">关注异常</div>
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

      <el-table :data="filteredLogs" border stripe v-loading="loading">
        <el-table-column prop="logId" label="日志ID" width="120" />
        <el-table-column prop="logLevel" label="级别" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.logLevel === 'ERROR' ? 'danger' : scope.row.logLevel === 'WARN' ? 'warning' : 'info'"
              size="small"
            >
              {{ scope.row.logLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskId" label="任务ID" width="120" />
        <el-table-column prop="nodeId" label="节点" width="140" />
        <el-table-column prop="logMessage" label="内容" min-width="260" />
      </el-table>
    </el-card>
  </div>
</template>
