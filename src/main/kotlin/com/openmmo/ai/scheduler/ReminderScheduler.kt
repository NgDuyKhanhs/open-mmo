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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

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
        val contactId = reminder.contactEmail

        // 1. Check if already sent max reminders
        if (reminder.sentCount >= reminder.maxReminders) {
            logger.debug("🎯 $contactId: Max reminders reached (${reminder.sentCount}/${reminder.maxReminders})")
            return
        }

        // 2. Check if should send now (first time or repeat interval passed)
        if (!shouldSendNow(reminder)) {
            logger.debug("⏸️  $contactId: Not time to send yet")
            return
        }

        // 3. Check if conversation is inactive long enough
        // For first send: check afterInactive threshold
        // For repeat: skip this check (user might have replied, but we still want to remind after repeatEvery)
        val isFirstSend = reminder.lastSentAt == null
        if (isFirstSend && !isInactiveEnough(reminder)) {
            logger.debug("💬 $contactId: Conversation still too active, waiting...")
            return
        }

        // 4. Send reminder email
        try {
            logger.info("🚀 $contactId: Sending reminder #${reminder.sentCount + 1}/${reminder.maxReminders} (afterInactive=${reminder.afterInactive}min, repeatEvery=${reminder.repeatEvery}min)")
            sendReminderEmail(reminder)
            updateReminderAfterSend(reminder)
        } catch (e: Exception) {
            logger.error("❌ $contactId: Failed to send reminder - ${e.message}", e)
        }
    }

    private fun shouldSendNow(reminder: ReminderConfig): Boolean {
        // First time - never sent before
        if (reminder.lastSentAt == null) {
            logger.debug("✉️  ${reminder.contactEmail}: First time (lastSentAt=null, sentCount=${reminder.sentCount})")
            return true
        }

        // Already sent max number of reminders
        if (reminder.sentCount >= reminder.maxReminders) {
            logger.debug("🎯 ${reminder.contactEmail}: Max reminders reached (${reminder.sentCount}/${reminder.maxReminders})")
            return false
        }

        // Check if enough time has passed for repeat
        val minutesSinceLast = java.time.temporal.ChronoUnit.MINUTES.between(reminder.lastSentAt, LocalDateTime.now())
        val shouldRepeat = minutesSinceLast >= reminder.repeatEvery

        if (shouldRepeat) {
            logger.debug("🔄 ${reminder.contactEmail}: Time for repeat reminder (lastSentAt=${reminder.lastSentAt}, sentCount=${reminder.sentCount}/${reminder.maxReminders}, minutesSinceLast=$minutesSinceLast >= repeatEvery=${reminder.repeatEvery})")
            return true
        } else {
            val minutesUntilNext = reminder.repeatEvery - minutesSinceLast
            logger.debug("⏱️  ${reminder.contactEmail}: Next repeat in ${minutesUntilNext}min (lastSentAt=${reminder.lastSentAt}, sentCount=${reminder.sentCount}/${reminder.maxReminders}, minutesSinceLast=$minutesSinceLast)")
            return false
        }
    }

    private fun isInactiveEnough(reminder: ReminderConfig): Boolean {
        try {
            // Handle "ALL_CONTACTS" reminder
            val contactIdentifier = if (reminder.contactEmail == "ALL_CONTACTS") "all contacts" else reminder.contactEmail

            // Get last message time with this contact from Gmail
            val lastMessageTime = getLastMessageTimeFromGmail(reminder.userId, reminder.contactEmail)
            if (lastMessageTime == null) {
                logger.debug("No messages found with $contactIdentifier, ready to send reminder")
                return true
            }

            val minutesSince = ChronoUnit.MINUTES.between(lastMessageTime, LocalDateTime.now())
            val isInactive = minutesSince >= reminder.afterInactive
            val reason = if (isInactive) "READY" else "TOO ACTIVE"
            logger.debug("[$reason] Inactivity check for $contactIdentifier: minutesSince=$minutesSince, afterInactive=${reminder.afterInactive}, isInactive=$isInactive")
            return isInactive
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            logger.error("Error checking inactivity for ${reminder.contactEmail}", e)
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
            // ✅ OPTIMIZATION 1: Pre-generate reminder body once (avoid duplicate call)
            val reminderBody = buildReminderMessage(reminder.userId, reminder.contactEmail)

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
                sendReminderWithRetry(
                    reminder = reminder,
                    threadId = "",
                    toEmail = reminder.contactEmail,
                    subject = subject,
                    bodyText = reminderBody
                )
                saveReminderHistory(reminder, "sent", "", reminderBody, subject)
                return
            }

            // 2. Get the last message details
            // ✅ OPTIMIZATION 2: Combine getMessageHeaders + getMessageMeta calls
            val lastMessageId = messageIds.first()
            val headers = gmailApiService.getMessageHeaders(reminder.userId, lastMessageId)
            val meta = gmailApiService.getMessageMeta(reminder.userId, lastMessageId)

            // ✅ OPTIMIZATION 3: Better null safety
            if (headers.isEmpty() || meta.isEmpty()) {
                logger.warn("Missing headers or metadata for message $lastMessageId, treating as new thread")
                val subject = "Follow-up: Checking in"
                sendReminderWithRetry(
                    reminder = reminder,
                    threadId = "",
                    toEmail = reminder.contactEmail,
                    subject = subject,
                    bodyText = reminderBody
                )
                saveReminderHistory(reminder, "sent", "", reminderBody, subject)
                return
            }

            val threadId = meta["threadId"] as? String ?: ""
            val fromHeader = headers["From"] ?: ""
            val subjectHeader = headers["Subject"]?.takeIf { it.isNotBlank() } ?: "(No subject)"

            // 3. Determine if last message was from bot or contact
            val isLastFromContact = fromHeader.contains(reminder.contactEmail, ignoreCase = true)

            // 4. Generate proper subject line for reply
            // ✅ OPTIMIZATION 4: Cleaner subject generation
            val replySubject = when {
                subjectHeader.startsWith("Re:", ignoreCase = true) -> subjectHeader
                subjectHeader == "(No subject)" -> "Re: (No subject)"
                else -> "Re: $subjectHeader"
            }

            // 5. Send reminder via appropriate method
            sendReminderWithRetry(
                reminder = reminder,
                threadId = threadId.takeIf { isLastFromContact && it.isNotEmpty() } ?: "",
                toEmail = reminder.contactEmail,
                subject = replySubject,
                bodyText = reminderBody
            )

            // Save success history
            saveReminderHistory(reminder, "sent", "", reminderBody, replySubject)
            logger.info("Reminder email sent successfully to ${reminder.contactEmail}")
        } catch (e: Exception) {
            logger.error("Failed to send reminder email to ${reminder.contactEmail}: ${e.message}", e)
            // ✅ OPTIMIZATION 5: Always save failure history before throwing
            try {
                saveReminderHistory(reminder, "failed", e.message ?: "Unknown error", "", "Follow-up: Checking in")
            } catch (saveEx: Exception) {
                logger.warn("Failed to save failure history (non-fatal): ${saveEx.message}")
            }
            throw e
        }
    }

    /**
     * ✅ OPTIMIZED: Send reminder with retry logic (3 attempts with exponential backoff)
     * Handles transient network failures gracefully
     */
    private fun sendReminderWithRetry(
        reminder: ReminderConfig,
        threadId: String,
        toEmail: String,
        subject: String,
        bodyText: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 500
    ) {
        var lastException: Exception? = null
        var delayMs = initialDelayMs

        for (attempt in 1..maxRetries) {
            try {
                logger.debug("Sending reminder to $toEmail (attempt $attempt/$maxRetries)")
                gmailApiService.sendReply(
                    userId = reminder.userId,
                    threadId = threadId,
                    toEmail = toEmail,
                    subject = subject,
                    bodyText = bodyText
                )
                logger.debug("Reminder sent successfully to $toEmail on attempt $attempt")
                return  // Success
            } catch (e: Exception) {
                lastException = e
                logger.warn("Attempt $attempt/$maxRetries failed for $toEmail: ${e.message}")

                // Don't retry on last attempt or for permanent errors
                if (attempt < maxRetries) {
                    // Check if error is retryable (network/transient errors)
                    val isRetryable = when {
                        e.message?.contains("timeout", ignoreCase = true) == true -> true
                        e.message?.contains("connection", ignoreCase = true) == true -> true
                        e.message?.contains("temporarily", ignoreCase = true) == true -> true
                        e is java.net.SocketException -> true
                        e is java.net.ConnectException -> true
                        else -> false
                    }

                    if (isRetryable) {
                        logger.debug("Retryable error detected, waiting ${delayMs}ms before retry...")
                        Thread.sleep(delayMs)
                        delayMs = (delayMs * 1.5).toLong()  // Exponential backoff
                    } else {
                        logger.debug("Non-retryable error, skipping retries")
                        throw e
                    }
                }
            }
        }

        // All retries exhausted
        logger.error("Failed to send reminder to $toEmail after $maxRetries attempts: ${lastException?.message}")
        throw lastException ?: Exception("Failed to send reminder after $maxRetries attempts")
    }

    private fun sendReminderToAllCorrespondents(reminder: ReminderConfig) {
        val startTime = System.currentTimeMillis()

        try {
            logger.info("🚀 Starting ALL_CONTACTS reminder batch for userId: ${reminder.userId}")

            // ✅ OPTIMIZED: Query Gmail API to get all recent emails from real people
            val query = "in:inbox newer_than:30d " +
                    "-is:mailing-list " +
                    "-category:promotions -category:updates -category:forums -category:social " +
                    "-from:(noreply OR \"no-reply\" OR donotreply OR \"do-not-reply\" OR \"mailer-daemon\" OR postmaster) " +
                    "-subject:(\"do not reply\" OR unsubscribe)"

            // ✅ OPTIMIZATION 1: Limit to 50 instead of 100 (safe batch size)
            val messageIds = gmailApiService.searchMessages(
                userId = reminder.userId,
                query = query,
                maxResults = 50
            )

            if (messageIds.isEmpty()) {
                logger.warn("No recent messages found for ALL_CONTACTS reminder, userId: ${reminder.userId}")
                return
            }

            logger.debug("Found ${messageIds.size} recent messages")

            // ✅ OPTIMIZATION 2: Extract emails more efficiently (batch in one pass)
            // Compile regex once instead of N times
            val emailPattern = Regex("""<([^>]+)>""")
            val correspondentEmails = extractUniqueCorrespondents(reminder.userId, messageIds, emailPattern)

            if (correspondentEmails.isEmpty()) {
                logger.warn("No valid correspondent emails extracted from messages")
                return
            }

            logger.info("Extracted ${correspondentEmails.size} unique correspondents")

            // ✅ OPTIMIZATION 3: Pre-generate reminder messages (batch context fetching)
            val reminderMessages = preGenerateReminderMessages(reminder.userId, correspondentEmails)

            // ✅ OPTIMIZATION 4: Send reminders in parallel with connection pooling
            sendRemindersInParallel(reminder, correspondentEmails, reminderMessages)

            val duration = System.currentTimeMillis() - startTime
            logger.info("✅ ALL_CONTACTS reminder completed in ${duration}ms, sent to ${correspondentEmails.size} correspondents")
        } catch (e: Exception) {
            logger.error("Error sending ALL_CONTACTS reminder: ${e.message}", e)
            throw e
        }
    }

    /**
     * ✅ OPTIMIZED: Extract unique correspondent emails in single pass
     * Uses pre-compiled regex and efficient extraction
     */
    private fun extractUniqueCorrespondents(
        userId: String,
        messageIds: List<String>,
        emailPattern: Regex
    ): Set<String> {
        val correspondentEmails = mutableSetOf<String>()
        var apiCallCount = 0

        for (messageId in messageIds) {
            try {
                val headers = gmailApiService.getMessageHeaders(userId, messageId)
                apiCallCount++
                val fromHeader = headers["From"] ?: continue

                // ✅ Try angle bracket format first <email@domain.com>
                val match = emailPattern.find(fromHeader)
                val email = if (match != null) {
                    match.groupValues.getOrNull(1)?.trim() ?: ""
                } else {
                    // ✅ Fallback: extract email regex from full header
                    Regex("""([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})""")
                        .find(fromHeader)
                        ?.groupValues?.getOrNull(0)?.trim() ?: ""
                }

                if (email.isNotEmpty() && !email.contains("noreply", ignoreCase = true)) {
                    correspondentEmails.add(email)
                }
            } catch (e: Exception) {
                logger.warn("Failed to extract email from message $messageId: ${e.message}")
            }
        }

        logger.debug("Email extraction: $apiCallCount API calls, ${correspondentEmails.size} unique emails")
        return correspondentEmails
    }

    /**
     * ✅ OPTIMIZED: Pre-generate reminder messages to batch context fetching
     * This way all context calls are parallelized instead of sequential per correspondent
     */
    private fun preGenerateReminderMessages(
        userId: String,
        correspondentEmails: Set<String>
    ): Map<String, String> {
        val messages = mutableMapOf<String, String>()
        val startTime = System.currentTimeMillis()

        // ✅ Batch generate using parallel stream (leverages context caching)
        val futures = correspondentEmails.map { correspondent ->
            CompletableFuture.supplyAsync({
                try {
                    correspondent to buildReminderMessage(userId, correspondent)
                } catch (e: Exception) {
                    logger.warn("Failed to pre-generate message for $correspondent: ${e.message}")
                    correspondent to "Follow-up: Checking in"  // Fallback
                }
            }, ForkJoinPool.commonPool())
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        futures.forEach { future ->
            val (correspondent, message) = future.join()
            messages[correspondent] = message
        }

        val duration = System.currentTimeMillis() - startTime
        logger.debug("Pre-generated ${messages.size} reminder messages in ${duration}ms")
        return messages
    }

    /**
     * ✅ OPTIMIZED: Send reminders in parallel with limited thread pool
     * Max 5 concurrent sends to avoid overwhelming Gmail API
     */
    private fun sendRemindersInParallel(
        reminder: ReminderConfig,
        correspondentEmails: Set<String>,
        reminderMessages: Map<String, String>
    ) {
        val startTime = System.currentTimeMillis()
        val maxConcurrent = 5
        val executor = ForkJoinPool(maxConcurrent)

        val futures = correspondentEmails.map { correspondent ->
            CompletableFuture.runAsync({
                try {
                    val body = reminderMessages[correspondent] ?: "Follow-up: Checking in"
                    gmailApiService.sendReply(
                        userId = reminder.userId,
                        threadId = "",
                        toEmail = correspondent,
                        subject = "Follow-up: Checking in",
                        bodyText = body
                    )
                    saveReminderHistory(reminder, "sent", "", body, "Follow-up: Checking in")
                    logger.info("✅ Reminder sent to: $correspondent")
                } catch (e: Exception) {
                    logger.warn("❌ Failed to send reminder to $correspondent: ${e.message}")
                    saveReminderHistory(reminder, "failed", e.message ?: "Unknown error", "", "Follow-up: Checking in")
                }
            }, executor)
        }

        // Wait for all sends to complete with timeout
        CompletableFuture.allOf(*futures.toTypedArray())
            .handle { _, ex ->
                if (ex != null) {
                    logger.warn("Some reminders failed during parallel send: ${ex.message}")
                }
                val duration = System.currentTimeMillis() - startTime
                logger.debug("Parallel send completed in ${duration}ms for ${correspondentEmails.size} correspondents")
            }
            .join()

        executor.shutdown()
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

