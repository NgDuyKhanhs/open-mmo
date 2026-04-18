package com.openmmo.ai.service.impl

import com.openmmo.ai.service.ILanguageDetectionService
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Language Detection Service Implementation
 * Uses the Lingua library for highly accurate language detection
 * Supports 75+ languages
 */
@Service
class LanguageDetectionServiceImpl : ILanguageDetectionService {

    companion object {
        private val logger = LoggerFactory.getLogger(LanguageDetectionServiceImpl::class.java)

        // Initialize Lingua detector once (thread-safe, reusable)
        private val detector = LanguageDetectorBuilder.fromAllLanguages()
            .build()
    }

    override fun detectLanguageFromEmails(email1: String, email2: String): String? {
        return try {
            val combinedText = "$email1 $email2"
            if (combinedText.isBlank()) {
                logger.debug("Combined email text is blank, defaulting to English")
                return "en"
            }

            val detected = detectLanguageInternal(combinedText)
            logger.debug("Detected language from 2 emails: $detected (combined length: ${combinedText.length} chars)")
            detected
        } catch (e: Exception) {
            logger.warn("Failed to detect language from emails: ${e.message}")
            "en"  // Fallback to English on error
        }
    }

    override fun detectLanguage(text: String): String? {
        return try {
            if (text.isBlank()) {
                logger.debug("Text is blank, defaulting to English")
                return "en"
            }

            val detected = detectLanguageInternal(text)
            logger.debug("Detected language: $detected (text length: ${text.length} chars)")
            detected
        } catch (e: Exception) {
            logger.warn("Failed to detect language: ${e.message}")
            "en"  // Fallback to English on error
        }
    }

    /**
     * Internal language detection using Lingua library
     * Returns ISO 639-1 language code
     */
    private fun detectLanguageInternal(text: String): String {
        if (text.isBlank()) return "en"

        return try {
            // Lingua detects language and returns Language enum
            val language = detector.detectLanguageOf(text)

            // Convert Language enum to language code (e.g., VIETNAMESE -> "vi")
            languageToCode(language)
        } catch (e: Exception) {
            logger.warn("Failed to detect language using Lingua: ${e.message}, defaulting to English")
            "en"
        }
    }

    /**
     * Convert Lingua Language enum to ISO 639-1 language code
     */
    private fun languageToCode(language: Language): String {
        return when (language) {
            Language.VIETNAMESE -> "vi"
            Language.ENGLISH -> "en"
            Language.CHINESE -> "zh"
            Language.JAPANESE -> "ja"
            Language.KOREAN -> "ko"
            Language.THAI -> "th"
            Language.RUSSIAN -> "ru"
            Language.FRENCH -> "fr"
            Language.GERMAN -> "de"
            Language.SPANISH -> "es"
            Language.PORTUGUESE -> "pt"
            Language.HINDI -> "hi"
            Language.INDONESIAN -> "id"
            Language.TAGALOG -> "tl"
            Language.ARABIC -> "ar"
            Language.TURKISH -> "tr"
            Language.POLISH -> "pl"
            Language.ITALIAN -> "it"
            Language.DUTCH -> "nl"
            Language.FINNISH -> "fi"
            Language.SWEDISH -> "sv"
            Language.DANISH -> "da"
            else -> {
                // For other languages, use the language's ISO 639-1 code
                logger.warn("Language code mapping not defined for: $language, defaulting to en")
                "en"
            }
        }
    }
}

