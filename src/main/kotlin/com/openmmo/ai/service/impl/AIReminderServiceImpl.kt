package com.openmmo.ai.service.impl

import com.openmmo.ai.service.IAIReminderService
import com.openmmo.ai.service.IGeminiAiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Qualifier

/**
 * AI Reminder Service Implementation
 * Generates personalized reminder emails using user's selected AI provider
 * Uses Groq (default) or Gemini based on user preference from BotConfig
 */
@Service
class AIReminderServiceImpl(
    @Qualifier("groqAiServiceImpl")
    private val groqAiService: IGeminiAiService,
    @Qualifier("geminiAiServiceImpl")
    private val geminiAiService: IGeminiAiService
) : IAIReminderService {

    companion object {
        private val logger = LoggerFactory.getLogger(AIReminderServiceImpl::class.java)
    }

    override fun generateReminderMessage(
        contactEmail: String,
        conversationContext: String?,
        aiProvider: String  // Default to Groq
    ): String {
        return try {
            val aiService = getAiServiceForProvider(aiProvider)
            val prompt = buildReminderPrompt(contactEmail, conversationContext)
            logger.debug("Generating reminder message for: $contactEmail using provider: $aiProvider")

            val message = aiService.generateText(prompt)
            logger.info("Successfully generated reminder message for: $contactEmail")

            message.trim()
        } catch (e: Exception) {
            logger.error("Failed to generate reminder message for $contactEmail using $aiProvider: ${e.message}", e)
            buildFallbackMessage(conversationContext)
        }
    }

    private fun buildReminderPrompt(
        contactEmail: String,
        conversationContext: String?
    ): String {
        return buildString {
            append("You are a concise email assistant. Write a friendly follow-up reminder email body.\n\n")
            append("RULES:\n")
            append("- Output ONLY the email body (no greeting, no closing signature, no subject).\n")
            append("- 2 to 5 sentences. Natural, polite, not salesy.\n")
            append("- Do NOT invent facts. If details are missing, keep it generic.\n")
            append("- Do NOT quote long text. No bullet lists unless absolutely necessary.\n\n")

            append("TARGET CONTACT: $contactEmail\n\n")

            append("RECENT CONVERSATION CONTEXT (may be truncated):\n")
            if (!conversationContext.isNullOrBlank()) {
                append(conversationContext)
            } else {
                append("(none)")
            }
        }
    }

    private fun buildFallbackMessage(conversationContext: String?): String {
        return if (!conversationContext.isNullOrBlank()) {
            """
It's been a while since we last connected. I was thinking about our previous conversation and wanted to reach out to see how things are going.

Is there anything I can help you with or any follow-up you'd like to discuss?
            """.trimIndent()
        } else {
            """
It's been a while, and I wanted to reach out to check in with you. I hope everything is going well.

Is there anything I can help you with?
            """.trimIndent()
        }
    }

    /**
     * Get the appropriate AI service based on provider preference
     * @param provider "groq" or "gemini"
     * @return IGeminiAiService instance (either Groq or Gemini impl)
     */
    private fun getAiServiceForProvider(provider: String): IGeminiAiService {
        return when (provider.lowercase()) {
            "gemini" -> {
                logger.debug("Using Gemini AI service for reminder generation")
                geminiAiService
            }
            else -> {
                logger.debug("Using Groq AI service for reminder generation (default)")
                groqAiService
            }
        }
    }
}

