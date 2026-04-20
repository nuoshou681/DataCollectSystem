<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

interface ProfileForm {
  name: string
  email: string
  role: string
}

function parseTokenProfile(): ProfileForm {
  const token = localStorage.getItem('token')
  if (!token) {
    return { name: '', email: '', role: '' }
  }
  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return { name: '', email: '', role: localStorage.getItem('role') || '' }
    }
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = atob(normalized)
    const parsed = JSON.parse(decoded)
    return {
      name: parsed.username || '',
      email: parsed.email || '',
      role: parsed.role || localStorage.getItem('role') || '',
    }
  } catch {
    return { name: '', email: '', role: localStorage.getItem('role') || '' }
  }
}

const tokenProfile = parseTokenProfile()

const form = ref<ProfileForm>({
  name: localStorage.getItem('profileName') || tokenProfile.name,
  email: localStorage.getItem('profileEmail') || tokenProfile.email,
  role: localStorage.getItem('profileRole') || tokenProfile.role || 'user',
})

const roleLabel = computed(() => (form.value.role === 'admin' ? '管理员' : '用户'))

function saveProfile() {
  localStorage.setItem('profileName', form.value.name.trim())
  localStorage.setItem('profileEmail', form.value.email.trim())
  localStorage.setItem('profileRole', form.value.role)
  ElMessage.success('个人资料已保存')
}
</script>

<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-title">个人资料</div>
      </template>
      <el-form label-width="120px" class="profile-form">
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="64" />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="roleLabel" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  justify-content: center;
  padding: 28px 20px;
}

.profile-card {
  width: min(720px, 100%);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.profile-form {
  margin-top: 10px;
}
</style>
