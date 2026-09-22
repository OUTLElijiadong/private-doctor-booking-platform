import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layout/Index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/Index.vue'), meta: { title: '数据看板', icon: 'Odometer' } },
      { path: 'doctors', component: () => import('@/views/doctor/List.vue'), meta: { title: '预约挂号', icon: 'FirstAidKit' } },
      { path: 'appointments', component: () => import('@/views/appointment/Index.vue'), meta: { title: '预约管理', icon: 'Calendar' } },
      { path: 'schedules', component: () => import('@/views/schedule/Index.vue'), meta: { title: '排班管理', icon: 'Clock', roles: ['DOCTOR', 'ADMIN'] } },
      { path: 'records', component: () => import('@/views/record/Index.vue'), meta: { title: '就诊病历', icon: 'Document' } },
      { path: 'prescriptions', component: () => import('@/views/prescription/Index.vue'), meta: { title: '电子处方', icon: 'Tickets' } },
      { path: 'evaluations', component: () => import('@/views/evaluation/Index.vue'), meta: { title: '医德评价', icon: 'Star' } },
      { path: 'reviews', component: () => import('@/views/review/Index.vue'), meta: { title: '资质审核', icon: 'Stamp', roles: ['DOCTOR', 'ADMIN'] } },
      { path: 'departments', component: () => import('@/views/department/Index.vue'), meta: { title: '科室管理', icon: 'OfficeBuilding', roles: ['ADMIN'] } },
      { path: 'medications', component: () => import('@/views/medication/Index.vue'), meta: { title: '药品管理', icon: 'Box', roles: ['ADMIN'] } },
      { path: 'suppliers', component: () => import('@/views/supplier/Index.vue'), meta: { title: '供应商管理', icon: 'Van', roles: ['ADMIN'] } },
      { path: 'users', component: () => import('@/views/user/Index.vue'), meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] } },
      { path: 'logs', component: () => import('@/views/log/Index.vue'), meta: { title: '运行日志', icon: 'Memo', roles: ['ADMIN'] } },
      { path: 'profile', component: () => import('@/views/profile/Index.vue'), meta: { title: '个人中心', icon: 'Setting' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '私人医生预约服务平台'
  const store = useUserStore()
  if (to.path === '/login' || to.path === '/register') {
    next()
    return
  }
  if (!store.token) {
    next('/login')
    return
  }
  // 角色路由控制
  if (to.meta.roles && !to.meta.roles.includes(store.role)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
