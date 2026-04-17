import { ref } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'
import {
  getAiProvider,
  setAiProvider,
} from '@/services/gmailService'

export function useAiProvider() {
  const authStore = useAuthStore()
  const selectedAiProvider = ref<'gemini' | 'groq'>('groq')

  const loadAiProvider = async () => {
    try {
      const savedProvider = localStorage.getItem('selectedAiProvider')
      if (savedProvider === 'gemini' || savedProvider === 'groq') {
        selectedAiProvider.value = savedProvider
      }

      if (authStore.accessToken) {
        try {
          const backendProvider = await getAiProvider(authStore.accessToken)
          if (backendProvider === 'gemini' || backendProvider === 'groq') {
            selectedAiProvider.value = backendProvider
            localStorage.setItem('selectedAiProvider', backendProvider)
          }
        } catch (err) {
          console.debug('Could not load AI provider from backend, using localStorage:', err)
        }
      }
    } catch (err) {
      console.error('Failed to load AI provider:', err)
    }
  }

  const changeAiProvider = async (provider: 'gemini' | 'groq') => {
    try {
      selectedAiProvider.value = provider
      localStorage.setItem('selectedAiProvider', provider)

      if (authStore.accessToken) {
        await setAiProvider(authStore.accessToken, provider)
        console.log(`✅ Switched AI provider to ${provider} and saved to backend`)
      }

      toast.success(`Switched to ${provider === 'gemini' ? 'Gemini' : 'Groq'} AI provider`)
    } catch (err) {
      console.error('Failed to change AI provider:', err)
      toast.error(err instanceof Error ? err.message : 'Error changing AI provider')
    }
  }

  return {
    selectedAiProvider,
    loadAiProvider,
    changeAiProvider,
  }
}

