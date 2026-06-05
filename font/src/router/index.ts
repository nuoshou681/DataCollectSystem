import { createRouter, createWebHistory } from 'vue-router'

import UserLayout from '@/views/layouts/UserLayout.vue'
import AdminLayout from '@/views/layouts/AdminLayout.vue'

const routes = [
  {
    path: '/',
    component: UserLayout,
    meta: { requiresAuth: true, requiresRole: 'user' },
    children: [
      { path: '', component: () => import('@/views/HomeView.vue') },
      { path: 'tasks', component: () => import('@/views/TaskListView.vue') },
      { path: 'tasks/create', component: () => import('@/views/TaskCreateView.vue') },
      { path: 'tasks/:id', component: () => import('@/views/TaskDetailView.vue') },
      { path: 'task-center', component: () => import('@/views/TaskManagementView.vue') },
      { path: 'schedules', component: () => import('@/views/TaskScheduleView.vue') },
      { path: 'stats', component: () => import('@/views/StatsDashboardView.vue') },
      { path: 'results', component: () => import('@/views/ResultCenterView.vue') },
      { path: 'exports', component: () => import('@/views/ExportHistoryView.vue') },
      { path: 'profile', component: () => import('@/views/ProfileView.vue') },
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresRole: 'admin' },
    children: [
      { path: '', component: () => import('@/views/AdminDashboardView.vue') },
      { path: 'task-center', component: () => import('@/views/AdminTaskCenterView.vue') },
      { path: 'task/:id', component: () => import('@/views/TaskDetailView.vue') },

      { path: 'exports', component: () => import('@/views/AdminExportCenterView.vue') },
      { path: 'nodes', component: () => import('@/views/AdminNodeView.vue') },


      { path: 'logs', component: () => import('@/views/LogView.vue') },
      { path: 'profile', component: () => import('@/views/ProfileView.vue') },
      { path: 'users', component: () => import('@/views/AdminUserView.vue') },
      { path: 'notifications', component: () => import('@/views/AdminNotificationView.vue') },
      { path: 'config', component: () => import('@/views/AdminConfigView.vue') },
    ]
  },
  { path: '/login', component: () => import('@/views/LoginView.vue') },
  { path: '/register', component: () => import('@/views/RegisterView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

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
