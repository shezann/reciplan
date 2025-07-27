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
} 