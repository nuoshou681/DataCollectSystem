<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchExportRecords } from '@/api/api'
import type { ExportRecord } from '@/types/entity'

const loading = ref(false)
const records = ref<ExportRecord[]>([])
const scopeFilter = ref('ALL')
const typeFilter = ref('ALL')

async function loadRecords() {
  loading.value = true
  try {
    records.value = await fetchExportRecords()
  } catch {
    ElMessage.error('管理员导出中心加载失败')
  } finally {
    loading.value = false
  }
}

const stats = computed(() => ({
  total: records.value.length,
  task: records.value.filter(item => item.exportScope === 'TASK').length,
  resultCenter: records.value.filter(item => item.exportScope === 'RESULT_CENTER').length,
  csv: records.value.filter(item => item.exportType === 'CSV').length,
  mhtml: records.value.filter(item => item.exportType === 'MHTML_ZIP').length,
}))

const filteredRecords = computed(() => {
  return records.value.filter(item => {
    const matchesScope = scopeFilter.value === 'ALL' || item.exportScope === scopeFilter.value
    const matchesType = typeFilter.value === 'ALL' || item.exportType === typeFilter.value
    return matchesScope && matchesType
  })
})

const recentTaskExports = computed(() => {
  return filteredRecords.value
    .filter(item => item.taskId)
    .slice(0, 8)
})

onMounted(() => {
  void loadRecords()
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-5 gap-4">
      <el-card v-loading="loading"><div class="text-sm text-slate-500">导出总量</div><div class="mt-2 text-2xl font-semibold">{{ stats.total }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">任务级导出</div><div class="mt-2 text-2xl font-semibold text-sky-600">{{ stats.task }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">结果中心导出</div><div class="mt-2 text-2xl font-semibold text-emerald-600">{{ stats.resultCenter }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">CSV 导出</div><div class="mt-2 text-2xl font-semibold text-amber-600">{{ stats.csv }}</div></el-card>
      <el-card v-loading="loading"><div class="text-sm text-slate-500">MHTML 打包</div><div class="mt-2 text-2xl font-semibold text-violet-600">{{ stats.mhtml }}</div></el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[1.15fr_0.95fr] gap-6">
      <el-card>
        <template #header>
          <div class="flex items-center justify-between gap-4 flex-wrap">
            <span class="font-semibold">导出记录治理</span>
            <div class="flex items-center gap-3 flex-wrap">
              <el-select v-model="scopeFilter" size="small" style="width: 160px">
                <el-option label="全部范围" value="ALL" />
                <el-option label="TASK" value="TASK" />
                <el-option label="RESULT_CENTER" value="RESULT_CENTER" />
              </el-select>
              <el-select v-model="typeFilter" size="small" style="width: 160px">
                <el-option label="全部类型" value="ALL" />
                <el-option label="CSV" value="CSV" />
                <el-option label="MHTML_ZIP" value="MHTML_ZIP" />
              </el-select>
              <el-button text type="primary" @click="loadRecords">刷新</el-button>
            </div>
          </div>
        </template>

        <el-table :data="filteredRecords" border stripe v-loading="loading">
          <el-table-column prop="exportId" label="导出ID" width="100" />
          <el-table-column prop="taskId" label="任务ID" width="100" />
          <el-table-column prop="exportScope" label="范围" width="150" />
          <el-table-column prop="exportType" label="类型" width="130" />
          <el-table-column prop="fileName" label="文件名" min-width="220" />
          <el-table-column prop="recordCount" label="记录数" width="100" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="createdAt" label="导出时间" min-width="180" />
        </el-table>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">最近任务级导出</span></template>
        <div class="space-y-3">
          <div v-for="item in recentTaskExports" :key="item.exportId" class="rounded-xl border border-slate-200 p-4">
            <div class="flex items-center justify-between gap-3">
              <div class="font-medium">任务 #{{ item.taskId }}</div>
              <el-tag type="info">{{ item.exportType }}</el-tag>
            </div>
            <div class="mt-2 text-sm text-slate-600">{{ item.fileName || '-' }}</div>
            <div class="mt-2 text-xs text-slate-400">
              记录数 {{ item.recordCount ?? 0 }} · {{ item.createdAt || '-' }}
            </div>
          </div>
          <el-empty v-if="!recentTaskExports.length" description="暂无任务级导出记录" />
        </div>
      </el-card>
    </section>
  </div>
</template>
