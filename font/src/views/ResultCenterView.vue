<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportPageResults, fetchBookmarks, removeBookmark } from '@/api/api'
import type { CrawlerPageResult } from '@/types/entity'

const loading = ref(false)
const exporting = ref(false)
const bookmarks = ref<CrawlerPageResult[]>([])

async function loadBookmarks() {
  loading.value = true
  try {
    bookmarks.value = await fetchBookmarks()
  } catch {
    ElMessage.error('收藏结果加载失败')
  } finally {
    loading.value = false
  }
}

function triggerBrowserDownload(blob: Blob, fileName: string) {
  const objectUrl = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(objectUrl)
}

async function exportAll() {
  exporting.value = true
  try {
    const result = await exportPageResults()
    triggerBrowserDownload(result.blob, result.fileName)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

async function unbookmark(pageResultId?: number) {
  if (!pageResultId) {
    return
  }
  try {
    await removeBookmark(pageResultId)
    ElMessage.success('已取消收藏')
    await loadBookmarks()
  } catch {
    ElMessage.error('取消收藏失败')
  }
}

const stats = computed(() => ({
  total: bookmarks.value.length,
  success: bookmarks.value.filter(item => item.success).length,
  failed: bookmarks.value.filter(item => !item.success).length,
}))

onMounted(() => {
  void loadBookmarks()
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <el-card><div class="text-sm text-gray-500">收藏结果</div><div class="text-2xl font-semibold mt-2">{{ stats.total }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">成功结果</div><div class="text-2xl font-semibold mt-2 text-emerald-600">{{ stats.success }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">失败结果</div><div class="text-2xl font-semibold mt-2 text-rose-600">{{ stats.failed }}</div></el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">结果中心</span>
          <div class="flex items-center gap-3">
            <el-button text type="primary" @click="loadBookmarks">刷新</el-button>
            <el-button type="success" :loading="exporting" @click="exportAll">导出 CSV</el-button>
          </div>
        </div>
      </template>

      <el-table :data="bookmarks" border stripe v-loading="loading">
        <el-table-column prop="pageResultId" label="结果ID" width="100" />
        <el-table-column prop="taskId" label="任务ID" width="100" />
        <el-table-column prop="pageTitle" label="标题" min-width="200" />
        <el-table-column prop="pageUrl" label="链接" min-width="240" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.success ? 'success' : 'danger'">{{ scope.row.success ? 'SUCCESS' : 'FAILED' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="siteType" label="站点" width="120" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" plain type="danger" @click="unbookmark(scope.row.pageResultId)">取消收藏</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
