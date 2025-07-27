package com.example.reciplan.ui.common

import com.example.reciplan.data.model.IngestErrorCode
import org.junit.Test
import org.junit.Assert.*

/**
 * Task 3: Unit tests for ErrorMapper
 * Tests error message mapping and retry eligibility for all error codes
 */
class ErrorMapperTest {

    @Test
    fun `getErrorMessage returns correct message for VIDEO_UNAVAILABLE`() {
        val message = ErrorMapper.getErrorMessage(IngestErrorCode.VIDEO_UNAVAILABLE)
        assertTrue("Message should mention video unavailability", 
            message.contains("no longer available", ignoreCase = true))
    }

    @Test
    fun `getErrorMessage returns correct message for ASR_FAILED`() {
        val message = ErrorMapper.getErrorMessage(IngestErrorCode.ASR_FAILED)
        assertTrue("Message should mention transcription/audio issues", 
            message.contains("transcribe", ignoreCase = true) || 
            message.contains("audio", ignoreCase = true))
    }

    @Test
    fun `getErrorMessage returns correct message for OCR_FAILED`() {
        val message = ErrorMapper.getErrorMessage(IngestErrorCode.OCR_FAILED)
        assertTrue("Message should mention text reading issues", 
            message.contains("read", ignoreCase = true) || 
            message.contains("text", ignoreCase = true))
    }

    @Test
    fun `getErrorMessage returns correct message for LLM_FAILED`() {
        val message = ErrorMapper.getErrorMessage(IngestErrorCode.LLM_FAILED)
        assertTrue("Message should mention AI processing issues", 
            message.contains("AI", ignoreCase = true) || 
            message.contains("process", ignoreCase = true))
    }

    @Test
    fun `getErrorMessage returns correct message for PERSIST_FAILED`() {
        val message = ErrorMapper.getErrorMessage(IngestErrorCode.PERSIST_FAILED)
        assertTrue("Message should mention save issues", 
            message.contains("save", ignoreCase = true))
    }

    @Test
    fun `getErrorMessage returns correct message for UNKNOWN_ERROR`() {
        val message = ErrorMapper.getErrorMessage(IngestErrorCode.UNKNOWN_ERROR)
        assertTrue("Message should mention general error", 
            message.contains("went wrong", ignoreCase = true))
    }

    @Test
    fun `isRecoverable returns false for VIDEO_UNAVAILABLE`() {
        assertFalse("VIDEO_UNAVAILABLE should not be recoverable", 
            ErrorMapper.isRecoverable(IngestErrorCode.VIDEO_UNAVAILABLE))
    }

    @Test
    fun `isRecoverable returns true for ASR_FAILED`() {
        assertTrue("ASR_FAILED should be recoverable", 
            ErrorMapper.isRecoverable(IngestErrorCode.ASR_FAILED))
    }

    @Test
    fun `isRecoverable returns true for OCR_FAILED`() {
        assertTrue("OCR_FAILED should be recoverable", 
            ErrorMapper.isRecoverable(IngestErrorCode.OCR_FAILED))
    }

    @Test
    fun `isRecoverable returns true for LLM_FAILED`() {
        assertTrue("LLM_FAILED should be recoverable", 
            ErrorMapper.isRecoverable(IngestErrorCode.LLM_FAILED))
    }

    @Test
    fun `isRecoverable returns true for PERSIST_FAILED`() {
        assertTrue("PERSIST_FAILED should be recoverable", 
            ErrorMapper.isRecoverable(IngestErrorCode.PERSIST_FAILED))
    }

    @Test
    fun `isRecoverable returns true for UNKNOWN_ERROR`() {
        assertTrue("UNKNOWN_ERROR should be recoverable", 
            ErrorMapper.isRecoverable(IngestErrorCode.UNKNOWN_ERROR))
    }

    @Test
    fun `getRetryActionText returns null for non-recoverable errors`() {
        val retryText = ErrorMapper.getRetryActionText(IngestErrorCode.VIDEO_UNAVAILABLE)
        assertNull("Non-recoverable errors should not have retry text", retryText)
    }

