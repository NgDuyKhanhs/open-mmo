package com.openmmo.ai.service.impl

import com.openmmo.ai.service.IAIReminderService
import com.openmmo.ai.service.IGeminiAiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Qualifier

/**
 * AI Reminder Service Implementation
 * Generates personalized reminder emails using user's selected AI provider
 * Uses Gemini (default) or Groq based on user preference from BotConfig
 * Supports multi-language reminders based on detected conversation language
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
        aiProvider: String,  // Default to Gemini
        language: String?
    ): String {
        return try {
            val aiService = getAiServiceForProvider(aiProvider)
            val prompt = buildReminderPrompt(contactEmail, conversationContext, language)
            logger.debug("Generating reminder message for: $contactEmail using provider: $aiProvider with language: $language")

            val message = aiService.generateText(prompt)
            logger.info("Successfully generated reminder message for: $contactEmail")

            message.trim()
        } catch (e: Exception) {
            logger.error("Failed to generate reminder message for $contactEmail using $aiProvider: ${e.message}", e)
            buildFallbackMessage(conversationContext, language)
        }
    }

    private fun buildReminderPrompt(
        contactEmail: String,
        conversationContext: String?,
        language: String? = null
    ): String {
        val languageInstruction = when (language?.lowercase()) {
            "vi" -> "\nIMPORTANT: Write your response in VIETNAMESE language."
            "ja" -> "\nIMPORTANT: Write your response in JAPANESE language."
            "zh", "zh_cn", "zh_tw" -> "\nIMPORTANT: Write your response in CHINESE language."
            "ko" -> "\nIMPORTANT: Write your response in KOREAN language."
            "fr" -> "\nIMPORTANT: Write your response in FRENCH language."
            "de" -> "\nIMPORTANT: Write your response in GERMAN language."
            "es" -> "\nIMPORTANT: Write your response in SPANISH language."
            "pt" -> "\nIMPORTANT: Write your response in PORTUGUESE language."
            "ru" -> "\nIMPORTANT: Write your response in RUSSIAN language."
            "th" -> "\nIMPORTANT: Write your response in THAI language."
            else -> "" // Default to English, no explicit instruction needed
        }

        return buildString {
            append("You are a concise email assistant. Write a friendly follow-up reminder email body.\n\n")
            append("RULES:\n")
            append("- Output ONLY the email body (no greeting, no closing signature, no subject).\n")
            append("- 2 to 5 sentences. Natural, polite, not salesy.\n")
            append("- Do NOT invent facts. If details are missing, keep it generic.\n")
            append("- Do NOT quote long text. No bullet lists unless absolutely necessary.")
            append(languageInstruction)
            append("\n\n")

            append("TARGET CONTACT: $contactEmail\n\n")

            append("RECENT CONVERSATION CONTEXT (may be truncated):\n")
            if (!conversationContext.isNullOrBlank()) {
                append(conversationContext)
            } else {
                append("(none)")
            }
        }
    }

    private fun buildFallbackMessage(conversationContext: String?, language: String? = null): String {
        return when (language?.lowercase()) {
            "vi" -> if (!conversationContext.isNullOrBlank()) {
                """
Đã lâu rồi kể từ lần cuối chúng ta tương tác. Tôi đã nghĩ về cuộc trò chuyện trước đây của chúng ta và muốn liên hệ để xem mọi thứ đang diễn ra như thế nào.

Có điều gì tôi có thể giúp bạn hoặc có lần theo dõi nào bạn muốn thảo luận không?
                """.trimIndent()
            } else {
                """
Đã lâu rồi, và tôi muốn liên hệ để kiểm tra với bạn. Tôi hy vọng mọi thứ đang diễn ra tốt đẹp.

Có điều gì tôi có thể giúp bạn không?
                """.trimIndent()
            }
            "ja" -> if (!conversationContext.isNullOrBlank()) {
                """
最後にご連絡してからしばらく経ちました。以前のご相談について思い出し、いかがお過ごしかお知らせいただきたくご連絡さしあげました。

ご質問がございましたら、または今後のご相談がございましたら、お知らせください。
                """.trimIndent()
            } else {
                """
お久しぶりです。ご連絡させていただきたく思います。いかがお過ごしでしょうか。

何かお力になれることはありますか？
                """.trimIndent()
            }
            else -> if (!conversationContext.isNullOrBlank()) {
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
    }

    /**
     * Get the appropriate AI service based on provider preference
     * @param provider "gemini" (default) or "groq"
     * @return IGeminiAiService instance (either Gemini or Groq impl)
     */
    private fun getAiServiceForProvider(provider: String): IGeminiAiService {
        return when (provider.lowercase()) {
            "groq" -> {
                logger.debug("Using Groq AI service for reminder generation")
                groqAiService
            }
            else -> {
                logger.debug("Using Gemini AI service for reminder generation (default)")
                geminiAiService
            }
        }
    }
}

