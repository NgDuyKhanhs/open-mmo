<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Mail, Settings, RefreshCw, HelpCircle, X, Search, Inbox, CheckCircle2, AlertCircle, Bot, Key, Clock, AlertTriangle, LinkIcon, Send, AlertOctagon, Zap, Gauge, Trash, Edit } from 'lucide-vue-next'
import { toast } from 'vue3-toastify'
import { useMailboxPagination } from '@/composables/email-ai-bot/useMailboxPagination'
import { useBotConfig } from '@/composables/email-ai-bot/useBotConfig'
import { useAiProvider } from '@/composables/email-ai-bot/useAiProvider'
import { useEmailBotStatus } from '@/composables/email-ai-bot/useEmailBotStatus'
import { useReminders } from '@/composables/email-ai-bot/useReminders'
import { useReminderHistory } from '@/composables/email-ai-bot/useReminderHistory'
import { type GmailStatus } from '@/services/gmailService'
import type { ReminderHistory } from '@/services/reminderService'

interface Props {
  status: GmailStatus
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'config-saved': []
}>()

const activeTab = ref<'mailbox' | 'config' | 'status' | 'reminders'>('mailbox')
const navbarExpanded = ref(true)

// Composables
const mailbox = useMailboxPagination()
const config = useBotConfig()
const provider = useAiProvider()
const { toggleBot } = useEmailBotStatus()
const reminders = useReminders()
const history = useReminderHistory()

// Modal states
const showModal = ref(false)
const showPromptHelp = ref(false)
const showSubjectKeywordWarning = ref(false)
const showEmailPreviewModal = ref(false)
const showDeleteConfirm = ref(false)
const showReminderHelp = ref(false)
const deleteConfirmEmail = ref<string>('')
const showReminderHistory = ref(false)
const historyDetailModal = ref<ReminderHistory | null>(null)
const selectedHistoryItem = ref<ReminderHistory | null>(null)

// Filter states
const filterHistoryStatus = ref('')
const filterHistoryContact = ref('')

// Initialize on component mount
onMounted(async () => {
  // Load mailbox by default
  await mailbox.loadEmails()
  // Initialize config and provider
  config.initializeConfig(props.status)
  await provider.loadAiProvider()
  // Load reminder stats on mount
  await history.loadReminderHistoryStats()
})

// Switch to mailbox tab and load emails
const switchToMailbox = async () => {
  activeTab.value = 'mailbox'
  if (mailbox.emails.value.length === 0) {
    await mailbox.loadEmails()
  }
}

// Close modals
const closeModal = () => {
  showModal.value = false
}

// Delete reminder
const confirmDelete = async () => {
  const contactEmail = deleteConfirmEmail.value
  showDeleteConfirm.value = false
  await reminders.confirmDelete(contactEmail)
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  deleteConfirmEmail.value = ''
}

// Computed
const filteredEmails = computed(() => {
  if (!props.status.triggerSubject || !props.status.triggerSubject.trim()) {
    return mailbox.emails.value
  }
  const keyword = props.status.triggerSubject.toLowerCase()
  return mailbox.emails.value.filter(email => email.subject.toLowerCase().includes(keyword))
})

// Filtered reminder histories
const filteredReminderHistories = computed(() => {
  return history.reminderHistories.value.filter(h => {
    const statusMatch = !filterHistoryStatus.value || h.status === filterHistoryStatus.value
    const contactMatch = !filterHistoryContact.value || h.contactEmail.toLowerCase().includes(filterHistoryContact.value.toLowerCase())
    return statusMatch && contactMatch
  })
})

// View reminder history detail
const viewReminderHistoryDetail = async (hist: ReminderHistory) => {
  try {
    const fullDetail = await history.viewReminderHistoryDetail(hist)
    historyDetailModal.value = fullDetail
  } catch (err) {
    console.error('Failed to fetch full detail:', err)
    toast.error('Failed to load reminder details')
  }
}

// Copy to clipboard
const copyToClipboard = (text: string) => {
  history.copyToClipboard(text)
}

