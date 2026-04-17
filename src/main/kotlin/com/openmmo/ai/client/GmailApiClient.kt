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
     * List messages with query and pagination support
     */
    fun listMessages(accessToken: String, query: String, maxResults: Int = 20, pageToken: String? = null): Map<String, Any> {
        logger.debug("Listing messages: query=$query, maxResults=$maxResults, pageToken=$pageToken")

        try {
            var url = "$gmailApiBase/messages?q=$query&maxResults=$maxResults"
            if (pageToken != null) {
                url += "&pageToken=$pageToken"
            }

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
            ).body as? Map<String, Any> ?: return mapOf("messages" to emptyList<Any>())

            logger.debug("Found ${(response["messages"] as? List<*>)?.size ?: 0} messages")

            return response
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
      * Get message headers only (optimized - metadata format)
      * Fetches only headers without full message body
      * ~95% smaller payload than full message
      * Useful for: extracting subject, from, to, date, reply-to, etc.
      */
     fun getMessageHeaders(accessToken: String, messageId: String): Map<String, Any> {
         logger.debug("Fetching message headers (metadata format): $messageId")

         try {
             // format=metadata with full list of common headers - much lighter than full message
             val metadataHeaders = listOf(
                 "From", "To", "Subject", "Date", "Reply-To", "Cc", "Bcc",
                 "Content-Type", "Message-ID", "In-Reply-To", "References"
             ).joinToString("&metadataHeaders=", prefix = "&metadataHeaders=")
             
             val url = "$gmailApiBase/messages/$messageId?format=metadata$metadataHeaders"
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

             logger.debug("Message headers fetched (metadata format): $messageId")
             return response
         } catch (e: Exception) {
             logger.error("Failed to get message headers: ${e.message}")
             throw e
         }
     }

     /**
      * Get message metadata only (minimal fields for listing)
      * Fetches: From, Subject, Date, Snippet, Labels
      * ~80% smaller payload than full message
      */
     fun getMessageMetadata(accessToken: String, messageId: String): Map<String, Any> {
         logger.debug("Fetching message metadata: $messageId")

         try {
             // format=metadata only returns headers specified in metadataHeaders
             val url = "$gmailApiBase/messages/$messageId?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date"
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

             logger.debug("Message metadata fetched: $messageId")
             return response
         } catch (e: Exception) {
             logger.error("Failed to get message metadata: ${e.message}")
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

    /**
     * Get thread details (metadata format - lightweight)
     * Includes message IDs, timestamps, and headers
     */
    fun getThread(accessToken: String, threadId: String): Map<String, Any> {
        logger.debug("Fetching thread: $threadId (metadata format)")

        try {
            // format=metadata returns only headers (lightweight)
            val url = "$gmailApiBase/threads/$threadId?format=metadata&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date"
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

            logger.debug("Thread fetched: $threadId")
            return response
        } catch (e: Exception) {
            logger.error("Failed to get thread $threadId: ${e.message}")
            throw e
        }
    }
}








