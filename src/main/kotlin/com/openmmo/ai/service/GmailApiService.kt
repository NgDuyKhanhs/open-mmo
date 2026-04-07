package com.openmmo.ai.service

import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.util.EncryptionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
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

@Service
class GmailApiService(
    private val connectionRepository: GmailConnectionRepository,
    private val oauthService: GmailOAuthService,
    private val restTemplate: RestTemplate,
    @Value("\${token.enc.key-base64}") private val tokenEncKey: String
) {

    companion object {
        private const val GMAIL_API_BASE = "https://www.googleapis.com/gmail/v1/users/me"
    }

    fun getMailbox(userId: String, boxType: String, maxResults: Int = 20): List<MailboxItem> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        val query = when (boxType) {
            "inbox" -> "in:inbox"
            "sent" -> "in:sent"
            "spam" -> "in:spam"
            "trash" -> "in:trash"
            else -> throw IllegalArgumentException("Invalid box type")
        }

        val url = "$GMAIL_API_BASE/messages?q=$query&maxResults=$maxResults"

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity<String>(headers)

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            Map::class.java
        ).body

        @Suppress("UNCHECKED_CAST")
        val messages = (response?.get("messages") as? List<Map<String, String>>) ?: emptyList()

        return messages.mapNotNull { msg ->
            val msgId = msg["id"] ?: return@mapNotNull null
            val threadId = msg["threadId"] ?: return@mapNotNull null

            try {
                val full = getMessageDetails(accessToken, msgId)
                full.copy(threadId = threadId)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun searchMessages(userId: String, query: String, maxResults: Int = 10): List<String> {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        val url = "$GMAIL_API_BASE/messages?q=$query&maxResults=$maxResults"

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity<String>(headers)

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            Map::class.java
        ).body

        @Suppress("UNCHECKED_CAST")
        val messages = (response?.get("messages") as? List<Map<String, String>>) ?: emptyList()
        return messages.mapNotNull { it["id"] }
    }

    fun getMessageDetails(accessToken: String, messageId: String): MailboxItem {
        val url = "$GMAIL_API_BASE/messages/$messageId"

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity<String>(headers)

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.exchange(url, HttpMethod.GET, request, Map::class.java).body
        val payload = response?.get("payload") as? Map<String, Any>
        val headersList = payload?.get("headers") as? List<Map<String, String>>

        val from = headersList?.find { it["name"] == "From" }?.get("value") ?: "Unknown"
        val subject = headersList?.find { it["name"] == "Subject" }?.get("value") ?: "(No subject)"
        val date = headersList?.find { it["name"] == "Date" }?.get("value") ?: ""
        val snippet = response?.get("snippet") as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val labels = response?.get("labelIds") as? List<String> ?: emptyList()

        return MailboxItem(
            id = messageId,
            threadId = response?.get("threadId") as? String ?: "",
            from = from,
            subject = subject,
            date = date,
            snippet = snippet,
            labels = labels
        )
    }

    fun getMessageBody(accessToken: String, messageId: String): String {
        val url = "$GMAIL_API_BASE/messages/$messageId"

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity<String>(headers)

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.exchange(url, HttpMethod.GET, request, Map::class.java).body
        val payload = response?.get("payload") as? Map<String, Any>

        return extractBodyText(payload)
    }

    fun sendReply(userId: String, threadId: String, toEmail: String, subject: String, bodyText: String) {
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

        // Encode entire message as UTF-8
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(rawMessage.toByteArray(Charsets.UTF_8))

        val sendUrl = "$GMAIL_API_BASE/messages/send"
        val body = mapOf("raw" to encoded)

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity(body, headers)

        try {
            restTemplate.exchange(sendUrl, HttpMethod.POST, request, Map::class.java)
        } catch (e: Exception) {
            // Log error but continue
            throw IllegalStateException("Failed to send reply: ${e.message}")
        }
    }

    fun modifyLabels(userId: String, messageId: String, addLabels: List<String>, removeLabels: List<String>) {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        val modifyUrl = "$GMAIL_API_BASE/messages/$messageId/modify"
        val body = mapOf(
            "addLabelIds" to addLabels,
            "removeLabelIds" to removeLabels
        )

        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity(body, headers)

        restTemplate.exchange(modifyUrl, HttpMethod.POST, request, Map::class.java)
    }

    fun ensureLabel(userId: String, labelName: String): String {
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val accessToken = getAccessToken(connection)

        // Try to find existing label
        val listUrl = "$GMAIL_API_BASE/labels"
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity<String>(headers)

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.exchange(listUrl, HttpMethod.GET, request, Map::class.java).body
        val labels = response?.get("labels") as? List<Map<String, String>> ?: emptyList()

        val existingLabel = labels.find { it["name"] == labelName }
        if (existingLabel != null) {
            return existingLabel["id"] ?: throw IllegalStateException("No label ID")
        }

        // Create new label
        val createUrl = "$GMAIL_API_BASE/labels"
        val createBody = mapOf(
            "name" to labelName,
            "labelListVisibility" to "labelShow",
            "messageListVisibility" to "show"
        )
        val createRequest = HttpEntity(createBody, headers)

        @Suppress("UNCHECKED_CAST")
        val createResponse = restTemplate.exchange(createUrl, HttpMethod.POST, createRequest, Map::class.java).body
        return createResponse?.get("id") as? String ?: throw IllegalStateException("Failed to create label")
    }

    private fun getAccessToken(connection: com.openmmo.ai.entity.GmailConnection): String {
        val decryptedRefreshToken = EncryptionUtil.decrypt(connection.refreshTokenEnc, tokenEncKey)
        return oauthService.refreshAccessToken(decryptedRefreshToken)
    }

    private fun extractBodyText(payload: Map<String, Any>?): String {
        if (payload == null) return ""

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
            val partBody = part["body"] as? Map<String, Any>
            val partData = partBody?.get("data") as? String
            if (!partData.isNullOrEmpty()) {
                return String(Base64.getUrlDecoder().decode(partData))
            }
        }

        return ""
    }
}

