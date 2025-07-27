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
     * Update job status from polling results
     */
    fun updateJobStatus(jobDetails: IngestJobDto) {
        _uiState.value = _uiState.value.copy(
            jobStatus = jobDetails.status,
            jobDetails = jobDetails
        )
        
        // Handle error states
        if (jobDetails.status == IngestStatus.FAILED && jobDetails.errorCode != null) {
            handleIngestError(jobDetails.errorCode)
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
    
    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
} 