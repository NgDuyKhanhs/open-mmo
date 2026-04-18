package com.openmmo.ai.service

/**
 * Service for detecting language from email conversations
 * Uses the main emails to determine the language of communication
 */
interface ILanguageDetectionService {
    /**
     * Detect language from 2 main emails
     *
     * @param email1 First email body text
     * @param email2 Second email body text
     * @return Language code (e.g., "en", "vi", "ja", etc.) or null if detection fails
     */
    fun detectLanguageFromEmails(email1: String, email2: String): String?

    /**
     * Detect language from a single text
     *
     * @param text Text to analyze
     * @return Language code (e.g., "en", "vi", "ja", etc.) or null if detection fails
     */
    fun detectLanguage(text: String): String?
}

