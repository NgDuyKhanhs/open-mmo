package com.openmmo.ai.scheduler

import com.openmmo.ai.entity.ReminderConfig
import com.openmmo.ai.entity.ReminderHistory
import com.openmmo.ai.repository.ReminderConfigRepository
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.repository.ReminderHistoryRepository
import com.openmmo.ai.service.IAIReminderService
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.impl.ReminderServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Reminder Scheduler
 * Processes reminders on a scheduled interval (every 5 minutes)
 * Checks conditions and sends reminder emails to inactive contacts
 * Logs all reminder history for audit and user analytics
 */
@Component
class ReminderScheduler(
    private val reminderRepository: ReminderConfigRepository,
    private val gmailApiService: IGmailApiService,
    private val correspondentMemoryRepository: CorrespondentMemoryRepository,
    private val gmailBotConfigRepository: GmailBotConfigRepository,
    private val aiReminderService: IAIReminderService,
    private val reminderService: ReminderServiceImpl,
    private val reminderHistoryRepository: ReminderHistoryRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ReminderScheduler::class.java)
    }

    /**
     * Process all enabled reminders every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    fun processReminders() {
        try {
            logger.debug("Processing reminders...")
            val allReminders = reminderRepository.findByEnabledTrue()

            for (reminder in allReminders) {
                try {
                    processReminder(reminder)
                } catch (e: Exception) {
                    logger.error("Error processing reminder for ${reminder.contactEmail}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error in processReminders scheduler", e)
        }
    }

    private fun processReminder(reminder: ReminderConfig) {
        // 1. Check if already sent max reminders
        if (reminder.sentCount >= reminder.maxReminders) {
            logger.debug("Reminder ${reminder.contactEmail} already sent max reminders")
            return
        }

        // 2. Check if should send now (considering first time - no repeats)
        if (!shouldSendNow(reminder)) {
            logger.debug("Reminder ${reminder.contactEmail} not yet time to send")
            return
        }

        // 3. Check if conversation is inactive long enough
        if (!isInactiveEnough(reminder)) {
            logger.debug("Reminder ${reminder.contactEmail} conversation still active")
            return
        }

        // 4. Send reminder email
        try {
            sendReminderEmail(reminder)
            updateReminderAfterSend(reminder)
        } catch (e: Exception) {
            logger.error("Failed to send reminder for ${reminder.contactEmail}", e)
        }
    }

    private fun shouldSendNow(reminder: ReminderConfig): Boolean {
        // Since repeatEvery is removed, reminder only sends once
        if (reminder.lastSentAt == null) {
            logger.debug("First time sending reminder for ${reminder.contactEmail}")
            return true
        }

        // Already sent once, don't send again
        logger.debug("Reminder ${reminder.contactEmail} already sent once, no repeat")
        return false
    }

    private fun isInactiveEnough(reminder: ReminderConfig): Boolean {
        try {
            // Handle "ALL_CONTACTS" reminder
            val contactIdentifier = if (reminder.contactEmail == "ALL_CONTACTS") "all contacts" else reminder.contactEmail

            // Get last message time with this contact from Gmail
            val lastMessageTime = getLastMessageTimeFromGmail(reminder.userId, reminder.contactEmail)
            if (lastMessageTime == null) {
                logger.debug("No messages found with $contactIdentifier, sending reminder")
                return true
            }

            val minutesSince = ChronoUnit.MINUTES.between(lastMessageTime, LocalDateTime.now())
            val isInactive = minutesSince >= reminder.afterInactive
            logger.debug("Inactivity check: $contactIdentifier, minutesSince=$minutesSince, afterInactive=${reminder.afterInactive}, isInactive=$isInactive")
            return isInactive
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            logger.error("Error checking inactivity for ${reminder.contactEmail}")
            return false
        }
    }

    private fun getLastMessageTimeFromGmail(userId: String, contactEmail: String): LocalDateTime? {
        return try {
            // Query Gmail API for messages from/to this contact
            val query = if (contactEmail == "ALL_CONTACTS") {
                // For "ALL_CONTACTS" reminders, get any recent message
                ""
            } else {
                // Search for messages from or to this specific contact
                "from:$contactEmail OR to:$contactEmail"
            }

            val messageIds = gmailApiService.searchMessages(
                userId = userId,
                query = query,
                maxResults = 1  // Get only the most recent one
            )

            if (messageIds.isEmpty()) {
                logger.debug("No messages found with $contactEmail")
                return null
            }

            val messageId = messageIds.first()
            val headers = gmailApiService.getMessageHeaders(userId, messageId)

            // Parse the Date header to get message time
            val dateStr = headers["Date"] ?: return null

            // Try to parse the date (Gmail uses RFC 2822 format)
            return try {
                LocalDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME)
            } catch (e: Exception) {
                logger.warn("Failed to parse date from header: $dateStr")
                null
            }
        } catch (e: Exception) {
            logger.error("Error getting last message time from Gmail: ${e.message}")
            null
        }
    }

    private fun sendReminderEmail(reminder: ReminderConfig) {
        logger.info("Sending reminder email to ${reminder.contactEmail} for user ${reminder.userId}")

        try {
            // Handle "ALL_CONTACTS" - send to all correspondents
            if (reminder.contactEmail == "ALL_CONTACTS") {
                logger.info("ALL_CONTACTS reminder: sending to all correspondents")
                sendReminderToAllCorrespondents(reminder)
                return
            }

            // Normal single contact flow
            // 1. Query Gmail API to find the last thread with this contact
            val query = "from:${reminder.contactEmail} OR to:${reminder.contactEmail}"

            val messageIds = gmailApiService.searchMessages(
                userId = reminder.userId,
                query = query,
                maxResults = 1
            )

            if (messageIds.isEmpty()) {
                logger.warn("No existing thread found with ${reminder.contactEmail}, creating new thread")
                // Send as new email (not a reply)
                val subject = "Follow-up: Checking in"
                val body = buildReminderMessage(reminder.userId, reminder.contactEmail)
                gmailApiService.sendReply(
                    userId = reminder.userId,
                    threadId = "",  // Empty threadId for new message
                    toEmail = reminder.contactEmail,
                    subject = subject,
                    bodyText = body
                )
                // Save success history
                saveReminderHistory(reminder, "sent", "", body, subject)
                return
            }

            // 2. Get the last message details
            val lastMessageId = messageIds.first()
            val headers = gmailApiService.getMessageHeaders(reminder.userId, lastMessageId)
            val meta = gmailApiService.getMessageMeta(reminder.userId, lastMessageId)

            val threadId = meta["threadId"] as? String ?: ""
            val fromHeader = headers["From"] ?: ""
            val subjectHeader = headers["Subject"] ?: "Re: (no subject)"

            // 3. Determine if last message was from bot or contact
            val isLastFromContact = fromHeader.contains(reminder.contactEmail, ignoreCase = true)

            // 4. Send reminder as reply or new thread
            val replySubject = if (subjectHeader.startsWith("Re:")) {
                subjectHeader
            } else {
                "Re: $subjectHeader"
            }

            val reminderBody = buildReminderMessage(reminder.userId, reminder.contactEmail)

            if (isLastFromContact && threadId.isNotEmpty()) {
                // Last message from contact - send as reply to thread
                logger.debug("Last message from contact, sending as reply to thread $threadId")
                gmailApiService.sendReply(
                    userId = reminder.userId,
                    threadId = threadId,
                    toEmail = reminder.contactEmail,
                    subject = replySubject,
                    bodyText = reminderBody
                )
            } else {
                // Last message from bot - send as new thread or reply anyway
                logger.debug("Last message from bot or no thread, sending new message")
                gmailApiService.sendReply(
                    userId = reminder.userId,
                    threadId = threadId.ifEmpty { "" },
                    toEmail = reminder.contactEmail,
                    subject = replySubject,
                    bodyText = reminderBody
                )
            }

            // Save success history
            saveReminderHistory(reminder, "sent", "", reminderBody, replySubject)
            logger.info("Reminder email sent successfully to ${reminder.contactEmail}")
        } catch (e: Exception) {
            logger.error("Failed to send reminder email to ${reminder.contactEmail}: ${e.message}", e)
            throw e
        }
    }

    private fun sendReminderToAllCorrespondents(reminder: ReminderConfig) {
        try {
            // Query Gmail API to get all recent emails from real people (not mailing lists/auto-replies)
            val query = "in:inbox newer_than:30d " +
                    "-is:mailing-list " +
                    "-category:promotions -category:updates -category:forums -category:social " +
                    "-from:(noreply OR \"no-reply\" OR donotreply OR \"do-not-reply\" OR \"mailer-daemon\" OR postmaster) " +
                    "-subject:(\"do not reply\" OR unsubscribe)"

            val messageIds = gmailApiService.searchMessages(
                userId = reminder.userId,
                query = query,
                maxResults = 100  // Get up to 100 recent emails from real people
            )

            if (messageIds.isEmpty()) {
                logger.warn("No recent messages found for ALL_CONTACTS reminder, userId: ${reminder.userId}")
                return
            }

            // Extract unique senders from messages
            val correspondentEmails = mutableSetOf<String>()
            for (messageId in messageIds) {
                try {
                    val headers = gmailApiService.getMessageHeaders(reminder.userId, messageId)
                    val fromHeader = headers["From"] ?: continue

                    // Extract email address from "Name <email@domain.com>" format
                    val emailPattern = Regex("""<([^>]+)>""")
                    val match = emailPattern.find(fromHeader)
                    val email = match?.groupValues?.get(1) ?: fromHeader.trim()

                    if (email.isNotEmpty() && !email.contains("noreply", ignoreCase = true)) {
                        correspondentEmails.add(email)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to extract email from message $messageId: ${e.message}")
                }
            }

            if (correspondentEmails.isEmpty()) {
                logger.warn("No valid correspondent emails extracted from messages")
                return
            }

            logger.info("Found ${correspondentEmails.size} unique correspondents for ALL_CONTACTS reminder from Gmail API")

            // Send reminder to each correspondent with personalized message
            for (correspondent in correspondentEmails) {
                try {
                    // Generate personalized message for each correspondent
                    val reminderBody = buildReminderMessage(reminder.userId, correspondent)
                    gmailApiService.sendReply(
                        userId = reminder.userId,
                        threadId = "",
                        toEmail = correspondent,
                        subject = "Follow-up: Checking in",
                        bodyText = reminderBody
                    )
                    // Save success history
                    saveReminderHistory(reminder, "sent", "", reminderBody, "Follow-up: Checking in")
                    logger.info("Reminder sent to correspondent: $correspondent")
                } catch (e: Exception) {
                    logger.warn("Failed to send reminder to $correspondent: ${e.message}")
                    // Save failed history
                    saveReminderHistory(reminder, "failed", e.message ?: "Unknown error", "", "Follow-up: Checking in")
                }
            }

            logger.info("ALL_CONTACTS reminder completed, sent to ${correspondentEmails.size} correspondents")
        } catch (e: Exception) {
            logger.error("Error sending ALL_CONTACTS reminder: ${e.message}", e)
            throw e
        }
    }

    private fun buildReminderMessage(
        userId: String,
        contactEmail: String
    ): String {
        // Delegate to ReminderServiceImpl which handles:
        // 1. Try to get conversation context from Gmail (cached 15 min)
        // 2. Fallback to profileSummary from DB
        // 3. Call AI service once with appropriate context
        return reminderService.buildReminderMessage(userId, contactEmail)
    }

    private fun updateReminderAfterSend(reminder: ReminderConfig) {
        val newSentCount = reminder.sentCount + 1
        val shouldDisable = newSentCount >= reminder.maxReminders

        val updated = reminder.copy(
            lastSentAt = LocalDateTime.now(),
            sentCount = newSentCount,
            enabled = if (shouldDisable) false else reminder.enabled,  // Auto-disable when limit reached
            updatedAt = LocalDateTime.now()
        )
        reminderRepository.save(updated)

        if (shouldDisable) {
            logger.info("Reminder ${reminder.contactEmail} auto-disabled: reached max reminders ($newSentCount/${reminder.maxReminders})")
        } else {
            logger.info("Updated reminder ${reminder.contactEmail}, sentCount=$newSentCount")
        }
    }

    /**
     * Save reminder history for audit and user analytics
     * @param reminder Reminder config
     * @param status Status: "sent", "failed", "skipped"
     * @param reason Reason if failed/skipped
     * @param body Email body
     * @param subject Email subject (optional)
     */
    private fun saveReminderHistory(
        reminder: ReminderConfig,
        status: String,
        reason: String,
        body: String,
        subject: String = "Follow-up: Checking in"
    ) {
        try {
            val history = ReminderHistory(
                userId = reminder.userId,
                contactEmail = reminder.contactEmail,
                subject = subject,
                body = body,
                status = status,
                reason = reason,
                sentAt = LocalDateTime.now()
            )
            reminderHistoryRepository.save(history)
            logger.debug("Saved reminder history for ${reminder.contactEmail}: status=$status")
        } catch (e: Exception) {
            logger.warn("Failed to save reminder history (non-fatal): ${e.message}")
        }
    }
}

