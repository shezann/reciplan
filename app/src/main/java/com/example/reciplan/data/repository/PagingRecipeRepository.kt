package com.example.reciplan.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.paging.RecipeFeedPagingSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * Repository that provides recipe feed data using Paging 3
 * Integrates with LikeRepository to provide real-time like state updates
 */
class PagingRecipeRepository(
    private val recipeApi: RecipeApi,
    private val likeRepository: LikeRepository,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val PAGE_SIZE = 10
        private const val PREFETCH_DISTANCE = 3
    }
    
    // Keep reference to current paging source for invalidation
    private var currentPagingSource: RecipeFeedPagingSource? = null
    
    // Create the pager once as a lazy property
    private val pager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2 // Load more on first load
            ),
            pagingSourceFactory = { 
                RecipeFeedPagingSource(recipeApi).also { 
                    currentPagingSource = it 
                }
            }
        )
    }
    
    // Share the pager flow to prevent multiple collections
    private val sharedPagerFlow by lazy {
        pager.flow
            .cachedIn(coroutineScope)
            .shareIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    }
    
    // Create the combined flow once as a lazy property
    private val recipeFeedFlow by lazy {
        combine(
            sharedPagerFlow,
            likeRepository.likeStates
        ) { pagingData, likeStates ->
            // Performance optimization: Only update recipes that have like state changes
            pagingData.map { recipe ->
                val likeState = likeStates[recipe.id]
                if (likeState != null) {
                    recipe.copy(
                        liked = likeState.liked,
                        likesCount = likeState.likesCount
                    )
                } else {
                    // Use original values from API if no cached state
                    recipe
                }
            }
        }
    }

    /**
     * Get recipe feed as PagingData with like fields
     * Automatically syncs with like state changes
     * Flow is properly cached and shared to prevent multiple collections
     */
    fun getRecipeFeedPaging(): Flow<PagingData<Recipe>> {
        return recipeFeedFlow
    }

    /**
     * Invalidate the current paging source to refresh data
     */
    fun invalidatePagingSource() {
        currentPagingSource?.invalidate()
    }

    /**
     * Update recipe like state optimistically in the cache
     * This will trigger UI updates through the combined flow
     */
    suspend fun updateRecipeLikeOptimistically(recipeId: String, liked: Boolean, likesCount: Int) {
        likeRepository.updateLikeStateOptimistically(recipeId, liked, likesCount)
    }

    /**
     * Preload like states for visible recipes to improve performance
     */
    suspend fun preloadLikeStates(recipes: List<Recipe>) {
        recipes.forEach { recipe ->
            // Only preload if we don't already have a cached state
            if (likeRepository.likeStates.value[recipe.id] == null) {
                likeRepository.updateLikeStateOptimistically(
                    recipe.id,
                    recipe.liked,
                    recipe.likesCount
                )
            }
        }
    }

    /**
     * Resolve conflicts between local optimistic state and server response
     * Called when API responses come back with updated like counts
     */
    suspend fun resolveRecipeLikeConflict(recipeId: String, serverLiked: Boolean, serverLikesCount: Int) {
        // If there's a conflict, server always wins
        val currentState = likeRepository.likeStates.value[recipeId]
        if (currentState != null && 
            (currentState.liked != serverLiked || currentState.likesCount != serverLikesCount)) {
            
            likeRepository.updateLikeStateOptimistically(recipeId, serverLiked, serverLikesCount)
        }
    }
}
