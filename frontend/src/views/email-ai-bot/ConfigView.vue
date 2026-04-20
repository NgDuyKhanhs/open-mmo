<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBotConfig } from '@/composables/email-ai-bot/useBotConfig'
import { useMailboxPagination } from '@/composables/email-ai-bot/useMailboxPagination'
import { toast } from 'vue3-toastify'
import { AlertTriangle, HelpCircle, X } from 'lucide-vue-next'

const mailbox = useMailboxPagination()
const config = useBotConfig()

const props = defineProps<{
  gmailStatus: any
  isLoadingStatus: boolean
}>()

const emit = defineEmits<{
  'config-saved': []
}>()

const showPromptHelp = ref(false)
const showSubjectKeywordWarning = ref(false)

const saveConfigAndReload = async () => {
  try {
    await config.updateConfig(props.gmailStatus)
    await mailbox.loadEmails()
    // Emit event to trigger parent to reload Gmail status (which includes triggerSubject)
    emit('config-saved')
    toast.success('Configuration saved! Filter applied to emails.')
  } catch (err) {
    toast.error('Failed to save configuration')
  }
}

onMounted(async () => {
  config.initializeConfig(props.gmailStatus)
  await config.loadCustomPrompt()
})
</script>

<template>
  <div class="tab-pane settings-section">
    <div class="tab-title-section">
      <h3 class="tab-title">Configuration</h3>
      <div class="tab-divider"></div>
    </div>

    <div class="config-form">
      <div class="optional-box">
        <div class="optional-box-header">
          <label class="form-label">Subject Keyword <span class="optional-badge">(Optional)</span></label>
          <button
            @click="showSubjectKeywordWarning = true"
            class="warning-icon"
            type="button"
            title="View info about Subject Keyword"
          >
            <AlertTriangle :size="16" />
          </button>
        </div>
        <input
          v-model="config.configSubject.value"
          type="text"
          placeholder="e.g., invoice, urgent, follow-up"
          class="form-input"
        />
      </div>

      <div class="optional-box">
        <div class="optional-box-header">
          <label class="form-label">Custom Prompt <span class="optional-badge">(Optional)</span></label>
          <button
            @click="showPromptHelp = true"
            class="help-button"
            title="View help for custom prompt"
          >
            <HelpCircle :size="16" />
          </button>
        </div>
        <textarea
          v-model="config.configPrompt.value"
          placeholder="Write a custom prompt for AI responses..."
          class="form-textarea"
        ></textarea>
      </div>

      <div class="form-actions">
        <button
          @click="saveConfigAndReload()"
          class="btn btn-primary btn-full"
          :disabled="config.isSaving.value || !config.hasConfigChanged.value"
        >
          {{ config.isSaving.value ? 'Saving...' : 'Save' }}
        </button>
      </div>
    </div>

    <!-- Modals -->
    <div v-if="showPromptHelp" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">Custom Prompt Help</h3>
          <button @click="showPromptHelp = false" class="close-button">
            <X :size="18" />
          </button>
        </div>
        <div class="modal-content">
          <div class="help-section">
            <h4 class="help-title">What is it?</h4>
            <p class="help-text">Instructions for how the AI should respond to emails. Define tone, style, and format.</p>
          </div>
          <div class="help-section">
            <h4 class="help-title">Examples</h4>
            <div class="example-box"><p class="example-content">"Keep responses professional and under 100 words"</p></div>
            <div class="example-box"><p class="example-content">"Be friendly and casual, like talking to colleagues"</p></div>
          </div>
          <div class="help-actions">
            <button @click="showPromptHelp = false" class="btn btn-primary">Got it</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showSubjectKeywordWarning" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">Subject Keyword Filter</h3>
          <button @click="showSubjectKeywordWarning = false" class="close-button">
            <X :size="18" />
          </button>
        </div>
        <div class="modal-content">
          <p class="help-text">Only auto-reply to emails with specific keywords in the subject. Leave empty to reply to all emails.</p>
          <p class="help-text" style="margin-top: 12px; font-size: 12px; color: rgba(232, 247, 255, 0.65);">Examples: "invoice", "support", "urgent"</p>
          <div class="help-actions" style="margin-top: 20px;">
            <button @click="showSubjectKeywordWarning = false" class="btn btn-primary">Understood</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>



