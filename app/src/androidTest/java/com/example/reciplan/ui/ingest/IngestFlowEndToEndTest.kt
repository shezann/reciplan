package com.example.reciplan.ui.ingest

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.reciplan.ReciplanApplication
import com.example.reciplan.data.model.IngestJobDto
import com.example.reciplan.data.model.IngestStatus
import com.example.reciplan.ui.theme.ReciplanTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IngestFlowEndToEndTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: AddFromTikTokViewModel
    private lateinit var viewModelFactory: ViewModelProvider.Factory

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as ReciplanApplication
        val appContainer = application.appContainer
        
        // Create ViewModel factory
        viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AddFromTikTokViewModel(appContainer.ingestRepository) as T
            }
        }
        
        // Clear any existing shared instance
        AddFromTikTokViewModel.clearSharedInstance()
        
        // Get fresh ViewModel instance
        viewModel = AddFromTikTokViewModel(appContainer.ingestRepository)
    }

    @Test
    fun fullIngestFlow_progressesThroughAllSteps_toCompletion() {
        // Start with IngestStatusScreen
        composeTestRule.setContent {
            ReciplanTheme {
                IngestStatusScreen(
                    jobId = "test-job-123",
                    onNavigateBack = { },
                    onNavigateToDraftPreview = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }

        // Verify initial state - should show header
        composeTestRule.onNodeWithText("Creating Recipe from TikTok").assertExists()
        composeTestRule.onNodeWithText("We're processing your video to extract recipe information").assertExists()

        // Test progression through all statuses
        val statusSequence = listOf(
            IngestStatus.QUEUED,
            IngestStatus.DOWNLOADING,
            IngestStatus.EXTRACTING,
            IngestStatus.TRANSCRIBING,
            IngestStatus.DRAFT_TRANSCRIBED,
            IngestStatus.OCRING,
            IngestStatus.OCR_DONE,
            IngestStatus.LLM_REFINING,
            IngestStatus.DRAFT_PARSED,
            IngestStatus.COMPLETED
        )

        statusSequence.forEachIndexed { index, status ->
            // Update ViewModel with current status
            val jobDto = createJobDtoForStatus(status, index + 1)
            
            composeTestRule.runOnUiThread {
                viewModel.updateJobStatus(jobDto)
            }

            // Wait for UI to update
            composeTestRule.waitForIdle()

            // Verify progress text shows correct step
            if (status != IngestStatus.COMPLETED) {
                composeTestRule.onNodeWithText("Step ${index + 1} of 10").assertExists()
            } else {
                composeTestRule.onNodeWithText("Complete!").assertExists()
            }

            // Verify step title is displayed
            val expectedTitle = getExpectedTitleForStatus(status)
            composeTestRule.onNodeWithText(expectedTitle).assertExists()

            // Verify progress stepper shows correct state
            verifyProgressStepperState(status, index + 1)

            // For non-terminal states, verify loader is shown
            if (status != IngestStatus.COMPLETED && status != IngestStatus.FAILED) {
                // Verify animated loader is present (check for refresh icon)
                composeTestRule.onNodeWithContentDescription("Loading").assertExists()
            }
        }

        // Verify completion state
        composeTestRule.onNodeWithText("Recipe created successfully!").assertExists()
        composeTestRule.onNodeWithText("Redirecting to recipe preview...").assertExists()
        composeTestRule.onNodeWithContentDescription("Completed").assertExists()
    }

    @Test
    fun ingestFlow_handlesErrorState_correctly() {
        composeTestRule.setContent {
            ReciplanTheme {
                IngestStatusScreen(
                    jobId = "error-job-123",
                    onNavigateBack = { },
                    onNavigateToDraftPreview = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }

        // Simulate progression to error state
        val jobDto = IngestJobDto(
            jobId = "error-job-123",
            status = IngestStatus.FAILED,
            title = "Failed Recipe",
            errorCode = com.example.reciplan.data.model.IngestErrorCode.ASR_FAILED
        )

        composeTestRule.runOnUiThread {
            viewModel.updateJobStatus(jobDto)
        }

        composeTestRule.waitForIdle()

        // Verify error state UI
        composeTestRule.onNodeWithText("Processing failed").assertExists()
        composeTestRule.onNodeWithText("Failed").assertExists()
        composeTestRule.onNodeWithContentDescription("Error").assertExists()

        // Verify progress stepper shows error state
        // The stepper should show error indicators for remaining steps
        verifyProgressStepperErrorState()
    }

    @Test
    fun progressStepper_animatesCorrectly_throughAllSteps() {
        composeTestRule.setContent {
            ReciplanTheme {
                ProgressStepper(
                    currentStep = 1,
                    totalSteps = 10,
                    isComplete = false,
                    hasError = false
                )
            }
        }

        // Verify initial state
        composeTestRule.onNodeWithText("1").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("7").assertExists()
        composeTestRule.onNodeWithText("10").assertExists()

        // Test progression through steps
        for (step in 2..10) {
            composeTestRule.setContent {
                ReciplanTheme {
                    ProgressStepper(
                        currentStep = step,
                        totalSteps = 10,
                        isComplete = step == 10,
                        hasError = false
                    )
                }
            }

            composeTestRule.waitForIdle()

            // Verify milestone steps show correct states
            when {
                step >= 10 -> {
                    // All steps should show completion
                    composeTestRule.onNodeWithContentDescription("Step 1 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 3 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 5 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 7 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 10 complete").assertExists()
                }
                step >= 7 -> {
                    composeTestRule.onNodeWithContentDescription("Step 1 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 3 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 5 complete").assertExists()
                }
                step >= 5 -> {
                    composeTestRule.onNodeWithContentDescription("Step 1 complete").assertExists()
                    composeTestRule.onNodeWithContentDescription("Step 3 complete").assertExists()
                }
                step >= 3 -> {
                    composeTestRule.onNodeWithContentDescription("Step 1 complete").assertExists()
                }
            }
        }
    }

    @Test
    fun ingestFlow_handlesStatusTransitions_smoothly() = runBlocking {
        composeTestRule.setContent {
            ReciplanTheme {
                IngestStatusScreen(
                    jobId = "transition-test-123",
                    onNavigateBack = { },
                    onNavigateToDraftPreview = { },
                    viewModelFactory = viewModelFactory
                )
            }
        }

        // Test rapid status transitions
        val rapidTransitions = listOf(
            IngestStatus.QUEUED,
            IngestStatus.DOWNLOADING,
            IngestStatus.EXTRACTING,
            IngestStatus.TRANSCRIBING,
            IngestStatus.LLM_REFINING,
            IngestStatus.COMPLETED
        )

        rapidTransitions.forEachIndexed { index, status ->
            val jobDto = createJobDtoForStatus(status, getStepForStatus(status))
            
            composeTestRule.runOnUiThread {
                viewModel.updateJobStatus(jobDto)
            }

            // Small delay to allow animations
            delay(100)
            composeTestRule.waitForIdle()

            // Verify the UI updated correctly
            val expectedTitle = getExpectedTitleForStatus(status)
            composeTestRule.onNodeWithText(expectedTitle).assertExists()
        }
    }

    @Test
    fun stepIndicator_showsCorrectAnimations_forCurrentStep() {
        // Test current step animation
        composeTestRule.setContent {
            ReciplanTheme {
                StepIndicator(
                    stepNumber = 5,
                    isActive = true,
                    isComplete = false,
                    hasError = false,
                    isCurrentStep = true
                )
            }
        }

        // Current step should not show the number (it shows pulsing animation)
        composeTestRule.onNodeWithText("5").assertDoesNotExist()
        
        // Should not show completion or error icons
        composeTestRule.onNodeWithContentDescription("Step 5 complete").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Step 5 error").assertDoesNotExist()
    }

    // Helper functions
    private fun createJobDtoForStatus(status: IngestStatus, step: Int): IngestJobDto {
        return IngestJobDto(
            jobId = "test-job-123",
            recipeId = if (status == IngestStatus.COMPLETED) "recipe-456" else null,
            status = status,
            title = "Test Recipe",
            transcript = "Test transcript",
            errorCode = null,
            recipeJson = if (status == IngestStatus.COMPLETED) {
                buildJsonObject {
                    put("title", JsonPrimitive("Test Recipe"))
                    put("ingredients", JsonPrimitive("Test ingredients"))
                }
            } else null,
            onscreenText = "Test onscreen text",
            ingredientCandidates = listOf("ingredient1", "ingredient2"),
            parseErrors = emptyList(),
            llmErrorMessage = null
        )
    }

    private fun getExpectedTitleForStatus(status: IngestStatus): String {
        return when (status) {
            IngestStatus.QUEUED -> "Queued"
            IngestStatus.DOWNLOADING -> "Downloading"
            IngestStatus.EXTRACTING -> "Extracting"
            IngestStatus.TRANSCRIBING -> "Transcribing"
            IngestStatus.DRAFT_TRANSCRIBED -> "Transcription Complete"
            IngestStatus.OCRING -> "Reading Text"
            IngestStatus.OCR_DONE -> "Text Extraction Complete"
            IngestStatus.LLM_REFINING -> "AI Processing"
            IngestStatus.DRAFT_PARSED -> "Recipe Generated"
            IngestStatus.DRAFT_PARSED_WITH_ERRORS -> "Recipe Generated (with warnings)"
            IngestStatus.COMPLETED -> "Complete"
            IngestStatus.FAILED -> "Failed"
        }
    }

    private fun getStepForStatus(status: IngestStatus): Int {
        return when (status) {
            IngestStatus.QUEUED -> 1
            IngestStatus.DOWNLOADING -> 2
            IngestStatus.EXTRACTING -> 3
            IngestStatus.TRANSCRIBING -> 4
            IngestStatus.DRAFT_TRANSCRIBED -> 5
            IngestStatus.OCRING -> 6
            IngestStatus.OCR_DONE -> 7
            IngestStatus.LLM_REFINING -> 8
            IngestStatus.DRAFT_PARSED -> 9
            IngestStatus.DRAFT_PARSED_WITH_ERRORS -> 9
            IngestStatus.COMPLETED -> 10
            IngestStatus.FAILED -> 0
        }
    }

    private fun verifyProgressStepperState(status: IngestStatus, currentStep: Int) {
        // Verify milestone steps show correct completion state
        val milestoneSteps = listOf(1, 3, 5, 7, 10)
        
        milestoneSteps.forEach { milestone ->
            when {
                status == IngestStatus.COMPLETED && milestone <= 10 -> {
                    // All steps should be complete
                    composeTestRule.onNodeWithContentDescription("Step $milestone complete").assertExists()
                }
                currentStep > milestone -> {
                    // Completed steps should show check mark
                    composeTestRule.onNodeWithContentDescription("Step $milestone complete").assertExists()
                }
                currentStep == milestone -> {
                    // Current step should show number or be active
                    // (Current step animation doesn't show number, so we don't assert text exists)
                }
                else -> {
                    // Future steps should show numbers
                    composeTestRule.onNodeWithText(milestone.toString()).assertExists()
                }
            }
        }
    }

    private fun verifyProgressStepperErrorState() {
        // In error state, remaining steps should show error indicators
        val milestoneSteps = listOf(1, 3, 5, 7, 10)
        
        milestoneSteps.forEach { milestone ->
            // Error state should show close icons for all steps
            composeTestRule.onNodeWithContentDescription("Step $milestone error").assertExists()
        }
    }
} 