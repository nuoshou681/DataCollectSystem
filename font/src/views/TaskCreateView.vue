<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BookOpenIcon, GlobeAltIcon, MagnifyingGlassIcon, NewspaperIcon } from '@heroicons/vue/24/outline'
import { dispatchBatchTask, dispatchTask, fetchResultTags, importTasksCsv } from '@/api/api'
import type { DispatchTaskPayload, ResultTag } from '@/types/entity'
import { getCurrentUserProfile } from '@/utils/auth'

type SiteKey = 'sohu' | 'bing' | 'baike' | 'tencent_news' | 'sina_news' | 'thepaper' | 'huanqiu' | 'chinanews' | 'cctv_news' | 'guancha' | 'wikipedia'

interface SiteOption {
  key: SiteKey
  label: string
  seedUrl: string
  siteType: string
  category: string
  domain: string
  description: string
  method: 'search' | 'scrape'
}

const siteOptions: SiteOption[] = [
  { key: 'sohu', label: '搜狐新闻', seedUrl: 'https://search.sohu.com/?keyword=', siteType: 'SOHU', category: '新闻资讯', domain: 'search.sohu.com', description: '搜狐新闻搜索引擎，覆盖全网新闻资讯', method: 'scrape' },
  { key: 'bing', label: 'Bing 搜索', seedUrl: 'https://www.bing.com/search?q=', siteType: 'BING', category: '搜索引擎', domain: 'www.bing.com', description: '微软 Bing 全网搜索，结果多样覆盖广', method: 'search' },
  { key: 'baike', label: '百度百科', seedUrl: 'https://baike.baidu.com/item/', siteType: 'BAIDU_BAIKE', category: '百科知识', domain: 'baike.baidu.com', description: '全球最大中文百科全书，权威知识库', method: 'scrape' },
  { key: 'tencent_news', label: '腾讯新闻', seedUrl: 'https://news.qq.com', siteType: 'TENCENT_NEWS', category: '新闻资讯', domain: 'news.qq.com', description: '腾讯新闻门户，实时热点资讯平台', method: 'search' },
  { key: 'sina_news', label: '新浪新闻', seedUrl: 'https://news.sina.com.cn', siteType: 'SINA_NEWS', category: '新闻资讯', domain: 'news.sina.com.cn', description: '新浪新闻中心，全方位时事报道', method: 'search' },
  { key: 'thepaper', label: '澎湃新闻', seedUrl: 'https://www.thepaper.cn', siteType: 'THEPAPER', category: '新闻资讯', domain: 'www.thepaper.cn', description: '澎湃新闻，专注时政与深度报道', method: 'search' },
  { key: 'huanqiu', label: '环球网', seedUrl: 'https://www.huanqiu.com', siteType: 'HUANQIU', category: '新闻资讯', domain: 'www.huanqiu.com', description: '环球网，全球视角的国际资讯平台', method: 'search' },
  { key: 'chinanews', label: '中国新闻网', seedUrl: 'https://www.chinanews.com.cn', siteType: 'CHINANEWS', category: '新闻资讯', domain: 'www.chinanews.com.cn', description: '中国新闻网，权威国家新闻通讯社', method: 'search' },
  { key: 'cctv_news', label: '央视网新闻', seedUrl: 'https://news.cctv.com', siteType: 'CCTV_NEWS', category: '新闻资讯', domain: 'news.cctv.com', description: '央视网新闻频道，官方主流媒体', method: 'search' },
  { key: 'guancha', label: '观察者网', seedUrl: 'https://www.guancha.cn', siteType: 'GUANCHA', category: '新闻资讯', domain: 'www.guancha.cn', description: '观察者网，深度评论与时事分析', method: 'search' },
  { key: 'wikipedia', label: '维基百科', seedUrl: 'https://en.wikipedia.org/w/index.php?search=', siteType: 'WIKIPEDIA', category: '百科知识', domain: 'en.wikipedia.org', description: '英文维基百科，全球最大开放百科', method: 'scrape' },
]

const CATEGORY_ICON: Record<string, any> = {
  '搜索引擎': MagnifyingGlassIcon,
  '新闻资讯': NewspaperIcon,
  '百科知识': BookOpenIcon,
}
const CATEGORY_ICON_COLOR: Record<string, string> = {
  '搜索引擎': 'text-amber-600',
  '新闻资讯': 'text-blue-600',
  '百科知识': 'text-emerald-600',
}

