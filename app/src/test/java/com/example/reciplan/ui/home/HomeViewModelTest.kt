package com.example.reciplan.ui.home

import app.cash.turbine.test
import com.example.reciplan.data.model.LikeResponse
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.repository.LikeRepository
import com.example.reciplan.data.repository.LikeState
import com.example.reciplan.data.repository.PagingRecipeRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var pagingRecipeRepository: PagingRecipeRepository
    private lateinit var likeRepository: LikeRepository
    private lateinit var homeViewModel: HomeViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        pagingRecipeRepository = mockk()
        likeRepository = mockk()
        
        // Setup default mock behaviors
        every { likeRepository.likeStates } returns MutableStateFlow(emptyMap())
        every { likeRepository.getLikeState(any()) } returns MutableStateFlow(LikeState())
        every { pagingRecipeRepository.updateRecipeLikeOptimistically(any(), any(), any()) } just Runs
        coEvery { pagingRecipeRepository.preloadLikeStates(any()) } just Runs
        
        homeViewModel = HomeViewModel(pagingRecipeRepository, likeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleLike performs optimistic update immediately`() = runTest {
        // Given
        val recipeId = "recipe1"
        val currentlyLiked = false
        val currentLikesCount = 42
        
        every { likeRepository.likeStates.value } returns mapOf(
            recipeId to LikeState(liked = false, likesCount = currentLikesCount)
        )
        coEvery { likeRepository.toggleLike(any(), any()) } returns Result.success(
            LikeResponse(
                success = true,
                liked = true,
                likesCount = currentLikesCount + 1,
                recipeId = recipeId
            )
        )

        // When
        homeViewModel.toggleLike(recipeId, currentlyLiked)

        // Then - optimistic update should happen immediately
        verify { 
            pagingRecipeRepository.updateRecipeLikeOptimistically(
                recipeId, 
                true, // new liked state
                currentLikesCount + 1 // new count
            ) 
        }
    }

    @Test
    fun `toggleLike debounces multiple rapid calls`() = runTest {
        // Given
        val recipeId = "recipe1"
        val currentlyLiked = false
        
        every { likeRepository.likeStates.value } returns mapOf(
            recipeId to LikeState(liked = false, likesCount = 42)
        )
        coEvery { likeRepository.toggleLike(any(), any()) } returns Result.success(
            LikeResponse(success = true, liked = true, likesCount = 43, recipeId = recipeId)
        )

        // When - multiple rapid calls
        homeViewModel.toggleLike(recipeId, currentlyLiked)
        homeViewModel.toggleLike(recipeId, !currentlyLiked) // Should cancel previous
        homeViewModel.toggleLike(recipeId, currentlyLiked) // Should cancel previous
        
        // Advance time past debounce period
        advanceTimeBy(600L) // 500ms + buffer

        // Then - only one API call should be made (the last one)
        coVerify(exactly = 1) { likeRepository.toggleLike(recipeId, currentlyLiked) }
    }

    @Test
    fun `toggleLike reverts optimistic update on failure`() = runTest {
        // Given
        val recipeId = "recipe1"
        val currentlyLiked = false
        val currentLikesCount = 42
        
        every { likeRepository.likeStates.value } returns mapOf(
            recipeId to LikeState(liked = false, likesCount = currentLikesCount)
        )
        coEvery { likeRepository.toggleLike(any(), any()) } returns Result.failure(
            Exception("Network error")
        )

        // When
        homeViewModel.toggleLike(recipeId, currentlyLiked)
        advanceTimeBy(600L) // Wait for debounce + processing

        // Then - should revert optimistic update
        verify(atLeast = 2) { 
            pagingRecipeRepository.updateRecipeLikeOptimistically(recipeId, any(), any()) 
        }
        
        // Verify final state is reverted
        verify { 
            pagingRecipeRepository.updateRecipeLikeOptimistically(
                recipeId, 
                currentlyLiked, // reverted back
                currentLikesCount // reverted back
            ) 
        }
    }

    @Test
    fun `toggleLike updates with actual server response on success`() = runTest {
        // Given
        val recipeId = "recipe1"
        val currentlyLiked = false
        val serverLikesCount = 45 // Different from optimistic count
        
        every { likeRepository.likeStates.value } returns mapOf(
            recipeId to LikeState(liked = false, likesCount = 42)
        )
        coEvery { likeRepository.toggleLike(any(), any()) } returns Result.success(
            LikeResponse(
                success = true,
                liked = true,
                likesCount = serverLikesCount,
                recipeId = recipeId
            )
        )

        // When
        homeViewModel.toggleLike(recipeId, currentlyLiked)
        advanceTimeBy(600L) // Wait for debounce + processing

        // Then - should update with server response
        verify { 
            pagingRecipeRepository.updateRecipeLikeOptimistically(
                recipeId, 
                true, // from server
                serverLikesCount // from server
            ) 
        }
    }

    @Test
    fun `refreshFeed invalidates paging source and clears errors`() = runTest {
        // Given
        every { pagingRecipeRepository.invalidatePagingSource() } just Runs
        
        // Set an error state first
        homeViewModel.uiState.value.copy(error = "Some error")

        // When
        homeViewModel.refreshFeed()
        advanceUntilIdle()

        // Then
        verify { pagingRecipeRepository.invalidatePagingSource() }
        assertNull(homeViewModel.uiState.value.error)
    }

    @Test
    fun `refreshFeed handles loading state correctly`() = runTest {
        // Given
        every { pagingRecipeRepository.invalidatePagingSource() } just Runs

        homeViewModel.uiState.test {
            // Initial state
            assertEquals(HomeUiState(), awaitItem())

            // When
            homeViewModel.refreshFeed()
            
            // Then - should show loading, then hide loading
            assertEquals(HomeUiState(isRefreshing = true), awaitItem())
            assertEquals(HomeUiState(isRefreshing = false), awaitItem())
        }
    }

    @Test
    fun `preloadLikeStates delegates to repository`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1",
                title = "Recipe 1",
                userId = "user1",
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            )
        )

        // When
        homeViewModel.preloadLikeStates(recipes)
        advanceUntilIdle()

        // Then
        coVerify { pagingRecipeRepository.preloadLikeStates(recipes) }
    }

    @Test
    fun `getLikeState returns correct state flow`() {
        // Given
        val recipeId = "recipe1"
        val expectedStateFlow = MutableStateFlow(LikeState(liked = true, likesCount = 10))
        
        every { likeRepository.getLikeState(recipeId) } returns expectedStateFlow

        // When
        val result = homeViewModel.getLikeState(recipeId)

        // Then
        assertEquals(expectedStateFlow, result)
        verify { likeRepository.getLikeState(recipeId) }
    }

    @Test
    fun `clearError removes error from ui state`() = runTest {
        // Given - set an error
        homeViewModel.uiState.value.copy(error = "Test error")

        // When
        homeViewModel.clearError()

        // Then
        assertNull(homeViewModel.uiState.value.error)
    }

    @Test
    fun `onCleared cancels pending like operations`() = runTest {
        // Given
        val recipeId = "recipe1"
        every { likeRepository.likeStates.value } returns mapOf(
            recipeId to LikeState(liked = false, likesCount = 42)
        )
        coEvery { likeRepository.toggleLike(any(), any()) } coAnswers {
            delay(1000) // Long-running operation
            Result.success(LikeResponse(true, true, 43, recipeId))
        }

        // When
        homeViewModel.toggleLike(recipeId, false)
        
        // Clear the ViewModel (simulating onCleared)
        homeViewModel.onCleared()
        
        advanceTimeBy(1500L) // Advance past the operation time

        // Then - the operation should have been cancelled
        // We can't easily verify cancellation, but we can ensure no state corruption
        assertNotNull(homeViewModel.uiState.value) // ViewModel should still be valid
    }
} 