// Save config and reload status
const saveConfigAndReload = async () => {
  await config.updateConfig(props.status)
  // Reload emails to apply new filter
  await mailbox.loadEmails()
  // Emit event to reload status from parent
  emit('config-saved')
}
</script>

<template>
  <div class="page-shell">
    <div class="page-container">
      <main class="main-content">
        <!-- Tab Navigation -->
        <div class="tab-header" :class="{ collapsed: !navbarExpanded }">
          <!-- Toggle Button -->
          <button
            @click="navbarExpanded = !navbarExpanded"
            class="navbar-toggle-btn"
            :title="navbarExpanded ? 'Collapse sidebar' : 'Expand sidebar'"
          >
            <svg v-if="navbarExpanded" viewBox="0 0 24 24" class="toggle-icon" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"></rect>
              <path d="M9 3v18" stroke-linecap="round"></path>
              <path d="M16 10l-3 2 3 2" stroke-linecap="round" stroke-linejoin="round"></path>
            </svg>
            <svg v-else viewBox="0 0 24 24" class="toggle-icon" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"></rect>
              <path d="M9 3v18" stroke-linecap="round"></path>
              <path d="M14 10l3 2-3 2" stroke-linecap="round" stroke-linejoin="round"></path>
            </svg>
          </button>

          <!-- Tab Buttons -->
          <button
            @click="switchToMailbox()"
            :class="['tab-button', { active: activeTab === 'mailbox' }]"
            title="View mailbox"
          >
            <Mail :size="18" class="tab-icon" />
            <span v-if="navbarExpanded" class="tab-label">Mailbox</span>
          </button>

          <button
            @click="activeTab = 'config'; config.loadCustomPrompt()"
            :class="['tab-button', { active: activeTab === 'config' }]"
            title="Bot settings"
          >
            <Settings :size="18" class="tab-icon" />
            <span v-if="navbarExpanded" class="tab-label">Config</span>
          </button>

          <button
            @click="activeTab = 'status'"
            :class="['tab-button', { active: activeTab === 'status' }]"
            title="Bot status"
          >
            <Bot :size="18" class="tab-icon" />
            <span v-if="navbarExpanded" class="tab-label">Status</span>
          </button>

          <button
            @click="activeTab = 'reminders'; reminders.loadReminders()"
            :class="['tab-button', { active: activeTab === 'reminders' }]"
            title="Email reminders"
          >
            <Clock :size="18" class="tab-icon" />
            <span v-if="navbarExpanded" class="tab-label">Reminders</span>
          </button>
        </div>

        <!-- Tab Content -->
        <div class="tab-content">
          <!-- Mailbox Tab -->
          <div v-show="activeTab === 'mailbox'" class="mailbox-section tab-pane">
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
                <span>No emails matching "{{ props.status.triggerSubject }}"</span>
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
          </div>

          <!-- Config Tab -->
          <div v-show="activeTab === 'config'" class="settings-section tab-pane">
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
          </div>

          <!-- Status Tab -->
          <div v-show="activeTab === 'status'" class="status-section tab-pane">
            <div class="tab-title-section">
              <h3 class="tab-title">Status & AI Model</h3>
              <div class="tab-divider"></div>
            </div>

            <div class="status-content">
              <div class="status-grid-modern">
                <div v-if="props.status.connected" class="modern-card bot-card">
                  <div class="card-header">
                    <Bot :size="18" class="card-icon" />
                    <span class="card-title">Bot Status</span>
                  </div>
                  <div class="card-content">
                    <div style="display: flex; align-items: center; gap: 12px;">
                      <label class="toggle-switch-large">
                        <input
                          type="checkbox"
                          :checked="props.status.botEnabled"
                          @change="toggleBot"
                        />
                        <span class="toggle-slider-large"></span>
                      </label>
                      <span :class="['status-badge-small', props.status.botEnabled ? 'active' : 'inactive']">
                        {{ props.status.botEnabled ? '● Running' : '● Paused' }}
                      </span>
                    </div>
                  </div>
                </div>

                <div v-if="props.status.lastRunAt" class="modern-card run-card">
                  <div class="card-header">
                    <Clock :size="18" class="card-icon" />
                    <span class="card-title">Last Run</span>
                  </div>
                  <div class="card-content">
                    <p class="run-time">{{ mailbox.formatDate(props.status.lastRunAt) }}</p>
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
                    @click="provider.changeAiProvider('groq')"
                    :class="['provider-card', { selected: provider.selectedAiProvider.value === 'groq' }]"
                  >
                    <div class="provider-card-header">
                      <span class="provider-name">Groq</span>
                    </div>
                    <div class="provider-details">
                      <p class="provider-feature">⚡ Fast & Cheap</p>
                      <p class="provider-feature">Recommended</p>
                    </div>
                    <div v-if="provider.selectedAiProvider.value === 'groq'" class="provider-checkmark">✓</div>
                  </div>

                  <div
                    @click="provider.changeAiProvider('gemini')"
                    :class="['provider-card', { selected: provider.selectedAiProvider.value === 'gemini' }]"
                  >
                    <div class="provider-card-header">
                      <span class="provider-name">Gemini</span>
                    </div>
                    <div class="provider-details">
                      <p class="provider-feature">🔬 Advanced</p>
                      <p class="provider-feature">Premium Model</p>
                    </div>
                    <div v-if="provider.selectedAiProvider.value === 'gemini'" class="provider-checkmark">✓</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Reminders Tab -->
          <div v-show="activeTab === 'reminders'" class="reminders-section tab-pane">
            <div class="tab-title-section">
              <h3 class="tab-title">Reminders</h3>
              <div class="tab-divider"></div>
            </div>

            <div class="reminders-content">
              <div class="reminder-form-container">
                <div class="new-reminder-header">
                  <h4 class="new-reminder-title">Create reminder</h4>
