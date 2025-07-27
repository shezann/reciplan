package com.example.reciplan.analytics

import com.example.reciplan.data.model.IngestErrorCode
import android.util.Log

/**
 * Task 3: Telemetry for ingest flow analytics
 * Emits analytics events for tracking user behavior and error patterns
 */
object Telemetry {
    
    private const val TAG = "Telemetry"
    
    /**
     * Event fired when user starts an ingest job
     */
    fun ingestStarted(url: String) {
        val event = mapOf(
            "event" to "ingest_started",
            "url_domain" to extractDomain(url),
            "timestamp" to System.currentTimeMillis()
        )
        
        logEvent("ingest_started", event)
        // TODO: Send to actual analytics service (Firebase, Mixpanel, etc.)
    }
    
    /**
     * Event fired when ingest job fails with error code
     */
    fun ingestFailed(errorCode: IngestErrorCode, jobId: String? = null) {
        val event = mapOf(
            "event" to "ingest_failed",
            "error_code" to errorCode.name,
            "job_id" to jobId,
            "timestamp" to System.currentTimeMillis()
        )
        
        logEvent("ingest_failed", event)
        // TODO: Send to actual analytics service
    }
    
    /**
     * Event fired when ingest job completes successfully
     */
    fun ingestSucceeded(jobId: String, recipeId: String? = null) {
        val event = mapOf(
            "event" to "ingest_succeeded",
            "job_id" to jobId,
            "recipe_id" to recipeId,
            "timestamp" to System.currentTimeMillis()
        )
        
        logEvent("ingest_succeeded", event)
        // TODO: Send to actual analytics service
    }
    
    /**
     * Event fired when user cancels an ingest job
     */
    fun ingestCancelled(jobId: String? = null) {
        val event = mapOf(
            "event" to "ingest_cancelled",
            "job_id" to jobId,
            "timestamp" to System.currentTimeMillis()
        )
        
        logEvent("ingest_cancelled", event)
        // TODO: Send to actual analytics service
    }
    
    /**
     * Event fired when user retries a failed ingest
     */
    fun ingestRetried(errorCode: IngestErrorCode, jobId: String? = null) {
        val event = mapOf(
            "event" to "ingest_retried",
            "original_error_code" to errorCode.name,
            "job_id" to jobId,
            "timestamp" to System.currentTimeMillis()
        )
        
        logEvent("ingest_retried", event)
        // TODO: Send to actual analytics service
    }
    
    /**
     * Extract domain from URL for privacy-safe analytics
     */
    private fun extractDomain(url: String): String {
        return try {
            when {
                url.contains("vm.tiktok.com") -> "vm.tiktok.com"
                url.contains("www.tiktok.com") -> "www.tiktok.com"
                url.contains("tiktok.com") -> "tiktok.com"
                else -> "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Log event for debugging and development
     */
    private fun logEvent(eventName: String, properties: Map<String, Any?>) {
        try {
            Log.d(TAG, "Analytics Event: $eventName")
            properties.forEach { (key, value) ->
                Log.d(TAG, "  $key: $value")
            }
        } catch (e: Exception) {
            // Ignore logging errors in tests
            println("Analytics Event: $eventName")
            properties.forEach { (key, value) ->
                println("  $key: $value")
            }
        }
    }
} 