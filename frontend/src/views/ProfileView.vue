<script setup lang="ts">
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/useAuthStore'
  import { computed, ref } from 'vue'

  const router = useRouter()
  const authStore = useAuthStore()
  const avatarFailed = ref(false)

  // Redirect to login if not authenticated
  if (!authStore.isLoggedIn) {
    router.push('/login')
  }

  // Format date
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  // Get user display name
  const displayName = computed(() => {
    if (authStore.user?.firstName || authStore.user?.lastName) {
      return `${authStore.user?.firstName || ''} ${authStore.user?.lastName || ''}`.trim()
    }
    return authStore.user?.username || 'User'
  })

  // Get user initials for avatar fallback
  const userInitials = computed(() => {
    const name = displayName.value
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
  })

  // Logout handler
  const handleLogout = async () => {
    authStore.performLogout()
    router.push('/')
  }

  // Go back handler
  const goBack = () => {
    router.back()
  }

  // Avatar error handler
  const handleAvatarError = () => {
    avatarFailed.value = true
  }
</script>

<template>
  <div class="profile-page">
    <div class="profile-background">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="profile-shell">
      <!-- Header -->


      <!-- Main Content -->
      <div class="profile-grid">
        <!-- Left Panel - Avatar & Basic Info -->
        <section class="panel overview">
          <div class="avatar-section">
            <div class="avatar-container">
              <div class="avatar">
                <img
                  v-if="authStore.user?.avatar && !avatarFailed"
                  :src="authStore.user.avatar"
                  :alt="displayName"
                  class="avatar-image"
                  @error="handleAvatarError"
                />
                <div v-else class="avatar-fallback">{{ userInitials }}</div>
              </div>
              <div class="avatar-glow"></div>
            </div>

            <div class="identity-info">
              <h2 class="display-name">{{ displayName }}</h2>
              <p class="display-email">{{ authStore.user?.email }}</p>
              <p class="username">@{{ authStore.user?.username }}</p>
            </div>
          </div>

          <!-- Status Overview -->
          <div class="status-overview">
            <div class="status-item">
              <span class="status-label">Account Status</span>
              <span class="status-value" :class="{ active: authStore.user?.isActive }">
                {{ authStore.user?.isActive ? '● Active' : '● Suspended' }}
              </span>
            </div>
            <div class="status-item">
              <span class="status-label">Email Verified</span>
              <span class="status-value" :class="{ verified: authStore.user?.emailVerified }">
                {{ authStore.user?.emailVerified ? '✓ Yes' : '✗ No' }}
              </span>
            </div>
          </div>

        </section>

        <!-- Right Panel - Details -->
        <section class="panel details">
          <div class="details-content">
            <!-- Account Information -->
            <div class="info-section">
              <h3 class="section-title">Account Information</h3>
              <div class="info-grid">
                <div class="info-card">
                  <p class="label">Username</p>
                  <p class="value mono">{{ authStore.user?.username }}</p>
                </div>

                <div class="info-card">
                  <p class="label">Email Address</p>
                  <p class="value">{{ authStore.user?.email || 'Not updated' }}</p>
                </div>

                <div class="info-card">
                  <p class="label">Account Created</p>
                  <p class="value">{{ formatDate(authStore.user?.createdAt || '') }}</p>
                </div>

                <div class="info-card">
                  <p class="label">Last Login</p>
                  <p class="value">
                    {{
                      authStore.user?.lastLoginAt
                        ? formatDate(authStore.user.lastLoginAt)
                        : 'No record'
                    }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Roles Section -->
            <div class="info-section">
              <h3 class="section-title">Access Roles</h3>
              <div class="roles-container">
                <span
                  v-for="role in authStore.user?.roles"
                  :key="role"
                  class="role-badge"
                >
                  {{ role }}
                </span>
                <span v-if="!authStore.user?.roles?.length" class="role-badge muted"
                  >No roles assigned</span
                >
              </div>
            </div>
          </div>

          <!-- Action Buttons -->

        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
  :root {
    --primary: #00f0ff;
    --accent: #ff006e;
    --background: rgba(12, 18, 32, 0.9);
    --surface: rgba(20, 28, 48, 0.6);
    --border: rgba(0, 240, 255, 0.15);
    --text-primary: #e8f7ff;
    --text-secondary: rgba(232, 247, 255, 0.72);
    --text-tertiary: rgba(232, 247, 255, 0.5);
  }

  .profile-page {
    min-height: 100vh;
    padding: 96px 32px 48px;
    color: var(--text-primary);
    position: relative;
    overflow-x: hidden;
  }

  .profile-background {
    position: fixed;
    inset: 0;
    z-index: 1;
    pointer-events: none;
  }

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(90px);
    opacity: 0.12;
    animation: float 8s ease-in-out infinite;
  }

  .orb-1 {
    width: 420px;
    height: 420px;
    background: radial-gradient(circle, #00f0ff, transparent);
    top: -80px;
    right: -80px;
  }

  .orb-2 {
    width: 320px;
    height: 320px;
    background: radial-gradient(circle, #ff006e, transparent);
    bottom: -120px;
    left: -80px;
    animation-delay: 1.8s;
  }

  .orb-3 {
    width: 360px;
    height: 360px;
    background: radial-gradient(circle, #00f0ff, transparent);
    top: 45%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 3s;
  }

  @keyframes float {
    0%, 100% {
      transform: translate(0, 0);
    }
    25% {
      transform: translate(20px, -15px);
    }
    50% {
      transform: translate(0, -30px);
    }
    75% {
      transform: translate(-20px, -15px);
    }
  }

  .profile-shell {
    position: relative;
    z-index: 2;
    width: min(1280px, 100%);
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 28px;
  }

  /* Header */
  .profile-header {
    animation: slideDown 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  @keyframes slideDown {
    from {
      opacity: 0;
      transform: translateY(-20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .header-top {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 12px;
  }

  .nav-button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 12px;
    border: 1px solid var(--border);
    background: rgba(0, 240, 255, 0.08);
    color: var(--primary);
    cursor: pointer;
    font-size: 18px;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .nav-button:hover {
    border-color: rgba(0, 240, 255, 0.35);
    background: rgba(0, 240, 255, 0.15);
    transform: translateX(-2px);
  }

  .page-title {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    background: linear-gradient(135deg, var(--primary) 0%, #ffffffb3 50%, var(--accent) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -0.5px;
  }

  .header-spacer {
    flex: 1;
  }

  .header-subtitle {
    margin: 0;
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 400;
  }

  /* Grid Layout */
  .profile-grid {
    display: grid;
    grid-template-columns: 380px 1fr;
    gap: 24px;
    align-items: start;
  }

  .panel {
    background: var(--background);
    border: 1px solid var(--border);
    border-radius: 20px;
    padding: 28px;
    backdrop-filter: blur(12px);
    transition: all 0.3s ease;
    animation: fadeInUp 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .panel:hover {
    border-color: rgba(0, 240, 255, 0.25);
    background: rgba(20, 28, 48, 0.8);
  }

  /* Overview Panel */
  .overview {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 18px;
    text-align: center;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--border);
  }

  .avatar-container {
    position: relative;
    margin-bottom: 8px;
  }

  .avatar {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    border: 2px solid var(--primary);
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.15), rgba(255, 0, 110, 0.15));
    overflow: hidden;
    display: grid;
    place-items: center;
    position: relative;
    z-index: 1;
    transition: all 0.3s ease;
  }

  .avatar:hover {
    border-color: var(--accent);
    transform: scale(1.05);
  }

  .avatar-glow {
    position: absolute;
    inset: -8px;
    border-radius: 50%;
    border: 1px solid rgba(0, 240, 255, 0.2);
    animation: pulse 3s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% {
      opacity: 0.3;
      transform: scale(1);
    }
    50% {
      opacity: 0.8;
      transform: scale(1.1);
    }
  }

  .avatar-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-fallback {
    font-size: 42px;
    font-weight: 700;
    background: linear-gradient(135deg, var(--primary), var(--accent));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .identity-info {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .display-name {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: var(--text-primary);
  }

  .display-email {
    margin: 0;
    font-size: 14px;
    color: var(--text-secondary);
  }

  .username {
    margin: 0;
    font-size: 13px;
    color: var(--text-tertiary);
    font-family: 'Courier New', monospace;
  }

  /* Status Overview */
  .status-overview {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .status-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--border);
    transition: all 0.3s ease;
  }

  .status-item:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(0, 240, 255, 0.2);
  }

  .status-label {
    font-size: 13px;
    color: var(--text-tertiary);
    font-weight: 500;
  }

  .status-value {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-secondary);
  }

  .status-value.active {
    color: #4ade80;
  }

  .status-value.verified {
    color: var(--primary);
  }

  /* Action Buttons */
  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .btn-secondary {
    padding: 12px 20px;
    border-radius: 12px;
    border: 1px solid rgba(255, 0, 110, 0.35);
    background: rgba(255, 0, 110, 0.12);
    color: #ff4f8b;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .btn-secondary:hover {
    border-color: rgba(255, 0, 110, 0.6);
    background: rgba(255, 0, 110, 0.2);
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(255, 0, 110, 0.15);
  }

  /* Details Panel */
  .details {
    display: flex;
    flex-direction: column;
  }

  .details-content {
    display: flex;
    flex-direction: column;
    gap: 32px;
  }

  .info-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
    animation: fadeInUp 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
    animation-fill-mode: both;
  }

  .info-section:nth-child(2) {
    animation-delay: 0.1s;
  }

  .section-title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--text-secondary);
  }

  .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 14px;
  }

  .info-card {
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 16px;
    background: rgba(255, 255, 255, 0.04);
    transition: all 0.3s ease;
  }

  .info-card:hover {
    border-color: rgba(0, 240, 255, 0.2);
    background: rgba(255, 255, 255, 0.08);
    transform: translateY(-2px);
  }

  .label {
    margin: 0;
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--text-tertiary);
    font-weight: 600;
  }

  .value {
    margin: 8px 0 0;
    font-size: 14px;
    color: var(--text-primary);
    word-break: break-word;
  }

  .value.mono {
    font-family: 'Courier New', monospace;
    font-size: 13px;
    color: var(--primary);
  }

  /* Roles */
  .roles-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .role-badge {
    display: inline-flex;
    align-items: center;
    padding: 8px 14px;
    border-radius: 10px;
    border: 1px solid var(--primary);
    background: rgba(0, 240, 255, 0.08);
    color: var(--primary);
    font-size: 12px;
    font-weight: 600;
    transition: all 0.3s ease;
  }

  .role-badge:hover {
    background: rgba(0, 240, 255, 0.15);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 240, 255, 0.2);
  }

  .role-badge.muted {
    border-color: var(--border);
    background: rgba(255, 255, 255, 0.04);
    color: var(--text-tertiary);
  }

  /* Responsive */
  @media (max-width: 1280px) {
    .profile-grid {
      gap: 20px;
    }

    .panel {
      padding: 24px;
    }
  }

  @media (max-width: 1024px) {
    .profile-page {
      padding: 88px 28px 40px;
    }

    .profile-shell {
      gap: 24px;
    }

    .profile-grid {
      grid-template-columns: 1fr;
      gap: 20px;
    }

    .avatar-section {
      flex-direction: row;
      text-align: left;
      align-items: flex-start;
      gap: 20px;
      padding-bottom: 20px;
    }

    .avatar-container {
      margin-bottom: 0;
      flex-shrink: 0;
    }

    .avatar {
      width: 110px;
      height: 110px;
    }

    .display-name {
      font-size: 22px;
    }

    .page-title {
      font-size: 28px;
    }

    .details-content {
      flex-direction: row;
      gap: 24px;
    }

    .info-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 768px) {
    .profile-page {
      padding: 0 20px ;
    }

    .profile-shell {
      gap: 20px;
    }

    .panel {
      padding: 20px;
      border-radius: 16px;
    }

    .page-title {
      font-size: 24px;
    }

    .header-top {
      gap: 16px;
    }

    .nav-button {
      width: 36px;
      height: 36px;
      font-size: 16px;
    }

    .info-grid {
      grid-template-columns: 1fr;
      gap: 12px;
    }

    .avatar-section {
      flex-direction: column;
      text-align: center;
      align-items: center;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--border);
    }

    .avatar {
      width: 100px;
      height: 100px;
    }

    .avatar-glow {
      inset: -6px;
    }

    .avatar-fallback {
      font-size: 36px;
    }

    .details-content {
      gap: 24px;
      flex-direction: column;
    }

    .section-title {
      font-size: 13px;
    }

    .status-overview {
      gap: 10px;
    }

    .status-item {
      padding: 10px;
    }

    .status-label {
      font-size: 12px;
    }

    .status-value {
      font-size: 12px;
    }
  }

  @media (max-width: 640px) {
    .profile-page {
      padding: 75px 16px 32px;
    }

    .profile-shell {
      gap: 16px;
    }

    .header-top {
      gap: 12px;
      margin-bottom: 8px;
    }

    .page-title {
      font-size: 20px;
    }

    .header-subtitle {
      font-size: 13px;
    }

    .panel {
      padding: 16px;
      border-radius: 14px;
    }

    .avatar {
      width: 95px;
      height: 95px;
    }

    .avatar-fallback {
      font-size: 32px;
    }

    .display-name {
      font-size: 18px;
    }

    .display-email {
      font-size: 13px;
    }

    .username {
      font-size: 12px;
    }

    .info-card {
      padding: 12px;
      border-radius: 10px;
    }

    .label {
      font-size: 11px;
    }

    .value {
      font-size: 13px;
      margin-top: 6px;
    }

    .role-badge {
      padding: 6px 10px;
      font-size: 11px;
    }

    .btn-secondary {
      padding: 10px 16px;
      font-size: 13px;
      border-radius: 10px;
    }

    .info-grid {
      gap: 8px;
    }
  }

  @media (max-width: 480px) {
    .profile-page {
      padding: 0 14px;
    }

    .page-title {
      font-size: 18px;
    }

    .header-top {
      gap: 10px;
      margin-bottom: 6px;
    }

    .nav-button {
      width: 32px;
      height: 32px;
      font-size: 14px;
    }

    .header-subtitle {
      font-size: 12px;
    }

    .panel {
      padding: 14px;
      border-radius: 12px;
    }

    .avatar {
      width: 90px;
      height: 90px;
    }

    .avatar-glow {
      inset: -4px;
    }

    .avatar-fallback {
      font-size: 28px;
    }

    .display-name {
      font-size: 16px;
    }

    .display-email {
      font-size: 12px;
    }

    .username {
      font-size: 11px;
    }

    .avatar-section {
      gap: 12px;
    }

    .section-title {
      font-size: 12px;
    }

    .label {
      font-size: 10px;
    }

    .value {
      font-size: 12px;
    }

    .value.mono {
      font-size: 11px;
    }

    .status-item {
      padding: 8px;
      gap: 8px;
    }

    .status-label {
      font-size: 11px;
    }

    .status-value {
      font-size: 11px;
    }

    .info-card {
      padding: 10px;
    }

    .role-badge {
      padding: 5px 8px;
      font-size: 10px;
    }

    .btn-secondary {
      padding: 8px 12px;
      font-size: 12px;
    }

    .roles-container {
      gap: 6px;
    }

    .info-grid {
      gap: 6px;
    }
  }

  @media (max-width: 360px) {
    .profile-page {
      padding: 68px 12px 24px;
    }

    .page-title {
      font-size: 16px;
    }

    .panel {
      padding: 12px;
    }

    .avatar {
      width: 80px;
      height: 80px;
    }

    .display-name {
      font-size: 14px;
    }

    .info-card {
      padding: 8px;
    }

    .label {
      font-size: 9px;
    }

    .value {
      font-size: 11px;
    }
  }
</style>
