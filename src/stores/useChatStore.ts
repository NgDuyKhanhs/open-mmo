/**
 * Chat Store
 * Centralized chat message management and bot logic
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { CHAT_CONFIG, getRandomBotResponse } from '@/config'

export interface ChatMessage {
  id: string
  type: 'user' | 'bot'
  content: string
  timestamp: Date
}

export const useChatStore = defineStore('chat', () => {
  // State
  const messages = ref<ChatMessage[]>([
    {
      id: '1',
      type: 'bot',
      content: CHAT_CONFIG.initialMessage,
      timestamp: new Date(),
    },
  ])
  const isLoading = ref(false)
  const inputValue = ref('')

  // Computed
  const messageCount = computed(() => messages.value.length)
  const userMessages = computed(() => messages.value.filter((m) => m.type === 'user'))
  const botMessages = computed(() => messages.value.filter((m) => m.type === 'bot'))
  const hasMessages = computed(() => messages.value.length > 0)

  // Actions
  const addUserMessage = (content: string) => {
    if (!content.trim()) return

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      type: 'user',
      content: content.trim(),
      timestamp: new Date(),
    }
    messages.value.push(userMessage)
    inputValue.value = ''

    return userMessage
  }

  const addBotMessage = (content: string) => {
    const botMessage: ChatMessage = {
      id: (Date.now() + 1).toString(),
      type: 'bot',
      content,
      timestamp: new Date(),
    }
    messages.value.push(botMessage)
    return botMessage
  }

  const getRandomBotReply = (): string => {
    return getRandomBotResponse()
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const setInputValue = (value: string) => {
    inputValue.value = value
  }

  const clearMessages = () => {
    messages.value = [
      {
        id: '1',
        type: 'bot',
        content: CHAT_CONFIG.initialMessage,
        timestamp: new Date(),
      },
    ]
  }

  /**
   * Simulate a bot conversation flow
   * Handles user input -> bot thinking -> bot response
   */
  const simulateConversation = async (userInput: string) => {
    // Add user message
    addUserMessage(userInput)

    // Start loading
    setLoading(true)

    // Simulate API delay
    await new Promise((resolve) => setTimeout(resolve, CHAT_CONFIG.responseDelay))

    // Add bot response
    const botReply = getRandomBotReply()
    addBotMessage(botReply)

    // Stop loading
    setLoading(false)
  }

  return {
    // State
    messages,
    isLoading,
    inputValue,

    // Computed
    messageCount,
    userMessages,
    botMessages,
    hasMessages,

    // Actions
    addUserMessage,
    addBotMessage,
    getRandomBotReply,
    setLoading,
    setInputValue,
    clearMessages,
    simulateConversation,
  }
})
