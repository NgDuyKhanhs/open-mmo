package com.openmmo.ai.service.impl

import com.openmmo.ai.dto.ReminderConfigDto
import com.openmmo.ai.dto.ReminderResponse
import com.openmmo.ai.dto.toResponse
import com.openmmo.ai.entity.ReminderConfig
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.ReminderConfigRepository
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.service.IReminderService
import com.openmmo.ai.service.IAIReminderService
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IConversationContextService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReminderServiceImpl(
    private val reminderRepository: ReminderConfigRepository,
    private val gmailApiService: IGmailApiService,
    private val correspondentMemoryRepository: CorrespondentMemoryRepository,
    private val gmailBotConfigRepository: GmailBotConfigRepository,
    private val aiReminderService: IAIReminderService,
    private val conversationContextService: IConversationContextService
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
     * Preference order:
     * 1. Try to get recent conversation context from Gmail (IConversationContextService)
     * 2. Fallback to profileSummary from CorrespondentMemory DB
     * 3. If both fail, pass null/empty to AI service (it will handle gracefully)
     */
    fun buildReminderMessage(
        userId: String,
        contactEmail: String
    ): String {
        // Step 1: Try to get conversation context from Gmail (cached 15 min)
        val gmailContext = conversationContextService.getContextForReminder(userId, contactEmail)

        // Step 2: Fallback to profileSummary from DB if Gmail context not available
        val context = gmailContext ?: getCorrespondentProfileSummary(userId, contactEmail)

        // Step 3: Get user's selected AI provider
        val aiProvider = getBotConfigAiProvider(userId)

        // Step 4: Call AI service once with context (either Gmail or DB summary or null)
        logger.debug("Building reminder message for $contactEmail with context source: ${if (gmailContext != null) "Gmail" else "DB/fallback"}")
        return aiReminderService.generateReminderMessage(contactEmail, context, aiProvider)
    }

    private fun getCorrespondentProfileSummary(userId: String, contactEmail: String): String {
        return try {
            val correspondent = correspondentMemoryRepository.findByUserIdAndCorrespondentEmail(userId, contactEmail)
            correspondent?.profileSummary?.takeIf { it.isNotBlank() } ?: ""
        } catch (e: Exception) {
            logger.warn("Failed to get profile summary for $contactEmail: ${e.message}")
            ""
        }
    }

    private fun getBotConfigAiProvider(userId: String): String {
        return try {
            val botConfig = gmailBotConfigRepository.findByUserId(userId)
            botConfig?.aiProvider?.takeIf { it.isNotBlank() } ?: "groq"
        } catch (e: Exception) {
            logger.warn("Failed to get AI provider for user $userId: ${e.message}")
            "groq"  // Default to Groq
        }
    }
}









