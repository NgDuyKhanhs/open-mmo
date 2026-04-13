package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "gmail_bot_configs")
data class GmailBotConfig(
    @Id
    val id: String? = null,
    val userId: String,
    val enabled: Boolean = false,
    val triggerSubject: String = "openmmo",
    val processedLabel: String = "OpenMMO_AutoReplied",
    val customPrompt: String = "",
    val aiProvider: String = "groq", // "groq" or "gemini" (default: groq for speed/cost)
    val lastRunAt: LocalDateTime? = null,
    val lastError: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

