import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import TaskView from '@/views/TaskView.vue'
import LogView from '@/views/LogView.vue'
import AdminNodeView from '@/views/AdminNodeView.vue'
import AdminDashboardView from '@/views/AdminDashboardView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ProfileView from '@/views/ProfileView.vue'
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
      { path: 'profile', component: ProfileView },
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresRole: 'admin' },
    children: [
      { path: '', component: AdminDashboardView },
      { path: 'nodes', component: AdminNodeView },
      { path: 'logs', component: LogView },
      { path: 'profile', component: ProfileView },
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
