<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Cog6ToothIcon } from '@heroicons/vue/24/outline'
import { fetchSystemConfigs, updateSystemConfig } from '@/api/api'
import type { SystemConfig } from '@/types/entity'

const configs = ref<SystemConfig[]>([])
const loading = ref(false)
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
  loading.value = true
  try {
    configs.value = await fetchSystemConfigs()
    for (const c of configs.value) {
      editing.value[c.configKey] = c.configValue || ''
    }
  } finally {
    loading.value = false
  }
}

async function handleSave(key: string) {
  saving.value[key] = true
  try {
    await updateSystemConfig(key, editing.value[key])
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

onMounted(loadConfigs)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">系统配置</h2>
        <p class="text-sm text-slate-500 mt-1">修改配置后即时生效，无需重启服务</p>
      </div>
      <el-button text type="primary" :loading="loading" @click="loadConfigs">刷新</el-button>
    </div>

    <el-card v-loading="loading">
      <template #header>
        <div class="flex items-center gap-2">
          <Cog6ToothIcon class="w-5 h-5 text-slate-500" />
          <span class="font-semibold">配置项</span>
        </div>
      </template>

      <div v-if="!loading && Object.keys(grouped).length === 0" class="py-12">
        <el-empty description="暂无配置项" />
      </div>

      <div v-for="(items, category) in grouped" :key="category" class="mb-6 last:mb-0">
        <h3 class="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">{{ categoryLabels[category] || category }}</h3>
        <div class="space-y-3">
          <div v-for="cfg in items" :key="cfg.configKey" class="flex flex-wrap items-center gap-4 rounded-lg border border-slate-200 p-4">
            <div class="flex-1 min-w-0">
              <div class="font-mono text-sm font-medium text-slate-700">{{ cfg.configKey }}</div>
              <div class="text-xs text-slate-500 mt-0.5">{{ cfg.description }}</div>
            </div>
            <div class="flex items-center gap-2">
              <el-input
                v-model="editing[cfg.configKey]"
                size="small"
                style="width:220px"
                :class="{ 'is-dirty': isDirty(cfg.configKey) }"
              />
              <el-button
                size="small"
                type="primary"
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
  </div>
</template>
