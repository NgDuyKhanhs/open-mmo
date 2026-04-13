package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GroqApiClient
import com.openmmo.ai.service.IGeminiAiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Groq AI Service Implementation
 * Fast AI-powered text generation using Groq API
 * Used for auto-reply generation (faster & cheaper than Gemini)
 */
@Service
class GroqAiServiceImpl(
    private val groqApiClient: GroqApiClient
) : IGeminiAiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GroqAiServiceImpl::class.java)
    }

    override fun generateReply(emailContent: String, customPrompt: String?): String {
        try {
            logger.info("Generating AI reply via Groq API, custom prompt: ${customPrompt != null}")
            return groqApiClient.generateReply(emailContent, customPrompt)
        } catch (e: Exception) {
            logger.error("Failed to generate reply via Groq: ${e.message}", e)
            throw e
        }
    }

    override fun generateText(prompt: String): String {
        try {
            logger.info("Generating text via Groq API, prompt length: ${prompt.length}")
            return groqApiClient.generateText(prompt)
        } catch (e: Exception) {
            logger.error("Failed to generate text via Groq: ${e.message}", e)
            throw e
        }
    }
}

