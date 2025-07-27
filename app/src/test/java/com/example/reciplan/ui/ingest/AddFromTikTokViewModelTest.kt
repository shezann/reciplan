package com.example.reciplan.ui.ingest

import com.example.reciplan.data.model.*
import com.example.reciplan.data.repository.IngestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AddFromTikTokViewModelTest {

    @Mock
    private lateinit var mockIngestRepository: IngestRepository

    private lateinit var viewModel: AddFromTikTokViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Clear shared instance to avoid interference between tests
        AddFromTikTokViewModel.clearSharedInstance()
        // Create a direct instance for testing instead of using the shared instance
        viewModel = AddFromTikTokViewModel(mockIngestRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Clean up shared instance after each test
        AddFromTikTokViewModel.clearSharedInstance()
    }

    @Test
    fun `validateTikTokUrl sets isValidUrl to true for valid URL`() = runTest {
        val validUrl = "https://www.tiktok.com/@user/video/123"

        viewModel.validateTikTokUrl(validUrl)

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isValidUrl)
    }

    @Test
    fun `validateTikTokUrl sets isValidUrl to true for vm tiktok com URL`() = runTest {
        val validUrl = "https://vm.tiktok.com/ZMScJ8vrH/"

        viewModel.validateTikTokUrl(validUrl)

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isValidUrl)
    }

    @Test
    fun `validateTikTokUrl sets isValidUrl to false for invalid URL`() = runTest {
        val invalidUrl = "https://www.youtube.com/watch?v=123"

        viewModel.validateTikTokUrl(invalidUrl)

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isValidUrl)
    }

    @Test
    fun `startIngest sets loading state correctly`() = runTest {
        val url = "https://www.tiktok.com/@user/video/123"
        val mockResponse = StartIngestResponse(
            jobId = "job123", 
            status = IngestStatus.QUEUED
        )
        
        whenever(mockIngestRepository.startIngest(url))
            .thenReturn(Result.success(mockResponse))
        whenever(mockIngestRepository.getActiveJobCount())
            .thenReturn(Result.success(1))

        viewModel.startIngest(url)

        val uiState = viewModel.uiState.first()
        assertEquals("job123", uiState.jobId)
        assertEquals(IngestStatus.QUEUED, uiState.jobStatus)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `startIngest handles failure correctly`() = runTest {
        val url = "https://www.tiktok.com/@user/video/123"
        val errorMessage = "Network error"
        
        whenever(mockIngestRepository.startIngest(url))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.startIngest(url)

        val uiState = viewModel.uiState.first()
        assertEquals(errorMessage, uiState.error)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `updateJobStatus handles completed status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            recipeId = "recipe456",
            status = IngestStatus.COMPLETED,
            title = "Test Recipe"
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(IngestStatus.COMPLETED, uiState.jobStatus)
        assertEquals("recipe456", uiState.jobDetails?.recipeId)
    }

    @Test
    fun `retryJob restarts polling for recoverable errors`() = runTest {
        // Set up initial error state
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.FAILED,
            errorCode = IngestErrorCode.VIDEO_UNAVAILABLE
        )
        
        viewModel.updateJobStatus(jobDetails)
        
        // Mock the poll job response for retry
        whenever(mockIngestRepository.pollJob("job123"))
            .thenReturn(Result.success(jobDetails.copy(status = IngestStatus.QUEUED)))

        viewModel.retryJob()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.isPolling)
    }

    // Task 4.4: Status mapping tests
    @Test
    fun `updateJobStatus maps QUEUED status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.QUEUED
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(1, uiState.currentStep)
        assertEquals(10, uiState.totalSteps)
        assertEquals("Queued", uiState.stepTitle)
        assertEquals("Your recipe is in the processing queue", uiState.stepDescription)
        assertFalse(uiState.isComplete)
        assertFalse(uiState.hasError)
    }

    @Test
    fun `updateJobStatus maps DOWNLOADING status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.DOWNLOADING
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(2, uiState.currentStep)
        assertEquals("Downloading", uiState.stepTitle)
        assertEquals("Downloading video from TikTok", uiState.stepDescription)
        assertFalse(uiState.isComplete)
        assertFalse(uiState.hasError)
    }

    @Test
    fun `updateJobStatus maps LLM_REFINING status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.LLM_REFINING
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(8, uiState.currentStep)
        assertEquals("AI Processing", uiState.stepTitle)
        assertEquals("Creating your recipe with AI", uiState.stepDescription)
        assertFalse(uiState.isComplete)
        assertFalse(uiState.hasError)
    }

    @Test
    fun `updateJobStatus maps COMPLETED status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.COMPLETED
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(10, uiState.currentStep)
        assertEquals("Complete", uiState.stepTitle)
        assertEquals("Your recipe is ready to view!", uiState.stepDescription)
        assertTrue(uiState.isComplete)
        assertFalse(uiState.hasError)
    }

    @Test
    fun `updateJobStatus maps FAILED status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.FAILED,
            errorCode = IngestErrorCode.VIDEO_UNAVAILABLE
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(0, uiState.currentStep)
        assertEquals("Failed", uiState.stepTitle)
        assertEquals("Something went wrong processing your video", uiState.stepDescription)
        assertFalse(uiState.isComplete)
        assertTrue(uiState.hasError)
    }

    @Test
    fun `updateJobStatus maps DRAFT_PARSED_WITH_ERRORS status correctly`() = runTest {
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.DRAFT_PARSED_WITH_ERRORS
        )

        viewModel.updateJobStatus(jobDetails)

        val uiState = viewModel.uiState.first()
        assertEquals(9, uiState.currentStep)
        assertEquals("Recipe Generated (with warnings)", uiState.stepTitle)
        assertEquals("Recipe created but may need review", uiState.stepDescription)
        assertFalse(uiState.isComplete)
        assertFalse(uiState.hasError)
    }

    @Test
    fun `getProgressPercentage returns correct values`() = runTest {
        // Test initial state
        assertEquals(0f, viewModel.getProgressPercentage())

        // Test mid-progress
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.LLM_REFINING // Step 8 of 10
        )
        viewModel.updateJobStatus(jobDetails)
        assertEquals(0.8f, viewModel.getProgressPercentage())

        // Test completion
        val completedJobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.COMPLETED // Step 10 of 10
        )
        viewModel.updateJobStatus(completedJobDetails)
        assertEquals(1.0f, viewModel.getProgressPercentage())
    }

    @Test
    fun `getProgressText returns correct messages`() = runTest {
        // Test initial state
        assertEquals("Step 0 of 10", viewModel.getProgressText())

        // Test mid-progress
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.TRANSCRIBING // Step 4 of 10
        )
        viewModel.updateJobStatus(jobDetails)
        assertEquals("Step 4 of 10", viewModel.getProgressText())

        // Test completion
        val completedJobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.COMPLETED
        )
        viewModel.updateJobStatus(completedJobDetails)
        assertEquals("Complete!", viewModel.getProgressText())

        // Test error
        val failedJobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.FAILED,
            errorCode = IngestErrorCode.VIDEO_UNAVAILABLE
        )
        viewModel.updateJobStatus(failedJobDetails)
        assertEquals("Processing failed", viewModel.getProgressText())
    }

    @Test
    fun `canRetryCurrentStatus returns correct values`() = runTest {
        // Test non-error status
        val jobDetails = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.DOWNLOADING
        )
        viewModel.updateJobStatus(jobDetails)
        assertFalse(viewModel.canRetryCurrentStatus())

        // Test recoverable error
        val recoverableError = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.FAILED,
            errorCode = IngestErrorCode.ASR_FAILED
        )
        viewModel.updateJobStatus(recoverableError)
        assertTrue(viewModel.canRetryCurrentStatus())

        // Test non-recoverable error
        val nonRecoverableError = IngestJobDto(
            jobId = "job123",
            status = IngestStatus.FAILED,
            errorCode = IngestErrorCode.VIDEO_UNAVAILABLE
        )
        viewModel.updateJobStatus(nonRecoverableError)
        assertFalse(viewModel.canRetryCurrentStatus())
    }

    @Test
    fun `all status transitions are mapped correctly`() = runTest {
        val allStatuses = listOf(
            IngestStatus.QUEUED,
            IngestStatus.DOWNLOADING,
            IngestStatus.EXTRACTING,
            IngestStatus.TRANSCRIBING,
            IngestStatus.DRAFT_TRANSCRIBED,
            IngestStatus.OCRING,
            IngestStatus.OCR_DONE,
            IngestStatus.LLM_REFINING,
            IngestStatus.DRAFT_PARSED,
            IngestStatus.DRAFT_PARSED_WITH_ERRORS,
            IngestStatus.COMPLETED,
            IngestStatus.FAILED
        )

        allStatuses.forEach { status ->
            val jobDetails = IngestJobDto(
                jobId = "job123",
                status = status,
                errorCode = if (status == IngestStatus.FAILED) IngestErrorCode.UNKNOWN_ERROR else null
            )

            viewModel.updateJobStatus(jobDetails)

            val uiState = viewModel.uiState.first()
            
            // Verify all status mappings have proper values
            assertTrue("Status $status should have non-empty title", uiState.stepTitle.isNotEmpty())
            assertTrue("Status $status should have non-empty description", uiState.stepDescription.isNotEmpty())
            assertTrue("Status $status should have valid step range", uiState.currentStep >= 0)
            assertEquals("Total steps should always be 10", 10, uiState.totalSteps)
            
            // Verify terminal states
            when (status) {
                IngestStatus.COMPLETED -> {
                    assertTrue("COMPLETED should be marked as complete", uiState.isComplete)
                    assertFalse("COMPLETED should not have error", uiState.hasError)
                }
                IngestStatus.FAILED -> {
                    assertFalse("FAILED should not be marked as complete", uiState.isComplete)
                    assertTrue("FAILED should have error", uiState.hasError)
                }
                else -> {
                    assertFalse("Non-terminal status should not be complete", uiState.isComplete)
                    assertFalse("Non-terminal status should not have error", uiState.hasError)
                }
            }
        }
    }
} 