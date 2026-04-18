package com.openmmo.ai.service.impl

import com.openmmo.ai.dto.ReminderConfigDto
import com.openmmo.ai.dto.ReminderResponse
import com.openmmo.ai.dto.toResponse
import com.openmmo.ai.repository.ReminderConfigRepository
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.service.IReminderService
import com.openmmo.ai.service.IAIReminderService
import com.openmmo.ai.service.IConversationContextService
import com.openmmo.ai.service.ICorrespondentMemoryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReminderServiceImpl(
    private val reminderRepository: ReminderConfigRepository,
    private val correspondentMemoryRepository: CorrespondentMemoryRepository,
    private val gmailBotConfigRepository: GmailBotConfigRepository,
    private val aiReminderService: IAIReminderService,
    private val conversationContextService: IConversationContextService,
    private val correspondentMemoryService: ICorrespondentMemoryService
) : IReminderService {

    companion object {
        private val logger = LoggerFactory.getLogger(ReminderServiceImpl::class.java)
    }

    override fun getReminders(userId: String): List<ReminderResponse> {
        logger.debug("Getting reminders for user: $userId")
        return reminderRepository.findByUserId(userId).map { it.toResponse() }
    }

    override fun getReminder(userId: String, contactEmail: String): ReminderResponse? {
        logger.debug("Getting reminder for user: $userId, contact: $contactEmail")
        return reminderRepository.findByUserIdAndContactEmail(userId, contactEmail)?.toResponse()
    }

    override fun saveReminder(userId: String, dto: ReminderConfigDto): Map<String, String> {
        logger.info("Saving reminder for user: $userId, contact: ${dto.contactEmail}")

        // Normalize contactEmail: convert "all" or empty to "ALL_CONTACTS"
        val normalizedEmail = when {
            dto.contactEmail.isEmpty() -> ""
            dto.contactEmail.equals("all", ignoreCase = true) -> ""
            else -> dto.contactEmail
        }

        // Validate email only if not empty (empty = apply to all contacts)
        if (normalizedEmail.isNotEmpty() && !normalizedEmail.contains("@")) {
            logger.warn("Invalid email: $normalizedEmail")
            return mapOf("status" to "error", "message" to "Invalid email address")
        }

        // Use "ALL_CONTACTS" as storage key for empty email
        val storageKey = if (normalizedEmail.isEmpty()) "ALL_CONTACTS" else normalizedEmail

        val existing = reminderRepository.findByUserIdAndContactEmail(userId, storageKey)

        // Set default values for null fields
        val maxReminders = dto.maxReminders ?: 5

        val reminder = if (existing != null) {
            // Update existing
            existing.copy(
                enabled = dto.enabled,
                afterInactive = dto.afterInactive,
                maxReminders = maxReminders,
                updatedAt = LocalDateTime.now()
            )
        } else {
            // Create new - use the storage key and set defaults for nulls
            dto.toEntity(userId, storageKey).copy(
                maxReminders = maxReminders
            )
        }

        reminderRepository.save(reminder)
        logger.info("Reminder saved for user: $userId, contact: $normalizedEmail, storageKey: $storageKey")

        return mapOf(
            "status" to "success",
            "message" to if (existing != null) "Reminder updated" else "Reminder created"
        )
    }

    override fun deleteReminder(userId: String, contactEmail: String): Map<String, String> {
        logger.info("Deleting reminder for user: $userId, contact: $contactEmail")

        // Handle "all contacts" deletion - normalize various forms to "ALL_CONTACTS"
        val lookupEmail = when {
            contactEmail.isEmpty() -> "ALL_CONTACTS"
            contactEmail.equals("all", ignoreCase = true) -> "ALL_CONTACTS"
            else -> contactEmail
        }

        val reminder = reminderRepository.findByUserIdAndContactEmail(userId, lookupEmail)
        if (reminder == null) {
            logger.warn("Reminder not found for userId=$userId, lookupEmail=$lookupEmail")
            return mapOf("status" to "error", "message" to "Reminder not found")
        }

        reminderRepository.deleteByUserIdAndContactEmail(userId, lookupEmail)
        logger.info("Reminder deleted for user: $userId, contact: $contactEmail")

        return mapOf("status" to "success", "message" to "Reminder deleted")
    }

    // ...(scheduler là ReminderScheduler có di chuyển)...

    /**
     * Build reminder message with conversation context
     *
     * Lazy build strategy (Phần 3):
     * 1. Call ensureMemoryUpToDate() to lazy-load contact memory (checks for new threads)
     *    - If no new threads since last update: cache hit, very cheap (no AI call)
     *    - If new threads: collect 5 threads, call AI for compression
     * 2. Get conversation context (Gmail or DB fallback)
     * 3. Use memory.preferredLanguage for language-aware reminder generation
     * 4. Generate reminder message
     *
     * This ensures memory is always fresh when reminder sends without extra calls during CRUD.
     */
    fun buildReminderMessage(
        userId: String,
        contactEmail: String
    ): String {
        // Step 0: Lazy-build memory (new approach - Phần 3)
        logger.debug("Lazy-building memory for $contactEmail during reminder generation...")
        try {
            correspondentMemoryService.ensureMemoryUpToDate(userId, contactEmail)
        } catch (e: Exception) {
            logger.warn("Failed to ensure memory up-to-date: ${e.message}, continuing with existing memory")
        }

        // Step 1: Try to get conversation context from Gmail (cached 15 min)
        val contextResult = conversationContextService.getContextForReminder(userId, contactEmail)
        val gmailContext = contextResult?.first
        val detectedLanguage = contextResult?.second

        // Step 2: Get correspondent memory (for context when thread is empty or to combine with Gmail)
        val memory = correspondentMemoryService.getOrCreate(userId, contactEmail)
        val correspondentMemoryContext = correspondentMemoryService.buildMemoryContextText(memory)

        // Step 3: Combine contexts (Gmail thread + correspondent memory)
        val context = when {
            // Case 1: Both Gmail context AND memory available → COMBINE them
            gmailContext != null && correspondentMemoryContext.isNotEmpty() -> {
                logger.debug("Combining Gmail thread context with correspondent memory for richer context")
                "$gmailContext\n\n[Historical Context]\n$correspondentMemoryContext"
            }
            // Case 2: Only Gmail context available
            gmailContext != null -> {
                logger.debug("Using Gmail thread context")
                gmailContext
            }
            // Case 3: Only memory available
            correspondentMemoryContext.isNotEmpty() -> {
                logger.debug("Using correspondent memory context")
                correspondentMemoryContext
            }
            // Case 4: Neither available → fallback to empty
            else -> {
                logger.warn("No context available for $contactEmail (neither Gmail thread nor memory)")
                ""
            }
        }

        // Step 4: Get user's selected AI provider
        val aiProvider = getBotConfigAiProvider(userId)

        // Step 5: Call AI service with combined context and detected language
        logger.debug("Building reminder message for $contactEmail with context source: ${when {
            gmailContext != null && correspondentMemoryContext.isNotEmpty() -> "Combined (Gmail + DB)"
            gmailContext != null -> "Gmail"
            correspondentMemoryContext.isNotEmpty() -> "DB Memory"
            else -> "None"
        }}, language: $detectedLanguage")
        return aiReminderService.generateReminderMessage(contactEmail, context, aiProvider, detectedLanguage)
    }


    private fun getBotConfigAiProvider(userId: String): String {
        return try {
            val botConfig = gmailBotConfigRepository.findByUserId(userId)
            botConfig?.aiProvider?.takeIf { it.isNotBlank() } ?: "gemini"
        } catch (e: Exception) {
            logger.warn("Failed to get AI provider for user $userId: ${e.message}")
            "gemini"  // Default to Gemini
        }
    }
}









