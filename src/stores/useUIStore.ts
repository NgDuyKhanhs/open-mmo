/**
 * UI Store
 * Global UI state management: loading states, navigation, theme preferences
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { NAVIGATION_CONFIG } from '@/config'

interface NavItem {
  id: number
  badge: string
  title: string
  path: string
}

export const useUIStore = defineStore('ui', () => {
  // State
  const isLoading = ref(false)
  const isMobileMenuOpen = ref(false)
  const activeNavPath = ref('/introduction')
  const navItems = ref<NavItem[]>(NAVIGATION_CONFIG)
  const isDarkMode = ref(true)

  // Computed
  const isNavigating = computed(() => isLoading.value)

  // Actions
  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const toggleMobileMenu = () => {
    isMobileMenuOpen.value = !isMobileMenuOpen.value
  }

  const closeMobileMenu = () => {
    isMobileMenuOpen.value = false
  }

  const openMobileMenu = () => {
    isMobileMenuOpen.value = true
  }

  const setActiveNavPath = (path: string) => {
    activeNavPath.value = path
    closeMobileMenu() // Auto-close mobile menu when navigating
  }

  const toggleDarkMode = () => {
    isDarkMode.value = !isDarkMode.value
    applyTheme()
  }

  const setDarkMode = (isDark: boolean) => {
    isDarkMode.value = isDark
    applyTheme()
  }

  const applyTheme = () => {
    if (isDarkMode.value) {
      document.documentElement.style.colorScheme = 'dark'
    } else {
      document.documentElement.style.colorScheme = 'light'
    }
  }

  return {
    // State
    isLoading,
    isMobileMenuOpen,
    activeNavPath,
    navItems,
    isDarkMode,

    // Computed
    isNavigating,

    // Actions
    setLoading,
    toggleMobileMenu,
    closeMobileMenu,
    openMobileMenu,
    setActiveNavPath,
    toggleDarkMode,
    setDarkMode,
    applyTheme,
  }
})
