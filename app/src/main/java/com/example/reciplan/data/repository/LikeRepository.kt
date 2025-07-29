package com.example.reciplan.data.repository

import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.LikeResponse
import com.example.reciplan.data.model.LikedStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

data class LikeState(
    val liked: Boolean = false,
    val likesCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class LikeRepository(
    private val recipeApi: RecipeApi
) {
    
    // Rate limiting: Track last request time per recipe (max 1 req/second)
    private val lastRequestTime = ConcurrentHashMap<String, Long>()
    private val rateLimitMs = 1000L // 1 second
    
    // Request deduplication: Track ongoing requests
    private val ongoingRequests = ConcurrentHashMap<String, Boolean>()
    private val requestMutex = Mutex()
    
    // Cache for like states to support optimistic updates
    private val _likeStates = MutableStateFlow<Map<String, LikeState>>(emptyMap())
    val likeStates: StateFlow<Map<String, LikeState>> = _likeStates.asStateFlow()
    
    // Cache individual recipe StateFlows to avoid recreating them
    private val recipeStateFlows = ConcurrentHashMap<String, StateFlow<LikeState>>()
    
    /**
     * Get like state for a specific recipe
     * Returns StateFlow that updates when like state changes
     * Uses caching to avoid creating multiple flows for the same recipe
     */
    fun getLikeState(recipeId: String): StateFlow<LikeState> {
        return recipeStateFlows.getOrPut(recipeId) {
            likeStates.map { statesMap ->
                statesMap[recipeId] ?: LikeState()
            }.stateIn(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()),
                started = SharingStarted.Lazily,
                initialValue = LikeState()
            )
        }
    }
    
    /**
     * Optimistically update like state in cache
     * Used by PagingRecipeRepository for immediate UI updates
     */
    fun updateLikeStateOptimistically(recipeId: String, liked: Boolean, likesCount: Int) {
        val currentStates = _likeStates.value.toMutableMap()
        currentStates[recipeId] = LikeState(
            liked = liked,
            likesCount = likesCount,
            isLoading = false,
            error = null
        )
        _likeStates.value = currentStates
    }
    
    /**
     * Refresh like state from server
     * Used when resolving conflicts or ensuring data consistency
     */
    suspend fun refreshLikeState(recipeId: String): Result<LikedStatusResponse> {
        return try {
            val response = recipeApi.getLikedStatus(recipeId)
            if (response.isSuccessful) {
                val likedStatus = response.body()!!
                updateLikeStateOptimistically(
                    recipeId,
                    likedStatus.liked,
                    likedStatus.likesCount
                )
                Result.success(likedStatus)
            } else {
                Result.failure(Exception("Failed to refresh like state: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Retry configuration
    private val maxRetries = 3
    private val baseRetryDelayMs = 1000L
    
    /**
     * Toggle like status for a recipe with optimistic updates and comprehensive error handling
     */
    suspend fun toggleLike(recipeId: String, currentlyLiked: Boolean): Result<LikeResponse> {
        return requestMutex.withLock {
            // Check if request is already in progress
            if (ongoingRequests[recipeId] == true) {
                return Result.failure(Exception("Request already in progress for recipe $recipeId"))
            }
            
            // Rate limiting check
            val lastRequest = lastRequestTime[recipeId] ?: 0
            val timeSinceLastRequest = System.currentTimeMillis() - lastRequest
            if (timeSinceLastRequest < rateLimitMs) {
                val waitTime = rateLimitMs - timeSinceLastRequest
                return Result.failure(Exception("Rate limit exceeded. Try again in ${waitTime}ms"))
            }
            
            // Mark request as ongoing
            ongoingRequests[recipeId] = true
            
            try {
                // Update state with loading indicator
                updateLikeState(recipeId) { state ->
                    state.copy(isLoading = true, error = null)
                }
                
                // Optimistic update
                val optimisticState = LikeState(
                    liked = !currentlyLiked,
                    likesCount = getCurrentLikeState(recipeId).likesCount + if (currentlyLiked) -1 else 1,
                    isLoading = true
                )
                updateLikeState(recipeId) { optimisticState }
                
                // Record request time for rate limiting
                lastRequestTime[recipeId] = System.currentTimeMillis()
                
                // Make the API call with retry logic
                val result = executeWithRetry(recipeId) {
                    if (currentlyLiked) {
                        recipeApi.unlikeRecipe(recipeId)
                    } else {
                        recipeApi.likeRecipe(recipeId)
                    }
                }
                
                result.fold(
                    onSuccess = { response ->
                        // Update with actual response
                        updateLikeState(recipeId) { 
                            LikeState(
                                liked = response.liked,
                                likesCount = response.likesCount,
                                isLoading = false,
                                error = null
                            )
                        }
                        Result.success(response)
                    },
                    onFailure = { error ->
                        // Rollback optimistic update on failure
                        updateLikeState(recipeId) { 
                            LikeState(
                                liked = currentlyLiked,
                                likesCount = getCurrentLikeState(recipeId).likesCount + if (currentlyLiked) 1 else -1,
                                isLoading = false,
                                error = error.message
                            )
                        }
                        Result.failure(error)
                    }
                )
            } finally {
                ongoingRequests.remove(recipeId)
                // Ensure loading state is cleared
                updateLikeState(recipeId) { state ->
                    state.copy(isLoading = false)
                }
            }
        }
    }
    
    /**
     * Get the current liked status for a recipe
     */
    suspend fun getLiked(recipeId: String): Result<LikedStatusResponse> {
        return try {
            updateLikeState(recipeId) { state ->
                state.copy(isLoading = true, error = null)
            }
            
            val response = recipeApi.getLikedStatus(recipeId)
            
            if (response.isSuccessful) {
                val body = response.body()!!
                updateLikeState(recipeId) { 
                    LikeState(
                        liked = body.liked,
                        likesCount = body.likesCount,
                        isLoading = false,
                        error = null
                    )
                }
                Result.success(body)
            } else {
                val error = handleHttpError(response)
                updateLikeState(recipeId) { state ->
                    state.copy(isLoading = false, error = error.message)
                }
                Result.failure(error)
            }
        } catch (e: Exception) {
            val error = handleNetworkError(e)
            updateLikeState(recipeId) { state ->
                state.copy(isLoading = false, error = error.message)
            }
            Result.failure(error)
        }
    }
    
    /**
     * Clear error state for a recipe
     */
    fun clearError(recipeId: String) {
        updateLikeState(recipeId) { state ->
            state.copy(error = null)
        }
    }
    
    private fun getCurrentLikeState(recipeId: String): LikeState {
        return _likeStates.value[recipeId] ?: LikeState()
    }
    
    private fun updateLikeState(recipeId: String, update: (LikeState) -> LikeState) {
        val currentStates = _likeStates.value.toMutableMap()
        currentStates[recipeId] = update(getCurrentLikeState(recipeId))
        _likeStates.value = currentStates
    }
    
    /**
     * Execute API call with exponential backoff retry logic
     */
    private suspend fun executeWithRetry(
        recipeId: String,
        apiCall: suspend () -> Response<LikeResponse>
    ): Result<LikeResponse> {
        var lastException: Exception? = null
        
        for (attempt in 0 until maxRetries) {
            try {
                val response = apiCall()
                
                if (response.isSuccessful) {
                    return Result.success(response.body()!!)
                } else {
                    val error = handleHttpError(response)
                    
                    // Don't retry on certain errors
                    when (response.code()) {
                        400, 403, 404, 409 -> return Result.failure(error)
                        401 -> {
            
                            return Result.failure(Exception("Authentication required"))
                        }
                        429 -> {
                            // Rate limited by server - extract retry after
                            val retryAfter = response.headers()["Retry-After"]?.toLongOrNull() ?: 5
                            delay(retryAfter * 1000)
                            lastException = error
                            continue
                        }
                        in 500..599 -> {
                            // Server errors - retry with backoff
                            if (attempt < maxRetries - 1) {
                                val delayMs = baseRetryDelayMs * (1L shl attempt) // Exponential backoff
                                delay(delayMs)
                                lastException = error
                                continue
                            }
                            return Result.failure(error)
                        }
                    }
                }
            } catch (e: Exception) {
                val error = handleNetworkError(e)
                lastException = error
                
                // Retry on network errors
                if (attempt < maxRetries - 1) {
                    val delayMs = baseRetryDelayMs * (1L shl attempt)
                    delay(delayMs)
                    continue
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown error after $maxRetries attempts"))
    }
    
    private fun handleHttpError(response: Response<*>): Exception {
        return when (response.code()) {
            400 -> Exception("Invalid request")
            401 -> Exception("Authentication required")
            403 -> Exception("Permission denied")
            404 -> Exception("Recipe not found")
            409 -> Exception("Conflict - like state may have changed")
            429 -> Exception("Too many requests - please wait")
            in 500..599 -> Exception("Server error - please try again")
            else -> Exception("HTTP ${response.code()}: ${response.message()}")
        }
    }
    
    private fun handleNetworkError(exception: Exception): Exception {
        return when (exception) {
            is UnknownHostException -> Exception("No internet connection")
            is SocketTimeoutException -> Exception("Request timed out")
            is HttpException -> Exception("Network error: ${exception.message}")
            else -> exception
        }
    }
}
