import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'
import {
  getMailboxPage,
  type Email,
} from '@/services/gmailService'
import { useRouter } from 'vue-router'

interface PageCache {
  emails: Email[]
  nextPageToken: string | null
}

export function useMailboxPagination() {
  const authStore = useAuthStore()
  const router = useRouter()
  const emails = ref<Email[]>([])
  const emailsCache = ref<PageCache[]>([])
  const selectedBox = ref<'inbox' | 'sent' | 'spam' | 'trash'>('inbox')
  const selectedEmail = ref<Email | null>(null)
  const isLoading = ref(false)
  const currentPageToken = ref<string | null>(null)
  const currentPageIndex = ref(0)
  const hasNextPage = ref(false)

  const formatDate = (dateString: string) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const extractEmail = (emailString: string): string => {
    if (!emailString) return ''
    const match = emailString.match(/<(.+?)>/)
    return match ? match[1] : emailString
  }

  const loadEmails = async () => {
    isLoading.value = true
    try {
      if (!authStore.accessToken) {
        toast.error('Authentication required')
        router.push('/login')
        return
      }

      const response = await getMailboxPage(
        authStore.accessToken,
        selectedBox.value.toLowerCase(),
        10,
        undefined
      )

      emails.value = response.emails
      currentPageToken.value = response.nextPageToken || null
      pageCache.value = [{
        emails: response.emails,
        nextPageToken: response.nextPageToken || null
      }]
      currentPageIndex.value = 0
      hasNextPage.value = !!response.nextPageToken
    } catch (err) {
      console.error('Failed to load emails:', err)
      if (err instanceof Error && err.message.includes('401')) {
        const refreshed = await authStore.refreshTokenManual()
        if (refreshed && authStore.accessToken) {
          try {
            const response = await getMailboxPage(
              authStore.accessToken,
              selectedBox.value.toLowerCase(),
              10,
              undefined
            )
            emails.value = response.emails
            hasNextPage.value = !!response.nextPageToken
          } catch (retryErr) {
            toast.error(retryErr instanceof Error ? retryErr.message : 'Error loading emails')
          }
        } else {
          toast.error('Session expired, please login again')
          router.push('/login')
        }
      } else {
        toast.error(err instanceof Error ? err.message : 'Error loading emails')
      }
    } finally {
      isLoading.value = false
    }
  }

  const loadPreviousPage = async () => {
    if (currentPageIndex.value <= 0) return

    isLoading.value = true
    try {
      currentPageIndex.value--
      const cachedPage = emailsCache.value[currentPageIndex.value]

      if (cachedPage) {
        emails.value = cachedPage.emails
        currentPageToken.value = cachedPage.nextPageToken || null
        hasNextPage.value = !!cachedPage.nextPageToken
        window.scrollTo(0, 0)
      }
    } catch (err) {
      currentPageIndex.value++
      toast.error(err instanceof Error ? err.message : 'Error loading previous page')
    } finally {
      isLoading.value = false
    }
  }

  const loadNextPage = async () => {
    if (!hasNextPage.value || isLoading.value || !authStore.accessToken) return

    isLoading.value = true
    try {
      const response = await getMailboxPage(
        authStore.accessToken,
        selectedBox.value.toLowerCase(),
        10,
        currentPageToken.value || undefined
      )

      emails.value = response.emails
      currentPageToken.value = response.nextPageToken || null
      currentPageIndex.value++
      emailsCache.value.push({
        emails: response.emails,
        nextPageToken: response.nextPageToken || null
      })

      hasNextPage.value = !!response.nextPageToken
      window.scrollTo(0, 0)
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Error loading next page')
    } finally {
      isLoading.value = false
    }
  }

  const selectBox = async (box: typeof selectedBox.value) => {
    if (isLoading.value) {
      toast.warning('⏳ Please wait for emails to finish loading')
      return
    }
    selectedBox.value = box
    selectedEmail.value = null
    await loadEmails()
  }

  const selectEmail = (email: Email) => {
    selectedEmail.value = email
  }

  const closeModal = () => {
    selectedEmail.value = null
  }

  // Use ref for pageCache to maintain reactivity
  const pageCache = ref<PageCache[]>([])

  return {
    emails,
    selectedBox,
    selectedEmail,
    isLoading,
    currentPageToken,
    currentPageIndex,
    hasNextPage,
    pageCache,
    formatDate,
    extractEmail,
    loadEmails,
    loadPreviousPage,
    loadNextPage,
    selectBox,
    selectEmail,
    closeModal,
  }
}

