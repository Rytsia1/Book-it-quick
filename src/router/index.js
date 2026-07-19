import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

// Import views
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Dashboard from '@/views/Dashboard.vue'
import Analytics from '@/views/Analytics.vue'
import Bills from '@/views/Bills.vue'
import Categories from '@/views/Categories.vue'
import Budget from '@/views/Budget.vue'

/**
 * Route Configuration
 * Public routes: /login, /register
 * Protected routes: /dashboard, /analytics, /bills (require JWT token)
 */
const routes = [
  {
    path: '/',
    redirect: () => {
      // Either the legacy `token` key (set by the old Login.vue)
      // OR the new `accessToken` key (set by the new Login.vue)
      // counts as a valid auth signal. Without this dual-key
      // check, a user with only `accessToken` set would be sent
      // to /login even though they're authenticated.
      const hasAuth = !!(
        localStorage.getItem('token') ||
        localStorage.getItem('accessToken')
      )
      return hasAuth ? '/dashboard' : '/login'
    },
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      requiresAuth: false,
      title: 'Login - Personal Bookkeeping',
    },
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: {
      requiresAuth: false,
      title: 'Register - Personal Bookkeeping',
    },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: {
      requiresAuth: true,
      title: 'Dashboard - Personal Bookkeeping',
    },
  },
  {
    path: '/bills',
    name: 'Bills',
    component: Bills,
    meta: {
      requiresAuth: true,
      title: 'Bills Management - Personal Bookkeeping',
    },
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: Analytics,
    meta: {
      requiresAuth: true,
      title: 'Analytics - Personal Bookkeeping',
    },
  },
  {
    path: '/categories',
    name: 'Categories',
    component: Categories,
    meta: {
      requiresAuth: true,
      title: 'Custom Categories - Personal Bookkeeping',
    },
  },
  {
    path: '/budget',
    name: 'Budget',
    component: Budget,
    meta: {
      requiresAuth: true,
      title: 'Budget - Personal Bookkeeping',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

// Create router instance
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/**
 * Global Before Guard - Authentication Check
 * Section 3.1: Check for JWT token in localStorage
 * If token doesn't exist and route requires auth, redirect to /login
 */
router.beforeEach((to, from, next) => {
  // Accept either the legacy `token` key OR the new `accessToken`
  // key as proof of authentication. (The new auth flow writes
  // both, but we check both so a user with only one of them set
  // \u2014 e.g. after a partial migration or a manual localStorage
  // edit \u2014 is still recognised as logged-in.)
  const hasAuth = !!(
    localStorage.getItem('token') ||
    localStorage.getItem('accessToken')
  )

  // Update document title
  document.title = to.meta.title || 'Personal Bookkeeping'

  // Check if route requires authentication
  if (to.meta.requiresAuth && !hasAuth) {
    // User is not authenticated and trying to access protected route
    ElMessage.warning('Please log in first to access this page')
    next('/login')
    return
  }

  // If user is logged in and trying to access login/register page, redirect to dashboard
  if (!to.meta.requiresAuth && hasAuth && (to.path === '/login' || to.path === '/register')) {
    next('/dashboard')
    return
  }

  // Allow navigation
  next()
})

/**
 * Global After Hook - Optional: for loading state management
 */
router.afterEach((to) => {
  // Cleanup or additional tracking can be done here
})

export default router
