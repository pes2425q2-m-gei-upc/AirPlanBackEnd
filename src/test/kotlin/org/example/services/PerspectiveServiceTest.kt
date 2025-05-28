package org.example.services

import kotlinx.coroutines.runBlocking
import org.example.services.PerspectiveService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PerspectiveServiceTest {
    private lateinit var perspectiveService: PerspectiveService

    @BeforeEach
    fun setUp() {
        // Instantiate service; if secret key is empty, methods should return false without HTTP calls
        perspectiveService = PerspectiveService()
    }

    @Test
    @DisplayName("analyzeMessage returns false for any text when API key is empty or service disabled")
    fun testAnalyzeMessageReturnsFalse() = runBlocking {
        val result = perspectiveService.analyzeMessage("test text")
        assertFalse(result, "Expected analyzeMessage to return false without valid API key or enabled settings")
    }

    @Test
    @DisplayName("analyzeMessages returns list of false for multiple texts when API key is empty or service disabled")
    fun testAnalyzeMessagesReturnsFalseList() = runBlocking {
        val texts = listOf("one", "two", "three")
        val results = perspectiveService.analyzeMessages(texts)
        assertEquals(texts.size, results.size, "Result list size should match input list size")
        assertTrue(results.all { it == false }, "All results should be false without valid API key or enabled settings")
    }

    @Test
    @DisplayName("analyzeMessages returns empty list for empty input")
    fun testAnalyzeMessagesEmptyInput() = runBlocking {
        val results = perspectiveService.analyzeMessages(emptyList())
        assertTrue(results.isEmpty(), "Empty input should produce empty result list")
    }
}
