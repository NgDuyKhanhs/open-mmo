<script setup lang="ts">
  import { ref, nextTick } from 'vue'
  import { CHAT_CONFIG, getRandomBotResponse } from '@/config'

  interface ChatMessage {
    id: string
    type: 'user' | 'bot'
    content: string
    timestamp: Date
  }

  const messages = ref<ChatMessage[]>([
    {
      id: '1',
      type: 'bot',
      content: CHAT_CONFIG.initialMessage,
      timestamp: new Date(),
    },
  ])
  const inputValue = ref('')
  const isLoading = ref(false)
  const messagesContainer = ref<HTMLElement>()

  const scrollToBottom = () => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }

  const sendMessage = async () => {
    if (!inputValue.value.trim()) return

    // Add user message
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      type: 'user',
      content: inputValue.value,
      timestamp: new Date(),
    }
    messages.value.push(userMessage)
    inputValue.value = ''

    // Simulate bot thinking
    isLoading.value = true
    await nextTick()
    scrollToBottom()

    // Simulate API response delay
    setTimeout(() => {
      const randomResponse = getRandomBotResponse()

      const botMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        type: 'bot',
        content: randomResponse,
        timestamp: new Date(),
      }
      messages.value.push(botMessage)
      isLoading.value = false
      scrollToBottom()
    }, CHAT_CONFIG.responseDelay)

    nextTick(() => {
      scrollToBottom()
    })
  }

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
  }
</script>

