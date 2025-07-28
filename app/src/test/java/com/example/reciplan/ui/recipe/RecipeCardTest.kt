package com.example.reciplan.ui.recipe

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.repository.LikeState
import com.example.reciplan.ui.theme.ReciplanTheme
import org.junit.Rule
import org.junit.Test

class RecipeCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recipeCard_displaysLikeButton() {
        val testRecipe = Recipe(
            id = "test-recipe",
            title = "Test Recipe",
            description = "A test recipe",
            userId = "user1",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val likeState = LikeState(
            liked = false,
            likesCount = 42,
            isLoading = false
        )

        composeTestRule.setContent {
            ReciplanTheme {
                RecipeCard(
                    recipe = testRecipe,
                    likeState = likeState,
                    onRecipeClick = {},
                    onLikeClick = { _, _ -> }
                )
            }
        }

        // Should show the recipe title
        composeTestRule.onNodeWithText("Test Recipe").assertExists()
        
        // Should show like count when > 0
        composeTestRule.onNodeWithText("42 likes").assertExists()
        
        // Should have a like button
        composeTestRule.onNodeWithContentDescription("Like. Currently 42 likes").assertExists()
    }

    @Test
    fun recipeCard_hidesLikeCountWhenZero() {
        val testRecipe = Recipe(
            id = "test-recipe",
            title = "Test Recipe",
            userId = "user1",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val likeState = LikeState(
            liked = false,
            likesCount = 0,
            isLoading = false
        )

        composeTestRule.setContent {
            ReciplanTheme {
                RecipeCard(
                    recipe = testRecipe,
                    likeState = likeState,
                    onRecipeClick = {},
                    onLikeClick = { _, _ -> }
                )
            }
        }

        // Should not show like count when 0
        composeTestRule.onNodeWithText("0 likes").assertDoesNotExist()
        composeTestRule.onNodeWithText("likes").assertDoesNotExist()
    }

    @Test
    fun recipeCard_triggersLikeCallback() {
        val testRecipe = Recipe(
            id = "test-recipe",
            title = "Test Recipe",
            userId = "user1",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val likeState = LikeState(
            liked = false,
            likesCount = 5,
            isLoading = false
        )

        var likeClicked = false
        var clickedRecipeId = ""
        var clickedCurrentlyLiked = true

        composeTestRule.setContent {
            ReciplanTheme {
                RecipeCard(
                    recipe = testRecipe,
                    likeState = likeState,
                    onRecipeClick = {},
                    onLikeClick = { recipeId, currentlyLiked ->
                        likeClicked = true
                        clickedRecipeId = recipeId
                        clickedCurrentlyLiked = currentlyLiked
                    }
                )
            }
        }

        // Click the like button
        composeTestRule.onNodeWithContentDescription("Like. Currently 5 likes").performClick()

        // Verify the callback was triggered with correct parameters
        assert(likeClicked)
        assert(clickedRecipeId == "test-recipe")
        assert(clickedCurrentlyLiked == false) // Should pass the current state
    }

    @Test
    fun recipeCard_showsErrorState() {
        val testRecipe = Recipe(
            id = "test-recipe",
            title = "Test Recipe",
            userId = "user1",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val likeState = LikeState(
            liked = false,
            likesCount = 5,
            isLoading = false,
            error = "Failed to like recipe"
        )

        composeTestRule.setContent {
            ReciplanTheme {
                RecipeCard(
                    recipe = testRecipe,
                    likeState = likeState,
                    onRecipeClick = {},
                    onLikeClick = { _, _ -> }
                )
            }
        }

        // Should show the error message
        composeTestRule.onNodeWithText("Failed to like recipe").assertExists()
    }
} 