<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchTaskSchedules, createTaskSchedule, updateTaskSchedule, deleteTaskSchedule, toggleTaskSchedule } from '@/api/api'
import type { TaskSchedule } from '@/types/entity'

const schedules = ref<TaskSchedule[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const form = ref<TaskSchedule>({ scheduleName: '', keyword: '', url: '', siteType: 'sohu', maxLinksPerLevel: 10, cronExpression: '60' })

const INTERVAL_PRESETS = [
  { label: '每10分钟', value: '10' },
  { label: '每30分钟', value: '30' },
  { label: '每1小时', value: '60' },
  { label: '每3小时', value: '180' },
  { label: '每6小时', value: '360' },
  { label: '每12小时', value: '720' },
  { label: '每24小时', value: '1440' },
]

async function load() { schedules.value = await fetchTaskSchedules() }

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
  if (dialogMode.value === 'create') {
    await createTaskSchedule(form.value)
    ElMessage.success('调度创建成功')
  } else {
    await updateTaskSchedule(form.value.scheduleId!, form.value)
    ElMessage.success('调度更新成功')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(id: number) {
  await deleteTaskSchedule(id)
  ElMessage.success('已删除')
  load()
}

async function handleToggle(s: TaskSchedule) {
  await toggleTaskSchedule(s.scheduleId!)
  ElMessage.success(s.enabled ? '已暂停' : '已启用')
  load()
}

function formatTime(v?: string | null) {
  if (!v) return '-'
  return v.replace('T', ' ').substring(0, 19)
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-gray-900">定时任务</h1>

    <div class="flex justify-between items-center">
      <p class="text-gray-500 text-sm">创建定时采集计划，系统将自动按间隔执行</p>
      <button class="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm" @click="openCreate">
        新建定时任务
      </button>
    </div>

    <div class="bg-white border rounded-xl overflow-hidden shadow-sm">
      <table class="w-full text-sm">
        <thead class="border-b bg-gray-50">
          <tr class="text-left text-gray-500 text-xs uppercase">
            <th class="px-4 py-3">名称</th>
            <th class="px-4 py-3">关键词</th>
            <th class="px-4 py-3">URL</th>
            <th class="px-4 py-3">站点</th>
            <th class="px-4 py-3">间隔(分)</th>
            <th class="px-4 py-3">状态</th>
            <th class="px-4 py-3">上次执行</th>
            <th class="px-4 py-3 text-right">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y">
          <tr v-for="s in schedules" :key="s.scheduleId" class="hover:bg-gray-50">
            <td class="px-4 py-3 font-medium">{{ s.scheduleName }}</td>
            <td class="px-4 py-3">{{ s.keyword }}</td>
            <td class="px-4 py-3 text-gray-500 max-w-48 truncate">{{ s.url || '-' }}</td>
            <td class="px-4 py-3">{{ s.siteType || '-' }}</td>
            <td class="px-4 py-3">{{ s.cronExpression }}</td>
            <td class="px-4 py-3">
              <span :class="s.enabled ? 'text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded' : 'text-gray-400 bg-gray-100 px-2 py-0.5 rounded'">
                {{ s.enabled ? '运行中' : '已暂停' }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500">{{ formatTime(s.lastRunAt) }}</td>
            <td class="px-4 py-3 text-right space-x-2">
              <button class="text-blue-600 hover:text-blue-500 text-xs" @click="openEdit(s)">编辑</button>
              <button :class="s.enabled ? 'text-amber-600' : 'text-emerald-600'" class="text-xs" @click="handleToggle(s)">
                {{ s.enabled ? '暂停' : '启用' }}
              </button>
              <button class="text-red-500 hover:text-red-400 text-xs" @click="handleDelete(s.scheduleId!)">删除</button>
            </td>
          </tr>
          <tr v-if="schedules.length === 0">
            <td colspan="8" class="px-4 py-8 text-center text-gray-400">暂无定时任务</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建定时任务' : '编辑定时任务'" width="480px" :close-on-click-modal="false">
      <div class="space-y-4">
        <div>
          <label class="text-gray-600 text-sm block mb-1">调度名称</label>
          <input v-model="form.scheduleName" class="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500" placeholder="如：每日采集">
        </div>
        <div>
          <label class="text-gray-600 text-sm block mb-1">关键词</label>
          <input v-model="form.keyword" class="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500" placeholder="搜索关键词">
        </div>
        <div>
          <label class="text-gray-600 text-sm block mb-1">采集 URL（可选）</label>
          <input v-model="form.url" class="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500" placeholder="https://...">
        </div>
        <div>
          <label class="text-gray-600 text-sm block mb-1">站点类型</label>
          <select v-model="form.siteType" class="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
            <option value="sohu">Sohu</option>
            <option value="bing">Bing</option>
            <option value="baidu_baike">BaiduBaike</option>
          </select>
        </div>
        <div>
          <label class="text-gray-600 text-sm block mb-1">执行间隔</label>
          <select v-model="form.cronExpression" class="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
            <option v-for="p in INTERVAL_PRESETS" :key="p.value" :value="p.value">{{ p.label }}</option>
          </select>
        </div>
      </div>
      <template #footer>
        <button class="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm mr-2" @click="dialogVisible = false">取消</button>
        <button class="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm" @click="handleSubmit">确定</button>
      </template>
    </el-dialog>
  </div>
</template>
