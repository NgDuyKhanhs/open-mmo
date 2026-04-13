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
 * Groq API Client
 * Handles all Groq API calls for fast AI-powered text generation
 * Uses OpenAI-compatible API endpoint
 * HTTP wrapper - no business logic
 */
@Component
class GroqApiClient(
    private val restTemplate: RestTemplate,
    @Value("\${groq.api-key}") private val groqApiKey: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GroqApiClient::class.java)
        private const val GROQ_API_BASE = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile"  // Fast & reliable model
    }

    private val objectMapper = ObjectMapper()

    /**
     * Generate text using Groq API with retry logic
     * @param prompt The prompt to send to Groq
     * @return Generated text response
     */
    fun generateText(prompt: String): String {
        logger.debug("Calling Groq API with prompt length: ${prompt.length}")

        var lastException: Exception? = null
        val maxRetries = 3

        repeat(maxRetries) { attempt ->
            try {
                val requestBody = mapOf(
                    "model" to MODEL,
                    "messages" to listOf(
                        mapOf(
                            "role" to "user",
                            "content" to prompt
                        )
                    ),
                    "temperature" to 0.7,
                    "max_tokens" to 1024
                )

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("Authorization", "Bearer $groqApiKey")
                }

                val request = HttpEntity(requestBody, headers)

                @Suppress("UNCHECKED_CAST")
                val response = restTemplate.exchange(
                    GROQ_API_BASE,
                    HttpMethod.POST,
                    request,
                    Map::class.java
                ).body as? Map<String, Any> ?: throw IllegalStateException("No response from Groq API")

                logger.debug("Groq API response received")

                // Extract text from response (OpenAI format)
                @Suppress("UNCHECKED_CAST")
                val choices = response["choices"] as? List<Map<String, Any>>
                    ?: throw IllegalStateException("No choices in Groq response")

                if (choices.isEmpty()) {
                    throw IllegalStateException("Groq API returned no choices")
                }

                @Suppress("UNCHECKED_CAST")
                val message = choices[0]["message"] as? Map<String, Any>
                    ?: throw IllegalStateException("No message in choice")

                val text = message["content"] as? String
                    ?: throw IllegalStateException("No content in message")

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
                    logger.warn("⚠️ Groq API returned 5xx error (attempt ${attempt + 1}/$maxRetries). Retrying in ${delayMs}ms...")
                    Thread.sleep(delayMs)
                } else {
                    logger.error("Failed to generate text from Groq (attempt ${attempt + 1}/$maxRetries): ${e.message}", e)
                }
            }
        }

        throw lastException ?: Exception("Failed to generate text from Groq API after $maxRetries attempts")
    }

    /**
     * Generate reply text for Gmail using Groq
     * Convenience method that wraps generateText for email replies
     * @param emailContent The email content to reply to
     * @param customPrompt Optional custom prompt to use instead of default
     * @return Generated reply text
     */
    fun generateReply(emailContent: String, customPrompt: String? = null): String {
        logger.debug("Generating email reply with Groq, custom prompt: ${customPrompt != null}")

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

