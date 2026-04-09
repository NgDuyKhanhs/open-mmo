package com.openmmo.ai.service

import com.openmmo.ai.entity.CorrespondentMemory

interface ICorrespondentMemoryService {

    /**
     * Get or create memory for a correspondent
     * @param userId User ID
     * @param correspondentEmail Correspondent email (will be normalized)
     * @return CorrespondentMemory instance
     */
    fun getOrCreate(userId: String, correspondentEmail: String): CorrespondentMemory

    /**
     * Build memory context text for prompt injection
     * @param memory CorrespondentMemory instance
     * @return Formatted text suitable for prompt
     */
    fun buildMemoryContextText(memory: CorrespondentMemory): String

    /**
     * Update memory after reply is sent
     * Uses Gemini to extract and update facts, summary, style preferences
     * @param userId User ID
     * @param correspondentEmail Correspondent email
     * @param threadId Gmail thread ID
     * @param messageId Gmail message ID
     * @param emailSubject Original email subject
     * @param emailBody Original email body
     * @param replyBody Reply body we sent
     * @return Updated CorrespondentMemory
     */
    fun updateAfterReply(
        userId: String,
        correspondentEmail: String,
        threadId: String,
        messageId: String,
        emailSubject: String,
        emailBody: String,
        replyBody: String
    ): CorrespondentMemory

    /**
     * Delete memory for one correspondent
     */
    fun forgetCorrespondent(userId: String, correspondentEmail: String)

    /**
     * Delete all memories for user
     */
    fun forgetAll(userId: String)
}

