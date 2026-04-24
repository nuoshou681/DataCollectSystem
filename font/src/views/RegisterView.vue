<template>
  <div class="flex flex-col items-center justify-center h-screen bg-gray-50">
    <form @submit.prevent="handleRegister" class="bg-white p-8 rounded shadow w-80">
      <h2 class="text-2xl font-bold mb-6">注册</h2>
      <input v-model="username" type="text" placeholder="用户名" class="input mb-4" required />
      <input v-model="email" type="email" placeholder="邮箱" class="input mb-4" required />
      <input v-model="password" type="password" placeholder="密码" class="input mb-4" required />
      <input v-model="confirm" type="password" placeholder="确认密码" class="input mb-4" required />
      <button type="submit" class="btn w-full">注册</button>
      <div class="mt-4 text-sm text-center">
        已有账号？
        <RouterLink to="/login" class="text-indigo-600">登录</RouterLink>
      </div>
      <div v-if="error" class="text-red-500 mt-2">{{ error }}</div>
    </form>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/api'
const username = ref('')
const email = ref('')
const password = ref('')
const confirm = ref('')
const error = ref('')
const router = useRouter()
async function handleRegister() {
  if (!username.value.trim()) {
    error.value = '用户名不能为空'
    return
  }
  if (password.value !== confirm.value) {
    error.value = '两次密码不一致'
    return
  }
  try {
    error.value = ''
    await register(username.value.trim(), email.value, password.value)
    router.push('/login')
  } catch (e: unknown) {
    // 只处理真正的网络错误，业务错误已由 axios 拦截器弹窗
    if (typeof e === 'object' && e !== null && 'message' in e) {
      error.value = (e as { message?: string }).message || '网络错误'
    } else {
      error.value = '网络错误'
    }
  }
}
</script>
<style scoped>
.input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn {
  background: #6366f1;
  color: #fff;
  padding: 8px;
  border-radius: 4px;
}
</style>
