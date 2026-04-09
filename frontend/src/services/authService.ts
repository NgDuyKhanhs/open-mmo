/**
 * Authentication API Service
 * Communicates with backend auth endpoints
 */

import { API_CONFIG } from '@/config/apiConfig'

const API_BASE_URL = API_CONFIG.AUTH.BASE

export interface RegisterPayload {
  email: string
  username: string
  password: string
  confirmPassword: string
  firstName?: string
  lastName?: string
}

export interface LoginPayload {
  emailOrUsername: string
  password: string
}

export interface GoogleLoginPayload {
  idToken: string
  accessToken?: string
}

export interface AuthResponse {
  success: boolean
  message: string
  user?: {
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
  accessToken?: string
  refreshToken?: string
  expiresIn?: number
}

export interface RefreshTokenPayload {
  refreshToken: string
}

/**
 * Helper to add Authorization header
 */
function getHeaders(token?: string): HeadersInit {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

/**
 * Helper to handle API errors
 */
async function handleResponse(response: Response): Promise<AuthResponse> {
  const data = await response.json()

  if (!response.ok) {
    throw new Error(data.message || `HTTP Error: ${response.status}`)
  }

  return data
}

/**
 * Register new user
 */
export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/register`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(payload),
  })

  return handleResponse(response)
}

/**
 * Login with email/username and password
 */
export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/login`, {
    method: 'POST',
    headers: getHeaders(),
    credentials: 'include', // 🔐 Send/receive cookies
    body: JSON.stringify(payload),
  })

  return handleResponse(response)
}

/**
 * Login with Google OAuth token
 */
export async function googleLogin(payload: GoogleLoginPayload): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/google-login`, {
    method: 'POST',
    headers: getHeaders(),
    credentials: 'include', // 🔐 Send/receive cookies
    body: JSON.stringify(payload),
  })

  return handleResponse(response)
}

/**
 * Refresh access token
 * 🔐 RefreshToken is auto-sent in HttpOnly cookie by browser
 */
export async function refreshToken(refreshToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/refresh-token`, {
    method: 'POST',
    headers: getHeaders(),
    credentials: 'include', // 🔐 Allow browser to send cookies
    // Only send body if refreshToken is provided (fallback for manual refresh)
    ...(refreshToken ? { body: JSON.stringify({ refreshToken }) } : {}),
  })

  return handleResponse(response)
}

/**
 * Get current user profile
 */
export async function getProfile(accessToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/profile`, {
    method: 'GET',
    headers: getHeaders(accessToken),
  })

  return handleResponse(response)
}

/**
 * Change password
 */
export async function changePassword(
  accessToken: string,
  currentPassword: string,
  newPassword: string,
  confirmPassword: string
): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/change-password`, {
    method: 'POST',
    headers: getHeaders(accessToken),
    body: JSON.stringify({
      currentPassword,
      newPassword,
      confirmPassword,
    }),
  })

  return handleResponse(response)
}

/**
 * Logout endpoint (for backend tracking)
 */
export async function logout(accessToken: string): Promise<AuthResponse> {
  const response = await fetch(`${API_BASE_URL}/logout`, {
    method: 'POST',
    headers: getHeaders(accessToken),
    credentials: 'include', // 🔐 Send/receive cookies
  })

  return handleResponse(response)
}
