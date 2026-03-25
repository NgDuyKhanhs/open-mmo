<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NAVIGATION_CONFIG } from '@/config'

const router = useRouter()
const route = useRoute()
const mobileMenuOpen = ref(false)
const navItems = NAVIGATION_CONFIG

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
</script>

<template>
  <nav class="navbar">
    <div class="navbar-container">
      <!-- Logo -->
      <div class="navbar-logo" @click="goHome">
        <span class="navbar-brand">OpenMMO</span>
      </div>

      <!-- Hamburger Menu -->
      <button class="hamburger" @click="toggleMenu" :class="{ active: mobileMenuOpen }">
        <span></span>
        <span></span>
        <span></span>
      </button>

      <!-- Nav Items -->
      <div class="navbar-menu" :class="{ open: mobileMenuOpen }">
        <a
          v-for="item in navItems"
          :key="item.id"
          :href="`#${item.path}`"
          class="navbar-item"
          :class="{ active: isActive(item.path) }"
          @click.prevent="navigate(item.path)"
        >
          <span class="nav-title">{{ item.title }}</span>
        </a>
      </div>
    </div>
  </nav>
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
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  flex-shrink: 0;
  z-index: 101;
}

.navbar-logo:hover {
  transform: scale(1.05);
}

.navbar-logo svg {
  width: 32px;
  height: 32px;
}

.navbar-brand {
  font-family: 'Comfortaa', sans-serif;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary) 0%, #ffffff83 50%, var(--accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
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
}

.navbar-item:hover {
  color: var(--primary);
  transform: translateY(-2px);
}

.navbar-item.active {
  color: var(--primary);
}

.nav-title {
  display: inline;
  font-weight: 500;
}

.hamburger {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px 0;
  z-index: 101;
  flex-shrink: 0;
}

.hamburger span {
  width: 24px;
  height: 2px;
  background: var(--primary);
  border-radius: 2px;
  transition: all 0.3s ease;
}

.hamburger.active span:nth-child(1) {
  transform: rotate(45deg) translate(8px, 8px);
}

.hamburger.active span:nth-child(2) {
  opacity: 0;
}

.hamburger.active span:nth-child(3) {
  transform: rotate(-45deg) translate(7px, -7px);
}

/* Desktop: 1200px and above */
@media (min-width: 1200px) {
  .navbar-item {
    padding: 8px 14px;
    font-size: 12px;
  }
}

/* Tablet Large: 992px - 1199px */
@media (max-width: 1199px) {
  .navbar-container {
    gap: 30px;
    padding: 15px 20px;
  }

  .navbar-menu {
    gap: 10px;
  }

  .navbar-item {
    padding: 7px 12px;
    font-size: 11px;
  }

  .nav-badge {
    width: 22px;
    height: 22px;
    font-size: 9px;
  }

  .navbar-brand {
    font-size: 16px;
  }
}

/* Tablet: 768px - 991px */
@media (max-width: 991px) {
  .navbar-container {
    gap: 20px;
    padding: 13px 16px;
  }

  .navbar-brand {
    display: inline;
    font-size: 15px;
  }

  .navbar-menu {
    gap: 8px;
  }

  .navbar-item {
    padding: 6px 10px;
    font-size: 10px;
    gap: 6px;
  }

  .nav-badge {
    width: 20px;
    height: 20px;
    font-size: 8px;
  }

  .navbar-logo svg {
    width: 28px;
    height: 28px;
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
    max-height: calc(100vh - 60px);
    overflow-y: auto;
    margin-top: 2px;
  }

  .navbar-menu.open {
    display: flex;
  }

  .hamburger {
    display: flex;
  }

  .navbar-brand {
    display: none;
  }

  .navbar-item {
    padding: 10px 12px;
    font-size: 11px;
    width: 100%;
    justify-content: flex-start;
  }

  .nav-badge {
    width: 24px;
    height: 24px;
    font-size: 9px;
    flex-shrink: 0;
  }

  .navbar-logo svg {
    width: 28px;
    height: 28px;
  }
}

/* Mobile: 480px - 639px */
@media (max-width: 639px) {
  .navbar-container {
    padding: 12px 12px;
  }

  .navbar-menu {
    top: calc(100% + 1px);
  }

  .navbar-item {
    padding: 9px 11px;
    font-size: 10px;
  }

  .nav-badge {
    width: 22px;
    height: 22px;
    font-size: 8px;
  }

  .navbar-logo svg {
    width: 26px;
    height: 26px;
  }
}

/* Small Mobile: Below 480px */
@media (max-width: 479px) {
  .navbar-container {
    padding: 11px 10px;
  }

  .navbar-menu {
    top: calc(100% + 1px);
  }

  .navbar-item {
    padding: 8px 10px;
    font-size: 9px;
    gap: 5px;
  }

  .nav-badge {
    width: 20px;
    height: 20px;
    font-size: 7.5px;
  }

  .hamburger span {
    width: 22px;
    height: 2px;
  }

  .navbar-logo svg {
    width: 24px;
    height: 24px;
  }
}
</style>
