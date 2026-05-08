<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CheckBadgeIcon, ShieldCheckIcon, UsersIcon, XCircleIcon } from '@heroicons/vue/24/outline'
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

onMounted(loadUsers)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-xl font-semibold text-slate-800">用户管理</h2>
        <p class="text-sm text-slate-500 mt-1">共 {{ users.length }} 个用户</p>
      </div>
      <el-button type="primary" @click="openCreate">新建用户</el-button>
    </div>

    <section class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <el-card shadow="hover" class="border-l-4 border-l-blue-500">
        <div class="flex items-center gap-3"><UsersIcon class="w-6 h-6 text-blue-500" /><div><div class="text-sm text-slate-500">总用户数</div><div class="text-2xl font-bold text-blue-600">{{ stats.total }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-emerald-500">
        <div class="flex items-center gap-3"><CheckBadgeIcon class="w-6 h-6 text-emerald-500" /><div><div class="text-sm text-slate-500">活跃用户</div><div class="text-2xl font-bold text-emerald-600">{{ stats.active }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-amber-500">
        <div class="flex items-center gap-3"><ShieldCheckIcon class="w-6 h-6 text-amber-500" /><div><div class="text-sm text-slate-500">管理员</div><div class="text-2xl font-bold text-amber-600">{{ stats.admin }}</div></div></div>
      </el-card>
      <el-card shadow="hover" class="border-l-4 border-l-red-500">
        <div class="flex items-center gap-3"><XCircleIcon class="w-6 h-6 text-red-500" /><div><div class="text-sm text-slate-500">已禁用</div><div class="text-2xl font-bold text-red-600">{{ stats.disabled }}</div></div></div>
      </el-card>
    </section>

    <el-card>
      <template #header><span class="font-semibold">用户列表</span></template>
      <el-table :data="users" border stripe v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="角色" width="110">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'admin' ? 'warning' : 'info'">
              {{ scope.row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'DISABLED' ? 'danger' : 'success'">
              {{ scope.row.status === 'DISABLED' ? '已禁用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" min-width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" text type="primary" @click="dialogUser = { userId: scope.row.userId!, username: scope.row.username, email: scope.row.email, password: '', role: scope.row.role! }; dialogMode = 'edit'; dialogVisible = true">编辑</el-button>
            <el-button size="small" text type="warning" @click="handleResetPassword(scope.row.userId!)">重置密码</el-button>
            <el-button size="small" text :type="scope.row.status === 'DISABLED' ? 'success' : 'danger'" @click="handleToggleStatus(scope.row)">
              {{ scope.row.status === 'DISABLED' ? '启用' : '禁用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建用户' : '编辑用户'" width="480px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="dialogUser.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="dialogUser.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="密码">
          <el-input v-model="dialogUser.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="dialogUser.role" style="width:100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
