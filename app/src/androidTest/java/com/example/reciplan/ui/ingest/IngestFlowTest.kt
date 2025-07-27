package com.example.reciplan.ui.ingest

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.reciplan.MainActivity
import com.example.reciplan.data.model.IngestErrorCode
import com.example.reciplan.data.model.IngestStatus
import com.example.reciplan.di.AppContainer
import com.example.reciplan.ui.common.ErrorMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 2.7: Instrumentation flow test
 * End-to-end tests for the ingest flow including happy path and VIDEO_UNAVAILABLE error path
 */
@RunWith(AndroidJUnit4::class)
class IngestFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var appContainer: AppContainer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
        
        // Get the app container from the instrumentation context
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appContainer = (context.applicationContext as com.example.reciplan.ReciplanApplication).appContainer
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testHappyPathFlow() {
        // Mock successful TikTok oEmbed response
        val oEmbedResponse = """
            {
                "author_name": "test_user",
                "title": "Test Recipe Video",
                "duration": 120
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(oEmbedResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock successful start ingest response
        val startIngestResponse = """
            {
                "job_id": "job_123",
                "recipe_id": null,
                "status": "QUEUED",
                "message": "Job started successfully"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(startIngestResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock successful job polling responses
        val pollingResponses = listOf(
            createJobResponse("job_123", IngestStatus.DOWNLOADING),
            createJobResponse("job_123", IngestStatus.EXTRACTING),
            createJobResponse("job_123", IngestStatus.TRANSCRIBING),
            createJobResponse("job_123", IngestStatus.DRAFT_TRANSCRIBED),
            createJobResponse("job_123", IngestStatus.OCRING),
            createJobResponse("job_123", IngestStatus.OCR_DONE),
            createJobResponse("job_123", IngestStatus.LLM_REFINING),
            createJobResponse("job_123", IngestStatus.DRAFT_PARSED),
            createJobResponse("job_123", IngestStatus.COMPLETED, recipeId = "recipe_456")
        )
        
        pollingResponses.forEach { response ->
            mockWebServer.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(response)
                .addHeader("Content-Type", "application/json"))
        }

        // Navigate to AddFromTikTok screen
        composeTestRule.onNodeWithText("Add from TikTok").performClick()
        
        // Enter TikTok URL
        val urlInput = composeTestRule.onNodeWithText("TikTok URL")
        urlInput.performTextInput("https://www.tiktok.com/@test_user/video/123456789")
        
        // Click Create Draft button
        composeTestRule.onNodeWithText("Create Draft").performClick()
        
        // Verify navigation to status screen
        composeTestRule.onNodeWithText("Processing Recipe").assertIsDisplayed()
        
        // Verify job ID is displayed
        composeTestRule.onNodeWithText("Job ID: job_123").assertIsDisplayed()
        
        // Verify stepper is shown
        composeTestRule.onNodeWithText("Processing Steps").assertIsDisplayed()
        
        // Verify status progression through the pipeline
        composeTestRule.onNodeWithText("Downloading").assertIsDisplayed()
        composeTestRule.onNodeWithText("Extracting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transcribing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Draft Transcribed").assertIsDisplayed()
        composeTestRule.onNodeWithText("OCR Processing").assertIsDisplayed()
        composeTestRule.onNodeWithText("OCR Complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Refining").assertIsDisplayed()
        composeTestRule.onNodeWithText("Draft Parsed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Completed").assertIsDisplayed()
        
        // Verify completion and navigation to draft preview
        composeTestRule.onNodeWithText("Completed").assertIsDisplayed()
    }

    @Test
    fun testVideoUnavailableErrorFlow() {
        // Mock successful TikTok oEmbed response
        val oEmbedResponse = """
            {
                "author_name": "test_user",
                "title": "Test Recipe Video",
                "duration": 120
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(oEmbedResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock successful start ingest response
        val startIngestResponse = """
            {
                "job_id": "job_error_123",
                "recipe_id": null,
                "status": "QUEUED",
                "message": "Job started successfully"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(startIngestResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock failed job response with VIDEO_UNAVAILABLE error
        val failedJobResponse = createFailedJobResponse(
            "job_error_123", 
            IngestErrorCode.VIDEO_UNAVAILABLE,
            "Video is no longer available"
        )
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(failedJobResponse)
            .addHeader("Content-Type", "application/json"))

        // Navigate to AddFromTikTok screen
        composeTestRule.onNodeWithText("Add from TikTok").performClick()
        
        // Enter TikTok URL
        val urlInput = composeTestRule.onNodeWithText("TikTok URL")
        urlInput.performTextInput("https://www.tiktok.com/@test_user/video/123456789")
        
        // Click Create Draft button
        composeTestRule.onNodeWithText("Create Draft").performClick()
        
        // Verify navigation to status screen
        composeTestRule.onNodeWithText("Processing Recipe").assertIsDisplayed()
        
        // Verify error message is displayed
        val errorMessage = ErrorMapper.getErrorMessage(IngestErrorCode.VIDEO_UNAVAILABLE)
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        
        // Verify no retry button is shown (VIDEO_UNAVAILABLE is not recoverable)
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
        
        // Verify error summary is displayed
        composeTestRule.onNodeWithText("Video unavailable").assertIsDisplayed()
    }

    @Test
    fun testRecoverableErrorFlow() {
        // Mock successful TikTok oEmbed response
        val oEmbedResponse = """
            {
                "author_name": "test_user",
                "title": "Test Recipe Video",
                "duration": 120
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(oEmbedResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock successful start ingest response
        val startIngestResponse = """
            {
                "job_id": "job_recoverable_123",
                "recipe_id": null,
                "status": "QUEUED",
                "message": "Job started successfully"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(startIngestResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock failed job response with ASR_FAILED error (recoverable)
        val failedJobResponse = createFailedJobResponse(
            "job_recoverable_123", 
            IngestErrorCode.ASR_FAILED,
            "Audio processing failed"
        )
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(failedJobResponse)
            .addHeader("Content-Type", "application/json"))

        // Navigate to AddFromTikTok screen
        composeTestRule.onNodeWithText("Add from TikTok").performClick()
        
        // Enter TikTok URL
        val urlInput = composeTestRule.onNodeWithText("TikTok URL")
        urlInput.performTextInput("https://www.tiktok.com/@test_user/video/123456789")
        
        // Click Create Draft button
        composeTestRule.onNodeWithText("Create Draft").performClick()
        
        // Verify navigation to status screen
        composeTestRule.onNodeWithText("Processing Recipe").assertIsDisplayed()
        
        // Verify error message is displayed
        val errorMessage = ErrorMapper.getErrorMessage(IngestErrorCode.ASR_FAILED)
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        
        // Verify retry button is shown (ASR_FAILED is recoverable)
        val retryText = ErrorMapper.getRetryActionText(IngestErrorCode.ASR_FAILED)
        composeTestRule.onNodeWithText(retryText!!).assertIsDisplayed()
        
        // Click retry button
        composeTestRule.onNodeWithText(retryText).performClick()
        
        // Verify retry action is triggered (loading state)
        composeTestRule.onNodeWithText("Processing Recipe").assertIsDisplayed()
    }

    @Test
    fun testDurationValidationFlow() {
        // Mock TikTok oEmbed response with video longer than 10 minutes
        val oEmbedResponse = """
            {
                "author_name": "test_user",
                "title": "Long Recipe Video",
                "duration": 900
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(oEmbedResponse)
            .addHeader("Content-Type", "application/json"))

        // Navigate to AddFromTikTok screen
        composeTestRule.onNodeWithText("Add from TikTok").performClick()
        
        // Enter TikTok URL
        val urlInput = composeTestRule.onNodeWithText("TikTok URL")
        urlInput.performTextInput("https://www.tiktok.com/@test_user/video/long_video")
        
        // Verify duration validation error is displayed
        composeTestRule.onNodeWithText("Video is too long").assertIsDisplayed()
        
        // Verify Create Draft button is disabled
        composeTestRule.onNodeWithText("Create Draft").assertIsNotEnabled()
    }

    @Test
    fun testJobLimitFlow() {
        // Mock successful TikTok oEmbed response
        val oEmbedResponse = """
            {
                "author_name": "test_user",
                "title": "Test Recipe Video",
                "duration": 120
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(oEmbedResponse)
            .addHeader("Content-Type", "application/json"))

        // Mock active jobs response with 3 active jobs
        val activeJobsResponse = """
            [
                {"job_id": "job_1", "status": "DOWNLOADING"},
                {"job_id": "job_2", "status": "EXTRACTING"},
                {"job_id": "job_3", "status": "TRANSCRIBING"}
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(activeJobsResponse)
            .addHeader("Content-Type", "application/json"))

        // Navigate to AddFromTikTok screen
        composeTestRule.onNodeWithText("Add from TikTok").performClick()
        
        // Enter TikTok URL
        val urlInput = composeTestRule.onNodeWithText("TikTok URL")
        urlInput.performTextInput("https://www.tiktok.com/@test_user/video/123456789")
        
        // Verify job limit message is displayed
        composeTestRule.onNodeWithText("You have reached the maximum number of active jobs").assertIsDisplayed()
        
        // Verify Create Draft button is disabled
        composeTestRule.onNodeWithText("Create Draft").assertIsNotEnabled()
    }

    // Helper functions to create mock responses
    private fun createJobResponse(jobId: String, status: IngestStatus, recipeId: String? = null): String {
        return """
            {
                "job_id": "$jobId",
                "recipe_id": ${if (recipeId != null) "\"$recipeId\"" else "null"},
                "status": "${status.name}",
                "title": "Test Recipe",
                "transcript": "This is a test recipe transcript",
                "error_code": null,
                "recipe_json": null,
                "onscreen_text": null,
                "ingredient_candidates": [],
                "parse_errors": [],
                "llm_error_message": null
            }
        """.trimIndent()
    }

    private fun createFailedJobResponse(jobId: String, errorCode: IngestErrorCode, errorMessage: String): String {
        return """
            {
                "job_id": "$jobId",
                "recipe_id": null,
                "status": "FAILED",
                "title": null,
                "transcript": null,
                "error_code": "${errorCode.name}",
                "recipe_json": null,
                "onscreen_text": null,
                "ingredient_candidates": [],
                "parse_errors": [],
                "llm_error_message": "$errorMessage"
            }
        """.trimIndent()
    }
} 