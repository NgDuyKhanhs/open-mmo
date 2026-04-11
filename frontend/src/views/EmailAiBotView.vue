<script setup lang="ts">
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/useAuthStore'
  import { computed, onMounted, ref } from 'vue'
  import { toast } from 'vue3-toastify'
  import navbarLogoUrl from '@/assets/navbar-logo.png'
  import { Mail, Settings, RefreshCw, HelpCircle, X, Search, Inbox, CheckCircle2, AlertCircle, Bot, Key, Clock, AlertTriangle, LinkIcon } from 'lucide-vue-next'
  import {
    getGmailStatus,
    startGmailConnection,
    enableBot,
    disableBot,
    updateBotConfig,
    updateCustomPrompt,
    getMailbox,
    type GmailStatus,
    type Email,
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
   const activeTab = ref<'mailbox' | 'config' | 'status'>('mailbox')

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
    } catch (err) {
      console.error('Failed to load status:', err)
      toast.error(err instanceof Error ? err.message : 'Failed to load status')
    }
  }

  // Load emails from selected mailbox
  const loadEmails = async () => {
    // Check cache first
     const cacheKey = selectedBox.value
     if (emailsCache.value.has(cacheKey)) {
       emails.value = emailsCache.value.get(cacheKey) || []
       return
     }

    isLoading.value = true
    try {
      if (!authStore.accessToken) {
        toast.error('Bạn cần đăng nhập để xem emails')
        router.push('/login')
        return
      }

      emails.value = await getMailbox(
        authStore.accessToken,
        selectedBox.value.toLowerCase(),
        20
      )
      // Cache the emails
      emailsCache.value.set(cacheKey, emails.value)
    } catch (err) {
      console.error('Failed to load emails:', err)
      if (err instanceof Error && err.message.includes('401')) {
        const refreshed = await authStore.refreshTokenManual()
        if (refreshed && authStore.accessToken) {
          try {
            emails.value = await getMailbox(
              authStore.accessToken,
              selectedBox.value.toLowerCase(),
              20
            )
            // Cache the emails
            emailsCache.value.set(cacheKey, emails.value)
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
      toast.success('Configuration saved')
    } catch (err) {
      console.error('Failed to update config:', err)
      toast.error(err instanceof Error ? err.message : 'Error updating configuration')
    } finally {
      isSaving.value = false
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

   // Refresh emails - clear cache and reload current mailbox
   const refreshEmails = async () => {
     if (isLoading.value) {
       toast.warning('⏳ Already loading emails')
       return
     }
     // Clear cache for current mailbox
     emailsCache.value.delete(selectedBox.value)
     toast.info('Refreshing emails...')
     await loadEmails()
     toast.success('Emails refreshed!')
   }

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
              <div class="tab-header">
                 <button
                   @click="activeTab = 'mailbox'"
                   :class="['tab-button', { active: activeTab === 'mailbox' }]"
                 >
                   <Mail :size="18" class="tab-icon" />
                 </button>
                  <button
                    @click="switchToConfigTab"
                    :class="['tab-button', { active: activeTab === 'config' }]"
                  >
                    <Settings :size="18" class="tab-icon" />

                  </button>
                  <button
                    @click="activeTab = 'status'"
                    :class="['tab-button', { active: activeTab === 'status' }]"
                  >
                    <LinkIcon :size="18" class="tab-icon" />
                  </button>
              </div>

           <!-- Tab Content -->
           <div class="tab-content">
             <!-- Mailbox Tab -->
              <div v-show="activeTab === 'mailbox'" class="mailbox-section tab-pane">
                 <div class="mailbox-header">
                  <div class="mailbox-selector">
                    <button
                      v-for="box in ['inbox', 'sent', 'spam']"
                      :key="box"
                      @click="selectBox(box as typeof selectedBox)"
                      :class="['mailbox-tab', { active: selectedBox === box }]"
                      :title="`View ${box} emails`"
                      :disabled="isLoading"
                    >
                      <span class="mailbox-label">{{ box.charAt(0).toUpperCase() + box.slice(1) }}</span>
                    </button>
                  </div>
                  <button
                    @click="refreshEmails"
                    class="refresh-btn"
                    :disabled="isLoading"
                    title="Refresh current mailbox"
                  >
                    <RefreshCw :size="18" class="refresh-icon" />
                  </button>
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
            </div>

             <!-- Settings Tab -->
             <div v-show="activeTab === 'config'" class="settings-section tab-pane">
               <!-- Bot Status Overview -->

              <div class="config-form">
                <!-- Trigger Subject -->
                <div class="form-section">
                  <label class="form-label">Trigger Subject Keyword</label>
                  <p class="form-description">
                    Bot will only respond to emails containing this keyword
                  </p>
                  <input
                    v-model="configSubject"
                    type="text"
                    placeholder="e.g., youtube, shopify, contact"
                    class="form-input"
                  />
                </div>

                <div class="form-divider"></div>

                <!-- Custom Prompt -->
                <div class="form-section">
                  <div class="label-row">
                    <label class="form-label">Custom AI Prompt</label>
                    <button @click="showPromptHelp = true" class="help-button" title="View examples">
                      <HelpCircle size="16" />
                    </button>
                  </div>
                  <p class="form-description">
                    Optional. Customize how AI responds. Use placeholders: {subject}, {senderEmail}, {body}
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
                    :disabled="isSaving || !configSubject.trim()"
                  >
                    {{ isSaving ? 'Saving...' : 'Save Configuration' }}
                  </button>
                 </div>
               </div>
             </div>

              <!-- Connection Status Tab -->
              <div v-show="activeTab === 'status'" class="status-section tab-pane">
                <div class="status-content">
                  <div class="status-header">
                    <h3 class="status-section-title">Connection</h3>
                    <p class="status-subtitle">Manage your Gmail account and bot settings</p>
                  </div>

                  <!-- Primary Connection Card -->
                  <div v-if="!status.connected" class="primary-connection-card disconnected-card">
                    <div class="connection-icon disconnected-icon">
                      <AlertCircle :size="48" />
                    </div>
                    <div class="connection-info">
                      <h4 class="connection-title">Gmail Account Not Connected</h4>
                      <p class="connection-description">Connect your Gmail account to enable AI-powered auto-replies</p>
                      <button @click="connectGmail" class="btn btn-primary btn-large" :disabled="isLoading">
                        <LinkIcon v-if="!isLoading" :size="16" />
                        <span v-if="!isLoading">Connect Gmail Account</span>
                        <span v-else>Connecting...</span>
                      </button>
                    </div>
                  </div>

                  <div v-else class="primary-connection-card connected-card">
                    <div class="connection-icon connected-icon">
                      <CheckCircle2 :size="48" />
                    </div>
                    <div class="connection-info">
                      <h4 class="connection-title">Connected</h4>
                      <p class="connection-email">{{ status.gmailAddress }}</p>
                    </div>
                  </div>

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

                  <!-- Quick Stats -->

                </div>
              </div>
           </div>
         </main>
       </div>
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
  </div>
</template>

<style scoped>
  * {
    font-family: Comfortaa, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  }

  .email-bot-page {
    min-height: 100vh;
    padding-top: 40px;
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
    min-height: 400px;
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

  .not-connected-icon {
    font-size: 72px;
    opacity: 0.8;
    animation: float 3s ease-in-out infinite;
    grid-column: 1;
    justify-self: center;
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
    width: 100%;
  }

  .status-badge.connected {
    background: rgba(0, 200, 100, 0.15);
    color: #00c864;
    border: 1px solid rgba(0, 200, 100, 0.3);
  }

  .status-badge.disconnected {
    background: rgba(255, 100, 100, 0.15);
    color: #ff6464;
    border: 1px solid rgba(255, 100, 100, 0.3);
  }

  .status-badge.active {
    background: rgba(0, 200, 100, 0.15);
    color: #00c864;
    border: 1px solid rgba(0, 200, 100, 0.3);
  }

  .status-badge.paused {
    background: rgba(255, 180, 0, 0.15);
    color: #ffb400;
    border: 1px solid rgba(255, 180, 0, 0.3);
  }

  .email-display {
    font-size: 12px;
    color: #00f0ff;
    word-break: break-all;
    font-family: monospace;
    padding: 6px;
    background: rgba(0, 240, 255, 0.05);
    border-radius: 6px;
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

  .sidebar-actions {
    display: flex;
    gap: 8px;
  }

  /* Status Overview */
  .status-overview {
    margin-bottom: 24px;
    padding-bottom: 20px;
  }

  .status-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
  }

  .status-info-card {
    padding: 16px;
    background: transparent;
    border: 1px solid rgba(0, 240, 255, 0.12);
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .status-label {
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.8px;
    color: rgba(232, 247, 255, 0.5);
    font-weight: 600;
  }

  .status-badge-large {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 10px 12px;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 600;
    width: 100%;
  }

  .status-badge-large.connected {
    background: rgba(0, 200, 100, 0.15);
    color: #00c864;
    border: 1px solid rgba(0, 200, 100, 0.3);
  }

  .status-badge-large.disconnected {
    background: rgba(255, 100, 100, 0.15);
    color: #ff6464;
    border: 1px solid rgba(255, 100, 100, 0.3);
  }

  .email-address-display {
    font-size: 12px;
    color: #00f0ff;
    word-break: break-all;
    font-family: monospace;
    padding: 8px;
    background: rgba(0, 240, 255, 0.08);
    border-radius: 8px;
  }

  .bot-toggle-container {
    display: flex;
    align-items: center;
    gap: 12px;
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

  .status-text {
    font-size: 12px;
    font-weight: 600;
  }

  .status-text.running {
    color: #00c864;
  }

  .status-text.paused {
    color: #ffb400;
  }

  .error-info {
    background: rgba(255, 100, 100, 0.08) !important;
    border-color: rgba(255, 100, 100, 0.15) !important;
  }

  .error-text {
    font-size: 12px;
    color: #ff6464;
    line-height: 1.4;
  }

  .section-divider {
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.2), transparent);
    margin: 20px 0;
  }

  /* Main Content */
  .main-content {
    background: transparent;
    border: 1px solid rgba(0, 240, 255, 0.16);
    border-radius: 16px;
    backdrop-filter: blur(10px);
    overflow: hidden;
    display: flex;
    flex-direction: row;
    min-height: 700px;
    position: relative;
  }

   .tab-header {
     display: flex;
     flex-direction: column;
     gap: 8px;
     border-bottom: none;
     border-right: 1px solid rgba(0, 240, 255, 0.1);
     background: transparent;
     padding: 16px;
     align-items: stretch;
     height: auto;
     flex-shrink: 0;
   }

    .tab-button {
      padding: 12px 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      border: none;
      background: transparent;
      color: rgba(232, 247, 255, 0.6);
      cursor: pointer;
      font-size: 13px;
      font-weight: 600;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      position: relative;
      overflow: hidden;
      white-space: nowrap;
      width: auto;
      min-width: auto;
      height: 44px;
      flex: 0 1 auto;
    }

   .tab-button:disabled {
     opacity: 0.5;
     cursor: not-allowed;
     pointer-events: none;
   }

  .tab-button::before {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  .tab-button:hover {
    color: #e8f7ff;
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
  }

   .tab-content {
     padding: 20px;
     flex: 1;
     display: flex;
     flex-direction: column;
     min-height: 600px;
     overflow: hidden;
   }

    /* ...existing code... */

    /* Tab Pane Animation */
    .tab-pane {
      animation: fadeInTab 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
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
   }

   .mailbox-selector {
     display: flex;
     align-items: center;
     gap: 8px;
     flex: 1;
     padding: 12px;
     border-radius: 12px;
   }

   .refresh-btn {
     display: flex;
     align-items: center;
     justify-content: center;
     width: 40px;
     height: 40px;
     padding: 0;
     border: 1px solid rgba(0, 240, 255, 0.15);
     background: rgba(0, 240, 255, 0.05);
     border-radius: 10px;
     cursor: pointer;
     transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     color: rgba(232, 247, 255, 0.65);
   }

   .refresh-btn:hover:not(:disabled) {
     border-color: rgba(0, 240, 255, 0.3);
     background: rgba(0, 240, 255, 0.09);
     color: #00f0ff;
   }

   .refresh-btn:active:not(:disabled) {
     transform: scale(0.95);
   }

   .refresh-btn:disabled {
     opacity: 0.5;
     cursor: not-allowed;
   }

   .refresh-icon {
     font-size: 18px;
     display: inline-block;
     transition: transform 0.6s linear;
   }

   .refresh-btn:not(:disabled):active .refresh-icon {
     animation: spin 0.6s linear;
   }

   @keyframes spin {
     to { transform: rotate(360deg); }
   }

   .mailbox-tab {
     display: inline-flex;
     align-items: center;
     justify-content: center;
     gap: 6px;
     padding: 10px 14px;
     border-radius: 10px;
     border: 1px solid rgba(0, 240, 255, 0.15);
     background: rgba(0, 240, 255, 0.05);
     color: rgba(232, 247, 255, 0.65);
     cursor: pointer;
     font-size: 12px;
     font-weight: 600;
     transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
     position: relative;
     flex: 1;
     white-space: nowrap;
   }

   .mailbox-tab:disabled {
     opacity: 0.5;
     cursor: not-allowed;
     pointer-events: none;
   }

  .mailbox-tab::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.15), transparent);
    opacity: 0;
    border-radius: 10px;
    transition: opacity 0.25s ease;
    pointer-events: none;
  }

  .mailbox-tab:hover {
    border-color: rgba(0, 240, 255, 0.3);
    background: rgba(0, 240, 255, 0.09);
    color: #00f0ff;
  }

  .mailbox-tab:hover::before {
    opacity: 1;
  }

  .mailbox-tab.active {
    border-color: rgba(0, 240, 255, 0.5);
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.15), rgba(0, 240, 255, 0.08));
    color: #00f0ff;
    box-shadow:
      0 4px 12px rgba(0, 240, 255, 0.12),
      inset 0 1px 0 rgba(0, 240, 255, 0.2);
  }

  .mailbox-tab.active::before {
    opacity: 0;
  }

  .mailbox-icon {
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }

  .mailbox-tab:hover .mailbox-icon {
    transform: scale(1.1) rotate(-5deg);
  }

  .mailbox-tab.active .mailbox-icon {
    transform: scale(1.15);
  }

  .mailbox-label {
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.2px;
    transition: all 0.25s ease;
  }

  .email-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 600px;
    overflow-y: auto;
    padding-right: 8px;
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
    padding: 14px;
    border-radius: 12px;
    border: 1px solid rgba(0, 240, 255, 0.12);
    background: rgba(0, 240, 255, 0.03);
    cursor: pointer;
    transition: all 0.25s ease;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .email-item:hover {
    border-color: rgba(0, 240, 255, 0.25);
    background: rgba(0, 240, 255, 0.08);
    transform: translateX(4px);
    box-shadow: 0 4px 12px rgba(0, 240, 255, 0.1);
  }

  .email-item.active {
    border-color: rgba(0, 240, 255, 0.4);
    background: rgba(0, 240, 255, 0.12);
    box-shadow: 0 4px 16px rgba(0, 240, 255, 0.15);
  }

   .email-from {
     font-size: 13px;
     font-weight: 600;
     color: #00f0ff;
     margin: 0;
     display: flex;
     align-items: center;
     gap: 8px;
   }

   .email-subject {
    font-size: 14px;
    font-weight: 700;
    color: #e8f7ff;
    margin: 0;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .email-snippet {
    font-size: 12px;
    color: rgba(232, 247, 255, 0.6);
    margin: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
  }

  .email-date {
    font-size: 11px;
    color: rgba(232, 247, 255, 0.4);
    margin: 0;
    margin-top: 4px;
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

  .placeholder-section {
    border-top: 1px solid rgba(0, 240, 255, 0.2);
    padding-top: 12px;
  }

  .placeholder-section h4 {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: #00f0ff;
  }

  .placeholder-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .placeholder-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px;
    border-radius: 8px;
    background: rgba(0, 240, 255, 0.05);
    border: 1px solid rgba(0, 240, 255, 0.1);
  }

  .placeholder-item code {
    padding: 4px 8px;
    border-radius: 4px;
    background: rgba(0, 240, 255, 0.15);
    color: #00f0ff;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
  }

   .placeholder-item span {
     font-size: 12px;
     color: rgba(232, 247, 255, 0.7);
   }

   /* Status Section */
   .status-section {
     min-height: 400px;
   }

   .status-content {
     display: flex;
     flex-direction: column;
     gap: 24px;
   }

   .status-section-title {
     margin: 0;
     font-size: 18px;
     font-weight: 600;
     color: #e8f7ff;
     letter-spacing: 0.5px;
   }

   .status-overview-full {
     margin-bottom: 12px;
     padding-bottom: 12px;
   }

   .status-detail {
     margin: 8px 0 0;
     padding: 8px 0;
     font-size: 12px;
     color: rgba(232, 247, 255, 0.6);
     display: flex;
     flex-direction: column;
     gap: 4px;
   }

   .detail-label {
     font-size: 11px;
     text-transform: uppercase;
     letter-spacing: 0.6px;
     color: rgba(232, 247, 255, 0.4);
     font-weight: 600;
   }

   .detail-value {
     font-family: 'Courier New', monospace;
     color: #00f0ff;
     word-break: break-all;
   }

   .trigger-keyword {
     padding: 10px 12px;
     border-radius: 8px;
     background: rgba(0, 240, 255, 0.08);
     border: 1px solid rgba(0, 240, 255, 0.2);
     font-size: 12px;
     color: #00f0ff;
     font-weight: 500;
   }

   .last-run {
     padding: 10px 12px;
     border-radius: 8px;
     background: rgba(0, 240, 255, 0.08);
     border: 1px solid rgba(0, 240, 255, 0.2);
     font-size: 12px;
     color: rgba(232, 247, 255, 0.7);
   }

    .action-card {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    /* Redesigned Status Tab Styles */
    .status-header {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 12px;
    }

    .status-subtitle {
      margin: 0;
      font-size: 13px;
      color: rgba(232, 247, 255, 0.6);
      font-weight: 400;
    }

    /* Primary Connection Card */
    .primary-connection-card {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 24px;
      border-radius: 14px;
      border: 1px solid rgba(0, 240, 255, 0.2);
      backdrop-filter: blur(10px);
      margin-bottom: 24px;
      transition: all 0.3s ease;
    }

    .primary-connection-card.connected-card {
      background: linear-gradient(135deg, rgba(0, 200, 100, 0.08), rgba(0, 240, 255, 0.08));
      border-color: rgba(0, 200, 100, 0.3);
    }

    .primary-connection-card.disconnected-card {
      background: linear-gradient(135deg, rgba(255, 100, 100, 0.08), rgba(255, 150, 100, 0.08));
      border-color: rgba(255, 100, 100, 0.3);
    }

    .connection-icon {
      font-size: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      width: 60px;
      height: 60px;
    }

    .connection-icon connected-icon {
      color: #00c864;
    }

    .connection-icon.disconnected-icon {
      color: #ff6464;
      animation: pulse 2s ease-in-out infinite;
    }

    @keyframes pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.1); }
    }

    .connection-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .connection-title {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
      color: #e8f7ff;
    }

    .connection-email {
      margin: 0;
      font-size: 13px;
      font-family: 'Courier New', monospace;
      color: #00f0ff;
      font-weight: 500;
    }

    .connection-description {
      margin: 0;
      font-size: 12px;
      color: rgba(232, 247, 255, 0.6);
    }

    .btn-large {
      padding: 12px 24px;
      font-size: 14px;
      font-weight: 600;
      width: auto;
    }

    /* Modern Card Grid */
    .status-grid-modern {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
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

    /* Keyword Display */
    .keyword-display {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      border-radius: 8px;
      background: rgba(0, 240, 255, 0.1);
      border: 1px solid rgba(0, 240, 255, 0.2);
      min-height: 32px;
    }

    .keyword-value {
      font-size: 13px;
      font-weight: 600;
      color: #00f0ff;
      font-family: 'Courier New', monospace;
    }

    .keyword-default {
      font-size: 12px;
      color: rgba(232, 247, 255, 0.5);
      font-style: italic;
    }

    .keyword-hint {
      margin: 0;
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

    /* Quick Stats Section */
    .quick-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 12px;
      padding: 16px;
      border-radius: 12px;
      border: 1px solid rgba(0, 240, 255, 0.15);
      background: rgba(0, 240, 255, 0.03);
    }

    .stat-item {
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: center;
      text-align: center;
    }

    .stat-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: rgba(232, 247, 255, 0.5);
      font-weight: 600;
    }

    .stat-value {
      font-size: 13px;
      font-weight: 700;
      color: rgba(232, 247, 255, 0.8);
      padding: 4px 8px;
      border-radius: 6px;
      background: rgba(0, 240, 255, 0.1);
    }

    .stat-value.active {
      color: #00c864;
      background: rgba(0, 200, 100, 0.15);
    }

    .stat-value.inactive {
      color: #ffb400;
      background: rgba(255, 180, 0, 0.15);
    }

    /* Responsive */
    @media (max-width: 768px) {
     .mailbox-selector {
       grid-template-columns: repeat(2, 1fr);
     }

     .status-grid {
       grid-template-columns: 1fr;
     }

     .tab-content {
       min-height: auto;
     }

     .primary-connection-card {
       flex-direction: column;
       text-align: center;
       gap: 16px;
     }

     .connection-info {
       align-items: center;
     }

     .status-grid-modern {
       grid-template-columns: 1fr;
     }

     .quick-stats {
       grid-template-columns: repeat(2, 1fr);
     }
   }
</style>

