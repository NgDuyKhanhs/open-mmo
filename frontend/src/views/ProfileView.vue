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
      <div class="profile-grid">
        <section class="panel overview">
          <div class="avatar-block">
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

            <div class="identity">
              <div class="display-name">{{ displayName }}</div>
            </div>
          </div>
        </section>

        <section class="panel details">
          <div class="info-grid">
            <div class="info-card">
              <p class="label">Username</p>
              <p class="value">{{ authStore.user?.username }}</p>
            </div>

            <div class="info-card">
              <p class="label">Email</p>
              <p class="value">{{ authStore.user?.email || 'Not updated' }}</p>
            </div>

            <div class="info-card">
              <p class="label">Email status</p>
              <p class="value">
                <span class="chip" :class="{ positive: authStore.user?.emailVerified }">
                  {{ authStore.user?.emailVerified ? 'Verified' : 'Unverified' }}
                </span>
              </p>
            </div>

            <div class="info-card">
              <p class="label">Account status</p>
              <p class="value">
                <span class="chip" :class="{ positive: authStore.user?.isActive }">
                  {{ authStore.user?.isActive ? 'Active' : 'Suspended' }}
                </span>
              </p>
            </div>

            <div class="info-card span-2">
              <p class="label">Roles</p>
              <div class="roles">
                <span v-for="role in authStore.user?.roles" :key="role" class="role-badge">{{
                  role
                }}</span>
                <span v-if="!authStore.user?.roles?.length" class="role-badge muted"
                  >No roles assigned</span
                >
              </div>
            </div>

            <div class="info-card">
              <p class="label">Account created</p>
              <p class="value">{{ formatDate(authStore.user?.createdAt || '') }}</p>
            </div>

            <div class="info-card">
              <p class="label">Last login</p>
              <p class="value">
                {{
                  authStore.user?.lastLoginAt ? formatDate(authStore.user.lastLoginAt) : 'No record'
                }}
              </p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .profile-page {
    min-height: 100vh;
    padding: 96px 32px 48px;
    color: #e8f7ff;
    position: relative;
    overflow: hidden;
  }

  .profile-background {
    position: absolute;
    inset: 0;
    z-index: 1;
    pointer-events: none;
  }

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(90px);
    opacity: 0.16;
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
    background: radial-gradient(circle, #00f0ff, transparent);
    top: 45%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 3s;
  }

  @keyframes float {
    0%,
    100% {
      transform: translate(0, 0);
    }
    25% {
      transform: translate(18px, -18px);
    }
    50% {
      transform: translate(0, -28px);
    }
    75% {
      transform: translate(-18px, -18px);
    }
  }

  .profile-shell {
    position: relative;
    z-index: 2;
    width: min(1200px, 100%);
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .profile-top {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 16px;
    align-items: center;
    background: rgba(12, 18, 32, 0.8);
    border: 1px solid rgba(0, 240, 255, 0.18);
    border-radius: 18px;
    padding: 18px 20px;
    backdrop-filter: blur(12px);
  }

  .title-stack {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .title-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .eyebrow {
    margin: 0;
    text-transform: uppercase;
    letter-spacing: 0.1em;
    font-size: 12px;
    color: rgba(255, 255, 255, 0.6);
  }

  .profile-top h1 {
    margin: 0;
    font-size: 26px;
    font-weight: 700;
    background: linear-gradient(135deg, #00f0ff 0%, #ffffffb3 50%, #ff006e 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .subtitle {
    margin: 0;
    color: rgba(232, 247, 255, 0.72);
    font-size: 14px;
  }

  .profile-grid {
    display: grid;
    grid-template-columns: 360px 1fr;
    gap: 16px;
    align-items: start;
  }

  .panel {
    background: rgba(12, 18, 32, 0.78);
    border: 1px solid rgba(0, 240, 255, 0.16);
    border-radius: 18px;
    padding: 20px;
    backdrop-filter: blur(10px);
  }

  .overview {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .avatar-block {
    display: grid;
    grid-template-columns: 104px 1fr;
    gap: 16px;
    align-items: center;
  }

  .avatar {
    width: 104px;
    height: 104px;
    border-radius: 50%;
    border: 3px solid rgba(0, 240, 255, 0.35);
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.12), rgba(255, 0, 110, 0.12));
    overflow: hidden;
    display: grid;
    place-items: center;
  }

  .avatar-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-fallback {
    font-size: 38px;
    font-weight: 700;
    background: linear-gradient(135deg, #00f0ff, #ff006e);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .identity {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .display-name {
    font-size: 22px;
    font-weight: 700;
  }

  .display-email {
    font-size: 14px;
    color: rgba(232, 247, 255, 0.78);
  }

  .username {
    font-size: 13px;
    color: rgba(232, 247, 255, 0.64);
  }

  .chip-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  .chip {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.12);
    background: rgba(255, 255, 255, 0.06);
    font-size: 13px;
    color: rgba(232, 247, 255, 0.9);
    margin: 10px 0;
  }

  .chip.positive {
    border-color: var(--primary);
    color: var(--primary);
  }

  .roles-block {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .label {
    margin: 0;
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: rgba(232, 247, 255, 0.6);
  }

  .roles {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .role-badge {
    padding: 6px 12px;
    border-radius: 10px;
    border: 1px solid var(--primary);
    color: var(--primary);
    background: rgba(255, 255, 255, 0.06);
    font-size: 12px;
    font-weight: 600;
    margin: 10px 0;
  }

  .role-badge.muted {
    border-color: rgba(255, 255, 255, 0.15);
    background: rgba(255, 255, 255, 0.06);
    color: rgba(232, 247, 255, 0.7);
  }

  .timeline {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .timeline-item {
    padding: 12px;
    border-radius: 12px;
    border: 1px solid rgba(0, 240, 255, 0.18);
    background: rgba(255, 255, 255, 0.04);
  }

  .value {
    margin: 6px 0 0;
    font-size: 14px;
    color: #e8f7ff;
  }

  .details .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
  }

  .info-card {
    border: 1px solid rgba(0, 240, 255, 0.18);
    border-radius: 12px;
    padding: 14px;
    background: rgba(255, 255, 255, 0.04);
  }

  .info-card .value {
    margin: 6px 0 0;
  }

  .span-2 {
    grid-column: span 2;
  }

  .icon-button,
  .ghost-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-radius: 12px;
    border: 1px solid rgba(0, 240, 255, 0.25);
    background: rgba(0, 240, 255, 0.08);
    color: #00f0ff;
    padding: 10px 14px;
    cursor: pointer;
    transition: all 0.25s ease;
  }

  .icon-button:hover,
  .ghost-button:hover {
    border-color: rgba(0, 240, 255, 0.4);
    transform: translateY(-1px);
  }

  .ghost-button.danger {
    border-color: rgba(255, 0, 110, 0.35);
    background: rgba(255, 0, 110, 0.12);
    color: #ff4f8b;
  }

  .status-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 10px;
    border-radius: 999px;
    border: 1px solid rgba(255, 255, 255, 0.14);
    background: rgba(255, 255, 255, 0.05);
    font-size: 12px;
    color: rgba(232, 247, 255, 0.9);
  }

  .status-chip.online {
    border-color: rgba(0, 240, 255, 0.35);
    background: rgba(0, 240, 255, 0.12);
    color: #00f0ff;
  }

  @media (max-width: 1024px) {
    .profile-grid {
      grid-template-columns: 1fr;
    }

    .avatar-block {
      grid-template-columns: auto;
      text-align: center;
      justify-items: center;
    }

    .identity {
      align-items: center;
    }

    .span-2 {
      grid-column: span 1;
    }
  }

  @media (max-width: 640px) {
    .profile-page {
      padding: 80px 16px 32px;
    }

    .profile-top {
      grid-template-columns: 1fr;
      justify-items: start;
    }

    .ghost-button {
      width: 100%;
    }

    .chip-row {
      flex-direction: column;
    }
  }

  @media (max-width: 420px) {
    .profile-top h1 {
      font-size: 22px;
    }

    .profile-top {
      padding: 16px;
    }

    .panel {
      padding: 16px;
    }
  }
</style>
