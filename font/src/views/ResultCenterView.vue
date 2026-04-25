<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  bindResultTag,
  createResultTag,
  deleteResultTag,
  exportPageResultMhtml,
  exportPageResults,
  fetchBookmarks,
  fetchPageResultTagBindings,
  fetchResultTags,
  removeBookmark,
  unbindResultTag,
} from '@/api/api'
import type { CrawlerPageResult, ResultTag } from '@/types/entity'

const loading = ref(false)
const exporting = ref(false)
const exportingMhtml = ref(false)
const tagLoading = ref(false)
const tagDialogVisible = ref(false)
const selectedTagId = ref<number | null>(null)
const categoryFilter = ref('')
const bookmarks = ref<CrawlerPageResult[]>([])
const resultTags = ref<ResultTag[]>([])
const tagBindingMap = ref<Record<number, ResultTag[]>>({})
const tagForm = reactive<ResultTag>({
  tagName: '',
  tagColor: '#2563eb',
  categoryName: '',
  description: '',
})

async function loadBookmarks() {
  loading.value = true
  try {
    bookmarks.value = await fetchBookmarks()
    const pageResultIds = bookmarks.value.map(item => item.pageResultId).filter((id): id is number => Boolean(id))
    tagBindingMap.value = pageResultIds.length ? await fetchPageResultTagBindings(pageResultIds) : {}
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

async function exportAllMhtml() {
  exportingMhtml.value = true
  try {
    const result = await exportPageResultMhtml()
    triggerBrowserDownload(result.blob, result.fileName)
    ElMessage.success('MHTML 导出成功')
  } catch {
    ElMessage.error('MHTML 导出失败')
  } finally {
    exportingMhtml.value = false
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

async function loadTags() {
  tagLoading.value = true
  try {
    resultTags.value = await fetchResultTags()
  } catch {
    ElMessage.error('结果标签加载失败')
  } finally {
    tagLoading.value = false
  }
}

function resetTagForm() {
  tagForm.tagName = ''
  tagForm.tagColor = '#2563eb'
  tagForm.categoryName = ''
  tagForm.description = ''
}

async function submitTag() {
  if (!tagForm.tagName.trim()) {
    ElMessage.error('标签名称不能为空')
    return
  }
  try {
    await createResultTag(tagForm)
    ElMessage.success('标签创建成功')
    resetTagForm()
    await loadTags()
  } catch {
    ElMessage.error('标签创建失败')
  }
}

async function removeTag(tagId?: number) {
  if (!tagId) {
    return
  }
  try {
    await deleteResultTag(tagId)
    ElMessage.success('标签已删除')
    await Promise.all([loadTags(), loadBookmarks()])
  } catch {
    ElMessage.error('删除标签失败')
  }
}

async function bindTag(pageResultId?: number) {
  if (!pageResultId || !selectedTagId.value) {
    ElMessage.error('请选择标签')
    return
  }
  try {
    await bindResultTag(selectedTagId.value, pageResultId)
    ElMessage.success('标签绑定成功')
    await loadBookmarks()
  } catch {
    ElMessage.error('标签绑定失败')
  }
}

async function removeTagFromResult(tagId?: number, pageResultId?: number) {
  if (!tagId || !pageResultId) {
    return
  }
  try {
    await unbindResultTag(tagId, pageResultId)
    ElMessage.success('标签解绑成功')
    await loadBookmarks()
  } catch {
    ElMessage.error('标签解绑失败')
  }
}

const filteredBookmarks = computed(() => {
  return bookmarks.value.filter(item => {
    const tags = item.pageResultId ? tagBindingMap.value[item.pageResultId] ?? [] : []
    if (!categoryFilter.value.trim()) {
      item.tags = tags
      return true
    }
    const matched = tags.some(tag => (tag.categoryName ?? '').includes(categoryFilter.value.trim()) || tag.tagName.includes(categoryFilter.value.trim()))
    item.tags = tags
    return matched
  })
})

const stats = computed(() => ({
  total: bookmarks.value.length,
  success: bookmarks.value.filter(item => item.success).length,
  failed: bookmarks.value.filter(item => !item.success).length,
  categories: new Set(resultTags.value.map(item => item.categoryName || '未分类')).size,
}))

onMounted(() => {
  void Promise.all([loadBookmarks(), loadTags()])
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card><div class="text-sm text-gray-500">收藏结果</div><div class="text-2xl font-semibold mt-2">{{ stats.total }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">成功结果</div><div class="text-2xl font-semibold mt-2 text-emerald-600">{{ stats.success }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">失败结果</div><div class="text-2xl font-semibold mt-2 text-rose-600">{{ stats.failed }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">标签分类</div><div class="text-2xl font-semibold mt-2 text-sky-600">{{ stats.categories }}</div></el-card>
    </section>

    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">结果中心</span>
          <div class="flex items-center gap-3">
            <el-input v-model="categoryFilter" size="small" placeholder="按标签/分类筛选" style="width: 220px" />
            <el-button text type="primary" @click="loadBookmarks">刷新</el-button>
            <el-button type="success" :loading="exporting" @click="exportAll">导出 CSV</el-button>
            <el-button type="warning" :loading="exportingMhtml" @click="exportAllMhtml">导出 MHTML</el-button>
            <el-button type="primary" plain @click="tagDialogVisible = true">标签管理</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredBookmarks" border stripe v-loading="loading">
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
        <el-table-column label="标签" min-width="240">
          <template #default="scope">
            <div class="flex flex-wrap gap-2">
              <el-tag
                v-for="tag in scope.row.tags || []"
                :key="`${scope.row.pageResultId}-${tag.tagId}`"
                closable
                :color="tag.tagColor || undefined"
                effect="dark"
                @close="removeTagFromResult(tag.tagId, scope.row.pageResultId)"
              >
                {{ tag.categoryName ? `${tag.categoryName} / ${tag.tagName}` : tag.tagName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-select v-model="selectedTagId" size="small" placeholder="选标签" style="width: 100px">
              <el-option v-for="tag in resultTags" :key="tag.tagId" :label="tag.tagName" :value="tag.tagId" />
            </el-select>
            <el-button size="small" plain type="primary" @click="bindTag(scope.row.pageResultId)">打标签</el-button>
            <el-button size="small" plain type="danger" @click="unbookmark(scope.row.pageResultId)">取消收藏</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="tagDialogVisible" title="结果标签管理" width="760px">
      <div class="grid grid-cols-1 md:grid-cols-[320px_1fr] gap-6">
        <el-card shadow="never">
          <template #header><span class="font-semibold">新增标签</span></template>
          <el-form label-width="90px">
            <el-form-item label="标签名">
              <el-input v-model="tagForm.tagName" maxlength="30" />
            </el-form-item>
            <el-form-item label="分类">
              <el-input v-model="tagForm.categoryName" placeholder="例如：内容质量" />
            </el-form-item>
            <el-form-item label="颜色">
              <el-color-picker v-model="tagForm.tagColor" />
            </el-form-item>
            <el-form-item label="说明">
              <el-input v-model="tagForm.description" type="textarea" :rows="3" />
            </el-form-item>
            <el-button type="primary" @click="submitTag">创建标签</el-button>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <template #header><span class="font-semibold">我的标签</span></template>
          <el-table :data="resultTags" border stripe v-loading="tagLoading">
            <el-table-column prop="tagName" label="标签名" min-width="120" />
            <el-table-column prop="categoryName" label="分类" min-width="120" />
            <el-table-column label="颜色" width="90">
              <template #default="scope">
                <span class="inline-block w-6 h-6 rounded-full border" :style="{ backgroundColor: scope.row.tagColor || '#94a3b8' }" />
              </template>
            </el-table-column>
            <el-table-column prop="bindingCount" label="使用次数" width="90" />
            <el-table-column prop="description" label="说明" min-width="160" />
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button size="small" plain type="danger" @click="removeTag(scope.row.tagId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>
