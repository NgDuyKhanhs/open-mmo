package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GmailApiClient
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.util.EncryptionUtil
import com.openmmo.ai.dto.MailboxItemResponse
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IGmailOAuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64

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
    private val oauthService: IGmailOAuthService,
    private val gmailApiClient: GmailApiClient,
    @Value("\${token.enc.key-base64}") private val tokenEncKey: String
) : IGmailApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailApiServiceImpl::class.java)
    }

    override fun getMailbox(userId: String, boxType: String, maxResults: Int): List<MailboxItemResponse> {
        try {
            logger.info("Getting mailbox for user: $userId, box=$boxType")

            val connection = connectionRepository.findByUserId(userId)
                ?: throw IllegalStateException("Gmail not connected")

            logger.debug("Gmail connection found, getting access token")
            val accessToken = getAccessToken(connection)

            val query = when (boxType) {
                "inbox" -> "in:inbox"
                "sent" -> "in:sent"
                "spam" -> "in:spam"
                "trash" -> "in:trash"
                else -> throw IllegalArgumentException("Invalid box type: $boxType")
            }

            val messages = gmailApiClient.listMessages(accessToken, query, maxResults)
            logger.info("Found ${messages.size} messages in mailbox")

            return messages.mapNotNull { msg ->
                val msgId = msg["id"] ?: return@mapNotNull null
                val threadId = msg["threadId"] ?: return@mapNotNull null

                try {
                    val full = getMessageDetails(accessToken, msgId)
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
            }
        } catch (e: Exception) {
            logger.error("Error getting mailbox: ${e.message}")
            throw e
        }
    }

    override fun searchMessages(userId: String, query: String, maxResults: Int): List<String> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        val messages = gmailApiClient.listMessages(accessToken, query, maxResults)
        return messages.mapNotNull { it["id"] }
    }

    override fun getMessageBody(userId: String, messageId: String): String {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)
        val response = gmailApiClient.getMessage(accessToken, messageId)
        return extractBodyText(response)
    }

    override fun sendReply(userId: String, threadId: String, toEmail: String, subject: String, bodyText: String) {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        // Create a properly encoded MIME message with UTF-8 charset
        val rawMessage = """
From: ${connection.gmailAddress}
To: $toEmail
Subject: =?UTF-8?B?${Base64.getEncoder().encodeToString("Re: $subject".toByteArray(Charsets.UTF_8))}?=
MIME-Version: 1.0
Content-Type: text/plain; charset="UTF-8"
Content-Transfer-Encoding: base64
In-Reply-To: <$threadId@gmail.com>
References: <$threadId@gmail.com>

$bodyText
        """.trimIndent()

        gmailApiClient.sendMessage(accessToken, rawMessage)
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
}

