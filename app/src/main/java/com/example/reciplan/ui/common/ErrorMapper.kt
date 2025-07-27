package com.example.reciplan.ui.common

import com.example.reciplan.data.model.IngestErrorCode

/**
 * Task 2.6: Error mapper for ingest error codes
 * Maps backend error codes to friendly user messages and determines retry eligibility
 */
object ErrorMapper {
    
    /**
     * Get a friendly error message for an ingest error code
     */
    fun getErrorMessage(errorCode: IngestErrorCode): String {
        return when (errorCode) {
            IngestErrorCode.VIDEO_UNAVAILABLE -> 
                "This TikTok video is no longer available. It may have been deleted or made private."
            
            IngestErrorCode.ASR_FAILED -> 
                "We couldn't transcribe the audio from this video. The video might be too quiet or have background noise."
            
            IngestErrorCode.OCR_FAILED -> 
                "We couldn't read the text on screen. The video quality might be too low or the text too small."
            
            IngestErrorCode.LLM_FAILED -> 
                "Our AI couldn't process this video properly. This might be due to unclear content or processing issues."
            
            IngestErrorCode.PERSIST_FAILED -> 
                "We couldn't save your recipe. Please try again or check your internet connection."
            
            IngestErrorCode.UNKNOWN_ERROR -> 
                "Something went wrong while processing your video. Please try again."
        }
    }
    
    /**
     * Check if an error is recoverable (can be retried)
     */
    fun isRecoverable(errorCode: IngestErrorCode): Boolean {
        return when (errorCode) {
            IngestErrorCode.VIDEO_UNAVAILABLE -> false // Video is gone, can't retry
            IngestErrorCode.ASR_FAILED -> true // Audio processing might work on retry
            IngestErrorCode.OCR_FAILED -> true // OCR might work on retry
            IngestErrorCode.LLM_FAILED -> true // AI processing might work on retry
            IngestErrorCode.PERSIST_FAILED -> true // Database issues are usually temporary
            IngestErrorCode.UNKNOWN_ERROR -> true // Unknown errors might be temporary
        }
    }
    
    /**
     * Get retry action text for recoverable errors
     */
    fun getRetryActionText(errorCode: IngestErrorCode): String? {
        return if (isRecoverable(errorCode)) {
            when (errorCode) {
                IngestErrorCode.ASR_FAILED -> "Retry Audio Processing"
                IngestErrorCode.OCR_FAILED -> "Retry Text Recognition"
                IngestErrorCode.LLM_FAILED -> "Retry AI Processing"
                IngestErrorCode.PERSIST_FAILED -> "Retry Save"
                IngestErrorCode.UNKNOWN_ERROR -> "Try Again"
                else -> null
            }
        } else {
            null
        }
    }
    
    /**
     * Get a short error summary for UI display
     */
    fun getErrorSummary(errorCode: IngestErrorCode): String {
        return when (errorCode) {
            IngestErrorCode.VIDEO_UNAVAILABLE -> "Video unavailable"
            IngestErrorCode.ASR_FAILED -> "Audio processing failed"
            IngestErrorCode.OCR_FAILED -> "Text recognition failed"
            IngestErrorCode.LLM_FAILED -> "AI processing failed"
            IngestErrorCode.PERSIST_FAILED -> "Save failed"
            IngestErrorCode.UNKNOWN_ERROR -> "Processing failed"
        }
    }
} 