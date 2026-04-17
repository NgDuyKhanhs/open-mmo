/**
 * Reminder API Service
 * Communicates with backend reminder endpoints
 * Handles reminder CRUD operations and history tracking
 */

import { API_CONFIG } from '@/config/apiConfig'

// ============================================
// INTERFACES
// ============================================

export interface ReminderConfig {
  contactEmail: string
  enabled: boolean
  afterInactive: number
  repeatEvery?: number | null
  maxReminders: number
  sendWindowStart: string
  sendWindowEnd: string
}

export interface ReminderResponse {
  contactEmail: string
  enabled: boolean
  afterInactive: number
  repeatEvery?: number | null
  maxReminders: number
  sendWindowStart: string
  sendWindowEnd: string
  sentCount: number
  lastSentAt?: string | null
}

export interface ReminderHistory {
  id: string
  contactEmail: string
  subject: string
  status: string
  reason: string
  sentAt: string
  bodyPreview: string
  body?: string
}

export interface ReminderHistoryStats {
  total: number
  sent: number
  failed: number
  skipped: number
  successRate: number
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
 */
async function ensureTokenFresh(token: string): Promise<string> {
  try {
    const { useAuthStore } = await import('@/stores/useAuthStore')
    const authStore = useAuthStore()

    if (authStore.isTokenExpiringSoon(token, 5 * 60 * 1000)) {
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
// REMINDERS - CRUD OPERATIONS
// ============================================

/**
 * Get all reminders for current user
 * @param token - User's JWT access token
 * @returns List of reminder configurations
 */
export async function getReminders(token: string): Promise<ReminderResponse[]> {
  const freshToken = await ensureTokenFresh(token)
  const API_BASE = API_CONFIG.BASE_URL + API_CONFIG.BASE_PATH
  const response = await fetch(`${API_BASE}/gmail/reminders`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  const data = await handleResponse<{ reminders: ReminderResponse[] }>(response)
  return data.reminders || []
}

/**
 * Create a new reminder
 * @param token - User's JWT access token
 * @param reminder - Reminder configuration
 * @returns Success response
 */
export async function createReminder(
  token: string,
  reminder: ReminderConfig
): Promise<{ status: string; message: string }> {
  const freshToken = await ensureTokenFresh(token)
  const API_BASE = API_CONFIG.BASE_URL + API_CONFIG.BASE_PATH
  const response = await fetch(`${API_BASE}/gmail/reminders`, {
    method: 'POST',
    headers: getHeaders(freshToken),
    credentials: 'include',
    body: JSON.stringify(reminder),
  })
  return handleResponse(response)
}

/**
 * Update existing reminder
 * @param token - User's JWT access token
 * @param contactEmail - Contact email to update (empty string for "All Contacts")
 * @param reminder - Updated reminder configuration
 * @returns Success response
 */
export async function updateReminder(
  token: string,
  contactEmail: string,
  reminder: ReminderConfig
): Promise<{ status: string; message: string }> {
  const freshToken = await ensureTokenFresh(token)
  const API_BASE = API_CONFIG.BASE_URL + API_CONFIG.BASE_PATH
  // Handle "All Contacts" case (empty string) - use special identifier
  const emailParam = contactEmail === '' ? 'all' : encodeURIComponent(contactEmail)
  const response = await fetch(`${API_BASE}/gmail/reminders/${emailParam}`, {
    method: 'PUT',
    headers: getHeaders(freshToken),
    credentials: 'include',
    body: JSON.stringify(reminder),
  })
  return handleResponse(response)
}

/**
 * Delete a reminder
 * @param token - User's JWT access token
 * @param contactEmail - Contact email to delete (empty string for "All Contacts")
 * @returns Success response
 */
export async function deleteReminder(
  token: string,
  contactEmail: string
): Promise<{ status: string; message: string }> {
  const freshToken = await ensureTokenFresh(token)
  const API_BASE = API_CONFIG.BASE_URL + API_CONFIG.BASE_PATH
  // Handle "All Contacts" case (empty string) - use special identifier
  const emailParam = contactEmail === '' ? 'all' : encodeURIComponent(contactEmail)
  const response = await fetch(`${API_BASE}/gmail/reminders/${emailParam}`, {
    method: 'DELETE',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  return handleResponse(response)
}

// ============================================
// REMINDER HISTORY
// ============================================

/**
 * Get reminder history for current user
 * @param token - User's JWT access token
 * @returns List of reminder history records
 */
export async function getReminderHistory(token: string): Promise<ReminderHistory[]> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.BASE_PATH}/reminders/history`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  const data = await handleResponse<{ data: ReminderHistory[] }>(response)
  return data.data || []
}

/**
 * Get reminder history statistics
 * @param token - User's JWT access token
 * @returns Statistics summary
 */
export async function getReminderHistoryStats(token: string): Promise<ReminderHistoryStats> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.BASE_PATH}/reminders/history/stats/summary`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  return handleResponse<ReminderHistoryStats>(response)
}

/**
 * Get detailed reminder history record
 * @param token - User's JWT access token
 * @param historyId - History record ID
 * @returns Full reminder history record with body
 */
export async function getReminderHistoryDetail(token: string, historyId: string): Promise<ReminderHistory> {
  const freshToken = await ensureTokenFresh(token)
  const response = await fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.BASE_PATH}/reminders/history/${historyId}`, {
    method: 'GET',
    headers: getHeaders(freshToken),
    credentials: 'include',
  })
  const data = await handleResponse<{ data: ReminderHistory }>(response)
  return data.data
}

