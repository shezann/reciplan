package com.example.reciplan.ui.ingest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.model.StartIngestResponse
import com.example.reciplan.data.model.IngestJobDto
import com.example.reciplan.data.model.IngestStatus
import com.example.reciplan.data.repository.IngestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.reciplan.data.model.IngestErrorCode
import com.example.reciplan.ui.common.ErrorMapper
import com.example.reciplan.analytics.Telemetry

data class AddFromTikTokUiState(
    val isLoading: Boolean = false,
    val jobId: String? = null,
    val error: String? = null,
    val isJobLimitReached: Boolean = false,
    val activeJobCount: Int = 0,
    val isValidUrl: Boolean = false,
    // Status fields
    val jobStatus: IngestStatus? = null,
    val jobDetails: IngestJobDto? = null,
    val isPolling: Boolean = false,
    // Progress indicator fields (Task 4.4)
    val currentStep: Int = 0,
    val totalSteps: Int = 10,
    val stepTitle: String = "",
    val stepDescription: String = "",
    val isComplete: Boolean = false,
    val hasError: Boolean = false,
    // Error handling fields
    val errorCode: IngestErrorCode? = null,
    val showErrorSnackbar: Boolean = false,
    val errorSnackbarMessage: String? = null,
    val canRetry: Boolean = false,
    val retryActionText: String? = null
)

