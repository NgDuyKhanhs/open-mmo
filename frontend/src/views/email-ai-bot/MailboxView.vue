<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMailboxPagination } from '@/composables/email-ai-bot/useMailboxPagination'
import { X } from 'lucide-vue-next'

const mailbox = useMailboxPagination()

const props = defineProps<{
  gmailStatus: any
  isLoadingStatus: boolean
}>()

const showEmailPreviewModal = ref(false)

const filteredEmails = computed(() => {
  if (!props.gmailStatus?.triggerSubject || !props.gmailStatus.triggerSubject.trim()) {
    return mailbox.emails.value
  }
  const keyword = props.gmailStatus.triggerSubject.toLowerCase()
  return mailbox.emails.value.filter(email => email.subject.toLowerCase().includes(keyword))
})

onMounted(async () => {
  if (props.gmailStatus?.connected) {
    await mailbox.loadEmails()
  }
})
</script>

<template>
  <div class="tab-pane mailbox-section">
    <div class="tab-title-section">
      <h3 class="tab-title">Mailbox</h3>
      <div class="tab-divider"></div>
    </div>
    <div class="mailbox-header">
      <div class="mailbox-selector">
        <button
          v-for="box in ['inbox', 'sent', 'spam']"
          :key="box"
          @click="mailbox.selectBox(box as any)"
          :class="['mailbox-tab-btn', { active: mailbox.selectedBox.value === box }]"
          :disabled="mailbox.isLoading.value"
        >
          {{ box.charAt(0).toUpperCase() + box.slice(1) }}
        </button>
      </div>
    </div>

    <div class="email-list">
      <div v-if="mailbox.isLoading.value" class="loading-state">
        <div class="spinner"></div>
        <span>Loading emails...</span>
      </div>
      <div v-else-if="mailbox.emails.value.length === 0" class="empty-state">
        <span>No emails in {{ mailbox.selectedBox.value }}</span>
      </div>
      <div v-else-if="filteredEmails.length === 0" class="empty-state">
        <span>No emails matching "{{ gmailStatus?.triggerSubject }}"</span>
      </div>
      <div v-else class="email-items">
        <div
          v-for="email in filteredEmails"
          :key="email.id"
          @click="() => { mailbox.selectEmail(email); showEmailPreviewModal = true }"
          class="email-item"
        >
          <div v-if="email.aiprovider" class="email-provider">
            {{ email.aiprovider.charAt(0).toUpperCase() + email.aiprovider.slice(1) }}
          </div>
          <div class="email-from">{{ mailbox.extractEmail(email.from) }}</div>
          <div class="email-subject">{{ email.subject }}</div>
          <div class="email-snippet">{{ email.snippet }}</div>
          <div class="email-date">{{ mailbox.formatDate(email.date) }}</div>
        </div>
      </div>
    </div>

    <div v-if="mailbox.currentPageIndex.value > 0 || mailbox.hasNextPage.value" class="pagination-controls">
      <button
        @click="mailbox.loadPreviousPage()"
        class="pagination-btn"
        :disabled="mailbox.isLoading.value || mailbox.currentPageIndex.value <= 0"
      >
        ← Previous
      </button>
      <div class="pagination-spacer"></div>
      <button
        @click="mailbox.loadNextPage()"
        class="pagination-btn"
        :disabled="mailbox.isLoading.value || !mailbox.hasNextPage.value"
      >
        Next →
      </button>
    </div>

    <!-- Email Preview Modal -->
    <div v-if="showEmailPreviewModal && mailbox.selectedEmail.value" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">Email Preview</h3>
          <button @click="showEmailPreviewModal = false" class="close-button">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-content">
          <div class="email-preview">
            <div class="preview-field">
              <span class="label">From</span>
              <span class="value">{{ mailbox.extractEmail(mailbox.selectedEmail.value.from) }}</span>
            </div>
            <div class="preview-field">
              <span class="label">Subject</span>
              <span class="value">{{ mailbox.selectedEmail.value.subject }}</span>
            </div>
            <div class="preview-field">
              <span class="label">Date</span>
              <span class="value">{{ mailbox.formatDate(mailbox.selectedEmail.value.date) }}</span>
            </div>
            <div class="preview-field">
              <span class="label">Message</span>
              <span class="value message">{{ mailbox.selectedEmail.value.snippet }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>



