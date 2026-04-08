package com.openmmo.ai.controller

import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin Controller
 * Admin-only operations for system management
 */
@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(
    origins = ["http://localhost:5173", "http://localhost:8080", "http://localhost:3000"],
    maxAge = 3600,
    allowCredentials = "true"
)
class AdminController(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AdminController::class.java)
    }

    /**
     * Clear all Gmail connections and bot configs
     * WARNING: This deletes all Gmail data!
     */
    @PostMapping("/gmail/clear-all")
    fun clearAllGmailConnections(): ResponseEntity<Map<String, Any>> {
        try {
            logger.warn("🔴🔴🔴 ADMIN: CLEARING ALL GMAIL DATA 🔴🔴🔴")

            connectionRepository.deleteAll()
            botConfigRepository.deleteAll()

            val remaining = mapOf(
                "gmail_connections" to connectionRepository.count(),
                "gmail_bot_configs" to botConfigRepository.count()
            )

            logger.info("✅ All Gmail data cleared. Remaining: $remaining")
            return ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "All Gmail connections and configs deleted",
                "remaining" to remaining
            ))
        } catch (e: Exception) {
            logger.error("Failed to clear Gmail connections", e)
            return ResponseEntity.status(500).body(mapOf("error" to "Failed: ${e.message}"))
        }
    }
}

