<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserPlusIcon } from '@heroicons/vue/24/outline'
import { register } from '@/api/api'

const username = ref('')
const email = ref('')
const password = ref('')
const confirm = ref('')
const loading = ref(false)
const router = useRouter()

async function handleRegister() {
  if (!username.value.trim()) { ElMessage.warning('用户名不能为空'); return }
  if (!email.value.trim()) { ElMessage.warning('邮箱不能为空'); return }
  if (!password.value) { ElMessage.warning('密码不能为空'); return }
  if (password.value !== confirm.value) { ElMessage.warning('两次密码不一致'); return }
  if (password.value.length < 6) { ElMessage.warning('密码至少6位'); return }

  loading.value = true
  try {
    await register(username.value.trim(), email.value, password.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: unknown) {
    if (typeof e === 'object' && e !== null && 'message' in e) {
      ElMessage.error((e as { message?: string }).message || '注册失败')
    } else {
      ElMessage.error('网络错误')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-slate-50">
    <div class="bg-white rounded-2xl shadow-lg border border-slate-200 w-[400px] max-w-[92vw] p-8">
      <div class="text-center mb-8">
        <div class="mx-auto w-12 h-12 bg-emerald-100 rounded-xl flex items-center justify-center mb-4">
          <UserPlusIcon class="w-6 h-6 text-emerald-600" />
        </div>
        <h2 class="text-2xl font-bold text-slate-800">注册</h2>
        <p class="text-sm text-slate-500 mt-1">创建账号以使用采集系统</p>
      </div>

      <el-form label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="请输入用户名" size="large" maxlength="32" clearable />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="email" type="email" placeholder="请输入邮箱" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="至少6位密码" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="confirm" type="password" placeholder="再次输入密码" size="large" show-password @keydown.enter="handleRegister" />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full mt-2" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form>

      <div class="mt-6 text-sm text-center text-slate-500">
        已有账号？
        <RouterLink to="/login" class="text-blue-600 font-medium hover:text-blue-700">立即登录</RouterLink>
      </div>
    </div>
  </div>
</template>
