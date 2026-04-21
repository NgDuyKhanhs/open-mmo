<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshCw, Clock, X, ChevronDown } from 'lucide-vue-next'
import { toast } from 'vue3-toastify'
import { useReminderHistory } from '@/composables/email-ai-bot/useReminderHistory'
import type { ReminderHistory } from '@/services/reminderService.ts'

const router = useRouter()
const history = useReminderHistory()

// ...existing code...

const props = defineProps<{
  gmailStatus: any
  isLoadingStatus: boolean
  botToggleState: boolean
}>()

// Modal states
const historyDetailModal = computed(() => history.historyDetailModal.value)

// Filter states
const filterHistoryStatus = ref('')
const filterHistoryContact = ref('')
const filterHistorySubject = ref('')

// Stats panel state
const isStatsExpanded = ref(false)

// Pagination
const historyPage = ref(1)
const itemsPerPage = 10

// ...existing code...

// Stats from API
const totalReminders = computed(() => {
  return history.reminderHistoryStats.value?.total || 0
})

const sentCount = computed(() => {
  return history.reminderHistoryStats.value?.sent || 0
})

const failedCount = computed(() => {
  return history.reminderHistoryStats.value?.failed || 0
})

const pendingCount = computed(() => {
  return history.reminderHistoryStats.value?.skipped || 0
})

const successRate = computed(() => {
  return history.reminderHistoryStats.value?.successRate || 0
})

// Computed
const filteredReminderHistories = computed(() => {
  return history.reminderHistories.value.filter(h => {
    const statusMatch = !filterHistoryStatus.value || h.status === filterHistoryStatus.value
    const contactMatch = !filterHistoryContact.value || h.contactEmail.toLowerCase().includes(filterHistoryContact.value.toLowerCase())
    const subjectMatch = !filterHistorySubject.value || h.subject.toLowerCase().includes(filterHistorySubject.value.toLowerCase())

    return statusMatch && contactMatch && subjectMatch
  })
})

const paginatedHistoryItems = computed(() => {
  const start = (historyPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredReminderHistories.value.slice(start, end)
})

const historyTotalPages = computed(() => {
  return Math.ceil(filteredReminderHistories.value.length / itemsPerPage)
})

// Format date with relative/absolute
const formatHistoryDate = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffSecs = Math.floor(diffMs / 1000)
    const diffMins = Math.floor(diffSecs / 60)
    const diffHours = Math.floor(diffMins / 60)
    const diffDays = Math.floor(diffHours / 24)

    if (diffSecs < 60) return 'just now'
    if (diffMins < 60) return `${diffMins}m ago`
    if (diffHours < 24) return `${diffHours}h ago`
    if (diffDays === 1) return `Yesterday ${date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}`
    if (diffDays <= 7) return `${diffDays}d ago`
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).replace(',', '')
  } catch {
    return dateString
  }
}

const getFullTimestamp = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
    const date = new Date(dateString)
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return dateString
  }
}

// Copy to clipboard
const copyToClipboard = (text: string) => {
  history.copyToClipboard(text)
}

// View detail
const viewReminderHistoryDetail = async (hist: ReminderHistory) => {
  await history.viewReminderHistoryDetail(hist)
}

// Pagination
const nextHistoryPage = () => {
  if (historyPage.value < historyTotalPages.value) {
    historyPage.value++
  }
}

const prevHistoryPage = () => {
  if (historyPage.value > 1) {
    historyPage.value--
  }
}

// Reset page on filter change
watch([filterHistoryStatus, filterHistoryContact, filterHistorySubject], () => {
  historyPage.value = 1
})

// Refresh history
const refreshHistory = async () => {
  await history.loadReminderHistory()
  await history.loadReminderHistoryStats()
}

onMounted(async () => {
  await history.loadReminderHistory()
  await history.loadReminderHistoryStats()
})
</script>

