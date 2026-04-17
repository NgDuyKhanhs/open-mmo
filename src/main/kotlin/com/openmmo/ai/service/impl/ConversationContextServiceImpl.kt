package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GmailApiClient
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.service.IConversationContextService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Conversation Context Service Implementation
 *
 * Responsibilities:
 * - Fetch recent emails from Gmail thread with a specific contact
 * - Build truncated conversation context for AI reminder generation
 * - Minimize Gmail API calls (target: 8 calls max per context fetch)
 * - Cache context per (userId, contactEmail) for 15 minutes
 *
 * Gmail API Calls per fetch (optimized):
 * 1. listMessages(maxResults=1) to find latest thread
 * 2. threads.get(threadId, format=metadata) to list message IDs
 * 3. messages.get(messageId) for last K messages (K=6)
 * Total: ~8 API calls max
 */
@Service
class ConversationContextServiceImpl(
    private val gmailApiClient: GmailApiClient,
    private val connectionRepository: GmailConnectionRepository,
    private val gmailApiServiceImpl: GmailApiServiceImpl
) : IConversationContextService {

    companion object {
        private val logger = LoggerFactory.getLogger(ConversationContextServiceImpl::class.java)

        // Context limits (to optimize token usage)
        private const val MAX_CHARS_PER_MESSAGE = 1200
        private const val MAX_TOTAL_CHARS = 6000
        private const val MAX_MESSAGES_IN_THREAD = 6  // Last 6 messages
    }

    /**
     * Get conversation context for reminder generation
     * Cached for 15 minutes to avoid repeated Gmail API calls
     */
    @Cacheable(
        cacheNames = ["reminderContext"],
        key = "'reminderContext:' + #userId + ':' + #contactEmail",
        unless = "#result == null"
    )
    override fun getContextForReminder(userId: String, contactEmail: String): String? {
        logger.debug("Fetching conversation context for user=$userId, contact=$contactEmail")

        return try {
            val connection = connectionRepository.findByUserId(userId)
                ?: run {
                    logger.warn("Gmail not connected for user: $userId")
                    return null
                }

            val accessToken = gmailApiServiceImpl.getAccessTokenPublic(connection)

            // Step 1: Find latest thread with this contact
            val threadId = findLatestThreadId(accessToken, contactEmail)
                ?: run {
                    logger.debug("No thread found for contact: $contactEmail")
                    return null
                }

            logger.debug("Found threadId: $threadId for contact: $contactEmail")

            // Step 2: Get all message IDs in thread (metadata format)
            val messageIds = getMessageIdsFromThread(accessToken, threadId)
            if (messageIds.isEmpty()) {
                logger.debug("Thread has no messages: $threadId")
                return null
            }

            // Step 3: Take last K messages
            val lastMessageIds = messageIds.takeLast(MAX_MESSAGES_IN_THREAD)
            logger.debug("Fetching last ${lastMessageIds.size} messages from thread: $threadId")

            // Step 4: Fetch body for each message
            val messages = lastMessageIds.mapNotNull { messageId ->
                fetchMessageForContext(accessToken, messageId)
            }

            if (messages.isEmpty()) {
                logger.debug("No usable messages found in thread: $threadId")
                return null
            }

            // Step 5: Build context transcript
            val context = buildContextTranscript(messages)

            logger.info("Built conversation context for $contactEmail: ${context.length} chars")
            context
        } catch (e: Exception) {
            logger.error("Failed to fetch conversation context for $contactEmail: ${e.message}", e)
            null  // Return null to trigger fallback to profileSummary
        }
    }

    /**
     * Find latest thread ID for a contact
     * Query: (from:CONTACT OR to:CONTACT) newer_than:90d
     * maxResults=1 to get most recent thread only
     */
    private fun findLatestThreadId(accessToken: String, contactEmail: String): String? {
        return try {
            val query = "(from:$contactEmail OR to:$contactEmail) newer_than:90d"

            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.listMessages(accessToken, query, maxResults = 1)
            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()

            messages.firstOrNull()?.get("threadId").also {
                logger.debug("Latest thread ID for $contactEmail: $it")
            }
        } catch (e: Exception) {
            logger.warn("Failed to find thread for $contactEmail: ${e.message}")
            null
        }
    }

    /**
     * Get all message IDs in a thread using metadata format (lightweight)
     */
    private fun getMessageIdsFromThread(accessToken: String, threadId: String): List<String> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.getThread(accessToken, threadId)
            val messages = response["messages"] as? List<Map<String, Any>> ?: emptyList()

            // Sort by timestamp ascending (oldest first)
            messages
                .sortedBy { (it["internalDate"] as? String)?.toLongOrNull() ?: 0L }
                .mapNotNull { it["id"] as? String }
                .also { logger.debug("Found ${it.size} messages in thread: $threadId") }
        } catch (e: Exception) {
            logger.warn("Failed to get thread messages for $threadId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetch message body and metadata for context building
     * Returns message object with role, date, subject, body
     */
    private fun fetchMessageForContext(
        accessToken: String,
        messageId: String
    ): Map<String, String>? {
        return try {
            val message = gmailApiClient.getMessage(accessToken, messageId)

            // Extract headers
            @Suppress("UNCHECKED_CAST")
            val headers = (message["payload"] as? Map<String, Any>)?.get("headers") as? List<Map<String, String>> ?: emptyList()
            val headerMap = headers.associateBy({ it["name"] ?: "" }, { it["value"] ?: "" })

            val from = headerMap["From"] ?: ""
            val date = headerMap["Date"] ?: ""
            val subject = headerMap["Subject"] ?: ""

            // Determine role (USER/CONTACT/OTHER)
            val role = when {
                from.contains("@", ignoreCase = true) -> "CONTACT"
                else -> "USER"  // Simplified: assume if not contact, then user
            }

            // Extract body (text/plain or text/html)
            val body = extractMessageBody(message)
            if (body.isBlank()) {
                logger.debug("Message $messageId has empty body, skipping")
                return null
            }

            // Truncate body to avoid excessive token usage
            val truncatedBody = body.take(MAX_CHARS_PER_MESSAGE)

            mapOf(
                "role" to role,
                "date" to formatDateForContext(date),
                "subject" to subject.take(100),  // Truncate subject
                "body" to truncatedBody
            )
        } catch (e: Exception) {
            logger.warn("Failed to fetch message $messageId for context: ${e.message}")
            null
        }
    }

    /**
     * Extract message body text (supports both text/plain and text/html)
     */
    private fun extractMessageBody(message: Map<String, Any>): String {
        return try {
            @Suppress("UNCHECKED_CAST")
            val payload = message["payload"] as? Map<String, Any> ?: return ""

            // Try to get text/plain part first
            @Suppress("UNCHECKED_CAST")
            val parts = payload["parts"] as? List<Map<String, Any>> ?: emptyList()
            val textPart = parts.firstOrNull { part ->
                (part["mimeType"] as? String)?.contains("text/plain") == true
            }

            if (textPart != null) {
                @Suppress("UNCHECKED_CAST")
                val body = textPart["body"] as? Map<String, Any> ?: return ""
                val data = body["data"] as? String ?: return ""
                decodeBase64(data)
            } else {
                // Fallback: try payload.body directly
                @Suppress("UNCHECKED_CAST")
                val body = payload["body"] as? Map<String, Any> ?: return ""
                val data = body["data"] as? String ?: return ""
                decodeBase64(data)
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract message body: ${e.message}")
            ""
        }
    }

    /**
     * Decode base64 URL-encoded string from Gmail API
     */
    private fun decodeBase64(encoded: String): String {
        return try {
            val decoded = java.util.Base64.getUrlDecoder().decode(encoded)
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.warn("Failed to decode base64: ${e.message}")
            ""
        }
    }

    /**
     * Build conversation transcript from messages
     * Format: CONTACT [date]: subject\nbody\n\nUSER [date]: subject\nbody\n...
     * Truncates to MAX_TOTAL_CHARS, prioritizing recent messages
     */
    private fun buildContextTranscript(messages: List<Map<String, String>>): String {
        val lines = mutableListOf<String>()
        var totalChars = 0

        // Iterate from oldest to newest to maintain chronological order
        for (message in messages) {
            val role = message["role"] ?: "OTHER"
            val msgDate = message["date"] ?: ""
            val subject = message["subject"] ?: ""
            val body = message["body"] ?: ""

            // Format: ROLE [date]: subject\nbody
            val messageText = buildString {
                append("$role [$msgDate]")
                if (subject.isNotBlank()) {
                    append(": $subject")
                }
                append("\n")
                append(body)
                append("\n")
            }

            // Check if adding this message would exceed limit
            if (totalChars + messageText.length > MAX_TOTAL_CHARS) {
                // If we already have content, truncate oldest messages
                if (lines.isNotEmpty()) {
                    // Remove oldest line(s) to make room for newer content
                    while (lines.isNotEmpty() && totalChars + messageText.length > MAX_TOTAL_CHARS) {
                        val removedLine = lines.removeAt(0)
                        totalChars -= removedLine.length
                    }
                }
            }

            lines.add(messageText)
            totalChars += messageText.length
        }

        return lines.joinToString("\n---\n").take(MAX_TOTAL_CHARS)
    }

    /**
     * Format date from RFC 2822 to readable format
     * Input: "Mon, 14 Apr 2026 10:30:00 +0000"
     * Output: "Apr 14 10:30"
     */
    private fun formatDateForContext(dateString: String): String {
        return try {
            // Simplified: just extract date part
            val parts = dateString.split(" ").takeIf { it.size >= 3 } ?: return dateString.take(10)
            "${parts[1]} ${parts[2]} ${parts[3].substring(0, 5)}"  // "14 Apr 10:30"
        } catch (e: Exception) {
            dateString.take(10)  // Fallback to first 10 chars
        }
    }
}