const router = useRouter()
const selectedSites = ref<SiteOption['key'][]>(['sohu'])
const siteKeywordFilter = ref('')
const commonKeyword = ref('')
const batchKeywordsText = ref('')
const batchName = ref('')
const batchNotes = ref('')
const batchMode = ref(false)
const selectedTaskTagIds = ref<number[]>([])
const submitting = ref(false)
const resultTags = ref<ResultTag[]>([])
const importingCsv = ref(false)
const csvInputRef = ref<HTMLInputElement | null>(null)

const siteOptionMap = new Map(siteOptions.map(option => [option.key, option]))

const siteCategories = computed(() => {
  const order = ['搜索引擎', '新闻资讯', '百科知识']
  const groups = new Map<string, SiteOption[]>()
  const filtered = siteKeywordFilter.value
    ? siteOptions.filter(s => s.label.includes(siteKeywordFilter.value) || s.domain.includes(siteKeywordFilter.value) || s.description.includes(siteKeywordFilter.value))
    : siteOptions
  for (const s of filtered) {
    const list = groups.get(s.category) || []
    list.push(s)
    groups.set(s.category, list)
  }
  return order.filter(cat => groups.has(cat)).map(cat => ({ name: cat, sites: groups.get(cat)! }))
})

function toggleSite(key: SiteKey) {
  const idx = selectedSites.value.indexOf(key)
  if (idx >= 0) {
    selectedSites.value.splice(idx, 1)
  } else {
    selectedSites.value.push(key)
  }
}

async function loadTags() {
  try { resultTags.value = await fetchResultTags() } catch { /* noop */ }
}

function createPayload(option: SiteOption, keyword: string): DispatchTaskPayload {
  const profile = getCurrentUserProfile()
  return {
    userId: profile.userId ?? null,
    keyword,
    url: option.seedUrl,
    source: 'manual',
    siteType: option.siteType,
    maxLinksPerLevel: 10,
    tagIds: selectedTaskTagIds.value,
  }
}

async function submitTask() {
  if (selectedSites.value.length === 0) { ElMessage.error('至少选择一个站点'); return }
  if (batchMode.value) {
    if (selectedSites.value.length !== 1) { ElMessage.error('批量创建任务时请只选择一个站点'); return }
    if (!batchKeywordsText.value.trim()) { ElMessage.error('请填写批量关键词'); return }
  } else if (!commonKeyword.value.trim()) {
    ElMessage.error('请填写关键词'); return
  }
  submitting.value = true
  try {
    if (batchMode.value) {
      const firstSelected = selectedSites.value[0]
      if (!firstSelected) throw new Error('请选择站点')
      const selectedSite = siteOptionMap.get(firstSelected)
      if (!selectedSite) throw new Error('站点配置不存在')
      await dispatchBatchTask({
        userId: getCurrentUserProfile().userId ?? null,
        url: selectedSite.seedUrl,
        siteType: selectedSite.siteType,
        source: 'manual',
        maxLinksPerLevel: 10,
        keywordsText: batchKeywordsText.value,
        batchName: batchName.value.trim(),
        batchNotes: batchNotes.value.trim(),
        keyword: '',
        tagIds: selectedTaskTagIds.value,
      })
      ElMessage.success('批量任务创建成功')
    } else {
      const requests = selectedSites.value.map(site => {
        const option = siteOptionMap.get(site)
        if (!option) return Promise.resolve(null)
        return dispatchTask(createPayload(option, commonKeyword.value.trim()))
      })
      await Promise.all(requests)
      ElMessage.success(`已创建 ${requests.length} 个采集任务`)
    }
    router.push('/tasks')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务创建失败')
  } finally {
    submitting.value = false
  }
}

function triggerCsvImport() { csvInputRef.value?.click() }

async function handleCsvFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importingCsv.value = true
  try {
    const result = await importTasksCsv(file, 10)
    if (result) ElMessage.success(`CSV导入完成: 成功 ${result.success ?? 0} 条，失败 ${result.failed ?? 0} 条`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : 'CSV导入失败')
  } finally {
    importingCsv.value = false
    input.value = ''
  }
}

