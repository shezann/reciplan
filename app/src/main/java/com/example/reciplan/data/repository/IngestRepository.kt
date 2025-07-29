package com.example.reciplan.data.repository

import com.example.reciplan.data.api.IngestApi
import com.example.reciplan.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class IngestRepository(
    private val ingestApi: IngestApi
) {
    
    /**
     * Start a new TikTok video ingest job
     * @param url The TikTok video URL to process
     * @return Result containing job ID and initial status
     */
    suspend fun startIngest(url: String): Result<StartIngestResponse> {
        return try {
    
            
            val request = StartIngestRequest(url = url)
            val response = ingestApi.startIngest(request)
            

            
            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {

                    Result.success(body)
                } else {

                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()

                Result.failure(handleError(response))
            }
        } catch (e: Exception) {

            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Poll the status of an ingest job
     * @param jobId The job ID to poll
     * @return Result containing the current job status and details
     */
    suspend fun pollJob(jobId: String): Result<IngestJobDto> {
        return try {
    
            
            val response = ingestApi.pollJob(jobId)
            

            
            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {

                    Result.success(body)
                } else {

                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()

                Result.failure(handleError(response))
            }
        } catch (e: Exception) {

            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Flow-based job polling for UI consumption
     * @param jobId The job ID to poll
     * @return Flow of job status updates
     */
    fun pollJobFlow(jobId: String): Flow<Result<IngestJobDto>> = flow {
        emit(pollJob(jobId))
    }
    
    /**
     * Flow-based ingest starting for UI consumption
     * @param url The TikTok video URL to process
     * @return Flow of start ingest result
     */
    fun startIngestFlow(url: String): Flow<Result<StartIngestResponse>> = flow {
        emit(startIngest(url))
    }
    
    /**
     * Get all active jobs for the current user
     * @return Result containing list of active jobs
     */
    suspend fun getActiveJobs(): Result<List<IngestJobDto>> {
        return try {
    
            
            val response = ingestApi.getActiveJobs()
            

            
            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    val activeJobs = body.filter { job ->
                        // Consider jobs as active if they're not in terminal states
                        job.status !in listOf(
                            IngestStatus.COMPLETED,
                            IngestStatus.FAILED
                        )
                    }

                    Result.success(activeJobs)
                } else {

                    Result.success(emptyList())
                }
            } else {
                val errorBody = response.errorBody()?.string()

                Result.failure(handleError(response))
            }
        } catch (e: Exception) {

            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Get the count of active jobs
     * @return Result containing the number of active jobs
     */
    suspend fun getActiveJobCount(): Result<Int> {
        return getActiveJobs().map { jobs -> jobs.size }
    }
    
    /**
     * Helper function to handle API errors
     */
    private fun handleError(response: Response<*>): Exception {
        val errorMessage = when (response.code()) {
            400 -> "Bad Request: Invalid TikTok URL provided"
            401 -> "Unauthorized: Please log in again"
            403 -> "Forbidden: You don't have permission to perform this action"
            404 -> "Not Found: Ingest job not found"
            409 -> "Conflict: Job already exists for this URL"
            422 -> "Validation Error: Please check your TikTok URL"
            429 -> "Rate Limited: Too many requests, please try again later"
            500 -> "Server Error: Please try again later"
            else -> "Network Error: ${response.message()}"
        }
        return Exception(errorMessage)
    }
} 