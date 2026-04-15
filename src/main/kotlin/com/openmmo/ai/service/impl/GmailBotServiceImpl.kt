package com.openmmo.ai.service.impl

import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.entity.GmailBotConfig
import com.openmmo.ai.dto.GmailStatusResponse
import com.openmmo.ai.service.IGmailBotService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * Gmail Bot Service Implementation
 * Manages Gmail bot configuration and status
 */
@Service
class GmailBotServiceImpl(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository
) : IGmailBotService {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailBotServiceImpl::class.java)
    }

    override fun getStatus(userId: String): GmailStatusResponse {
        logger.info("Getting Gmail bot status for user: $userId")

        val connection = connectionRepository.findByUserId(userId)
        val config = botConfigRepository.findByUserId(userId)

        return GmailStatusResponse(
            connected = connection != null,
            gmailAddress = connection?.gmailAddress ?: "",
            botEnabled = config?.enabled ?: false,
            triggerSubject = config?.triggerSubject ?: "openmmo",
            lastRunAt = config?.lastRunAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            lastError = config?.lastError
        )
    }

    override fun enableBot(userId: String): Map<String, String> {
        logger.info("Enabling Gmail bot for user: $userId")

        connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        val config = botConfigRepository.findByUserId(userId)
            ?: GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(enabled = true))
        logger.debug("Gmail bot enabled for user: $userId")

        return mapOf("status" to "Bot enabled")
    }

    override fun disableBot(userId: String): Map<String, String> {
        logger.info("Disabling Gmail bot for user: $userId")

        val config = botConfigRepository.findByUserId(userId)
        if (config != null) {
            botConfigRepository.save(config.copy(enabled = false))
            logger.debug("Gmail bot disabled for user: $userId")
        }

        return mapOf("status" to "Bot disabled")
    }

    override fun updateConfig(userId: String, triggerSubject: String): Map<String, String> {
        logger.info("Updating Gmail bot config for user: $userId")

        // triggerSubject is now optional - can be empty to reply to ALL emails
        // If blank, will use skip rules (13 rules) to filter spam/promotional/system emails
        val trimmedSubject = triggerSubject.trim().lowercase()
        if (trimmedSubject.isBlank()) {
            logger.warn("⚠️ triggerSubject is blank for user: $userId - bot will reply to ALL emails (with skip rules for safety)")
        } else {
            logger.debug("triggerSubject set to: $trimmedSubject for user: $userId")
        }

        val config = botConfigRepository.findByUserId(userId)
            ?: GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(triggerSubject = trimmedSubject))
        logger.debug("Gmail bot config updated for user: $userId")

        return mapOf(
            "status" to "success",
            "message" to "Config updated" + if (trimmedSubject.isBlank()) " (will reply to ALL emails)" else ""
        )
    }

    override fun getPrompt(userId: String): Map<String, String> {
        logger.debug("Getting Gmail bot prompt for user: $userId")

        val config = botConfigRepository.findByUserId(userId)

        // customPrompt can be empty string (use default) or have value (use custom)
        val customPrompt = config?.customPrompt ?: ""
        val isUsingDefault = customPrompt.isBlank()

        return mapOf(
            "customPrompt" to customPrompt,
            "isUsingDefault" to isUsingDefault.toString(),
            "status" to "success"
        )
    }

    override fun updatePrompt(userId: String, customPrompt: String): Map<String, String> {
        logger.info("Updating Gmail bot prompt for user: $userId")

        val config = botConfigRepository.findByUserId(userId)
            ?: GmailBotConfig(userId = userId)

        // Always save customPrompt (even if empty), just trim whitespace
        val promptToSave = customPrompt.trim()

        botConfigRepository.save(config.copy(customPrompt = promptToSave))
        logger.debug("Gmail bot prompt updated for user: $userId, isEmpty=${promptToSave.isBlank()}")

        return mapOf(
            "status" to "success",
            "message" to if (promptToSave.isBlank()) "Prompt cleared, will use default" else "Custom prompt saved"
        )
    }

    override fun getAiProvider(userId: String): Map<String, String> {
        logger.debug("Getting AI provider for user: $userId")

        val config = botConfigRepository.findByUserId(userId)
        val provider = config?.aiProvider ?: "groq"

        return mapOf(
            "aiProvider" to provider,
            "status" to "success"
        )
    }

    override fun setAiProvider(userId: String, provider: String): Map<String, String> {
        logger.info("Setting AI provider for user: $userId to: $provider")

        val validProviders = setOf("groq", "gemini")
        val normalizedProvider = provider.lowercase()

        if (normalizedProvider !in validProviders) {
            logger.warn("Invalid AI provider: $provider for user: $userId")
            return mapOf(
                "status" to "error",
                "message" to "Invalid provider. Must be 'groq' or 'gemini'"
            )
        }

        val config = botConfigRepository.findByUserId(userId)
            ?: GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(aiProvider = normalizedProvider))
        logger.debug("AI provider updated for user: $userId to: $normalizedProvider")

        return mapOf(
            "status" to "success",
            "message" to "AI provider updated to: $normalizedProvider",
            "aiProvider" to normalizedProvider
        )
    }
}

