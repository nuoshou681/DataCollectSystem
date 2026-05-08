<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LockClosedIcon } from '@heroicons/vue/24/outline'
import { login } from '@/api/api'
import { persistAuthSession } from '@/utils/auth'

const email = ref('')
const password = ref('')
const loading = ref(false)
const router = useRouter()

async function handleLogin() {
  if (!email.value.trim() || !password.value) {
    ElMessage.warning('请填写邮箱和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(email.value, password.value)
    if (res === null) {
      email.value = ''
      password.value = ''
      ElMessage.error('邮箱或密码错误')
    } else {
      persistAuthSession(String(res.token), String(res.role || 'user'))
      if (res.role === 'admin') {
        router.push('/admin')
      } else {
        router.push('/')
      }
    }
  } catch {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-slate-50">
    <div class="bg-white rounded-2xl shadow-lg border border-slate-200 w-[400px] max-w-[92vw] p-8">
      <div class="text-center mb-8">
        <div class="mx-auto w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center mb-4">
          <LockClosedIcon class="w-6 h-6 text-blue-600" />
        </div>
        <h2 class="text-2xl font-bold text-slate-800">登录</h2>
        <p class="text-sm text-slate-500 mt-1">分布式数据采集系统</p>
      </div>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="邮箱">
          <el-input v-model="email" type="email" placeholder="请输入邮箱" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="请输入密码" size="large" show-password @keydown.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full mt-2" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <div class="mt-6 text-sm text-center text-slate-500">
        还没有账号？
        <RouterLink to="/register" class="text-blue-600 font-medium hover:text-blue-700">立即注册</RouterLink>
      </div>
    </div>
  </div>
</template>
