<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleStackIcon, CloudIcon, CpuChipIcon, ServerIcon } from '@heroicons/vue/24/outline'
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

function statusTag(s: string) {
  return s === 'UP' ? 'success' as const : 'danger' as const
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">系统健康监控</h2>
        <p class="text-sm text-slate-500 mt-1">MySQL、RabbitMQ、JVM 内存与磁盘空间</p>
      </div>
      <el-button text type="primary" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-if="loading && !health" class="flex items-center justify-center py-16">
      <el-icon class="is-loading" :size="24" />
      <span class="text-slate-400 ml-2">加载中...</span>
    </div>

    <template v-if="health">
      <section class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center gap-2">
              <CircleStackIcon class="w-5 h-5 text-blue-500" />
              <span class="font-semibold">MySQL 数据库</span>
            </div>
          </template>
          <div class="flex items-center gap-3">
            <el-tag :type="statusTag(health.mysql.status)">
              {{ health.mysql.status === 'UP' ? '运行正常' : '连接异常' }}
            </el-tag>
          </div>
          <div v-if="health.mysql.error" class="text-sm text-red-500 mt-2 bg-red-50 rounded-lg p-3">{{ health.mysql.error }}</div>
        </el-card>

        <el-card shadow="hover">
          <template #header>
            <div class="flex items-center gap-2">
              <CloudIcon class="w-5 h-5 text-violet-500" />
              <span class="font-semibold">RabbitMQ 消息队列</span>
            </div>
          </template>
          <div class="flex items-center gap-3">
            <el-tag :type="statusTag(health.rabbitmq.status)">
              {{ health.rabbitmq.status === 'UP' ? '运行正常' : '连接异常' }}
            </el-tag>
          </div>
          <div v-if="health.rabbitmq.error" class="text-sm text-red-500 mt-2 bg-red-50 rounded-lg p-3">{{ health.rabbitmq.error }}</div>
        </el-card>
      </section>

      <el-card>
        <template #header>
          <div class="flex items-center gap-2">
            <CpuChipIcon class="w-5 h-5 text-emerald-500" />
            <span class="font-semibold">JVM 内存</span>
          </div>
        </template>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-slate-800">{{ ((health.jvm.usedMemoryMB ?? 0) / 1024).toFixed(1) }} GB</div>
            <div class="text-sm text-slate-500">已用内存</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-blue-600">{{ ((health.jvm.maxMemoryMB ?? 0) / 1024).toFixed(1) }} GB</div>
            <div class="text-sm text-slate-500">最大内存</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-emerald-600">{{ ((health.jvm.freeMemoryMB ?? 0) / 1024).toFixed(1) }} GB</div>
            <div class="text-sm text-slate-500">空闲内存</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-violet-600">{{ health.jvm.processors }}</div>
            <div class="text-sm text-slate-500">CPU 核心数</div>
          </div>
        </div>
        <div class="bg-slate-100 rounded-full h-3 overflow-hidden">
          <div class="bg-gradient-to-r from-blue-500 to-violet-500 h-full rounded-full transition-all"
               :style="{ width: ((health.jvm.usedMemoryMB ?? 0) / (health.jvm.maxMemoryMB ?? 1) * 100).toFixed(1) + '%' }" />
        </div>
        <div class="text-sm text-slate-500 mt-1">
          使用率 {{ ((health.jvm.usedMemoryMB ?? 0) / (health.jvm.maxMemoryMB ?? 1) * 100).toFixed(1) }}%
        </div>
      </el-card>

      <el-card>
        <template #header>
          <div class="flex items-center gap-2">
            <ServerIcon class="w-5 h-5 text-amber-500" />
            <span class="font-semibold">磁盘空间</span>
          </div>
        </template>
        <div class="grid grid-cols-3 gap-4 mb-4">
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-slate-800">{{ health.disk.totalGB }} GB</div>
            <div class="text-sm text-slate-500">总容量</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-emerald-600">{{ health.disk.freeGB }} GB</div>
            <div class="text-sm text-slate-500">可用空间</div>
          </div>
          <div class="rounded-xl border border-slate-200 p-4">
            <div class="text-2xl font-bold text-amber-600">{{ health.disk.usableGB }} GB</div>
            <div class="text-sm text-slate-500">可写入</div>
          </div>
        </div>
        <div class="bg-slate-100 rounded-full h-3 overflow-hidden">
          <div class="bg-gradient-to-r from-emerald-500 to-amber-500 h-full rounded-full"
               :style="{ width: ((1 - health.disk.freeGB / (health.disk.totalGB || 1)) * 100).toFixed(1) + '%' }" />
        </div>
        <div class="text-sm text-slate-500 mt-1">
          已用 {{ ((1 - health.disk.freeGB / (health.disk.totalGB || 1)) * 100).toFixed(1) }}%
        </div>
      </el-card>
    </template>
  </div>
</template>
