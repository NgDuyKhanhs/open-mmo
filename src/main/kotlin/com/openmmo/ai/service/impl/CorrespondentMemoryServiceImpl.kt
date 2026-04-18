package com.openmmo.ai.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.openmmo.ai.client.GmailApiClient
import com.openmmo.ai.entity.CorrespondentMemory
import com.openmmo.ai.entity.GmailConnection
import com.openmmo.ai.entity.MemoryFact
import com.openmmo.ai.entity.MemoryUpdateResponse
import com.openmmo.ai.entity.StylePrefs
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.service.ICorrespondentMemoryService
import com.openmmo.ai.service.IGeminiAiService
import com.openmmo.ai.service.IGmailApiService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64

/**
 * Correspondent Memory Service Implementation
 * Manages per-correspondent conversation context, facts, and style preferences
 * Uses user's selected AI provider (Groq or Gemini) to extract memory updates
 */
@Service
class CorrespondentMemoryServiceImpl(
    private val memoryRepository: CorrespondentMemoryRepository,
    private val gmailConnectionRepository: GmailConnectionRepository,
    private val gmailBotConfigRepository: GmailBotConfigRepository,
    private val gmailApiClient: GmailApiClient,
    private val gmailApiService: IGmailApiService,
    @Qualifier("groqAiServiceImpl")
    private val groqAiService: IGeminiAiService,
    @Qualifier("geminiAiServiceImpl")
    private val geminiAiService: IGeminiAiService
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
        replyBody: String,
        aiProvider: String
    ): CorrespondentMemory {
        try {
            logger.info("Updating memory for user=$userId, correspondent=$correspondentEmail using provider=$aiProvider")

            var memory = getOrCreate(userId, correspondentEmail)

            // Check if sensitive data present
            val isSensitive = detectSensitiveData(emailBody + "\n" + replyBody)

            // Call selected AI provider to extract update
            val updateResponse = extractMemoryUpdate(
                emailSubject,
                emailBody,
                replyBody,
                memory.profileSummary,
                memory.facts,
                isSensitive,
                aiProvider
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

    /**
     * Ensure memory is up-to-date (lazy build strategy)
     *
     * Behavior:
     * 1. Normalize email, load or create empty memory
     * 2. Query Gmail for latest thread with contact (90 days)
     * 3. Check if thread changed (cache hit detection)
     * 4. If thread changed: collect 5 threads, call AI for compression
     * 5. Update preferredLanguage and languageConfidence
     * 6. Return updated memory
     */
    private fun decryptAccessToken(connection: GmailConnection): String {
        return try {
            // Use GmailApiServiceImpl's public method if available
            if (gmailApiService is GmailApiServiceImpl) {
                gmailApiService.getAccessTokenPublic(connection)
            } else {
                // Fallback: If token starts with "Bearer ", assume it's already decrypted
                ""  // This shouldn't happen in normal operation
            }
        } catch (e: Exception) {
            logger.warn("Failed to get access token: ${e.message}")
            ""
        }
    }

    override fun ensureMemoryUpToDate(userId: String, correspondentEmail: String): CorrespondentMemory {
        try {
            val normalizedEmail = correspondentEmail.trim().lowercase()
            logger.debug("Ensuring memory up-to-date for user=$userId, correspondent=$normalizedEmail")

            // Step 1: Load or create empty memory
            var memory = getOrCreate(userId, normalizedEmail)

            // Step 2: Get Gmail connection
            val connection = gmailConnectionRepository.findByUserId(userId)
            if (connection == null) {
                logger.warn("Gmail not connected for user=$userId, returning existing memory")
                return memory
            }

            val accessToken = decryptAccessToken(connection)

            // Step 3: Find latest thread ID for this contact
            val latestThreadId = findLatestThreadId(accessToken, normalizedEmail)
            if (latestThreadId == null) {
                logger.debug("No threads found for $normalizedEmail in last 90 days, memory unchanged")
                return memory
            }

            // Step 4: Check for cache hit (no new threads)
            if (latestThreadId == memory.lastThreadId) {
                logger.debug("Cache hit: latestThreadId=$latestThreadId matches lastThreadId, updating lastSeenAt only")
                memory = memory.copy(
                    lastSeenAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                return memoryRepository.save(memory)
            }

            // Step 5: Thread changed - collect recent threads and compress
            logger.info("Thread changed: old=${memory.lastThreadId}, new=$latestThreadId, compressing memory...")

            val threadExcerpts = collectThreadExcerpts(accessToken, normalizedEmail)
            if (threadExcerpts.isEmpty()) {
                logger.debug("No thread content to compress, updating lastThreadId only")
                memory = memory.copy(
                    lastThreadId = latestThreadId,
                    lastSeenAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                return memoryRepository.save(memory)
            }

            // Step 6: Call AI to compress and detect language
            val botConfig = gmailBotConfigRepository.findByUserId(userId)
            val aiProvider = botConfig?.aiProvider?.lowercase() ?: "gemini"

            val (detectedLanguage, confidence, updateResponse) = compressMemoryWithAi(
                correspondentEmail = normalizedEmail,
                threadExcerpts = threadExcerpts,
                currentMemory = memory,
                aiProvider = aiProvider
            )

            // Step 7: Merge AI response into memory
            memory = mergeMemoryUpdate(memory, updateResponse, "", latestThreadId)

            // Step 8: Update with language detection and thread info
            memory = memory.copy(
                preferredLanguage = detectedLanguage,
                languageConfidence = confidence,
                lastThreadId = latestThreadId,
                lastSeenAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                version = memory.version + 1
            )

            val saved = memoryRepository.save(memory)
            logger.info("Memory updated: language=$detectedLanguage (confidence=$confidence), threadId=$latestThreadId")
            return saved

        } catch (e: Exception) {
            logger.error("Error ensuring memory up-to-date for user=$userId, correspondent=$correspondentEmail: ${e.message}", e)
            // Don't crash - return existing memory or create new empty one
            return getOrCreate(userId, correspondentEmail)
        }
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
        isSensitive: Boolean,
        aiProvider: String
    ): MemoryUpdateResponse {
        val factsStr = if (currentFacts.isNotEmpty()) {
            currentFacts.joinToString("\n") { "${it.key}: ${it.value}" }
        } else {
            "None yet"
        }

        val prompt = """
You are an email memory manager. Extract key facts and insights from an email conversation.

INSTRUCTIONS:
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
            val aiService = getAiServiceForProvider(aiProvider)
            aiService.generateText(prompt)
        } catch (e: Exception) {
            logger.warn("Failed to call $aiProvider for memory update: ${e.message}")
            return MemoryUpdateResponse()
        }

        return try {
            // Strip markdown backticks if present (Groq sometimes returns markdown format)
            val cleanedResponse = responseText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            logger.debug("Parsing memory update response (${cleanedResponse.length} chars)")
            objectMapper.readValue(cleanedResponse, MemoryUpdateResponse::class.java)
        } catch (e: Exception) {
            logger.warn("Failed to parse $aiProvider response as JSON: ${e.message}, response=$responseText")
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

    /**
     * Get the appropriate AI service based on provider preference
     * @param provider "groq" or "gemini"
     * @return IGeminiAiService instance (either Groq or Gemini impl)
     */
    private fun getAiServiceForProvider(provider: String): IGeminiAiService {
        return when (provider.lowercase()) {
            "gemini" -> {
                logger.debug("Using Gemini AI service for memory extraction")
                geminiAiService
            }
            else -> {
                logger.debug("Using Groq AI service for memory extraction (default)")
                groqAiService
            }
        }
    }

    // ==================== Private helpers for ensureMemoryUpToDate ====================

    private fun findLatestThreadId(accessToken: String, contactEmail: String): String? {
        return try {
            val query = "(from:$contactEmail OR to:$contactEmail) newer_than:90d"
            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.listMessages(accessToken, query, maxResults = 1)
            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()
            messages.firstOrNull()?.get("threadId").also {
                logger.debug("Latest threadId for $contactEmail: $it")
            }
        } catch (e: Exception) {
            logger.warn("Failed to find latest thread for $contactEmail: ${e.message}")
            null
        }
    }

    private fun collectThreadExcerpts(accessToken: String, contactEmail: String): String {
        return try {
            val query = "(from:$contactEmail OR to:$contactEmail) newer_than:90d"

            @Suppress("UNCHECKED_CAST")
            val response = gmailApiClient.listMessages(accessToken, query, maxResults = 15)
            val messages = response["messages"] as? List<Map<String, String>> ?: emptyList()

            // Get unique thread IDs
            val threadIds = messages.mapNotNull { it["threadId"] }.distinct().take(5)
            logger.debug("Collecting excerpts from ${threadIds.size} threads")

            val excerpts = mutableListOf<String>()
            var totalChars = 0
            val MAX_TOTAL_CHARS = 20000

            for (threadId in threadIds) {
                if (totalChars >= MAX_TOTAL_CHARS) break

                try {
                    @Suppress("UNCHECKED_CAST")
                    val threadData = gmailApiClient.getThread(accessToken, threadId)
                    val threadMessages = threadData["messages"] as? List<Map<String, Any>> ?: emptyList()

                    // Take last 2 messages from thread
                    threadMessages.takeLast(2).forEach { msg ->
                        val payload = msg["payload"] as? Map<String, Any>
                        val headers = payload?.get("headers") as? List<Map<String, String>> ?: emptyList()

                        val subject = headers.find { it["name"] == "Subject" }?.get("value") ?: "No Subject"
                        val date = headers.find { it["name"] == "Date" }?.get("value") ?: ""
                        val from = headers.find { it["name"] == "From" }?.get("value") ?: ""

                        val body = extractMessageBody(msg)
                        val truncatedBody = body.take(1200)

                        val excerpt = "[$date] From: $from\nSubject: $subject\n$truncatedBody\n---"
                        excerpts.add(excerpt)
                        totalChars += excerpt.length
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to collect excerpt from thread $threadId: ${e.message}")
                }
            }

            excerpts.joinToString("\n\n").take(MAX_TOTAL_CHARS).also {
                logger.debug("Collected ${it.length} chars from ${excerpts.size} message excerpts")
            }
        } catch (e: Exception) {
            logger.warn("Failed to collect thread excerpts: ${e.message}")
            ""
        }
    }

    private fun extractMessageBody(message: Map<String, Any>): String {
        return try {
            val payload = message["payload"] as? Map<String, Any> ?: return ""

            // Try to get plain text body first
            val parts = payload["parts"] as? List<Map<String, Any>>
            if (!parts.isNullOrEmpty()) {
                for (part in parts) {
                    val mimeType = part["mimeType"] as? String ?: ""
                    if (mimeType == "text/plain") {
                        val body = part["body"] as? Map<String, String>
                        val data = body?.get("data")
                        if (!data.isNullOrBlank()) {
                            return String(Base64.getDecoder().decode(data)).take(1200)
                        }
                    }
                }
            }

            // Fallback to main body
            val body = payload["body"] as? Map<String, String> ?: return ""
            val data = body["data"]
            if (!data.isNullOrBlank()) {
                String(Base64.getDecoder().decode(data)).take(1200)
            } else {
                ""
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract message body: ${e.message}")
            ""
        }
    }

    private fun compressMemoryWithAi(
        correspondentEmail: String,
        threadExcerpts: String,
        currentMemory: CorrespondentMemory,
        aiProvider: String
    ): Triple<String?, Double?, MemoryUpdateResponse> {
        return try {
            val factsStr = if (currentMemory.facts.isNotEmpty()) {
                currentMemory.facts.joinToString("\n") { "${it.key}: ${it.value}" }
            } else {
                "None yet"
            }

            val prompt = """
You are an email memory compressor. Extract profile, facts, language, and style from email threads.

INSTRUCTIONS:
- Detect contact's preferred language (vi, en, etc.) from thread content
- Extract key personal/business facts (name, role, company, interests, etc.)
- Identify communication style (formal, casual, friendly, technical)
- Return valid JSON matching schema
- Do NOT capture sensitive data
- Keep summary under 500 chars

EMAIL THREADS FROM CONTACT '$correspondentEmail':
$threadExcerpts

CURRENT MEMORY:
Summary: ${currentMemory.profileSummary.ifEmpty { "Not set" }}
Facts: $factsStr

Extract and return JSON:
{
  "summary_patch": "Brief profile update or key info (plain text)",
  "facts_add": [
    {
      "key": "fact_key",
      "value": "fact value",
      "confidence": 0.8
    }
  ],
  "facts_remove_keys": [],
  "style_prefs": {
    "language": "vi or en or other code",
    "tone": "formal or casual or friendly or null",
    "formattingNotes": "any style notes or null"
  },
  "sensitive": false,
  "language_confidence": 0.9
}

Return ONLY valid JSON, no other text.
            """.trimIndent()

            logger.debug("Calling $aiProvider for memory compression...")
            val responseText = try {
                val aiService = getAiServiceForProvider(aiProvider)
                aiService.generateText(prompt)
            } catch (e: Exception) {
                logger.warn("Failed to call $aiProvider: ${e.message}, trying fallback...")
                val fallbackService = if (aiProvider == "gemini") groqAiService else geminiAiService
                fallbackService.generateText(prompt)
            }

            val cleanedResponse = responseText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            logger.debug("Parsing AI response (${cleanedResponse.length} chars)")

            @Suppress("UNCHECKED_CAST")
            val parsed = objectMapper.readValue(cleanedResponse, Map::class.java) as Map<String, Any>

            val stylePrefsMap = (parsed["style_prefs"] as? Map<String, Any>) ?: mapOf()
            val detectedLanguage = (stylePrefsMap["language"] ?: parsed["language_code"]) as? String
            val confidence = (parsed["language_confidence"] as? Number)?.toDouble() ?: 0.8

            // Convert parsed map to MemoryUpdateResponse
            @Suppress("UNCHECKED_CAST")
            val stylePrefs = parsed["style_prefs"] as? Map<String, String> ?: mapOf()
            @Suppress("UNCHECKED_CAST")
            val factsAdd = (parsed["facts_add"] as? List<Map<String, Any>> ?: emptyList()).map { factMap ->
                MemoryFact(
                    key = (factMap["key"] as? String)?.lowercase()?.replace(" ", "_") ?: "unknown",
                    value = factMap["value"] as? String ?: "",
                    confidence = (factMap["confidence"] as? Number)?.toDouble() ?: 0.6
                )
            }

            @Suppress("UNCHECKED_CAST")
            val updateResponse = MemoryUpdateResponse(
                summary_patch = parsed["summary_patch"] as? String ?: "",
                facts_add = factsAdd,
                facts_remove_keys = (parsed["facts_remove_keys"] as? List<String>) ?: emptyList(),
                style_prefs = StylePrefs(
                    language = stylePrefs["language"],
                    tone = stylePrefs["tone"],
                    formattingNotes = stylePrefs["formattingNotes"]
                ),
                sensitive = (parsed["sensitive"] as? Boolean) ?: false
            )

            logger.debug("Memory compression result: language=$detectedLanguage, confidence=$confidence")
            Triple(detectedLanguage, confidence, updateResponse)
        } catch (e: Exception) {
            logger.warn("Failed to compress memory: ${e.message}, returning empty update")
            Triple(null, null, MemoryUpdateResponse())
        }
    }
}
