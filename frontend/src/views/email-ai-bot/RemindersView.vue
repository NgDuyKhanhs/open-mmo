<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useReminders } from '@/composables/email-ai-bot/useReminders'
import { toast } from 'vue3-toastify'
import { HelpCircle, X } from 'lucide-vue-next'

const reminders = useReminders()

const showReminderHelp = ref(false)
const remindersPage = ref(1)
const itemsPerPage = 10

const paginatedReminders = computed(() => {
  const start = (remindersPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return reminders.reminders.value.slice(start, end)
})

const remindersTotalPages = computed(() => {
  return Math.ceil(reminders.reminders.value.length / itemsPerPage)
})

const nextRemindersPage = () => {
  if (remindersPage.value < remindersTotalPages.value) {
    remindersPage.value++
  }
}

const prevRemindersPage = () => {
  if (remindersPage.value > 1) {
    remindersPage.value--
  }
}

onMounted(async () => {
  await reminders.loadReminders()
})
</script>

<template>
  <div class="tab-pane reminders-section">
    <div class="tab-title-section">
      <h3 class="tab-title">Reminders</h3>
      <div class="tab-divider"></div>
    </div>

    <div class="reminders-content">
      <div class="reminder-form-container">
        <div class="new-reminder-header">
          <h4 class="new-reminder-title">Create reminder</h4>
          <button
            @click="showReminderHelp = true"
            class="help-icon-btn"
            type="button"
            title="How reminders work"
          >
            <HelpCircle :size="16" />
          </button>
        </div>

        <div class="form-section">
          <label class="form-label"
            >Remind after inactivity (minutes) <span class="required">*</span></label
          >
          <input
            v-model.number="reminders.newReminder.value.afterInactive"
            type="number"
            min="5"
            placeholder="e.g., 60"
            class="form-input"
          />
        </div>

        <div class="form-section">
          <label class="form-label">Additional options</label>
          <div class="optional-fields-toggles">
            <div>
              <label class="toggle-option">
                <input
                  v-model="reminders.visibleOptionalFields.value.contactEmail"
                  type="checkbox"
                  @change="
                    () =>
                      reminders.visibleOptionalFields.value.contactEmail && reminders.loadContacts()
                  "
                />
                <span>Apply to specific contact</span>
              </label>
              <div v-if="reminders.visibleOptionalFields.value.contactEmail" class="optional-input">
                <div v-if="reminders.isLoadingContacts.value" class="loading-contacts">
                  <div class="spinner" style="width: 16px; height: 16px"></div>
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
                <p class="form-description">
                  Minutes between repeat reminders (default: 1440 = 24 hours)
                </p>
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
                <input v-model="reminders.visibleOptionalFields.value.enabled" type="checkbox" />
                <span>Enable/Disable</span>
              </label>
              <div v-if="reminders.visibleOptionalFields.value.enabled" class="optional-input">
                <label class="toggle-switch">
                  <input v-model="reminders.newReminder.value.enabled" type="checkbox" />
                  <span class="slider"></span>
                  <span class="label-text">{{
                    reminders.newReminder.value.enabled ? 'Enabled' : 'Disabled'
                  }}</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div class="reminder-action-buttons">
          <button
            @click="reminders.saveReminder()"
            class="btn btn-action-save"
            :disabled="reminders.isSavingReminder.value"
          >
            {{ reminders.isSavingReminder.value ? 'Saving...' : 'Save' }}
          </button>
          <button @click="reminders.resetReminderForm()" class="btn btn-action-cancel">
            Cancel
          </button>
        </div>
      </div>

      <div class="configured-reminders">
        <div class="reminders-section-header">
          <h4 class="reminders-section-title">Configuration</h4>
          <span class="reminders-count">({{ reminders.reminders.value.length }})</span>
        </div>

        <div v-if="reminders.reminders.value.length > 0" class="table-pagination-wrapper">
          <div class="reminders-table-wrapper">
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
                <tr v-for="reminder in paginatedReminders" :key="reminder.contactEmail">
                  <td class="email-cell col-email">{{ reminder.contactEmail || 'All Emails' }}</td>
                  <td class="status-cell col-status">
                    <span :class="['status-badge', reminder.enabled ? 'active' : 'inactive']">
                      {{ reminder.enabled ? '● Active' : '● Inactive' }}
                    </span>
                  </td>
                  <td class="schedule-cell col-schedule">{{ reminder.afterInactive }}</td>
                  <td class="schedule-cell col-repeat">{{ reminder.repeatEvery || 1440 }}</td>
                  <td
                    :class="[
                      'max-cell',
                      'col-max',
                      { 'max-reached': reminder.sentCount >= reminder.maxReminders },
                    ]"
                  >
                    <span class="reminders-progress"
                      >{{ reminder.sentCount }}/{{ reminder.maxReminders }}</span
                    >
                  </td>
                  <td class="actions-cell col-actions">
                    <div class="actions-group">
                      <button
                        @click="reminders.editReminder(reminder)"
                        class="action-btn edit-btn"
                        title="Edit"
                      >
                        ✎
                      </button>
                      <button
                        @click="reminders.showDeleteConfirmPopup(reminder.contactEmail)"
                        class="action-btn delete-btn"
                        title="Delete"
                      >
                        🗑
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="table-pagination-controls" v-if="reminders.reminders.value.length > itemsPerPage">
             <span class="pagination-info-text">
               Page {{ remindersPage }} of {{ remindersTotalPages }} ({{
                 reminders.reminders.value.length
               }}
               total)
             </span>
             <div class="pagination-buttons">
               <button
                 @click="prevRemindersPage()"
                 class="pagination-btn"
                 :disabled="remindersPage === 1"
                 title="Previous page"
               >
                 ← Prev
               </button>
               <button
                 @click="nextRemindersPage()"
                 class="pagination-btn"
                 :disabled="remindersPage === remindersTotalPages"
                 title="Next page"
               >
                 Next →
               </button>
             </div>
           </div>
        </div>
        <div v-else class="no-reminders">
          <p>No reminders configured yet</p>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="reminders.showDeleteConfirm.value" class="modal-overlay">
      <div class="modal-container" style="width: min(400px, 90vw)">
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

          <div class="detail-actions" style="margin-top: 24px">
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
      <div class="modal-container reminder-help-modal" style="max-width: 600px">
        <div class="modal-header">
          <h3 class="modal-title">How it works</h3>
          <button @click="showReminderHelp = false" class="close-button">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-content">
          <div class="help-section help-section-warning">
            <h4 class="help-title help-title-warning">⚠️ Important: Bot must be enabled</h4>
            <p class="help-text">Reminders will not work if the bot is disabled. Make sure bot is running before setting up reminders.</p>
          </div>

          <div class="help-section">
            <h4 class="help-title">📋 After Inactivity</h4>
            <p class="help-text">Send first reminder after N minutes of no reply</p>
            <div class="help-example">
              <strong>Example:</strong> 60 min → send reminder when user inactive for 60+ minutes
            </div>
          </div>

          <div class="help-section">
            <h4 class="help-title">🔄 Repeat Interval</h4>
            <p class="help-text">Send follow-ups every N minutes (even if user replies)</p>
            <div class="help-example">
              <strong>Example:</strong> 1440 min (24h) → reminder daily
            </div>
          </div>

          <div class="help-section">
            <h4 class="help-title">🎯 Max Reminders</h4>
            <p class="help-text">Stop sending after reaching this limit</p>
            <div class="help-example"><strong>Example:</strong> 5 → send max 5 reminders total</div>
          </div>

          <div class="help-section">
            <h4 class="help-title">📅 Example Timeline</h4>
            <div class="timeline-example">
              <div class="timeline-item">
                <span class="timeline-time">11:00</span>
                <span class="timeline-event">Reminder #1 (after 60 min inactivity)</span>
              </div>
              <div class="timeline-item">
                <span class="timeline-time">12:00</span>
                <span class="timeline-event">User replies</span>
              </div>
              <div class="timeline-item">
                <span class="timeline-time">+24h</span>
                <span class="timeline-event">Reminder #2 (still sends, user replied)</span>
              </div>
            </div>
          </div>

          <div class="help-note">
            💡 Repeat is time-based, not activity-based. Regular follow-ups even if user responds.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
