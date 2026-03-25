<script setup lang="ts">
import { ref } from 'vue'

const isLoading = ref(false)

const handleGoogleLogin = async () => {
  isLoading.value = true
  // Simulate API call to Google login
  setTimeout(() => {
    console.log('Google login initiated')
    // In a real app, you would call Google OAuth here
    // window.location.href = 'https://accounts.google.com/o/oauth2/v2/auth?...'
    isLoading.value = false
  }, 1000)
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
          <p class="login-subtitle">Truy cập vào tài khoản của bạn</p>
        </div>

        <!-- Google Login Button -->
        <div class="login-content">
          <button class="google-login-btn" @click="handleGoogleLogin" :disabled="isLoading">
            <svg
              class="google-icon"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                fill="#EA4335"
              />
            </svg>
            <span v-if="!isLoading">Đăng nhập bằng Google</span>
            <span v-else>Đang kết nối...</span>
          </button>

          <!-- Divider -->
          <div class="divider">
            <span>hoặc</span>
          </div>

          <!-- Info Text -->
          <div class="login-info">
            <p>Sử dụng tài khoản Google của bạn để đăng nhập nhanh chóng và an toàn</p>
          </div>
        </div>

        <!-- Footer -->
        <div class="login-footer">
          <p class="terms-text">
            Bằng cách đăng nhập, bạn đồng ý với
            <a href="#" class="terms-link">Điều khoản dịch vụ</a>
            và
            <a href="#" class="terms-link">Chính sách bảo mật</a>
          </p>
        </div>
      </div>

      <!-- Floating Cards -->
      <div class="floating-card card-1">
        <span class="card-icon">🔐</span>
      </div>
      <div class="floating-card card-2">
        <span class="card-icon">💻</span>
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
  padding: 0;
  width: 100%;
}

/* Background Grid */
.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  display: none;
}

.orb {
  display: none;
}

/* Login Container */
.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
}

/* Login Card */
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

/* Header */
.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-family: 'Comfortaa', sans-serif;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--primary);
}

.login-subtitle {
  font-family: 'Comfortaa', sans-serif;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

/* Content */
.login-content {
  margin-bottom: 30px;
}

/* Google Login Button */
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

/* Divider */
.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.2), transparent);
}

/* Info Text */
.login-info {
  text-align: center;
  padding: 16px;
  background: rgba(0, 240, 255, 0.05);
  border: 1px solid rgba(0, 240, 255, 0.1);
  border-radius: 12px;
}

.login-info p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

/* Footer */
.login-footer {
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 240, 255, 0.1);
}

.terms-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.8;
}

.terms-link {
  color: var(--primary);
  text-decoration: none;
  transition: color 0.3s ease;
  border-bottom: 1px solid transparent;
}

.terms-link:hover {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

/* Floating Cards */
.floating-card {
  position: absolute;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, rgba(0, 240, 255, 0.1), rgba(255, 0, 212, 0.1));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  opacity: 0;
}

.card-1 {
  top: 20%;
  left: 5%;
}

.card-2 {
  bottom: 20%;
  right: 5%;
}

@media (max-width: 768px) {
  .login-card {
    padding: 30px 20px;
  }

  .login-title {
    font-size: 26px;
  }

  .floating-card {
    display: none;
  }
}
</style>
