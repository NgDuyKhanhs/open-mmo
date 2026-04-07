package com.openmmo.ai.service

import com.openmmo.ai.repository.GmailBotConfigRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class GeminiService(
    private val restTemplate: RestTemplate,
    private val botConfigRepository: GmailBotConfigRepository,
    @Value("\${gemini.api-key:}") private val geminiApiKey: String
) {

    companion object {
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

        // Default prompt (not const because multiline strings can't be const)
        private val DEFAULT_PROMPT = """
            You are a helpful customer service AI assistant. Generate a professional, concise email reply based on the following:
            
            Subject: {subject}
            From: {senderEmail}
            Original Message: {body}
            
            Guidelines:
            - Keep response concise (2-3 sentences or bullet points if needed)
            - Be helpful and professional
            - Don't make up information; ask clarifying questions if needed (max 1-3 questions)
            - Refuse harmful or illegal requests politely
            - Sign off professionally
            
            Reply:
        """.trimIndent()
    }

    fun generateReply(userId: String, emailSubject: String, emailBody: String, senderEmail: String, triggerSubject: String = ""): String {
        if (geminiApiKey.isBlank()) {
            return "Thank you for your email about '$emailSubject'. We've received your message and will get back to you soon."
        }

        // Load custom prompt from user's config, or use default
        val config = botConfigRepository.findByUserId(userId)
        val customPromptTemplate = if (!config?.customPrompt.isNullOrBlank()) {
            config.customPrompt
        } else {
            DEFAULT_PROMPT
        }

        val prompt = customPromptTemplate
            .replace("{subject}", emailSubject)
            .replace("{senderEmail}", senderEmail)
            .replace("{body}", emailBody)
            .replace("{triggerSubject}", triggerSubject)

        return try {
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                )
            )

            val url = "$GEMINI_API_URL?key=$geminiApiKey"
            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.postForObject(url, requestBody, Map::class.java) as Map<*, *>

            @Suppress("UNCHECKED_CAST")
            val candidates = response?.get("candidates") as? List<Map<String, Any>>
            if (!candidates.isNullOrEmpty()) {
                val content = candidates[0]["content"] as? Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val parts = content?.get("parts") as? List<Map<String, String>>
                if (!parts.isNullOrEmpty()) {
                    return parts[0]["text"] ?: "Reply generation failed"
                }
            }

            "Thank you for contacting us. We appreciate your interest and will respond shortly."
        } catch (_: Exception) {
            "Thank you for your email. We've received your message."
        }
    }
}

