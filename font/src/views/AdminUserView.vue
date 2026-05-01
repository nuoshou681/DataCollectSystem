<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchUsers, createUser, updateUser, resetUserPassword, setUserStatus,
} from '@/api/api'
import type { UserInfo } from '@/types/entity'

const users = ref<UserInfo[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogUser = ref({ userId: 0, username: '', email: '', password: '', role: 'user' })

const stats = computed(() => ({
  total: users.value.length,
  active: users.value.filter(u => u.status === 'ACTIVE' || !u.status).length,
  admin: users.value.filter(u => u.role === 'admin').length,
  disabled: users.value.filter(u => u.status === 'DISABLED').length,
}))

async function loadUsers() {
  loading.value = true
  try {
    users.value = await fetchUsers()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  dialogUser.value = { userId: 0, username: '', email: '', password: '', role: 'user' }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (dialogMode.value === 'create') {
    const res = await createUser(dialogUser.value)
    if (res && (res as any).code !== undefined && (res as any).code !== 0) {
      ElMessage.error((res as any).msg || '创建失败')
      return
    }
    ElMessage.success('用户创建成功')
  } else {
    await updateUser(dialogUser.value.userId, {
      username: dialogUser.value.username,
      email: dialogUser.value.email,
      role: dialogUser.value.role,
    })
    ElMessage.success('用户更新成功')
  }
  dialogVisible.value = false
  loadUsers()
}

async function handleResetPassword(userId: number) {
  try {
    await ElMessageBox.confirm('确定要重置该用户密码为 123456 吗？', '重置密码', { type: 'warning' })
    await resetUserPassword(userId)
    ElMessage.success('密码已重置为 123456')
  } catch { /* cancelled */ }
}

async function handleToggleStatus(user: UserInfo) {
  const newStatus = user.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED'
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, `${action}用户`, { type: 'warning' })
    await setUserStatus(user.userId!, newStatus)
    ElMessage.success(`用户已${action}`)
    loadUsers()
  } catch { /* cancelled */ }
}

function formatTime(v?: string | null) {
  if (!v) return '-'
  return v.replace('T', ' ').substring(0, 19)
}

onMounted(loadUsers)
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold text-white">用户管理</h1>

    <div class="grid grid-cols-4 gap-4">
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-white">{{ stats.total }}</div>
        <div class="text-gray-400 text-sm">总用户数</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-emerald-400">{{ stats.active }}</div>
        <div class="text-gray-400 text-sm">活跃用户</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-amber-400">{{ stats.admin }}</div>
        <div class="text-gray-400 text-sm">管理员</div>
      </div>
      <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl p-4">
        <div class="text-2xl font-bold text-red-400">{{ stats.disabled }}</div>
        <div class="text-gray-400 text-sm">已禁用</div>
      </div>
    </div>

    <div class="flex justify-between items-center">
      <div class="text-gray-400 text-sm">共 {{ users.length }} 个用户</div>
      <button class="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm" @click="openCreate">
        新建用户
      </button>
    </div>

    <div class="bg-gray-800/60 border border-gray-700/50 rounded-xl overflow-hidden">
      <table class="w-full text-sm text-gray-300">
        <thead class="border-b border-gray-700/50">
          <tr class="text-left text-gray-400 text-xs uppercase">
            <th class="px-4 py-3">ID</th>
            <th class="px-4 py-3">用户名</th>
            <th class="px-4 py-3">邮箱</th>
            <th class="px-4 py-3">角色</th>
            <th class="px-4 py-3">状态</th>
            <th class="px-4 py-3">上次登录</th>
            <th class="px-4 py-3">注册时间</th>
            <th class="px-4 py-3 text-right">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-700/30">
          <tr v-for="u in users" :key="u.userId" class="hover:bg-gray-700/30 transition-colors">
            <td class="px-4 py-3 font-mono text-gray-500">{{ u.userId }}</td>
            <td class="px-4 py-3 text-white">{{ u.username }}</td>
            <td class="px-4 py-3">{{ u.email }}</td>
            <td class="px-4 py-3">
              <span :class="u.role === 'admin' ? 'text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded' : 'text-blue-400 bg-blue-400/10 px-2 py-0.5 rounded'">
                {{ u.role === 'admin' ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span :class="u.status === 'DISABLED' ? 'text-red-400 bg-red-400/10 px-2 py-0.5 rounded' : 'text-emerald-400 bg-emerald-400/10 px-2 py-0.5 rounded'">
                {{ u.status === 'DISABLED' ? '已禁用' : '正常' }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500">{{ formatTime(u.lastLoginAt) }}</td>
            <td class="px-4 py-3 text-gray-500">{{ formatTime(u.createdAt) }}</td>
            <td class="px-4 py-3 text-right space-x-2">
              <button class="text-blue-400 hover:text-blue-300 text-xs" @click="dialogUser = { userId: u.userId!, username: u.username, email: u.email, password: '', role: u.role! }; dialogMode = 'edit'; dialogVisible = true">编辑</button>
              <button class="text-amber-400 hover:text-amber-300 text-xs" @click="handleResetPassword(u.userId!)">重置密码</button>
              <button :class="u.status === 'DISABLED' ? 'text-emerald-400 hover:text-emerald-300' : 'text-red-400 hover:text-red-300'" class="text-xs" @click="handleToggleStatus(u)">
                {{ u.status === 'DISABLED' ? '启用' : '禁用' }}
              </button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="8" class="px-4 py-8 text-center text-gray-500">暂无用户</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建用户' : '编辑用户'" width="480px" :close-on-click-modal="false">
      <div class="space-y-4">
        <div>
          <label class="text-gray-300 text-sm block mb-1">用户名</label>
          <input v-model="dialogUser.username" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-blue-500" placeholder="请输入用户名">
        </div>
        <div>
          <label class="text-gray-300 text-sm block mb-1">邮箱</label>
          <input v-model="dialogUser.email" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-blue-500" placeholder="请输入邮箱">
        </div>
        <div v-if="dialogMode === 'create'">
          <label class="text-gray-300 text-sm block mb-1">密码</label>
          <input v-model="dialogUser.password" type="password" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-blue-500" placeholder="请输入密码">
        </div>
        <div>
          <label class="text-gray-300 text-sm block mb-1">角色</label>
          <select v-model="dialogUser.role" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-blue-500">
            <option value="user">普通用户</option>
            <option value="admin">管理员</option>
          </select>
        </div>
      </div>
      <template #footer>
        <button class="bg-gray-600 hover:bg-gray-500 text-white px-4 py-2 rounded-lg text-sm mr-2" @click="dialogVisible = false">取消</button>
        <button class="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm" @click="handleSubmit">确定</button>
      </template>
    </el-dialog>
  </div>
</template>
