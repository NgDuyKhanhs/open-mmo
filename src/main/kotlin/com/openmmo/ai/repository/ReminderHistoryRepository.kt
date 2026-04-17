package com.openmmo.ai.repository

import com.openmmo.ai.entity.ReminderHistory
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for Reminder History
 */
@Repository
interface ReminderHistoryRepository : MongoRepository<ReminderHistory, String> {

    /**
     * Find all reminder history for a user
     * @param userId User ID
     * @return List of reminder history sorted by sentAt descending
     */
    fun findByUserIdOrderBySentAtDesc(userId: String): List<ReminderHistory>

    /**
     * Find reminder history for a specific contact
     * @param userId User ID
     * @param contactEmail Contact email
     * @return List of reminder history
     */
    fun findByUserIdAndContactEmailOrderBySentAtDesc(userId: String, contactEmail: String): List<ReminderHistory>

    /**
     * Find reminder history within date range
     * @param userId User ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of reminder history
     */
    fun findByUserIdAndSentAtBetweenOrderBySentAtDesc(userId: String, startDate: LocalDateTime, endDate: LocalDateTime): List<ReminderHistory>

    /**
     * Find reminder history by status
     * @param userId User ID
     * @param status Status (sent, failed, skipped)
     * @return List of reminder history
     */
    fun findByUserIdAndStatusOrderBySentAtDesc(userId: String, status: String): List<ReminderHistory>
}

