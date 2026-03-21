import { createRouter, createWebHistory } from 'vue-router'

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
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrdersView.vue'),
        },
        {
          path: 'vehicles',
          name: 'vehicles',
          component: () => import('@/views/VehicleView.vue'),
        },
        {
          path: 'dispatch',
          name: 'dispatch',
          component: () => import('@/views/DispatchView.vue'),
        },
        {
          path: 'tracking',
          name: 'tracking',
          component: () => import('@/views/TrackingView.vue'),
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/StatsView.vue'),
        },
        {
          path: 'driver/tasks',
          name: 'driverTasks',
          component: () => import('@/views/DriverTaskView.vue'),
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to) => {
  const token = sessionStorage.getItem('accessToken')
  if (to.meta.requiresAuth && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
})

export default router