<template>
  <div class="tab-pane history-section">
    <div class="tab-title-section">
      <h3 class="tab-title">History</h3>
      <div class="tab-divider"></div>
    </div>

    <div class="history-content">
      <!-- Stats Panel - Collapsible -->
      <div class="stats-panel">
        <!-- Stats Header -->
        <div class="stats-panel-header" @click="isStatsExpanded = !isStatsExpanded">
          <div class="stats-header-left">
            <ChevronDown
              :size="20"
              class="stats-chevron"
              :class="{ expanded: isStatsExpanded }"
            />
            <h4 class="stats-panel-title">Summary</h4>
          </div>
          <div v-if="!isStatsExpanded" class="stats-summary">
            <span class="summary-item">Total: {{ totalReminders }}</span>
            <span class="summary-divider">•</span>
            <span class="summary-item" v-if="totalReminders > 0">
              Success: {{ successRate }}%
            </span>
          </div>
        </div>

        <!-- Stats Content - Expandable -->
        <div v-show="isStatsExpanded" class="stats-panel-content">
          <div class="history-stats-header">
            <div class="stats-card">
              <div class="stats-label">Total</div>
              <div class="stats-value">{{ totalReminders }}</div>
            </div>
            <div class="stats-card">
              <div class="stats-label">Sent</div>
              <div class="stats-value stats-success">
                {{ sentCount }}
              </div>
            </div>
            <div class="stats-card">
              <div class="stats-label">Failed</div>
              <div class="stats-value stats-error">
                {{ failedCount }}
              </div>
            </div>
            <div class="stats-card">
              <div class="stats-label">Pending</div>
              <div class="stats-value stats-pending">
                {{ pendingCount }}
              </div>
            </div>
            <div v-if="totalReminders > 0" class="stats-card stats-rate">
              <div class="stats-label">Success Rate</div>
              <div class="stats-value stats-rate-value">
                {{ successRate }}%
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- History List -->
      <div class="history-list">
        <div v-if="history.isLoadingReminderHistory?.value" class="loading-state">
          <div class="spinner"></div>
          <span>Loading reminder history...</span>
        </div>
        <div v-else-if="history.reminderHistories?.value?.length === 0" class="empty-state">
          <Clock :size="32" class="empty-icon" />
          <span>No reminder history found</span>
        </div>
        <div v-else class="table-pagination-wrapper">
          <div class="history-table-wrapper">
            <table class="history-table">
              <thead>
                <tr>
                  <th class="col-contact">CONTACT</th>
                  <th class="col-status">STATUS</th>
                  <th class="col-subject">SUBJECT</th>
                  <th class="col-date">DATE</th>
                  <th class="col-action">ACTION</th>
                </tr>
                <!-- Filters Row -->
                <tr class="filters-header-row">
                  <td class="col-contact filter-cell">
                    <input
                      v-model="filterHistoryContact"
                      type="text"
                      placeholder="Search email..."
                      class="filter-input-inline"
                    />
                  </td>
                  <td class="col-status filter-cell">
                    <select v-model="filterHistoryStatus" class="filter-select-inline">
                      <option value="">All</option>
                      <option value="sent">Sent</option>
                      <option value="failed">Failed</option>
                      <option value="pending">Pending</option>
                    </select>
                  </td>
                  <td class="col-subject filter-cell">
                    <input
                      v-model="filterHistorySubject"
                      type="text"
                      placeholder="Search subject..."
                      class="filter-input-inline"
                    />
                  </td>
                  <td class="col-date filter-cell">
                  </td>
                  <td class="col-action filter-cell">
                    <button
                      @click="refreshHistory"
                      class="btn btn-filter-refresh"
                      :disabled="history.isLoadingReminderHistory?.value || false"
                      title="Refresh history and stats"
                    >
                      <RefreshCw v-if="!history.isLoadingReminderHistory?.value" :size="16" />
                      <span
                        v-else
                        class="spinner"
                        style="width: 16px; height: 16px; display: inline-block"
                      ></span>
                    </button>
                  </td>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in paginatedHistoryItems" :key="item.id" class="history-row">
                  <td class="col-contact">{{ item.contactEmail }}</td>
                  <td class="col-status" :data-status="item.status">
                    <span :class="['status-badge', item.status, `status-${item.status}`]">
                      <span class="status-text">{{ item.status }}</span>
                    </span>
                  </td>
                  <td class="col-subject">{{ item.subject || '(No subject)' }}</td>
                  <td class="col-date" :title="getFullTimestamp(item.sentAt)">
                    <span class="date-display">{{ formatHistoryDate(item.sentAt) }}</span>
                  </td>
                  <td class="col-action">
                    <button
                      @click="viewReminderHistoryDetail(item)"
                      class="btn btn-link"
                      title="View full details"
                    >
                      Details
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
           <div class="table-pagination-controls">
             <span class="pagination-info-text">
               Page {{ historyPage }} of {{ historyTotalPages }} ({{
                 filteredReminderHistories.length
               }}
               total)
             </span>
             <div class="pagination-buttons">
               <button
                 @click="prevHistoryPage"
                 class="pagination-btn"
                 :disabled="historyPage === 1"
                 title="Previous page"
               >
                 ← Prev
               </button>
               <button
                 @click="nextHistoryPage"
                 class="pagination-btn"
                 :disabled="historyPage === historyTotalPages || filteredReminderHistories.length == 0"
                 title="Next page"
               >
                 Next →
               </button>
             </div>
           </div>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <div v-if="historyDetailModal" class="modal-overlay">
      <div class="modal-container modal-detail history-detail-modal">
        <div class="modal-header history-detail-header">
          <div class="header-title-section">
            <h3 class="modal-title"></h3>
          </div>
          <button @click="history.historyDetailModal.value = null" class="close-button">
            <X :size="20" />
          </button>
        </div>

        <div class="modal-content history-detail-content">
          <!-- Status Badge & Contact Section -->
          <div class="detail-card status-card">
            <div class="card-content-row">
              <div class="card-section">
                <span class="detail-label">Contact</span>
                <p class="detail-value contact-value">{{ historyDetailModal.contactEmail }}</p>
              </div>
              <div class="card-section">
                <span class="detail-label">Status</span>
                <span :class="['status-badge-large', historyDetailModal.status, `status-${historyDetailModal.status}`]">
                  {{ historyDetailModal.status }}
                </span>
              </div>
              <div class="card-section">
                <span class="detail-label">Sent Time</span>
                <p class="detail-value time-value">{{ formatHistoryDate(historyDetailModal.sentAt) }}</p>
                <p class="detail-timestamp">{{ getFullTimestamp(historyDetailModal.sentAt) }}</p>
              </div>
            </div>
          </div>

          <!-- Email Content Section -->
          <div class="detail-card content-card">
            <div class="card-header1">
              <h4 class="card-title1">Email Content</h4>
            </div>
            <div class="card-divider"></div>

            <div class="card-section">
              <span class="detail-label">Subject</span>
              <p class="detail-value subject-value">{{ historyDetailModal.subject || '(No subject)' }}</p>
            </div>

            <div class="card-section message-section">
              <div class="section-header">
                <span style="margin-top: 12px" class="detail-label">Message Body</span>
              </div>
              <div class="message-preview">
                {{ historyDetailModal.body || '(No message content)' }}
              </div>
            </div>
          </div>


        </div>
      </div>
    </div>
  </div>
</template>
