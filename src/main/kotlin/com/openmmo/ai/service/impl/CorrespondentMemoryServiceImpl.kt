package com.openmmo.ai.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.openmmo.ai.entity.CorrespondentMemory
import com.openmmo.ai.entity.MemoryFact
import com.openmmo.ai.entity.MemoryUpdateResponse
import com.openmmo.ai.entity.StylePrefs
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.service.ICorrespondentMemoryService
import com.openmmo.ai.client.GeminiApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Correspondent Memory Service Implementation
 * Manages per-correspondent conversation context, facts, and style preferences
 */
@Service
class CorrespondentMemoryServiceImpl(
    private val memoryRepository: CorrespondentMemoryRepository,
    private val geminiApiClient: GeminiApiClient
) : ICorrespondentMemoryService {

    companion object {
        private val logger = LoggerFactory.getLogger(CorrespondentMemoryServiceImpl::class.java)
        private val objectMapper = ObjectMapper().registerKotlinModule()
        private const val MAX_SUMMARY_LENGTH = 3000
        private const val MAX_FACTS_COUNT = 30
        private const val MAX_MEMORY_CONTEXT_LENGTH = 2000

        // Sensitive pattern detection
        private val SENSITIVE_PATTERNS = listOf(
            Regex("""OTP|verification.*code|confirm.*code""", RegexOption.IGNORE_CASE),
            Regex("""\b\d{4,8}\b"""),  // 4-8 digit codes
            Regex("""password|mật khẩu|passwd""", RegexOption.IGNORE_CASE),
            Regex("""token|api.*key|secret|credential""", RegexOption.IGNORE_CASE),
            Regex("""credit.*card|cvv|card.*number""", RegexOption.IGNORE_CASE),
            Regex("""ssn|cccd|cmnd|định danh|identify""", RegexOption.IGNORE_CASE)
        )
    }

    override fun getOrCreate(userId: String, correspondentEmail: String): CorrespondentMemory {
        val normalizedEmail = correspondentEmail.trim().lowercase()

        val existing = memoryRepository.findByUserIdAndCorrespondentEmail(userId, normalizedEmail)
        if (existing != null) {
            return existing
        }

        logger.info("Creating new memory for user=$userId, correspondent=$normalizedEmail")
        val memory = CorrespondentMemory(
            userId = userId,
            correspondentEmail = normalizedEmail,
            lastSeenAt = LocalDateTime.now()
        )
        return memoryRepository.save(memory)
    }

    override fun buildMemoryContextText(memory: CorrespondentMemory): String {
        if (memory.profileSummary.isEmpty() && memory.facts.isEmpty() && memory.stylePrefs.language == null) {
            return ""  // No memory yet
        }

        val sb = StringBuilder()
        sb.append("=== CORRESPONDENT MEMORY ===\n")

        if (memory.profileSummary.isNotEmpty()) {
            sb.append("PROFILE_SUMMARY:\n${memory.profileSummary}\n\n")
        }

        if (memory.facts.isNotEmpty()) {
            sb.append("FACTS:\n")
            memory.facts.forEach { fact ->
                val confStr = String.format("%.1f", fact.confidence * 100)
                sb.append("- ${fact.key}: ${fact.value} (confidence=$confStr%)\n")
            }
            sb.append("\n")
        }

        if (memory.stylePrefs.language != null || memory.stylePrefs.tone != null) {
            sb.append("STYLE_PREFERENCES:\n")
            if (memory.stylePrefs.language != null) {
                sb.append("- Language: ${memory.stylePrefs.language}\n")
            }
            if (memory.stylePrefs.tone != null) {
                sb.append("- Tone: ${memory.stylePrefs.tone}\n")
            }
            if (memory.stylePrefs.formattingNotes != null) {
                sb.append("- Notes: ${memory.stylePrefs.formattingNotes}\n")
            }
        }

        val contextText = sb.toString()

        // Truncate if too long
        return if (contextText.length > MAX_MEMORY_CONTEXT_LENGTH) {
            contextText.substring(0, MAX_MEMORY_CONTEXT_LENGTH) + "\n... (truncated)"
        } else {
            contextText
        }
    }

    override fun updateAfterReply(
        userId: String,
        correspondentEmail: String,
        threadId: String,
        messageId: String,
        emailSubject: String,
        emailBody: String,
        replyBody: String
    ): CorrespondentMemory {
        try {
            logger.info("Updating memory for user=$userId, correspondent=$correspondentEmail")

            var memory = getOrCreate(userId, correspondentEmail)

            // Check if sensitive data present
            val isSensitive = detectSensitiveData(emailBody + "\n" + replyBody)

            // Call Gemini to extract update
            val updateResponse = extractMemoryUpdate(
                emailSubject,
                emailBody,
                replyBody,
                memory.profileSummary,
                memory.facts,
                isSensitive
            )

            // Merge update into memory
            memory = mergeMemoryUpdate(memory, updateResponse, messageId, threadId)

            memory = memory.copy(
                lastSeenAt = LocalDateTime.now(),
                lastThreadId = threadId,
                updatedAt = LocalDateTime.now(),
                version = memory.version + 1
            )

            return memoryRepository.save(memory)
        } catch (e: Exception) {
            logger.error("Error updating memory for user=$userId, correspondent=$correspondentEmail: ${e.message}", e)
            // Don't throw - memory update is auxiliary feature
            return getOrCreate(userId, correspondentEmail)
        }
    }

    override fun forgetCorrespondent(userId: String, correspondentEmail: String) {
        val normalizedEmail = correspondentEmail.trim().lowercase()
        val memory = memoryRepository.findByUserIdAndCorrespondentEmail(userId, normalizedEmail)
        if (memory != null) {
            memoryRepository.deleteById(memory.id!!)
            logger.info("Deleted memory for user=$userId, correspondent=$normalizedEmail")
        }
    }

    override fun forgetAll(userId: String) {
        val memories = memoryRepository.findByUserId(userId)
        memoryRepository.deleteAll(memories)
        logger.info("Deleted all memories for user=$userId (${memories.size} records)")
    }

    // ==================== Private helpers ====================

    private fun detectSensitiveData(text: String): Boolean {
        SENSITIVE_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(text)) {
                logger.debug("Detected sensitive data pattern: ${pattern.pattern}")
                return true
            }
        }
        return false
    }

    private fun extractMemoryUpdate(
        emailSubject: String,
        emailBody: String,
        replyBody: String,
        currentSummary: String,
        currentFacts: List<MemoryFact>,
        isSensitive: Boolean
    ): MemoryUpdateResponse {
        val factsStr = if (currentFacts.isNotEmpty()) {
            currentFacts.joinToString("\n") { "${it.key}: ${it.value}" }
        } else {
            "None yet"
        }

        val prompt = """
You are an email memory manager. Extract key facts and insights from an email conversation.

IMPORTANT RULES:
- Do NOT capture sensitive data (OTP, passwords, tokens, credit cards, personal IDs)
- Only extract factual, non-sensitive information
- Return valid JSON matching the schema below
- If no new facts, return empty arrays
- Keep summary under 500 chars
- Tone should be identified from the email style

Email Subject: $emailSubject
Email Body:
$emailBody

Reply Sent:
$replyBody

Current Memory:
- Summary: ${currentSummary.ifEmpty { "Not set" }}
- Facts: $factsStr
- Sensitive data present: $isSensitive

Extract and return JSON:
{
  "summary_patch": "Brief update or new info to add to summary (plain text, no JSON)",
  "facts_add": [
    {
      "key": "fact_key_in_snake_case",
      "value": "fact value",
      "confidence": 0.8
    }
  ],
  "facts_remove_keys": ["outdated_fact_key"],
  "style_prefs": {
    "language": "vi or en or null",
    "tone": "friendly or formal or professional or null",
    "formattingNotes": "any formatting preference or null"
  },
  "sensitive": $isSensitive
}

Return ONLY valid JSON, no other text.
        """.trimIndent()

        val responseText = try {
            geminiApiClient.generateText(prompt)
        } catch (e: Exception) {
            logger.warn("Failed to call Gemini for memory update: ${e.message}")
            return MemoryUpdateResponse()
        }

        return try {
            objectMapper.readValue(responseText, MemoryUpdateResponse::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to parse Gemini response as JSON: ${e.message}, response=$responseText")
            MemoryUpdateResponse()
        }
    }

    private fun mergeMemoryUpdate(
        memory: CorrespondentMemory,
        update: MemoryUpdateResponse,
        messageId: String,
        threadId: String
    ): CorrespondentMemory {
        var updated = memory

        // Merge summary
        if (update.summary_patch.isNotEmpty()) {
            val newSummary = if (updated.profileSummary.isEmpty()) {
                update.summary_patch
            } else {
                "${updated.profileSummary}\n---\n${update.summary_patch}"
            }
            updated = updated.copy(
                profileSummary = truncateSummary(newSummary)
            )
            logger.debug("Updated summary for memory")
        }

        // Merge facts
        if (!update.sensitive) {
            var factsList = updated.facts.toMutableList()

            // Remove facts
            update.facts_remove_keys.forEach { keyToRemove ->
                factsList = factsList.filterNot { it.key == keyToRemove.lowercase() }.toMutableList()
            }

            // Add/update facts
            update.facts_add.forEach { newFact ->
                val normalizedKey = newFact.key.lowercase().replace(" ", "_")
                val existingIdx = factsList.indexOfFirst { it.key == normalizedKey }

                if (existingIdx >= 0) {
                    val existing = factsList[existingIdx]
                    // Update if confidence is higher
                    if (newFact.confidence > existing.confidence) {
                        factsList[existingIdx] = newFact.copy(key = normalizedKey, sourceMessageId = messageId)
                        logger.debug("Updated fact: $normalizedKey")
                    }
                } else {
                    factsList.add(newFact.copy(key = normalizedKey, sourceMessageId = messageId))
                    logger.debug("Added fact: $normalizedKey")
                }
            }

            // Cap facts count
            if (factsList.size > MAX_FACTS_COUNT) {
                factsList = factsList.sortedByDescending { it.confidence }
                    .take(MAX_FACTS_COUNT)
                    .toMutableList()
                logger.info("Capped facts to $MAX_FACTS_COUNT")
            }

            updated = updated.copy(facts = factsList)
        } else {
            logger.debug("Skipping fact update due to sensitive data detected")
        }

        // Merge style prefs
        if (update.style_prefs.language != null ||
            update.style_prefs.tone != null ||
            update.style_prefs.formattingNotes != null) {

            val merged = StylePrefs(
                language = update.style_prefs.language ?: updated.stylePrefs.language,
                tone = update.style_prefs.tone ?: updated.stylePrefs.tone,
                formattingNotes = update.style_prefs.formattingNotes ?: updated.stylePrefs.formattingNotes
            )
            updated = updated.copy(stylePrefs = merged)
            logger.debug("Updated style preferences")
        }

        return updated
    }

    private fun truncateSummary(summary: String): String {
        return if (summary.length > MAX_SUMMARY_LENGTH) {
            summary.substring(0, MAX_SUMMARY_LENGTH) + "..."
        } else {
            summary
        }
    }
}

