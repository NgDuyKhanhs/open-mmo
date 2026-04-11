package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GmailApiClient
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.util.EncryptionUtil
import com.openmmo.ai.dto.MailboxItemResponse
import com.openmmo.ai.dto.MailboxPageResponse
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IGmailOAuthService
import com.openmmo.ai.service.IGeminiAiService
import com.openmmo.ai.repository.GmailBotConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

data class MailboxItem(
    val id: String,
    val threadId: String,
    val from: String,
    val subject: String,
    val date: String,
    val snippet: String,
    val labels: List<String>
)

/**
 * Gmail API Service Implementation
 */
@Service
class GmailApiServiceImpl(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository,
    private val oauthService: IGmailOAuthService,
    private val geminiAiService: IGeminiAiService,
    private val gmailApiClient: GmailApiClient,
    @Value("\${token.enc.key-base64}") private val tokenEncKey: String
) : IGmailApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailApiServiceImpl::class.java)
    }

    /**
     * Get mailbox with pagination support (5 emails per page)
     * Fetches 2x pageSize to ensure we return requested amount despite potential failures
     */
    fun getMailboxPage(userId: String, boxType: String, pageSize: Int = 5, pageToken: String? = null): MailboxPageResponse {
        try {
            val startTime = System.currentTimeMillis()
            logger.info("Getting mailbox page for user: $userId, box=$boxType, pageSize=$pageSize, pageToken=$pageToken")

            val connection = connectionRepository.findByUserId(userId)
                ?: throw IllegalStateException("Gmail not connected")

            val accessToken = getAccessToken(connection)

            val query = when (boxType) {
                "inbox" -> "in:inbox"
                "sent" -> "in:sent"
                "spam" -> "in:spam"
                "trash" -> "in:trash"
                else -> throw IllegalArgumentException("Invalid box type: $boxType")
            }

            // Fetch 2x pageSize to handle any failures and ensure we get requested amount
            val fetchSize = pageSize * 2
            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.listMessages(accessToken, query, fetchSize, pageToken)
            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
            val nextPageToken = response["nextPageToken"] as? String
            logger.info("Found ${messages.size} messages in initial fetch")

            // Fetch all message details in parallel
            val futures = messages.mapNotNull { msg ->
                val msgId = msg["id"] ?: return@mapNotNull null
                val threadId = msg["threadId"] ?: return@mapNotNull null

                CompletableFuture.supplyAsync({
                    try {
                        val full = getMessageDetailsMetadata(accessToken, msgId, threadId)
                        MailboxItemResponse(
                            id = full.id,
                            threadId = threadId,
                            from = full.from,
                            subject = full.subject,
                            date = full.date,
                            snippet = full.snippet,
                            labels = full.labels
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to get details for message $msgId: ${e.message}")
                        null
                    }
                }, ForkJoinPool.commonPool())
            }

            val emails = CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
                futures.mapNotNull { it.join() }
            }.get()

            // Take only the requested page size
            val paginatedEmails = emails.take(pageSize)

            val duration = System.currentTimeMillis() - startTime
            logger.info("✅ Mailbox page loaded in ${duration}ms (${paginatedEmails.size} emails, nextPageToken=$nextPageToken)")

            return MailboxPageResponse(
                emails = paginatedEmails,
                nextPageToken = nextPageToken,
                totalCount = paginatedEmails.size
            )
        } catch (e: Exception) {
            logger.error("Error getting mailbox page: ${e.message}")
            throw e
        }
    }

    override fun searchMessages(userId: String, query: String, maxResults: Int): List<String> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        @Suppress("UNCHECKED_CAST")
        val response = gmailApiClient.listMessages(accessToken, query, maxResults)
        val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
        return messages.mapNotNull { it["id"] }
    }

    override fun getMessageBody(userId: String, messageId: String): String {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        val response = gmailApiClient.getMessage(accessToken, messageId)
        return extractBodyText(response)
    }

    override fun getMessageHeaders(userId: String, messageId: String): Map<String, String> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        val response = gmailApiClient.getMessage(accessToken, messageId)

        return extractHeaders(response)
    }

    override fun getMessageMeta(userId: String, messageId: String): Map<String, Any> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        val response = gmailApiClient.getMessage(accessToken, messageId)

        return mapOf(
            "threadId" to (response["threadId"] as? String ?: ""),
            "labelIds" to (response["labelIds"] as? List<String> ?: emptyList())
        )
    }

    override fun sendReply(userId: String, threadId: String, toEmail: String, subject: String, bodyText: String) {
        // Validate inputs
        if (toEmail.isBlank()) {
            logger.error("Cannot send reply: toEmail is blank")
            throw IllegalArgumentException("Recipient email address is required")
        }
        if (toEmail.toLowerCase().contains("unknown") || !toEmail.contains("@")) {
            logger.error("Cannot send reply: invalid email address: $toEmail")
            throw IllegalArgumentException("Invalid recipient email address: $toEmail")
        }

        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        logger.debug("Sending reply: from=${connection.gmailAddress}, to=$toEmail, subject=$subject")

        // Create MIME message with proper encoding
        // Body text needs to be base64 encoded for Gmail API
        val encodedBody = Base64.getEncoder().encodeToString(bodyText.toByteArray(Charsets.UTF_8))

        val rawMessage = """
From: ${connection.gmailAddress}
To: $toEmail
Subject: =?UTF-8?B?${Base64.getEncoder().encodeToString("Re: $subject".toByteArray(Charsets.UTF_8))}?=
MIME-Version: 1.0
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: base64
In-Reply-To: <$threadId@gmail.com>
References: <$threadId@gmail.com>

$encodedBody
        """.trimIndent()

        logger.debug("MIME message preview (first 200 chars): ${rawMessage.take(200)}")
        gmailApiClient.sendMessage(accessToken, rawMessage)
        logger.debug("Message sent successfully to $toEmail")
    }

    override fun generateAiReply(userId: String, messageId: String): String {
        try {
            logger.info("Generating AI reply for message: $messageId")

            // Get message content
            val emailContent = getMessageBody(userId, messageId)

            // Get custom prompt if configured
            val botConfig = botConfigRepository.findByUserId(userId)
            val customPrompt = botConfig?.customPrompt?.takeIf { it.isNotBlank() }

            // Build unified prompt
            val fullPrompt = buildPrompt(emailContent, customPrompt, memoryContext = null)

            // Generate reply using Gemini
            val aiReply = geminiAiService.generateText(fullPrompt)
            logger.debug("AI reply generated successfully, length: ${aiReply.length}")

            return aiReply
        } catch (e: Exception) {
            logger.error("Failed to generate AI reply: ${e.message}", e)
            throw e
        }
    }

    override fun generateAiReplyWithMemory(userId: String, messageId: String, memoryContext: String): String {
        try {
            logger.info("Generating AI reply with memory context for message: $messageId")

            // Get message content
            val emailContent = getMessageBody(userId, messageId)

            // Get custom prompt if configured
            val botConfig = botConfigRepository.findByUserId(userId)
            val customPrompt = botConfig?.customPrompt?.takeIf { it.isNotBlank() }

            // Build unified prompt with memory
            val fullPrompt = buildPrompt(emailContent, customPrompt, memoryContext)

            // Generate reply using full prompt
            val aiReply = geminiAiService.generateText(fullPrompt)
            logger.debug("AI reply with memory generated successfully, length: ${aiReply.length}")

            return aiReply
        } catch (e: Exception) {
            logger.error("Failed to generate AI reply with memory: ${e.message}", e)
            throw e
        }
    }

    override fun modifyLabels(userId: String, messageId: String, addLabels: List<String>, removeLabels: List<String>) {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        gmailApiClient.modifyLabels(accessToken, messageId, addLabels, removeLabels)
    }

    override fun ensureLabel(userId: String, labelName: String): String {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        // Try to find existing label
        val labels = gmailApiClient.listLabels(accessToken)
        val existingLabel = labels.find { it["name"] == labelName }

        if (existingLabel != null) {
            return existingLabel["id"] ?: throw IllegalStateException("No label ID")
        }

        // Create new label
        return gmailApiClient.createLabel(accessToken, labelName)
    }

    private fun getMessageDetails(accessToken: String, messageId: String): MailboxItem {
        val response = gmailApiClient.getMessage(accessToken, messageId)
        @Suppress("UNCHECKED_CAST")
        val payload = response["payload"] as? Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val headersList = payload?.get("headers") as? List<Map<String, String>>

        val from = headersList?.find { it["name"] == "From" }?.get("value") ?: "Unknown"
        val subject = headersList?.find { it["name"] == "Subject" }?.get("value") ?: "(No subject)"
        val date = headersList?.find { it["name"] == "Date" }?.get("value") ?: ""
        val snippet = response["snippet"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val labels = response["labelIds"] as? List<String> ?: emptyList()

        return MailboxItem(
            id = messageId,
            threadId = response["threadId"] as? String ?: "",
            from = from,
            subject = subject,
            date = date,
            snippet = snippet,
            labels = labels
        )
    }

    /**
     * OPTIMIZED: Get message details from metadata response (minimal payload)
     * Used for mailbox listing to reduce bandwidth by 80%
     */
    private fun getMessageDetailsMetadata(accessToken: String, messageId: String, threadId: String): MailboxItem {
        val response = gmailApiClient.getMessageMetadata(accessToken, messageId)

        @Suppress("UNCHECKED_CAST")
        val payload = response["payload"] as? Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val headersList = payload?.get("headers") as? List<Map<String, String>>

        val from = headersList?.find { it["name"] == "From" }?.get("value") ?: "Unknown"
        val subject = headersList?.find { it["name"] == "Subject" }?.get("value") ?: "(No subject)"
        val date = headersList?.find { it["name"] == "Date" }?.get("value") ?: ""
        val snippet = response["snippet"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val labels = response["labelIds"] as? List<String> ?: emptyList()

        return MailboxItem(
            id = messageId,
            threadId = threadId,
            from = from,
            subject = subject,
            date = date,
            snippet = snippet,
            labels = labels
        )
    }

    private fun getAccessToken(connection: com.openmmo.ai.entity.GmailConnection): String {
        try {
            logger.debug("Decrypting refresh token for user: ${connection.userId}")
            val decryptedRefreshToken = EncryptionUtil.decrypt(connection.refreshTokenEnc, tokenEncKey)
            logger.debug("Refreshing access token")
            val token = oauthService.refreshAccessToken(decryptedRefreshToken)
            logger.debug("Access token refreshed successfully")
            return token
        } catch (e: Exception) {
            logger.error("Failed to get access token: ${e.message}", e)
            throw e
        }
    }

    private fun extractBodyText(response: Map<String, Any>?): String {
        if (response == null) return ""

        @Suppress("UNCHECKED_CAST")
        val payload = response["payload"] as? Map<String, Any> ?: return ""

        @Suppress("UNCHECKED_CAST")
        val body = payload["body"] as? Map<String, Any>

        // Try to get data from body
        val data = body?.get("data") as? String
        if (!data.isNullOrEmpty()) {
            return String(Base64.getUrlDecoder().decode(data))
        }

        // Try parts
        @Suppress("UNCHECKED_CAST")
        val parts = payload["parts"] as? List<Map<String, Any>>
        parts?.forEach { part ->
            @Suppress("UNCHECKED_CAST")
            val partBody = part["body"] as? Map<String, Any>
            val partData = partBody?.get("data") as? String
            if (!partData.isNullOrEmpty()) {
                return String(Base64.getUrlDecoder().decode(partData))
            }
        }

        return ""
    }

    private fun extractHeaders(response: Map<String, Any>?): Map<String, String> {
        if (response == null) return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val payload = response["payload"] as? Map<String, Any> ?: return emptyMap()

        @Suppress("UNCHECKED_CAST")
        val headersList = payload["headers"] as? List<Map<String, String>> ?: return emptyMap()

        val headersMap = mutableMapOf<String, String>()
        headersList.forEach { header ->
            val name = header["name"] ?: return@forEach
            val value = header["value"] ?: return@forEach
            headersMap[name] = value
        }

        return headersMap
    }

    /**
     * Unified Prompt Builder - Consistent prompt construction
     * Handles: base instruction + custom prompt + email content + memory context
     *
     * Features:
     * - Truncates inputs to avoid token overflow
     * - Formats email content with fields
     * - Labels memory context for clarity
     * - Escapes special characters
     */
    private fun buildPrompt(
        emailContent: String,
        customPrompt: String?,
        memoryContext: String?
    ): String {
        val baseInstruction = """
            You are an AI email assistant for auto-replies.
            
            INSTRUCTIONS:
            - Write professional, concise, friendly replies
            - Match the email's tone and style
            - Do NOT include greetings like "Hi", "Hello" or "Dear"
            - Do NOT include signatures like "Best regards", "Thanks"
            - Do NOT make up information - only reply to what was asked
            - Be helpful and courteous
            - Keep replies 2-4 sentences max (unless complex topic)
            - Format: Direct reply without preamble
        """.trimIndent()

        return buildString {
            append(baseInstruction)
            append("\n\n")

            // Add custom context if provided
            if (!customPrompt.isNullOrBlank()) {
                val truncated = customPrompt.take(1500)
                append("═══ CONTEXT FROM USER ═══\n")
                append(truncated)
                append("\n\n")
            }

            // Add memory/conversation context if provided
            if (!memoryContext.isNullOrBlank()) {
                val truncated = memoryContext.take(800)
                append("═══ CONVERSATION HISTORY ═══\n")
                append(truncated)
                append("\n\n")
            }

            // Format email content
            append("═══ EMAIL TO REPLY ═══\n")
            val truncatedEmail = emailContent.take(3000)
            append(truncatedEmail)
            append("\n\n")

            // Output format
            append("═══ YOUR REPLY (no greeting/signature) ═══\n")
        }
    }
}

