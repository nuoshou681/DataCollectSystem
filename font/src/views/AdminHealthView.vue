<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchHealth } from '@/api/api'
import type { HealthInfo } from '@/types/entity'

const health = ref<HealthInfo | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    health.value = await fetchHealth()
  } catch {
    ElMessage.error('获取健康状态失败')
  } finally {
    loading.value = false
  }
}

function statusBadge(s: string) {
  return s === 'UP' ? 'text-emerald-400 bg-emerald-400/10' : 'text-red-400 bg-red-400/10'
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-white">系统健康监控</h1>
      <button class="text-sm text-blue-400 hover:text-blue-300" @click="load" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <div v-if="!health" class="text-center text-gray-500 py-16">加载中...</div>

    <template v-else>
      <div class="grid grid-cols-2 gap-4">
        <!-- MySQL -->
        <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
          <h3 class="text-sm font-medium text-gray-400 mb-3">MySQL 数据库</h3>
          <div class="flex items-center gap-2">
            <span :class="statusBadge(health.mysql.status)" class="px-3 py-1 rounded text-sm font-medium">
              {{ health.mysql.status === 'UP' ? '运行正常' : '连接异常' }}
            </span>
          </div>
          <div v-if="health.mysql.error" class="text-red-400 text-xs mt-2">{{ health.mysql.error }}</div>
        </div>

        <!-- RabbitMQ -->
        <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
          <h3 class="text-sm font-medium text-gray-400 mb-3">RabbitMQ 消息队列</h3>
          <div class="flex items-center gap-2">
            <span :class="statusBadge(health.rabbitmq.status)" class="px-3 py-1 rounded text-sm font-medium">
              {{ health.rabbitmq.status === 'UP' ? '运行正常' : '连接异常' }}
            </span>
          </div>
          <div v-if="health.rabbitmq.error" class="text-red-400 text-xs mt-2">{{ health.rabbitmq.error }}</div>
        </div>
      </div>

      <!-- JVM -->
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
        <h3 class="text-sm font-medium text-gray-400 mb-4">JVM 内存</h3>
        <div class="grid grid-cols-4 gap-4">
          <div>
            <div class="text-2xl font-bold text-white">{{ ((health.jvm.usedMemoryMB ?? 0) / 1024).toFixed(1) }} GB</div>
            <div class="text-gray-500 text-xs">已用内存</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-blue-400">{{ ((health.jvm.maxMemoryMB ?? 0) / 1024).toFixed(1) }} GB</div>
            <div class="text-gray-500 text-xs">最大内存</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-emerald-400">{{ ((health.jvm.freeMemoryMB ?? 0) / 1024).toFixed(1) }} GB</div>
            <div class="text-gray-500 text-xs">空闲内存</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-violet-400">{{ health.jvm.processors }}</div>
            <div class="text-gray-500 text-xs">CPU 核心数</div>
          </div>
        </div>
        <!-- Memory bar -->
        <div class="mt-4 bg-gray-700 rounded-full h-3 overflow-hidden">
          <div class="bg-gradient-to-r from-blue-500 to-violet-500 h-full rounded-full transition-all"
               :style="{ width: ((health.jvm.usedMemoryMB ?? 0) / (health.jvm.maxMemoryMB ?? 1) * 100).toFixed(1) + '%' }">
          </div>
        </div>
        <div class="text-gray-500 text-xs mt-1">
          使用率 {{ ((health.jvm.usedMemoryMB ?? 0) / (health.jvm.maxMemoryMB ?? 1) * 100).toFixed(1) }}%
        </div>
      </div>

      <!-- Disk -->
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-5">
        <h3 class="text-sm font-medium text-gray-400 mb-4">磁盘空间</h3>
        <div class="grid grid-cols-3 gap-4">
          <div>
            <div class="text-2xl font-bold text-white">{{ health.disk.totalGB }} GB</div>
            <div class="text-gray-500 text-xs">总容量</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-emerald-400">{{ health.disk.freeGB }} GB</div>
            <div class="text-gray-500 text-xs">可用空间</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-amber-400">{{ health.disk.usableGB }} GB</div>
            <div class="text-gray-500 text-xs">可写入</div>
          </div>
        </div>
        <div class="mt-4 bg-gray-700 rounded-full h-3 overflow-hidden">
          <div class="bg-gradient-to-r from-emerald-500 to-amber-500 h-full rounded-full"
               :style="{ width: ((1 - health.disk.freeGB / (health.disk.totalGB || 1)) * 100).toFixed(1) + '%' }">
          </div>
        </div>
        <div class="text-gray-500 text-xs mt-1">
          已用 {{ ((1 - health.disk.freeGB / (health.disk.totalGB || 1)) * 100).toFixed(1) }}%
        </div>
      </div>
    </template>
  </div>
</template>
