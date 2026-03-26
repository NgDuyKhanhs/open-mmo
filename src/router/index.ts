import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * Route metadata type definition
 */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    description?: string
    requiresAuth?: boolean
  }
}

/**
 * Lazy-loaded route components with code-splitting
 */
const IntroductionView = () => import('@/views/IntroductionView.vue')
const ServicesView = () => import('@/views/ServicesView.vue')
const ContactView = () => import('@/views/ContactView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const ProfileView = () => import('@/views/ProfileView.vue')
const NotFoundView = () => import('@/views/NotFoundView.vue')

/**
 * Route configuration with metadata and lazy loading
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/introduction',
  },
  {
    path: '/introduction',
    name: 'introduction',
    component: IntroductionView,
    meta: {
      title: 'Giới Thiệu - OpenMMO | Kiếm Tiền Online',
      description:
        'Khám phá OpenMMO - Nền tảng toàn diện để kiếm tiền online với đào tạo, công cụ và hỗ trợ 24/7',
      requiresAuth: false,
    },
  },
  {
    path: '/services',
    name: 'services',
    component: ServicesView,
    meta: {
      title: 'Dịch Vụ - OpenMMO | Kiếm Tiền, Kỹ Năng, Công Cụ',
      description:
        'Khám phá các dịch vụ của OpenMMO: kiếm tiền, kỹ năng, công cụ và hỗ trợ để phát triển sự nghiệp',
      requiresAuth: false,
    },
  },
  {
    path: '/contact',
    name: 'contact',
    component: ContactView,
    meta: {
      title: 'Liên Hệ - OpenMMO',
      description: 'Liên hệ với đội ngũ OpenMMO để nhận hỗ trợ và tư vấn',
      requiresAuth: false,
    },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      title: 'Đăng Nhập - OpenMMO',
      description: 'Đăng nhập vào tài khoản OpenMMO của bạn',
      requiresAuth: false,
    },
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: {
      title: 'Hồ Sơ Cá Nhân - OpenMMO',
      description: 'Quản lý thông tin hồ sơ cá nhân của bạn',
      requiresAuth: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: {
      title: '404 - Trang Không Tìm Thấy',
      description: 'Trang bạn tìm kiếm không tồn tại',
      requiresAuth: false,
    },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0 }
  },
  routes,
})

/**
 * Navigation guard to check authentication requirements
 */
router.beforeEach((to, _from, next) => {
  // Dynamic import to avoid circular dependency
  import('@/stores/useAuthStore').then(({ useAuthStore }) => {
    const authStore = useAuthStore()

    // Initialize auth state from localStorage on first load
    if (!authStore.user && !authStore.accessToken) {
      authStore.initializeAuth()
    }

    const requiresAuth = to.meta.requiresAuth ?? false

    if (requiresAuth && !authStore.isLoggedIn) {
      // Redirect to login if route requires auth and user is not logged in
      next({ name: 'login' })
    } else if (to.name === 'login' && authStore.isLoggedIn) {
      // Redirect to profile if user tries to access login while logged in
      next({ name: 'profile' })
    } else {
      next()
    }
  })
})

/**
 * Update document title and meta tags after each route navigation
 */
router.afterEach((to) => {
  const title = (to.meta.title as string) || 'OpenMMO - Kiếm Tiền Online'
  const description =
    (to.meta.description as string) || 'OpenMMO - Nền tảng toàn diện để kiếm tiền online'

  // Update page title
  document.title = title

  // Update meta description
  let metaDescription = document.querySelector('meta[name="description"]')
  if (!metaDescription) {
    metaDescription = document.createElement('meta')
    metaDescription.setAttribute('name', 'description')
    document.head.appendChild(metaDescription)
  }
  metaDescription.setAttribute('content', description)
})

export default router
