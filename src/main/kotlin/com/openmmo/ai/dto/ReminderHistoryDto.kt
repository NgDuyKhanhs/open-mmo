package com.openmmo.ai.dto

import java.time.LocalDateTime

/**
 * Reminder History DTO for API responses
 */
data class ReminderHistoryResponse(
    val id: String,
    val userId: String,
    val contactEmail: String,
    val subject: String,
    val body: String,
    val status: String,  // "sent", "failed", "skipped"
    val reason: String = "",
    val sentAt: LocalDateTime,
    val createdAt: LocalDateTime
)

/**
 * Reminder History summary for list view
 */
data class ReminderHistoryItemResponse(
    val id: String,
    val contactEmail: String,
    val subject: String,
    val status: String,  // "sent" (green), "failed" (red), "skipped" (gray)
    val reason: String = "",
    val sentAt: LocalDateTime,
    val bodyPreview: String  // First 100 chars of body
)

/**
 * Extension function to convert entity to DTO
 */
fun com.openmmo.ai.entity.ReminderHistory.toResponse(): ReminderHistoryResponse {
    return ReminderHistoryResponse(
        id = this.id ?: "",
        userId = this.userId,
        contactEmail = this.contactEmail,
        subject = this.subject,
        body = this.body,
        status = this.status,
        reason = this.reason,
        sentAt = this.sentAt,
        createdAt = this.createdAt
    )
}

/**
 * Extension function to convert entity to item response (for list)
 */
fun com.openmmo.ai.entity.ReminderHistory.toItemResponse(): ReminderHistoryItemResponse {
    return ReminderHistoryItemResponse(
        id = this.id ?: "",
        contactEmail = this.contactEmail,
        subject = this.subject,
        status = this.status,
        reason = this.reason,
        sentAt = this.sentAt,
        bodyPreview = this.body.take(100).let { if (it.length >= 100) "$it..." else it }
    )
}

