package com.openmmo.ai.repository

import com.openmmo.ai.entity.GmailProcessedMessage
import org.springframework.data.mongodb.repository.MongoRepository

interface GmailProcessedMessageRepository : MongoRepository<GmailProcessedMessage, String> {
    fun findByUserIdAndMessageId(userId: String, messageId: String): GmailProcessedMessage?
}

