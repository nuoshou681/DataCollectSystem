<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink,useRouter } from 'vue-router';
import { login } from '@/api/api';
const email = ref('');
const password = ref('');
const error = ref('');
const router = useRouter();
async function handleLogin() {
  try {
    error.value = '';
    const res = await login(email.value, password.value);
    console.log("token is:",res);
    // 登陆失败，清除表单
    if(res === null) {
      email.value = '';
      password.value = ''
    }else {
      // res.data就是token
      if (res && res.data) {
        localStorage.setItem('token', String(res.data.token))
        localStorage.setItem('role', String(res.data.role || 'user'))
        // 跳转首页等后续操作
        if (res.data.role === 'admin') {
          router.push('/admin')
        } else {
          router.push('/')
        }
      }
    }
  } catch (e: unknown) {
    if (typeof e === 'object' && e && 'response' in e) {
      error.value = (e as { response?: { data?: { msg?: string } } }).response?.data?.msg || '网络错误';
    } else {
      error.value = '网络错误';
    }
  }
}
</script>
<template>
  <div class="flex flex-col items-center justify-center h-screen bg-gray-50">
    <form @submit.prevent="handleLogin" class=" bg-white p-8 rounded shadow w-80">
      <h2 class="text-2xl font-bold mb-6">登陆</h2>
      <input v-model="email" type="email" placeholder="邮箱" class=" input mb-4" required />
      <input v-model="password" type="password" placeholder="密码" class="input mb-4" required />
      <button type="submit" class="btn w-full">登陆</button>
      <div v-if="error" class="text-red-500 text-sm mt-2 text-center">{{ error }}</div>
      <div class="mt-4 text-sm text-center">
        <RouterLink to="/register" class=" text-indigo-600">注册</RouterLink>
      </div>
    </form>
  </div>
</template>
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
