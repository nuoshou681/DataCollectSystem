<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const bulkOperating = ref(false)
const bookmarks = ref<CrawlerPageResult[]>([])
const resultTags = ref<ResultTag[]>([])
const tagBindingMap = ref<Record<number, ResultTag[]>>({})
const selectedRows = ref<CrawlerPageResult[]>([])
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

function handleSelectionChange(rows: CrawlerPageResult[]) {
  selectedRows.value = rows
}

async function bulkBindTag() {
  if (!selectedTagId.value || selectedRows.value.length === 0) {
    ElMessage.error('请先选择结果和标签')
    return
  }
  bulkOperating.value = true
  try {
    await Promise.all(
      selectedRows.value
        .map(item => item.pageResultId)
        .filter((id): id is number => Boolean(id))
        .map(pageResultId => bindResultTag(selectedTagId.value as number, pageResultId)),
    )
    ElMessage.success(`已批量打标签 ${selectedRows.value.length} 条`)
    await loadBookmarks()
  } catch {
    ElMessage.error('批量打标签失败')
  } finally {
    bulkOperating.value = false
  }
}

async function bulkUnbookmark() {
  if (selectedRows.value.length === 0) {
    ElMessage.error('请先选择要取消收藏的结果')
    return
  }
  try {
    await ElMessageBox.confirm(`确定取消收藏 ${selectedRows.value.length} 条结果吗？`, '批量取消收藏', {
      type: 'warning',
    })
  } catch {
    return
  }

  bulkOperating.value = true
  try {
    await Promise.all(
      selectedRows.value
        .map(item => item.pageResultId)
        .filter((id): id is number => Boolean(id))
        .map(pageResultId => removeBookmark(pageResultId)),
    )
    ElMessage.success(`已取消收藏 ${selectedRows.value.length} 条`)
    selectedRows.value = []
    await loadBookmarks()
  } catch {
    ElMessage.error('批量取消收藏失败')
  } finally {
    bulkOperating.value = false
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

const activeCategoryGroups = computed(() => {
  const groups = new Map<string, number>()
  for (const tag of resultTags.value) {
    const key = tag.categoryName || '未分类'
    groups.set(key, (groups.get(key) ?? 0) + (tag.bindingCount ?? 0))
  }
  return Array.from(groups.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
})

const topTags = computed(() => {
  return [...resultTags.value]
    .sort((a, b) => (b.bindingCount ?? 0) - (a.bindingCount ?? 0))
    .slice(0, 6)
})

const selectedCount = computed(() => selectedRows.value.length)

const hotspotSlices = computed(() => {
  const total = activeCategoryGroups.value.reduce((sum, item) => sum + item.count, 0)
  if (total <= 0) {
    return []
  }
  const palette = ['#0ea5e9', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#f97316', '#64748b']
  let cursor = 0
  return activeCategoryGroups.value.map((group, index) => {
    const ratio = group.count / total
    const start = cursor
    const end = cursor + ratio * 360
    cursor = end
    return {
      ...group,
      color: palette[index % palette.length],
      start,
      end,
      percent: Math.round(ratio * 1000) / 10,
    }
  })
})

const hotspotChartStyle = computed(() => {
  if (!hotspotSlices.value.length) {
    return {
      background: 'conic-gradient(#e2e8f0 0deg 360deg)',
    }
  }
  const segments = hotspotSlices.value.map(item => `${item.color} ${item.start}deg ${item.end}deg`)
  return {
    background: `conic-gradient(${segments.join(', ')})`,
  }
})

const stats = computed(() => ({
  total: bookmarks.value.length,
  success: bookmarks.value.filter(item => item.success).length,
  failed: bookmarks.value.filter(item => !item.success).length,
  categories: new Set(resultTags.value.map(item => item.categoryName || '未分类')).size,
  taggedResults: filteredBookmarks.value.filter(item => (item.tags?.length ?? 0) > 0).length,
}))

onMounted(() => {
  void Promise.all([loadBookmarks(), loadTags()])
})
</script>

<template>
  <div class="space-y-6">
    <section class="grid grid-cols-1 md:grid-cols-5 gap-4">
      <el-card><div class="text-sm text-gray-500">收藏结果</div><div class="text-2xl font-semibold mt-2">{{ stats.total }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">成功结果</div><div class="text-2xl font-semibold mt-2 text-emerald-600">{{ stats.success }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">失败结果</div><div class="text-2xl font-semibold mt-2 text-rose-600">{{ stats.failed }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">标签分类</div><div class="text-2xl font-semibold mt-2 text-sky-600">{{ stats.categories }}</div></el-card>
      <el-card><div class="text-sm text-gray-500">已打标签结果</div><div class="text-2xl font-semibold mt-2 text-amber-600">{{ stats.taggedResults }}</div></el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[1.2fr_1fr] gap-6">
      <el-card>
        <template #header><span class="font-semibold">标签使用榜</span></template>
        <div class="space-y-3">
          <div
            v-for="tag in topTags"
            :key="tag.tagId"
            class="rounded-xl border border-slate-200 px-4 py-3"
          >
            <div class="flex items-center justify-between gap-4">
              <div class="flex items-center gap-3">
                <span class="inline-block h-3 w-3 rounded-full" :style="{ backgroundColor: tag.tagColor || '#94a3b8' }" />
                <div>
                  <div class="font-medium">{{ tag.tagName }}</div>
                  <div class="text-xs text-slate-500">{{ tag.categoryName || '未分类' }}</div>
                </div>
              </div>
              <el-tag type="info">{{ tag.bindingCount || 0 }} 次</el-tag>
            </div>
          </div>
          <el-empty v-if="!topTags.length" description="还没有标签使用数据" />
        </div>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">分类分布</span></template>
        <div class="space-y-4">
          <div
            v-for="group in activeCategoryGroups"
            :key="group.name"
            class="space-y-2"
          >
            <div class="flex items-center justify-between text-sm">
              <span>{{ group.name }}</span>
              <span class="text-slate-500">{{ group.count }}</span>
            </div>
            <el-progress
              :percentage="stats.total ? Math.min(100, Math.round((group.count / Math.max(stats.total, 1)) * 100)) : 0"
              :stroke-width="10"
              status="success"
            />
          </div>
          <el-empty v-if="!activeCategoryGroups.length" description="还没有分类数据" />
        </div>
      </el-card>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-[1fr_1.2fr] gap-6">
      <el-card>
        <template #header><span class="font-semibold">采集热点方向分析</span></template>
        <div class="hotspot-panel">
          <div class="hotspot-chart-wrap">
            <div class="hotspot-chart" :style="hotspotChartStyle">
              <div class="hotspot-hole">
                <strong>{{ activeCategoryGroups.length }}</strong>
                <span>主题方向</span>
              </div>
            </div>
          </div>
          <div class="hotspot-legend">
            <div
              v-for="slice in hotspotSlices"
              :key="slice.name"
              class="hotspot-legend-item"
            >
              <span class="hotspot-dot" :style="{ backgroundColor: slice.color }" />
              <div class="hotspot-text">
                <div class="hotspot-name">{{ slice.name }}</div>
                <div class="hotspot-meta">{{ slice.count }} 条 · {{ slice.percent }}%</div>
              </div>
            </div>
            <el-empty v-if="!hotspotSlices.length" description="暂无热点方向数据" />
          </div>
        </div>
      </el-card>

      <el-card>
        <template #header><span class="font-semibold">大数据主题解读</span></template>
        <div class="insight-grid">
          <div class="insight-card">
            <div class="insight-label">主题聚类</div>
            <div class="insight-value">{{ activeCategoryGroups[0]?.name || '未形成' }}</div>
            <p class="insight-desc">当前标签分类中占比最高的采集主题，可作为热点方向的主类目展示。</p>
          </div>
          <div class="insight-card">
            <div class="insight-label">热点覆盖率</div>
            <div class="insight-value">{{ stats.total ? Math.round((stats.taggedResults / stats.total) * 100) : 0 }}%</div>
            <p class="insight-desc">已打标签结果占收藏结果的比例，可包装为“已完成语义归类的数据覆盖率”。</p>
          </div>
          <div class="insight-card">
            <div class="insight-label">分类离散度</div>
            <div class="insight-value">{{ stats.categories }}</div>
            <p class="insight-desc">当前采集结果被划分出的主题类别数量，可用来体现数据分布的广度。</p>
          </div>
          <div class="insight-card">
            <div class="insight-label">标签活跃度</div>
            <div class="insight-value">{{ topTags[0]?.bindingCount || 0 }}</div>
            <p class="insight-desc">最活跃标签的使用次数，可以解释为当前热点主题下的高频内容聚集度。</p>
          </div>
        </div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="result-toolbar">
          <span class="font-semibold">结果中心</span>
          <div class="result-toolbar-actions">
            <el-input v-model="categoryFilter" size="small" placeholder="按标签/分类筛选" class="toolbar-search" />
            <el-tag type="info" size="small">已选 {{ selectedCount }}</el-tag>
            <el-select v-model="selectedTagId" size="small" placeholder="批量选标签" class="toolbar-select">
              <el-option v-for="tag in resultTags" :key="tag.tagId" :label="tag.tagName" :value="tag.tagId" />
            </el-select>
            <el-button size="small" type="primary" plain :loading="bulkOperating" @click="bulkBindTag">批量打标签</el-button>
            <el-button size="small" type="danger" plain :loading="bulkOperating" @click="bulkUnbookmark">批量取消收藏</el-button>
            <el-button text type="primary" @click="loadBookmarks">刷新</el-button>
            <el-button type="success" :loading="exporting" @click="exportAll">导出 CSV</el-button>
            <el-button type="warning" :loading="exportingMhtml" @click="exportAllMhtml">导出 MHTML</el-button>
            <el-button type="primary" plain @click="tagDialogVisible = true">标签管理</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredBookmarks" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
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

<style scoped>
.result-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.result-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-search {
  width: 220px;
  max-width: 100%;
}

.toolbar-select {
  width: 128px;
  max-width: 100%;
}

.hotspot-panel {
  display: grid;
  grid-template-columns: minmax(220px, 280px) 1fr;
  gap: 24px;
  align-items: center;
}

.hotspot-chart-wrap {
  display: flex;
  justify-content: center;
}

.hotspot-chart {
  width: min(260px, 58vw);
  aspect-ratio: 1;
  border-radius: 50%;
  position: relative;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.2);
}

.hotspot-hole {
  position: absolute;
  inset: 18%;
  border-radius: 50%;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #0f172a;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.8);
}

.hotspot-hole strong {
  font-size: clamp(24px, 3vw, 36px);
  line-height: 1;
}

.hotspot-hole span {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hotspot-legend {
  display: grid;
  gap: 12px;
}

.hotspot-legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #f8fafc;
}

.hotspot-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.hotspot-text {
  min-width: 0;
}

.hotspot-name {
  font-weight: 600;
  color: #0f172a;
}

.hotspot-meta {
  font-size: 12px;
  color: #64748b;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.insight-card {
  min-height: 150px;
  padding: 18px;
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.12), transparent 34%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
}

.insight-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.insight-value {
  margin-top: 10px;
  font-size: clamp(24px, 2.8vw, 34px);
  font-weight: 700;
  color: #0f172a;
}

.insight-desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

@media (max-width: 1024px) {
  .hotspot-panel {
    grid-template-columns: 1fr;
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .result-toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .toolbar-search,
  .toolbar-select {
    width: 100%;
  }
}
</style>
