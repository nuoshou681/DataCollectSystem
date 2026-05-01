<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
    <h1 class="text-2xl font-bold text-white">系统配置</h1>

    <div class="text-gray-400 text-sm">修改配置后即时生效，无需重启服务。</div>

    <div v-if="loading" class="text-center text-gray-500 py-12">加载中...</div>

    <div v-else v-for="(items, category) in grouped" :key="category" class="bg-gray-800/60 border border-gray-700/50 rounded-xl overflow-hidden">
      <div class="px-4 py-3 border-b border-gray-700/30 bg-gray-800/40">
        <h3 class="text-sm font-medium text-gray-300">{{ categoryLabels[category] || category }}</h3>
      </div>
      <div class="divide-y divide-gray-700/20">
        <div v-for="cfg in items" :key="cfg.configKey" class="px-4 py-3 flex items-center gap-4">
          <div class="flex-1 min-w-0">
            <div class="text-white text-sm font-mono">{{ cfg.configKey }}</div>
            <div class="text-gray-500 text-xs mt-0.5">{{ cfg.description }}</div>
          </div>
          <div class="flex items-center gap-2">
            <input
              v-model="editing[cfg.configKey]"
              class="w-48 bg-gray-700 border border-gray-600 rounded-lg px-3 py-1.5 text-white text-sm focus:outline-none focus:border-blue-500"
              :class="{ 'border-amber-500': isDirty(cfg.configKey) }"
            >
            <button
              :disabled="!isDirty(cfg.configKey) || saving[cfg.configKey]"
              class="bg-blue-600 hover:bg-blue-500 disabled:opacity-40 disabled:cursor-not-allowed text-white px-3 py-1.5 rounded-lg text-xs whitespace-nowrap"
              @click="handleSave(cfg.configKey)"
            >
              {{ saving[cfg.configKey] ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && Object.keys(grouped).length === 0" class="text-center text-gray-500 py-12">
      暂无配置项
    </div>
  </div>
</template>
