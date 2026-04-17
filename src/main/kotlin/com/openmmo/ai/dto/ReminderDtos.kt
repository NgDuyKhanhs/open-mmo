package com.openmmo.ai.dto

import com.openmmo.ai.entity.ReminderConfig

data class ReminderConfigDto(
    val contactEmail: String,
    val enabled: Boolean = true,
    val afterInactive: Int,
    val repeatEvery: Int = 1440,    // minutes between repeats (default 24h)
    val maxReminders: Int = 5
) {
    fun toEntity(userId: String, contactEmail: String = this.contactEmail) = ReminderConfig(
        userId = userId,
        contactEmail = contactEmail,
        enabled = enabled,
        afterInactive = afterInactive,
        repeatEvery = repeatEvery,
        maxReminders = maxReminders
    )
}

data class ReminderResponse(
    val contactEmail: String,
    val enabled: Boolean,
    val afterInactive: Int,
    val repeatEvery: Int,           // minutes between repeats
    val maxReminders: Int,
    val sentCount: Int = 0,
    val lastSentAt: String? = null
)

fun ReminderConfig.toResponse() = ReminderResponse(
    // Convert "ALL_CONTACTS" marker back to empty string for frontend
    contactEmail = if (contactEmail == "ALL_CONTACTS") "" else contactEmail,
    enabled = enabled,
    afterInactive = afterInactive,
    repeatEvery = repeatEvery,
    maxReminders = maxReminders,
    sentCount = sentCount,
    lastSentAt = lastSentAt?.toString()
)


