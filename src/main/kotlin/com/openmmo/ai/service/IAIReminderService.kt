package com.openmmo.ai.service

/**
 * Service for AI-powered personalized reminder message generation
 * Uses user's selected AI provider (Groq or Gemini) for message generation
 */
interface IAIReminderService {
    /**
     * Generate personalized reminder message based on conversation context or memory
     *
     * @param contactEmail The email address of the contact
     * @param conversationContext Recent conversation context from Gmail (preferred) or profile summary from DB (fallback)
     *                           Can be null/empty if no context available
     * @param aiProvider The AI provider to use: "groq" (default) or "gemini"
     * @return Generated personalized reminder message (email body only, no greeting/signature)
     */
    fun generateReminderMessage(
        contactEmail: String,
        conversationContext: String?,
        aiProvider: String = "groq"
    ): String
}




