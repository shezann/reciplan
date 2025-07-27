package com.example.reciplan.data.repository

import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.LikeResponse
import com.example.reciplan.data.model.LikedStatusResponse
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import java.util.concurrent.TimeUnit

class LikeRepositoryTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var recipeApi: RecipeApi
    private lateinit var likeRepository: LikeRepository
    
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
        
        recipeApi = retrofit.create(RecipeApi::class.java)
        likeRepository = LikeRepository(recipeApi)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `toggleLike should perform optimistic update and succeed`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        val mockResponse = """
            {
                "success": true,
                "liked": true,
                "likes_count": 42,
                "recipe_id": "$recipeId"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .setHeader("Content-Type", "application/json"))
        
        // Act
        val result = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        
        // Assert
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { response ->
            assertTrue(response.success)
            assertTrue(response.liked)
            assertEquals(42, response.likesCount)
            assertEquals(recipeId, response.recipeId)
        }
        
        // Verify final state
        val finalState = likeRepository.likeStates.first()[recipeId]
        assertNotNull(finalState)
        assertTrue(finalState!!.liked)
        assertEquals(42, finalState.likesCount)
        assertFalse(finalState.isLoading)
        assertNull(finalState.error)
    }
    
    @Test
    fun `toggleLike should rollback optimistic update on 500 error`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"))
        
        // Act
        val result = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals("Server error - please try again", result.exceptionOrNull()?.message)
        
        // Verify rollback state
        val finalState = likeRepository.likeStates.first()[recipeId]
        assertNotNull(finalState)
        assertFalse(finalState!!.liked) // Should be rolled back to original state
        assertFalse(finalState.isLoading)
        assertNotNull(finalState.error)
    }
    
    @Test
    fun `toggleLike should enforce rate limiting`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // First request
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true, "liked": true, "likes_count": 1, "recipe_id": "$recipeId"}""")
            .setHeader("Content-Type", "application/json"))
        
        // Act - First request should succeed
        val firstResult = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        assertTrue(firstResult.isSuccess)
        
        // Act - Second request immediately should fail due to rate limiting
        val secondResult = likeRepository.toggleLike(recipeId, currentlyLiked = true)
        assertTrue(secondResult.isFailure)
        assertTrue(secondResult.exceptionOrNull()?.message?.contains("Rate limit exceeded") == true)
    }
    
    @Test
    fun `toggleLike should handle request deduplication`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // Simulate slow response
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true, "liked": true, "likes_count": 1, "recipe_id": "$recipeId"}""")
            .setBodyDelay(2, TimeUnit.SECONDS)
            .setHeader("Content-Type", "application/json"))
        
        // Act - Start first request (will be slow)
        val firstRequestDeferred = kotlinx.coroutines.async { 
            likeRepository.toggleLike(recipeId, currentlyLiked = false)
        }
        
        // Give the first request time to start
        delay(100)
        
        // Second request should fail due to deduplication
        val secondResult = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        assertTrue(secondResult.isFailure)
        assertTrue(secondResult.exceptionOrNull()?.message?.contains("Request already in progress") == true)
        
        // First request should still complete successfully
        val firstResult = firstRequestDeferred.await()
        assertTrue(firstResult.isSuccess)
    }
    
    @Test
    fun `toggleLike should handle 401 auth error`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized"))
        
        // Act
        val result = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals("Authentication required", result.exceptionOrNull()?.message)
        
        // Verify rollback occurred
        val finalState = likeRepository.likeStates.first()[recipeId]
        assertNotNull(finalState)
        assertFalse(finalState!!.liked) // Should be rolled back
        assertNotNull(finalState.error)
    }
    
    @Test
    fun `toggleLike should handle 409 conflict error without retry`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(409)
            .setBody("Conflict"))
        
        // Act
        val result = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        
        // Assert
        assertTrue(result.isFailure)
        assertEquals("Conflict - like state may have changed", result.exceptionOrNull()?.message)
        
        // Verify only one request was made (no retries for 409)
        assertEquals(1, mockWebServer.requestCount)
    }
    
    @Test
    fun `toggleLike should retry on 500 error with exponential backoff`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // First two requests fail, third succeeds
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true, "liked": true, "likes_count": 1, "recipe_id": "$recipeId"}""")
            .setHeader("Content-Type", "application/json"))
        
        // Act
        val result = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(3, mockWebServer.requestCount) // Should have retried twice
    }
    
    @Test
    fun `toggleLike should handle 429 rate limiting with retry after`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // First request gets rate limited, second succeeds
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", "1")
            .setBody("Too Many Requests"))
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true, "liked": true, "likes_count": 1, "recipe_id": "$recipeId"}""")
            .setHeader("Content-Type", "application/json"))
        
        // Act
        val result = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(2, mockWebServer.requestCount) // Should have retried after delay
    }
    
    @Test
    fun `getLiked should return current status`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        val mockResponse = """
            {
                "liked": true,
                "likes_count": 42,
                "recipe_id": "$recipeId"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .setHeader("Content-Type", "application/json"))
        
        // Act
        val result = likeRepository.getLiked(recipeId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { response ->
            assertTrue(response.liked)
            assertEquals(42, response.likesCount)
            assertEquals(recipeId, response.recipeId)
        }
        
        // Verify state update
        val state = likeRepository.likeStates.first()[recipeId]
        assertNotNull(state)
        assertTrue(state!!.liked)
        assertEquals(42, state.likesCount)
    }
    
    @Test
    fun `getLiked should handle network errors`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // Simulate network error by shutting down server
        mockWebServer.shutdown()
        
        // Act
        val result = likeRepository.getLiked(recipeId)
        
        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("connection") == true ||
                  result.exceptionOrNull()?.message?.contains("network") == true)
        
        // Verify error state
        val state = likeRepository.likeStates.first()[recipeId]
        assertNotNull(state)
        assertNotNull(state!!.error)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `clearError should remove error state`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // Cause an error first
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        likeRepository.getLiked(recipeId)
        
        // Verify error exists
        var state = likeRepository.likeStates.first()[recipeId]
        assertNotNull(state?.error)
        
        // Act
        likeRepository.clearError(recipeId)
        
        // Assert
        state = likeRepository.likeStates.first()[recipeId]
        assertNull(state?.error)
    }
    
    @Test
    fun `rate limiting should reset after time passes`() = runTest {
        // Arrange
        val recipeId = "recipe123"
        
        // Mock successful responses
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true, "liked": true, "likes_count": 1, "recipe_id": "$recipeId"}""")
            .setHeader("Content-Type", "application/json"))
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true, "liked": false, "likes_count": 0, "recipe_id": "$recipeId"}""")
            .setHeader("Content-Type", "application/json"))
        
        // Act - First request
        val firstResult = likeRepository.toggleLike(recipeId, currentlyLiked = false)
        assertTrue(firstResult.isSuccess)
        
        // Wait for rate limit to reset (1 second + small buffer)
        delay(1100)
        
        // Second request should now succeed
        val secondResult = likeRepository.toggleLike(recipeId, currentlyLiked = true)
        assertTrue(secondResult.isSuccess)
        
        // Verify both requests were made
        assertEquals(2, mockWebServer.requestCount)
    }
} 