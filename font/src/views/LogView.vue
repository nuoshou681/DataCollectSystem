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
    const matchesKeyword = !keyword
      || String(log.taskId).includes(keyword)
      || String(log.nodeKey ?? log.nodeId ?? '').toLowerCase().includes(keyword)
      || log.logMessage.toLowerCase().includes(keyword)
    return matchesLevel && matchesKeyword
  })
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
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">任务事件镜像日志</span>
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
        <el-table-column prop="logId" label="日志ID" width="100" />
        <el-table-column prop="logLevel" label="级别" width="100" />
        <el-table-column prop="taskId" label="任务ID" width="100" />
        <el-table-column label="节点" width="160">
          <template #default="scope">{{ scope.row.nodeKey || scope.row.nodeId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="180" />
        <el-table-column prop="logMessage" label="内容" min-width="320" />
      </el-table>
    </el-card>
  </div>
</template>
