package com.openmmo.ai.controller

import com.openmmo.ai.dto.GmailStatusResponse
import com.openmmo.ai.dto.ConnectGmailResponse
import com.openmmo.ai.service.GmailOAuthService
import com.openmmo.ai.service.GmailApiService
import com.openmmo.ai.service.MailboxItem
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.Authentication
import org.springframework.web.servlet.view.RedirectView
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/gmail")
@CrossOrigin(
    origins = ["http://localhost:5173", "http://localhost:8080", "http://localhost:3000"],
    maxAge = 3600,
    allowCredentials = "true"
)
class GmailController(
    private val oauthService: GmailOAuthService,
    private val apiService: GmailApiService,
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GmailController::class.java)
    }

    @GetMapping("/connect/start")
    fun startConnect(authentication: Authentication): ResponseEntity<ConnectGmailResponse> {
        val userId = authentication.name
        val authUrl = oauthService.generateAuthUrl(userId)
        return ResponseEntity.ok(ConnectGmailResponse(url = authUrl))
    }

    @GetMapping("/connect/callback")
    fun handleCallback(
        @RequestParam code: String,
        @RequestParam state: String
    ): RedirectView {
        try {
            // Extract userId from state parameter (format: userId:randomPart)
            val parts = state.split(":")
            if (parts.size < 2) {
                throw IllegalArgumentException("Invalid state format")
            }
            val userId = parts[0]
            val redirectUrl = oauthService.handleCallback(code, state, userId)
            // Redirect to frontend with connection status
            return RedirectView(redirectUrl)
        } catch (e: Exception) {
            println("Gmail callback error: ${e.message}")
            e.printStackTrace()
            // Return error redirect to frontend
            val appUrl = oauthService.getAppWebUrl()
            return RedirectView("$appUrl/email-ai-bot?error=${e.message}")
        }
    }

    @GetMapping("/status")
    fun getStatus(authentication: Authentication): ResponseEntity<GmailStatusResponse> {
        val userId = authentication.name

        val connection = connectionRepository.findByUserId(userId)
        val config = botConfigRepository.findByUserId(userId)

        return ResponseEntity.ok(GmailStatusResponse(
            connected = connection != null,
            gmailAddress = connection?.gmailAddress ?: "",
            botEnabled = config?.enabled ?: false,
            triggerSubject = config?.triggerSubject ?: "openmmo",
            lastRunAt = config?.lastRunAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            lastError = config?.lastError
        ))
    }

    @PostMapping("/bot/enable")
    fun enableBot(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        val connection = connectionRepository.findByUserId(userId)
            ?: throw IllegalStateException("Gmail not connected")

        var config = botConfigRepository.findByUserId(userId)
        if (config == null) {
            config = com.openmmo.ai.entity.GmailBotConfig(userId = userId)
        }

        botConfigRepository.save(config.copy(enabled = true))
        return ResponseEntity.ok(mapOf("status" to "Bot enabled"))
    }

    @PostMapping("/bot/disable")
    fun disableBot(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val userId = authentication.name

        var config = botConfigRepository.findByUserId(userId)
        if (config != null) {
            botConfigRepository.save(config.copy(enabled = false))
        }

        return ResponseEntity.ok(mapOf("status" to "Bot disabled"))
    }

    @PutMapping("/bot/config")
    fun updateConfig(
        @RequestBody request: Map<String, String>,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        val triggerSubject = request["triggerSubject"] ?: return ResponseEntity.badRequest()
            .body(mapOf("error" to "Missing triggerSubject"))

        var config = botConfigRepository.findByUserId(userId)
            ?: com.openmmo.ai.entity.GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(triggerSubject = triggerSubject))
        return ResponseEntity.ok(mapOf("status" to "Config updated"))
    }

    @GetMapping("/bot/prompt")
    fun getPrompt(
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        val config = botConfigRepository.findByUserId(userId)
        val customPrompt = config?.customPrompt ?: ""

        return ResponseEntity.ok(mapOf("customPrompt" to customPrompt))
    }

    @PutMapping("/bot/prompt")
    fun updatePrompt(
        @RequestBody request: Map<String, String>,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        val customPrompt = request["customPrompt"] ?: ""

        var config = botConfigRepository.findByUserId(userId)
            ?: com.openmmo.ai.entity.GmailBotConfig(userId = userId)

        botConfigRepository.save(config.copy(customPrompt = customPrompt))
        return ResponseEntity.ok(mapOf("status" to "Prompt updated"))
    }

    @GetMapping("/mailbox")
    fun getMailbox(
        @RequestParam box: String,
        @RequestParam(defaultValue = "20") max: Int,
        authentication: Authentication
    ): ResponseEntity<List<MailboxItem>> {
        try {
            val userId = authentication.name
            logger.info("Fetching Gmail mailbox: userId=$userId, box=$box, max=$max")

            val connection = connectionRepository.findByUserId(userId)
            if (connection == null) {
                logger.warn("Gmail not connected for user: $userId")
                return ResponseEntity.status(401).body(emptyList())
            }

            logger.debug("Gmail connection found for user: $userId, email=${connection.gmailAddress}")
            val items = apiService.getMailbox(userId, box, max)
            logger.info("Successfully fetched ${items.size} emails from mailbox")
            return ResponseEntity.ok(items)
        } catch (e: IllegalStateException) {
            logger.error("Gmail state error: ${e.message}")
            return ResponseEntity.status(401).body(emptyList())
        } catch (e: Exception) {
            logger.error("Error fetching mailbox", e)
            return ResponseEntity.status(500).body(emptyList())
        }
    }
}

