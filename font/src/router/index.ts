import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import TaskView from '@/views/TaskView.vue'
import LogView from '@/views/LogView.vue'
import AdminNodeView from '@/views/AdminNodeView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import MainLayout from '@/views/MainLayout.vue'
const routes = [
  {
    path: '/',
    component: MainLayout,
    meta:{ requiresAuth:true},
    children: [
      { path:'',component:HomeView },
      { path:'task',component:TaskView},
      { path:'log', component:LogView},
      { path:'admin/nodes', component:AdminNodeView }
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
    //测试
    next()
    // next('/login')
  } else {
    next()
  }
})
export default router
