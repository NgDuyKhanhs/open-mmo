  <script setup lang="ts">
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/useAuthStore'
  import { computed, onMounted, ref, watch } from 'vue'
  import { toast } from 'vue3-toastify'
  import navbarLogoUrl from '@/assets/navbar-logo.png'
   import { Mail, Settings, RefreshCw, HelpCircle, X, Search, Inbox, CheckCircle2, AlertCircle, Bot, Key, Clock, AlertTriangle, LinkIcon, Send, AlertOctagon, Zap, Gauge, Trash, Edit } from 'lucide-vue-next'
  import {
    getGmailStatus,
    startGmailConnection,
    enableBot,
    disableBot,
    updateBotConfig,
    updateCustomPrompt,
    getMailboxPage,
    getAiProvider,
    setAiProvider,
    getContactsList,
    getReminders,
    createReminder,
    updateReminder,
    deleteReminder,
    type GmailStatus,
    type Email,
    type MailboxPageResponse,
    type ReminderConfig,
    type ReminderResponse,
  } from '@/services/gmailService'

  const router = useRouter()
  const authStore = useAuthStore()

  // Redirect to login if not authenticated
  onMounted(() => {
    authStore.initializeAuth()
    if (!authStore.isLoggedIn) {
      console.warn('⚠️  User not logged in, redirecting to login')
      router.push('/login')
      return
    }

    // Check if OAuth callback was successful
    const connected = router.currentRoute.value.query.connected
    const error = router.currentRoute.value.query.error

    if (connected === '1') {
      toast.success('Gmail connected successfully!')
      // Clear query params
      router.replace('/email-ai-bot')
    } else if (error) {
      console.error('❌ Gmail OAuth failed:', error)
      toast.error(`Failed to connect Gmail: ${error}`)
      // Clear query params
      router.replace('/email-ai-bot')
    }

    loadStatus()
  })

  // State management
  const status = ref<GmailStatus>({
    connected: false,
    gmailAddress: '',
    botEnabled: false,
    triggerSubject: 'openmmo',
    lastRunAt: null,
    lastError: null,
  })

   const emails = ref<Email[]>([])
   const emailsCache = ref<Map<string, Email[]>>(new Map())
   const selectedBox = ref<'inbox' | 'sent' | 'spam' | 'trash'>('inbox')
   const selectedEmail = ref<Email | null>(null)
   const isLoading = ref(false)
    const isSaving = ref(false)
    const showModal = ref(false)
      const configSubject = ref('')
      const configPrompt = ref('')
      const showPromptHelp = ref(false)
      const activeTab = ref<'mailbox' | 'config' | 'status' | 'reminders' | 'connection'>('mailbox')
      const selectedAiProvider = ref<'gemini' | 'groq'>('groq')

      // Track original config values to detect changes
      const originalConfigSubject = ref('')
      const originalConfigPrompt = ref('')

      // Navbar toggle state
      const navbarExpanded = ref(true)

      // 🆕 Reminder Configuration State
      interface ReminderConfig {
        contactEmail: string
        enabled: boolean
        afterInactive: number // minutes
        maxReminders: number
      }

        const reminders = ref<ReminderConfig[]>([])
        const contactsList = ref<string[]>([])  // List of contact emails
        const isLoadingContacts = ref(false)
        const newReminder = ref<ReminderConfig>({
          contactEmail: '',
          enabled: true,
          afterInactive: 60,
          maxReminders: 5,
        })
       const editingReminderId = ref<string | null>(null)
       const isSavingReminder = ref(false)
       const showReminderForm = ref(false)

       // 🆕 Delete confirmation popup state
       const showDeleteConfirm = ref(false)
       const deleteConfirmEmail = ref<string>('')

        // 🆕 Optional fields visibility
         const visibleOptionalFields = ref<{
           contactEmail: boolean
           maxReminders: boolean
           enabled: boolean
         }>({
           contactEmail: false,  // Default checked để user thấy là for specific contact
           maxReminders: false,
           enabled: false,
         })

      // 🆕 Pagination state
    const currentPageToken = ref<string | null>(null)
    interface PageCache {
      emails: Email[]
      nextPageToken: string | null
    }
    const pageCache = ref<PageCache[]>([]) // Cache pages: [page1, page2, page3...]
    const currentPageIndex = ref(0)
    const hasNextPage = ref(false)

  // Load custom prompt when switching to config tab
  const switchToConfigTab = async () => {
    activeTab.value = 'config'
    await loadCustomPrompt()
  }

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
         // Track original prompt value
         originalConfigPrompt.value = data.customPrompt || ''
       }
     } catch (err) {
       console.error('Failed to load custom prompt:', err)
     }
   }

  // Format date utility
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

  // Load status from backend
  const loadStatus = async () => {
    try {
      if (!authStore.accessToken) {
        toast.error('Authentication required')
        return
      }

       status.value = await getGmailStatus(authStore.accessToken)
       configSubject.value = status.value.triggerSubject
       // Track original values to detect changes
       originalConfigSubject.value = status.value.triggerSubject
     } catch (err) {
       console.error('Failed to load status:', err)
       toast.error(err instanceof Error ? err.message : 'Failed to load status')
     }
   }

    // Load emails from selected mailbox with pagination
    const loadEmails = async () => {
      isLoading.value = true
      try {
        if (!authStore.accessToken) {
          toast.error('Bạn cần đăng nhập để xem emails')
          router.push('/login')
          return
        }

        // Use pagination API (10 emails per page)
        const response = await getMailboxPage(
          authStore.accessToken,
          selectedBox.value.toLowerCase(),
          10,
          undefined // Always start from first page (no token)
        )

        emails.value = response.emails
        currentPageToken.value = response.nextPageToken || null

        // Initialize page cache for first page
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

    // Load previous page - use cached data
    const loadPreviousPage = async () => {
      if (currentPageIndex.value <= 0) return

      isLoading.value = true
      try {
        // Move back one position
        currentPageIndex.value--
        const cachedPage = pageCache.value[currentPageIndex.value]

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

    // Load next page
    const loadNextPage = async () => {
      if (!hasNextPage.value || isLoading.value || !authStore.accessToken) return

      isLoading.value = true
      try {
        // Get next page from API using current token
        const response = await getMailboxPage(
          authStore.accessToken,
          selectedBox.value.toLowerCase(),
          10,
          currentPageToken.value || undefined
        )

        emails.value = response.emails
        currentPageToken.value = response.nextPageToken || null

        // Cache this page
        currentPageIndex.value++
        pageCache.value.push({
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

  // Connect Gmail
  const connectGmail = async () => {
    try {
      if (!authStore.accessToken) {
        toast.error('Authentication required. Please login first.')
        router.push('/login')
        return
      }
      const response = await startGmailConnection(authStore.accessToken)
      window.location.href = response.url
    } catch (err) {
      console.error('Failed to connect Gmail:', err)
      toast.error(err instanceof Error ? err.message : 'Error connecting to Gmail')
    }
  }

  // Toggle bot enable/disable
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

   // Update bot configuration
   const updateConfig = async () => {
     isSaving.value = true
     try {
       if (configSubject.value !== status.value.triggerSubject) {
         await updateBotConfig(authStore.accessToken, {
           triggerSubject: configSubject.value,
         })
       }

       if (configPrompt.value !== '') {
         await updateCustomPrompt(authStore.accessToken, configPrompt.value)
       }

       await loadStatus()
       // Reload emails to reflect new triggerSubject filter immediately
       // This ensures emails list updates when switching from config tab to mail tab
       await loadEmails()

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

     // Change AI provider
     const changeAiProvider = async (provider: 'gemini' | 'groq') => {
       try {
         selectedAiProvider.value = provider

         // Save to localStorage for persistence
         localStorage.setItem('selectedAiProvider', provider)

         // Send to backend
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

       // 🆕 Load reminders for current user
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

       // 🆕 Load contacts list
       const loadContacts = async () => {
         try {
           if (!authStore.accessToken) return
           isLoadingContacts.value = true
           const contacts = await getContactsList(authStore.accessToken)
           contactsList.value = contacts || []
         } catch (err) {
           console.error('Failed to load contacts:', err)
           toast.error('Failed to load contacts')
         } finally {
           isLoadingContacts.value = false
         }
       }

         // 🆕 Save or update reminder
           const saveReminder = async () => {
           // Validation: afterInactive is always required
           if (!newReminder.value.afterInactive || newReminder.value.afterInactive < 5) {
             toast.error('Remind after inactivity must be at least 5 minutes')
             return
           }

           // Validation: maxReminders must be set if checkbox is checked
           if (visibleOptionalFields.value.maxReminders && (!newReminder.value.maxReminders || newReminder.value.maxReminders < 1)) {
             toast.error('Max reminders must be at least 1')
             return
           }

           // Validation: if contactEmail field is visible, it must not be empty
           if (visibleOptionalFields.value.contactEmail && !newReminder.value.contactEmail.trim()) {
             toast.error('Please enter a contact email or uncheck "Apply to Specific Contact"')
             return
           }

          isSavingReminder.value = true
          try {
            if (!authStore.accessToken) return

              // Build reminder object - send exactly what user configured
              const reminderData: ReminderConfig = {
                 contactEmail: visibleOptionalFields.value.contactEmail ? newReminder.value.contactEmail : '',
                 enabled: visibleOptionalFields.value.enabled ? newReminder.value.enabled : true,
                 afterInactive: newReminder.value.afterInactive,
                 maxReminders: visibleOptionalFields.value.maxReminders ? newReminder.value.maxReminders : 5,
               }

           if (editingReminderId.value) {
             // Update existing - use original contactEmail as the path parameter
             const result = await updateReminder(authStore.accessToken, editingReminderId.value, reminderData)
             if (result.status === 'success') {
               toast.success(result.message)
               await loadReminders()
               resetReminderForm()
             } else {
               toast.error(result.message || 'Failed to update reminder')
             }
           } else {
             // Create new
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

       // 🆕 Delete reminder - show popup
       const showDeleteConfirmPopup = (contactEmail: string) => {
         deleteConfirmEmail.value = contactEmail
         showDeleteConfirm.value = true
       }

       // 🆕 Confirm delete
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

       // 🆕 Cancel delete
       const cancelDelete = () => {
         showDeleteConfirm.value = false
         deleteConfirmEmail.value = ''
       }

        // 🆕 Edit reminder
        const editReminder = async (reminder: ReminderConfig) => {
          showReminderForm.value = true

          // Ensure the selected contact can be rendered in the dropdown when editing
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
            }

            editingReminderId.value = reminder.contactEmail

            // Update optional fields visibility based on reminder data
            visibleOptionalFields.value = {
              contactEmail: reminder.contactEmail.trim().length > 0, // Show if has specific contact
              maxReminders: reminder.maxReminders !== null && reminder.maxReminders !== undefined,
              enabled: reminder.enabled !== null && reminder.enabled !== undefined,
            }
        }

       // 🆕 Reset reminder form
        const resetReminderForm = () => {
          newReminder.value = {
            contactEmail: '',
            enabled: true,
            afterInactive: 60,
            maxReminders: 5,
          }
          editingReminderId.value = null
          showReminderForm.value = false
          // Reset optional fields visibility
          visibleOptionalFields.value = {
            contactEmail: false,
            maxReminders: false,
            enabled: false,
          }
       }

     // 🆕 Toggle reminder form
     const toggleReminderForm = () => {
       if (showReminderForm.value) {
         // Closing form
         resetReminderForm()
       } else {
         // Opening form - load contacts list
         resetReminderForm()
         loadContacts()
         showReminderForm.value = true
       }
     }

     // Select mailbox and load emails
     const selectBox = async (box: typeof selectedBox.value) => {
       // Prevent switching if emails are loading
       if (isLoading.value) {
         toast.warning('⏳ Please wait for emails to finish loading')
         return
       }
       selectedBox.value = box
       selectedEmail.value = null
       await loadEmails()
     }

      // Watch for changes in contactEmail visibility to load contacts
      watch(
        () => visibleOptionalFields.value.contactEmail,
        async (isVisible) => {
          if (isVisible) {
            // Always load contacts when checkbox is checked, or if list is empty
            if (contactsList.value.length === 0 && !isLoadingContacts.value) {
              await loadContacts()
            }
          }
        }
      )

    // Select email and show preview
    const selectEmail = (email: Email) => {
      selectedEmail.value = email
      showModal.value = true
    }

  // Close modal
  const closeModal = () => {
    showModal.value = false
    selectedEmail.value = null
  }

  // Check for OAuth callback
  const checkOAuthCallback = async () => {
    const params = new URLSearchParams(window.location.search)
    if (params.has('connected')) {
      await loadStatus()
      window.history.replaceState({}, document.title, window.location.pathname)
    }
  }

   // On mount
   onMounted(async () => {
     authStore.initializeAuth()

     // Restore AI provider from localStorage first
     const savedProvider = localStorage.getItem('selectedAiProvider')
     if (savedProvider === 'gemini' || savedProvider === 'groq') {
       selectedAiProvider.value = savedProvider
     }

     // Also try to load from backend
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

     await checkOAuthCallback()
     await loadStatus()
     if (status.value.connected) {
       await loadEmails()
     }
   })

  // Computed properties
  const statusMessage = computed(() => {
    if (!status.value.connected) return 'Not connected'
    return status.value.botEnabled ? 'Active' : 'Inactive'
  })

  const statusColor = computed(() => {
    if (!status.value.connected) return 'neutral'
    return status.value.botEnabled ? 'success' : 'warning'
  })

  // Filter emails by trigger subject keyword
  const filteredEmails = computed(() => {
    if (!status.value.triggerSubject || !status.value.triggerSubject.trim()) {
      return emails.value
    }
    const keyword = status.value.triggerSubject.toLowerCase()
    return emails.value.filter(email => email.subject.toLowerCase().includes(keyword))
  })

  // Check if config has changed (to enable/disable Save button)
  const hasConfigChanged = computed(() => {
    const subjectChanged = configSubject.value !== originalConfigSubject.value
    const promptChanged = configPrompt.value !== originalConfigPrompt.value
    return subjectChanged || promptChanged
  })

  // Extract email address from "Name <email@domain.com>" format
  const extractEmail = (emailString: string): string => {
    if (!emailString) return ''
    const match = emailString.match(/<(.+?)>/)
    return match ? match[1] : emailString
  }
</script>

<template>
  <div class="email-bot-page">
    <div class="page-background">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="page-shell">

      <div class="page-container">
        <!-- Not Connected Screen -->
        <div v-if="!status.connected" class="not-connected-screen">
          <div class="not-connected-card">
            <div class="not-connected-logo">
              <img :src="navbarLogoUrl" alt="OpenMMO Logo" class="logo-image" />
            </div>
            <div class="not-connected-content">
              <h2 class="not-connected-title">Connect Your Gmail Account</h2>
              <p class="not-connected-description">
                Enable AI-powered email automation to automatically respond to incoming emails based on your custom settings.
              </p>
              <div class="not-connected-features">
                <div class="feature-item">
                  <span class="feature-icon">✓</span>
                  <span class="feature-text">Automatic email replies</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">✓</span>
                  <span class="feature-text">Custom AI prompts</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">✓</span>
                  <span class="feature-text">Keyword-based filtering</span>
                </div>
                <div class="feature-item">
                  <span class="feature-icon">✓</span>
                  <span class="feature-text">Message management</span>
                </div>
              </div>
              <button @click="connectGmail" class="btn-connect-gmail" :disabled="isLoading">
                <span v-if="!isLoading">Connect Gmail Account</span>
                <span v-else>Connecting...</span>
              </button>
              <p class="not-connected-info">
                We only access your emails to send automated replies. Your data is encrypted and secure.
              </p>
            </div>
          </div>
        </div>

        <!-- Main Content Area -->
        <main v-if="status.connected" class="main-content">
               <!-- Tab Navigation -->
               <div class="tab-header" :class="{ collapsed: !navbarExpanded }">
                    <!-- Toggle Button -->
                    <button
                      @click="navbarExpanded = !navbarExpanded"
                      class="navbar-toggle-btn"
                      :title="navbarExpanded ? 'Collapse sidebar' : 'Expand sidebar'"
                    >
                      <!-- Expand Icon -->
                      <svg v-if="navbarExpanded" viewBox="0 0 24 24" class="toggle-icon" fill="none" stroke="currentColor" stroke-width="1.5">
                        <rect x="3" y="3" width="18" height="18" rx="2"></rect>
                        <path d="M9 3v18" stroke-linecap="round"></path>
                        <path d="M16 10l-3 2 3 2" stroke-linecap="round" stroke-linejoin="round"></path>
                      </svg>
                      <!-- Collapse Icon -->
                      <svg v-else viewBox="0 0 24 24" class="toggle-icon" fill="none" stroke="currentColor" stroke-width="1.5">
                        <rect x="3" y="3" width="18" height="18" rx="2"></rect>
                        <path d="M9 3v18" stroke-linecap="round"></path>
                        <path d="M14 10l3 2-3 2" stroke-linecap="round" stroke-linejoin="round"></path>
                      </svg>
                    </button>

                   <button
                     @click="activeTab = 'mailbox'"
                     :class="['tab-button', { active: activeTab === 'mailbox' }]"
                     title="View mailbox"
                   >
                     <Mail :size="18" class="tab-icon" />
                     <span v-if="navbarExpanded" class="tab-label">Mailbox</span>
                   </button>
                    <button
                      @click="switchToConfigTab"
                      :class="['tab-button', { active: activeTab === 'config' }]"
                      title="Bot settings"
                    >
                      <Settings :size="18" class="tab-icon" />
                      <span v-if="navbarExpanded" class="tab-label">Config</span>
                    </button>
                     <button
                       @click="activeTab = 'status'"
                       :class="['tab-button', { active: activeTab === 'status' }]"
                       title="Bot status"
                     >
                       <Bot :size="18" class="tab-icon" />
                       <span v-if="navbarExpanded" class="tab-label">Status</span>
                     </button>
                     <button
                       @click="activeTab = 'reminders'; loadReminders()"
                       :class="['tab-button', { active: activeTab === 'reminders' }]"
                       title="Email reminders"
                     >
                       <Clock :size="18" class="tab-icon" />
                       <span v-if="navbarExpanded" class="tab-label">Reminders</span>
                     </button>
                  </div>

            <!-- Tab Content -->
            <div class="tab-content">
              <!-- Mailbox Tab -->
               <div v-show="activeTab === 'mailbox'" class="mailbox-section tab-pane">
                  <div class="tab-title-section">
                    <h3 class="tab-title">Mailbox</h3>
                    <div class="tab-divider"></div>
                  </div>
                  <div class="mailbox-header">
                  <div class="mailbox-selector">
                    <button
                      @click="selectBox('inbox')"
                      :class="['mailbox-tab-btn', { active: selectedBox === 'inbox' }]"
                      :title="'View inbox emails'"
                      :disabled="isLoading"
                    >
                      <Inbox :size="18" class="mailbox-icon" />
                      <span class="mailbox-btn-label">Inbox</span>
                      <div class="mailbox-indicator" v-if="selectedBox === 'inbox'"></div>
                    </button>

                    <button
                      @click="selectBox('sent')"
                      :class="['mailbox-tab-btn', { active: selectedBox === 'sent' }]"
                      :title="'View sent emails'"
                      :disabled="isLoading"
                    >
                      <Send :size="18" class="mailbox-icon" />
                      <span class="mailbox-btn-label">Sent</span>
                      <div class="mailbox-indicator" v-if="selectedBox === 'sent'"></div>
                    </button>

                    <button
                      @click="selectBox('spam')"
                      :class="['mailbox-tab-btn', { active: selectedBox === 'spam' }]"
                      :title="'View spam emails'"
                      :disabled="isLoading"
                    >
                      <AlertOctagon :size="18" class="mailbox-icon" />
                      <span class="mailbox-btn-label">Spam</span>
                      <div class="mailbox-indicator" v-if="selectedBox === 'spam'"></div>
                    </button>
                  </div>
                 </div>

                <div class="email-list">
                    <div v-if="isLoading" class="loading-state">
                      <div class="spinner"></div>
                      <span>Loading emails...</span>
                    </div>
                    <div v-else-if="emails.length === 0" class="empty-state">
                      <Inbox :size="32" class="empty-icon" />
                      <span>No emails in {{ selectedBox }}</span>
                    </div>
                    <div v-else-if="filteredEmails.length === 0" class="empty-state">
                      <Search :size="32" class="empty-icon" />
                      <span>No emails matching "{{ status.triggerSubject }}"</span>
                    </div>
                    <div v-else class="email-items">
                      <div
                        v-for="email in filteredEmails"
                        :key="email.id"
                        @click="selectEmail(email)"
                        class="email-item"
                      >
                        <div v-if="email.aiprovider" class="email-provider">
                          {{ email.aiprovider.charAt(0).toUpperCase() + email.aiprovider.slice(1) }}
                        </div>
                        <div class="email-from">
                          <Mail size="14" />
                          {{ extractEmail(email.from) }}
                        </div>
                        <div class="email-subject">{{ email.subject }}</div>
                        <div class="email-snippet">{{ email.snippet }}</div>
                        <div class="email-date">{{ formatDate(email.date) }}</div>
                      </div>
                    </div>
                  </div>

                  <!-- Pagination Controls -->
                  <div v-if="currentPageIndex > 0 || hasNextPage" class="pagination-controls">
                    <button
                      @click="loadPreviousPage"
                      class="pagination-btn pagination-btn-prev"
                      :disabled="isLoading || currentPageIndex <= 0"
                      title="Previous page"
                    >
                      ← Previous
                    </button>
                    <div class="pagination-spacer"></div>
                    <button
                      @click="loadNextPage"
                      class="pagination-btn pagination-btn-next"
                      :disabled="isLoading || !hasNextPage"
                      title="Next page"
                    >
                      Next →
                    </button>
                  </div>
                </div>

              <!-- Settings Tab -->
              <div v-show="activeTab === 'config'" class="settings-section tab-pane">
                <!-- Tab Title -->
                <div class="tab-title-section">
                  <h3 class="tab-title">Configuration</h3>
                  <div class="tab-divider"></div>
                </div>

               <div class="config-form">
                <!-- Trigger Subject -->
                <div class="form-section">
                  <label class="form-label">Subject Keyword <span class="optional-badge">(Optional)</span></label>
                  <p class="form-description">
                    Leave empty to reply to ALL emails, or enter a keyword to filter by subject.
                    <strong>NEW:</strong> Automatic rules will skip attachments, promotional emails, and system notifications to save quota.
                  </p>
                  <input
                    v-model="configSubject"
                    type="text"
                    placeholder="e.g., youtube, shopify, contact (or leave empty for all emails)"
                    class="form-input"
                  />
                  <div v-if="!configSubject.trim()" class="info-box">
                    ⚠️ <strong>Reply to ALL mode:</strong> Bot will reply to all emails but skip auto-generated, mailing lists, promotional, and low-quality emails (13 skip rules).
                  </div>
                </div>

                <div class="form-divider"></div>

                <!-- Custom Prompt -->
                <div class="form-section">
                  <div class="label-row">
                    <label class="form-label">Custom Prompt (Optional)</label>
                    <button @click="showPromptHelp = true" class="help-button" title="View examples">
                      <HelpCircle size="16" />
                    </button>
                  </div>
                  <p class="form-description">
                    Customize how AI responds
                  </p>
                  <textarea
                    v-model="configPrompt"
                    placeholder="Write a custom prompt for AI responses..."
                    class="form-textarea"
                  ></textarea>
                </div>

                <!-- Save Button -->
                <div class="form-actions">
                  <button
                    @click="updateConfig"
                    class="btn btn-primary btn-full"
                    :disabled="isSaving || !hasConfigChanged"
                  >
                    {{ isSaving ? 'Saving...' : 'Save Configuration' }}
                  </button>
                 </div>
               </div>
             </div>

                 <!-- Status Tab - Bot Status & AI Provider -->
                 <div v-show="activeTab === 'status'" class="status-section tab-pane">
                   <!-- Tab Title -->
                   <div class="tab-title-section">
                     <h3 class="tab-title">Status & AI Model</h3>
                     <div class="tab-divider"></div>
                   </div>

                   <div class="status-content">

                    <!-- Status Grid -->
                    <div class="status-grid-modern">
                      <!-- Bot Status -->
                      <div v-if="status.connected" class="modern-card bot-card">
                        <div class="card-header">
                          <Bot :size="18" class="card-icon" />
                          <span class="card-title">Bot Status</span>
                        </div>
                        <div class="card-content">
                          <div class="bot-toggle-large">
                            <label class="toggle-switch-large">
                              <input
                                type="checkbox"
                                :checked="status.botEnabled"
                                @change="toggleBot"
                                :disabled="isSaving"
                              />
                              <span class="toggle-slider-large"></span>
                            </label>
                            <div class="toggle-info">
                              <span :class="['status-badge-small', status.botEnabled ? 'active' : 'inactive']">
                                {{ status.botEnabled ? '● Running' : '● Paused' }}
                              </span>
                              <span class="toggle-description">
                                {{ status.botEnabled ? 'Auto-replies are enabled' : 'Auto-replies are paused' }}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                      <!-- Last Run Info -->
                      <div v-if="status.lastRunAt" class="modern-card run-card">
                        <div class="card-header">
                          <Clock :size="18" class="card-icon" />
                          <span class="card-title">Last Run</span>
                        </div>
                        <div class="card-content">
                          <p class="run-time">{{ formatDate(status.lastRunAt) }}</p>
                          <p class="run-hint">Last time the bot processed emails</p>
                        </div>
                      </div>

                      <!-- Error Info -->
                      <div v-if="status.lastError" class="modern-card error-card">
                        <div class="card-header error-header">
                          <AlertTriangle :size="18" class="card-icon" />
                          <span class="card-title">Last Error</span>
                        </div>
                        <div class="card-content">
                          <p class="error-message">{{ status.lastError }}</p>
                        </div>
                      </div>
                    </div>

                    <!-- AI Provider Selection Section -->
                    <div v-if="status.connected" class="ai-provider-section">
                      <div class="provider-header">
                        <h4 class="provider-title">AI Model Selection</h4>
                        <p class="provider-description">Choose which AI model to use for auto-replies</p>
                      </div>

                      <div class="provider-grid">
                        <!-- Groq Option -->
                        <div
                          @click="changeAiProvider('groq')"
                          :class="['provider-card', { selected: selectedAiProvider === 'groq' }]"
                        >
                          <div class="provider-card-header">
                            <Zap :size="24" class="provider-icon groq-icon" />
                            <span class="provider-name">Groq</span>
                          </div>
                          <div class="provider-details">
                            <p class="provider-feature">⚡ Fast & Cheap</p>
                            <p class="provider-feature">30 requests/min</p>
                            <p class="provider-feature">Recommended</p>
                          </div>
                          <div v-if="selectedAiProvider === 'groq'" class="provider-checkmark">✓</div>
                        </div>

                        <!-- Gemini Option -->
                        <div
                          @click="changeAiProvider('gemini')"
                          :class="['provider-card', { selected: selectedAiProvider === 'gemini' }]"
                        >
                          <div class="provider-card-header">
                            <Gauge :size="24" class="provider-icon gemini-icon" />
                            <span class="provider-name">Gemini</span>
                          </div>
                          <div class="provider-details">
                            <p class="provider-feature">🔬 Advanced</p>
                            <p class="provider-feature">High Quality</p>
                            <p class="provider-feature">Premium Model</p>
                          </div>
                          <div v-if="selectedAiProvider === 'gemini'" class="provider-checkmark">✓</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                  <!-- Connection Tab - Account & AI Provider Selection -->
                  <div v-show="activeTab === 'connection'" class="connection-section tab-pane" style="display: none;">
                  </div>

                   <!-- Reminders Tab -->
                   <div v-show="activeTab === 'reminders'" class="reminders-section tab-pane">
                     <!-- Tab Title -->
                     <div class="tab-title-section">
                       <h3 class="tab-title">Reminders</h3>
                       <div class="tab-divider"></div>
                     </div>

                     <!-- Reminders Content -->
                     <div class="reminders-content">
                       <!-- New Reminder Section -->
                       <div class="reminder-form-container">
                         <div class="new-reminder-header">
                           <h4 class="new-reminder-title">New Reminder</h4>
                           <div class="form-actions" style="margin-top: 0;">
                             <button @click="saveReminder" class="btn btn-small" :disabled="isSavingReminder">
                               {{ isSavingReminder ? 'Saving...' : 'Save' }}
                             </button>
                             <button @click="toggleReminderForm" class="btn btn-secondary btn-small">Cancel</button>
                             <button class="btn-help" title="Help">?</button>
                           </div>
                         </div>

                         <!-- Main Input -->
                         <div class="form-section">
                           <label class="form-label">
                             Remind after inactivity (minutes) <span class="required">*</span>
                             <span class="info-icon" title="Remind user after this period of inactivity">(1)</span>
                           </label>
                           <input
                             v-model.number="newReminder.afterInactive"
                             type="number"
                             min="5"
                             placeholder="e.g., 60"
                             class="form-input"
                           />
                         </div>

                         <!-- Optional Settings -->
                         <div class="optional-settings">
                           <p class="optional-label">Optional settings</p>
                           <p class="optional-description">Select which fields to use</p>

                           <div class="checkbox-group">
                             <label class="checkbox-item">
                               <input
                                 v-model="visibleOptionalFields.contactEmail"
                                 type="checkbox"
                               />
                               <span class="checkbox-text">Apply to specific contact</span>
                               <span class="info-icon" title="Apply reminder to specific contact">(1)</span>
                             </label>
                              <div v-if="visibleOptionalFields.contactEmail" class="conditional-field">
                                <select v-model="newReminder.contactEmail" class="form-input form-select">
                                  <option value="">Select contact...</option>
                                  <option v-for="contact in contactsList" :key="contact" :value="contact">
                                    {{ contact }}
                                  </option>
                                </select>
                              </div>
                           </div>

                           <div class="checkbox-group">
                             <label class="checkbox-item">
                               <input
                                 v-model="visibleOptionalFields.maxReminders"
                                 type="checkbox"
                               />
                               <span class="checkbox-text">Max reminders limit</span>
                             </label>
                             <div v-if="visibleOptionalFields.maxReminders" class="conditional-field">
                               <input
                                 v-model.number="newReminder.maxReminders"
                                 type="number"
                                 min="1"
                                 placeholder="e.g., 5"
                                 class="form-input"
                               />
                             </div>
                            </div>

                            <div class="checkbox-group">
                              <label class="checkbox-item">
                                <input
                                  v-model="visibleOptionalFields.enabled"
                                  type="checkbox"
                                />
                                <span class="checkbox-text">Enable/Disable status</span>
                              </label>
                              <div v-if="visibleOptionalFields.enabled" class="conditional-field">
                                <label class="toggle-switch">
                                  <input
                                    v-model="newReminder.enabled"
                                    type="checkbox"
                                  />
                                  <span class="toggle-slider"></span>
                                  <span class="toggle-label">{{ newReminder.enabled ? 'Enabled' : 'Disabled' }}</span>
                                </label>
                              </div>
                            </div>
                         </div>
                       </div>

                       <!-- Configured Reminders Section -->
                       <div class="configured-reminders">
                         <div class="reminders-section-header">
                           <h4 class="reminders-section-title">Configured Reminders</h4>
                           <span class="reminders-count">({{ reminders.length }})</span>
                         </div>

                         <div v-if="reminders.length > 0" class="reminders-table-container">
                            <table class="reminders-table">
                              <thead>
                                <tr>
                                  <th>EMAIL</th>
                                  <th>STATUS</th>
                                  <th>SCHEDULE</th>
                                  <th>MAX REMINDERS</th>
                                  <th>ACTIONS</th>
                                </tr>
                              </thead>
                              <tbody>
                                <tr v-for="reminder in reminders" :key="reminder.contactEmail">
                                  <td class="email-cell">{{ reminder.contactEmail || 'All Emails' }}</td>
                                  <td class="status-cell">
                                    <span :class="['status-badge', reminder.enabled ? 'active' : 'inactive']">
                                      {{ reminder.enabled ? '● Active' : '● Inactive' }}
                                    </span>
                                  </td>
                                  <td class="schedule-cell">Every {{ reminder.afterInactive }}m</td>
                                  <td class="max-cell">{{ reminder.maxReminders }}</td>
                                   <td class="actions-cell">
                                    <button
                                      @click="editReminder(reminder)"
                                      class="action-btn edit-btn"
                                      title="Edit"
                                    >
                                      <Edit :size="16" />
                                    </button>
                                    <button
                                      @click="showDeleteConfirmPopup(reminder.contactEmail)"
                                      class="action-btn delete-btn"
                                      title="Delete"
                                    >
                                      <Trash :size="16" />
                                    </button>
                                  </td>
                               </tr>
                             </tbody>
                           </table>
                         </div>
                         <div v-else class="no-reminders">
                           <p>No reminders configured yet</p>
                         </div>
                       </div>
                     </div>
                   </div>
             </div>
           </main>
         </div>

    <!-- Email Preview Modal -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>Email Preview</h3>
          <span @click="closeModal" class="close-btn">x</span>
        </div>
        <div class="modal-content" v-if="selectedEmail">
          <div class="email-preview">
            <div class="preview-field">
              <p class="label">From</p>
              <p class="value">{{ selectedEmail.from }}</p>
            </div>
            <div class="preview-field">
              <p class="label">Subject</p>
              <p class="value">{{ selectedEmail.subject }}</p>
            </div>
            <div class="preview-field">
              <p class="label">Date</p>
              <p class="value">{{ formatDate(selectedEmail.date) }}</p>
            </div>
            <div class="preview-field">
              <p class="label">Message</p>
              <p class="value message">{{ selectedEmail.snippet }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Prompt Help Modal -->
    <div v-if="showPromptHelp" class="modal-overlay" @click="showPromptHelp = false">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>Prompt Examples</h3>
          <span @click="showPromptHelp = false" class="close-btn">x</span>
        </div>
        <div class="modal-content">
          <div class="help-content">
            <div class="example-section">
              <h4>💼 Business Inquiry Example</h4>
              <div class="code-block">Hi there,

Thank you for your interest in {subject}.

Regarding your question: {body}

I'd be happy to help! Feel free to reach out if you need more information.

Best regards</div>
             </div>
           </div>
         </div>
       </div>
     </div>

     <!-- Delete Confirmation Modal -->
     <div v-if="showDeleteConfirm" class="modal-overlay" @click="cancelDelete">
       <div class="modal delete-modal" @click.stop>
         <div class="modal-header">
           <h3>Delete Reminder</h3>
           <span @click="cancelDelete" class="close-btn">x</span>
         </div>
         <div class="modal-content">
           <p class="delete-message">Are you sure you want to delete the reminder for <strong>{{ deleteConfirmEmail }}</strong>?</p>
           <p class="delete-warning">This action cannot be undone.</p>
         </div>
         <div class="modal-footer">
           <button @click="cancelDelete" class="btn btn-secondary">Cancel</button>
           <button @click="confirmDelete" class="btn btn-delete">Delete</button>
         </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  * {
    font-family: Comfortaa, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  }

  .email-bot-page {
    min-height: 100vh;
    padding-top: 60px;
    color: #e8f7ff;
    position: relative;
    overflow: hidden;
  }

  .page-background {
    position: absolute;
    inset: 0;
    z-index: 1;
    pointer-events: none;
  }

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(90px);
    opacity: 0.12;
    animation: float 7s ease-in-out infinite;
  }

  .orb-1 {
    width: 420px;
    height: 420px;
    top: -80px;
    right: -80px;
  }

  .orb-2 {
    width: 320px;
    height: 320px;
    bottom: -120px;
    left: -80px;
    animation-delay: 1.8s;
  }

  .orb-3 {
    width: 360px;
    height: 360px;
    top: 45%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 3s;
  }

  @keyframes float {
    0%, 100% { transform: translate(0, 0); }
    25% { transform: translate(18px, -18px); }
    50% { transform: translate(0, -28px); }
    75% { transform: translate(-18px, -18px); }
  }

  .page-shell {
    position: relative;
    z-index: 2;
    width: 100%;
    margin: 0;
    padding: 0;
  }

  /* ...existing code... */

  /* Main Layout */
  .page-container {
    display: grid;
    grid-template-columns: 1fr;
    gap: 24px;
    width: 100%;
    height: 100%;
  }

  /* Not Connected Screen */
  .not-connected-screen {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: calc(100vh - 60px);
    padding: 24px;
  }

  .not-connected-card {
    background: transparent;
    border: 1px solid rgba(0, 240, 255, 0.16);
    border-radius: 20px;
    backdrop-filter: blur(10px);
    padding: 40px 50px;
    max-width: 900px;
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: center;
    gap: 48px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  }

  .not-connected-logo {
    grid-column: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
 .close-btn {
  cursor: pointer;
  font-size: 18px;
  }
  .logo-image {
    width: 300px;
    height: 300px;
    object-fit: contain;
    filter: drop-shadow(0 8px 24px rgba(0, 240, 255, 0.2));
    animation: float 3s ease-in-out infinite;
  }

  .not-connected-content {
    grid-column: 2;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .not-connected-title {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: #e8f7ff;
    line-height: 1.3;
    text-align: left;
  }

  .not-connected-description {
    margin: 0;
    font-size: 13px;
    color: rgba(232, 247, 255, 0.7);
    line-height: 1.5;
    text-align: left;
  }

  .not-connected-features {
    display: flex;
    flex-direction: column;
    gap: 10px;
    text-align: left;
  }

  .feature-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
    border-radius: 8px;
    background: rgba(0, 240, 255, 0.04);
    border: 1px solid rgba(0, 240, 255, 0.1);
  }

  .feature-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: rgba(0, 200, 100, 0.2);
    color: #00c864;
    font-size: 12px;
    font-weight: 700;
    flex-shrink: 0;
  }

  .feature-text {
    font-size: 12px;
    color: rgba(232, 247, 255, 0.75);
    font-weight: 500;
  }

  .btn-connect-gmail {
    width: 100%;
    padding: 14px 20px;
    border-radius: 12px;
    border: none;
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.2), rgba(0, 200, 100, 0.15));
    color: #00f0ff;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 8px 24px rgba(0, 240, 255, 0.15);
    font-family: inherit;
    border: 1px solid rgba(0, 240, 255, 0.3);
    margin-top: 8px;
  }

  .btn-connect-gmail:not(:disabled):hover {
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.3), rgba(0, 200, 100, 0.25));
    border-color: rgba(0, 240, 255, 0.5);
    box-shadow: 0 12px 32px rgba(0, 240, 255, 0.25);
    transform: translateY(-2px);
  }

  .btn-connect-gmail:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .not-connected-info {
    margin: 0;
    font-size: 11px;
    color: rgba(232, 247, 255, 0.5);
    line-height: 1.5;
    text-align: left;
  }

  .status-badge {
    display: inline-block;
    padding: 8px 12px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
    text-align: center;
    width: 100px;
  }

  .status-badge.active {
    background: rgba(0, 200, 100, 0.15);
    color: #00c864;
    border: 1px solid rgba(0, 200, 100, 0.3);
  }

  .error-card {
    background: rgba(255, 100, 100, 0.1) !important;
    border-color: rgba(255, 100, 100, 0.2) !important;
  }

  .error-message {
    font-size: 12px;
    color: #ff6464;
    line-height: 1.4;
  }

  .toggle-switch-large {
    position: relative;
    display: inline-block;
    width: 48px;
    height: 26px;
  }

  .toggle-switch-large input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .toggle-slider-large {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(255, 255, 255, 0.2);
    transition: 0.3s;
    border-radius: 26px;
  }

  .toggle-slider-large:before {
    position: absolute;
    content: "";
    height: 22px;
    width: 22px;
    left: 2px;
    bottom: 2px;
    background-color: white;
    transition: 0.3s;
    border-radius: 50%;
  }

  input:checked + .toggle-slider-large {
    background-color: #00c864;
  }

  input:checked + .toggle-slider-large:before {
    transform: translateX(22px);
  }

  input:disabled + .toggle-slider-large {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Main Content */
  .main-content {
    background: transparent;
    height: auto;
    min-height: calc(100vh - 60px);
    border-radius: 16px;
    backdrop-filter: blur(10px);
    overflow: hidden;
    display: flex;
    flex-direction: row;
    position: relative;
    padding-top: 10px;
  }

     .tab-header {
       display: flex;
       flex-direction: column;
       gap: 12px;
       border-bottom: none;
       border-right: 1px solid rgba(0, 240, 255, 0.15);
       background: linear-gradient(180deg, rgba(0, 240, 255, 0.02) 0%, rgba(0, 200, 100, 0.01) 100%);
       padding: 20px 14px;
       height: auto;
       align-items: start;
       flex-shrink: 0;
       overflow: hidden;
       /* Smooth width transition for sidebar */
       transition: width 0.3s ease-in-out, padding 0.3s ease-in-out;
       width: 150px;
     }

     .tab-header.collapsed {
       width: 75px;
       padding: 16px 10px;
     }

       .navbar-toggle-btn {
         padding: 10px;
         display: flex;
         align-items: center;
         justify-content: center;
         border: 1px solid rgba(0, 240, 255, 0.15);
         background: linear-gradient(135deg, rgba(0, 240, 255, 0.06), rgba(0, 200, 100, 0.03));
         border-radius: 12px;
         color: rgba(232, 247, 255, 0.7);
         cursor: pointer;
         transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
         margin-bottom: 8px;
         margin-left: auto;
         height: 44px;
         width: 44px;
         font-size: 16px;
         position: relative;
         overflow: hidden;
         z-index: 10;
       }

       /* Animation when navbar is expanded */
       .tab-header:not(.collapsed) .navbar-toggle-btn {
         animation: toggleButtonPulseExpand 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
       }

       /* Animation when navbar is collapsed */
       .tab-header.collapsed .navbar-toggle-btn {
         animation: toggleButtonPulseCollapse 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
         margin-left: auto;
         margin-right: auto;
       }

       .navbar-toggle-btn::before {
         content: '';
         position: absolute;
         inset: 0;
         background: radial-gradient(circle at center, rgba(0, 240, 255, 0.12) 0%, transparent 70%);
         opacity: 0;
         transition: opacity 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
         border-radius: 12px;
       }

       .navbar-toggle-btn:hover {
         border-color: rgba(0, 240, 255, 0.3);
         background: linear-gradient(135deg, rgba(0, 240, 255, 0.12), rgba(0, 200, 100, 0.08));
         color: #00f0ff;
         box-shadow: 0 4px 16px rgba(0, 240, 255, 0.12);
       }

       .navbar-toggle-btn:hover::before {
         opacity: 1;
       }

       .navbar-toggle-btn:active {
         transform: scale(0.93);
       }

       .toggle-icon {
         transition: transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
         display: flex;
         align-items: center;
         justify-content: center;
         width: 20px;
         height: 20px;
         color: inherit;
       }

       .navbar-toggle-btn:hover .toggle-icon {
         transform: scale(1.15);
       }

       .navbar-toggle-btn:active .toggle-icon {
         transform: scale(0.9);
       }

       /* Animation keyframes for toggle button state change */
       @keyframes toggleButtonPulseExpand {
         0% {
           transform: scale(0.95);
           border-color: rgba(0, 240, 255, 0.12);
           background: linear-gradient(135deg, rgba(0, 240, 255, 0.04), rgba(0, 200, 100, 0.02));
           box-shadow: 0 0 0 rgba(0, 240, 255, 0);
         }
         50% {
           transform: scale(1.08);
           border-color: rgba(0, 240, 255, 0.35);
           background: linear-gradient(135deg, rgba(0, 240, 255, 0.14), rgba(0, 200, 100, 0.1));
           box-shadow: 0 0 20px rgba(0, 240, 255, 0.25);
         }
         100% {
           transform: scale(1);
           border-color: rgba(0, 240, 255, 0.2);
           background: linear-gradient(135deg, rgba(0, 240, 255, 0.08), rgba(0, 200, 100, 0.05));
           box-shadow: 0 4px 16px rgba(0, 240, 255, 0.12);
         }
       }

       @keyframes toggleButtonPulseCollapse {
         0% {
           transform: scale(0.95);
           border-color: rgba(0, 240, 255, 0.2);
           background: linear-gradient(135deg, rgba(0, 240, 255, 0.08), rgba(0, 200, 100, 0.05));
           box-shadow: 0 4px 16px rgba(0, 240, 255, 0.12);
         }
         50% {
           transform: scale(1.08);
           border-color: rgba(0, 240, 255, 0.35);
           background: linear-gradient(135deg, rgba(0, 240, 255, 0.14), rgba(0, 200, 100, 0.1));
           box-shadow: 0 0 20px rgba(0, 240, 255, 0.25);
         }
         100% {
           transform: scale(1);
           border-color: rgba(0, 240, 255, 0.12);
           background: linear-gradient(135deg, rgba(0, 240, 255, 0.04), rgba(0, 200, 100, 0.02));
           box-shadow: 0 0 0 rgba(0, 240, 255, 0);
         }
       }

       .tab-button {
         padding: 14px 16px;
         display: flex;
         align-items: center;
         justify-content: center;
         gap: 12px;
         cursor: pointer;
         font-size: 13px;
         font-weight: 600;
         transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
         position: relative;
         overflow: visible;
         white-space: nowrap;
         width: auto;
         min-width: auto;
         height: 44px;
         flex: 0 1 auto;
         letter-spacing: 0.3px;
         border:none;
         background: transparent;
          color: rgba(232, 247, 255, 0.7);

       }

        .tab-label {
          display: inline-block;
          max-width: 100px;
          overflow: hidden;
          opacity: 1;
          /* Slide in from left + fade in */
          transform: translateX(0px);
          transition: opacity 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94), transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
          white-space: nowrap;
          flex-shrink: 0;
        }

        .tab-header.collapsed .tab-label {
          /* Slide out to left + fade out */
          opacity: 0;
          transform: translateX(-25px);
          pointer-events: none;
        }

     .tab-button:disabled {
      opacity: 0.4;
      cursor: not-allowed;
      pointer-events: none;
    }

   .tab-button::before {
     content: '';
     position: absolute;
     inset: 0;
     background: radial-gradient(circle at center, rgba(0, 240, 255, 0.08) 0%, transparent 70%);
     opacity: 0;
     transition: opacity 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     pointer-events: none;
   }

   .tab-button:hover {
        color: #e8f7ff;
     transform: translateY(-2px);
   }

   .tab-button:hover::before {
     opacity: 1;
   }

   .tab-button.active {
     color: #00f0ff;
   }

   .tab-button.active::before {
     opacity: 0;
   }

    .tab-icon {
      font-size: 18px;
      transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      flex-shrink: 0;
    }

    .tab-button:hover .tab-icon {
      transform: scale(1.1);
    }

    .tab-button.active .tab-icon {
      transform: scale(1.15);
    }

   /* Tab Title Section */
    .tab-title-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 20px;
      padding-bottom: 16px;
      padding-top: 16px;
      flex-shrink: 0;
    }

   .tab-title {
     font-size: 20px;
     font-weight: 700;
     background: linear-gradient(135deg, #e8f7ff 0%, #00f0ff 50%, #00d4ff 100%);
     -webkit-background-clip: text;
     -webkit-text-fill-color: transparent;
     background-clip: text;
     margin: 0;
     letter-spacing: 0.5px;
     text-transform: uppercase;
     font-family: 'Comfortaa', sans-serif;
     text-shadow: 0 0 20px rgba(0, 240, 255, 0.1);
   }

   .tab-divider {
     height: 1px;
     background: rgba(0, 240, 255, 0.5);
     opacity: .4;
     border-radius: 1px;
     box-shadow: 0 0 12px rgba(0, 240, 255, 0.2);
     padding: 0;
   }

    .tab-content {
     padding: 20px;
     flex: 1;
     display: flex;
     flex-direction: column;
     overflow: hidden;
     transition: none;
    }

    /* ...existing code... */

    /* Tab Pane Animation */
    .tab-pane {
      animation: fadeInTab 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      display: flex;
      flex-direction: column;
      flex: 1;
      min-height: 0;
      overflow: hidden;
    }

    @keyframes fadeInTab {
      from {
        opacity: 0;
        transform: translateX(8px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    /* Mailbox Section */
    .mailbox-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 24px;
      flex-shrink: 0;
    }

   .mailbox-selector {
     display: flex;
     align-items: center;
     gap: 8px;
     flex: 1;
     padding: 12px;
     border-radius: 12px;
   }

   /* Mailbox Tab Buttons - New Design */
   .mailbox-tab-btn {
     display: flex;
     align-items: center;
     justify-content: center;
     gap: 10px;
     padding: 11px 18px;
     border: 1px solid rgba(0, 240, 255, 0.12);
     background: rgba(0, 240, 255, 0.04);
     color: rgba(232, 247, 255, 0.65);
     cursor: pointer;
     font-size: 13px;
     font-weight: 600;
     border-radius: 11px;
     transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     position: relative;
     flex: 1;
     white-space: nowrap;
     overflow: hidden;
     letter-spacing: 0.3px;
   }

   .mailbox-tab-btn:disabled {
     opacity: 0.4;
     cursor: not-allowed;
     pointer-events: none;
   }

   .mailbox-tab-btn:hover:not(:disabled) {
     border-color: rgba(0, 240, 255, 0.25);
     background: rgba(0, 240, 255, 0.08);
     color: #e8f7ff;
     transform: translateY(-2px);
     box-shadow: 0 4px 12px rgba(0, 240, 255, 0.1);
   }

   .mailbox-tab-btn.active {
     border-color: rgba(0, 240, 255, 0.4);
     background: linear-gradient(135deg, rgba(0, 240, 255, 0.12), rgba(0, 240, 255, 0.06));
     color: #00f0ff;
     box-shadow:
       0 4px 16px rgba(0, 240, 255, 0.15),
       inset 0 1px 0 rgba(0, 240, 255, 0.15);
     animation: mailboxActivePulse 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
   }

   @keyframes mailboxActivePulse {
     0% {
       transform: scale(0.98);
       opacity: 0.9;
     }
     50% {
       transform: scale(1.02);
     }
     100% {
       transform: scale(1);
       opacity: 1;
     }
   }

   .mailbox-icon {
     font-size: 18px;
     display: flex;
     align-items: center;
     justify-content: center;
     transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     flex-shrink: 0;
   }

   .mailbox-tab-btn:hover:not(:disabled) .mailbox-icon {
     transform: scale(1.15) rotate(-8deg);
   }

   .mailbox-tab-btn.active .mailbox-icon {
     transform: scale(1.2) rotate(0deg);
   }

   .mailbox-btn-label {
     font-size: 13px;
     font-weight: 600;
     letter-spacing: 0.3px;
     display: inline-block;
   }

   .mailbox-indicator {
     position: absolute;
     bottom: -1px;
     left: 0;
     right: 0;
     height: 3px;
     background: linear-gradient(90deg, #00f0ff, #00d4ff);
     border-radius: 3px 3px 0 0;
     animation: mailboxIndicatorSlide 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     box-shadow: 0 -2px 8px rgba(0, 240, 255, 0.3);
   }

   @keyframes mailboxIndicatorSlide {
     from {
       height: 0;
       opacity: 0;
       transform: scaleX(0.3);
     }
     to {
       height: 3px;
       opacity: 1;
       transform: scaleX(1);
     }
   }

   .mailbox-icon {
     font-size: 16px;
     display: flex;
     align-items: center;
     justify-content: center;
     transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
   }

      .email-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      overflow-y: auto;
      padding-right: 8px;
      max-height: clamp(250px, calc(100vh - 280px), 500px);
    }

  .email-list::-webkit-scrollbar {
    width: 6px;
  }

  .email-list::-webkit-scrollbar-track {
    background: rgba(0, 240, 255, 0.04);
    border-radius: 3px;
  }

  .email-list::-webkit-scrollbar-thumb {
    background: rgba(0, 240, 255, 0.2);
    border-radius: 3px;
    transition: background 0.2s;
  }

  .email-list::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 240, 255, 0.35);
  }

  .loading-state,
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 60px 20px;
    text-align: center;
    color: rgba(232, 247, 255, 0.5);
    font-size: 14px;
  }

  .empty-icon {
    font-size: 32px;
    opacity: 0.5;
  }

  .spinner {
    width: 20px;
    height: 20px;
    border: 2px solid rgba(0, 240, 255, 0.2);
    border-top-color: #00f0ff;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .email-items {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

    .email-item {
      padding: 22px 14px 12px 14px;
      border-radius: 10px;
      border: 1px solid rgba(0, 240, 255, 0.12);
      background: rgb(175 175 175 / 6%);
      cursor: pointer;
      transition: all 0.25s ease;
      display: grid;
      grid-template-columns: 1fr auto;
      grid-template-rows: auto auto auto;
      gap: 4px 12px;
      position: relative;
      min-height: 60px;
    }

   .email-item:hover {
     border-color: rgba(0, 240, 255, 0.25);
     background: rgba(0, 240, 255, 0.08);
     transform: translateX(4px);
     box-shadow: 0 4px 12px rgba(0, 240, 255, 0.1);
   }

    .email-from {
      font-size: 12px;
      font-weight: 600;
      color: #00f0ff;
      margin: 0;
      display: flex;
      align-items: center;
      gap: 6px;
      grid-column: 1;
      grid-row: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .email-subject {
     font-size: 13px;
     font-weight: 700;
     color: #e8f7ff;
     margin: 0;
     line-height: 1.3;
     display: -webkit-box;
     -webkit-line-clamp: 1;
     -webkit-box-orient: vertical;
     overflow: hidden;
     grid-column: 1;
     grid-row: 2;
   }

   .email-snippet {
     font-size: 11px;
     color: rgba(232, 247, 255, 0.55);
     margin: 0;
     display: -webkit-box;
     -webkit-line-clamp: 1;
     -webkit-box-orient: vertical;
     overflow: hidden;
     line-height: 1.4;
     grid-column: 1;
     grid-row: 3;
   }

   .email-date {
     font-size: 10px;
     color: rgba(232, 247, 255, 0.45);
     margin: 0;
     grid-column: 2;
     grid-row: 1 / 3;
      display: flex;
      align-items: center;
      justify-content: flex-end;
      text-align: right;
      white-space: nowrap;
      padding-left: 8px;
    }

    .email-provider {
      position: absolute;
      top: -2px;
      left: -2px;
      font-size: 8px;
      font-weight: 900;
      color: #fff;
      background: linear-gradient(135deg, #0099ff 0%, #0077cc 100%);
      padding: 5px 16px 5px 10px;
      white-space: nowrap;
      text-transform: uppercase;
      letter-spacing: 1px;
      z-index: 10;
      box-shadow:
        0 3px 10px rgba(0, 153, 255, 0.4),
        -2px 2px 5px rgba(0, 0, 0, 0.2),
        inset 0 1px 0 rgba(255, 255, 255, 0.2);
      /* Ribbon tail style - cắt góc bên phải */
      clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 50%, calc(100% - 10px) 100%, 0 100%);
      transform: skewX(-3deg);
      box-sizing: border-box;
    }

    .email-provider::before {
      content: '';
      position: absolute;
      bottom: -5px;
      left: 0;
      width: 0;
      height: 0;
      border-left: 5px solid transparent;
      border-right: 0 solid transparent;
      border-top: 5px solid #005599;
    }

    .email-provider::after {
      content: '';
      position: absolute;
      bottom: -5px;
      right: 0;
      width: 0;
      height: 0;
      border-right: 10px solid transparent;
      border-left: 0 solid transparent;
      border-top: 5px solid #005599;
    }

    /* Responsive - Tablet */
    @media (max-width: 1024px) {
      .email-provider {
        font-size: 7px;
        padding: 4px 14px 4px 9px;
        letter-spacing: 0.8px;
        clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 50%, calc(100% - 8px) 100%, 0 100%);
        box-shadow:
          0 2px 8px rgba(0, 153, 255, 0.35),
          -2px 2px 4px rgba(0, 0, 0, 0.15),
          inset 0 1px 0 rgba(255, 255, 255, 0.2);
      }

      .email-provider::before {
        border-left: 4px solid transparent;
        border-top: 4px solid #005599;
        bottom: -4px;
      }

      .email-provider::after {
        border-right: 8px solid transparent;
        border-top: 4px solid #005599;
        bottom: -4px;
      }

      .email-item {
        padding: 20px 12px 10px 12px;
        gap: 3px 10px;
      }
    }

    /* Responsive - Mobile */
    @media (max-width: 768px) {
      .email-provider {
        font-size: 6px;
        padding: 3px 12px 3px 8px;
        letter-spacing: 0.5px;
        top: -1px;
        left: -1px;
        clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 50%, calc(100% - 6px) 100%, 0 100%);
        box-shadow:
          0 2px 6px rgba(0, 153, 255, 0.3),
          -1px 1px 3px rgba(0, 0, 0, 0.1),
          inset 0 1px 0 rgba(255, 255, 255, 0.2);
      }

      .email-provider::before {
        border-left: 3px solid transparent;
        border-top: 3px solid #005599;
        bottom: -3px;
      }

      .email-provider::after {
        border-right: 6px solid transparent;
        border-top: 3px solid #005599;
        bottom: -3px;
      }

      .email-item {
        padding: 18px 10px 8px 10px;
        gap: 2px 8px;
        min-height: 55px;
      }

      .email-from {
        font-size: 11px;
        gap: 4px;
      }

      .email-subject {
        font-size: 12px;
      }

      .email-snippet {
        font-size: 10px;
      }

      .email-date {
        font-size: 9px;
      }
    }

    /* Responsive - Small Mobile */
    @media (max-width: 480px) {
      .email-provider {
        font-size: 5px;
        padding: 2px 10px 2px 6px;
        letter-spacing: 0.3px;
        font-weight: 800;
        clip-path: polygon(0 0, calc(100% - 5px) 0, 100% 50%, calc(100% - 5px) 100%, 0 100%);
        box-shadow:
          0 1px 4px rgba(0, 153, 255, 0.25),
          -1px 1px 2px rgba(0, 0, 0, 0.08);
      }

      .email-provider::before {
        border-left: 2px solid transparent;
        border-top: 2px solid #005599;
        bottom: -2px;
      }

      .email-provider::after {
        border-right: 4px solid transparent;
        border-top: 2px solid #005599;
        bottom: -2px;
      }

      .email-item {
        padding: 16px 8px 6px 8px;
        gap: 2px 6px;
      }

      .email-from {
        font-size: 10px;
        gap: 3px;
      }

      .email-subject {
        font-size: 11px;
      }

      .email-snippet {
        font-size: 9px;
      }

      .email-date {
        font-size: 8px;
      }
    }

    /* Pagination Controls */
    .pagination-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      padding: 14px 12px;
      margin-top: 8px;
      border-radius: 0 0 10px 10px;
      flex-shrink: 0;
    }

   .pagination-spacer {
     flex: 1;
   }

   .pagination-btn {
     padding: 8px 14px;
     border: 1px solid rgba(0, 240, 255, 0.15);
     background: rgba(0, 240, 255, 0.05);
     color: #00f0ff;
     border-radius: 8px;
     font-size: 12px;
     font-weight: 600;
     cursor: pointer;
     transition: all 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     white-space: nowrap;
     font-family: inherit;
   }

   .pagination-btn:hover:not(:disabled) {
     border-color: rgba(0, 240, 255, 0.3);
     background: rgba(0, 240, 255, 0.12);
     transform: translateY(-1px);
     box-shadow: 0 4px 8px rgba(0, 240, 255, 0.08);
   }

   .pagination-btn:active:not(:disabled) {
     transform: translateY(0);
   }

   .pagination-btn:disabled {
     opacity: 0.35;
     cursor: not-allowed;
     border-color: rgba(0, 240, 255, 0.08);
   }

   .pagination-btn-prev,
   .pagination-btn-next {
     min-width: 90px;
   }

  /* Settings Section */
  .settings-section {
    min-height: 400px;
  }

  .config-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .form-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .form-label {
    font-size: 13px;
    font-weight: 600;
    color: #e8f7ff;
  }

  .form-description {
    font-size: 12px;
    color: rgba(232, 247, 255, 0.5);
    margin: 0;
  }

  .label-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .help-button {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    border: 1px solid rgba(0, 240, 255, 0.4);
    background: rgba(0, 240, 255, 0.1);
    color: #00f0ff;
    cursor: pointer;
    font-size: 12px;
    font-weight: 700;
    transition: all 0.2s;
  }

  .help-button:hover {
    background: rgba(0, 240, 255, 0.2);
    border-color: rgba(0, 240, 255, 0.6);
  }

  .form-input,
  .form-textarea {
    padding: 12px;
    border-radius: 10px;
    border: 1px solid rgba(0, 240, 255, 0.2);
    background: rgba(0, 240, 255, 0.04);
    color: #e8f7ff;
    font-size: 13px;
    font-family: inherit;
    transition: all 0.2s ease;
  }

  .form-input:focus,
  .form-textarea:focus {
    outline: none;
    border-color: rgba(0, 240, 255, 0.4);
    background: rgba(0, 240, 255, 0.08);
  }

  .form-select {
    cursor: pointer;
    appearance: none;
    background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='rgba(0,240,255,0.6)'%3e%3cpath d='M6 9l6 6 6-6'/%3e%3c/svg%3e");
    background-repeat: no-repeat;
    background-position: right 10px center;
    background-size: 1.5em 1.5em;
    padding-right: 36px;
  }

  .form-select:hover {
    border-color: rgba(0, 240, 255, 0.4);
  }

  .form-select:focus {
    border-color: rgba(0, 240, 255, 0.6);
    box-shadow: 0 0 0 3px rgba(0, 240, 255, 0.1);
  }

  .form-select option {
    background: rgba(10, 10, 18, 0.9);
    color: rgba(232, 247, 255, 0.9);
    padding: 10px;
  }

  .form-textarea {
    min-height: 140px;
    resize: vertical;
  }

  .form-textarea::-webkit-scrollbar {
    width: 6px;
  }

  .form-textarea::-webkit-scrollbar-track {
    background: rgba(0, 240, 255, 0.04);
    border-radius: 3px;
  }

  .form-textarea::-webkit-scrollbar-thumb {
    background: rgba(0, 240, 255, 0.2);
    border-radius: 3px;
  }

  .form-textarea::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 240, 255, 0.35);
  }

  .form-divider {
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.2), transparent);
  }

  .form-actions {
    display: flex;
    gap: 10px;
    margin-top: 10px;
  }

  /* Button Styles */
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 16px;
    border-radius: 10px;
    border: 1px solid;
    background: rgba(0, 0, 0, 0.2);
    color: #e8f7ff;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.25s ease;
    font-family: inherit;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn:not(:disabled):hover {
    transform: translateY(-2px);
  }

  .btn-primary {
    border-color: rgba(0, 240, 255, 0.35);
    background: rgba(0, 240, 255, 0.12);
    color: #00f0ff;
  }

  .btn-primary:not(:disabled):hover {
    background: rgba(0, 240, 255, 0.2);
    border-color: rgba(0, 240, 255, 0.5);
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    color: rgba(232, 247, 255, 0.4);
  }

  .btn-primary:disabled {
    background: rgba(0, 240, 255, 0.05);
    border-color: rgba(0, 240, 255, 0.15);
  }

  .btn-full {
    width: 100%;
  }

  /* Modals */
  .modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    backdrop-filter: blur(4px);
    animation: fadeIn 0.2s ease;
  }

  @keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }

  .modal {
    background: rgba(12, 18, 32, 0.95);
    border: 1px solid rgba(0, 240, 255, 0.2);
    border-radius: 16px;
    width: min(500px, 90vw);
    max-height: 80vh;
    overflow-y: auto;
    backdrop-filter: blur(10px);
    animation: slideUp 0.3s ease;
  }

  @keyframes slideUp {
    from {
      transform: translateY(20px);
      opacity: 0;
    }
    to {
      transform: translateY(0);
      opacity: 1;
    }
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid rgba(0, 240, 255, 0.12);
  }

  .modal-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }

   .modal-content {
     padding: 20px;
   }

   .modal-footer {
     display: flex;
     gap: 12px;
     justify-content: flex-end;
     padding: 16px 20px;
     border-top: 1px solid rgba(0, 240, 255, 0.12);
   }

   .delete-modal {
     width: min(400px, 90vw);
   }

   .delete-message {
     margin: 0 0 12px 0;
     font-size: 14px;
     color: rgba(232, 247, 255, 0.9);
     line-height: 1.5;
   }

   .delete-message strong {
     color: #ff6464;
     font-weight: 600;
   }

   .delete-warning {
     margin: 0;
     font-size: 12px;
     color: rgba(255, 100, 100, 0.7);
   }

   .btn-delete {
     padding: 10px 24px;
     background: rgba(255, 100, 100, 0.15);
     border: 1px solid rgba(255, 100, 100, 0.3);
     color: #ff6464;
     border-radius: 8px;
     cursor: pointer;
     font-weight: 600;
     transition: all 0.25s ease;
   }

   .btn-delete:hover {
     background: rgba(255, 100, 100, 0.25);
     border-color: rgba(255, 100, 100, 0.5);
   }

   .email-preview {
     display: flex;
     flex-direction: column;
     gap: 16px;
   }

  .preview-field {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .preview-field .label {
    font-size: 11px;
    text-transform: uppercase;
    color: rgba(232, 247, 255, 0.5);
    font-weight: 600;
    letter-spacing: 0.8px;
  }

  .preview-field .value {
    padding: 10px 12px;
    border-radius: 8px;
    background: rgba(0, 240, 255, 0.06);
    border: 1px solid rgba(0, 240, 255, 0.12);
    font-size: 13px;
    word-break: break-word;
  }

  .preview-field .message {
    line-height: 1.6;
    white-space: pre-wrap;
    max-height: 300px;
    overflow-y: auto;
  }

  .help-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .example-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .example-section h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #00f0ff;
  }

  .code-block {
    padding: 12px;
    border-radius: 8px;
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(0, 240, 255, 0.2);
    color: #00f0ff;
    font-size: 12px;
    font-family: 'Courier New', monospace;
    line-height: 1.6;
    word-break: break-word;
    white-space: pre-wrap;
  }

    /* Modern Card Grid */
    .status-grid-modern {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }

    .modern-card {
      padding: 16px;
      border-radius: 12px;
      border: 1px solid rgba(0, 240, 255, 0.15);
      background: rgba(0, 240, 255, 0.05);
      backdrop-filter: blur(10px);
      display: flex;
      flex-direction: column;
      gap: 12px;
      transition: all 0.3s ease;
    }

    .modern-card:hover {
      border-color: rgba(0, 240, 255, 0.3);
      background: rgba(0, 240, 255, 0.08);
      transform: translateY(-4px);
      box-shadow: 0 8px 16px rgba(0, 240, 255, 0.1);
    }

    .modern-card.bot-card {
      grid-column: span 1;
    }

    .modern-card.keyword-card {
      grid-column: span 1;
    }

    .modern-card.run-card {
      grid-column: span 1;
    }

    .modern-card.error-card {
      background: rgba(255, 100, 100, 0.08);
      border-color: rgba(255, 100, 100, 0.2);
    }

    .modern-card.error-card:hover {
      background: rgba(255, 100, 100, 0.12);
      border-color: rgba(255, 100, 100, 0.3);
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 4px;
    }

    .card-header.error-header {
      color: #ff6464;
    }

    .card-icon {
      font-size: 18px;
      color: #00f0ff;
      flex-shrink: 0;
    }

    .card-title {
      font-size: 13px;
      font-weight: 600;
      color: #e8f7ff;
      text-transform: capitalize;
    }

    .card-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    /* Bot Toggle in Card */
    .bot-toggle-large {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .toggle-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .status-badge-small {
      font-size: 12px;
      font-weight: 600;
      color: rgba(232, 247, 255, 0.8);
    }

    .status-badge-small.active {
      color: #00c864;
    }

    .status-badge-small.inactive {
      color: #ffb400;
    }

    .toggle-description {
      font-size: 11px;
      color: rgba(232, 247, 255, 0.5);
    }

    /* Run Time Display */
    .run-time {
      margin: 0;
      font-size: 12px;
      color: #00f0ff;
      font-weight: 500;
    }

    .run-hint {
      margin: 0;
      font-size: 11px;
      color: rgba(232, 247, 255, 0.5);
    }

    /* Error Message */
    .error-message {
      margin: 0;
      font-size: 12px;
      color: #ff6464;
      line-height: 1.5;
      word-break: break-word;
    }

   /* Mailbox Tab Responsive Styles */
   @media (max-width: 1024px) {
     .mailbox-tab-btn {
       padding: 10px 14px;
       gap: 8px;
       font-size: 12px;
     }

     .mailbox-icon {
       font-size: 16px;
     }
   }

   @media (max-width: 768px) {
     .mailbox-header {
       margin-bottom: 16px;
     }

     .mailbox-selector {
       gap: 6px;
       padding: 10px;
     }

     .mailbox-tab-btn {
       padding: 10px 12px;
       gap: 6px;
       font-size: 11px;
       border-radius: 9px;
     }

     .mailbox-icon {
       font-size: 16px;
     }

     .mailbox-btn-label {
       font-size: 11px;
     }
   }

   @media (max-width: 480px) {
     .mailbox-selector {
       gap: 4px;
       padding: 8px;
     }

     .mailbox-tab-btn {
       padding: 8px 10px;
       gap: 4px;
       font-size: 10px;
       border-radius: 8px;
     }

     .mailbox-icon {
       font-size: 14px;
     }

     .mailbox-btn-label {
       font-size: 10px;
       display: none;
     }

     .mailbox-indicator {
       height: 2px;
     }
   }

      /* Responsive */
      @media (max-width: 768px) {
      .email-bot-page {
        padding-top: 20px;
      }

      .not-connected-screen {
        min-height: calc(100vh - 20px);
      }

      .main-content {
        flex-direction: column;
        height: auto;
        min-height: calc(100vh - 20px);
        border-radius: 12px;
      }

      .tab-header {
        flex-direction: row;
        gap: 4px;
        border-right: none;
        border-bottom: 1px solid rgba(0, 240, 255, 0.1);
        padding: 8px;
        width: 100%;
        justify-content: flex-start;
        overflow-x: auto;
      }

      .tab-button {
        height: 40px;
        padding: 8px 12px;
        font-size: 12px;
        flex: 0 0 auto;
      }

      .tab-content {
        min-height: auto;
        padding: 12px;
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      .mailbox-header {
        margin-bottom: 8px;
        flex-wrap: wrap;
        gap: 8px;
      }

      .mailbox-selector {
        padding: 8px;
        gap: 6px;
      }

      .email-list {
        gap: 6px;
        padding-right: 4px;
        max-height: clamp(250px, calc(100vh - 300px), 450px);
        overflow-y: auto;
      }

      .email-item {
        padding: 10px;
        gap: 3px 8px;
        min-height: 50px;
      }

      .email-from {
        font-size: 11px;
        gap: 4px;
      }

      .email-subject {
        font-size: 12px;
      }

      .email-snippet {
        font-size: 10px;
        -webkit-line-clamp: 1;
      }

      .email-date {
        font-size: 9px;
      }

      .pagination-controls {
        padding: 10px 8px;
        gap: 8px;
      }

      .pagination-btn {
        padding: 6px 10px;
        font-size: 11px;
        min-width: 70px;
      }

      .pagination-btn-prev,
      .pagination-btn-next {
        min-width: 70px;
      }

      .not-connected-card {
        grid-template-columns: 1fr;
        gap: 24px;
        padding: 20px;
      }

      .not-connected-content {
        grid-column: 1;
      }

      .not-connected-logo {
        display: none;
      }

      .logo-image {
        width: 150px;
        height: 150px;
      }

      .not-connected-title {
        font-size: 18px;
      }

      .not-connected-description {
        font-size: 12px;
      }

      .not-connected-features {
        gap: 8px;
      }

      .feature-item {
        padding: 8px;
        gap: 8px;
      }

      .feature-text {
        font-size: 11px;
      }

      .btn-connect-gmail {
        padding: 10px 16px;
        font-size: 12px;
      }

      .config-form {
        gap: 16px;
      }

      .form-section {
        gap: 6px;
      }

      .form-label {
        font-size: 12px;
      }

      .form-description {
        font-size: 11px;
      }

      .form-input,
      .form-textarea {
        padding: 10px;
        font-size: 12px;
      }

      .form-textarea {
        min-height: 100px;
      }

      .modal {
        width: min(90vw, 400px);
        max-height: 90vh;
      }

      .modal-header {
        padding: 16px;
      }

      .modal-header h3 {
        font-size: 16px;
      }

      .modal-content {
        padding: 16px;
      }

      .example-section h4 {
        font-size: 13px;
      }

      .code-block {
        padding: 10px;
        font-size: 11px;
      }

      .card-title {
        font-size: 12px;
      }

      .btn {
        padding: 10px 14px;
        font-size: 13px;
      }

      .help-button {
        width: 22px;
        height: 22px;
        font-size: 11px;
      }
     }

     /* Extra small devices */
     @media (max-width: 480px) {
      .email-bot-page {
        padding-top: 12px;
      }

      .not-connected-screen {
        min-height: calc(100vh - 12px);
      }

      .mailbox-selector {
        padding: 6px;
        gap: 4px;
      }

        flex: none;
        max-height: clamp(200px, calc(100vh - 320px), 350px);
        overflow-y: auto;
      }

      .email-item {
        padding: 8px;
        min-height: 48px;
      }

      .email-from {
        font-size: 12px;
      }

      .email-subject {
        font-size: 11px;
      }

      .email-snippet {
        font-size: 11px;
      }

      .email-date {
        font-size: 8px;
      }

      .pagination-controls {
        padding: 8px 6px;
        gap: 6px;
        flex-shrink: 0;
      }

      .pagination-btn {
        padding: 5px 8px;
        font-size: 10px;
        min-width: 60px;
      }

      .not-connected-card {
        padding: 16px;
        gap: 16px;
      }

      .logo-image {
        width: 120px;
        height: 120px;
      }

      .not-connected-title {
        font-size: 16px;
      }

      .not-connected-description {
        font-size: 11px;
      }

      .form-label {
        font-size: 11px;
      }

      .form-input,
      .form-textarea {
        padding: 8px;
        font-size: 11px;
      }

      .btn {
        padding: 8px 12px;
        font-size: 12px;
      }

    /* Connection Section Styles */
    .connection-section {
      min-height: 400px;
    }


  /* AI Provider Selection */
  .status-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .ai-provider-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 20px;
    border-radius: 12px;
    background: rgba(0, 240, 255, 0.04);
    border: 1px solid rgba(0, 240, 255, 0.12);
  }

  .provider-header {
    margin-bottom: 8px;
  }

  .provider-title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: #e8f7ff;
  }

  .provider-description {
    margin: 6px 0 0 0;
    font-size: 12px;
    color: rgba(232, 247, 255, 0.6);
  }

  .provider-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .provider-card {
    padding: 16px;
    border-radius: 12px;
    border: 2px solid rgba(0, 240, 255, 0.15);
    background: rgba(0, 240, 255, 0.04);
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .provider-card:hover {
    border-color: rgba(0, 240, 255, 0.3);
    background: rgba(0, 240, 255, 0.08);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 240, 255, 0.1);
  }

  .provider-card.selected {
    border-color: rgba(0, 240, 255, 0.5);
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.12), rgba(0, 240, 255, 0.06));
    box-shadow: 0 4px 16px rgba(0, 240, 255, 0.15), inset 0 1px 0 rgba(0, 240, 255, 0.15);
  }

  .provider-card-header {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .provider-icon {
    color: #00f0ff;
  }

  .provider-icon.groq-icon {
    color: #00d4ff;
  }

  .provider-icon.gemini-icon {
    color: #ff9d00;
  }

  .provider-name {
    font-size: 14px;
    font-weight: 700;
    color: #e8f7ff;
  }

  .provider-details {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .provider-feature {
    margin: 0;
    font-size: 11px;
    color: rgba(232, 247, 255, 0.7);
    font-weight: 500;
  }

  .provider-checkmark {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(0, 200, 100, 0.2);
    color: #00c864;
    font-size: 14px;
    font-weight: 700;
    border: 1px solid rgba(0, 200, 100, 0.4);
  }

  /* Info Box for warnings/tips */
  .info-box {
    margin: 12px 0 0 0;
    padding: 12px 14px;
    border-radius: 8px;
    background: rgba(255, 193, 7, 0.08);
    border: 1px solid rgba(255, 193, 7, 0.2);
    font-size: 12px;
    color: rgba(255, 247, 223, 0.9);
    line-height: 1.5;
  }

  .info-box strong {
    color: #ffc107;
    font-weight: 600;
  }

  /* Optional Badge */
  .optional-badge {
    font-size: 11px;
    color: rgba(0, 240, 255, 0.6);
    font-weight: 400;
    font-style: italic;
  }

  /* Responsive Provider Grid */
  @media (max-width: 768px) {
    .provider-grid {
      grid-template-columns: 1fr;
    }
  }

  /* Responsive Tab Layout for Mobile */
  @media (max-width: 1024px) {
    .main-content {
      height: auto;
      min-height: calc(100vh - 60px);
      flex-direction: column;
    }

    .tab-header {
      display: flex;
      flex-direction: row;
      gap: 4px;
      border-bottom: 1px solid rgba(0, 240, 255, 0.1);
      border-right: none;
      padding: 8px;
      height: auto;
      width: 100%;
      justify-content: flex-start;
      overflow-x: auto;
    }

    .tab-button {
      padding: 8px 12px;
      font-size: 12px;
      height: 40px;
      min-width: auto;
      flex: 0 0 auto;
    }

    .tab-label {
      display: none;
    }

    .tab-content {
      padding: 12px;
    }

    .settings-section {
      min-height: auto;
      max-height: auto;
    }

    .config-form {
      gap: 20px;
    }

    .status-grid-modern {
      grid-template-columns: 1fr;
    }

    .email-list {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
    }
  }

  /* Extra Small Devices */
  @media (max-width: 480px) {
    .main-content {
      min-height: calc(100vh - 60px);
      height: auto;
      border-radius: 8px;
      margin-top:35px
    }

    .tab-header {
      gap: 2px;
      padding: 4px;
      padding-top: 14px;
    }

    .tab-button {
      padding: 6px 10px;
      font-size: 11px;
      height: 38px;
    }

    .tab-icon {
      font-size: 16px;
    }

    .tab-content {
      padding: 8px;
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    .form-input,
    .form-textarea {
      padding: 10px;
      font-size: 14px;
    }

    .form-textarea {
      min-height: 100px;
      max-height: 200px;
    }

    .mailbox-header {
      gap: 8px;
      margin-bottom: 12px;
    }

    .email-list {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      gap: 6px;
    }

     .email-item {
       padding: 12px 10px 8px 10px;
       min-height: 50px;
     }

     .status-grid-modern {
       gap: 8px;
     }

     .provider-card {
       padding: 12px;
     }
   }

   /* Mobile Responsive - Hide toggle, Icon-only navbar */
   @media (max-width: 768px) {
      .main-content {
        flex-direction: column;
        height: auto;
        min-height: calc(100vh - 60px);
      }

     .tab-header {
       flex-direction: row;
       width: 100% !important;
       height: auto;
       border-right: none;
       border-bottom: 1px solid rgba(0, 240, 255, 0.15);
       padding: 12px 14px;
       gap: 8px;
       overflow-x: auto;
       overflow-y: hidden;
       align-items: center;
       background: linear-gradient(90deg, rgba(0, 240, 255, 0.02) 0%, rgba(0, 200, 100, 0.01) 100%);
       transition: none;
     }

     .tab-header.collapsed {
       width: 100% !important;
       padding: 12px 14px;
     }

     /* Hide toggle button on mobile */
     .navbar-toggle-btn {
       display: none !important;
     }

     /* Make tab buttons icon-only on mobile */
     .tab-button {
       padding: 10px 12px;
       gap: 0;
       width: auto;
       height: 40px;
       min-width: 44px;
     }

     /* Hide labels on mobile */
     .tab-label {
       display: none !important;
     }

     .tab-header.collapsed .tab-label {
       display: none !important;
     }

     .tab-icon {
       font-size: 18px;
     }

      /* Make tab content full width */
      .tab-content {
        padding: 16px;
        flex: 1;
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      /* Adjust tab buttons styling for mobile */
      .tab-button:hover {
        transform: scale(1.08);
      }

        /* Email list adjustments */
        .email-list {
          max-height: clamp(250px, calc(100vh - 300px), 450px);
          overflow-y: auto;
        }

      /* Pagination adjustments */
      .pagination-controls {
        flex-shrink: 0;
      }

      /* Mailbox section adjustments */
      .mailbox-tab-btn {
        padding: 9px 14px;
        font-size: 12px;
        gap: 8px;
      }

     .mailbox-btn-label {
       font-size: 12px;
     }

     /* Form adjustments for mobile */
     .form-input,
     .form-textarea {
       font-size: 16px; /* Prevent zoom on iOS */
     }

     /* Status section */
     .status-grid-modern {
       grid-template-columns: 1fr;
     }

      /* Email item adjustments */
      .email-item {
        padding: 16px 12px 10px 12px;
      }

      /* Tab title responsive */
      .tab-title-section {
        margin-bottom: 16px;
        padding-bottom: 12px;
        padding-top: 12px;
        gap: 10px;
      }

      .tab-title {
        font-size: 16px;
        letter-spacing: 0.3px;
      }

      .tab-divider {
        height: 1px;
      }

      /* Modal adjustments */
      .modal {
        width: 90%;
        max-width: 100%;
        max-height: 90vh;
      }

       .modal-content {
         max-height: 70vh;
         overflow-y: auto;
       }

       /* Reminders layout: stack on mobile */
       .reminders-layout {
         grid-template-columns: 1fr;
         gap: 16px;
       }

       .reminder-form-card {
         position: static;
       }
     }

    /* Tablet Responsive */
    @media (min-width: 769px) and (max-width: 1024px) {
      .tab-header {
        width: 120px;
      }

      .tab-header.collapsed {
        width: 60px;
      }

      .tab-button {
        padding: 12px 12px;
      }

      .tab-content {
        padding: 16px;
      }

      /* Tablet title adjustments */
      .tab-title-section {
        margin-bottom: 18px;
        padding-bottom: 14px;
        padding-top: 14px;
      }

      .tab-title {
        font-size: 18px;
        letter-spacing: 0.4px;
      }
    }

    /* Small mobile adjustments */
    @media (max-width: 480px) {
      .tab-header {
        padding: 10px 8px;
        gap: 6px;
      }

      .tab-button {
        padding: 8px 10px;
        min-width: 40px;
        height: 36px;
      }

      .tab-icon {
        font-size: 16px;
      }

      .tab-content {
        padding: 12px;
      }

      /* Small mobile title adjustments */
      .tab-title-section {
        margin-bottom: 14px;
        padding-bottom: 10px;
        padding-top: 10px;
        gap: 8px;
      }

      .tab-title {
        font-size: 14px;
        letter-spacing: 0.2px;
      }

      .tab-divider {
        height: 1px;
        box-shadow: 0 0 8px rgba(0, 240, 255, 0.15);
      }

      .mailbox-selector {
        gap: 4px;
        padding: 8px;
      }

      .mailbox-tab-btn {
        padding: 8px 10px;
        font-size: 11px;
        height: 36px;
      }

      .email-item {
        padding: 12px 8px 8px 8px;
        min-height: 50px;
      }

      .email-from {
        font-size: 11px;
      }

      .email-subject {
        font-size: 12px;
      }

      .email-snippet {
        font-size: 10px;
      }

      .email-date {
        font-size: 9px;
      }

      .pagination-controls {
        flex-direction: column;
        gap: 8px;
      }

      .pagination-btn {
        width: 100%;
        padding: 10px 12px;
       font-size: 12px;
     }

     .form-section {
       margin-bottom: 16px;
     }

     .form-label {
       font-size: 12px;
     }

     .form-input,
     .form-textarea {
       padding: 10px;
       font-size: 14px;
     }

     .form-textarea {
       min-height: 100px;
       max-height: 200px;
     }

     .modal {
       width: 95%;
     }

     .modal-header {
       padding: 12px;
     }

     .modal-content {
       padding: 12px;
       max-height: 80vh;
     }

     .provider-grid {
       grid-template-columns: 1fr;
     }

      .provider-card {
        padding: 12px;
      }
    }

    /* Reminders Section Styling */
    .reminders-section {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .reminders-content {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

      /* Two Column Layout - Form 30%, List 70% */
      .reminders-layout {
        display: grid;
        grid-template-columns: minmax(0, 0.35fr) minmax(0, 0.65fr);
        gap: 24px;
        align-items: start;
      }

     .reminder-form-section {
       display: flex;
       flex-direction: column;
       gap: 16px;
        min-width: 0;
     }

     .reminders-list-section {
       display: flex;
       flex-direction: column;
       gap: 12px;
        min-width: 0;
     }

      /* Responsive: Tablet (max-width: 1024px) - Keep 30/70 ratio */
      @media (max-width: 1024px) {
        .reminders-layout {
          grid-template-columns: minmax(280px, 0.35fr) minmax(0, 0.65fr);
          gap: 20px;
        }

        .reminder-form-card {
          padding: 16px;
        }

        .form-title {
          font-size: 15px;
        }

        .reminders-table {
          min-width: 680px;
        }
      }

     /* Responsive: Small Tablet (max-width: 768px) - Stack */
     @media (max-width: 768px) {
       .reminders-layout {
         grid-template-columns: 1fr;
         gap: 16px;
       }

        .reminders-table {
          min-width: 620px;
        }

       .reminder-form-section {
         gap: 12px;
       }

       .reminder-form-card {
         padding: 14px;
       }

        .form-title {
          font-size: 14px;
          padding: 9px 14px;
        }

        .list-title {
          font-size: 12px;
          padding: 9px 14px;
        }
     }

     /* Responsive: Mobile (max-width: 480px) - Compact */
     @media (max-width: 480px) {
       .reminders-layout {
         grid-template-columns: 1fr;
         gap: 12px;
       }

        .reminders-table {
          min-width: 560px;
        }

       .reminder-form-section {
         gap: 10px;
       }

       .reminder-form-card {
         padding: 12px;
         gap: 12px;
       }

        .form-title {
          font-size: 13px;
          padding: 8px 12px;
        }

        .list-title {
          font-size: 11px;
          padding: 8px 12px;
        }
     }

      .list-title {
       font-size: 14px;
       font-weight: 700;
       color: rgba(232, 247, 255, 0.7);
       margin: 0;
       background: transparent;
       cursor: pointer;
       transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
       letter-spacing: 0.3px;
       position: relative;
       display: inline-block;
      }

      .list-title:hover {
        color: #00f0ff;
      }

      .form-title {
        font-size: 14px;
        font-weight: 700;
        color: #00f0ff;
        margin: 0;
        background: transparent;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
        letter-spacing: 0.3px;
        position: relative;
        display: inline-block;
      }

      .form-title:hover {
        color: #00f0ff;
      }

      /* Section Title Wrapper */
      .title-bg-container {
        width: 100%;
        padding: 0px 16px;
        background: linear-gradient(90deg, rgba(0, 240, 255, 0.08) 0%, rgba(0, 212, 255, 0.04) 100%);
        border-radius: 3px;
      }

      .section-title-wrapper  {
        display: flex;
        align-items: center;
        gap: 10px;
        margin: 0;
        padding: 5px 0;
      }

      .section-title-icon {
        font-size: 20px;
        opacity: 0.8;
      }

      .form-title,
      .list-title {
        font-size: 12px;
        font-weight: 700;
        background: linear-gradient(135deg, #00f0ff 5%, #00d4ff 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        margin: 0;
        letter-spacing: 0.5px;

      }

      .list-title {
        margin: 0;
      }

      .reminder-count {
        font-size: 12px;
        color: rgba(232, 247, 255, 0.6);
        font-weight: 500;
        padding: 2px 8px;
        background: rgba(0, 240, 255, 0.05);
        border-radius: 12px;
        margin-left: auto;
      }

      /* Responsive: Tablet (max-width: 768px) */
      @media (max-width: 768px) {
        .title-bg-container {
          margin: -10px -14px 6px -14px;
          padding: 6px 14px;
        }

        .section-title-wrapper {
          gap: 8px;
          margin-bottom: 0;
        }

        .section-title-icon {
          font-size: 18px;
        }

        .form-title,
        .list-title {
          font-size: 14px;
        }

        .reminder-count {
          font-size: 11px;
          padding: 2px 6px;
        }
      }

      /* Mobile: Extra Small (max-width: 480px) */
      @media (max-width: 480px) {
        .title-bg-container {
          margin: -8px -12px 6px -12px;
          padding: 6px 12px;
        }

        .section-title-wrapper {
          gap: 6px;
          margin-bottom: 0;
        }

        .section-title-icon {
          font-size: 16px;
        }

        .form-title,
        .list-title {
          font-size: 13px;
        }

        .reminder-count {
          font-size: 10px;
          padding: 2px 6px;
        }
      }

      /* Responsive: Tablet (max-width: 768px) */
      @media (max-width: 768px) {
        .form-title {
          font-size: 14px;
          padding: 10px 14px;
        }

        .list-title {
          font-size: 12px;
          padding: 10px 14px;
        }
      }

      /* Responsive: Mobile (max-width: 480px) */
      @media (max-width: 480px) {
        .form-title {
          font-size: 12px;
          padding: 8px 12px;
          border-bottom-width: 2px;
        }

        .list-title {
          font-size: 11px;
          padding: 8px 12px;
          border-bottom-width: 2px;
        }
      }

     /* Form Header with Info Icon */
    .form-header-with-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: relative;
      gap: 12px;
    }

    .form-header-with-info .section-title-wrapper {
      flex: 0 0 auto;
    }

    /* Right side wrapper groups info icon and actions */
    .header-right-side {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    /* Right side wrapper for info icon and actions */
    .form-header-with-info > .info-icon-container,
    .form-header-with-info > .title-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .label-with-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    /* Small Info Icon (inline with label) */
    .small-info-icon-container {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .small-info-icon {
      width: 18px;
      height: 18px;
      border-radius: 50%;
      border: 1.5px solid rgba(0, 240, 255, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
      color: rgba(0, 240, 255, 0.5);
      cursor: help;
      transition: all 0.2s ease;
      flex-shrink: 0;
    }

    .small-info-icon:hover {
      border-color: rgba(0, 240, 255, 0.7);
      color: #00f0ff;
      background: rgba(0, 240, 255, 0.08);
    }

    .small-info-tooltip {
      display: none;
      position: absolute;
      top: -40px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(10, 10, 18, 0.98);
      border: 1px solid rgba(0, 240, 255, 0.25);
      border-radius: 6px;
      padding: 8px 10px;
      min-width: 160px;
      font-size: 11px;
      color: rgba(232, 247, 255, 0.8);
      z-index: 1000;
      box-shadow: 0 2px 8px rgba(0, 240, 255, 0.08);
      animation: slideInDown 0.2s ease;
      white-space: nowrap;
      text-align: center;
      pointer-events: none;
    }

    .small-info-icon-container:hover .small-info-tooltip {
      display: block;
    }

    @keyframes slideInDown {
      from {
        opacity: 0;
        transform: translateX(-50%) translateY(5px);
      }
      to {
        opacity: 1;
        transform: translateX(-50%) translateY(0);
      }
    }

    .info-icon-container {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .info-icon {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      border: 1.5px solid rgba(0, 240, 255, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      color: rgba(0, 240, 255, 0.6);
      cursor: help;
      transition: all 0.3s ease;
    }

    .info-icon:hover {
      border-color: rgba(0, 240, 255, 0.6);
      color: #00f0ff;
      background: rgba(0, 240, 255, 0.1);
    }

    /* Title Actions Container */
    .title-actions {
      display: flex;
      gap: 8px;
      align-items: center;
    }

    .title-actions .btn-small {
      padding: 6px 12px;
      font-size: 12px;
      height: auto;
      width: auto;
      min-width: auto;
    }

    .info-tooltip {
      display: none;
      position: absolute;
      top: 100%;
      right: 0;
      margin-top: 8px;
      background: rgba(10, 10, 18, 0.98);
      border: 1px solid rgba(0, 240, 255, 0.25);
      border-radius: 10px;
      padding: 12px 14px;
      min-width: 280px;
      font-size: 11px;
      color: rgba(232, 247, 255, 0.7);
      z-index: 1000;
      box-shadow: 0 4px 16px rgba(0, 240, 255, 0.1);
      animation: slideInUp 0.2s ease;
    }

    .info-icon-container:hover .info-tooltip {
      display: block;
    }

    .info-tooltip strong {
      display: block;
      color: #00f0ff;
      margin-bottom: 8px;
    }

    .info-tooltip ul {
      margin: 0;
      padding-left: 16px;
      list-style: disc;
    }

    .info-tooltip li {
      margin: 4px 0;
      line-height: 1.4;
    }

    @keyframes slideInUp {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .reminder-form-card {
      background: rgba(0, 240, 255, 0.04);
      border: 1px solid rgba(0, 240, 255, 0.15);
      border-radius: 12px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      height: fit-content;
      position: sticky;
      top: 20px;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .time-range {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .time-separator {
      color: rgba(232, 247, 255, 0.5);
      font-size: 12px;
    }

    .form-checkbox {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: rgba(232, 247, 255, 0.7);
      font-size: 13px;
    }

    .form-checkbox input {
      cursor: pointer;
    }


    /* Optional Fields Section */
    .optional-fields-section {
      background: rgba(0, 200, 100, 0.05);
      border: 1px solid rgba(0, 200, 100, 0.15);
      border-radius: 10px;
      padding: 16px;
      margin: 16px 0;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .optional-header {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-bottom: 8px;
      border-bottom: 1px solid rgba(0, 200, 100, 0.1);
    }

    .optional-title {
      font-size: 13px;
      font-weight: 700;
      color: #00d4ff;
      margin: 0;
    }

    .optional-hint {
      font-size: 11px;
      color: rgba(232, 247, 255, 0.5);
      margin: 0;
    }

    .optional-field-toggle {
      display: flex;
      padding: 8px 0;
    }

    .toggle-checkbox {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: rgba(232, 247, 255, 0.7);
      font-size: 12px;
      user-select: none;
    }

    .toggle-checkbox input {
      cursor: pointer;
      accent-color: #00f0ff;
    }

    .toggle-checkbox:hover {
      color: #e8f7ff;
    }

    /* Toggle label with info icon */
    .toggle-label-with-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: rgba(232, 247, 255, 0.7);
      font-size: 12px;
      user-select: none;
      position: relative;
    }

    .toggle-label-with-info:hover {
      color: #e8f7ff;
    }

    .toggle-label-with-info .small-info-icon-container {
      margin-left: 4px;
    }

    .optional-field-input {
      padding-left: 8px;
      border-left: 2px solid rgba(0, 200, 100, 0.2);
      margin: 0;
    }

      /* Reminders Table Styles */
      .reminders-table-wrapper {
        overflow-x: auto;
        border: 1px solid rgba(0, 240, 255, 0.15);
        border-radius: 10px;
        background: rgba(0, 240, 255, 0.02);
      }

      .reminders-table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0;
        font-size: 13px;
      }

      .reminders-table thead {
        background: rgba(0, 240, 255, 0.08);
        border-bottom: 2px solid rgba(0, 240, 255, 0.15);
      }

      .reminders-table thead th {
        padding: 14px 12px;
        text-align: center;
        font-weight: 700;
        color: #00f0ff;
        font-size: 12px;
        letter-spacing: 0.5px;
        text-transform: uppercase;
        white-space: nowrap;
        user-select: none;
      }

      .reminders-table tbody tr {
        border-bottom: 1px solid rgba(0, 240, 255, 0.1);
        transition: all 0.25s ease;
        background: rgba(10, 10, 18, 0.3);
      }

      .reminders-table tbody tr:hover {
        background: rgba(0, 240, 255, 0.06);
        border-bottom-color: rgba(0, 240, 255, 0.2);
      }

      .reminder-row.disabled-row {
        opacity: 0.5;
      }

      .reminders-table tbody td {
        padding: 12px;
        color: rgba(232, 247, 255, 0.75);
        vertical-align: middle;
      }

      .col-email {
        width: 25%;
        min-width: 180px;
      }

      .col-status {
        width: 15%;
        min-width: 100px;
      }

      .col-schedule {
        width: 22%;
        min-width: 140px;
      }

      .col-window {
        width: 18%;
        min-width: 120px;
      }

      .col-max {
        width: 10%;
        min-width: 80px;
        text-align: center;
      }

      .col-actions {
        width: 10%;
        min-width: 80px;
        text-align: center;
      }

      .email-cell {
        background: linear-gradient(135deg, #00f0ff 0%, #00d4ff 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        font-weight: 600;
        word-break: break-word;
      }

      .schedule-text,
      .window-text {
        font-size: 12px;
        color: rgba(232, 247, 255, 0.7);
      }

      .max-reminders {
        display: inline-block;
        background: rgba(0, 200, 100, 0.1);
        color: #00c864;
        border: 1px solid rgba(0, 200, 100, 0.2);
        padding: 4px 8px;
        border-radius: 6px;
        font-weight: 600;
        font-size: 12px;
      }

      .actions-group {
        display: flex;
        gap: 6px;
        justify-content: center;
      }

      .status-badge {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        padding: 4px 10px;
        border-radius: 6px;
        font-weight: 700;
        letter-spacing: 0.2px;
      }

      .status-badge.active {
        background: rgba(0, 200, 100, 0.15);
        color: #00c864;
        border: 1px solid rgba(0, 200, 100, 0.3);
      }

      .status-badge.inactive {
        background: rgba(255, 180, 0, 0.15);
        color: #ffb400;
        border: 1px solid rgba(255, 180, 0, 0.3);
      }

      /* Responsive: Tablet (max-width: 1024px) */
      @media (max-width: 1024px) {
        .reminders-table thead th {
          padding: 12px 10px;
          font-size: 11px;
        }

        .reminders-table tbody td {
          padding: 10px 8px;
          font-size: 12px;
        }

        .col-email { width: 20%; min-width: 140px; }
        .col-status { width: 12%; min-width: 80px; }
        .col-schedule { width: 20%; min-width: 120px; }
        .col-window { width: 16%; min-width: 100px; }
        .col-max { width: 12%; min-width: 70px; }
        .col-actions { width: 10%; min-width: 70px; }

      }

      /* Responsive: Small Tablet (max-width: 768px) */
      @media (max-width: 768px) {
        .reminders-table-wrapper {
          border-radius: 8px;
        }

        .reminders-table thead th {
          padding: 10px 6px;
          font-size: 10px;
        }

        .reminders-table tbody td {
          padding: 8px 6px;
          font-size: 11px;
        }

        .col-email { width: 18%; min-width: 100px; }
        .col-status { width: 10%; min-width: 60px; }
        .col-schedule { width: 18%; min-width: 100px; }
        .col-window { width: 14%; min-width: 80px; }
        .col-max { width: 10%; min-width: 50px; }
        .col-actions { width: 12%; min-width: 60px; }

        .email-cell {
          font-size: 11px;
        }

        .schedule-text,
        .window-text {
          font-size: 10px;
        }

        .actions-group {
          gap: 4px;
        }

      }

      /* Responsive: Mobile (max-width: 480px) */
      @media (max-width: 480px) {
        .reminders-table-wrapper {
          border-radius: 6px;
          overflow-x: auto;
          -webkit-overflow-scrolling: touch;
        }

        .reminders-table {
          font-size: 11px;
        }

        .reminders-table thead th {
          padding: 8px 4px;
          font-size: 9px;
          letter-spacing: 0.2px;
        }

        .reminders-table tbody td {
          padding: 6px 4px;
          font-size: 10px;
        }

        .col-email { width: 25%; min-width: 90px; }
        .col-status { width: 12%; min-width: 50px; }
        .col-schedule { width: 20%; min-width: 90px; }
        .col-window { width: 18%; min-width: 70px; }
        .col-max { width: 10%; min-width: 40px; }
        .col-actions { width: 15%; min-width: 50px; }

        .email-cell {
          font-size: 10px;
        }

        .schedule-text,
        .window-text {
          font-size: 9px;
        }

        .max-reminders {
          padding: 3px 6px;
          font-size: 10px;
        }

        .actions-group {
          gap: 3px;
        }

        .btn-small {
          width: auto;
          height: 28px;
          font-size: 11px;
        }

        .status-badge {
          font-size: 10px;
          padding: 3px 8px;
        }
      }

    .btn-small {
      width: 32px;
      height: 32px;
      border: 1px solid rgba(0, 240, 255, 0.15);
      background: transparent;
      color: rgba(232, 247, 255, 0.6);
      border-radius: 8px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.25s ease;
    }

    .btn-small:hover {
      border-color: rgba(0, 240, 255, 0.3);
      color: #e8f7ff;
    }

    .btn-edit:hover {
      background: rgba(0, 240, 255, 0.1);
    }

    .btn-delete:hover {
      border-color: rgba(255, 100, 100, 0.3);
      color: #ff6464;
      background: rgba(255, 100, 100, 0.1);
    }

    .form-actions {
      display: flex;
      gap: 12px;
    }

    .btn-secondary {
      flex: 1;
      padding: 12px 20px;
      border-radius: 10px;
      border: 1px solid rgba(0, 240, 255, 0.15);
      background: transparent;
      color: rgba(232, 247, 255, 0.6);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.25s ease;
    }

    .btn-secondary:hover {
      border-color: rgba(0, 240, 255, 0.3);
      background: rgba(0, 240, 255, 0.08);
      color: #e8f7ff;
    }

    /* Reminders Tab Styles */
    .reminders-content {
      display: flex;
      flex-direction: column;
      gap: 32px;
      overflow-y: auto;
      max-height: calc(100vh - 280px);
      padding-right: 8px;
    }

    .reminders-content::-webkit-scrollbar {
      width: 6px;
    }

    .reminders-content::-webkit-scrollbar-track {
      background: rgba(0, 240, 255, 0.04);
      border-radius: 3px;
    }

    .reminders-content::-webkit-scrollbar-thumb {
      background: rgba(0, 240, 255, 0.2);
      border-radius: 3px;
    }

    .reminders-content::-webkit-scrollbar-thumb:hover {
      background: rgba(0, 240, 255, 0.35);
    }

    .reminder-form-container {
      background: rgba(0, 240, 255, 0.02);
      border: 1px solid rgba(0, 240, 255, 0.1);
      border-radius: 12px;
      padding: 24px;
    }

    .new-reminder-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding-bottom: 16px;
      border-bottom: 1px solid rgba(0, 240, 255, 0.1);
    }

    .new-reminder-title {
      font-size: 16px;
      font-weight: 700;
      color: #00f0ff;
      margin: 0;
      letter-spacing: 0.3px;
    }

    .new-reminder-header .form-actions {
      margin-top: 0;
    }

    .btn-small {
      padding: 8px 16px;
      font-size: 12px;
      height: auto;
      min-width: auto;
    }

    .btn-help {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      padding: 0;
      border: 1px solid rgba(0, 240, 255, 0.2);
      border-radius: 50%;
      background: transparent;
      color: rgba(232, 247, 255, 0.6);
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .btn-help:hover {
      border-color: rgba(0, 240, 255, 0.4);
      background: rgba(0, 240, 255, 0.08);
      color: #00f0ff;
    }

    .form-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      font-weight: 600;
      color: rgba(232, 247, 255, 0.85);
      margin-bottom: 8px;
    }

    .required {
      color: #ff6464;
    }

    .info-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 18px;
      height: 18px;
      border: 1px solid rgba(0, 240, 255, 0.3);
      border-radius: 50%;
      color: rgba(0, 240, 255, 0.6);
      font-size: 11px;
      cursor: help;
    }

    .optional-settings {
      margin-top: 24px;
      padding-top: 20px;
      border-top: 1px solid rgba(0, 240, 255, 0.1);
    }

    .optional-label {
      font-size: 13px;
      font-weight: 700;
      color: rgba(0, 240, 255, 0.8);
      margin: 0 0 4px 0;
    }

    .optional-description {
      font-size: 12px;
      color: rgba(232, 247, 255, 0.5);
      margin: 0 0 16px 0;
    }

    .checkbox-group {
      margin-bottom: 16px;
    }

    .checkbox-item {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      user-select: none;
      margin-bottom: 8px;
    }

    .checkbox-item input[type="checkbox"] {
      width: 16px;
      height: 16px;
      cursor: pointer;
      accent-color: #00f0ff;
    }

    .checkbox-text {
      font-size: 12px;
      color: rgba(232, 247, 255, 0.75);
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .conditional-field {
      margin-left: 26px;
      margin-top: 8px;
      margin-bottom: 12px;
    }

    .time-input-row {
      display: flex;
      gap: 12px;
      align-items: center;
    }

    .time-input {
      flex: 1;
    }

    .time-input input {
      width: 100%;
    }

    .time-separator {
      color: rgba(232, 247, 255, 0.5);
      font-size: 14px;
      font-weight: 600;
      display: flex;
      align-items: center;
      margin-top: 20px;
    }

    .configured-reminders {
      margin-top: 16px;
      background: rgba(0, 240, 255, 0.02);
      border: 1px solid rgba(0, 240, 255, 0.1);
      border-radius: 12px;
      padding: 24px;
    }

    .reminders-section-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid rgba(0, 240, 255, 0.15);
    }

    .reminders-section-title {
      font-size: 15px;
      font-weight: 700;
      color: rgba(0, 240, 255, 0.9);
      margin: 0;
      letter-spacing: 0.3px;
    }

    .reminders-count {
      font-size: 12px;
      color: rgba(232, 247, 255, 0.5);
    }

    .reminders-table-container {
      border: 1px solid rgba(0, 240, 255, 0.1);
      border-radius: 10px;
      overflow: hidden;
    }

    .reminders-table {
      width: 100%;
      border-collapse: collapse;
      background: transparent;
    }

    .reminders-table thead {
      background: rgba(0, 240, 255, 0.04);
      border-bottom: 1px solid rgba(0, 240, 255, 0.15);
    }

    .reminders-table th {
      padding: 14px 12px;
      text-align: center;
      font-size: 11px;
      font-weight: 700;
      color: #00f0ff;
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }

    .reminders-table th:nth-child(1) { width: 25%; } /* EMAIL */
    .reminders-table th:nth-child(2) { width: 20%; } /* STATUS */
    .reminders-table th:nth-child(3) { width: 25%; } /* SCHEDULE */
    .reminders-table th:nth-child(4) { width: 15%; } /* MAX REMINDERS */

    .reminders-table td {
      padding: 14px 12px;
      border-bottom: 1px solid rgba(0, 240, 255, 0.08);
      font-size: 12px;
      color: rgba(232, 247, 255, 0.8);
      text-align: center;
      vertical-align: middle;
    }

    .reminders-table td:nth-child(1) { width: 25%; } /* EMAIL */
    .reminders-table td:nth-child(2) { width: 20%; } /* STATUS */
    .reminders-table td:nth-child(3) { width: 25%; } /* SCHEDULE */
    .reminders-table td:nth-child(4) { width: 15%; } /* MAX REMINDERS */

    .reminders-table tbody tr:last-child td {
      border-bottom: none;
    }

    .reminders-table tbody tr:hover {
      background: rgba(0, 240, 255, 0.03);
    }

    .email-cell {
      color: #00f0ff;
      font-weight: 500;
    }

    .status-cell {
      /* styling handled by parent */
    }

    .status-badge {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      white-space: nowrap;
    }

    .status-badge.active {
      background: rgba(0, 200, 100, 0.15);
      color: #00c864;
      border: 1px solid rgba(0, 200, 100, 0.3);
    }

    .status-badge.inactive {
      background: rgba(255, 150, 100, 0.15);
      color: #ff9664;
      border: 1px solid rgba(255, 150, 100, 0.3);
    }

    .schedule-cell {
      /* styling handled by parent */
    }

    .window-cell {
      /* styling handled by parent */
    }

    .max-cell {
      /* styling handled by parent */
    }

    .reminders-table .actions-cell {
      display: flex !important;
      gap: 8px !important;
      justify-content: center !important;
      align-items: center !important;
      padding: 14px 12px !important;
      text-align: center !important;
      vertical-align: middle !important;
      border-bottom: 1px solid rgba(0, 240, 255, 0.08) !important;
      font-size: 12px !important;
      color: rgba(232, 247, 255, 0.8) !important;
    }

    .action-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      padding: 0;
      border: none;
      border-radius: 6px;
      background: transparent;
      cursor: pointer;
      transition: all 0.3s ease;
      flex-shrink: 0;
    }

    .action-btn svg {
      stroke-width: 2;
      flex-shrink: 0;
    }

    .action-btn.edit-btn {
      color: rgba(0, 240, 255, 0.7);
    }

    .action-btn.edit-btn:hover {
      background: rgba(0, 240, 255, 0.15);
      color: #00f0ff;
    }

    .action-btn.delete-btn {
      color: rgba(255, 100, 100, 0.7);
    }

    .action-btn.delete-btn:hover {
      background: rgba(255, 100, 100, 0.15);
      color: #ff6464;
    }

    .no-reminders {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      text-align: center;
      color: rgba(232, 247, 255, 0.5);
      font-size: 14px;
    }

    .no-reminders p {
      margin: 0;
    }

    /* Contact List Select Styling */
    .conditional-field select {
      width: 100%;
      padding: 12px 36px 12px 12px;
      border-radius: 10px;
      border: 1px solid rgba(0, 240, 255, 0.2);
      background: rgba(0, 240, 255, 0.04) url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='rgba(0,240,255,0.6)'%3e%3cpath d='M6 9l6 6 6-6'/%3e%3c/svg%3e") no-repeat right 10px center;
      background-size: 1.5em 1.5em;
      color: #e8f7ff;
      font-size: 13px;
      font-family: inherit;
      cursor: pointer;
      appearance: none;
      transition: all 0.2s ease;
    }

    .conditional-field select:hover {
      border-color: rgba(0, 240, 255, 0.35);
      background-color: rgba(0, 240, 255, 0.06);
    }

    .conditional-field select:focus {
      outline: none;
      border-color: rgba(0, 240, 255, 0.5);
      background-color: rgba(0, 240, 255, 0.1);
      box-shadow: 0 0 0 3px rgba(0, 240, 255, 0.1);
    }

    .conditional-field select option {
      background: rgba(10, 10, 18, 0.95);
      color: rgba(232, 247, 255, 0.9);
      padding: 10px;
      border: none;
    }

    .conditional-field select option:hover {
      background: rgba(0, 240, 255, 0.2);
      color: #00f0ff;
    }

    .conditional-field select option:checked {
      background: linear-gradient(rgba(0, 240, 255, 0.2), rgba(0, 240, 255, 0.2));
      color: #00f0ff;
    }

    /* Toggle Switch for Enable/Disable */
    .toggle-switch {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      user-select: none;
    }

    .toggle-switch input[type="checkbox"] {
      position: absolute;
      opacity: 0;
      width: 0;
      height: 0;
    }

    .toggle-slider {
      position: relative;
      display: inline-block;
      width: 44px;
      height: 24px;
      background-color: rgba(255, 100, 100, 0.2);
      border-radius: 12px;
      border: 1px solid rgba(255, 100, 100, 0.3);
      transition: all 0.3s ease;
    }

    .toggle-slider::before {
      content: '';
      position: absolute;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background-color: #ff6464;
      left: 2px;
      top: 2px;
      transition: all 0.3s ease;
    }

    .toggle-switch input[type="checkbox"]:checked + .toggle-slider {
      background-color: rgba(0, 200, 100, 0.2);
      border-color: rgba(0, 200, 100, 0.3);
    }

    .toggle-switch input[type="checkbox"]:checked + .toggle-slider::before {
      background-color: #00c864;
      transform: translateX(20px);
    }

    .toggle-label {
      font-size: 12px;
      color: rgba(232, 247, 255, 0.75);
      min-width: 60px;
    }
 </style>

