<script setup lang="ts">
import { onMounted } from 'vue'
import navbarLogoUrl from '@/assets/navbar-logo.png'
import { useAuthGuard } from '@/composables/email-ai-bot/useAuthGuard'
import { useEmailBotStatus } from '@/composables/email-ai-bot/useEmailBotStatus'
import EmailAiBotPage from '@/components/email-ai-bot/EmailAiBotPage.vue'
import '@/styles/email-ai-bot.css'

const { ensureLoggedInOrRedirect, handleOAuthCallback } = useAuthGuard()
const { status, loadStatus } = useEmailBotStatus()

onMounted(async () => {
  const isLoggedIn = await ensureLoggedInOrRedirect()
  if (!isLoggedIn) return
  await handleOAuthCallback()
  await loadStatus()
})
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
              <button @click="loadStatus()" class="btn-connect-gmail">
                Connect Gmail Account
              </button>
              <p class="not-connected-info">
                We only access your emails to send automated replies. Your data is encrypted and secure.
              </p>
            </div>
          </div>
        </div>

        <!-- Main Content Area -->
        <EmailAiBotPage v-if="status.connected" :status="status" @config-saved="loadStatus" />
      </div>
    </div>
  </div>
</template>

