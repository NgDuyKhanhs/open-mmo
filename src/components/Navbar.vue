<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { NAVIGATION_CONFIG } from '@/config'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)
const profileMenuOpen = ref(false)
const navItems = NAVIGATION_CONFIG

// Initialize auth on mount
authStore.initializeAuth()

const isActive = (path: string) => {
  return route.path === path
}

const navigate = (path: string) => {
  router.push(path)
  mobileMenuOpen.value = false
}

const goHome = () => {
  router.push('/')
  mobileMenuOpen.value = false
}

const toggleMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const toggleProfileMenu = () => {
  profileMenuOpen.value = !profileMenuOpen.value
}

const goToProfile = () => {
  router.push('/profile')
  profileMenuOpen.value = false
  mobileMenuOpen.value = false
}

const closeMenus = () => {
  mobileMenuOpen.value = false
  profileMenuOpen.value = false
}

const handleLogout = () => {
  authStore.performLogout()
  closeMenus()
  router.push('/')
}

// Get user initials for avatar fallback
const userInitials = computed(() => {
  const name = (authStore.user?.firstName || '') + ' ' + (authStore.user?.lastName || '')
  return name
    .trim()
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
})

// Filter nav items: hide login when auth, hide login from items
const displayNavItems = computed(() => {
  return navItems.filter((item) => {
    // Hide login item always (we show profile icon instead)
    return item.path !== '/login'
  })
})
</script>

<template>
  <nav class="navbar">
    <div class="navbar-container">
      <!-- Logo -->
      <div class="navbar-logo" @click="goHome">
        <img src="@/assets/navbar-logo.png" alt="OpenMMO" class="navbar-logo-image" />
      </div>

      <!-- Hamburger Menu -->
      <button class="hamburger" @click="toggleMenu" :class="{ active: mobileMenuOpen }">
        <span></span>
        <span></span>
        <span></span>
      </button>

      <!-- Nav Items -->
      <div class="navbar-menu" :class="{ open: mobileMenuOpen }">
        <!-- Regular nav items -->
        <a
          v-for="item in displayNavItems"
          :key="item.id"
          :href="`#${item.path}`"
          class="navbar-item"
          :class="{ active: isActive(item.path) }"
          @click.prevent="navigate(item.path)"
        >
          <span class="nav-title">{{ item.title }}</span>
        </a>

        <!-- Auth/Profile section for mobile -->
        <div v-if="authStore.isLoggedIn" class="profile-section-mobile">
          <button class="navbar-item profile-item-mobile" @click="goToProfile">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"
              />
            </svg>
            <span class="nav-title">Profile</span>
          </button>
          <button class="navbar-item logout-item-mobile" @click="handleLogout">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                d="M10 7H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-1M15 11l4-4m0 0l-4 4m4-4v8"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span class="nav-title">Log Out</span>
          </button>
        </div>

        <!-- Login link for non-authenticated users -->
        <a
          v-else
          href="#/login"
          class="navbar-item login-item"
          :class="{ active: isActive('/login') }"
          @click.prevent="navigate('/login')"
        >
          <span class="nav-title">Log In</span>
        </a>
      </div>

      <!-- Profile Icon (Desktop) -->
      <div v-if="authStore.isLoggedIn" class="profile-icon-container">
        <button
          class="profile-icon-button"
          @click="toggleProfileMenu"
          :title="authStore.user?.email"
        >
          <div class="avatar-mini">
            <img
              v-if="authStore.user?.avatar"
              :src="authStore.user.avatar"
              :alt="authStore.user?.username || 'Profile'"
              class="avatar-image"
            />
            <div v-else class="avatar-fallback">{{ userInitials }}</div>
          </div>
        </button>

        <!-- Profile Dropdown Menu -->
        <div v-if="profileMenuOpen" class="profile-menu" @click.stop>
          <div class="profile-menu-header">
            <div class="menu-user-info">
              <div class="menu-user-name">
                {{ authStore.user?.firstName }} {{ authStore.user?.lastName }}
              </div>
              <div class="menu-user-email">{{ authStore.user?.email }}</div>
            </div>
          </div>

          <div class="menu-divider"></div>

          <button class="menu-item" @click="goToProfile">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"
              />
            </svg>
            My Profile
          </button>

          <div class="menu-divider"></div>

          <button class="menu-item logout" @click="handleLogout">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                d="M10 7H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-1M15 11l4-4m0 0l-4 4m4-4v8"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            Log Out
          </button>
        </div>
      </div>
    </div>
  </nav>

  <!-- Backdrop for mobile menu and profile menu -->
  <div v-if="mobileMenuOpen || profileMenuOpen" class="backdrop" @click="closeMenus"></div>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(180deg, rgba(10, 10, 18, 0.95) 0%, rgba(10, 10, 18, 0.8) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 240, 255, 0.1);
}

.navbar-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 40px;
  position: relative;
  width: 100%;
}

.navbar-logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
  z-index: 101;
  width: 200px;
  height: 38px;
  overflow: hidden;
  border-radius: 10px;
}

.navbar-logo:hover {
  transform: translateY(-1px) scale(1.02);
}

