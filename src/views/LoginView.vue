<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

interface GoogleCredentialResponse {
  credential: string
  select_by: string
}

interface GoogleIdentity {
  accounts: {
    id: {
      initialize: (config: any) => void
      renderButton: (element: HTMLElement, config: any) => void
      cancel: () => void
    }
  }
}

declare const window: Window & { google: GoogleIdentity }

const router = useRouter()
const authStore = useAuthStore()
const isLoading = ref(false)
const googleButtonRef = ref<HTMLDivElement>()

// Initialize Google Sign-In button
onMounted(() => {
  if (!window.google) {
    authStore.error = 'Error loading Google Sign-In script'
    return
  }

  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  if (!googleClientId || googleClientId.includes('TU_GOOGLE')) {
    authStore.error = 'Google OAuth not configured. Please set VITE_GOOGLE_CLIENT_ID'
    return
  }

  // Clear previous error when initializing
  authStore.error = ''

  window.google.accounts.id.initialize({
    client_id: googleClientId,
    callback: handleGoogleResponse,
    auto_select: false,
  })

  if (googleButtonRef.value) {
    window.google.accounts.id.renderButton(googleButtonRef.value, {
      theme: 'outline',
      size: 'large',
      width: '100%',
      locale: 'vi',
    })
  }
})

// Handle Google response
const handleGoogleResponse = async (response: GoogleCredentialResponse) => {
  isLoading.value = true
  try {
    const { credential } = response
    if (!credential) {
      authStore.error = 'Failed to get credentials from Google'
      return
    }

    // Use store's loginWithGoogle method which handles everything
    const success = await authStore.loginWithGoogle({ idToken: credential })

    if (success) {
      // Login successful, redirect to profile
      router.push('/profile')
    }
    // Error message is already set in the store
  } catch (error) {
    console.error('Google login error:', error)
    authStore.error =
      error instanceof Error ? error.message : 'An error occurred during Google login'
  } finally {
    isLoading.value = false
  }
}

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="login-section">
    <!-- Background Orb -->
    <div class="login-background">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>

    <!-- Login Container -->
    <div class="login-container">
      <div class="login-card">
        <!-- Header -->
        <div class="login-header">
          <h1 class="login-title">OpenMMO</h1>
          <p class="login-subtitle">Đăng nhập để tiếp tục</p>
        </div>

        <!-- Error Message -->
        <div v-if="authStore.error" class="error-message">
          <span class="error-icon">⚠️</span>
          {{ authStore.error }}
        </div>

        <!-- Login Content -->
        <div class="login-content">
          <!-- Google Sign-In Button Container -->
          <div ref="googleButtonRef" class="google-button-container"></div>
        </div>

        <!-- Footer -->
        <div class="login-footer">
          <p class="footer-text">
            Quay lại
            <button class="back-link" @click="goHome">Trang chủ</button>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  overflow: hidden;
  padding: 20px;
  width: 100%;
  background: linear-gradient(135deg, #0a0a12 0%, #1a1a2e 50%, #16213e 100%);
  margin-top: 60px;
}

.login-background {
  display: none;
}

.orb {
  display: none;
}

.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
}

.login-card {
  background: linear-gradient(135deg, rgba(10, 10, 18, 0.95) 0%, rgba(10, 10, 18, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 240, 255, 0.1);
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-family: 'Comfortaa', sans-serif;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #00f0ff 0%, #ffffff83 50%, var(--accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.login-subtitle {
  font-family: 'Comfortaa', sans-serif;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

.login-content {
  margin-bottom: 30px;
}

.google-button-container {
  display: flex;
  justify-content: center;
  width: 100%;
}

.google-button-container :deep(.g_id_signin) {
  width: 100% !important;
}

.google-button-container :deep(.g_id_signin > div) {
  width: 100% !important;
}

.google-button-container :deep(.g_id_signin button) {
  width: 100% !important;
  margin: 0 auto !important;
}

.google-login-btn {
  width: 100%;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #ffffff;
  border: none;
  border-radius: 12px;
  font-family: 'Comfortaa', sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.google-login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s ease;
}

.google-login-btn:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.google-login-btn:hover:not(:disabled)::before {
  left: 100%;
}

.google-login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.google-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.error-message {
  margin-bottom: 24px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #fca5a5;
  animation: slideDown 0.3s ease-out;
}

.error-icon {
  font-size: 16px;
  flex-shrink: 0;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-footer {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 240, 255, 0.1);
}

.footer-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.back-link {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  font-family: inherit;
  padding: 0;
}

.back-link:hover {
  color: #ffffff;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-section {
    padding: 15px;
  }

  .login-card {
    padding: 30px 20px;
  }

  .login-title {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 25px 15px;
  }

  .login-header {
    margin-bottom: 30px;
  }

  .login-title {
    font-size: 24px;
  }

  .login-subtitle {
    font-size: 12px;
  }

  .google-login-btn {
    padding: 14px 12px;
    font-size: 14px;
    gap: 10px;
  }

  .google-icon {
    width: 18px;
    height: 18px;
  }
}
</style>
