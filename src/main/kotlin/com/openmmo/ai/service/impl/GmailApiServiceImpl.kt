package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GmailApiClient
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.util.EncryptionUtil
import com.openmmo.ai.dto.MailboxItemResponse
import com.openmmo.ai.dto.MailboxPageResponse
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IGmailOAuthService
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.repository.GmailProcessedMessageRepository
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.entity.GmailProcessedMessage
import com.openmmo.ai.exception.GmailRefreshTokenException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import com.openmmo.ai.client.GroqApiClient
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.time.LocalDateTime

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
 * - Uses Groq API for fast & cheap AI text generation
 */
@Service
class GmailApiServiceImpl(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository,
    private val gmailProcessedMessageRepository: GmailProcessedMessageRepository,
    private val correspondentMemoryRepository: CorrespondentMemoryRepository,
    private val oauthService: IGmailOAuthService,
    private val groqApiClient: GroqApiClient,
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
     * Get mailbox with pagination support (10 emails per page)
     * FEATURES:
     * - Applies subject filter from BotConfig to Gmail API query
     * - Fetches 2x pageSize to ensure we return requested amount
     * - Auto-paginates if current page doesn't have enough matches
     * - FIXED: Retries with fresh token on 401 Unauthorized error
     */
    fun getMailboxPage(userId: String, boxType: String, pageSize: Int = 10, pageToken: String? = null): MailboxPageResponse {
        try {
            val startTime = System.currentTimeMillis()
            logger.info("Getting mailbox page for user: $userId, box=$boxType, pageSize=$pageSize, pageToken=$pageToken")

            val connection = connectionRepository.findByUserId(userId)
                ?: throw IllegalStateException("Gmail not connected")

            var accessToken = getAccessToken(connection)

            // Build base query with box type
            var query = when (boxType) {
                "inbox" -> "in:inbox"
                "sent" -> "in:sent"
                "spam" -> "in:spam"
                "trash" -> "in:trash"
                else -> throw IllegalArgumentException("Invalid box type: $boxType")
            }

            val botConfig = getOrCacheBotConfig(userId)
            botConfig?.triggerSubject?.takeIf { it.isNotBlank() }?.let { triggerSubject ->
                query += " subject:${triggerSubject.trim()}"
                logger.info("Applied subject filter: '$triggerSubject'")
            }

            var retryCount = 0
            val maxRetries = 2

            while (retryCount <= maxRetries) {
                try {
                    logger.debug("Attempting to fetch mailbox page, retry=$retryCount/$maxRetries, box=$boxType")
                    @Suppress("UNCHECKED_CAST")
                    val response = gmailApiClient.listMessages(accessToken, query, pageSize, pageToken)
                    val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
                    val nextPageToken = response["nextPageToken"] as? String
                    logger.debug("Successfully fetched ${messages.size} messages after $retryCount retries")
                    return processMailboxMessages(userId, messages, nextPageToken, pageSize, startTime, boxType)
                } catch (e: org.springframework.web.client.HttpClientErrorException) {
                    // If 401 Unauthorized, refresh token and retry
                    if (e.statusCode.value() == 401 && retryCount < maxRetries) {
                        logger.warn("Access token expired (401), attempt ${retryCount + 1}/$maxRetries - refreshing token for user: $userId")
                        try {
                            val cache = tokenCache.get() ?: mutableMapOf<String, String>().also { tokenCache.set(it) }
                            cache.remove(userId)
                            accessToken = getAccessToken(connection)
                            logger.debug("Token refreshed successfully, retrying mailbox fetch...")
                            retryCount++
                        } catch (refreshError: Exception) {
                            logger.error("Failed to refresh access token during mailbox fetch: ${refreshError.message}", refreshError)
                            throw refreshError
                        }
                    } else if (e.statusCode.value() == 401) {
                        logger.error("Access token still invalid after $maxRetries retries for mailbox, user needs to reconnect: $userId")
                        throw e
                    } else {
                        logger.error("HTTP error fetching mailbox: ${e.statusCode} - ${e.responseBodyAsString}")
                        throw e
                    }
                }
            }

            throw IllegalStateException("Failed to fetch mailbox page after $maxRetries retries")
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
        startTime: Long,
        boxType: String
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

                    // Look up aiProvider if this is a sent email
                    var aiProvider: String? = null
                    if (boxType == "sent") {
                        // Query by threadId to find the reply record
                        val processed = gmailProcessedMessageRepository.findByUserIdAndThreadId(userId, threadId)
                        aiProvider = processed?.aiProvider
                    }

                    MailboxItemResponse(
                        id = full.id,
                        threadId = threadId,
                        from = full.from,
                        subject = full.subject,
                        date = full.date,
                        snippet = full.snippet,
                        labels = full.labels,
                        aiprovider = aiProvider
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
        var retryCount = 0
        val maxRetries = 2

        while (retryCount <= maxRetries) {
            try {
                logger.debug("Attempting to search messages, retry=$retryCount/$maxRetries")
                @Suppress("UNCHECKED_CAST")
                val response = gmailApiClient.listMessages(accessToken, query, maxResults)
                val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
                logger.debug("Successfully fetched ${messages.size} messages after $retryCount retries")
                return messages.mapNotNull { it["id"] }
            } catch (e: org.springframework.web.client.HttpClientErrorException) {
                if (e.statusCode.value() == 401 && retryCount < maxRetries) {
                    logger.warn("Access token expired (401), attempt ${retryCount + 1}/$maxRetries - refreshing token for user: $userId")
                    try {
                        // Force refresh token
                        val cache = tokenCache.get() ?: mutableMapOf<String, String>().also { tokenCache.set(it) }
                        cache.remove(userId)
                        accessToken = getAccessToken(connection)
                        logger.debug("Token refreshed successfully, retrying...")
                        retryCount++
                    } catch (refreshError: Exception) {
                        logger.error("Failed to refresh access token: ${refreshError.message}", refreshError)
                        throw refreshError
                    }
                } else if (e.statusCode.value() == 401) {
                    logger.error("Access token still invalid after $maxRetries retries, user needs to reconnect: $userId")
                    throw e
                } else {
                    logger.error("HTTP error searching messages: ${e.statusCode} - ${e.responseBodyAsString}")
                    throw e
                }
            } catch (e: Exception) {
                logger.error("Unexpected error searching messages: ${e.message}", e)
                throw e
            }
        }

        throw IllegalStateException("Failed to search messages after $maxRetries retries")
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
        val response = gmailApiClient.getMessageHeaders(accessToken, messageId)

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

            // Generate reply using Groq
            val aiReply = groqApiClient.generateText(fullPrompt)
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

            // Generate reply using Groq
            val aiReply = groqApiClient.generateText(fullPrompt)
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
     * Public method to get access token - used by other services like ConversationContextService
     */
    fun getAccessTokenPublic(connection: com.openmmo.ai.entity.GmailConnection): String {
        return getAccessToken(connection)
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
        } catch (e: GmailRefreshTokenException) {
            logger.error("Gmail refresh token is invalid or expired for user: $userId. User needs to reconnect.", e)
            // Don't wrap it, let it propagate so the controller can catch it
            throw e
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

    /**
     * Save the AI provider for a processed message
     * Called after sending a reply to track which AI provider was used
     */
    fun saveProcessedMessage(userId: String, messageId: String, threadId: String, aiProvider: String) {
        try {
            logger.debug("Saving processed message record: userId=$userId, messageId=$messageId, aiProvider=$aiProvider")
            val processed = GmailProcessedMessage(
                userId = userId,
                messageId = messageId,
                threadId = threadId,
                aiProvider = aiProvider,
                processedAt = LocalDateTime.now()
            )
            gmailProcessedMessageRepository.save(processed)
            logger.debug("Processed message saved successfully")
        } catch (e: Exception) {
            logger.warn("Failed to save processed message record (non-fatal): ${e.message}")
            // Non-fatal error - don't throw, just log
        }
    }

    override fun getContactsList(userId: String): List<String> {
        return try {
            logger.debug("Getting contacts list from Gmail inbox for user: $userId")

            val connection = connectionRepository.findByUserId(userId)
                ?: run {
                    logger.warn("Gmail not connected for user: $userId")
                    return emptyList()
                }

            val accessToken = getAccessToken(connection)

            // Query Gmail API to get emails from real people only (no system/auto-generated)
            // Comprehensive filters to extract only genuine contacts:
            // - in:inbox: only inbox emails
            // - newer_than:30d: only last 30 days (recent contacts)
            // - -is:mailing-list: exclude mailing lists
            // - -category:promotions/updates/forums/social: exclude Gmail auto-categories
            // - -from: exclude noreply, no-reply, donotreply, do-not-reply, mailer-daemon, postmaster
            // - -subject: exclude emails with "do not reply" or "unsubscribe" in subject
            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.listMessages(
                accessToken = accessToken,
                query = "in:inbox newer_than:30d " +
                        "-is:mailing-list " +
                        "-category:promotions -category:updates -category:forums -category:social " +
                        "-from:(noreply OR \"no-reply\" OR donotreply OR \"do-not-reply\" OR \"mailer-daemon\" OR postmaster) " +
                        "-subject:(\"do not reply\" OR unsubscribe)",
                maxResults = 100  // Get up to 100 recent emails from real people
            )

            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
            logger.debug("Found ${messages.size} emails in inbox")

            // Extract unique sender emails from all inbox messages
            val contactEmails = mutableSetOf<String>()

            for (msg in messages) {
                val msgId = msg["id"] ?: continue
                try {
                    // Fetch message to get "From" header
                    val msgDetails = gmailApiClient.getMessageMetadata(accessToken, msgId)
                    @Suppress("UNCHECKED_CAST")
                    val payload = msgDetails["payload"] as? Map<String, Any>
                    @Suppress("UNCHECKED_CAST")
                    val headers = payload?.get("headers") as? List<Map<String, String>>

                    // Extract "From" header and parse email address
                    val fromHeader = headers?.find { it["name"] == "From" }?.get("value") ?: continue
                    val email = extractEmailFromHeader(fromHeader)

                    if (email.isNotEmpty() && email.contains("@")) {
                        contactEmails.add(email)
                    }
                } catch (e: Exception) {
                    logger.debug("Failed to extract contact from message $msgId: ${e.message}")
                    // Continue processing other messages
                }
            }

            val sortedContacts = contactEmails.sorted()
            logger.debug("Extracted ${sortedContacts.size} unique contact emails from inbox")
            sortedContacts
        } catch (e: Exception) {
            logger.error("Error getting contacts list from Gmail: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Extract email address from RFC 2822 format header
     * Input examples:
     * - "John Doe <john@example.com>" → "john@example.com"
     * - "john@example.com" → "john@example.com"
     * - "contact@domain.co.uk" → "contact@domain.co.uk"
     */
    private fun extractEmailFromHeader(fromHeader: String): String {
        // Try to extract from <...> format first (most common)
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
}