loadTags()
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">创建采集任务</h2>
        <p class="text-sm text-slate-500 mt-1">选择站点并配置关键词，系统将调度 CrawlerNode 执行采集</p>
      </div>
      <div class="flex items-center gap-3">
        <input ref="csvInputRef" type="file" accept=".csv" style="display:none" @change="handleCsvFileChange" />
        <el-button plain :loading="importingCsv" @click="triggerCsvImport">
          {{ importingCsv ? '导入中...' : '批量导入CSV' }}
        </el-button>
        <el-button @click="router.push('/tasks')">返回任务列表</el-button>
      </div>
    </div>

    <el-card>
      <template #header><span class="font-semibold">选择采集站点</span></template>
      <div class="space-y-4">
        <div class="flex items-center gap-3">
          <el-input v-model="siteKeywordFilter" size="small" placeholder="搜索站点..." clearable style="width: 260px" />
          <span class="text-xs text-slate-400">已选 <b class="text-blue-600">{{ selectedSites.length }}</b> 个站点</span>
          <el-button v-if="selectedSites.length" text size="small" type="danger" @click="selectedSites = []">清空选择</el-button>
        </div>
        <div v-for="cat in siteCategories" :key="cat.name" class="space-y-2">
          <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">{{ cat.name }}</div>
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            <div
              v-for="site in cat.sites"
              :key="site.key"
              class="relative rounded-xl border-2 p-4 cursor-pointer transition-all duration-200 select-none"
              :class="selectedSites.includes(site.key) ? 'border-blue-500 bg-blue-50 shadow-md shadow-blue-100' : 'border-slate-200 hover:border-slate-300 hover:shadow-sm bg-white'"
              @click="toggleSite(site.key)"
            >
              <div v-if="selectedSites.includes(site.key)" class="absolute top-2 right-2 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center">
                <span class="text-white text-xs leading-none">&#10003;</span>
              </div>
              <div class="flex items-center gap-3 mb-2">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0" :class="selectedSites.includes(site.key) ? 'bg-blue-100' : 'bg-slate-100'">
                  <component :is="CATEGORY_ICON[site.category]" class="w-5 h-5" :class="CATEGORY_ICON_COLOR[site.category]" />
                </div>
                <div class="min-w-0">
                  <div class="font-semibold text-sm truncate">{{ site.label }}</div>
                  <el-tag size="small" :type="site.method === 'search' ? 'success' : 'warning'" class="mt-0.5" style="font-size:10px;height:18px;line-height:18px;padding:0 4px">
                    {{ site.method === 'search' ? 'Firecrawl /search' : '页面提取' }}
                  </el-tag>
                </div>
              </div>
              <div class="text-xs text-slate-500 leading-relaxed line-clamp-2">{{ site.description }}</div>
              <div class="text-xs text-slate-400 mt-2 truncate font-mono">{{ site.domain }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header><span class="font-semibold">配置关键词</span></template>
      <el-form label-width="120px" class="max-w-2xl">
        <el-form-item label="采集模式">
          <div class="flex items-center gap-4">
            <el-switch v-model="batchMode" active-text="批量关键词" inactive-text="单关键词" />
            <span class="text-xs text-slate-400">{{ batchMode ? '输入多个关键词，系统为每个关键词创建独立任务' : '所有选中站点使用同一个关键词' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="任务标签">
          <el-select v-model="selectedTaskTagIds" multiple collapse-tags collapse-tags-tooltip clearable placeholder="给本次任务预设标签" style="width: 100%">
            <el-option v-for="tag in resultTags" :key="tag.tagId" :label="tag.categoryName ? `${tag.categoryName} / ${tag.tagName}` : tag.tagName" :value="tag.tagId!" />
          </el-select>
        </el-form-item>
        <template v-if="batchMode">
          <el-form-item label="批次名称"><el-input v-model="batchName" placeholder="例如：4月热点人物批次" /></el-form-item>
          <el-form-item label="批次备注"><el-input v-model="batchNotes" placeholder="可选" /></el-form-item>
          <el-form-item label="批量关键词">
            <el-input v-model="batchKeywordsText" type="textarea" :rows="6" placeholder="每行一个关键词，或用逗号分隔" />
          </el-form-item>
        </template>
        <el-form-item v-else label="关键词">
          <el-input v-model="commonKeyword" maxlength="80" show-word-limit placeholder="输入统一关键词，如：人工智能" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="submitTask">
            {{ batchMode ? '批量创建任务' : `创建任务 (${selectedSites.length} 个站点)` }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
