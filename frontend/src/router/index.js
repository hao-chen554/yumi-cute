import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/create',
      name: 'create',
      component: () => import('../views/CreateOrderView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('../views/MyOrdersView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/orders/:id',
      name: 'orderDetail',
      component: () => import('../views/OrderDetailView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')

  // 规则一：标记了 requiresAuth 的页面，没登录就送去登录页
  if (to.meta.requiresAuth && !token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  // 规则二：已经登录了，就别再去看登录页和注册页了
  if ((to.path === '/login' || to.path === '/register') && token) {
    return { path: '/' }
  }
})

export default router