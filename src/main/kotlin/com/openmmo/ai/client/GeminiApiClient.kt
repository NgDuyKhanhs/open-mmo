package com.openmmo.ai.client

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Gemini API Client
 * Handles all Google Gemini API calls for AI-powered text generation
 * HTTP wrapper - no business logic
 */
@Component
class GeminiApiClient(
    private val restTemplate: RestTemplate,
    @Value("\${gemini.api-key}") private val geminiApiKey: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GeminiApiClient::class.java)
        private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-2.5-flash"
    }

    private val objectMapper = ObjectMapper()

    /**
     * Generate text using Gemini API with retry logic
     * @param prompt The prompt to send to Gemini
     * @return Generated text response
     */
    fun generateText(prompt: String): String {
        logger.debug("Calling Gemini API with prompt length: ${prompt.length}")

        var lastException: Exception? = null
        val maxRetries = 3

        repeat(maxRetries) { attempt ->
            try {
                val url = "$GEMINI_API_BASE/$MODEL:generateContent?key=$geminiApiKey"

                val requestBody = mapOf(
                    "contents" to listOf(
                        mapOf(
                            "parts" to listOf(
                                mapOf("text" to prompt)
                            )
                        )
                    )
                )

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }

                val request = HttpEntity(requestBody, headers)

                @Suppress("UNCHECKED_CAST")
                val response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map::class.java
                ).body as? Map<String, Any> ?: throw IllegalStateException("No response from Gemini API")

                logger.debug("Gemini API response received")

                // Extract text from response
                @Suppress("UNCHECKED_CAST")
                val candidates = response["candidates"] as? List<Map<String, Any>>
                    ?: throw IllegalStateException("No candidates in Gemini response")

                if (candidates.isEmpty()) {
                    throw IllegalStateException("Gemini API returned no candidates")
                }

                @Suppress("UNCHECKED_CAST")
                val content = candidates[0]["content"] as? Map<String, Any>
                    ?: throw IllegalStateException("No content in candidate")

                @Suppress("UNCHECKED_CAST")
                val parts = content["parts"] as? List<Map<String, Any>>
                    ?: throw IllegalStateException("No parts in content")

                if (parts.isEmpty()) {
                    throw IllegalStateException("No parts in response")
                }

                val text = parts[0]["text"] as? String
                    ?: throw IllegalStateException("No text in part")

                logger.debug("Generated text length: ${text.length}")
                return text

            } catch (e: Exception) {
                lastException = e

                // Check if it's a 5xx error (temporary)
                val is5xxError = e.toString().contains("503") ||
                                e.toString().contains("502") ||
                                e.toString().contains("500")

                if (is5xxError && attempt < maxRetries - 1) {
                    val delayMs = (1000 * Math.pow(2.0, attempt.toDouble())).toLong()
                    logger.warn("⚠️ Gemini API returned 5xx error (attempt ${attempt + 1}/$maxRetries). Retrying in ${delayMs}ms...")
                    Thread.sleep(delayMs)
                } else {
                    logger.error("Failed to generate text from Gemini (attempt ${attempt + 1}/$maxRetries): ${e.message}", e)
                }
            }
        }

        throw lastException ?: Exception("Failed to generate text from Gemini API after $maxRetries attempts")
    }

    /**
     * Generate reply text for Gmail
     * Convenience method that wraps generateText for email replies
     * @param emailContent The email content to reply to
     * @param customPrompt Optional custom prompt to use instead of default
     * @return Generated reply text
     */
    fun generateReply(emailContent: String, customPrompt: String? = null): String {
        // NOTE: This method now uses unified buildPrompt from GmailApiServiceImpl
        // Kept for backward compatibility but prefer buildPrompt() approach
        logger.debug("Generating email reply with custom prompt: ${customPrompt != null}")

        val prompt = if (customPrompt.isNullOrBlank()) {
            """
                Generate a professional and helpful reply to the following email.
                    Keep it concise and friendly. Do not include greeting or signature.
                    
                    Email content:
                    $emailContent
            """.trimIndent()
        } else {
            """
                $customPrompt
                
                Email content to reply to:
                $emailContent
            """.trimIndent()
        }

        return generateText(prompt)
    }
}

