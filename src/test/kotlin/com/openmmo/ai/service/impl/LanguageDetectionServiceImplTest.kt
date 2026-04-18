package com.openmmo.ai.service.impl

import com.openmmo.ai.service.ILanguageDetectionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("Language Detection Service Tests")
class LanguageDetectionServiceImplTest {

    private lateinit var languageDetectionService: ILanguageDetectionService

    @BeforeEach
    fun setUp() {
        languageDetectionService = LanguageDetectionServiceImpl()
    }

    @Test
    @DisplayName("Should detect English language")
    fun testDetectEnglish() {
        val englishText = "Hello, how are you doing today? I hope everything is going well with you."
        val language = languageDetectionService.detectLanguage(englishText)
        assertEquals("en", language)
    }

    @Test
    @DisplayName("Should detect Vietnamese language")
    fun testDetectVietnamese() {
        val vietnameseText = "Xin chào, bạn khỏe không? Tôi hy vọng mọi thứ đang diễn ra tốt đẹp với bạn."
        val language = languageDetectionService.detectLanguage(vietnameseText)
        assertEquals("vi", language)
    }

    @Test
    @DisplayName("Should detect Japanese language")
    fun testDetectJapanese() {
        val japaneseText = "こんにちは。お元気ですか？今日はいい天気ですね。"
        val language = languageDetectionService.detectLanguage(japaneseText)
        assertEquals("ja", language)
    }

    @Test
    @DisplayName("Should detect from combined emails")
    fun testDetectFromTwoEmails() {
        val email1 = "Hello, how are you today?"
        val email2 = "I hope everything is going well with you. Please let me know."
        val language = languageDetectionService.detectLanguageFromEmails(email1, email2)
        assertEquals("en", language)
    }

    @Test
    @DisplayName("Should return null for too short text")
    fun testShortTextReturnsNull() {
        val shortText = "Hi"
        val language = languageDetectionService.detectLanguage(shortText)
        assertNull(language)
    }

    @Test
    @DisplayName("Should detect language with sufficient context from two emails")
    fun testDetectVietnameseFromTwoEmails() {
        val email1 = "Xin chào, bạn khỏe không?"
        val email2 = "Tôi hy vọng mọi thứ đang diễn ra tốt đẹp."
        val language = languageDetectionService.detectLanguageFromEmails(email1, email2)
        assertEquals("vi", language)
    }
}

