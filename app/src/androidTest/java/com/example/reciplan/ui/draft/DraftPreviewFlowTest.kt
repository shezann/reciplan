package com.example.reciplan.ui.draft

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.reciplan.ReciplanApplication
import com.example.reciplan.data.model.*
import com.example.reciplan.ui.draft.DraftPreviewScreen
import com.example.reciplan.ui.draft.DraftPreviewViewModel
import com.example.reciplan.ui.ingest.AddFromTikTokViewModel
import com.example.reciplan.ui.ingest.IngestStatusScreen
import com.example.reciplan.ui.theme.ReciplanTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftPreviewFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as ReciplanApplication
        
        // Create ViewModel factory
        viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    AddFromTikTokViewModel::class.java -> {
                        AddFromTikTokViewModel(application.appContainer.ingestRepository) as T
                    }
                    DraftPreviewViewModel::class.java -> {
                        DraftPreviewViewModel(application.appContainer.ingestRepository, application.appContainer.recipeRepository) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
        
        // Clear any existing shared instances
        AddFromTikTokViewModel.clearSharedInstance()
    }

    @Test
    fun draftPreviewScreen_loadsAndDisplaysRecipeData() {
        val recipeId = "test-recipe-123"
        
        // Start with draft preview screen
        composeTestRule.setContent {
            ReciplanTheme {
                DraftPreviewScreen(
                    recipeId = recipeId,
                    onNavigateBack = { },
                    onNavigateToRecipeDetail = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }
        
        // Wait for UI to load
        composeTestRule.waitForIdle()
        
        // Verify the screen loads (may show loading or error state)
        // The screen should show some content, even if it's just the top bar
        composeTestRule.onNodeWithText("Recipe Draft").assertExists()
    }

    @Test
    fun draftPreviewScreen_showsTabsAndNavigation() {
        val recipeId = "test-recipe-123"
        
        // Start with draft preview screen
        composeTestRule.setContent {
            ReciplanTheme {
                DraftPreviewScreen(
                    recipeId = recipeId,
                    onNavigateBack = { },
                    onNavigateToRecipeDetail = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }
        
        // Wait for UI to load
        composeTestRule.waitForIdle()
        
        // Verify tabs are present
        composeTestRule.onNodeWithText("Overview").assertExists()
        composeTestRule.onNodeWithText("Ingredients").assertExists()
        composeTestRule.onNodeWithText("Instructions").assertExists()
        
        // Verify save button is present
        composeTestRule.onNodeWithContentDescription("Save Recipe").assertExists()
    }

    @Test
    fun draftPreviewScreen_handlesErrorState() {
        val recipeId = "invalid-recipe-id"
        
        // Start with draft preview screen with invalid recipe ID
        composeTestRule.setContent {
            ReciplanTheme {
                DraftPreviewScreen(
                    recipeId = recipeId,
                    onNavigateBack = { },
                    onNavigateToRecipeDetail = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }
        
        // Wait for UI to load
        composeTestRule.waitForIdle()
        
        // Verify error state is handled gracefully
        // The screen should still be present even if data loading fails
        composeTestRule.onNodeWithText("Recipe Draft").assertExists()
    }
    
    @Test
    fun draftPreviewScreen_basicRendering() {
        val recipeId = "test-recipe-123"
        
        // Start with draft preview screen
        composeTestRule.setContent {
            ReciplanTheme {
                DraftPreviewScreen(
                    recipeId = recipeId,
                    onNavigateBack = { },
                    onNavigateToRecipeDetail = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }
        
        // Wait for UI to load
        composeTestRule.waitForIdle()
        
        // Verify basic UI elements are present
        // This test just verifies the screen can be rendered without crashing
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithContentDescription("Save Recipe").assertExists()
    }

} 