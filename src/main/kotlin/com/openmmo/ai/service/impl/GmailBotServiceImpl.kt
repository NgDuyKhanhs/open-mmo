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

        val config = botConfigRepository.findByUserId(userId)
            ?: GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(triggerSubject = triggerSubject))
        logger.debug("Gmail bot config updated for user: $userId")

        return mapOf("status" to "Config updated")
    }

    override fun getPrompt(userId: String): Map<String, String> {
        logger.debug("Getting Gmail bot prompt for user: $userId")

        val config = botConfigRepository.findByUserId(userId)
        val customPrompt = config?.customPrompt ?: ""

        return mapOf("customPrompt" to customPrompt)
    }

    override fun updatePrompt(userId: String, customPrompt: String): Map<String, String> {
        logger.info("Updating Gmail bot prompt for user: $userId")

        val config = botConfigRepository.findByUserId(userId)
            ?: GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(customPrompt = customPrompt))
        logger.debug("Gmail bot prompt updated for user: $userId")

        return mapOf("status" to "Prompt updated")
    }
}

