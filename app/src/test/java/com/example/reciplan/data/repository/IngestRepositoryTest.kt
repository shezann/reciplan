package com.example.reciplan.data.repository

import com.example.reciplan.data.api.IngestApi
import com.example.reciplan.data.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class IngestRepositoryTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var ingestApi: IngestApi
    private lateinit var ingestRepository: IngestRepository
    
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        
        ingestApi = retrofit.create(IngestApi::class.java)
        ingestRepository = IngestRepository(ingestApi)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `startIngest success - returns job ID and status`() = runTest {
        // Given
        val successResponse = """
            {
                "job_id": "job_123",
                "recipe_id": null,
                "status": "QUEUED",
                "message": "Job queued successfully"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(successResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("job_123", response?.jobId)
        assertEquals(IngestStatus.QUEUED, response?.status)
        assertEquals("Job queued successfully", response?.message)
    }
    
    @Test
    fun `startIngest with recipe ID - returns both IDs`() = runTest {
        // Given
        val successResponse = """
            {
                "job_id": "job_456",
                "recipe_id": "recipe_789",
                "status": "QUEUED",
                "message": "Job queued successfully"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(successResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/456")
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("job_456", response?.jobId)
        assertEquals("recipe_789", response?.recipeId)
        assertEquals(IngestStatus.QUEUED, response?.status)
    }
    
    @Test
    fun `pollJob success - returns complete job data`() = runTest {
        // Given
        val successResponse = """
            {
                "job_id": "job_123",
                "recipe_id": "recipe_456",
                "status": "COMPLETED",
                "title": "Amazing Pasta Recipe",
                "transcript": "Today I'm making amazing pasta...",
                "error_code": null,
                "recipe_json": "{\"title\":\"Amazing Pasta\",\"ingredients\":[]}",
                "onscreen_text": "2 cups flour, 3 eggs",
                "ingredient_candidates": ["flour", "eggs", "salt"],
                "parse_errors": [],
                "llm_error_message": null
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(successResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = ingestRepository.pollJob("job_123")
        
        // Then
        assertTrue(result.isSuccess)
        val job = result.getOrNull()
        assertNotNull(job)
        assertEquals("job_123", job?.jobId)
        assertEquals("recipe_456", job?.recipeId)
        assertEquals(IngestStatus.COMPLETED, job?.status)
        assertEquals("Amazing Pasta Recipe", job?.title)
        assertEquals("Today I'm making amazing pasta...", job?.transcript)
        assertEquals("2 cups flour, 3 eggs", job?.onscreenText)
        assertEquals(listOf("flour", "eggs", "salt"), job?.ingredientCandidates)
        assertTrue(job?.parseErrors?.isEmpty() == true)
        assertNull(job?.errorCode)
        assertNull(job?.llmErrorMessage)
    }
    
    @Test
    fun `pollJob failed status - returns error data`() = runTest {
        // Given
        val failedResponse = """
            {
                "job_id": "job_789",
                "recipe_id": null,
                "status": "FAILED",
                "title": null,
                "transcript": null,
                "error_code": "VIDEO_UNAVAILABLE",
                "recipe_json": null,
                "onscreen_text": null,
                "ingredient_candidates": [],
                "parse_errors": [],
                "llm_error_message": "Video is no longer available"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(failedResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = ingestRepository.pollJob("job_789")
        
        // Then
        assertTrue(result.isSuccess)
        val job = result.getOrNull()
        assertNotNull(job)
        assertEquals("job_789", job?.jobId)
        assertEquals(IngestStatus.FAILED, job?.status)
        assertEquals(IngestErrorCode.VIDEO_UNAVAILABLE, job?.errorCode)
        assertEquals("Video is no longer available", job?.llmErrorMessage)
        assertNull(job?.recipeId)
        assertNull(job?.title)
        assertNull(job?.transcript)
    }
    
    @Test
    fun `startIngest 400 error - returns bad request error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(400)
            .setBody("Bad Request"))
        
        // When
        val result = ingestRepository.startIngest("invalid-url")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Bad Request") == true)
        assertTrue(exception?.message?.contains("Invalid TikTok URL") == true)
    }
    
    @Test
    fun `startIngest 401 error - returns unauthorized error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Unauthorized") == true)
        assertTrue(exception?.message?.contains("log in again") == true)
    }
    
    @Test
    fun `startIngest 403 error - returns forbidden error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(403)
            .setBody("Forbidden"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Forbidden") == true)
        assertTrue(exception?.message?.contains("don't have permission") == true)
    }
    
    @Test
    fun `startIngest 409 error - returns conflict error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(409)
            .setBody("Conflict"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Conflict") == true)
        assertTrue(exception?.message?.contains("already exists") == true)
    }
    
    @Test
    fun `startIngest 422 error - returns validation error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(422)
            .setBody("Validation Error"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Validation Error") == true)
        assertTrue(exception?.message?.contains("check your TikTok URL") == true)
    }
    
    @Test
    fun `startIngest 429 error - returns rate limit error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(429)
            .setBody("Rate Limited"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Rate Limited") == true)
        assertTrue(exception?.message?.contains("try again later") == true)
    }
    
    @Test
    fun `startIngest 500 error - returns server error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Server Error"))
        
        // When
        val result = ingestRepository.startIngest("https://www.tiktok.com/@user/video/123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Server Error") == true)
        assertTrue(exception?.message?.contains("try again later") == true)
    }
    
    @Test
    fun `pollJob 404 error - returns not found error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"))
        
        // When
        val result = ingestRepository.pollJob("nonexistent-job")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Not Found") == true)
        assertTrue(exception?.message?.contains("Ingest job not found") == true)
    }
    
    @Test
    fun `pollJob network error - returns network error`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(503)
            .setBody("Service Unavailable"))
        
        // When
        val result = ingestRepository.pollJob("job_123")
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Network Error") == true)
    }
    
    @Test
    fun `pollJob with all error codes - handles different failure scenarios`() = runTest {
        val errorCodes = listOf(
            IngestErrorCode.VIDEO_UNAVAILABLE,
            IngestErrorCode.ASR_FAILED,
            IngestErrorCode.OCR_FAILED,
            IngestErrorCode.LLM_FAILED,
            IngestErrorCode.PERSIST_FAILED,
            IngestErrorCode.UNKNOWN_ERROR
        )
        
        errorCodes.forEach { errorCode ->
            // Given
            val errorResponse = """
                {
                    "job_id": "job_error_${errorCode.name}",
                    "recipe_id": null,
                    "status": "FAILED",
                    "title": null,
                    "transcript": null,
                    "error_code": "${errorCode.name}",
                    "recipe_json": null,
                    "onscreen_text": null,
                    "ingredient_candidates": [],
                    "parse_errors": [],
                    "llm_error_message": "Error occurred during ${errorCode.name.lowercase()}"
                }
            """.trimIndent()
            
            mockWebServer.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json"))
            
            // When
            val result = ingestRepository.pollJob("job_error_${errorCode.name}")
            
            // Then
            assertTrue(result.isSuccess)
            val job = result.getOrNull()
            assertNotNull(job)
            assertEquals(IngestStatus.FAILED, job?.status)
            assertEquals(errorCode, job?.errorCode)
            assertTrue(job?.llmErrorMessage?.contains(errorCode.name.lowercase()) == true)
        }
    }
    
    @Test
    fun `pollJob with all status transitions - handles all pipeline stages`() = runTest {
        val statuses = listOf(
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
            IngestStatus.COMPLETED
        )
        
        statuses.forEach { status ->
            // Given
            val statusResponse = """
                {
                    "job_id": "job_status_${status.name}",
                    "recipe_id": "recipe_${status.name}",
                    "status": "${status.name}",
                    "title": "Recipe in ${status.getDisplayName()}",
                    "transcript": "Transcript for ${status.getDisplayName()}",
                    "error_code": null,
                    "recipe_json": null,
                    "onscreen_text": null,
                    "ingredient_candidates": [],
                    "parse_errors": [],
                    "llm_error_message": null
                }
            """.trimIndent()
            
            mockWebServer.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(statusResponse)
                .addHeader("Content-Type", "application/json"))
            
            // When
            val result = ingestRepository.pollJob("job_status_${status.name}")
            
            // Then
            assertTrue(result.isSuccess)
            val job = result.getOrNull()
            assertNotNull(job)
            assertEquals(status, job?.status)
            assertEquals("Recipe in ${status.getDisplayName()}", job?.title)
            assertEquals("Transcript for ${status.getDisplayName()}", job?.transcript)
        }
    }
} 