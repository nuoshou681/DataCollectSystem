<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchTaskBatchDetail, fetchTaskBatches } from '@/api/api'
import type { TaskBatch, TaskBatchDetail } from '@/types/entity'

const loading = ref(false)
const batches = ref<TaskBatch[]>([])
const detailLoading = ref(false)
const activeDetail = ref<TaskBatchDetail | null>(null)
const detailVisible = ref(false)

async function loadBatches() {
  loading.value = true
  try {
    batches.value = await fetchTaskBatches()
  } catch {
    ElMessage.error('任务批次加载失败')
  } finally {
    loading.value = false
  }
}

const stats = computed(() => ({
  total: batches.value.length,
  running: batches.value.filter(item => item.status === 'RUNNING').length,
  finished: batches.value.filter(item => item.status === 'FINISHED').length,
  partialFailed: batches.value.filter(item => item.status === 'PARTIAL_FAILED').length,
}))

onMounted(() => {
  void loadBatches()
})

async function openDetail(batchId: string) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    activeDetail.value = await fetchTaskBatchDetail(batchId)
  } catch {
    ElMessage.error('批次详情加载失败')
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card><div class="text-sm text-gray-500">批次总数</div><div class="text-2xl font-semibold mt-2">{{ stats.total }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">执行中</div><div class="text-2xl font-semibold mt-2 text-sky-600">{{ stats.running }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">已完成</div><div class="text-2xl font-semibold mt-2 text-emerald-600">{{ stats.finished }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">部分失败</div><div class="text-2xl font-semibold mt-2 text-rose-600">{{ stats.partialFailed }}</div></el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">任务批次管理</span>
          <el-button text type="primary" @click="loadBatches">刷新</el-button>
        </div>
      </template>

      <el-table :data="batches" border stripe v-loading="loading">
        <el-table-column prop="batchId" label="批次号" min-width="180" />
        <el-table-column prop="batchName" label="批次名" min-width="180" />
        <el-table-column prop="taskCount" label="任务数" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'FINISHED' ? 'success' : scope.row.status === 'RUNNING' ? 'warning' : scope.row.status === 'PARTIAL_FAILED' ? 'danger' : 'info'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" min-width="220" />
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button text type="primary" @click="openDetail(scope.row.batchId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" title="批次详情" size="50%">
      <div v-loading="detailLoading" class="space-y-4">
        <template v-if="activeDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="批次号">{{ activeDetail.batch.batchId }}</el-descriptions-item>
            <el-descriptions-item label="批次名称">{{ activeDetail.batch.batchName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ activeDetail.batch.status }}</el-descriptions-item>
            <el-descriptions-item label="任务数">{{ activeDetail.batch.taskCount }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ activeDetail.batch.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ activeDetail.batch.updatedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ activeDetail.batch.notes || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="activeDetail.tasks" border stripe>
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column prop="keyword" label="关键词" min-width="140" />
            <el-table-column prop="url" label="种子地址" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.runtime?.status === 'FINISHED' ? 'success' : scope.row.runtime?.status === 'RUNNING' ? 'warning' : scope.row.runtime?.status === 'PARTIAL_FAILED' || scope.row.runtime?.status === 'FAILED' ? 'danger' : 'info'">
                  {{ scope.row.runtime?.status || scope.row.taskStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" min-width="180">
              <template #default="scope">
                {{ scope.row.runtime?.completedPages ?? 0 }}/{{ scope.row.runtime?.expectedPages ?? scope.row.totalPages ?? 0 }}
              </template>
            </el-table-column>
            <el-table-column label="节点" min-width="140">
              <template #default="scope">
                {{ scope.row.runtime?.assignedNodeId || scope.row.nodeId || '未分配' }}
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>
