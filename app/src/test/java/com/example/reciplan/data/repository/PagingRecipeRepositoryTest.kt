package com.example.reciplan.data.repository

import app.cash.turbine.test
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.*
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class PagingRecipeRepositoryTest {

    private lateinit var recipeApi: RecipeApi
    private lateinit var likeRepository: LikeRepository
    private lateinit var pagingRecipeRepository: PagingRecipeRepository

    @Before
    fun setup() {
        recipeApi = mockk()
        likeRepository = mockk()
        pagingRecipeRepository = PagingRecipeRepository(recipeApi, likeRepository)
    }

    @Test
    fun `preloadLikeStates initializes cache with API values when not already cached`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1",
                title = "Test Recipe",
                userId = "user1",
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z",
                liked = true,
                likesCount = 5
            )
        )

        every { likeRepository.likeStates.value } returns emptyMap()
        every { likeRepository.updateLikeStateOptimistically(any(), any(), any()) } just Runs

        // When
        pagingRecipeRepository.preloadLikeStates(recipes)

        // Then
        verify { likeRepository.updateLikeStateOptimistically("1", true, 5) }
    }

    @Test
    fun `preloadLikeStates skips recipes already in cache`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1",
                title = "Test Recipe",
                userId = "user1",
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z",
                liked = true,
                likesCount = 5
            )
        )

        val existingState = mapOf("1" to LikeState(liked = false, likesCount = 3))
        every { likeRepository.likeStates.value } returns existingState

        // When
        pagingRecipeRepository.preloadLikeStates(recipes)

        // Then
        verify(exactly = 0) { likeRepository.updateLikeStateOptimistically(any(), any(), any()) }
    }

    @Test
    fun `updateRecipeLikeOptimistically delegates to likeRepository`() = runTest {
        // Given
        every { likeRepository.updateLikeStateOptimistically(any(), any(), any()) } just Runs

        // When
        pagingRecipeRepository.updateRecipeLikeOptimistically("recipe1", true, 10)

        // Then
        verify { likeRepository.updateLikeStateOptimistically("recipe1", true, 10) }
    }

    @Test
    fun `resolveRecipeLikeConflict refreshes state from server`() = runTest {
        // Given
        coEvery { likeRepository.refreshLikeState(any()) } returns Result.success(
            LikedStatusResponse(liked = true, likesCount = 8, recipeId = "recipe1")
        )

        // When
        pagingRecipeRepository.resolveRecipeLikeConflict("recipe1")

        // Then
        coVerify { likeRepository.refreshLikeState("recipe1") }
    }
} 