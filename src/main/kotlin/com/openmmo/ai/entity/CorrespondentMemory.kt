package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Per-correspondent memory for Gmail auto-reply
 * Stores conversation context, facts, and style preferences per user + correspondent
 */
@Document(collection = "correspondent_memory")
@CompoundIndexes(
    CompoundIndex(name = "user_email_idx", def = "{'userId': 1, 'correspondentEmail': 1}", unique = true)
)
data class CorrespondentMemory(
    @Id val id: String? = null,
    val userId: String,
    val correspondentEmail: String,  // lowercase, trimmed
    val profileSummary: String = "",
    val facts: List<MemoryFact> = emptyList(),
    val stylePrefs: StylePrefs = StylePrefs(),
    val preferredLanguage: String? = null,  // "vi", "en", etc. (detected from contact messages)
    val languageConfidence: Double? = null,  // 0.0 - 1.0
    val lastSeenAt: LocalDateTime? = null,
    val lastThreadId: String? = null,
    val version: Int = 1,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class MemoryFact(
    val key: String,  // lowercase snake_case
    val value: String,
    val confidence: Double = 0.6,  // 0.0 - 1.0
    val sourceMessageId: String? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class StylePrefs(
    val language: String? = null,  // "vi", "en" (deprecated: use preferredLanguage)
    val tone: String? = null,      // "friendly", "formal", "professional"
    val formattingNotes: String? = null
)

/**
 * DTO for memory update response from Gemini
 */
data class MemoryUpdateResponse(
    val summary_patch: String = "",
    val facts_add: List<MemoryFact> = emptyList(),
    val facts_remove_keys: List<String> = emptyList(),
    val style_prefs: StylePrefs = StylePrefs(),
    val sensitive: Boolean = false
)

