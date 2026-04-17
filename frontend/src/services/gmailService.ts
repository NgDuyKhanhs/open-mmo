/**
 * Gmail API Service
 * Communicates with backend Gmail endpoints
 * Handles Gmail OAuth, bot configuration, and mailbox operations
 */

import { API_CONFIG } from '@/config/apiConfig'

const API_BASE_URL = API_CONFIG.GMAIL.BASE

// ============================================
// INTERFACES
// ============================================

export interface GmailStatus {
  connected: boolean
  gmailAddress: string
  botEnabled: boolean
  triggerSubject: string
  lastRunAt: string | null
  lastError: string | null
}

export interface Email {
  id: string
  threadId: string
  from: string
  subject: string
  date: string
  snippet: string
  labels: string[]
  aiprovider?: string
}

export interface MailboxPageResponse {
  emails: Email[]
  nextPageToken?: string
  totalCount: number
}

export interface ConnectResponse {
  url: string
}

export interface ApiResponse<T = any> {
  success?: boolean
  message?: string
  data?: T
  error?: string
}

export interface BotConfigPayload {
  triggerSubject?: string
  customPrompt?: string
}

// ============================================
// HELPERS
// ============================================

/**
 * Create headers with authorization token
 */
function getHeaders(token: string): HeadersInit {
  if (!token) {
    console.warn('Token is empty/null/undefined!', token)
  }

  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  }
}

/**
 * Ensure token is fresh (refresh if needed before API call)
 * Import from authStore dynamically to avoid circular dependency
 * Note: RefreshToken is stored in HttpOnly cookie, not in authStore
 */
async function ensureTokenFresh(token: string): Promise<string> {
  try {
    const { useAuthStore } = await import('@/stores/useAuthStore')
    const authStore = useAuthStore()

     // Check if token is expiring within 5 minutes
     if (authStore.isTokenExpiringSoon(token, 5 * 60 * 1000)) {
       // Refresh token is in HttpOnly cookie (auto-managed by browser)
       // Just call refreshTokenManual, it will use cookie automatically
       const refreshed = await authStore.refreshTokenManual()
       if (refreshed && authStore.accessToken) {
         return authStore.accessToken
       } else {
         console.error('❌ Token refresh failed')
         throw new Error('Token refresh failed')
       }
     }

    return token
  } catch (error) {
    console.error('❌ Error in ensureTokenFresh:', error)
    throw error
  }
}

/**
 * Handle API response
 */
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `HTTP Error: ${response.status}`)
  }
  return response.json()
}

// ============================================
// GMAIL OAUTH
// ============================================

/**
 * Start Gmail OAuth connection flow
 * @param token - User's JWT access token
 * @returns OAuth URL to redirect user to Google login
 */
export async function startGmailConnection(token: string): Promise<ConnectResponse> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/connect/start`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include', //  Send/receive cookies
  })
  return handleResponse<ConnectResponse>(response)
}

/**
 * Handle Gmail OAuth callback
 * @param code - Authorization code from Google
 * @param state - State parameter for CSRF protection
 * @param token - User's JWT access token
 * @returns Redirect URL
 */
export async function handleGmailCallback(
  code: string,
  state: string,
  token: string
): Promise<{ redirect_url: string }> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(
    `${API_BASE_URL}/connect/callback?code=${code}&state=${state}`,
    {
      method: 'GET',
      headers: getHeaders(freshToken),
      credentials: 'include', //  Send/receive cookies
    }
  )
  return handleResponse(response)
}

// ============================================
// GMAIL STATUS & CONFIGURATION
// ============================================

/**
 * Get current Gmail connection status and bot configuration
 * @param token - User's JWT access token
 * @returns Gmail status and bot config
 */
export async function getGmailStatus(token: string): Promise<GmailStatus> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/status`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include', //  Send/receive cookies
  })
  return handleResponse<GmailStatus>(response)
}

/**
 * Enable Gmail bot
 * @param token - User's JWT access token
 * @returns Success response
 */
