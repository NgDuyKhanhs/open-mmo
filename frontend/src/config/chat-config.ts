/**
 * Chat Assistant Configuration
 * Bot responses and chat settings
 */

export const BOT_RESPONSES = [
  'I am happy to help. What would you like advice on?',
  'We build outstanding digital experiences. Do you have a project in mind?',
  'Would you like to learn more about our services? Explore your options with us.',
  'Our team combines innovation with human-centered design. How can we support you?',
  'I can help with information about our services, portfolio, and team.',
]

export const CHAT_CONFIG = {
  initialMessage: 'How can I help you today?',
  placeholder: 'Type your message...',
  header: 'Assistant',
  responseDelay: 600, // milliseconds
}

export function getRandomBotResponse(): string {
  return BOT_RESPONSES[Math.floor(Math.random() * BOT_RESPONSES.length)]!
}
