package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "gmail_connections")
data class GmailConnection(
    @Id
    val id: String? = null,
    val userId: String,
    val gmailAddress: String,
    val refreshTokenEnc: String, // AES-GCM encrypted
    val scopes: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

