<script setup lang="ts">
import { ref, onMounted, provide, watch } from 'vue'
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
import { useGmailMetaStore } from '@/stores/useGmailMetaStore'
import { useMailboxPagination } from '@/composables/email-ai-bot/useMailboxPagination'
import { useEmailBotStatus } from '@/composables/email-ai-bot/useEmailBotStatus'
import { enableBot, disableBot } from '@/services/gmailService'
import navbarLogoUrl from '@/assets/navbar-logo.png'
import '@/styles/email-ai-bot.css'

// Router
const router = useRouter()
const route = useRoute()

// Stores
const authStore = useAuthStore()
const gmailMetaStore = useGmailMetaStore()

// Auth & Status
const { connectGmail } = useEmailBotStatus()

// UI State
const navbarExpanded = ref(true)
const botToggleState = ref(false)

// Tab list for navigation
const tabs = [
  { name: 'email-bot-mailbox', label: 'Mailbox', icon: Mail },
  { name: 'email-bot-config', label: 'Config', icon: Settings },
  { name: 'email-bot-status', label: 'Status', icon: Bot },
  { name: 'email-bot-reminders', label: 'Reminders', icon: Clock2 },
  { name: 'email-bot-history', label: 'History', icon: History },
]

// Handle bot toggle
const handleBotToggle = async (event: Event) => {
  const checkbox = event.target as HTMLInputElement
  const isChecked = checkbox.checked
  botToggleState.value = isChecked

  try {
    if (isChecked) {
      await enableBot(authStore.accessToken!)
      toast.success('Bot enabled')
    } else {
      await disableBot(authStore.accessToken!)
      toast.success('Bot disabled')
    }
    // Refresh metadata to get updated status
    await gmailMetaStore.refresh()
  } catch (err) {
    botToggleState.value = !isChecked
    console.error('Failed to toggle bot:', err)
    toast.error('Failed to toggle bot status')
  }
}

// Handle config saved event - reload Gmail status to get updated triggerSubject
const handleConfigSaved = async () => {
  await gmailMetaStore.refresh()
}

// Check if tab is active
const isTabActive = (tabName: string) => {
  return route.name === tabName
}

// Navigate to tab
const navigateToTab = (tabName: string) => {
  router.push({ name: tabName })
}

// Initialize on mount
onMounted(async () => {
  try {
    await gmailMetaStore.ensureLoaded()
    botToggleState.value = gmailMetaStore.gmailStatus?.botEnabled ?? false

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
    console.error('Failed to load Gmail metadata:', err)
    toast.error('Failed to load Gmail metadata')
  }
})

// Watch for access token changes to reset cache
watch(
  () => authStore.accessToken,
  (newToken) => {
    if (!newToken) {
      // User logged out
      gmailMetaStore.reset()
    } else {
      // Token changed, reload metadata
      gmailMetaStore.reset()
      gmailMetaStore.ensureLoaded()
    }
  }
)

// Provide shared data to child routes
provide('gmailStatus', gmailMetaStore.gmailStatus)
provide('botToggleState', botToggleState)
provide('handleBotToggle', handleBotToggle)
provide('isLoadingStatus', gmailMetaStore.isLoadingStatus)
</script>

<template>
  <div class="email-bot-page">
    <div class="page-shell">
      <div class="page-container">
        <!-- Not Connected Screen -->
        <div v-if="!gmailMetaStore.gmailStatus?.connected" class="not-connected-screen">
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
                   :key="route.fullPath"
                   :gmail-status="gmailMetaStore.gmailStatus"
                   :is-loading-status="gmailMetaStore.isLoadingStatus"
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