<template>
  <div class="chatbot-wrapper">
    <!-- Chat Window -->
    <div class="chat-window">
      <!-- Header -->
      <div class="chat-header">
        <div class="chat-header-content">
          <h3>{{ CHAT_CONFIG.header }}</h3>
        </div>
      </div>

      <!-- Messages -->
      <div class="chat-messages" ref="messagesContainer">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-group"
          :class="message.type"
        >
          <div class="message-bubble">
            <p>{{ message.content }}</p>
          </div>
          <span class="message-time">{{ formatTime(message.timestamp) }}</span>
        </div>

        <!-- Loading Indicator -->
        <div v-if="isLoading" class="message-group bot">
          <div class="message-bubble">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <textarea
          v-model="inputValue"
          :placeholder="CHAT_CONFIG.placeholder"
          @keydown="handleKeyDown"
          :disabled="isLoading"
          class="chat-input"
        ></textarea>
        <button class="send-btn" @click="sendMessage" :disabled="!inputValue.trim() || isLoading">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M16.6915026,12.4744748 L3.50612381,13.2599618 C3.19218622,13.2599618 3.03521743,13.4170592 3.03521743,13.5741566 L1.15159189,20.0151496 C0.8376543,20.8006365 0.99,21.89 1.77946707,22.52 C2.41,22.99 3.50612381,23.1 4.13399899,22.8429026 L21.714504,14.0454487 C22.6563168,13.5741566 23.1272231,12.6315722 22.9702544,11.6889879 L4.13399899,1.16346272 C3.34915502,0.9 2.40734225,1.00636533 1.77946707,1.4776575 C0.994623095,2.10604706 0.837654326,3.0486314 1.15159189,3.99 L3.03521743,10.4310621 C3.03521743,10.5881595 3.19218622,10.7452569 3.50612381,10.7452569 L16.6915026,11.5307438 C16.6915026,11.5307438 17.1624089,11.5307438 17.1624089,12.0020359 C17.1624089,12.4744748 16.6915026,12.4744748 16.6915026,12.4744748 Z"
              fill="currentColor"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .chatbot-wrapper {
    width: 100%;
    font-family: 'Comfortaa', sans-serif;
  }

  /* Chat Window */
  .chat-window {
    width: 100%;
    height: 500px;
    background: linear-gradient(135deg, rgba(10, 10, 18, 0.95) 0%, rgba(10, 10, 18, 0.85) 100%);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(0, 240, 255, 0.2);
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    animation: slideUp 0.3s ease-out;
  }

  @keyframes slideUp {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* Chat Header */
  .chat-header {
    padding: 16px;
    border-bottom: 1px solid rgba(0, 240, 255, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.05) 0%, rgba(255, 0, 212, 0.05) 100%);
  }

  .chat-header-content {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .chat-header-content h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--primary);
    letter-spacing: 0.5px;
  }

  .status-badge {
    display: inline-block;
    width: 8px;
    height: 8px;
    background: #00ff00;
    border-radius: 50%;
    box-shadow: 0 0 8px rgba(0, 255, 0, 0.6);
  }

  .close-btn {
    background: none;
    border: none;
    color: rgba(255, 255, 255, 0.6);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px;
    transition: all 0.3s ease;
  }

  .close-btn:hover {
    color: #00f0ff;
  }

  /* Messages Container */
  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .chat-messages::-webkit-scrollbar {
    width: 6px;
  }

  .chat-messages::-webkit-scrollbar-track {
    background: transparent;
  }

  .chat-messages::-webkit-scrollbar-thumb {
    background: rgba(0, 240, 255, 0.3);
    border-radius: 3px;
  }

  .chat-messages::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 240, 255, 0.5);
  }

  /* Message Bubble */
  .message-group {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    animation: messageSlide 0.3s ease-out;
  }

  @keyframes messageSlide {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .message-group.user {
    justify-content: flex-end;
  }

  .message-group.bot {
    justify-content: flex-start;
  }

  .message-bubble {
    max-width: 75%;
    padding: 10px 14px;
    border-radius: 12px;
    word-wrap: break-word;
    line-height: 1.5;
  }

  .message-group.user .message-bubble {
    border: 1px solid rgba(0, 240, 255, 0.3);
    color: #ffffff;
    font-size: 13px;
  }

  .message-group.bot .message-bubble {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(0, 240, 255, 0.15);
    color: rgba(255, 255, 255, 0.8);
    font-size: 13px;
  }

  .message-bubble p {
    margin: 0;
  }

  .message-time {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.4);
    min-width: fit-content;
  }

  /* Typing Indicator */
  .typing-indicator {
    display: flex;
    gap: 4px;
  }

  .typing-indicator span {
    width: 6px;
    height: 6px;
    background: rgba(0, 240, 255, 0.6);
    border-radius: 50%;
    animation: typing 1.4s infinite;
  }

  .typing-indicator span:nth-child(2) {
    animation-delay: 0.2s;
  }

  .typing-indicator span:nth-child(3) {
    animation-delay: 0.4s;
  }

  @keyframes typing {
    0%,
    60%,
    100% {
      opacity: 0.3;
      transform: translateY(0);
    }
    30% {
      opacity: 1;
      transform: translateY(-8px);
    }
  }

  /* Input Area */
  .chat-input-area {
    display: flex;
    align-items: center;
    padding: 12px;
    border-top: 1px solid rgba(0, 240, 255, 0.1);
    display: flex;
    gap: 8px;
    background: rgba(0, 240, 255, 0.02);
  }

  .chat-input {
    flex: 1;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(0, 240, 255, 0.15);
    color: #ffffff;
    padding: 10px 12px;
    border-radius: 8px;
    font-family: 'Comfortaa', sans-serif;
    font-size: 13px;
    resize: none;
    max-height: 80px;
    transition: all 0.3s ease;
  }

  .chat-input::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }

  .chat-input:focus {
    outline: none;
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(0, 240, 255, 0.3);
    box-shadow: 0 0 12px rgba(0, 240, 255, 0.1);
  }

  .chat-input:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  /* Send Button */
  .send-btn {
    width: 40px;
    height: 40px;
    border: none;
    color: #ffffff;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    background: transparent;
  }

  .send-btn:hover:not(:disabled) {
    transform: scale(1.05);
    box-shadow: 0 4px 12px rgba(0, 240, 255, 0.3);
  }

  .send-btn:active:not(:disabled) {
    transform: scale(0.95);
  }

  .send-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .chat-window {
      height: 450px;
    }
  }
</style>
