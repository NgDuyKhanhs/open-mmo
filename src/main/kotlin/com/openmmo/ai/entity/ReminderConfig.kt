package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "reminder_configs")
data class ReminderConfig(
    @Id
    val id: String? = null,
    val userId: String,
    val contactEmail: String,
    val enabled: Boolean = true,
    val afterInactive: Int,              // minutes before FIRST reminder
    val repeatEvery: Int = 1440,         // minutes between repeats (default: 24 hours)
    val maxReminders: Int = 5,            // maximum reminders to send
    val lastSentAt: LocalDateTime? = null,
    val sentCount: Int = 0,              // how many reminders already sent
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

