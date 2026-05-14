<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  DocumentTextIcon,
  ServerStackIcon,
  ClockIcon,
} from '@heroicons/vue/24/outline'
import { fetchTaskLogs } from '@/api/api'
import type { TaskLog } from '@/types/entity'
import { useRoute } from 'vue-router'

const route = useRoute()

const logs = ref<TaskLog[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const level = ref('ALL')
const keyword = ref('')

async function loadLogs() {
  loading.value = true
  try {
    const result = await fetchTaskLogs(page.value, size.value, level.value, keyword.value || undefined)
    logs.value = result.records
    total.value = result.total
  } catch {
    ElMessage.error('日志加载失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  loadLogs()
}

function handleSizeChange(s: number) {
  size.value = s
  page.value = 1
  loadLogs()
}

let keywordTimer: ReturnType<typeof setTimeout> | null = null
function handleKeywordChange() {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    page.value = 1
    loadLogs()
  }, 400)
}

watch(level, () => {
  page.value = 1
  loadLogs()
})

// Current page level breakdown
const levelBreakdown = computed(() => {
  const current = { info: 0, warn: 0, error: 0 }
  for (const l of logs.value) {
    const key = l.logLevel?.toLowerCase() ?? ''
    if (key === 'info') current.info++
    else if (key === 'warn' || key === 'warning') current.warn++
    else if (key === 'error') current.error++
  }
  return current
})

const levelFilters = computed(() => [
  { label: 'INFO', count: levelBreakdown.value.info, icon: InformationCircleIcon, color: 'border-l-blue-500', bg: 'bg-blue-50', textColor: 'text-blue-600', active: level.value === 'INFO' },
  { label: 'WARN', count: levelBreakdown.value.warn, icon: ExclamationTriangleIcon, color: 'border-l-amber-500', bg: 'bg-amber-50', textColor: 'text-amber-600', active: level.value === 'WARN' },
  { label: 'ERROR', count: levelBreakdown.value.error, icon: ExclamationCircleIcon, color: 'border-l-red-500', bg: 'bg-red-50', textColor: 'text-red-600', active: level.value === 'ERROR' },
])

function handleLevelFilterClick(l: string) {
  level.value = level.value === l ? 'ALL' : l
}

function rowClassName({ row }: { row: TaskLog }) {
  const lvl = row.logLevel?.toUpperCase()
  if (lvl === 'ERROR') return 'log-row-error'
  if (lvl === 'WARN') return 'log-row-warn'
  return ''
}

function formatTime(raw?: string) {
  if (!raw) return '-'
  const d = new Date(raw)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// Global total (shown in header)
onMounted(() => {
  if (route.query.taskId) {
    keyword.value = String(route.query.taskId)
  }
  loadLogs()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">审计日志</h2>
        <p class="text-sm text-slate-500 mt-1">全系统任务日志、节点消息与运行事件</p>
      </div>
      <div class="flex items-center gap-3 text-xs text-slate-400">
        <el-button size="small" text type="primary" @click="loadLogs" :loading="loading">刷新</el-button>
      </div>
    </div>

    <!-- Stats row -->
    <section class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <el-card shadow="hover" class="border-l-4 border-l-slate-500 cursor-pointer transition-all"
        :class="level === 'ALL' ? 'bg-slate-50 ring-1 ring-slate-300' : ''"
        @click="level = 'ALL'"
      >
        <div class="flex items-center gap-3">
          <DocumentTextIcon class="w-5 h-5 text-slate-500" />
          <div>
            <div class="text-xs text-slate-400">全部日志</div>
            <div class="text-xl font-bold text-slate-700">{{ total }}</div>
          </div>
        </div>
        <div v-if="level === 'ALL'" class="mt-1 text-xs text-slate-500">当前筛选</div>
      </el-card>

      <el-card
        v-for="f in levelFilters" :key="f.label"
        shadow="hover"
        class="border-l-4 cursor-pointer transition-all"
        :class="[f.color, f.active ? f.bg + ' ring-1 ring-slate-300' : '']"
        @click="handleLevelFilterClick(f.label)"
      >
        <div class="flex items-center gap-3">
          <component :is="f.icon" class="w-5 h-5" :class="f.textColor" />
          <div>
            <div class="text-xs text-slate-400">{{ f.label }}</div>
            <div class="text-xl font-bold" :class="f.textColor">{{ f.count }}</div>
          </div>
          <el-tag v-if="f.active" size="small" type="warning" class="ml-auto">筛选中</el-tag>
        </div>
      </el-card>
    </section>

    <!-- Main table card -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between flex-wrap gap-3">
          <div class="flex items-center gap-2">
            <span class="font-semibold">日志记录</span>
            <el-tag size="small" type="info">{{ total }} 条</el-tag>
          </div>
          <div class="flex items-center gap-3">
            <el-select v-model="level" size="small" style="width:100px">
              <el-option label="全部" value="ALL" />
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
            </el-select>
            <el-input
              v-model="keyword"
              size="small"
              placeholder="搜索消息 / 节点 / 任务ID"
              style="width:240px"
              clearable
              @input="handleKeywordChange"
              @clear="handleKeywordChange"
            >
              <template #prefix>
                <svg class="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </template>
            </el-input>
          </div>
        </div>
      </template>

      <!-- Level filter chips -->
      <div class="flex items-center gap-2 mb-3">
        <span class="text-xs text-slate-400 mr-1">快速筛选：</span>
        <el-tag
          :type="level === 'ALL' ? '' : 'info'"
          size="small"
          class="cursor-pointer"
          :effect="level === 'ALL' ? 'dark' : 'plain'"
          @click="level = 'ALL'"
        >全部</el-tag>
        <el-tag
          :type="level === 'INFO' ? 'info' : 'info'"
          size="small"
          class="cursor-pointer"
          :effect="level === 'INFO' ? 'dark' : 'plain'"
          @click="handleLevelFilterClick('INFO')"
        >INFO</el-tag>
        <el-tag
          :type="level === 'WARN' ? 'warning' : 'warning'"
          size="small"
          class="cursor-pointer"
          :effect="level === 'WARN' ? 'dark' : 'plain'"
          @click="handleLevelFilterClick('WARN')"
        >WARN</el-tag>
        <el-tag
          :type="level === 'ERROR' ? 'danger' : 'danger'"
          size="small"
          class="cursor-pointer"
          :effect="level === 'ERROR' ? 'dark' : 'plain'"
          @click="handleLevelFilterClick('ERROR')"
        >ERROR</el-tag>
      </div>

      <el-table
        :data="logs" border stripe v-loading="loading"
        :row-class-name="rowClassName"
        highlight-current-row
      >
        <el-table-column prop="logId" label="日志ID" width="90" align="center" />
        <el-table-column label="时间" min-width="155" sortable prop="createdAt">
          <template #default="scope">
            <div class="flex items-center gap-1.5">
              <ClockIcon class="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
              <span class="text-sm font-mono text-slate-600">{{ formatTime(scope.row.createdAt) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="80" align="center">
          <template #default="scope">
            <el-tag
              :type="scope.row.logLevel === 'ERROR' ? 'danger' : scope.row.logLevel === 'WARN' ? 'warning' : 'info'"
              size="small"
              effect="dark"
            >
              {{ scope.row.logLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskId" label="任务ID" width="90" align="center" />
        <el-table-column label="来源节点" width="150">
          <template #default="scope">
            <div class="flex items-center gap-1.5" v-if="scope.row.nodeKey || scope.row.nodeId">
              <ServerStackIcon class="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
              <span class="text-sm font-mono text-slate-600">{{ scope.row.nodeKey || scope.row.nodeId }}</span>
            </div>
            <span v-else class="text-xs text-slate-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="日志内容" min-width="400">
          <template #default="scope">
            <div class="text-sm" :class="{
              'text-red-700 font-medium': scope.row.logLevel === 'ERROR',
              'text-amber-700': scope.row.logLevel === 'WARN',
              'text-slate-700': scope.row.logLevel === 'INFO',
            }">
              {{ scope.row.logMessage }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope">
            <el-button
              size="small"
              text
              type="primary"
              @click="keyword = String(scope.row.taskId); handleKeywordChange()"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无日志记录" :image-size="80">
            <template #description>
              <div class="text-slate-400">暂无日志记录</div>
              <div class="text-xs text-slate-400 mt-1">尝试调整筛选条件或搜索关键字</div>
            </template>
          </el-empty>
        </template>
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
:deep(.log-row-error) {
  background-color: #fef2f2 !important;
}
:deep(.log-row-error:hover) {
  background-color: #fee2e2 !important;
}
:deep(.log-row-warn) {
  background-color: #fffbeb !important;
}
:deep(.log-row-warn:hover) {
  background-color: #fef3c7 !important;
}
</style>
