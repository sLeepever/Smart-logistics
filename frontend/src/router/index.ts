import { createRouter, createWebHistory } from 'vue-router'
import { isAppRole, resolveRoleHomeRoute } from '@/stores/auth'

const operationalRoles = ['admin', 'dispatcher']
const adminRoles = ['admin']
const driverRoles = ['driver']
const customerRoles = ['customer']

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('@/views/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { allowedRoles: operationalRoles },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrdersView.vue'),
          meta: { allowedRoles: operationalRoles },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UserManagementView.vue'),
          meta: { allowedRoles: adminRoles },
        },
        {
          path: 'vehicles',
          name: 'vehicles',
          component: () => import('@/views/VehicleView.vue'),
          meta: { allowedRoles: operationalRoles },
        },
        {
          path: 'dispatch',
          name: 'dispatch',
          component: () => import('@/views/DispatchView.vue'),
          meta: { allowedRoles: operationalRoles },
        },
        {
          path: 'tracking',
          name: 'tracking',
          component: () => import('@/views/TrackingView.vue'),
          meta: { allowedRoles: operationalRoles },
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/StatsView.vue'),
          meta: { allowedRoles: operationalRoles },
        },
        {
          path: 'driver/tasks',
          name: 'driverTasks',
          component: () => import('@/views/DriverTaskView.vue'),
          meta: { allowedRoles: driverRoles },
        },
        {
          path: 'customer/home',
          name: 'customerHome',
          component: () => import('@/views/CustomerHomeView.vue'),
          meta: { allowedRoles: customerRoles },
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to) => {
  const token = sessionStorage.getItem('accessToken')
  const role = sessionStorage.getItem('role') || ''

  if (to.meta.requiresAuth && !token) {
    return '/login'
  }

  if (token && !role) {
    sessionStorage.clear()
    if (to.path !== '/login') {
      return '/login'
    }
    return
  }

  if (token && !isAppRole(role)) {
    sessionStorage.clear()
    return '/login'
  }

  if (to.path === '/login' && token) {
    return resolveRoleHomeRoute(role)
  }

  const allowedRoles = (to.meta as { allowedRoles?: string[] }).allowedRoles
  if (token && allowedRoles?.length && !allowedRoles.includes(role)) {
    return resolveRoleHomeRoute(role)
  }
})

export default router
