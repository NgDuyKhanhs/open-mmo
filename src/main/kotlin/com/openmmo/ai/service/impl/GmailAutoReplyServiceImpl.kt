package com.openmmo.ai.service.impl

import com.openmmo.ai.service.IGmailAutoReplyService
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IGmailBotService
import com.openmmo.ai.service.ICorrespondentMemoryService
import com.openmmo.ai.service.IGeminiAiService
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.repository.GmailConnectionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Gmail Auto-Reply Service Implementation
 * Automatically generates and sends replies to emails matching trigger conditions
 * Uses user's selected AI provider (Groq for speed/cost, Gemini for quality)
 */
@Service
class GmailAutoReplyServiceImpl(
    private val gmailApiService: IGmailApiService,
    private val gmailBotService: IGmailBotService,
    private val botConfigRepository: GmailBotConfigRepository,
    private val connectionRepository: GmailConnectionRepository,
    private val correspondentMemoryService: ICorrespondentMemoryService,
    @Qualifier("groqAiServiceImpl")
    private val groqAiService: IGeminiAiService,
    @Qualifier("geminiAiServiceImpl")
    private val geminiAiService: IGeminiAiService
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
            val query = "subject:${triggerSubject.lowercase()} -label:$AI_BOT_REPLY_LABEL is:unread"
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
                        // Get full headers for skip checks
                        val headers = gmailApiService.getMessageHeaders(userId, messageId)

                        // Check skip rules
                        if (shouldSkipEmail(emailFrom, emailSubject, emailContent, headers, userId)) {
                            logger.info("Skipping email $messageId for user $userId due to skip rules")
                        } else {
                            // Load memory for this correspondent
                            val memory = correspondentMemoryService.getOrCreate(userId, emailFrom)
                            val memoryContext = correspondentMemoryService.buildMemoryContextText(memory)

                            // Get user's selected AI provider and custom prompt
                            val botConfig = botConfigRepository.findByUserId(userId)
                            val selectedProvider = botConfig?.aiProvider?.lowercase() ?: "groq"
                            val customPrompt = botConfig?.customPrompt?.takeIf { it.isNotBlank() }
                            logger.debug("Using AI provider: $selectedProvider for user: $userId")

                            // Generate AI reply with selected provider
                            logger.debug("Generating AI reply with memory for: $messageId")
                            var aiReply: String
                            try {
                                val aiService = getAiServiceForProvider(selectedProvider)
                                val fullPrompt = buildPromptWithMemory(emailContent, memoryContext, customPrompt)
                                aiReply = aiService.generateText(fullPrompt)
                                logger.debug("AI reply generated via $selectedProvider, length: ${aiReply.length}")
                            } catch (e: Exception) {
                                logger.warn("⚠️ Failed to generate AI reply via $selectedProvider (${e.message}), using fallback response")
                                aiReply = generateFallbackReply(emailFrom, emailSubject)
                                logger.debug("Fallback reply used, length: ${aiReply.length}")
                            }

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

                                    // Save processed message with AI provider info
                                    if (gmailApiService is GmailApiServiceImpl) {
                                        gmailApiService.saveProcessedMessage(
                                            userId = userId,
                                            messageId = messageId,
                                            threadId = threadId,
                                            aiProvider = selectedProvider
                                        )
                                    }
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
                                        replyBody = aiReply,
                                        aiProvider = selectedProvider
                                    )
                                    logger.debug("Memory updated successfully")
                                } catch (e: Exception) {
                                    logger.warn("Failed to update memory (non-fatal): ${e.message}")
                                }

                                logger.info("Successfully replied to email $messageId for user: $userId")
                                repliedCount++
                            }
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

    /**
     * Check if email should be skipped based on multiple rules
     */
    private fun shouldSkipEmail(
        emailFrom: String,
        emailSubject: String,
        emailBody: String,
        headers: Map<String, String>,
        userId: String
    ): Boolean {
        val headersLower = headers.mapKeys { it.key.lowercase() }
        val contentLower = (emailSubject + " " + emailBody).lowercase()

        // Rule 1: Auto-generated/auto-replied emails
        if (headersLower.containsKey("auto-submitted") ||
            headersLower["auto-submitted"]?.contains("auto-generated") == true ||
            headersLower["auto-submitted"]?.contains("auto-replied") == true ||
            headersLower.containsKey("x-auto-response-suppress") ||
            headersLower["precedence"]?.let { it.contains("bulk") || it.contains("junk") || it.contains("list") } == true) {
            logger.info("Skip rule 1: Auto-generated email")
            return true
        }

        // Rule 2: Mailing lists/newsletters
        if (headersLower.containsKey("list-id") || headersLower.containsKey("list-unsubscribe")) {
            logger.info("Skip rule 2: Mailing list/newsletter")
            return true
        }

        // Rule 3: Bounce/delivery failures
        if (emailFrom.lowercase().contains("mailer-daemon") ||
            emailFrom.lowercase().contains("postmaster") ||
            contentLower.contains("undelivered") ||
            contentLower.contains("mail delivery subsystem")) {
            logger.info("Skip rule 3: Bounce/delivery failure")
            return true
        }

        // Rule 4: No-reply addresses
        if (emailFrom.lowercase().contains("no-reply") ||
            emailFrom.lowercase().contains("noreply") ||
            emailFrom.lowercase().contains("do-not-reply") ||
            emailFrom.lowercase().contains("donotreply")) {
            logger.info("Skip rule 4: No-reply address")
            return true
        }

        // Rule 5: Promotions/social/receipts/OTP/notifications
        val categories = headersLower["x-gm-raw"]?.lowercase() ?: ""
        if (categories.contains("promotions") ||
            categories.contains("social") ||
            contentLower.contains("otp") ||
            contentLower.contains("verification code") ||
            contentLower.contains("confirm code") ||
            contentLower.contains("receipt") ||
            contentLower.contains("invoice") ||
            contentLower.contains("notification") ||
            contentLower.contains("alert")) {
            logger.info("Skip rule 5: Promotions/social/OTP/notifications")
            return true
        }

        // Rule 6: Calendar invites
        if (emailBody.contains("BEGIN:VCALENDAR") ||
            headersLower["content-type"]?.contains("text/calendar") == true) {
            logger.info("Skip rule 6: Calendar invite")
            return true
        }

        // Rule 7: Email from self
        val userEmail = headersLower["to"]?.lowercase() ?: ""
        if (emailFrom.lowercase() == userEmail.lowercase()) {
            logger.info("Skip rule 7: Email from self")
            return true
        }

        return false
    }

    /**
     * Generate fallback reply when AI generation fails
     * Notifies user that current AI provider is experiencing issues
     * Used as failsafe when selected AI service (Groq/Gemini) is unavailable or returns error
     */
    private fun generateFallbackReply(senderEmail: String, senderSubject: String): String {
        return """Thank you for your email. Our AI service is currently experiencing temporary issues. 

Please try again in a few moments, or you may want to switch to an alternative AI provider for faster processing.

We appreciate your patience and will resolve this shortly.

Best regards"""
    }

    /**
     * Build prompt with memory context and custom prompt for AI generation
     * Formats the email, memory context, and custom instructions into a structured prompt
     */
    private fun buildPromptWithMemory(emailContent: String, memoryContext: String, customPrompt: String?): String {
        return buildString {
            append("You are an AI email assistant for auto-replies.\n\n")
            append("INSTRUCTIONS:\n")
            append("- Write professional, concise, friendly replies\n")
            append("- Match the email's tone and style\n")
            append("- Do NOT include greetings like 'Hi', 'Hello' or 'Dear'\n")
            append("- Do NOT include signatures like 'Best regards', 'Thanks'\n")
            append("- Do NOT make up information - only reply to what was asked\n")
            append("- Be helpful and courteous\n")
            append("- Keep replies 2-4 sentences max (unless complex topic)\n")
            append("- Format: Direct reply without preamble\n\n")

            // Add custom prompt if provided
            if (!customPrompt.isNullOrBlank()) {
                append("═══ CUSTOM INSTRUCTIONS ═══\n")
                append(customPrompt)
                append("\n\n")
            }

            if (memoryContext.isNotBlank()) {
                append("═══ CONVERSATION HISTORY ═══\n")
                append(memoryContext.take(800))
                append("\n\n")
            }

            append("═══ EMAIL TO REPLY ═══\n")
            append(emailContent.take(3000))
            append("\n\n")
            append("═══ YOUR REPLY (no greeting/signature) ═══\n")
        }
    }

    /**
     * Get the appropriate AI service based on user's provider preference
     * @param provider "groq" or "gemini"
     * @return IGeminiAiService instance (either Groq or Gemini impl)
     */
    private fun getAiServiceForProvider(provider: String): IGeminiAiService {
        return when (provider.lowercase()) {
            "gemini" -> {
                logger.debug("Using Gemini AI service")
                geminiAiService
            }
            else -> {
                logger.debug("Using Groq AI service (default)")
                groqAiService
            }
        }
    }
}
