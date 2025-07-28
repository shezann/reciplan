package com.example.reciplan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.repository.LikeRepository
import com.example.reciplan.data.repository.PagingRecipeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class HomeUiState(
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val pagingRecipeRepository: PagingRecipeRepository,
    private val likeRepository: LikeRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Recipe feed with paging support (cached in repository)
    val recipeFeed: Flow<PagingData<Recipe>> = pagingRecipeRepository
        .getRecipeFeedPaging()

    // Debouncing state for like operations
    private val likeDebouncingMap = mutableMapOf<String, Job>()
    private val debounceDuration = 500L // 500ms debounce

    /**
     * Toggle like status for a recipe with debouncing to prevent rapid clicks
     */
    fun toggleLike(recipeId: String, currentlyLiked: Boolean) {
        // Cancel any existing debounce job for this recipe
        likeDebouncingMap[recipeId]?.cancel()
        
        // Create new debounced job
        likeDebouncingMap[recipeId] = viewModelScope.launch {
            try {
                // Immediate optimistic update for better UX
                val currentLikesCount = getCurrentLikesCount(recipeId)
                val newLiked = !currentlyLiked
                val newLikesCount = if (newLiked) currentLikesCount + 1 else maxOf(0, currentLikesCount - 1)
                
                // Update optimistically
                pagingRecipeRepository.updateRecipeLikeOptimistically(
                    recipeId, 
                    newLiked, 
                    newLikesCount
                )
                
                // Wait for debounce period
                delay(debounceDuration)
                
                // Execute the actual API call
                val result = likeRepository.toggleLike(recipeId, currentlyLiked)
                result.fold(
                    onSuccess = { likeResponse ->
                        // Update with actual server response
                        pagingRecipeRepository.updateRecipeLikeOptimistically(
                            recipeId,
                            likeResponse.liked,
                            likeResponse.likesCount
                        )
                        clearError()
                    },
                    onFailure = { error ->
                        // Revert optimistic update on failure
                        pagingRecipeRepository.updateRecipeLikeOptimistically(
                            recipeId,
                            currentlyLiked,
                            currentLikesCount
                        )
                        setError("Failed to ${if (currentlyLiked) "unlike" else "like"} recipe: ${error.message}")
                    }
                )
            } catch (e: CancellationException) {
                // Job was cancelled (new request came in), do nothing
            } finally {
                // Clean up the debounce job
                likeDebouncingMap.remove(recipeId)
            }
        }
    }

    /**
     * Get current likes count for a recipe from the like repository cache
     */
    private fun getCurrentLikesCount(recipeId: String): Int {
        return likeRepository.likeStates.value[recipeId]?.likesCount ?: 0
    }

    /**
     * Get current like state for a recipe
     */
    fun getLikeState(recipeId: String): StateFlow<com.example.reciplan.data.repository.LikeState> {
        return likeRepository.getLikeState(recipeId)
    }

    /**
     * Refresh the recipe feed
     */
    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                pagingRecipeRepository.invalidatePagingSource()
                clearError()
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Set error state
     */
    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    /**
     * Preload like states for visible recipes for better performance
     */
    fun preloadLikeStates(recipes: List<Recipe>) {
        viewModelScope.launch {
            pagingRecipeRepository.preloadLikeStates(recipes)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel all pending like operations
        likeDebouncingMap.values.forEach { it.cancel() }
        likeDebouncingMap.clear()
    }
}