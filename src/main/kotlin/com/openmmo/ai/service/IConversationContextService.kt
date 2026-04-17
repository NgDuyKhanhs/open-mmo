package com.openmmo.ai.service

/**
 * Service for fetching and building conversation context from Gmail API
 * Optimized for reminder generation with caching and minimal API calls
 */
interface IConversationContextService {
    /**
     * Get conversation context for a specific contact
     * Fetches recent emails from thread and builds a truncated context string
     *
     * @param userId User ID
     * @param contactEmail Contact email address
     * @return Conversation context string (truncated), or null if no recent conversation found
     *         Falls back to empty string if fetch fails
     */
    fun getContextForReminder(userId: String, contactEmail: String): String?
}

