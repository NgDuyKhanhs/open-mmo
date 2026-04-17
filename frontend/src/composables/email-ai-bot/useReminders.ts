import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { toast } from 'vue3-toastify'
import {
  getReminders,
  createReminder,
  updateReminder,
  deleteReminder,
  type ReminderConfig,
} from '@/services/reminderService'

export function useReminders() {
  const authStore = useAuthStore()

  // Types - rename UI config to avoid conflict with service type
  type UiReminderConfig = {
    contactEmail: string
    enabled: boolean
    afterInactive: number
    maxReminders: number
    sentCount: number
  }

  // State
  const reminders = ref<ReminderConfig[]>([])
  const contactsList = ref<string[]>([])
  const isLoadingContacts = ref(false)
  const newReminder = ref<UiReminderConfig>({
    contactEmail: '',
    enabled: true,
    afterInactive: 60,
    maxReminders: 5,
    sentCount: 0,
  })
  const editingReminderId = ref<string | null>(null)
  const isSavingReminder = ref(false)
  const showReminderForm = ref(false)
  const showDeleteConfirm = ref(false)
  const deleteConfirmEmail = ref<string>('')

  // Optional fields visibility
  const visibleOptionalFields = ref<{
    contactEmail: boolean
    maxReminders: boolean
    enabled: boolean
  }>({
    contactEmail: false,
    maxReminders: false,
    enabled: false,
  })

  // Auto-disable reminders when limit reached
  watch(
    reminders,
    (updatedReminders) => {
      updatedReminders.forEach((reminder) => {
        if (reminder.enabled && reminder.sentCount >= reminder.maxReminders) {
          reminder.enabled = false
        }
      })
    },
    { deep: true }
  )

  // Load reminders
  const loadReminders = async () => {
    try {
      if (!authStore.accessToken) return
      const remindersList = await getReminders(authStore.accessToken)
      reminders.value = remindersList || []
    } catch (err) {
      console.error('Failed to load reminders:', err)
      toast.error('Failed to load reminders')
    }
  }

  // Load contacts
  const loadContacts = async () => {
    try {
      if (!authStore.accessToken) return
      isLoadingContacts.value = true

      // Note: getContactsList is not available in reminderService
      // For now, contacts will be populated from Gmail when needed
      // In a future update, this endpoint should be added to the backend
      contactsList.value = []

      // TODO: Uncomment when backend API is available
      // const contacts = await getContactsList(authStore.accessToken)
      // contactsList.value = contacts || []
    } catch (err) {
      console.error('Failed to load contacts:', err)
      // Silently fail - contacts are optional
    } finally {
      isLoadingContacts.value = false
    }
  }

  // Save reminder
  const saveReminder = async () => {
    if (!newReminder.value.afterInactive || newReminder.value.afterInactive < 5) {
      toast.error('Remind after inactivity must be at least 5 minutes')
      return
    }

    if (visibleOptionalFields.value.maxReminders && (!newReminder.value.maxReminders || newReminder.value.maxReminders < 1)) {
      toast.error('Max reminders must be at least 1')
      return
    }

    if (visibleOptionalFields.value.contactEmail && !newReminder.value.contactEmail.trim()) {
      toast.error('Please enter a contact email or uncheck "Apply to Specific Contact"')
      return
    }

    isSavingReminder.value = true
    try {
      if (!authStore.accessToken) return

      const reminderData: ReminderConfig = {
        contactEmail: visibleOptionalFields.value.contactEmail ? newReminder.value.contactEmail : '',
        enabled: visibleOptionalFields.value.enabled ? newReminder.value.enabled : true,
        afterInactive: newReminder.value.afterInactive,
        maxReminders: visibleOptionalFields.value.maxReminders ? newReminder.value.maxReminders : 5,
        sentCount: newReminder.value.sentCount || 0,
      }

      if (editingReminderId.value) {
        const result = await updateReminder(authStore.accessToken, editingReminderId.value, reminderData)
        if (result.status === 'success') {
          toast.success(result.message)
          await loadReminders()
          resetReminderForm()
        } else {
          toast.error(result.message || 'Failed to update reminder')
        }
      } else {
        const result = await createReminder(authStore.accessToken, reminderData)
        if (result.status === 'success') {
          toast.success(result.message)
          await loadReminders()
          resetReminderForm()
        } else {
          toast.error(result.message || 'Failed to create reminder')
        }
      }
    } catch (err) {
      console.error('Failed to save reminder:', err)
      toast.error(err instanceof Error ? err.message : 'Error saving reminder')
    } finally {
      isSavingReminder.value = false
    }
  }

  // Delete reminder
  const showDeleteConfirmPopup = (contactEmail: string) => {
    deleteConfirmEmail.value = contactEmail
    showDeleteConfirm.value = true
  }

  const confirmDelete = async () => {
    const contactEmail = deleteConfirmEmail.value
    showDeleteConfirm.value = false

    try {
      if (!authStore.accessToken) return

      const result = await deleteReminder(authStore.accessToken, contactEmail)
      if (result.status === 'success') {
        toast.success(result.message)
        await loadReminders()
      } else {
        toast.error(result.message || 'Failed to delete reminder')
      }
    } catch (err) {
      console.error('Failed to delete reminder:', err)
      toast.error(err instanceof Error ? err.message : 'Error deleting reminder')
    }
  }

  const cancelDelete = () => {
    showDeleteConfirm.value = false
    deleteConfirmEmail.value = ''
  }

  // Edit reminder
  const editReminder = async (reminder: ReminderConfig) => {
    showReminderForm.value = true

    if (reminder.contactEmail) {
      if (contactsList.value.length === 0) {
        await loadContacts()
      }

      if (!contactsList.value.includes(reminder.contactEmail)) {
        contactsList.value = [reminder.contactEmail, ...contactsList.value]
      }
    }

    newReminder.value = {
      contactEmail: reminder.contactEmail || '',
      enabled: reminder.enabled,
      afterInactive: reminder.afterInactive,
      maxReminders: reminder.maxReminders,
      sentCount: 0,
    }

    editingReminderId.value = reminder.contactEmail

    visibleOptionalFields.value = {
      contactEmail: reminder.contactEmail.trim().length > 0,
      maxReminders: reminder.maxReminders !== null && reminder.maxReminders !== undefined,
      enabled: reminder.enabled !== null && reminder.enabled !== undefined,
    }
  }

  // Reset form
  const resetReminderForm = () => {
    newReminder.value = {
      contactEmail: '',
      enabled: true,
      afterInactive: 60,
      maxReminders: 5,
      sentCount: 0,
    }
    editingReminderId.value = null
    showReminderForm.value = false
    visibleOptionalFields.value = {
      contactEmail: false,
      maxReminders: false,
      enabled: false,
    }
  }

  const toggleReminderForm = () => {
    if (showReminderForm.value) {
      resetReminderForm()
    } else {
      resetReminderForm()
      loadContacts()
      showReminderForm.value = true
    }
  }

  return {
    reminders,
    contactsList,
    isLoadingContacts,
    newReminder,
    editingReminderId,
    isSavingReminder,
    showReminderForm,
    showDeleteConfirm,
    deleteConfirmEmail,
    visibleOptionalFields,
    loadReminders,
    loadContacts,
    saveReminder,
    showDeleteConfirmPopup,
    confirmDelete,
    cancelDelete,
    editReminder,
    resetReminderForm,
    toggleReminderForm,
  }
}



