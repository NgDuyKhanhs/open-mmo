package com.openmmo.ai.service

/**
 * Service for AI-powered personalized reminder message generation
 * Uses user's selected AI provider (Groq or Gemini) for message generation
 */
interface IAIReminderService {
    /**
     * Generate personalized reminder message based on conversation history
     *
     * @param contactEmail The email address of the contact
     * @param profileSummary The memory/conversation history with this contact
     * @param aiProvider The AI provider to use: "groq" (default) or "gemini"
     * @return Generated personalized reminder message
     */
    fun generateReminderMessage(
        contactEmail: String,
        profileSummary: String,
        aiProvider: String = "groq"
    ): String
}


