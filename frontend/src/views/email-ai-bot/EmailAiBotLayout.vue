<script setup lang="ts">
import { ref,  onMounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Mail,
  Settings,
  Bot,
  History,
  Clock2,
} from 'lucide-vue-next'
import { toast } from 'vue3-toastify'
import { useAuthStore } from '@/stores/useAuthStore'
import { useMailboxPagination } from '@/composables/email-ai-bot/useMailboxPagination'
import { useAiProvider } from '@/composables/email-ai-bot/useAiProvider'
import { useEmailBotStatus } from '@/composables/email-ai-bot/useEmailBotStatus'
import { type GmailStatus } from '@/services/gmailService'
import navbarLogoUrl from '@/assets/navbar-logo.png'
import '@/styles/email-ai-bot.css'

// Router
const router = useRouter()
const route = useRoute()

// Auth & Status
const provider = useAiProvider()
const { toggleBot, status: emailBotStatus, loadStatus, connectGmail } = useEmailBotStatus()

// UI State
const navbarExpanded = ref(true)
const botToggleState = ref(false)
const gmailStatus = ref<GmailStatus | null>(null)
const isLoadingStatus = ref(false)

// Tab list for navigation
const tabs = [
  { name: 'email-bot-mailbox', label: 'Mailbox', icon: Mail },
  { name: 'email-bot-config', label: 'Config', icon: Settings },
  { name: 'email-bot-status', label: 'Status', icon: Bot },
  { name: 'email-bot-reminders', label: 'Reminders', icon: Clock2 },
  { name: 'email-bot-history', label: 'History', icon: History },
]

// Load Gmail status on mount
const loadGmailStatus = async () => {
  try {
    isLoadingStatus.value = true
    // Use the proper gmailService function through useEmailBotStatus composable
    await loadStatus()
    gmailStatus.value = emailBotStatus.value
    botToggleState.value = gmailStatus.value.botEnabled

    // Handle OAuth query parameters (from Gmail OAuth callback)
    if (route.query.connected === '1') {
      toast.success('Gmail connected successfully!')
      // Clear query and redirect to mailbox
      router.replace({ name: 'email-bot-mailbox' })
    } else if (route.query.error) {
      toast.error(`Connection failed: ${route.query.error}`)
      router.replace({ name: 'email-bot-mailbox' })
    }
  } catch (err) {
    console.error('Failed to load Gmail status:', err)
    toast.error('Failed to load Gmail status')
  } finally {
    isLoadingStatus.value = false
  }
}

// Handle bot toggle
const handleBotToggle = async (event: Event) => {
  const checkbox = event.target as HTMLInputElement
  const isChecked = checkbox.checked
  botToggleState.value = isChecked

  try {
    await toggleBot()
  } catch (err) {
    botToggleState.value = !isChecked
    console.error('Failed to toggle bot:', err)
    toast.error('Failed to toggle bot status')
  }
}

// Check if tab is active
const isTabActive = (tabName: string) => {
  return route.name === tabName
}

// Navigate to tab
const navigateToTab = (tabName: string) => {
  router.push({ name: tabName })
}

// Handle config saved event - reload Gmail status to get updated triggerSubject
const handleConfigSaved = async () => {
  await loadGmailStatus()
}

// Initialize on mount
onMounted(async () => {
  await loadGmailStatus()
  await provider.loadAiProvider()
})

// Provide shared data to child routes
provide('gmailStatus', gmailStatus)
provide('botToggleState', botToggleState)
provide('handleBotToggle', handleBotToggle)
provide('isLoadingStatus', isLoadingStatus)
</script>

<template>
  <div class="email-bot-page">
    <div class="page-shell">
      <div class="page-container">
        <!-- Not Connected Screen -->
        <div v-if="!gmailStatus?.connected" class="not-connected-screen">
          <div class="not-connected-card">
            <div class="not-connected-logo">
              <img :src="navbarLogoUrl" alt="OpenMMO Logo" class="logo-image" />
            </div>
            <div class="not-connected-content">
              <h2 class="not-connected-title">Connect Your Gmail Account</h2>
              <p class="not-connected-description">
                Enable AI-powered email automation to automatically respond to incoming emails based
                on your custom settings.
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
              <button @click="connectGmail" class="btn-connect-gmail">Connect Gmail Account</button>
              <p class="not-connected-info">
                We only access your emails to send automated replies. Your data is encrypted and
                secure.
              </p>
            </div>
          </div>
        </div>

        <!-- Connected Content -->
        <main v-else class="main-content">
          <!-- Tab Navigation Sidebar -->
          <div class="tab-header" :class="{ collapsed: navbarExpanded }">
            <!-- Toggle Button -->
            <button
              @click.stop="navbarExpanded = !navbarExpanded"
              class="navbar-toggle-btn"
              :title="navbarExpanded ? 'Collapse sidebar' : 'Expand sidebar'"
            >
              <svg
                v-if="navbarExpanded"
                viewBox="0 0 24 24"
                class="toggle-icon"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <rect x="3" y="3" width="18" height="18" rx="2"></rect>
                <path d="M9 3v18" stroke-linecap="round"></path>
                <path d="M16 10l-3 2 3 2" stroke-linecap="round" stroke-linejoin="round"></path>
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
                class="toggle-icon"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <rect x="3" y="3" width="18" height="18" rx="2"></rect>
                <path d="M9 3v18" stroke-linecap="round"></path>
                <path d="M14 10l3 2-3 2" stroke-linecap="round" stroke-linejoin="round"></path>
              </svg>
            </button>

            <!-- Tab Buttons Container -->
            <div class="tab-buttons-container">
              <button
                v-for="tab in tabs"
                :key="tab.name"
                @click.stop="navigateToTab(tab.name)"
                :class="['tab-button', { active: isTabActive(tab.name) }]"
                :title="tab.label"
              >
                 <component :is="tab.icon" :size="18" class="tab-icon" />
                 <span class="tab-label">{{ tab.label }}</span>
              </button>
            </div>
          </div>

           <!-- Tab Content - RouterView will render the current tab view -->
           <div class="tab-content">
             <RouterView v-slot="{ Component, route }">

                 <component
                   :is="Component"
                   :key="route.name"
                   :gmail-status="gmailStatus"
                   :is-loading-status="isLoadingStatus"
                   :bot-toggle-state="botToggleState"
                   @bot-toggle="handleBotToggle"
                   @config-saved="handleConfigSaved"
                 />

             </RouterView>
           </div>
        </main>
      </div>
    </div>
  </div>
</template>
