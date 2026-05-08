<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchTaskSchedules, createTaskSchedule, updateTaskSchedule, deleteTaskSchedule, toggleTaskSchedule } from '@/api/api'
import type { TaskSchedule } from '@/types/entity'

const schedules = ref<TaskSchedule[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const form = ref<TaskSchedule>({ scheduleName: '', keyword: '', url: '', siteType: 'sohu', maxLinksPerLevel: 10, cronExpression: '60' })
const loading = ref(false)

const SITE_OPTIONS = [
  { value: 'sohu', label: '搜狐新闻', seedUrl: 'https://search.sohu.com/?keyword=' },
  { value: 'bing', label: 'Bing 搜索', seedUrl: 'https://www.bing.com/search?q=' },
  { value: 'baidu_baike', label: '百度百科', seedUrl: 'https://baike.baidu.com/item/' },
  { value: 'tencent_news', label: '腾讯新闻', seedUrl: 'https://news.qq.com' },
  { value: 'sina_news', label: '新浪新闻', seedUrl: 'https://news.sina.com.cn' },
  { value: 'thepaper', label: '澎湃新闻', seedUrl: 'https://www.thepaper.cn' },
  { value: 'huanqiu', label: '环球网', seedUrl: 'https://www.huanqiu.com' },
  { value: 'chinanews', label: '中国新闻网', seedUrl: 'https://www.chinanews.com.cn' },
  { value: 'cctv_news', label: '央视网新闻', seedUrl: 'https://news.cctv.com' },
  { value: 'guancha', label: '观察者网', seedUrl: 'https://www.guancha.cn' },
  { value: 'wikipedia', label: '维基百科(EN)', seedUrl: 'https://en.wikipedia.org/w/index.php?search=' },
]

const INTERVAL_PRESETS = [
  { label: '每10分钟', value: '10' },
  { label: '每30分钟', value: '30' },
  { label: '每1小时', value: '60' },
  { label: '每3小时', value: '180' },
  { label: '每6小时', value: '360' },
  { label: '每12小时', value: '720' },
  { label: '每24小时', value: '1440' },
]

function intervalLabel(value: string) {
  return INTERVAL_PRESETS.find(p => p.value === value)?.label ?? `${value}分钟`
}

async function load() {
  loading.value = true
  try {
    schedules.value = await fetchTaskSchedules()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  form.value = { scheduleName: '', keyword: '', url: '', siteType: 'sohu', maxLinksPerLevel: 10, cronExpression: '60' }
  dialogVisible.value = true
}

function openEdit(s: TaskSchedule) {
  dialogMode.value = 'edit'
  form.value = { ...s }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.scheduleName.trim()) {
    ElMessage.error('调度名称不能为空')
    return
  }
  if (!form.value.keyword.trim()) {
    ElMessage.error('关键词不能为空')
    return
  }
  try {
    if (dialogMode.value === 'create') {
      await createTaskSchedule(form.value)
      ElMessage.success('调度创建成功')
    } else {
      await updateTaskSchedule(form.value.scheduleId!, form.value)
      ElMessage.success('调度更新成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该定时任务吗？', '删除确认', { type: 'warning' })
    await deleteTaskSchedule(id)
    ElMessage.success('已删除')
    load()
  } catch { /* cancelled */ }
}

async function handleToggle(s: TaskSchedule) {
  try {
    await toggleTaskSchedule(s.scheduleId!)
    ElMessage.success(s.enabled ? '已暂停' : '已启用')
    load()
  } catch {
    ElMessage.error('操作失败')
  }
}

function formatTime(v?: string | null) {
  if (!v) return '-'
  return v.replace('T', ' ').substring(0, 19)
}

onMounted(load)
</script>

<template>
  <div class="p-6 space-y-6">
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">定时任务</span>
          <div class="flex items-center gap-3">
            <el-button type="primary" @click="openCreate">新建定时任务</el-button>
            <el-button text type="primary" :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>
      </template>
      <p class="text-sm text-gray-500 mb-4">创建定时采集计划，系统每分钟检查并在到期时自动执行</p>
      <el-table :data="schedules" border stripe>
        <el-table-column prop="scheduleName" label="名称" min-width="140" />
        <el-table-column prop="keyword" label="关键词" width="120" />
        <el-table-column label="URL" min-width="200">
          <template #default="scope">
            <span class="text-gray-500 text-xs">{{ scope.row.url || '(自动推导)' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="siteType" label="站点" width="100" />
        <el-table-column label="执行间隔" width="120">
          <template #default="scope">{{ intervalLabel(scope.row.cronExpression) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="small">
              {{ scope.row.enabled ? '运行中' : '已暂停' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上次执行" width="170">
          <template #default="scope">
            <span class="text-sm text-gray-500">{{ formatTime(scope.row.lastRunAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="下次执行" width="170">
          <template #default="scope">
            <span class="text-sm text-gray-500">{{ formatTime(scope.row.nextRunAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" text type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" text :type="scope.row.enabled ? 'warning' : 'success'" @click="handleToggle(scope.row)">
              {{ scope.row.enabled ? '暂停' : '启用' }}
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(scope.row.scheduleId!)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建定时任务' : '编辑定时任务'" width="520px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="调度名称" required>
          <el-input v-model="form.scheduleName" maxlength="40" placeholder="如：每日搜狐新闻采集" />
        </el-form-item>
        <el-form-item label="关键词" required>
          <el-input v-model="form.keyword" maxlength="80" placeholder="搜索关键词" />
        </el-form-item>
        <el-form-item label="采集站点">
          <el-select v-model="form.siteType" style="width: 100%">
            <el-option v-for="s in SITE_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="自定义URL">
          <el-input v-model="form.url" placeholder="留空则自动使用站点默认地址" />
        </el-form-item>
        <el-form-item label="执行间隔">
          <el-select v-model="form.cronExpression" style="width: 100%">
            <el-option v-for="p in INTERVAL_PRESETS" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="爬取深度">
          <el-input-number v-model="form.maxLinksPerLevel" :min="1" :max="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
