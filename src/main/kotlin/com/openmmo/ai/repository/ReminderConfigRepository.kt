package com.openmmo.ai.repository

import com.openmmo.ai.entity.ReminderConfig
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ReminderConfigRepository : MongoRepository<ReminderConfig, String> {
    fun findByUserId(userId: String): List<ReminderConfig>

    fun findByUserIdAndContactEmail(userId: String, contactEmail: String): ReminderConfig?

    fun findByEnabledTrue(): List<ReminderConfig>

    fun deleteByUserIdAndContactEmail(userId: String, contactEmail: String)
}

