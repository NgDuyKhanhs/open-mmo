package com.openmmo.ai.service.impl

import com.openmmo.ai.dto.ReminderConfigDto
import com.openmmo.ai.dto.ReminderResponse
import com.openmmo.ai.dto.toResponse
import com.openmmo.ai.entity.ReminderConfig
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.ReminderConfigRepository
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.service.IReminderService
import com.openmmo.ai.service.IGmailApiService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class ReminderServiceImpl(
    private val reminderRepository: ReminderConfigRepository,
    private val gmailApiService: IGmailApiService,
    private val correspondentMemoryRepository: CorrespondentMemoryRepository
) : IReminderService {

    companion object {
        private val logger = LoggerFactory.getLogger(ReminderServiceImpl::class.java)
        private val VN_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh")
    }

    override fun getReminders(userId: String): List<ReminderResponse> {
        logger.debug("Getting reminders for user: $userId")
        return reminderRepository.findByUserId(userId).map { it.toResponse() }
    }

    override fun getReminder(userId: String, contactEmail: String): ReminderResponse? {
        logger.debug("Getting reminder for user: $userId, contact: $contactEmail")
        return reminderRepository.findByUserIdAndContactEmail(userId, contactEmail)?.toResponse()
    }

    override fun saveReminder(userId: String, dto: ReminderConfigDto): Map<String, String> {
        logger.info("Saving reminder for user: $userId, contact: ${dto.contactEmail}")

        // Normalize contactEmail: convert "all" or empty to "ALL_CONTACTS"
        val normalizedEmail = when {
            dto.contactEmail.isEmpty() -> ""
            dto.contactEmail.equals("all", ignoreCase = true) -> ""
            else -> dto.contactEmail
        }

        // Validate email only if not empty (empty = apply to all contacts)
        if (normalizedEmail.isNotEmpty() && !normalizedEmail.contains("@")) {
            logger.warn("Invalid email: $normalizedEmail")
            return mapOf("status" to "error", "message" to "Invalid email address")
        }

        // Use "ALL_CONTACTS" as storage key for empty email
        val storageKey = if (normalizedEmail.isEmpty()) "ALL_CONTACTS" else normalizedEmail

        val existing = reminderRepository.findByUserIdAndContactEmail(userId, storageKey)

        // Set default values for null fields
        val maxReminders = dto.maxReminders ?: 5

        val reminder = if (existing != null) {
            // Update existing
            existing.copy(
                enabled = dto.enabled,
                afterInactive = dto.afterInactive,
                maxReminders = maxReminders,
                updatedAt = LocalDateTime.now()
            )
        } else {
            // Create new - use the storage key and set defaults for nulls
            dto.toEntity(userId, storageKey).copy(
                maxReminders = maxReminders
            )
        }

        reminderRepository.save(reminder)
        logger.info("Reminder saved for user: $userId, contact: $normalizedEmail, storageKey: $storageKey")

        return mapOf(
            "status" to "success",
            "message" to if (existing != null) "Reminder updated" else "Reminder created"
        )
    }

    override fun deleteReminder(userId: String, contactEmail: String): Map<String, String> {
        logger.info("Deleting reminder for user: $userId, contact: $contactEmail")

        // Handle "all contacts" deletion - normalize various forms to "ALL_CONTACTS"
        val lookupEmail = when {
            contactEmail.isEmpty() -> "ALL_CONTACTS"
            contactEmail.equals("all", ignoreCase = true) -> "ALL_CONTACTS"
            else -> contactEmail
        }

        val reminder = reminderRepository.findByUserIdAndContactEmail(userId, lookupEmail)
        if (reminder == null) {
            logger.warn("Reminder not found for userId=$userId, lookupEmail=$lookupEmail")
            return mapOf("status" to "error", "message" to "Reminder not found")
        }

        reminderRepository.deleteByUserIdAndContactEmail(userId, lookupEmail)
        logger.info("Reminder deleted for user: $userId, contact: $contactEmail")

        return mapOf("status" to "success", "message" to "Reminder deleted")
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    override fun processReminders() {
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
            // Use "from" or "to" operator to find messages with this contact
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
            // Example: "Mon, 14 Apr 2026 10:30:00 +0700"
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
                val body = buildReminderMessage()
                gmailApiService.sendReply(
                    userId = reminder.userId,
                    threadId = "",  // Empty threadId for new message
                    toEmail = reminder.contactEmail,
                    subject = subject,
                    bodyText = body
                )
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

            val reminderBody = buildReminderMessage()

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

            logger.info("Reminder email sent successfully to ${reminder.contactEmail}")
        } catch (e: Exception) {
            logger.error("Failed to send reminder email to ${reminder.contactEmail}: ${e.message}", e)
            throw e
        }
    }

    private fun sendReminderToAllCorrespondents(reminder: ReminderConfig) {
        try {
            // Query all correspondents from correspondent_memory table for this user
            val allCorrespondents = correspondentMemoryRepository.findByUserId(reminder.userId)

            if (allCorrespondents.isEmpty()) {
                logger.warn("No correspondents found in database for ALL_CONTACTS reminder, userId: ${reminder.userId}")
                return
            }

            // Extract unique correspondent emails
            val correspondentEmails = allCorrespondents.mapNotNull { it.correspondentEmail }.toSet()
            logger.info("Found ${correspondentEmails.size} correspondents for ALL_CONTACTS reminder from database")

            // Send reminder to each correspondent
            val reminderBody = buildReminderMessage()
            for (correspondent in correspondentEmails) {
                try {
                    gmailApiService.sendReply(
                        userId = reminder.userId,
                        threadId = "",
                        toEmail = correspondent,
                        subject = "Follow-up: Checking in",
                        bodyText = reminderBody
                    )
                    logger.info("Reminder sent to correspondent: $correspondent")
                } catch (e: Exception) {
                    logger.warn("Failed to send reminder to $correspondent: ${e.message}")
                }
            }

            logger.info("ALL_CONTACTS reminder completed, sent to ${correspondentEmails.size} correspondents")
        } catch (e: Exception) {
            logger.error("Error sending ALL_CONTACTS reminder: ${e.message}", e)
            throw e
        }
    }

    private fun buildReminderMessage(): String {
        return """
            Hi there,
            
            Just reaching out to follow up. Let me know if there's anything you need assistance with.
            
            Looking forward to hearing from you!
        """.trimIndent()
    }

    private fun updateReminderAfterSend(reminder: ReminderConfig) {
        val updated = reminder.copy(
            lastSentAt = LocalDateTime.now(),
            sentCount = reminder.sentCount + 1,
            updatedAt = LocalDateTime.now()
        )
        reminderRepository.save(updated)
        logger.info("Updated reminder ${reminder.contactEmail}, sentCount=${updated.sentCount}")
    }
}







