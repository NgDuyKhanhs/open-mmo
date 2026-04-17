package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Reminder History Entity
 * Tracks each reminder sent to contacts
 * Used for audit, analytics, and user history display
 */
@Document(collection = "reminder_history")
data class ReminderHistory(
    @Id
    val id: String? = null,
    val userId: String,
    val contactEmail: String,
    val subject: String,
    val body: String,
    val status: String,  // "sent", "failed", "skipped"
    val reason: String = "",  // Reason if failed/skipped
    val sentAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

