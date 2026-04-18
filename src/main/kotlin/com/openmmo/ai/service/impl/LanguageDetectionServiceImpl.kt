package com.openmmo.ai.service.impl

import com.openmmo.ai.service.ILanguageDetectionService
import com.pemistahl.lingua.api.Language
import com.pemistahl.lingua.api.LanguageDetector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Language Detection Service Implementation
 *
 * Uses Lingua library to detect language from email text
 * Supports 75 languages with high accuracy
 */
@Service
class LanguageDetectionServiceImpl : ILanguageDetectionService {

    companion object {
        private val logger = LoggerFactory.getLogger(LanguageDetectionServiceImpl::class.java)
        private val detector: LanguageDetector = LanguageDetector.builder()
            .withLanguages(Language.values().toList())
            .withPreloadedLanguageModels()
            .build()
    }

    /**
     * Detect language from 2 main emails
     * Combines both emails for better accuracy
     */
    override fun detectLanguageFromEmails(email1: String, email2: String): String? {
        return try {
            // Combine both emails with a separator
            val combinedText = "$email1 $email2"

            // Need minimum text for accurate detection
            if (combinedText.length < 30) {
                logger.debug("Combined email text too short for language detection (${combinedText.length} chars)")
                return null
            }

            val language = detector.detectLanguageOf(combinedText)
            val languageCode = language.isoCode639_1.toString()

            logger.debug("Detected language from 2 emails: $languageCode (${language.name})")
            languageCode
        } catch (e: Exception) {
            logger.warn("Failed to detect language from emails: ${e.message}")
            null
        }
    }

    /**
     * Detect language from a single text
     */
    override fun detectLanguage(text: String): String? {
        return try {
            if (text.length < 30) {
                logger.debug("Text too short for language detection (${text.length} chars)")
                return null
            }

            val language = detector.detectLanguageOf(text)
            val languageCode = language.isoCode639_1.toString()

            logger.debug("Detected language: $languageCode (${language.name})")
            languageCode
        } catch (e: Exception) {
            logger.warn("Failed to detect language: ${e.message}")
            null
        }
    }
}

