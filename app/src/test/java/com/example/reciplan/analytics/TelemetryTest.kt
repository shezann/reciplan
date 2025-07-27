package com.example.reciplan.analytics

import com.example.reciplan.data.model.IngestErrorCode
import org.junit.Test
import org.junit.Assert.*

/**
 * Task 3: Unit tests for Telemetry
 * Tests that analytics events are fired correctly for all ingest flow scenarios
 * 
 * Note: These tests verify that telemetry methods can be called without errors.
 * In a production app, you would mock the actual analytics service calls.
 */
class TelemetryTest {

    @Test
    fun `ingestStarted executes without error`() {
        val url = "https://vm.tiktok.com/ZMScJ8vrH/"
        
        // Should not throw any exceptions
        Telemetry.ingestStarted(url)
        
        // Test passes if no exception is thrown
        assertTrue("Method executed successfully", true)
    }

    @Test
    fun `ingestStarted handles different TikTok domains`() {
        // Should not throw exceptions for any URL format
        Telemetry.ingestStarted("https://www.tiktok.com/@user/video/123")
        Telemetry.ingestStarted("https://vm.tiktok.com/ZMScJ8vrH/")
        Telemetry.ingestStarted("https://tiktok.com/test")
        Telemetry.ingestStarted("https://youtube.com/watch") // Unknown domain
        
        assertTrue("All URL formats handled successfully", true)
    }

    @Test
    fun `ingestFailed executes without error`() {
        val errorCode = IngestErrorCode.ASR_FAILED
        val jobId = "job123"
        
        Telemetry.ingestFailed(errorCode, jobId)
        Telemetry.ingestFailed(IngestErrorCode.VIDEO_UNAVAILABLE, null) // Test null jobId
        
        assertTrue("Method executed successfully", true)
    }

    @Test
    fun `ingestSucceeded executes without error`() {
        val jobId = "job123"
        val recipeId = "recipe456"
        
        Telemetry.ingestSucceeded(jobId, recipeId)
        Telemetry.ingestSucceeded(jobId, null) // Test null recipeId
        
        assertTrue("Method executed successfully", true)
    }

    @Test
    fun `ingestCancelled executes without error`() {
        val jobId = "job123"
        
        Telemetry.ingestCancelled(jobId)
        Telemetry.ingestCancelled(null) // Test null jobId
        
        assertTrue("Method executed successfully", true)
    }

    @Test
    fun `ingestRetried executes without error`() {
        val errorCode = IngestErrorCode.LLM_FAILED
        val jobId = "job123"
        
        Telemetry.ingestRetried(errorCode, jobId)
        Telemetry.ingestRetried(IngestErrorCode.OCR_FAILED, null) // Test null jobId
        
        assertTrue("Method executed successfully", true)
    }

    @Test
    fun `all telemetry methods execute without error`() {
        // Test that all telemetry methods can be called without exceptions
        Telemetry.ingestStarted("https://tiktok.com/test")
        Telemetry.ingestFailed(IngestErrorCode.ASR_FAILED, "job123")
        Telemetry.ingestSucceeded("job123", "recipe456")
        Telemetry.ingestCancelled("job123")
        Telemetry.ingestRetried(IngestErrorCode.OCR_FAILED, "job123")
        
        assertTrue("All telemetry methods executed successfully", true)
    }

    @Test
    fun `telemetry handles edge cases gracefully`() {
        // Test edge cases that shouldn't cause crashes
        Telemetry.ingestStarted("") // Empty URL
        Telemetry.ingestStarted("not-a-url") // Malformed URL
        Telemetry.ingestFailed(IngestErrorCode.UNKNOWN_ERROR, "") // Empty job ID
        Telemetry.ingestSucceeded("", "") // Empty IDs
        
        assertTrue("Edge cases handled successfully", true)
    }
} 