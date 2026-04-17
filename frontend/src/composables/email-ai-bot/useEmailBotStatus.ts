import { ref, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'
import {
  getGmailStatus,
  startGmailConnection,
  enableBot,
  disableBot,
  type GmailStatus,
} from '@/services/gmailService'

export function useEmailBotStatus() {
  const authStore = useAuthStore()
  const status = ref<GmailStatus>({
    connected: false,
    gmailAddress: '',
    botEnabled: false,
    triggerSubject: 'openmmo',
    lastRunAt: null,
    lastError: null,
  })
  const isSaving = ref(false)

  const loadStatus = async () => {
    try {
      if (!authStore.accessToken) {
        toast.error('Authentication required')
        return
      }

      status.value = await getGmailStatus(authStore.accessToken)
    } catch (err) {
      console.error('Failed to load status:', err)
      toast.error(err instanceof Error ? err.message : 'Failed to load status')
    }
  }

  const connectGmail = async () => {
    try {
      if (!authStore.accessToken) {
        toast.error('Authentication required. Please login first.')
        return
      }
      const response = await startGmailConnection(authStore.accessToken)
      window.location.href = response.url
    } catch (err) {
      console.error('Failed to connect Gmail:', err)
      toast.error(err instanceof Error ? err.message : 'Error connecting to Gmail')
    }
  }

  const toggleBot = async () => {
    isSaving.value = true
    try {
      if (status.value.botEnabled) {
        await disableBot(authStore.accessToken)
        toast.success('Bot disabled')
      } else {
        await enableBot(authStore.accessToken)
        toast.success('Bot enabled')
      }
      await loadStatus()
    } catch (err) {
      console.error('Failed to toggle bot:', err)
      toast.error(err instanceof Error ? err.message : 'Error toggling bot')
    } finally {
      isSaving.value = false
    }
  }

  return {
    status,
    isSaving,
    loadStatus,
    connectGmail,
    toggleBot,
  }
}

