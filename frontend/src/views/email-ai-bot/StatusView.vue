<script setup lang="ts">
import { useAiProvider } from '@/composables/email-ai-bot/useAiProvider'
import { useGmailMetaStore } from '@/stores/useGmailMetaStore'
import { useMailboxPagination } from '@/composables/email-ai-bot/useMailboxPagination'
import { Bot, Clock } from 'lucide-vue-next'

const provider = useAiProvider()
const gmailMetaStore = useGmailMetaStore()
const mailbox = useMailboxPagination()

const props = defineProps<{
  gmailStatus: any
  isLoadingStatus: boolean
  botToggleState: boolean
}>()

const emit = defineEmits<{
  'bot-toggle': [event: Event]
}>()

// Load initial AI provider from store if available
if (gmailMetaStore.aiProvider) {
  provider.selectedAiProvider.value = gmailMetaStore.aiProvider
}
</script>

<template>
  <div class="tab-pane status-section">
    <div class="tab-title-section">
      <h3 class="tab-title">Status & AI Model</h3>
      <div class="tab-divider"></div>
    </div>

    <div class="status-content">
      <div class="status-grid-modern">
        <div v-if="gmailStatus?.connected" class="modern-card bot-card">
          <div class="card-header">
            <Bot :size="18" class="card-icon" />
            <span class="card-title">Bot Status</span>
          </div>
          <div class="card-content">
            <div style="display: flex; align-items: center; gap: 12px;">
              <label class="toggle-switch-large">
                <input
                  :checked="botToggleState"
                  type="checkbox"
                  @change="emit('bot-toggle', $event)"
                />
                <span class="toggle-slider-large"></span>
              </label>
              <span :class="['status-badge-small', botToggleState ? 'active' : 'inactive']">
                {{ botToggleState ? 'Running' : 'Paused' }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="gmailStatus?.lastRunAt" class="modern-card run-card">
          <div class="card-header">
            <Clock :size="18" class="card-icon" />
            <span class="card-title">Last Run</span>
          </div>
          <div class="card-content">
            <p class="run-time">{{ mailbox.formatDate(gmailStatus.lastRunAt) }}</p>
          </div>
        </div>
      </div>

      <div class="ai-provider-section">
        <div class="provider-header">
          <h4 class="provider-title">AI Model Selection</h4>
          <p class="provider-description">Choose which AI model to use for auto-replies</p>
        </div>

        <div class="provider-grid">
          <div
            @click="provider.changeAiProvider('gemini')"
            :class="['provider-card', { selected: provider.selectedAiProvider.value === 'gemini' }]"
          >
            <div class="provider-card-header">
              <span class="provider-name">Gemini</span>
            </div>
            <div class="provider-details">
              <p class="provider-feature">🔬 Advanced</p>
              <p class="provider-feature">Recommended</p>
            </div>
            <div v-if="provider.selectedAiProvider.value === 'gemini'" class="provider-checkmark">✓</div>
          </div>

          <div
            @click="provider.changeAiProvider('groq')"
            :class="['provider-card', { selected: provider.selectedAiProvider.value === 'groq' }]"
          >
            <div class="provider-card-header">
              <span class="provider-name">Groq</span>
            </div>
            <div class="provider-details">
              <p class="provider-feature">⚡ Fast & Cheap</p>
              <p class="provider-feature">Alternative</p>
            </div>
            <div v-if="provider.selectedAiProvider.value === 'groq'" class="provider-checkmark">✓</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>



