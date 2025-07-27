package com.example.reciplan.data.api

import com.example.reciplan.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface IngestApi {
    
    /**
     * Start a new TikTok video ingest job
     * POST /ingest/tiktok
     */
    @POST("ingest/tiktok")
    suspend fun startIngest(
        @Body request: StartIngestRequest
    ): Response<StartIngestResponse>
    
    /**
     * Get the status and details of an ingest job
     * GET /ingest/jobs/{id}
     */
    @GET("ingest/jobs/{id}")
    suspend fun pollJob(
        @Path("id") jobId: String
    ): Response<IngestJobDto>
    
    /**
     * Get all active jobs for the current user
     * GET /ingest/jobs/active
     */
    @GET("ingest/jobs/active")
    suspend fun getActiveJobs(): Response<List<IngestJobDto>>
} 