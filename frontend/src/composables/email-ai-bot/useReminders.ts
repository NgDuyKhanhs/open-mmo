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
import { getContactsList } from '@/services/gmailService'

export function useReminders() {
  const authStore = useAuthStore()

  // Types - rename UI config to avoid conflict with service type
  type UiReminderConfig = {
    contactEmail: string
    enabled: boolean
    afterInactive: number
    repeatEvery: number
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
    repeatEvery: 1440,
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
    repeatEvery: boolean
    enabled: boolean
  }>({
    contactEmail: false,
    maxReminders: false,
    repeatEvery: false,
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
      if (!authStore.accessToken) {
        console.warn('No access token available')
        return
      }

      // Skip if already loading
      if (isLoadingContacts.value) {
        console.debug('Already loading contacts, skipping...')
        return
      }

      // Only skip if we have data
      if (contactsList.value.length > 0) {
        console.debug('Contacts already loaded, skipping API call')
        return
      }

      console.log('Loading contacts from API...')
      isLoadingContacts.value = true

      const contacts = await getContactsList(authStore.accessToken)
      console.log('Contacts loaded:', contacts.length, 'items')
      contactsList.value = contacts || []
    } catch (err) {
      console.error('Failed to load contacts:', err)
      toast.error('Failed to load contacts')
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

     if (visibleOptionalFields.value.repeatEvery && (!newReminder.value.repeatEvery || newReminder.value.repeatEvery < 1)) {
       toast.error('Repeat every must be at least 1 minute')
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
         repeatEvery: visibleOptionalFields.value.repeatEvery ? newReminder.value.repeatEvery : 1440,
         maxReminders: visibleOptionalFields.value.maxReminders ? newReminder.value.maxReminders : 5,
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
       repeatEvery: reminder.repeatEvery || 1440,
       maxReminders: reminder.maxReminders,
       sentCount: 0,
     }

     editingReminderId.value = reminder.contactEmail

     // Simple checks for checkbox visibility
     const hasContactEmail = !!(reminder.contactEmail && reminder.contactEmail.trim().length > 0)
     const hasMaxReminders = !!(reminder.maxReminders && reminder.maxReminders > 0)
     const hasRepeatEvery = !!(reminder.repeatEvery && reminder.repeatEvery > 0)

     visibleOptionalFields.value = {
       contactEmail: hasContactEmail,
       maxReminders: hasMaxReminders,
       repeatEvery: hasRepeatEvery,
       enabled: reminder.enabled !== null && reminder.enabled !== undefined,
     }
   }

   // Reset form
   const resetReminderForm = () => {
     newReminder.value = {
       contactEmail: '',
       enabled: true,
       afterInactive: 60,
       repeatEvery: 1440,
       maxReminders: 5,
       sentCount: 0,
     }
     editingReminderId.value = null
     showReminderForm.value = false
     visibleOptionalFields.value = {
       contactEmail: false,
       maxReminders: false,
       repeatEvery: false,
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



