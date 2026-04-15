package com.openmmo.ai.service

import com.openmmo.ai.dto.ReminderConfigDto
import com.openmmo.ai.dto.ReminderResponse

interface IReminderService {
    /**
     * Get all reminders for a user
     */
    fun getReminders(userId: String): List<ReminderResponse>

    /**
     * Get specific reminder
     */
    fun getReminder(userId: String, contactEmail: String): ReminderResponse?

    /**
     * Create or update reminder
     */
    fun saveReminder(userId: String, dto: ReminderConfigDto): Map<String, String>

    /**
     * Delete reminder
     */
    fun deleteReminder(userId: String, contactEmail: String): Map<String, String>

    /**
     * Process reminders - called by scheduler
     */
    fun processReminders()
}

