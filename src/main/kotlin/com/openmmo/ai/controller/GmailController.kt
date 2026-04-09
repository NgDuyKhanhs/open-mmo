package com.openmmo.ai.controller

import com.openmmo.ai.dto.GmailStatusResponse
import com.openmmo.ai.dto.ConnectGmailResponse
import com.openmmo.ai.dto.MailboxItemResponse
import com.openmmo.ai.service.IGmailOAuthService
import com.openmmo.ai.service.IGmailApiService
import com.openmmo.ai.service.IGmailBotService
import com.openmmo.ai.service.IGmailAutoReplyService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.Authentication
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/v1/gmail")
class GmailController(
    private val oauthService: IGmailOAuthService,
    private val apiService: IGmailApiService,
    private val botService: IGmailBotService,
    private val autoReplyService: IGmailAutoReplyService
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
        logger.info("Getting Gmail status for user: $userId")
        return ResponseEntity.ok(botService.getStatus(userId))
    }

    @PostMapping("/bot/enable")
    fun enableBot(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        logger.info("Enabling Gmail bot for user: $userId")
        return ResponseEntity.ok(botService.enableBot(userId))
    }

    @PostMapping("/bot/disable")
    fun disableBot(authentication: Authentication): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        logger.info("Disabling Gmail bot for user: $userId")
        return ResponseEntity.ok(botService.disableBot(userId))
    }

    @PutMapping("/bot/config")
    fun updateConfig(
        @RequestBody request: Map<String, String>,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        val triggerSubject = request["triggerSubject"] ?: return ResponseEntity.badRequest()
            .body(mapOf(
                "status" to "error",
                "message" to "Missing triggerSubject"
            ))

        logger.info("Updating Gmail bot config for user: $userId")
        val result = botService.updateConfig(userId, triggerSubject)

        // Check if update was successful or validation failed
        return if (result["status"] == "error") {
            ResponseEntity.badRequest().body(result)
        } else {
            ResponseEntity.ok(result)
        }
    }

    @GetMapping("/bot/prompt")
    fun getPrompt(
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        logger.debug("Getting Gmail bot prompt for user: $userId")
        return ResponseEntity.ok(botService.getPrompt(userId))
    }

    @PutMapping("/bot/prompt")
    fun updatePrompt(
        @RequestBody request: Map<String, String>,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        val customPrompt = request["customPrompt"] ?: ""

        logger.info("Updating Gmail bot prompt for user: $userId")
        return ResponseEntity.ok(botService.updatePrompt(userId, customPrompt))
    }

    @GetMapping("/mailbox")
    fun getMailbox(
        @RequestParam box: String,
        @RequestParam(defaultValue = "20") max: Int,
        authentication: Authentication
    ): ResponseEntity<List<MailboxItemResponse>> {
        val userId = authentication.name
        logger.info("Fetching Gmail mailbox: userId=$userId, box=$box, max=$max")
        val items = apiService.getMailbox(userId, box, max)
        logger.info("Successfully fetched ${items.size} emails from mailbox")
        return ResponseEntity.ok(items)
    }

    @PostMapping("/ai-reply")
    fun generateAiReply(
        @RequestParam messageId: String,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        logger.info("Generating AI reply for message: $messageId")
        val aiReply = apiService.generateAiReply(userId, messageId)
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "reply" to aiReply
        ))
    }

    @PostMapping("/auto-reply")
    fun triggerAutoReply(
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userId = authentication.name
        logger.info("Manually triggering auto-reply for user: $userId")
        val replied = autoReplyService.autoReplyForUser(userId)
        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Auto-reply completed",
            "emailsReplied" to replied
        ))
    }

    @PostMapping("/auto-reply/all")
    fun triggerAutoReplyAll(): ResponseEntity<Map<String, Any>> {
        logger.info("Manually triggering auto-reply for all users")
        val result = autoReplyService.autoReplyEmails()
        return ResponseEntity.ok(result)
    }
}

