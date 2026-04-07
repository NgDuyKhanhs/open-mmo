package com.openmmo.ai.repository

import com.openmmo.ai.entity.GmailConnection
import org.springframework.data.mongodb.repository.MongoRepository

interface GmailConnectionRepository : MongoRepository<GmailConnection, String> {
    fun findByUserId(userId: String): GmailConnection?
}

