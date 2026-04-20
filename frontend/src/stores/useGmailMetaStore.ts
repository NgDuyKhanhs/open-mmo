import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from './useAuthStore'
import { getGmailStatus, getAiProvider, type GmailStatus } from '@/services/gmailService'

const TTL_MS = 30 * 1000 // 30 seconds cache

export const useGmailMetaStore = defineStore('gmailMeta', () => {
  // State
  const gmailStatus = ref<GmailStatus | null>(null)
  const aiProvider = ref<'gemini' | 'groq'>('gemini')
  const isLoadingStatus = ref(false)
  const isLoadingProvider = ref(false)
  const error = ref<string | null>(null)
  const lastFetchedAt = ref<number>(0)

  // Inflight promise for deduplication
  let inflightPromise: Promise<void> | null = null

  // Getters
  const isLoading = computed(() => isLoadingStatus.value || isLoadingProvider.value)
  const isCacheValid = computed(() => {
    const now = Date.now()
    return lastFetchedAt.value > 0 && (now - lastFetchedAt.value) < TTL_MS
  })

  // Fetch both endpoints in parallel
  const _fetchMeta = async (token: string) => {
    try {
      error.value = null
      isLoadingStatus.value = true
      isLoadingProvider.value = true

      // Fetch both in parallel
      const [status, provider] = await Promise.all([
        getGmailStatus(token),
        getAiProvider(token).catch((err) => {
          console.debug('Could not load AI provider from backend:', err)
          return aiProvider.value // Return current value on error
        }),
      ])

      gmailStatus.value = status
      if (provider === 'gemini' || provider === 'groq') {
        aiProvider.value = provider
        localStorage.setItem('selectedAiProvider', provider)
      }

      lastFetchedAt.value = Date.now()
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to load Gmail metadata'
      console.error('Failed to load Gmail metadata:', err)
      throw err
    } finally {
      isLoadingStatus.value = false
      isLoadingProvider.value = false
    }
  }

  // Main load function with cache + dedupe logic
  const ensureLoaded = async (options: { force?: boolean } = {}) => {
    const authStore = useAuthStore()
    const { force = false } = options

    if (!authStore.accessToken) {
      error.value = 'Authentication required'
      throw new Error('No access token')
    }

    // If we have a valid cache and not forcing, return immediately
    if (!force && isCacheValid.value && gmailStatus.value && aiProvider.value) {
      return
    }

    // If there's already an inflight request, return that promise (dedupe)
    if (inflightPromise) {
      return inflightPromise
    }

    // Create new inflight promise
    inflightPromise = _fetchMeta(authStore.accessToken)
      .finally(() => {
        inflightPromise = null // Clear inflight after completion
      })

    return inflightPromise
  }

  // Force refresh - bypass cache
  const refresh = async () => {
    return ensureLoaded({ force: true })
  }

  // Reset cache (e.g., on logout or token change)
  const reset = () => {
    gmailStatus.value = null
    aiProvider.value = 'gemini'
    isLoadingStatus.value = false
    isLoadingProvider.value = false
    error.value = null
    lastFetchedAt.value = 0
    inflightPromise = null
  }

  return {
    // State
    gmailStatus,
    aiProvider,
    isLoadingStatus,
    isLoadingProvider,
    error,
    lastFetchedAt,

    // Getters
    isLoading,
    isCacheValid,

    // Actions
    ensureLoaded,
    refresh,
    reset,
  }
})

