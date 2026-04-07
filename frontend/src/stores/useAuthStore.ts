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
  isLoading: boolean
  error: string | null
}

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'auth_access_token',
  USER: 'auth_user',
  // 🔐 IMPORTANT: RefreshToken is now stored in HttpOnly cookie, not localStorage
}

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Initialize from localStorage
  const initializeAuth = () => {
    const storedAccessToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    const storedUser = localStorage.getItem(STORAGE_KEYS.USER)

    if (storedAccessToken) {
      accessToken.value = storedAccessToken
    }
    if (storedUser) {
      try {
        user.value = JSON.parse(storedUser)
      } catch (e) {
        console.error('Failed to parse stored user:', e)
        clearAuth()
      }
    }

    // 🔐 RefreshToken is auto-managed by browser via HttpOnly cookie
    console.log('✅ Auth initialized from localStorage. RefreshToken is in HttpOnly cookie.')
  }

  // Getters
  const isLoggedIn = computed(() => !!user.value && !!accessToken.value)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)
  const isModerator = computed(() => user.value?.roles.includes('MODERATOR') ?? false)
  const token = computed(() => accessToken.value)

  // Helper to save to localStorage
  const saveToStorage = () => {
    if (accessToken.value) {
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken.value)
    }
    if (user.value) {
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user.value))
    }
    // 🔐 RefreshToken is NOT saved to localStorage (stored in HttpOnly cookie by backend)
  }

  // Helper to clear auth state
  const clearAuth = () => {
    user.value = null
    accessToken.value = null
    error.value = null
    localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
    localStorage.removeItem(STORAGE_KEYS.USER)
    // 🔐 Browser will auto-clear HttpOnly cookie on logout
  }

  // Helper to handle auth response
  const handleAuthResponse = (response: AuthResponse) => {
    if (response.success && response.user && response.accessToken) {
      user.value = response.user
      accessToken.value = response.accessToken
      // 🔐 RefreshToken is NOT stored here - it's in HttpOnly cookie
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
    try {
      console.log('🔄 Attempting token refresh...')
      // 🔐 RefreshToken is in HttpOnly cookie - just send empty body
      // Backend will automatically read from cookie
      const response = await refreshTokenAPI('')

      if (response.success && response.accessToken) {
        accessToken.value = response.accessToken
        // 🔐 New RefreshToken is in HttpOnly cookie (not in response)
        saveToStorage()
        console.log('✅ Token refreshed successfully')
        return true
      } else {
        console.error('❌ Token refresh failed:', response.message)
        clearAuth()
        return false
      }
    } catch (err) {
      console.error('❌ Token refresh error:', err)
      clearAuth()
      return false
    }
  }

   const clearError = () => {
     error.value = null
   }

   // Helper to decode JWT and check expiration
   const isTokenExpiringSoon = (token: string | null, thresholdMs: number = 5 * 60 * 1000): boolean => {
     if (!token) return true // No token = expired

     try {
       // JWT format: header.payload.signature
       const parts = token.split('.')
       if (parts.length !== 3) return true

       // Decode payload (add padding if needed)
       let payload = parts[1]
       const padLength = 4 - (payload.length % 4)
       if (padLength !== 4) {
         payload += '='.repeat(padLength)
       }

       const decoded = JSON.parse(atob(payload))
       const expirationTime = decoded.exp * 1000 // Convert to milliseconds
       const currentTime = Date.now()
       const timeUntilExpiration = expirationTime - currentTime

       console.log('🔐 Token expiration check:', {
         expiresIn: Math.floor(timeUntilExpiration / 1000) + 's',
         threshold: Math.floor(thresholdMs / 1000) + 's',
         willRefresh: timeUntilExpiration < thresholdMs,
       })

       return timeUntilExpiration < thresholdMs
     } catch (e) {
       console.error('Failed to decode token:', e)
       return true // Assume expired if decode fails
     }
   }

    return {
     // State
     user,
     accessToken,
     isLoading,
     error,

     // Getters
     isLoggedIn,
     isAdmin,
     isModerator,
     token,

     // Actions
     initializeAuth,
     loginWithCredentials,
     loginWithGoogle,
     performLogout,
     refreshTokenManual,
     clearError,
     clearAuth,
     isTokenExpiringSoon,
   }
})
