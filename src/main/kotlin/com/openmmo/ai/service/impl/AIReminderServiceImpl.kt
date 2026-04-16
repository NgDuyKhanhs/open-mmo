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
        profileSummary: String,
        aiProvider: String  // Default to Groq
    ): String {
        return try {
            val aiService = getAiServiceForProvider(aiProvider)
            val prompt = buildReminderPrompt(contactEmail, profileSummary)
            logger.debug("Generating reminder message for: $contactEmail using provider: $aiProvider")

            val message = aiService.generateText(prompt)
            logger.info("Successfully generated reminder message for: $contactEmail")

            message.trim()
        } catch (e: Exception) {
            logger.error("Failed to generate reminder message for $contactEmail using $aiProvider: ${e.message}", e)
            buildFallbackMessage(profileSummary)
        }
    }

    private fun buildReminderPrompt(
        contactEmail: String,
        profileSummary: String
    ): String {
        return if (profileSummary.isNotBlank()) {
            """
You are a professional but friendly email assistant. Generate a personalized follow-up email.

Contact: $contactEmail
Previous conversation summary: $profileSummary

Guidelines:
- Reference the previous conversation naturally
- Show genuine interest in following up
- Offer specific help based on the conversation context
- Keep tone warm and professional
- Keep it concise (2-3 sentences)
- Write as a natural email, not a template
- Do NOT include greeting/salutation (just the body)
- Do NOT include closing signature (just the body)

Generate ONLY the email body:
            """.trimIndent()
        } else {
            """
You are a professional email assistant. Generate a warm follow-up email for someone we haven't heard from in a while.

Contact: $contactEmail

Guidelines:
- Keep it warm and professional
- Show genuine interest in reconnecting
- Offer help without being pushy
- Keep it concise (2-3 sentences)
- Do NOT include greeting/salutation (just the body)
- Do NOT include closing signature (just the body)

Generate ONLY the email body:
            """.trimIndent()
        }
    }

    private fun buildFallbackMessage(profileSummary: String): String {
        return if (profileSummary.isNotBlank()) {
            """
It's been a while since we last connected. I was thinking about our previous conversation regarding $profileSummary, and I wanted to reach out to see how things are going.

Is there anything I can help you with or any follow-up you'd like to discuss?

Looking forward to hearing from you!
            """.trimIndent()
        } else {
            """
It's been a while, and I wanted to reach out to check in with you. I hope everything is going well.

Is there anything I can help you with?

Looking forward to connecting again!
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