class AddFromTikTokViewModel(
    private val ingestRepository: IngestRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddFromTikTokUiState())
    val uiState: StateFlow<AddFromTikTokUiState> = _uiState.asStateFlow()
    
    private var pollingJob: kotlinx.coroutines.Job? = null
    
    companion object {
        private val TIKTOK_URL_PATTERN = Regex("https?://(www\\.)?(vm\\.)?tiktok\\.com/.*")
        private const val POLL_INTERVAL_INITIAL = 4000L
        private const val POLL_INTERVAL_BACKOFF = 8000L
        private const val POLL_BACKOFF_THRESHOLD = 30
        
        // Shared instance for navigation
        private var sharedInstance: AddFromTikTokViewModel? = null
        
        fun getSharedInstance(ingestRepository: IngestRepository): AddFromTikTokViewModel {
            if (sharedInstance == null) {
                sharedInstance = AddFromTikTokViewModel(ingestRepository).apply {
                    // Only check active job count for the shared instance
                    checkActiveJobCount()
                }
            }
            return sharedInstance!!
        }
        
        fun clearSharedInstance() {
            sharedInstance?.stopPolling()
            sharedInstance = null
        }
    }
    
    /**
     * Validate that the URL is a TikTok link
     */
    fun validateTikTokUrl(url: String) {
        val isValidTikTokUrl = url.isNotBlank() && TIKTOK_URL_PATTERN.matches(url)
        _uiState.value = _uiState.value.copy(isValidUrl = isValidTikTokUrl)
    }

    /**
     * Start an ingest job with the given TikTok URL
     */
    fun startIngest(url: String) {
        if (_uiState.value.isLoading) return
        
        // Fire telemetry event for ingest started
        Telemetry.ingestStarted(url)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            ingestRepository.startIngest(url).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        jobId = response.jobId,
                        error = null,
                        jobStatus = response.status,
                        isPolling = true
                    )
                    startJobPolling(response.jobId)
                    checkActiveJobCount()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to start ingest job"
                    )
                }
            )
        }
    }
    
    /**
     * Data class for status UI mapping information
     */
    private data class StatusUiInfo(
        val step: Int,
        val totalSteps: Int,
        val title: String,
        val description: String,
        val isComplete: Boolean,
        val hasError: Boolean
    )

    /**
     * Map IngestStatus to UI state information for progress indicator
     */
    private fun mapStatusToUiState(status: IngestStatus): StatusUiInfo {
        return when (status) {
            IngestStatus.QUEUED -> StatusUiInfo(
                step = 1,
                totalSteps = 10,
                title = "Queued",
                description = "Your recipe is in the processing queue",
                isComplete = false,
                hasError = false
            )
            IngestStatus.DOWNLOADING -> StatusUiInfo(
                step = 2,
                totalSteps = 10,
                title = "Downloading",
                description = "Downloading video from TikTok",
                isComplete = false,
                hasError = false
            )
            IngestStatus.EXTRACTING -> StatusUiInfo(
                step = 3,
                totalSteps = 10,
                title = "Extracting",
                description = "Extracting audio and video content",
                isComplete = false,
                hasError = false
            )
            IngestStatus.TRANSCRIBING -> StatusUiInfo(
                step = 4,
                totalSteps = 10,
                title = "Transcribing",
                description = "Converting speech to text",
                isComplete = false,
                hasError = false
            )
            IngestStatus.DRAFT_TRANSCRIBED -> StatusUiInfo(
                step = 5,
                totalSteps = 10,
                title = "Transcription Complete",
                description = "Audio transcription finished",
                isComplete = false,
                hasError = false
            )
            IngestStatus.OCRING -> StatusUiInfo(
                step = 6,
                totalSteps = 10,
                title = "Reading Text",
                description = "Extracting text from video frames",
                isComplete = false,
                hasError = false
            )
            IngestStatus.OCR_DONE -> StatusUiInfo(
                step = 7,
                totalSteps = 10,
                title = "Text Extraction Complete",
                description = "On-screen text has been captured",
                isComplete = false,
                hasError = false
            )
            IngestStatus.LLM_REFINING -> StatusUiInfo(
                step = 8,
                totalSteps = 10,
                title = "AI Processing",
                description = "Creating your recipe with AI",
                isComplete = false,
                hasError = false
            )
            IngestStatus.DRAFT_PARSED -> StatusUiInfo(
                step = 9,
                totalSteps = 10,
                title = "Recipe Generated",
                description = "Your recipe draft is ready",
                isComplete = false,
                hasError = false
            )
            IngestStatus.DRAFT_PARSED_WITH_ERRORS -> StatusUiInfo(
                step = 9,
                totalSteps = 10,
                title = "Recipe Generated (with warnings)",
                description = "Recipe created but may need review",
                isComplete = false,
                hasError = false
            )
            IngestStatus.COMPLETED -> StatusUiInfo(
                step = 10,
                totalSteps = 10,
                title = "Complete",
                description = "Your recipe is ready to view!",
                isComplete = true,
                hasError = false
            )
            IngestStatus.FAILED -> StatusUiInfo(
                step = 0,
                totalSteps = 10,
                title = "Failed",
                description = "Something went wrong processing your video",
                isComplete = false,
                hasError = true
            )
        }
    }

    /**
     * Update job status from polling results
     */
    fun updateJobStatus(jobDetails: IngestJobDto) {
        val statusInfo = mapStatusToUiState(jobDetails.status)
        
        _uiState.value = _uiState.value.copy(
            jobStatus = jobDetails.status,
            jobDetails = jobDetails,
            currentStep = statusInfo.step,
            totalSteps = statusInfo.totalSteps,
            stepTitle = statusInfo.title,
            stepDescription = statusInfo.description,
            isComplete = statusInfo.isComplete,
            hasError = statusInfo.hasError
        )
        
        // Fire telemetry events for terminal states
        when (jobDetails.status) {
            IngestStatus.COMPLETED -> {
                Telemetry.ingestSucceeded(
                    jobId = jobDetails.jobId ?: "unknown",
                    recipeId = jobDetails.recipeId
                )
            }
            IngestStatus.FAILED -> {
                if (jobDetails.errorCode != null) {
                    Telemetry.ingestFailed(
                        errorCode = jobDetails.errorCode,
                        jobId = jobDetails.jobId
                    )
                    handleIngestError(jobDetails.errorCode)
                }
            }
            else -> {
                // Non-terminal status, continue polling
            }
        }
        
        // Stop polling if job is complete
        if (jobDetails.status == IngestStatus.COMPLETED || jobDetails.status == IngestStatus.FAILED) {
            pollingJob?.cancel()
            pollingJob = null
        }
    }
    
    /**
     * Handle ingest error with friendly message and retry option
     */
    private fun handleIngestError(errorCode: IngestErrorCode) {
        val errorMessage = ErrorMapper.getErrorMessage(errorCode)
        val canRetry = ErrorMapper.isRecoverable(errorCode)
        val retryActionText = ErrorMapper.getRetryActionText(errorCode)
        
        _uiState.value = _uiState.value.copy(
            errorCode = errorCode,
            showErrorSnackbar = true,
            errorSnackbarMessage = errorMessage,
            canRetry = canRetry,
            retryActionText = retryActionText
        )
    }
    
    /**
     * Retry the current job
     */
    fun retryJob() {
        val currentJobId = _uiState.value.jobId
        val currentErrorCode = _uiState.value.errorCode
        
        if (currentJobId != null && currentErrorCode != null && ErrorMapper.isRecoverable(currentErrorCode)) {
            // Fire telemetry event for retry
            Telemetry.ingestRetried(currentErrorCode, currentJobId)
            
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    showErrorSnackbar = false,
                    errorSnackbarMessage = null,
                    isPolling = true,
                    errorCode = null,
                    canRetry = false,
                    retryActionText = null
                )
                startJobPolling(currentJobId)
            }
        }
    }
    
    /**
     * Dismiss error snackbar
     */
    fun dismissErrorSnackbar() {
        _uiState.value = _uiState.value.copy(
            showErrorSnackbar = false,
            errorSnackbarMessage = null
        )
    }
    
    /**
     * Check the number of active jobs to enforce the limit
     */
    private fun checkActiveJobCount() {
        viewModelScope.launch {
            ingestRepository.getActiveJobCount().fold(
                onSuccess = { count ->
                    val isLimitReached = count >= 3
                    _uiState.value = _uiState.value.copy(
                        activeJobCount = count,
                        isJobLimitReached = isLimitReached
                    )
                },
                onFailure = { 
                    // Allow user to proceed if count check fails
                    _uiState.value = _uiState.value.copy(
                        activeJobCount = 0,
                        isJobLimitReached = false
                    )
                }
            )
        }
    }
    
    /**
     * Start polling for job status
     */
    fun startJobPolling(jobId: String) {
        stopPolling()
        
        _uiState.value = _uiState.value.copy(isPolling = true)
        
        pollingJob = viewModelScope.launch {
            var pollCount = 0
            
            while (true) {
                try {
                    val result = ingestRepository.pollJob(jobId)
                    
                    if (result.isSuccess) {
                        val jobDetails = result.getOrNull()
                        if (jobDetails != null) {
                            // Set the job ID if it's missing from the response
                            val updatedJobDetails = if (jobDetails.jobId == null) {
                                jobDetails.copy(jobId = jobId)
                            } else {
                                jobDetails
                            }
                            updateJobStatus(updatedJobDetails)
                            
                            // Stop polling if job is complete
                            if (jobDetails.status == IngestStatus.COMPLETED || jobDetails.status == IngestStatus.FAILED) {
                                break
                            }
                        }
                    }
                    
                    pollCount++
                    
                    // Use exponential backoff after threshold
                    val pollInterval = if (pollCount >= POLL_BACKOFF_THRESHOLD) {
                        POLL_INTERVAL_BACKOFF
                    } else {
                        POLL_INTERVAL_INITIAL
                    }
                    
                    delay(pollInterval)
                    
                } catch (e: Exception) {
                    delay(POLL_INTERVAL_INITIAL)
                }
            }
        }
    }
    
        /**
     * Stop polling
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _uiState.value = _uiState.value.copy(isPolling = false)
    }
    
    /**
     * Get current progress as a percentage (0.0 to 1.0)
     */
    fun getProgressPercentage(): Float {
        val currentState = _uiState.value
        return if (currentState.totalSteps > 0) {
            currentState.currentStep.toFloat() / currentState.totalSteps.toFloat()
        } else {
            0f
        }
    }

    /**
     * Get progress text for display (e.g., "Step 3 of 10")
     */
    fun getProgressText(): String {
        val currentState = _uiState.value
        return if (currentState.hasError) {
            "Processing failed"
        } else if (currentState.isComplete) {
            "Complete!"
        } else {
            "Step ${currentState.currentStep} of ${currentState.totalSteps}"
        }
    }

    /**
     * Check if the current status allows for retry
     */
    fun canRetryCurrentStatus(): Boolean {
        val currentState = _uiState.value
        return currentState.hasError && currentState.errorCode != null && 
               ErrorMapper.isRecoverable(currentState.errorCode)
    }

    /**
     * Cancel current ingest job (for when user navigates away)
     */
    fun cancelIngest() {
        val currentJobId = _uiState.value.jobId
        val currentStatus = _uiState.value.jobStatus
        
        // Only fire cancellation event if job is still in progress
        if (currentJobId != null && currentStatus != null && 
            currentStatus != IngestStatus.COMPLETED && currentStatus != IngestStatus.FAILED) {
            Telemetry.ingestCancelled(currentJobId)
        }
        
        stopPolling()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
} 