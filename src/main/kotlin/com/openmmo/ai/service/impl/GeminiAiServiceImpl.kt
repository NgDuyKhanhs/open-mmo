package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GeminiApiClient
import com.openmmo.ai.service.IGeminiAiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Gemini AI Service Implementation
 * Handles AI-powered text generation for email replies
 */
@Service
class GeminiAiServiceImpl(
    private val geminiApiClient: GeminiApiClient
) : IGeminiAiService {

    companion object {
        private val logger = LoggerFactory.getLogger(GeminiAiServiceImpl::class.java)
    }

    override fun generateReply(emailContent: String, customPrompt: String?): String {
        try {
            logger.info("Generating AI reply for email, custom prompt: ${customPrompt != null}")
            return geminiApiClient.generateReply(emailContent, customPrompt)
        } catch (e: Exception) {
            logger.error("Failed to generate AI reply: ${e.message}", e)
            throw e
        }
    }

    override fun generateText(prompt: String): String {
        try {
            logger.debug("Generating text with Gemini, prompt length: ${prompt.length}")
            return geminiApiClient.generateText(prompt)
        } catch (e: Exception) {
            logger.error("Failed to generate text from Gemini: ${e.message}", e)
            throw e
        }
    }
}