    @Test
    fun `getRetryActionText returns appropriate text for recoverable errors`() {
        val asrRetryText = ErrorMapper.getRetryActionText(IngestErrorCode.ASR_FAILED)
        assertNotNull("ASR_FAILED should have retry text", asrRetryText)
        assertTrue("ASR retry text should mention audio", 
            asrRetryText!!.contains("Audio", ignoreCase = true))

        val ocrRetryText = ErrorMapper.getRetryActionText(IngestErrorCode.OCR_FAILED)
        assertNotNull("OCR_FAILED should have retry text", ocrRetryText)
        assertTrue("OCR retry text should mention text", 
            ocrRetryText!!.contains("Text", ignoreCase = true))

        val llmRetryText = ErrorMapper.getRetryActionText(IngestErrorCode.LLM_FAILED)
        assertNotNull("LLM_FAILED should have retry text", llmRetryText)
        assertTrue("LLM retry text should mention AI", 
            llmRetryText!!.contains("AI", ignoreCase = true))

        val persistRetryText = ErrorMapper.getRetryActionText(IngestErrorCode.PERSIST_FAILED)
        assertNotNull("PERSIST_FAILED should have retry text", persistRetryText)
        assertTrue("Persist retry text should mention save", 
            persistRetryText!!.contains("Save", ignoreCase = true))

        val unknownRetryText = ErrorMapper.getRetryActionText(IngestErrorCode.UNKNOWN_ERROR)
        assertNotNull("UNKNOWN_ERROR should have retry text", unknownRetryText)
        assertTrue("Unknown retry text should be generic", 
            unknownRetryText!!.contains("Again", ignoreCase = true))
    }

    @Test
    fun `getErrorSummary returns concise summaries for all error codes`() {
        val videoSummary = ErrorMapper.getErrorSummary(IngestErrorCode.VIDEO_UNAVAILABLE)
        assertTrue("Video summary should be concise", videoSummary.length < 30)
        assertTrue("Video summary should mention unavailable", 
            videoSummary.contains("unavailable", ignoreCase = true))

        val asrSummary = ErrorMapper.getErrorSummary(IngestErrorCode.ASR_FAILED)
        assertTrue("ASR summary should be concise", asrSummary.length < 30)
        assertTrue("ASR summary should mention audio", 
            asrSummary.contains("audio", ignoreCase = true))

        val ocrSummary = ErrorMapper.getErrorSummary(IngestErrorCode.OCR_FAILED)
        assertTrue("OCR summary should be concise", ocrSummary.length < 30)
        assertTrue("OCR summary should mention text", 
            ocrSummary.contains("text", ignoreCase = true))

        val llmSummary = ErrorMapper.getErrorSummary(IngestErrorCode.LLM_FAILED)
        assertTrue("LLM summary should be concise", llmSummary.length < 30)
        assertTrue("LLM summary should mention AI", 
            llmSummary.contains("AI", ignoreCase = true))

        val persistSummary = ErrorMapper.getErrorSummary(IngestErrorCode.PERSIST_FAILED)
        assertTrue("Persist summary should be concise", persistSummary.length < 30)
        assertTrue("Persist summary should mention save", 
            persistSummary.contains("save", ignoreCase = true))

        val unknownSummary = ErrorMapper.getErrorSummary(IngestErrorCode.UNKNOWN_ERROR)
        assertTrue("Unknown summary should be concise", unknownSummary.length < 30)
        assertTrue("Unknown summary should mention processing", 
            unknownSummary.contains("processing", ignoreCase = true))
    }

    @Test
    fun `all error codes have non-empty messages`() {
        val errorCodes = IngestErrorCode.values()
        
        for (errorCode in errorCodes) {
            val message = ErrorMapper.getErrorMessage(errorCode)
            assertFalse("Error message should not be empty for $errorCode", 
                message.isBlank())
            
            val summary = ErrorMapper.getErrorSummary(errorCode)
            assertFalse("Error summary should not be empty for $errorCode", 
                summary.isBlank())
        }
    }
} 