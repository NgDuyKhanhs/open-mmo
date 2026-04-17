import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'

export function useAuthGuard() {
  const router = useRouter()
  const authStore = useAuthStore()

  const ensureLoggedInOrRedirect = async () => {
    authStore.initializeAuth()
    if (!authStore.isLoggedIn) {
      console.warn('⚠️  User not logged in, redirecting to login')
      router.push('/login')
      return false
    }
    return true
  }

  const handleOAuthCallback = async () => {
    const connected = router.currentRoute.value.query.connected
    const error = router.currentRoute.value.query.error

    if (connected === '1') {
      toast.success('Gmail connected successfully!')
      router.replace('/email-ai-bot')
    } else if (error) {
      console.error('❌ Gmail OAuth failed:', error)
      toast.error(`Failed to connect Gmail: ${error}`)
      router.replace('/email-ai-bot')
    }
  }

  return {
    ensureLoggedInOrRedirect,
    handleOAuthCallback,
  }
}

