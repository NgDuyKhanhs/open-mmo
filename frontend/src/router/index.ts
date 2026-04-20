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
const PrivacyPolicyView = () => import('@/views/PrivacyPolicyView.vue')
const TermsOfServiceView = () => import('@/views/TermsOfServiceView.vue')
const NotFoundView = () => import('@/views/NotFoundView.vue')

// Email AI Bot layout + nested views
const EmailAiBotLayout = () => import('@/views/email-ai-bot/EmailAiBotLayout.vue')
const MailboxView = () => import('@/views/email-ai-bot/MailboxView.vue')
const ConfigView = () => import('@/views/email-ai-bot/ConfigView.vue')
const StatusView = () => import('@/views/email-ai-bot/StatusView.vue')
const RemindersView = () => import('@/views/email-ai-bot/RemindersView.vue')
const HistoryView = () => import('@/views/email-ai-bot/HistoryView.vue')

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
      title: 'Introduction - OpenMMO.ai - Turn ideas into digital businesses with AI',
      description:
        'Explore OpenMMO - A complete platform for online monetization with training, tools, and 24/7 support.',
      requiresAuth: false,
    },
  },
  {
    path: '/services',
    name: 'services',
    component: ServicesView,
    meta: {
      title: 'Services - OpenMMO.ai - Turn ideas into digital businesses with AI',
      description:
        'Discover OpenMMO services: monetization, skills, tools, and support to grow your career.',
      requiresAuth: false,
    },
  },
  {
    path: '/contact',
    name: 'contact',
    component: ContactView,
    meta: {
      title: 'Contact - OpenMMO.ai - Turn ideas into digital businesses with AI',
      description: 'Contact the OpenMMO team for support and consulting.',
      requiresAuth: false,
    },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      title: 'Log In - OpenMMO.ai - Turn ideas into digital businesses with AI',
      description: 'Sign in to your OpenMMO account.',
      requiresAuth: false,
    },
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: {
      title: 'My Profile - OpenMMO.ai - Turn ideas into digital businesses with AI',
      description: 'Manage your personal profile details.',
      requiresAuth: true,
    },
  },
  {
    path: '/email-ai-bot',
    component: EmailAiBotLayout,
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        redirect: { name: 'email-bot-mailbox' },
      },
      {
        path: 'mailbox',
        name: 'email-bot-mailbox',
        component: MailboxView,
        meta: {
          title: 'Mailbox - Email AI Bot',
          description: 'Manage your email mailbox with AI assistance.',
          requiresAuth: true,
        },
      },
      {
        path: 'config',
        name: 'email-bot-config',
        component: ConfigView,
        meta: {
          title: 'Configuration - Email AI Bot',
          description: 'Configure your email AI bot settings.',
          requiresAuth: true,
        },
      },
      {
        path: 'status',
        name: 'email-bot-status',
        component: StatusView,
        meta: {
          title: 'Status - Email AI Bot',
          description: 'Check your email AI bot status and settings.',
          requiresAuth: true,
        },
      },
      {
        path: 'reminders',
        name: 'email-bot-reminders',
        component: RemindersView,
        meta: {
          title: 'Reminders - Email AI Bot',
          description: 'Manage email reminders.',
          requiresAuth: true,
        },
      },
      {
        path: 'history',
        name: 'email-bot-history',
        component: HistoryView,
        meta: {
          title: 'History - Email AI Bot',
          description: 'View email reminder history and statistics.',
          requiresAuth: true,
        },
      },
    ],
  },
  {
    path: '/privacy-policy',
    name: 'privacy-policy',
    component: PrivacyPolicyView,
    meta: {
      title: 'Privacy Policy - OpenMMO.ai',
      description: 'Privacy Policy for OpenMMO - protecting your data and privacy.',
      requiresAuth: false,
    },
  },
  {
    path: '/terms-of-service',
    name: 'terms-of-service',
    component: TermsOfServiceView,
    meta: {
      title: 'Terms of Service - OpenMMO.ai',
      description: 'Terms of Service for OpenMMO - our commitment to you.',
      requiresAuth: false,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: {
      title: '404 - Page Not Found',
      description: 'The page you are looking for does not exist.',
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
  const title = (to.meta.title as string) || 'OpenMMO - Online Monetization'
  const description =
    (to.meta.description as string) || 'OpenMMO - A complete platform for online monetization'

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
