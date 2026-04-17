import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'
import {
  getReminderHistory,
  getReminderHistoryStats,
  getReminderHistoryDetail,
  type ReminderHistory,
  type ReminderHistoryStats,
} from '@/services/reminderService'

export function useReminderHistory() {
  const authStore = useAuthStore()

  const reminderHistories = ref<ReminderHistory[]>([])
  const reminderHistoryStats = ref<ReminderHistoryStats | null>(null)
  const isLoadingReminderHistory = ref(false)
  const filterHistoryStatus = ref('')
  const filterHistoryContact = ref('')
  const historyDetailModal = ref<ReminderHistory | null>(null)

  const loadReminderHistory = async () => {
    if (!authStore.accessToken) return
    isLoadingReminderHistory.value = true
    try {
      const histories = await getReminderHistory(authStore.accessToken)
      reminderHistories.value = histories
    } catch (err) {
      console.error('Failed to load reminder history:', err)
      toast.error('Failed to load reminder history')
    } finally {
      isLoadingReminderHistory.value = false
    }
  }

  const loadReminderHistoryStats = async () => {
    if (!authStore.accessToken) return
    try {
      const stats = await getReminderHistoryStats(authStore.accessToken)
      reminderHistoryStats.value = stats
    } catch (err) {
      console.error('Failed to load reminder history stats:', err)
    }
  }

  const formatReminderDate = (dateStr: string) => {
    const date = new Date(dateStr)
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const viewReminderHistoryDetail = async (history: ReminderHistory) => {
    try {
      if (!authStore.accessToken) return
      const fullDetail = await getReminderHistoryDetail(authStore.accessToken, history.id)
      historyDetailModal.value = fullDetail
    } catch (err) {
      console.error('Failed to fetch full detail:', err)
      toast.error('Failed to load reminder details')
    }
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    toast.success('Copied to clipboard!')
  }

  const filteredReminderHistories = computed(() => {
    return reminderHistories.value.filter(h => {
      const statusMatch = !filterHistoryStatus.value || h.status === filterHistoryStatus.value
      const contactMatch = !filterHistoryContact.value || h.contactEmail.toLowerCase().includes(filterHistoryContact.value.toLowerCase())
      return statusMatch && contactMatch
    })
  })

  return {
    reminderHistories,
    reminderHistoryStats,
    isLoadingReminderHistory,
    filterHistoryStatus,
    filterHistoryContact,
    historyDetailModal,
    filteredReminderHistories,
    loadReminderHistory,
    loadReminderHistoryStats,
    formatReminderDate,
    viewReminderHistoryDetail,
    copyToClipboard,
  }
}

