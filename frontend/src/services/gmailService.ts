/**
 * Gmail API Service
 * Communicates with backend at http://localhost:8080/api/v1/gmail
 * Handles Gmail OAuth, bot configuration, and mailbox operations
 */

const API_BASE_URL = 'http://localhost:8080/api/v1/gmail'

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
      console.log('⚠️  Token expiring soon, attempting refresh...')

      // Refresh token is in HttpOnly cookie (auto-managed by browser)
      // Just call refreshTokenManual, it will use cookie automatically
      const refreshed = await authStore.refreshTokenManual()
      if (refreshed && authStore.accessToken) {
        console.log('✅ Token refreshed successfully')
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
 * Get emails from a specific mailbox
 * @param token - User's JWT access token
 * @param box - Mailbox name (INBOX, SENT, SPAM, TRASH)
 * @param max - Maximum number of emails to fetch (default: 20)
 * @returns Array of emails
 */
export async function getMailbox(
  token: string,
  box: string = 'INBOX',
  max: number = 20
): Promise<Email[]> {
  const freshToken = await ensureTokenFresh(token)
  const url = new URL(`${API_BASE_URL}/mailbox`)
  url.searchParams.append('box', box)
  url.searchParams.append('max', max.toString())

  const response = await fetch(url.toString(), {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include', //  Send/receive cookies
  })
  return handleResponse<Email[]>(response)
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

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

