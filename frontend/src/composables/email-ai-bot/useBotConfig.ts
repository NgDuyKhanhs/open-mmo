import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'
import {
  updateBotConfig,
  updateCustomPrompt,
  getGmailStatus,
  type GmailStatus,
} from '@/services/gmailService'

export function useBotConfig() {
  const authStore = useAuthStore()
  const configSubject = ref('')
  const configPrompt = ref('')
  const originalConfigSubject = ref('')
  const originalConfigPrompt = ref('')
  const isSaving = ref(false)

  const hasConfigChanged = computed(() => {
    const subjectChanged = configSubject.value !== originalConfigSubject.value
    const promptChanged = configPrompt.value !== originalConfigPrompt.value
    return subjectChanged || promptChanged
  })

  const loadCustomPrompt = async () => {
    try {
      if (!authStore.accessToken) return
      const { API_CONFIG } = await import('@/config/apiConfig')
      const response = await fetch(API_CONFIG.GMAIL.BOT_PROMPT, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authStore.accessToken}`,
        },
      })
      if (response.ok) {
        const data = await response.json()
        if (data.customPrompt) configPrompt.value = data.customPrompt
        originalConfigPrompt.value = data.customPrompt || ''
      }
    } catch (err) {
      console.error('Failed to load custom prompt:', err)
    }
  }

  const updateConfig = async (status: GmailStatus) => {
    isSaving.value = true
    try {
      if (configSubject.value !== status.triggerSubject) {
        await updateBotConfig(authStore.accessToken, {
          triggerSubject: configSubject.value,
        })
      }

      if (configPrompt.value !== '') {
        await updateCustomPrompt(authStore.accessToken, configPrompt.value)
      }

      // Reset original values to disable Save button
      originalConfigSubject.value = configSubject.value
      originalConfigPrompt.value = configPrompt.value

      toast.success('Configuration saved')
    } catch (err) {
      console.error('Failed to update config:', err)
      toast.error(err instanceof Error ? err.message : 'Error updating configuration')
    } finally {
      isSaving.value = false
    }
  }

  const initializeConfig = (status: GmailStatus) => {
    configSubject.value = status.triggerSubject
    originalConfigSubject.value = status.triggerSubject
  }

  return {
    configSubject,
    configPrompt,
    originalConfigSubject,
    originalConfigPrompt,
    isSaving,
    hasConfigChanged,
    loadCustomPrompt,
    updateConfig,
    initializeConfig,
  }
}

