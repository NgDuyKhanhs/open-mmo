package com.openmmo.ai.service

/**
 * Gmail Auto-Reply Service Interface
 * Handles automatic email reply generation and sending
 */
interface IGmailAutoReplyService {
    fun autoReplyEmails(): Map<String, Any>
    fun autoReplyForUser(userId: String): Int
}

