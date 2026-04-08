package com.openmmo.ai.client

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.Base64

/**
 * Gmail API Client
 * Handles all Gmail API calls
 * HTTP wrapper - no business logic
 */
@Component
class GmailApiClient(
    private val restTemplate: RestTemplate,
    @Value("\${gmail.api.base}") private val gmailApiBase: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailApiClient::class.java)
    }

    /**
     * List messages with query
     */
    fun listMessages(accessToken: String, query: String, maxResults: Int = 20): List<Map<String, String>> {
        logger.debug("Listing messages: query=$query, maxResults=$maxResults")

        try {
            val url = "$gmailApiBase/messages?q=$query&maxResults=$maxResults"
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
            ).body as? Map<String, Any> ?: return emptyList()

            @Suppress("UNCHECKED_CAST")
            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
            logger.debug("Found ${messages.size} messages")

            return messages
        } catch (e: HttpClientErrorException) {
            logger.error("HTTP error listing messages: ${e.statusCode} - ${e.responseBodyAsString}")
            throw e
        }
    }

    /**
     * Get message details
     */
    fun getMessage(accessToken: String, messageId: String): Map<String, Any> {
        logger.debug("Fetching message: $messageId")

        try {
            val url = "$gmailApiBase/messages/$messageId"
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
            ).body as? Map<String, Any> ?: throw IllegalStateException("No response from Gmail API")

            logger.debug("Message fetched: $messageId")
            return response
        } catch (e: Exception) {
            logger.error("Failed to get message: ${e.message}")
            throw e
        }
    }

    /**
     * Send message (reply)
     */
    fun sendMessage(accessToken: String, rawMessage: String) {
        logger.debug("Sending message")

        try {
            val sendUrl = "$gmailApiBase/messages/send"
            val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(rawMessage.toByteArray(Charsets.UTF_8))
            val body = mapOf("raw" to encoded)

            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
            }
            val request = HttpEntity(body, headers)

            restTemplate.exchange(sendUrl, HttpMethod.POST, request, Map::class.java)
            logger.debug("Message sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send message: ${e.message}")
            throw e
        }
    }

    /**
     * Modify message labels
     */
    fun modifyLabels(
        accessToken: String,
        messageId: String,
        addLabels: List<String>,
        removeLabels: List<String>
    ) {
        logger.debug("Modifying labels for message: $messageId")

        try {
            val modifyUrl = "$gmailApiBase/messages/$messageId/modify"
            val body = mapOf(
                "addLabelIds" to addLabels,
                "removeLabelIds" to removeLabels
            )

            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
            }
            val request = HttpEntity(body, headers)

            restTemplate.exchange(modifyUrl, HttpMethod.POST, request, Map::class.java)
            logger.debug("Labels modified successfully")
        } catch (e: Exception) {
            logger.error("Failed to modify labels: ${e.message}")
            throw e
        }
    }

    /**
     * List all labels
     */
    fun listLabels(accessToken: String): List<Map<String, String>> {
        logger.debug("Listing labels")

        try {
            val listUrl = "$gmailApiBase/labels"
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
            }
            val request = HttpEntity<String>(headers)

            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.exchange(
                listUrl,
                HttpMethod.GET,
                request,
                Map::class.java
            ).body as? Map<String, Any> ?: return emptyList()

            @Suppress("UNCHECKED_CAST")
            val labels = response["labels"] as? List<Map<String, String>> ?: emptyList()
            logger.debug("Found ${labels.size} labels")

            return labels
        } catch (e: Exception) {
            logger.error("Failed to list labels: ${e.message}")
            throw e
        }
    }

    /**
     * Create label
     */
    fun createLabel(
        accessToken: String,
        name: String
    ): String {
        logger.debug("Creating label: $name")

        try {
            val createUrl = "$gmailApiBase/labels"
            val createBody = mapOf(
                "name" to name,
                "labelListVisibility" to "labelShow",
                "messageListVisibility" to "show"
            )

            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
            }
            val createRequest = HttpEntity(createBody, headers)

            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.exchange(
                createUrl,
                HttpMethod.POST,
                createRequest,
                Map::class.java
            ).body as? Map<String, Any> ?: throw IllegalStateException("No response from create label")

            val labelId = response["id"] as? String
                ?: throw IllegalStateException("No label ID in response")

            logger.debug("Label created: $name ($labelId)")
            return labelId
        } catch (e: Exception) {
            logger.error("Failed to create label: ${e.message}")
            throw e
        }
    }
}








