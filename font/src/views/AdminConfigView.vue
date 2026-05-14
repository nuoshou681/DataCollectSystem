<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArchiveBoxIcon, BellIcon, BoltIcon, Cog6ToothIcon, DocumentIcon, DocumentTextIcon, QueueListIcon, TrashIcon } from '@heroicons/vue/24/outline'
import { fetchCleanupStats, fetchSystemConfigs, runCleanup, updateSystemConfig } from '@/api/api'
import type { CleanupStats, SystemConfig } from '@/types/entity'

const activeTab = ref('config')

// ── Config tab ──

const configs = ref<SystemConfig[]>([])
const configLoading = ref(false)
const editing = ref<Record<string, string>>({})
const saving = ref<Record<string, boolean>>({})

const grouped = computed(() => {
  const map: Record<string, SystemConfig[]> = {}
  for (const c of configs.value) {
    const cat = c.category || 'GENERAL'
    if (!map[cat]) map[cat] = []
    map[cat].push(c)
  }
  return map
})

const categoryLabels: Record<string, string> = {
  '任务配置': '任务配置',
  '系统参数': '系统参数',
  'GENERAL': '通用设置',
}

async function loadConfigs() {
  configLoading.value = true
  try {
    configs.value = await fetchSystemConfigs()
    for (const c of configs.value) {
      editing.value[c.configKey] = c.configValue || ''
    }
  } finally {
    configLoading.value = false
  }
}

