/**
 * API Configuration
 * Centralized configuration for all API endpoints
 * Uses environment variables for flexibility in deployment
 */

// Get API base URL from environment variable or use a default
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const API_BASE_PATH = import.meta.env.VITE_API_BASE || '/api/v1'

// Build full API endpoints
export const API_CONFIG = {
  BASE_URL: API_BASE_URL,
  BASE_PATH: API_BASE_PATH,

  // Auth endpoints
  AUTH: {
    BASE: `${API_BASE_URL}${API_BASE_PATH}/auth`,
    REGISTER: `${API_BASE_URL}${API_BASE_PATH}/auth/register`,
    LOGIN: `${API_BASE_URL}${API_BASE_PATH}/auth/login`,
    GOOGLE_LOGIN: `${API_BASE_URL}${API_BASE_PATH}/auth/google-login`,
    REFRESH_TOKEN: `${API_BASE_URL}${API_BASE_PATH}/auth/refresh-token`,
    PROFILE: `${API_BASE_URL}${API_BASE_PATH}/auth/profile`,
    CHANGE_PASSWORD: `${API_BASE_URL}${API_BASE_PATH}/auth/change-password`,
    LOGOUT: `${API_BASE_URL}${API_BASE_PATH}/auth/logout`,
  },

   // Gmail endpoints
   GMAIL: {
     BASE: `${API_BASE_URL}${API_BASE_PATH}/gmail`,
     CONNECT_START: `${API_BASE_URL}${API_BASE_PATH}/gmail/connect/start`,
     CONNECT_CALLBACK: `${API_BASE_URL}${API_BASE_PATH}/gmail/connect/callback`,
     STATUS: `${API_BASE_URL}${API_BASE_PATH}/gmail/status`,
     BOT_ENABLE: `${API_BASE_URL}${API_BASE_PATH}/gmail/bot/enable`,
     BOT_DISABLE: `${API_BASE_URL}${API_BASE_PATH}/gmail/bot/disable`,
     BOT_CONFIG: `${API_BASE_URL}${API_BASE_PATH}/gmail/bot/config`,
     BOT_PROMPT: `${API_BASE_URL}${API_BASE_PATH}/gmail/bot/prompt`,
     MAILBOX: `${API_BASE_URL}${API_BASE_PATH}/gmail/mailbox`,
     REMINDERS: `${API_BASE_URL}${API_BASE_PATH}/gmail/reminders`,
     SEND_REPLY: `${API_BASE_URL}${API_BASE_PATH}/gmail/send-reply`,
     CONTACTS: `${API_BASE_URL}${API_BASE_PATH}/gmail/contacts`,
   },
 }

 export default API_CONFIG