<!--                  <button-->
<!--                    @click="showReminderHelp = true"-->
<!--                    class="help-icon-btn"-->
<!--                    title="How reminders work"-->
<!--                  >-->
<!--                    ?-->
<!--                  </button>-->
                </div>

                <div class="form-section">
                  <label class="form-label">Remind after inactivity (minutes) <span class="required">*</span></label>
                  <input
                    v-model.number="reminders.newReminder.value.afterInactive"
                    type="number"
                    min="5"
                    placeholder="e.g., 60"
                    class="form-input"
                  />
                </div>

                <!-- Optional Fields Toggle -->
                <div class="form-section">
                  <label class="form-label">Additional options</label>
                  <div class="optional-fields-toggles">
                    <div>
                      <label class="toggle-option">
                        <input
                          v-model="reminders.visibleOptionalFields.value.contactEmail"
                          type="checkbox"
                          @change="() => reminders.visibleOptionalFields.value.contactEmail && reminders.loadContacts()"
                        />
                        <span>Apply to specific contact</span>
                      </label>
                      <div v-if="reminders.visibleOptionalFields.value.contactEmail" class="optional-input">
                        <div v-if="reminders.isLoadingContacts.value" class="loading-contacts">
                          <div class="spinner" style="width: 16px; height: 16px;"></div>
                          <span>Loading contacts...</span>
                        </div>
                        <select
                          v-else
                          v-model="reminders.newReminder.value.contactEmail"
                          class="form-select"
                          @mousedown="reminders.loadContacts()"
                          required
                        >
                          <option value="">Select a contact...</option>
                          <option
                            v-for="contact in reminders.contactsList.value"
                            :key="contact"
                            :value="contact"
                          >
                            {{ contact }}
                          </option>
                        </select>
                      </div>
                    </div>

                    <div>
                      <label class="toggle-option">
                        <input
                          v-model="reminders.visibleOptionalFields.value.repeatEvery"
                          type="checkbox"
                        />
                        <span>Set repeat interval</span>
                      </label>
                      <div v-if="reminders.visibleOptionalFields.value.repeatEvery" class="optional-input">
                        <input
                          v-model.number="reminders.newReminder.value.repeatEvery"
                          type="number"
                          min="1"
                          placeholder="e.g., 1440 (24 hours)"
                          class="form-input"
                        />
                        <p class="form-description">Minutes between repeat reminders (default: 1440 = 24 hours)</p>
                      </div>
                    </div>

                    <div>
                      <label class="toggle-option">
                        <input
                          v-model="reminders.visibleOptionalFields.value.maxReminders"
                          type="checkbox"
                        />
                        <span>Set max reminders</span>
                      </label>
                      <div v-if="reminders.visibleOptionalFields.value.maxReminders" class="optional-input">
                        <input
                          v-model.number="reminders.newReminder.value.maxReminders"
                          type="number"
                          min="1"
                          placeholder="e.g., 5"
                          class="form-input"
                        />
                        <p class="form-description">Maximum number of reminders to send</p>
                      </div>
                    </div>

                    <div>
                      <label class="toggle-option">
                        <input
                          v-model="reminders.visibleOptionalFields.value.enabled"
                          type="checkbox"
                        />
                        <span>Enable/Disable</span>
                      </label>
                      <div v-if="reminders.visibleOptionalFields.value.enabled" class="optional-input">
                        <label class="toggle-switch">
                          <input v-model="reminders.newReminder.value.enabled" type="checkbox" />
                          <span class="slider"></span>
                          <span class="label-text">{{ reminders.newReminder.value.enabled ? 'Enabled' : 'Disabled' }}</span>
                        </label>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="reminder-action-buttons">
                  <button @click="reminders.saveReminder()" class="btn btn-action-save" :disabled="reminders.isSavingReminder.value">
                    {{ reminders.isSavingReminder.value ? 'Saving...' : 'Save' }}
                  </button>
                  <button @click="reminders.resetReminderForm()" class="btn btn-action-cancel">Cancel</button>
                </div>
              </div>

              <div class="configured-reminders">
                <div class="reminders-section-header">
                  <h4 class="reminders-section-title">Configuration</h4>
                  <span class="reminders-count">({{ reminders.reminders.value.length }})</span>
                </div>

                <div v-if="reminders.reminders.value.length > 0" class="reminders-table-wrapper">
                  <table class="reminders-table">
                    <thead>
                      <tr>
                        <th class="col-email">EMAIL</th>
                        <th class="col-status">STATUS</th>
                        <th class="col-schedule">WAIT (min)</th>
                        <th class="col-repeat">REPEAT (min)</th>
                        <th class="col-max">REMINDERS</th>
                        <th class="col-actions">ACTIONS</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="reminder in reminders.reminders.value" :key="reminder.contactEmail">
                        <td class="email-cell col-email">{{ reminder.contactEmail || 'All Emails' }}</td>
                        <td class="status-cell col-status">
                          <span :class="['status-badge', reminder.enabled ? 'active' : 'inactive']">
                            {{ reminder.enabled ? '● Active' : '● Inactive' }}
                          </span>
                        </td>
                        <td class="schedule-cell col-schedule">{{ reminder.afterInactive }}</td>
                        <td class="schedule-cell col-repeat">{{ reminder.repeatEvery || 1440 }}</td>
                        <td
                          :class="['max-cell', 'col-max', { 'max-reached': reminder.sentCount >= reminder.maxReminders }]"
                        >
                          <span class="reminders-progress">{{ reminder.sentCount }}/{{ reminder.maxReminders }}</span>
                        </td>
                        <td class="actions-cell col-actions">
                          <div class="actions-group">
                            <button @click="reminders.editReminder(reminder)" class="action-btn edit-btn" title="Edit">✎</button>
                            <button @click="reminders.showDeleteConfirmPopup(reminder.contactEmail)" class="action-btn delete-btn" title="Delete">🗑</button>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="no-reminders">
                  <p>No reminders configured yet</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Reminder History Modal -->
        <div v-if="showReminderHistory" class="modal-overlay">
          <div class="modal-container">
            <div class="modal-header">
              <h3 class="modal-title">Reminder History</h3>
              <button @click="showReminderHistory = false" class="close-button">
                <X :size="18" />
              </button>
            </div>

            <div class="modal-content">
              <div class="history-filters">
                <div class="filter-group">
                  <label class="filter-label">Status</label>
                  <select v-model="filterHistoryStatus" class="filter-select">
                    <option value="">All</option>
                    <option value="sent">Sent</option>
                    <option value="failed">Failed</option>
                    <option value="pending">Pending</option>
                  </select>
                </div>

                <div class="filter-group">
                  <label class="filter-label">Contact</label>
                  <input
                    v-model="filterHistoryContact"
                    type="text"
                    placeholder="Search by contact"
                    class="filter-input"
                  />
                </div>
              </div>

              <div class="history-list">
                <div v-if="history.isLoading" class="loading-state">
                  <div class="spinner"></div>
                  <span>Loading history...</span>
                </div>
                <div v-else-if="filteredReminderHistories.length === 0" class="empty-state">
                  <span>No reminder history found</span>
                </div>
                <div v-else class="history-items">
                  <div
                    v-for="item in filteredReminderHistories"
                    :key="item.id"
                    class="history-item"
                  >
                    <div class="history-item-content">
                      <div class="history-item-header">
                        <span class="history-item-status" :class="item.status">{{ item.status }}</span>
                        <span class="history-item-date">{{ mailbox.formatDate(item.createdAt) }}</span>
                      </div>
                      <div class="history-item-body">
                        <div class="history-item-contact">
                          <strong>Contact:</strong> {{ item.contactEmail }}
                        </div>
                        <div class="history-item-reminder">
                          <strong>Reminder:</strong> {{ item.reminderText }}
                        </div>
                      </div>
                    </div>
                    <div class="history-item-actions">
                      <button @click="viewReminderHistoryDetail(item)" class="btn btn-link">
                        View Details
                      </button>
                      <button @click="copyToClipboard(item.contactEmail)" class="btn btn-link">
                        Copy Email
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Reminder History Detail Modal -->
        <div v-if="historyDetailModal" class="modal-overlay">
          <div class="modal-container modal-detail">
            <div class="modal-header">
              <h3 class="modal-title">Reminder History Detail</h3>
              <button @click="historyDetailModal = null" class="close-button">
                <X :size="18" />
              </button>
            </div>

            <div class="modal-content">
              <div class="detail-section">
                <h4 class="detail-title">Summary</h4>
                <div class="detail-item">
                  <span class="detail-label">Contact:</span>
                  <span class="detail-value">{{ historyDetailModal.contactEmail }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Status:</span>
                  <span class="detail-value" :class="historyDetailModal.status">{{ historyDetailModal.status }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Next Reminder:</span>
                  <span class="detail-value">
                    {{ historyDetailModal.nextReminderAt ? mailbox.formatDate(historyDetailModal.nextReminderAt) : 'N/A' }}
                  </span>
                </div>
              </div>

              <div class="detail-section">
                <h4 class="detail-title">Reminder Content</h4>
                <div class="detail-item">
                  <span class="detail-label">Subject:</span>
                  <span class="detail-value">{{ historyDetailModal.subject }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">Message:</span>
                  <span class="detail-value">{{ historyDetailModal.message }}</span>
                </div>
              </div>

              <div class="detail-actions">
                <button @click="copyToClipboard(historyDetailModal.message)" class="btn btn-primary">
                  Copy Message
                </button>
                <button @click="showReminderHistory = true; historyDetailModal = null" class="btn btn-secondary">
                  Back to History
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Custom Prompt Help Modal -->
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
                <p class="help-text">
                  Instructions for how the AI should respond to emails. Define tone, style, and format.
                </p>
              </div>

              <div class="help-section">
                <h4 class="help-title">Examples</h4>
                <div class="example-box">
                  <p class="example-content">"Keep responses professional and under 100 words"</p>
                </div>
                <div class="example-box">
                  <p class="example-content">"Be friendly and casual, like talking to colleagues"</p>
                </div>
              </div>

              <div class="help-actions">
                <button @click="showPromptHelp = false" class="btn btn-primary">
                  Got it
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Subject Keyword Warning Modal -->
        <div v-if="showSubjectKeywordWarning" class="modal-overlay">
          <div class="modal-container">
            <div class="modal-header">
              <h3 class="modal-title">Subject Keyword Filter</h3>
              <button @click="showSubjectKeywordWarning = false" class="close-button">
                <X :size="18" />
              </button>
            </div>

            <div class="modal-content">
              <p class="help-text">
                Only auto-reply to emails with specific keywords in the subject. Leave empty to reply to all emails.
              </p>
              <p class="help-text" style="margin-top: 12px; font-size: 12px; color: rgba(232, 247, 255, 0.65);">
                Examples: "invoice", "support", "urgent"
              </p>

              <div class="help-actions" style="margin-top: 20px;">
                <button @click="showSubjectKeywordWarning = false" class="btn btn-primary">
                  Understood
                </button>
              </div>
            </div>
          </div>
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

        <!-- Delete Reminder Confirm Modal -->
        <div v-if="reminders.showDeleteConfirm.value" class="modal-overlay">
          <div class="modal-container" style="width: min(400px, 90vw);">
            <div class="modal-header">
              <h3 class="modal-title">Delete Reminder</h3>
              <button @click="reminders.cancelDelete()" class="close-button">
                <X :size="18" />
              </button>
            </div>

            <div class="modal-content">
              <p class="delete-message">
                Are you sure you want to delete the reminder for
                <strong>{{ reminders.deleteConfirmEmail.value || 'All Emails' }}</strong>?
              </p>
              <p class="delete-warning">This action cannot be undone.</p>

              <div class="detail-actions" style="margin-top: 24px;">
                <button @click="reminders.confirmDelete()" class="btn btn-delete">
                  Delete
                </button>
                <button @click="reminders.cancelDelete()" class="btn btn-secondary">
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Reminder Help Modal -->
        <div v-if="showReminderHelp" class="modal-overlay">
          <div class="modal-container reminder-help-modal" style="max-width: 600px;">
            <div class="modal-header">
              <h3 class="modal-title">How Reminders Work</h3>
              <button @click="showReminderHelp = false" class="close-button">
                <X :size="18" />
              </button>
            </div>

            <div class="modal-content">
              <div class="help-section">
                <h4 class="help-title">📋 First Reminder</h4>
                <p class="help-text">
                  <strong>Remind after inactivity:</strong> Bot waits for this many minutes of conversation inactivity before sending the first reminder.
                </p>
                <div class="help-example">
                  <strong>Example:</strong> If set to 60 minutes, bot sends first reminder when user hasn't replied for 60+ minutes.
                </div>
              </div>

              <div class="help-section">
                <h4 class="help-title">🔄 Repeat Reminders</h4>
                <p class="help-text">
                  <strong>Repeat every:</strong> After first reminder, bot sends follow-ups at this interval, regardless of conversation activity.
                </p>
                <div class="help-example">
                  <strong>Example:</strong> If set to 1440 minutes (24 hours), bot sends reminder every day. Even if user replies, next reminder still sends 24 hours after the previous one.
                </div>
              </div>

              <div class="help-section">
                <h4 class="help-title">🎯 Max Reminders</h4>
                <p class="help-text">
                  <strong>Maximum reminders:</strong> Total number of reminders to send before stopping.
                </p>
                <div class="help-example">
                  <strong>Example:</strong> If set to 5, bot sends at most 5 reminders. After that, no more reminders are sent.
                </div>
              </div>

              <div class="help-section">
                <h4 class="help-title">📅 Timeline Example</h4>
                <div class="timeline-example">
                  <div class="timeline-item">
                    <span class="timeline-time">Day 1 - 11:00</span>
                    <span class="timeline-event">Send Reminder #1 (after 60 min inactivity)</span>
                  </div>
                  <div class="timeline-item">
                    <span class="timeline-time">Day 1 - 12:00</span>
                    <span class="timeline-event">User replies (conversation active again)</span>
                  </div>
                  <div class="timeline-item">
                    <span class="timeline-time">Day 2 - 11:00</span>
                    <span class="timeline-event">Send Reminder #2 (24 hours after #1, even though user replied)</span>
                  </div>
                  <div class="timeline-item">
                    <span class="timeline-time">Day 3-5 - 11:00</span>
                    <span class="timeline-event">Send Reminders #3, #4, #5 (then stop)</span>
                  </div>
                </div>
              </div>

              <div class="help-note">
                💡 <strong>Note:</strong> Repeat interval is based on when bot sends reminders, not on conversation activity. This ensures regular follow-ups even if user keeps replying.
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* Minimal scoped styles - most CSS is in global file */
</style>

