<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArchiveBoxIcon, ArrowDownTrayIcon, DocumentTextIcon } from '@heroicons/vue/24/outline'
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
    ElMessage.error('导出记录加载失败')
  } finally {
    loading.value = false
  }
}

const stats = computed(() => ({
  total: records.value.length,
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

onMounted(() => { void loadRecords() })
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <ArrowDownTrayIcon class="w-6 h-6 text-blue-500" />
          <div>
            <div class="text-sm text-slate-500">导出总量</div>
            <div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <DocumentTextIcon class="w-6 h-6 text-emerald-500" />
          <div>
            <div class="text-sm text-slate-500">CSV 导出</div>
            <div class="text-2xl font-bold text-emerald-600">{{ stats.csv }}</div>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-violet-500" v-loading="loading">
        <div class="flex items-center gap-3">
          <ArchiveBoxIcon class="w-6 h-6 text-violet-500" />
          <div>
            <div class="text-sm text-slate-500">MHTML 打包</div>
            <div class="text-2xl font-bold text-violet-600">{{ stats.mhtml }}</div>
          </div>
        </div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between gap-4 flex-wrap">
          <span class="font-semibold">导出记录</span>
          <div class="flex items-center gap-3 flex-wrap">
            <el-select v-model="scopeFilter" size="small" style="width: 140px">
              <el-option label="全部范围" value="ALL" />
              <el-option label="TASK" value="TASK" />
              <el-option label="RESULT_CENTER" value="RESULT_CENTER" />
            </el-select>
            <el-select v-model="typeFilter" size="small" style="width: 140px">
              <el-option label="全部类型" value="ALL" />
              <el-option label="CSV" value="CSV" />
              <el-option label="MHTML_ZIP" value="MHTML_ZIP" />
            </el-select>
            <el-button size="small" text type="primary" @click="loadRecords">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredRecords" border stripe v-loading="loading">
        <el-table-column prop="exportId" label="导出ID" width="100" />
        <el-table-column prop="taskId" label="任务ID" width="100" />
        <el-table-column prop="exportScope" label="范围" width="140" />
        <el-table-column prop="exportType" label="类型" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
        <el-table-column prop="recordCount" label="记录数" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'COMPLETED' ? 'success' : scope.row.status === 'FAILED' ? 'danger' : 'info'" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="导出时间" min-width="170" />
        <template #empty><el-empty description="暂无导出记录" :image-size="80" /></template>
      </el-table>
    </el-card>
  </div>
</template>
