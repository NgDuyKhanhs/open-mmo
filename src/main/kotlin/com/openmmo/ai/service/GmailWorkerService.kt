package com.openmmo.ai.service

import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.repository.GmailProcessedMessageRepository
import com.openmmo.ai.entity.GmailProcessedMessage
import com.openmmo.ai.util.EncryptionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

@Service
class GmailWorkerService(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository,
    private val processedRepository: GmailProcessedMessageRepository,
    private val gmailApiService: GmailApiService,
    private val geminiService: GeminiService,
    private val oauthService: GmailOAuthService,
    @Value("\${token.enc.key-base64}") private val tokenEncKey: String
) {

    private val logger = LoggerFactory.getLogger(GmailWorkerService::class.java)

    @Scheduled(fixedDelayString = "PT1M") // Run every minute
    fun pollAndReplyEmails() {
        logger.debug("Starting Gmail polling job")

        try {
            // Get all users with connected Gmail and enabled bot
            val configs = botConfigRepository.findAll()
                .filter { it.enabled }

            configs.forEach { config ->
                try {
                    processUserEmails(config.userId)
                } catch (e: Exception) {
                    logger.error("Error processing emails for user ${config.userId}", e)
                    updateBotError(config.userId, e.message ?: "Unknown error")
                }
            }
        } catch (e: Exception) {
            logger.error("Error in Gmail polling job", e)
        }
    }

    private fun processUserEmails(userId: String) {
        val config = botConfigRepository.findByUserId(userId)
            ?: throw IllegalStateException("No bot config for user $userId")

        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("No Gmail connection for user $userId")

        val accessToken = getAccessToken(connection)

        // Ensure processed label exists
        val labelId = gmailApiService.ensureLabel(userId, config.processedLabel)

        // Search for unread emails with trigger subject
        val query = buildSearchQuery(config.triggerSubject, config.processedLabel, connection.gmailAddress)
        val messageIds = gmailApiService.searchMessages(userId, query, maxResults = 10)

        logger.info("Found ${messageIds.size} emails to process for user $userId")

        messageIds.forEach { messageId ->
            try {
                // Check if already processed
                if (processedRepository.findByUserIdAndMessageId(userId, messageId) != null) {
                    logger.debug("Message $messageId already processed, skipping")
                    return@forEach
                }

                // Get full message details
                val messageDetails = gmailApiService.getMessageDetails(accessToken, messageId)
                val messageBody = gmailApiService.getMessageBody(accessToken, messageId)

                // Generate reply using Gemini
                val reply = geminiService.generateReply(
                    userId,
                    messageDetails.subject,
                    messageBody,
                    messageDetails.from,
                    config.triggerSubject  // Pass trigger subject to use in prompt
                )

                // Send reply
                gmailApiService.sendReply(
                    userId,
                    messageDetails.threadId,
                    messageDetails.from,
                    messageDetails.subject,
                    reply
                )

                // Modify labels
                gmailApiService.modifyLabels(
                    userId,
                    messageId,
                    addLabels = listOf(labelId),
                    removeLabels = listOf("UNREAD")
                )

                // Record processed message
                processedRepository.save(GmailProcessedMessage(
                    userId = userId,
                    messageId = messageId,
                    threadId = messageDetails.threadId
                ))

                logger.info("Successfully replied to message $messageId")
            } catch (e: Exception) {
                logger.error("Error processing message $messageId", e)
            }
        }

        // Update bot config with run time
        val updated = config.copy(
            lastRunAt = LocalDateTime.now(),
            lastError = null
        )
        botConfigRepository.save(updated)
    }

    private fun buildSearchQuery(triggerSubject: String, processedLabel: String, gmailAddress: String): String {
        // Exclude automated emails from services
        val excludedEmails = listOf(
            "noreply@", "no-reply@", "donotreply@", "do-not-reply@",
            "notifications@", "automated@", "system@", "robot@",
            "mailer-daemon@", "mail-daemon@", "postmaster@",
            "support@youtube.com", "noreply@youtube.com",
            "no-reply@github.com", "noreply@github.com",
            "noreply@google.com", "noreply@accounts.google.com"
        )

        val excludeFilters = excludedEmails.joinToString(" ") { "-from:$it" }

        return "in:inbox is:unread subject:$triggerSubject -label:$processedLabel -from:$gmailAddress $excludeFilters"
    }

    private fun getAccessToken(connection: com.openmmo.ai.entity.GmailConnection): String {
        val decryptedRefreshToken = EncryptionUtil.decrypt(connection.refreshTokenEnc, tokenEncKey)
        return oauthService.refreshAccessToken(decryptedRefreshToken)
    }

    private fun updateBotError(userId: String, error: String) {
        val config = botConfigRepository.findByUserId(userId)
        if (config != null) {
            botConfigRepository.save(config.copy(lastError = error))
        }
    }
}