.navbar-logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  filter: drop-shadow(0 0 12px rgba(0, 240, 255, 0.22));
}

.navbar-menu {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex: 1;
  align-items: center;
}

.navbar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  text-decoration: none;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
  cursor: pointer;
  letter-spacing: 0.5px;
  border: none;
  background: transparent;
}

.navbar-item:hover {
  color: var(--primary);
  transform: translateY(-2px);
}

.navbar-item.active {
  color: var(--primary);
  border-bottom: 2px solid var(--primary);
}

.profile-section-mobile {
  display: none;
  width: 100%;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 240, 255, 0.1);
  margin-top: 12px;
}

.profile-item-mobile,
.logout-item-mobile {
  justify-content: flex-start;
  color: rgba(255, 255, 255, 0.6) !important;
  border-bottom: none !important;
}

.logout-item-mobile {
  color: rgba(255, 0, 110, 0.6) !important;
}

.logout-item-mobile:hover {
  color: #ff006e !important;
}

.login-item {
  font-weight: 600;
}

.profile-icon-container {
  position: relative;
  flex-shrink: 0;
  z-index: 101;
}

.profile-icon-button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(0, 240, 255, 0.3);
  background: transparent;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.profile-icon-button:hover {
  border-color: rgba(0, 240, 255, 0.6);
  transform: scale(1.05);
}

.avatar-mini {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 240, 255, 0.1), rgba(255, 0, 110, 0.1));
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 14px;
  font-weight: 700;
  background: linear-gradient(135deg, #00f0ff, #ff006e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.profile-menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  background: linear-gradient(135deg, rgba(10, 10, 18, 0.98) 0%, rgba(10, 10, 18, 0.95) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 12px;
  min-width: 280px;
  box-shadow: 0 8px 32px rgba(0, 240, 255, 0.1);
  z-index: 1000;
  animation: slideDown 0.2s ease-out;
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

.profile-menu-header {
  padding: 16px;
}

.menu-user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
}

.menu-user-email {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  word-break: break-all;
}

.menu-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.2), transparent);
}

.menu-item {
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  font-family: 'Comfortaa', sans-serif;
}

.menu-item:hover {
  background: rgba(0, 240, 255, 0.1);
  color: #00f0ff;
}

.menu-item.logout {
  color: rgba(255, 0, 110, 0.7);
}

.menu-item.logout:hover {
  background: rgba(255, 0, 110, 0.1);
  color: #ff006e;
}

/* Hamburger Menu */
.hamburger {
  display: none;
  flex-direction: column;
  gap: 6px;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  cursor: pointer;
  z-index: 101;
}

.hamburger span {
  width: 24px;
  height: 2px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 1px;
  transition: all 0.3s ease;
}

.hamburger.active span:nth-child(1) {
  transform: rotate(45deg) translate(12px, 12px);
}

.hamburger.active span:nth-child(2) {
  opacity: 0;
}

.hamburger.active span:nth-child(3) {
  transform: rotate(-45deg) translate(10px, -10px);
}

/* Backdrop */
.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: transparent;
  z-index: 99;
}

/* Tablet: 768px - 991px */
@media (max-width: 991px) {
  .navbar-container {
    gap: 20px;
    padding: 13px 16px;
  }

  .navbar-logo {
    width: 118px;
    height: 34px;
  }

  .navbar-menu {
    gap: 8px;
  }

  .navbar-item {
    padding: 6px 10px;
    font-size: 10px;
    gap: 6px;
  }

  .profile-icon-button {
    width: 36px;
    height: 36px;
  }
}

/* Small Tablet: 640px - 767px */
@media (max-width: 767px) {
  .navbar-container {
    gap: 12px;
    padding: 12px 14px;
  }

  .navbar-menu {
    display: none;
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex-direction: column;
    background: rgba(10, 10, 18, 0.98);
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(0, 240, 255, 0.1);
    padding: 12px;
    gap: 8px;
    z-index: 99;
    margin-top: 2px;
  }

  .navbar-menu.open {
    display: flex;
  }

  .hamburger {
    display: flex;
  }

  .navbar-logo {
    width: 108px;
    height: 32px;
  }

  .navbar-item {
    padding: 10px 12px;
    font-size: 11px;
    width: 100%;
    justify-content: flex-start;
    border-bottom: none !important;
  }

  .profile-section-mobile {
    display: flex;
  }

  .profile-icon-container {
    display: none;
  }

  .profile-menu {
    position: fixed;
    bottom: 20px;
    right: 20px;
    left: 20px;
    top: auto;
    min-width: unset;
  }
}

/* Mobile: 480px - 639px */
@media (max-width: 639px) {
  .navbar-container {
    padding: 12px 12px;
  }

  .navbar-item {
    padding: 9px 11px;
    font-size: 10px;
  }
}

/* Small Mobile: Below 480px */
@media (max-width: 479px) {
  .navbar-container {
    padding: 11px 10px;
  }

  .navbar-item {
    padding: 8px 10px;
    font-size: 9px;
    gap: 5px;
  }

  .hamburger span {
    width: 22px;
    height: 2px;
  }

  .profile-menu {
    bottom: 16px;
    right: 16px;
    left: 16px;
  }
}
</style>
