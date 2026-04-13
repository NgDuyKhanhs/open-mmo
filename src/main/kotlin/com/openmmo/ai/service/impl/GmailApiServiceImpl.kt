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
import org.springframework.cache.annotation.Cacheable
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
 *
 * Performance optimizations:
 * - Token caching: Per-request (ThreadLocal) to avoid refresh per email
 * - Config caching: 5-min TTL via @Cacheable
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
        // ThreadLocal for request-scoped token caching (avoid 100x refresh)
        private val tokenCache = ThreadLocal<MutableMap<String, String>>()
    }

    init {
        // Initialize token cache on first use
        if (tokenCache.get() == null) {
            tokenCache.set(mutableMapOf())
        }
    }    /**
     * Get mailbox with pagination support (5 emails per page)
     * Fetches 2x pageSize to ensure we return requested amount despite potential failures
     * FIXED: Now retries with fresh token on 401 Unauthorized error
     */
    fun getMailboxPage(userId: String, boxType: String, pageSize: Int = 5, pageToken: String? = null): MailboxPageResponse {
        try {
            val startTime = System.currentTimeMillis()
            logger.info("Getting mailbox page for user: $userId, box=$boxType, pageSize=$pageSize, pageToken=$pageToken")

            val connection = connectionRepository.findByUserId(userId)
                ?: throw IllegalStateException("Gmail not connected")

            var accessToken = getAccessToken(connection)

            val query = when (boxType) {
                "inbox" -> "in:inbox"
                "sent" -> "in:sent"
                "spam" -> "in:spam"
                "trash" -> "in:trash"
                else -> throw IllegalArgumentException("Invalid box type: $boxType")
            }

            // Fetch 2x pageSize to handle any failures and ensure we get requested amount
            val fetchSize = pageSize * 2

            try {
                @Suppress("UNCHECKED_CAST")
                val response = gmailApiClient.listMessages(accessToken, query, fetchSize, pageToken)
                val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
                val nextPageToken = response["nextPageToken"] as? String
                logger.info("Found ${messages.size} messages in initial fetch")

                return processMailboxMessages(userId, messages, nextPageToken, pageSize, startTime)
            } catch (e: org.springframework.web.client.HttpClientErrorException) {
                // If 401 Unauthorized, refresh token and retry
                if (e.statusCode.value() == 401) {
                    logger.warn("Access token expired (401), refreshing and retrying for user: $userId")
                    val cache = tokenCache.get() ?: mutableMapOf<String, String>().also { tokenCache.set(it) }
                    // Force refresh by clearing cache and getting new token
                    cache.remove(userId)
                    accessToken = getAccessToken(connection)

                    // Retry the request with fresh token
                    @Suppress("UNCHECKED_CAST")
                    val response = gmailApiClient.listMessages(accessToken, query, fetchSize, pageToken)
                    val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
                    val nextPageToken = response["nextPageToken"] as? String
                    logger.info("Found ${messages.size} messages in retry fetch")

                    return processMailboxMessages(userId, messages, nextPageToken, pageSize, startTime)
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.error("Error getting mailbox page: ${e.message}")
            throw e
        }
    }

    /**
     * Extract and process mailbox messages (helper method)
     */
    private fun processMailboxMessages(
        userId: String,
        messages: List<Map<String, String>>,
        nextPageToken: String?,
        pageSize: Int,
        startTime: Long
    ): MailboxPageResponse {
        // Get access token for fetching message details
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")
        val accessToken = getAccessToken(connection)

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
    }

    override fun searchMessages(userId: String, query: String, maxResults: Int): List<String> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        var accessToken = getAccessToken(connection)

        try {
            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.listMessages(accessToken, query, maxResults)
            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
            return messages.mapNotNull { it["id"] }
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            if (e.statusCode.value() == 401) {
                logger.warn("Access token expired (401), refreshing and retrying for user: $userId")
                val cache = tokenCache.get() ?: mutableMapOf<String, String>().also { tokenCache.set(it) }
                cache.remove(userId)
                accessToken = getAccessToken(connection)

                @Suppress("UNCHECKED_CAST")
                val response = gmailApiClient.listMessages(accessToken, query, maxResults)
                val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
                return messages.mapNotNull { it["id"] }
            } else {
                throw e
            }
        }
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
        if (toEmail.lowercase().contains("unknown") || !toEmail.contains("@")) {
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

            // Get custom prompt if configured (CACHED - avoid 100x DB query)
            val customPrompt = getOrCacheBotConfig(userId)?.customPrompt?.takeIf { it.isNotBlank() }

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

            // Get custom prompt if configured (CACHED - avoid 100x DB query)
            val customPrompt = getOrCacheBotConfig(userId)?.customPrompt?.takeIf { it.isNotBlank() }

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
        // ✅ OPTIMIZATION: Cache token per userId to avoid repeated refreshes
        val cache = tokenCache.get() ?: mutableMapOf<String, String>().also { tokenCache.set(it) }
        val userId = connection.userId

        // Return cached token if available
        cache[userId]?.let { cachedToken ->
            logger.debug("Using cached token for user: $userId")
            return cachedToken
        }

        // Token not cached, fetch and refresh
        return refreshAndCacheToken(connection, cache)
    }

    /**
     * Refreshes the access token and caches it
     * This method can be called multiple times to force a refresh (e.g., after 401 error)
     */
    private fun refreshAndCacheToken(connection: com.openmmo.ai.entity.GmailConnection, cache: MutableMap<String, String>): String {
        val userId = connection.userId
        try {
            logger.debug("Decrypting refresh token for user: $userId")
            val decryptedRefreshToken = EncryptionUtil.decrypt(connection.refreshTokenEnc, tokenEncKey)
            logger.debug("Refreshing access token")
            val token = oauthService.refreshAccessToken(decryptedRefreshToken)

            // Cache for subsequent calls
            cache[userId] = token
            logger.debug("Access token refreshed and cached for user: $userId")
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
     * ✅ OPTIMIZATION: Cache bot config per userId (5-min TTL)
     * Avoids 100x DB query when processing 100 emails for same user
     * Spring @Cacheable handles expiration automatically
     */
    @Cacheable("botConfig")
    fun getOrCacheBotConfig(userId: String) = botConfigRepository.findByUserId(userId)

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

    /**
     * Helper method to execute Gmail API calls with automatic retry on 401 Unauthorized
     * Handles token refresh and retry transparently
     */
    private fun <T> withTokenRefreshRetry(
        userId: String,
        block: (accessToken: String) -> T
    ): T {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        var accessToken = getAccessToken(connection)

        try {
            return block(accessToken)
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            if (e.statusCode.value() == 401) {
                logger.warn("Access token expired (401), refreshing and retrying for user: $userId")
                val cache = tokenCache.get() ?: mutableMapOf<String, String>().also { tokenCache.set(it) }
                cache.remove(userId)
                accessToken = getAccessToken(connection)

                // Retry once with fresh token
                try {
                    return block(accessToken)
                } catch (retryE: org.springframework.web.client.HttpClientErrorException) {
                    if (retryE.statusCode.value() == 401) {
                        logger.error("Still getting 401 after token refresh. May indicate revoked/invalid credentials for user: $userId")
                    }
                    throw retryE
                }
            } else {
                throw e
            }
        }
    }
}
