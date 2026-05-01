import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import TaskView from '@/views/TaskView.vue'
import TaskManagementView from '@/views/TaskManagementView.vue'
import LogView from '@/views/LogView.vue'
import AdminNodeView from '@/views/AdminNodeView.vue'
import AdminDashboardView from '@/views/AdminDashboardView.vue'
import AdminBatchView from '@/views/AdminBatchView.vue'
import AdminTaskCenterView from '@/views/AdminTaskCenterView.vue'
import AdminExportCenterView from '@/views/AdminExportCenterView.vue'
import AdminUserView from '@/views/AdminUserView.vue'
import AdminConfigView from '@/views/AdminConfigView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ProfileView from '@/views/ProfileView.vue'
import ResultCenterView from '@/views/ResultCenterView.vue'
import ExportHistoryView from '@/views/ExportHistoryView.vue'
import UserLayout from '@/views/layouts/UserLayout.vue'
import AdminLayout from '@/views/layouts/AdminLayout.vue'
const routes = [
  {
    path: '/',
    component: UserLayout,
    meta: { requiresAuth: true, requiresRole: 'user' },
    children: [
      { path: '', component: HomeView },
      { path: 'tasks', component: TaskView },
      { path: 'task-center', component: TaskManagementView },
      { path: 'results', component: ResultCenterView },
      { path: 'exports', component: ExportHistoryView },
      { path: 'profile', component: ProfileView },
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresRole: 'admin' },
    children: [
      { path: '', component: AdminDashboardView },
      { path: 'task-center', component: AdminTaskCenterView },
      { path: 'exports', component: AdminExportCenterView },
      { path: 'nodes', component: AdminNodeView },
      { path: 'batches', component: AdminBatchView },
      { path: 'logs', component: LogView },
      { path: 'profile', component: ProfileView },
      { path: 'users', component: AdminUserView },
      { path: 'config', component: AdminConfigView },
    ]
  },
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

//路由守位
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  const role = localStorage.getItem('role') || 'user'
  if (to.meta.requiresRole === 'admin' && role !== 'admin') {
    next('/')
    return
  }
  if (to.meta.requiresRole === 'user' && role === 'admin') {
    next('/admin')
    return
  }

  next()
})
export default router
