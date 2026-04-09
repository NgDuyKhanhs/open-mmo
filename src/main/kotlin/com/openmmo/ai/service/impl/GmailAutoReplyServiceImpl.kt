package com.openmmo.ai.service.impl

import com.openmmo.ai.service.IGmailAutoReplyService
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IGmailBotService
import com.openmmo.ai.service.ICorrespondentMemoryService
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.repository.GmailConnectionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Gmail Auto-Reply Service Implementation
 * Automatically generates and sends replies to emails matching trigger conditions
 */
@Service
class GmailAutoReplyServiceImpl(
    private val gmailApiService: IGmailApiService,
    private val gmailBotService: IGmailBotService,
    private val botConfigRepository: GmailBotConfigRepository,
    private val connectionRepository: GmailConnectionRepository,
    private val correspondentMemoryService: ICorrespondentMemoryService
) : IGmailAutoReplyService {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailAutoReplyServiceImpl::class.java)
        private const val AI_BOT_REPLY_LABEL = "AI_BOT_REPLY"
    }

    /**
     * Auto-reply all emails for users with bot enabled
     * @return Map with statistics (totalProcessed, replied, errors)
     */
    override fun autoReplyEmails(): Map<String, Any> {
        logger.info("Starting auto-reply for users with bot enabled")

        var totalProcessed = 0
        var totalReplied = 0
        var totalErrors = 0

        try {
            // Query only configs with bot enabled (more efficient than loading all)
            val enabledConfigs = botConfigRepository.findByEnabledTrue()
            logger.info("Found ${enabledConfigs.size} users with bot enabled")

            enabledConfigs.forEach { config ->
                try {
                    val replied = autoReplyForUser(config.userId)
                    totalReplied += replied
                    totalProcessed++
                } catch (e: Exception) {
                    logger.error("Error auto-replying for user ${config.userId}: ${e.message}")
                    totalErrors++
                }
            }

            logger.info("Auto-reply completed: processed=$totalProcessed, replied=$totalReplied, errors=$totalErrors")

            return mapOf(
                "status" to "success",
                "totalProcessed" to totalProcessed,
                "totalReplied" to totalReplied,
                "totalErrors" to totalErrors
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error occurred"
            val errorType = e::class.simpleName ?: "Exception"
            logger.error("Fatal error in auto-reply: $errorMessage", e)
            return mapOf<String, Any>(
                "status" to "error",
                "message" to errorMessage,
                "errorType" to errorType,
                "totalProcessed" to totalProcessed,
                "totalReplied" to totalReplied,
                "totalErrors" to totalErrors
            )
        }
    }

    /**
     * Auto-reply emails for specific user
     * @param userId User ID
     * @return Number of emails replied
     */
    override fun autoReplyForUser(userId: String): Int {
        try {
            logger.info("Auto-replying emails for user: $userId")

            // 1. Check if bot is enabled
            val status = gmailBotService.getStatus(userId)
            if (!status.botEnabled) {
                logger.debug("Bot not enabled for user: $userId, skipping")
                return 0
            }

            // 2. Get bot config
            val config = botConfigRepository.findByUserId(userId)
            if (config == null) {
                logger.warn("Bot config not found for user: $userId")
                return 0
            }

            val triggerSubject = config.triggerSubject
            logger.debug("Bot trigger subject: $triggerSubject for user: $userId")

            // 3. Get unreplied emails matching trigger subject
            val query = "subject:$triggerSubject -label:$AI_BOT_REPLY_LABEL is:unread"
            val messageIds = gmailApiService.searchMessages(userId, query, maxResults = 10)
            logger.info("Found ${messageIds.size} unreplied emails matching trigger for user: $userId")

            var repliedCount = 0

            // 4. Process each email
            messageIds.forEach { messageId ->
                try {
                    logger.debug("Processing email: $messageId")

                    // Get email body
                    logger.debug("Fetching email content for: $messageId")
                    val emailContent = gmailApiService.getMessageBody(userId, messageId)
                    logger.debug("Email content length: ${emailContent.length}")

                    // Get email details to extract sender info (from Gmail message headers)
                    logger.debug("Fetching email headers for: $messageId")
                    val (emailFrom, emailSubject) = getEmailHeadersFromGmail(userId, messageId)
                    logger.debug("Extracted - From: $emailFrom, Subject: $emailSubject")

                    // Get threadId
                    val meta = gmailApiService.getMessageMeta(userId, messageId)
                    val threadId = meta["threadId"] as? String ?: messageId

                    if (emailFrom.isNotEmpty() && emailSubject.isNotEmpty()) {
                        // Load memory for this correspondent
                        val memory = correspondentMemoryService.getOrCreate(userId, emailFrom)
                        val memoryContext = correspondentMemoryService.buildMemoryContextText(memory)

                        // Generate AI reply with memory context
                        logger.debug("Generating AI reply with memory for: $messageId")
                        val aiReply = gmailApiService.generateAiReplyWithMemory(userId, messageId, memoryContext)
                        logger.debug("AI reply generated with memory, length: ${aiReply.length}")

                        // Validate email before sending
                        if (!emailFrom.contains("@")) {
                            logger.warn("⚠️ Invalid recipient email format: $emailFrom, skipping reply")
                        } else {
                            // Send reply
                            logger.debug("Sending reply to: $emailFrom (subject: $emailSubject)")
                            try {
                                gmailApiService.sendReply(
                                    userId = userId,
                                    threadId = threadId,
                                    toEmail = emailFrom,
                                    subject = emailSubject,
                                    bodyText = aiReply
                                )
                                logger.debug("Reply sent successfully")
                            } catch (e: Exception) {
                                logger.error("Failed to send reply: ${e.message}", e)
                                throw e
                            }

                            // Ensure AI_BOT_REPLY label exists
                            logger.debug("Ensuring AI_BOT_REPLY label exists")
                            val labelId = gmailApiService.ensureLabel(userId, AI_BOT_REPLY_LABEL)
                            logger.debug("Label ID: $labelId")

                            // Add AI_BOT_REPLY label and remove UNREAD
                            logger.debug("Modifying labels for: $messageId")
                            gmailApiService.modifyLabels(
                                userId = userId,
                                messageId = messageId,
                                addLabels = listOf(labelId),
                                removeLabels = listOf("UNREAD")
                            )
                            logger.debug("Labels modified successfully")

                            // Update memory with new correspondence
                            logger.debug("Updating memory after reply for: $messageId")
                            try {
                                correspondentMemoryService.updateAfterReply(
                                    userId = userId,
                                    correspondentEmail = emailFrom,
                                    threadId = threadId,
                                    messageId = messageId,
                                    emailSubject = emailSubject,
                                    emailBody = emailContent,
                                    replyBody = aiReply
                                )
                                logger.debug("Memory updated successfully")
                            } catch (e: Exception) {
                                logger.warn("Failed to update memory (non-fatal): ${e.message}")
                            }

                            logger.info("✅ Successfully replied to email $messageId for user: $userId")
                            repliedCount++
                        }
                    } else {
                        logger.warn("⚠️ Could not extract email details from message: $messageId (from=$emailFrom, subject=$emailSubject)")
                    }
                } catch (e: Exception) {
                    logger.error("❌ Error processing email $messageId for user $userId: ${e.message}", e)
                    // Continue with next email
                }
            }

            logger.info("Auto-reply completed for user $userId: replied=$repliedCount")
            return repliedCount

        } catch (e: Exception) {
            logger.error("Error auto-replying for user $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Extract email sender and subject from Gmail API headers
     * Uses Gmail API response directly instead of parsing text
     */
    private fun getEmailHeadersFromGmail(userId: String, messageId: String): Pair<String, String> {
        try {
            logger.debug("Extracting headers from Gmail API for message: $messageId")
            val headers = gmailApiService.getMessageHeaders(userId, messageId)

            val emailFrom = headers["From"] ?: ""
            val emailSubject = headers["Subject"] ?: ""

            logger.debug("Gmail API headers - From: $emailFrom, Subject: $emailSubject")

            if (emailFrom.isNotEmpty() && emailSubject.isNotEmpty()) {
                // Extract email address from various formats
                val cleanEmail = extractEmailFromHeader(emailFrom)

                if (cleanEmail.isNotEmpty()) {
                    logger.debug("Cleaned email: $cleanEmail")
                    return Pair(cleanEmail, emailSubject)
                } else {
                    logger.warn("Could not extract email from From header: $emailFrom")
                    return Pair("", emailSubject)
                }
            }

            logger.debug("Missing headers - From: $emailFrom, Subject: $emailSubject")
            return Pair(emailFrom, emailSubject)
        } catch (e: Exception) {
            logger.warn("Could not extract from Gmail API: ${e.message}", e)
            return Pair("", "")
        }
    }

    /**
     * Extract email address from From header
     * Handles formats: "Name <email@example.com>", "email@example.com", "Name email@example.com"
     */
    private fun extractEmailFromHeader(fromHeader: String): String {
        // Try to extract from <...> format first
        val angleMatch = Regex("<(.+?)>").find(fromHeader)
        if (angleMatch != null) {
            val email = angleMatch.groupValues.getOrNull(1)
            if (!email.isNullOrEmpty() && email.contains("@")) {
                return email.trim()
            }
        }

        // Try to extract email using regex pattern
        val emailMatch = Regex("([\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,})").find(fromHeader)
        if (emailMatch != null) {
            val email = emailMatch.groupValues.getOrNull(0)
            if (!email.isNullOrEmpty()) {
                return email.trim()
            }
        }

        logger.debug("Could not extract email from header: $fromHeader")
        return ""
    }

    /**
     * Extract sender email from email content
     * Simple extraction - in production use proper email parsing
     */
    private fun extractEmailFrom(emailContent: String): String {
        // Try multiple patterns
        val patterns = listOf(
            Regex("From:\\s*<(.+?)>"),                           // From: <email@example.com>
            Regex("From:\\s*\"?[^\"]*\"?\\s*<(.+?)>"),          // From: "Name" <email@example.com>
            Regex("From:\\s*([\\w.+-]+@[\\w.-]+)")              // From: email@example.com
        )

        for (pattern in patterns) {
            val match = pattern.find(emailContent)
            if (match != null) {
                val email = match.groupValues.getOrNull(1) ?: ""
                if (email.isNotEmpty()) {
                    logger.debug("Extracted email: $email")
                    return email
                }
            }
        }

        logger.debug("Could not extract email from content")
        return ""
    }

    /**
     * Extract subject from email content
     * Simple extraction - in production use proper email parsing
     */
    private fun extractEmailSubject(emailContent: String): String {
        // Try to extract from Subject header
        val subjectMatch = Regex("Subject:\\s*(.+?)(?:\r?\n|$)").find(emailContent)
        val subject = subjectMatch?.groupValues?.getOrNull(1)?.trim() ?: ""

        if (subject.isNotEmpty()) {
            logger.debug("Extracted subject: $subject")
            return subject
        }

        logger.debug("Could not extract subject from content")
        return ""
    }
}