async function handleSave(key: string) {
  saving.value[key] = true
  try {
    await updateSystemConfig(key, editing.value[key] ?? '')
    ElMessage.success('配置已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value[key] = false
  }
}

function isDirty(key: string) {
  const original = configs.value.find(c => c.configKey === key)
  return editing.value[key] !== (original?.configValue || '')
}

// ── Cleanup tab ──

const cleanupStats = ref<CleanupStats | null>(null)
const cleanupLoading = ref(false)
const days = ref(30)
const cleaning = ref(false)

async function loadCleanup() {
  cleanupLoading.value = true
  try {
    cleanupStats.value = await fetchCleanupStats()
  } catch {
    ElMessage.error('获取统计失败')
  } finally {
    cleanupLoading.value = false
  }
}

async function handleCleanup() {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${days.value} 天前的归档任务、日志、事件和通知吗？此操作不可恢复！`,
      '数据清理确认',
      { type: 'warning', confirmButtonText: '确认清理', cancelButtonText: '取消' },
    )
    cleaning.value = true
    const result = await runCleanup(days.value)
    if (result) {
      ElMessage.success(
        `清理完成: 归档任务${result.deletedArchivedTasks ?? 0}条, 日志${result.deletedLogs ?? 0}条, 事件${result.deletedEvents ?? 0}条, 通知${result.deletedNotifications ?? 0}条`,
      )
    }
    await loadCleanup()
  } catch {
    // cancelled
  } finally {
    cleaning.value = false
  }
}

onMounted(() => { loadConfigs(); loadCleanup() })
</script>

<template>
  <div class="space-y-6">
    <div>
      <h2 class="text-xl font-semibold text-slate-800">系统维护</h2>
      <p class="text-sm text-slate-500 mt-1">配置管理、数据清理与系统运维</p>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Tab 1: Config -->
      <el-tab-pane label="系统配置" name="config">
        <el-card v-loading="configLoading">
          <template #header>
            <div class="flex items-center gap-2">
              <Cog6ToothIcon class="w-5 h-5 text-slate-500" />
              <span class="font-semibold">配置项</span>
              <el-button size="small" text type="primary" class="ml-auto" @click="loadConfigs">刷新</el-button>
            </div>
          </template>

          <el-empty v-if="!configLoading && Object.keys(grouped).length === 0" description="暂无配置项" />

          <div v-for="(items, category) in grouped" :key="category" class="mb-6 last:mb-0">
            <h3 class="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">
              {{ categoryLabels[category] || category }}
            </h3>
            <div class="space-y-3">
              <div
                v-for="cfg in items" :key="cfg.configKey"
                class="flex flex-wrap items-center gap-4 rounded-lg border border-slate-200 p-4"
              >
                <div class="flex-1 min-w-0">
                  <div class="font-mono text-sm font-medium text-slate-700">{{ cfg.configKey }}</div>
                  <div class="text-xs text-slate-500 mt-0.5">{{ cfg.description }}</div>
                </div>
                <div class="flex items-center gap-2">
                  <el-input
                    v-model="editing[cfg.configKey]"
                    size="small" style="width:220px"
                  />
                  <el-button
                    size="small" type="primary"
                    :disabled="!isDirty(cfg.configKey)"
                    :loading="saving[cfg.configKey]"
                    @click="handleSave(cfg.configKey)"
                  >
                    {{ saving[cfg.configKey] ? '保存中...' : '保存' }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: Cleanup -->
      <el-tab-pane label="数据清理" name="cleanup">
        <!-- Stats mini cards -->
        <section class="grid grid-cols-3 md:grid-cols-6 gap-3 mb-6">
          <el-card shadow="hover" class="border-l-4 border-l-blue-500" v-if="cleanupStats">
            <div class="flex items-center gap-2">
              <QueueListIcon class="w-4 h-4 text-blue-500" />
              <div>
                <div class="text-xs text-slate-500">总任务</div>
                <div class="text-lg font-bold text-blue-600">{{ cleanupStats.totalTasks }}</div>
              </div>
            </div>
          </el-card>
          <el-card shadow="hover" class="border-l-4 border-l-violet-500" v-if="cleanupStats">
            <div class="flex items-center gap-2">
              <DocumentTextIcon class="w-4 h-4 text-violet-500" />
              <div>
                <div class="text-xs text-slate-500">页面结果</div>
                <div class="text-lg font-bold text-violet-600">{{ cleanupStats.totalPageResults }}</div>
              </div>
            </div>
          </el-card>
          <el-card shadow="hover" class="border-l-4 border-l-amber-500" v-if="cleanupStats">
            <div class="flex items-center gap-2">
              <DocumentIcon class="w-4 h-4 text-amber-500" />
              <div>
                <div class="text-xs text-slate-500">日志</div>
                <div class="text-lg font-bold text-amber-600">{{ cleanupStats.totalLogs }}</div>
              </div>
            </div>
          </el-card>
          <el-card shadow="hover" class="border-l-4 border-l-emerald-500" v-if="cleanupStats">
            <div class="flex items-center gap-2">
              <BoltIcon class="w-4 h-4 text-emerald-500" />
              <div>
                <div class="text-xs text-slate-500">事件</div>
                <div class="text-lg font-bold text-emerald-600">{{ cleanupStats.totalEvents }}</div>
              </div>
            </div>
          </el-card>
          <el-card shadow="hover" class="border-l-4 border-l-pink-400" v-if="cleanupStats">
            <div class="flex items-center gap-2">
              <BellIcon class="w-4 h-4 text-pink-400" />
              <div>
                <div class="text-xs text-slate-500">通知</div>
                <div class="text-lg font-bold" style="color:#f472b6">{{ cleanupStats.totalNotifications }}</div>
              </div>
            </div>
          </el-card>
          <el-card shadow="hover" class="border-l-4 border-l-red-500" v-if="cleanupStats">
            <div class="flex items-center gap-2">
              <ArchiveBoxIcon class="w-4 h-4 text-red-500" />
              <div>
                <div class="text-xs text-slate-500">已归档</div>
                <div class="text-lg font-bold text-red-600">{{ cleanupStats.archivedTasks }}</div>
              </div>
            </div>
          </el-card>
        </section>

        <el-card>
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-semibold">清理设置</span>
              <el-button size="small" text type="primary" @click="loadCleanup">刷新</el-button>
            </div>
          </template>
          <div class="flex flex-wrap items-end gap-4">
            <div>
              <div class="text-sm text-slate-500 mb-2">清理 {{ days }} 天前的数据</div>
              <el-slider v-model="days" :min="1" :max="365" show-input style="width:320px" />
            </div>
            <el-button type="danger" :loading="cleaning" @click="handleCleanup">
              <TrashIcon class="w-4 h-4 mr-1" />
              {{ cleaning ? '清理中...' : '执行清理' }}
            </el-button>
          </div>
          <p class="text-xs text-slate-400 mt-3">
            将删除 {{ days }} 天前的已归档任务、系统日志、任务事件和旧通知。活跃任务不受影响。
          </p>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