export async function enableBot(token: string): Promise<{ status: string }> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/bot/enable`, {
    method: 'POST',
    headers: getHeaders(freshToken),
    credentials: 'include', //  Send/receive cookies
  })
  return handleResponse(response)
}

/**
 * Disable Gmail bot
 * @param token - User's JWT access token
 * @returns Success response
 */
export async function disableBot(token: string): Promise<{ status: string }> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/bot/disable`, {
    method: 'POST',
    headers: getHeaders(freshToken),
    credentials: 'include', //  Send/receive cookies
  })
  return handleResponse(response)
}

/**
 * Update bot configuration
 * @param token - User's JWT access token
 * @param config - Bot configuration
 * @returns Success response
 */
export async function updateBotConfig(
  token: string,
  config: BotConfigPayload
): Promise<{ status: string }> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/bot/config`, {
    method: 'PUT',
    headers: getHeaders(freshToken),
    credentials: 'include', // 🔐 Send/receive cookies
    body: JSON.stringify(config),
  })
  return handleResponse(response)
}

/**
 * Update custom AI prompt
 * @param token - User's JWT access token
 * @param customPrompt - Custom prompt for AI replies
 * @returns Success response
 */
export async function updateCustomPrompt(
  token: string,
  customPrompt: string
): Promise<{ status: string }> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/bot/prompt`, {
    method: 'PUT',
    headers: getHeaders(freshToken),
    credentials: 'include', // 🔐 Send/receive cookies
    body: JSON.stringify({ customPrompt }),
  })
  return handleResponse(response)
}

// ============================================
// MAILBOX OPERATIONS
// ============================================

/**
 * 🆕 Get emails from a specific mailbox with pagination
 * @param token - User's JWT access token
 * @param box - Mailbox name (INBOX, SENT, SPAM, TRASH)
 * @param pageSize - Number of emails per page (default: 5)
 * @param pageToken - Token for pagination (optional)
 * @returns Paginated email response with nextPageToken
 */
export async function getMailboxPage(
  token: string,
  box: string = 'INBOX',
  pageSize: number = 5,
  pageToken?: string
): Promise<MailboxPageResponse> {
  const freshToken = await ensureTokenFresh(token)
  const url = new URL(`${API_BASE_URL}/mailbox-page`)
  url.searchParams.append('box', box)
  url.searchParams.append('pageSize', pageSize.toString())
  if (pageToken) {
    url.searchParams.append('pageToken', pageToken)
  }

  const response = await fetch(url.toString(), {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  return handleResponse<MailboxPageResponse>(response)
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

/**
 * Get user's selected AI provider
 * @param token - User's JWT access token
 * @returns AI provider ("groq" or "gemini")
 */
export async function getAiProvider(token: string): Promise<string> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/ai-provider`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  const data = await handleResponse<{ aiProvider: string }>(response)
  return data.aiProvider
}

/**
 * Set user's selected AI provider
 * @param token - User's JWT access token
 * @param provider - "groq" or "gemini"
 * @returns Success response
 */
export async function setAiProvider(token: string, provider: string): Promise<{ status: string; aiProvider: string }> {
  const freshToken = await ensureTokenFresh(token)
  const url = new URL(`${API_BASE_URL}/ai-provider`)
  url.searchParams.append('provider', provider)

  const response = await fetch(url.toString(), {
    method: 'POST',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  return handleResponse<{ status: string; aiProvider: string }>(response)
}

/**
 * Format email date to readable string
 */
export function formatEmailDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString()
  } catch {
    return dateStr
  }
}

/**
 * Format datetime to readable string
 */
export function formatDateTime(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleString()
  } catch {
    return dateStr
  }
}

// ============================================
// CONTACTS
// ============================================

/**
 * Get list of contact emails
 * @param token - User's JWT access token
 * @returns List of unique contact emails extracted from mailbox
 */
export async function getContactsList(token: string): Promise<string[]> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_BASE_URL}/contacts`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  const data = await handleResponse<{ contacts: string[] }>(response)
   return data.contacts || []
}


