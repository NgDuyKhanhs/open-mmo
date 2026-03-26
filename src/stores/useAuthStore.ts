import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, logout, googleLogin, refreshToken as refreshTokenAPI } from '@/services/authService'
import type { LoginPayload, GoogleLoginPayload, AuthResponse } from '@/services/authService'

export interface User {
  id: string
  email: string
  username: string
  firstName?: string
  lastName?: string
  avatar?: string
  emailVerified: boolean
  roles: string[]
  isActive: boolean
  createdAt: string
  lastLoginAt?: string
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isLoading: boolean
  error: string | null
}

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'auth_access_token',
  REFRESH_TOKEN: 'auth_refresh_token',
  USER: 'auth_user',
}

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Initialize from localStorage
  const initializeAuth = () => {
    const storedAccessToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    const storedRefreshToken = localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN)
    const storedUser = localStorage.getItem(STORAGE_KEYS.USER)

    if (storedAccessToken) {
      accessToken.value = storedAccessToken
    }
    if (storedRefreshToken) {
      refreshToken.value = storedRefreshToken
    }
    if (storedUser) {
      try {
        user.value = JSON.parse(storedUser)
      } catch (e) {
        console.error('Failed to parse stored user:', e)
        clearAuth()
      }
    }
  }

  // Getters
  const isLoggedIn = computed(() => !!user.value && !!accessToken.value)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)
  const isModerator = computed(() => user.value?.roles.includes('MODERATOR') ?? false)

  // Helper to save to localStorage
  const saveToStorage = () => {
    if (accessToken.value) {
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken.value)
    }
    if (refreshToken.value) {
      localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken.value)
    }
    if (user.value) {
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user.value))
    }
  }

  // Helper to clear auth state
  const clearAuth = () => {
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    error.value = null
    localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
    localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
    localStorage.removeItem(STORAGE_KEYS.USER)
  }

  // Helper to handle auth response
  const handleAuthResponse = (response: AuthResponse) => {
    if (response.success && response.user && response.accessToken) {
      user.value = response.user
      accessToken.value = response.accessToken
      if (response.refreshToken) {
        refreshToken.value = response.refreshToken
      }
      saveToStorage()
      error.value = null
      return true
    } else {
      error.value = response.message || 'Authentication failed'
      return false
    }
  }

  // Actions
  const loginWithCredentials = async (payload: LoginPayload) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await login(payload)
      const success = handleAuthResponse(response)

      if (success) {
        console.log('Login successful:', user.value?.email)
      }
      return success
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Login failed'
      error.value = message
      console.error('Login error:', message)
      return false
    } finally {
      isLoading.value = false
    }
  }

  const loginWithGoogle = async (payload: GoogleLoginPayload) => {
    isLoading.value = true
    error.value = null

    try {
      const response = await googleLogin(payload)
      const success = handleAuthResponse(response)

      if (success) {
        console.log('Google login successful:', user.value?.email)
      }
      return success
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Google login failed'
      error.value = message
      console.error('Google login error:', message)
      return false
    } finally {
      isLoading.value = false
    }
  }

  const performLogout = async () => {
    isLoading.value = true
    error.value = null

    try {
      if (accessToken.value) {
        await logout(accessToken.value)
      }
    } catch (err) {
      console.error('Logout API call failed:', err)
      // Still clear auth even if API call fails
    } finally {
      clearAuth()
      isLoading.value = false
      console.log('Logout successful')
    }
  }

  const refreshTokenManual = async () => {
    if (!refreshToken.value) {
      clearAuth()
      return false
    }

    try {
      const response = await refreshTokenAPI(refreshToken.value)

      if (response.success && response.accessToken) {
        accessToken.value = response.accessToken
        saveToStorage()
        return true
      } else {
        clearAuth()
        return false
      }
    } catch (err) {
      console.error('Token refresh failed:', err)
      clearAuth()
      return false
    }
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // State
    user,
    accessToken,
    refreshToken,
    isLoading,
    error,

    // Getters
    isLoggedIn,
    isAdmin,
    isModerator,

    // Actions
    initializeAuth,
    loginWithCredentials,
    loginWithGoogle,
    performLogout,
    refreshTokenManual,
    clearError,
    clearAuth,
  }
})
