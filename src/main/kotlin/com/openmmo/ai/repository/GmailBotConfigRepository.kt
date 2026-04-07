package com.openmmo.ai.repository

import com.openmmo.ai.entity.GmailBotConfig
import org.springframework.data.mongodb.repository.MongoRepository

interface GmailBotConfigRepository : MongoRepository<GmailBotConfig, String> {
    fun findByUserId(userId: String): GmailBotConfig?
}

