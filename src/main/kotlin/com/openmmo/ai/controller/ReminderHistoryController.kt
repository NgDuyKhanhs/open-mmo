package com.openmmo.ai.controller

import com.openmmo.ai.dto.toItemResponse
import com.openmmo.ai.dto.toResponse
import com.openmmo.ai.repository.ReminderHistoryRepository
import com.openmmo.ai.util.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Reminder History Controller
 * API endpoints for retrieving reminder history
 */
@RestController
@RequestMapping("/api/v1/reminders/history")
class ReminderHistoryController(
    private val reminderHistoryRepository: ReminderHistoryRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ReminderHistoryController::class.java)
    }

    /**
     * Get all reminder history for logged-in user
     */
    @GetMapping
    fun getReminderHistory(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val token = authHeader.removePrefix("Bearer ")
            val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return ResponseEntity.status(401).body(mapOf("status" to "error", "message" to "Unauthorized"))
            logger.debug("Getting reminder history for user: {}", userId)

            val histories = reminderHistoryRepository.findByUserIdOrderBySentAtDesc(userId)
            logger.debug("Found {} reminder histories", histories.size)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "total" to histories.size,
                    "data" to histories.map { it.toItemResponse() }
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting reminder history", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    /**
     * Get reminder history for a specific contact
     */
    @GetMapping("/contact/{contactEmail}")
    fun getReminderHistoryForContact(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable contactEmail: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val token = authHeader.removePrefix("Bearer ")
            val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return ResponseEntity.status(401).body(mapOf("status" to "error", "message" to "Unauthorized"))
            logger.debug("Getting reminder history for {}", contactEmail)

            val histories = reminderHistoryRepository.findByUserIdAndContactEmailOrderBySentAtDesc(userId, contactEmail)
            logger.debug("Found {} reminders for contact", histories.size)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "contactEmail" to contactEmail,
                    "total" to histories.size,
                    "data" to histories.map { it.toItemResponse() }
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting reminder history for contact", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    /**
     * Get reminder history by status
     */
    @GetMapping("/status/{status}")
    fun getReminderHistoryByStatus(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable status: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val token = authHeader.removePrefix("Bearer ")
            val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return ResponseEntity.status(401).body(mapOf("status" to "error", "message" to "Unauthorized"))
            logger.debug("Getting reminder history with status: {}", status)

            val validStatuses = setOf("sent", "failed", "skipped")
            if (!validStatuses.contains(status)) {
                return ResponseEntity.badRequest().body(
                    mapOf(
                        "status" to "error",
                        "message" to "Invalid status. Must be one of: $validStatuses"
                    )
                )
            }

            val histories = reminderHistoryRepository.findByUserIdAndStatusOrderBySentAtDesc(userId, status)
            logger.debug("Found {} reminders with status", histories.size)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "filter" to status,
                    "total" to histories.size,
                    "data" to histories.map { it.toItemResponse() }
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting reminder history by status", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    /**
     * Get reminder history for specific date range
     */
    @GetMapping("/date-range")
    fun getReminderHistoryByDateRange(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val token = authHeader.removePrefix("Bearer ")
            val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return ResponseEntity.status(401).body(mapOf("status" to "error", "message" to "Unauthorized"))
            logger.debug("Getting reminder history from {} to {}", startDate, endDate)

            val start = LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIDNIGHT)
            val end = LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX)

            val histories = reminderHistoryRepository.findByUserIdAndSentAtBetweenOrderBySentAtDesc(userId, start, end)
            logger.debug("Found {} reminders in date range", histories.size)

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "total" to histories.size,
                    "data" to histories.map { it.toItemResponse() }
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting reminder history by date range", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    /**
     * Get single reminder history detail
     */
    @GetMapping("/{id}")
    fun getReminderHistoryDetail(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val token = authHeader.removePrefix("Bearer ")
            val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return ResponseEntity.status(401).body(mapOf("status" to "error", "message" to "Unauthorized"))
            logger.debug("Getting reminder history detail: {}", id)

            val history = reminderHistoryRepository.findById(id)
            if (history.isEmpty) {
                return ResponseEntity.notFound().build()
            }

            val reminder = history.get()
            if (reminder.userId != userId) {
                return ResponseEntity.status(403).body(
                    mapOf("status" to "error", "message" to "Forbidden")
                )
            }

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "data" to reminder.toResponse()
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting reminder history detail", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }

    /**
     * Get reminder history statistics
     */
    @GetMapping("/stats/summary")
    fun getReminderHistoryStats(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val token = authHeader.removePrefix("Bearer ")
            val userId = jwtTokenProvider.getUserIdFromToken(token) ?: return ResponseEntity.status(401).body(mapOf("status" to "error", "message" to "Unauthorized"))
            logger.debug("Getting reminder history statistics")

            val allHistories = reminderHistoryRepository.findByUserIdOrderBySentAtDesc(userId)
            val sentCount = allHistories.count { it.status == "sent" }
            val failedCount = allHistories.count { it.status == "failed" }
            val skippedCount = allHistories.count { it.status == "skipped" }

            ResponseEntity.ok(
                mapOf(
                    "status" to "success",
                    "total" to allHistories.size,
                    "sent" to sentCount,
                    "failed" to failedCount,
                    "skipped" to skippedCount,
                    "successRate" to if (allHistories.isNotEmpty()) {
                        (sentCount * 100) / allHistories.size
                    } else {
                        0
                    }
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting reminder history statistics", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            )
        }
    }
}

