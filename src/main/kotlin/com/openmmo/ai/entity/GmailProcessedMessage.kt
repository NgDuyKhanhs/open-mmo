package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime

@Document(collection = "gmail_processed_messages")
data class GmailProcessedMessage(
    @Id
    val id: String? = null,
    val userId: String,
    val messageId: String,
    val threadId: String,
    val aiProvider: String? = null,
    @Indexed(expireAfterSeconds = 7776000) // 90 days TTL
    val processedAt: LocalDateTime = LocalDateTime.now()
)

