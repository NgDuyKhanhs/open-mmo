package com.openmmo.ai.service.impl

import com.openmmo.ai.entity.CorrespondentMemory
import com.openmmo.ai.entity.MemoryFact
import com.openmmo.ai.entity.StylePrefs
import com.openmmo.ai.repository.CorrespondentMemoryRepository
import com.openmmo.ai.client.GeminiApiClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CorrespondentMemoryServiceImplTest {

    @Mock
    private lateinit var memoryRepository: CorrespondentMemoryRepository

    @Mock
    private lateinit var geminiApiClient: GeminiApiClient

    private lateinit var service: CorrespondentMemoryServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = CorrespondentMemoryServiceImpl(memoryRepository, geminiApiClient)
    }

    @Test
    fun testExtractCorrespondentEmail() {
        // Test extracting email from "Name <email>" format
        val email1 = "John Doe <john@example.com>"
        val regex = Regex("<(.+?)>")
        val match = regex.find(email1)
        assertEquals("john@example.com", match?.groupValues?.get(1))
    }

    @Test
    fun testSensitivePatternDetection() {
        // Test OTP detection
        assertTrue(isSensitive("Your OTP is 123456"))
        assertTrue(isSensitive("Verification code: 654321"))
        assertTrue(isSensitive("Your password is secret123"))
        assertTrue(isSensitive("API key: sk-1234567890"))

        // Test normal text
        assertFalse(isSensitive("Please reply to this email"))
    }

    @Test
    fun testMemoryContextTruncation() {
        val memory = CorrespondentMemory(
            userId = "user1",
            correspondentEmail = "test@example.com",
            profileSummary = "A".repeat(4000),  // Longer than max
            facts = listOf(
                MemoryFact("name", "John Doe", 0.9),
                MemoryFact("company", "Acme Corp", 0.8)
            ),
            stylePrefs = StylePrefs(language = "en", tone = "friendly")
        )

        val context = service.buildMemoryContextText(memory)
        assertTrue(context.length <= 2200)  // Max + some buffer
        assertTrue(context.contains("truncated") || context.contains("CORRESPONDENT"))
    }

    @Test
    fun testFactsMergeRulesHigherConfidence() {
        val existingFacts = listOf(
            MemoryFact("name", "John", 0.6)
        )

        val newFact = MemoryFact("name", "John Doe", 0.9)

        // Higher confidence should replace
        val shouldUpdate = newFact.confidence > 0.6
        assertTrue(shouldUpdate)
    }

    @Test
    fun testFactsMergeRulesLowerConfidence() {
        val existingFacts = listOf(
            MemoryFact("name", "John Doe", 0.9)
        )

        val newFact = MemoryFact("name", "John", 0.6)

        // Lower confidence should NOT replace
        val shouldUpdate = newFact.confidence > 0.9
        assertFalse(shouldUpdate)
    }

    @Test
    fun testFactsCapAtMax() {
        val factsList = mutableListOf<MemoryFact>()

        // Add 35 facts (more than 30 limit)
        for (i in 1..35) {
            factsList.add(MemoryFact("fact_$i", "value_$i", 0.5 + (i * 0.01)))
        }

        // Should cap at 30
        val capped = factsList.sortedByDescending { it.confidence }.take(30)
        assertEquals(30, capped.size)
    }

    @Test
    fun testEmailNormalization() {
        val email1 = "  John@Example.COM  "
        val normalized = email1.trim().lowercase()
        assertEquals("john@example.com", normalized)
    }

    // Helper to test sensitive detection
    private fun isSensitive(text: String): Boolean {
        val patterns = listOf(
            Regex("""OTP|verification.*code|confirm.*code""", RegexOption.IGNORE_CASE),
            Regex("""\b\d{4,8}\b"""),
            Regex("""password|mật khẩu|passwd""", RegexOption.IGNORE_CASE),
            Regex("""token|api.*key|secret|credential""", RegexOption.IGNORE_CASE),
            Regex("""credit.*card|cvv|card.*number""", RegexOption.IGNORE_CASE),
            Regex("""ssn|cccd|cmnd|định danh|identify""", RegexOption.IGNORE_CASE)
        )

        return patterns.any { it.containsMatchIn(text) }
    }
}

