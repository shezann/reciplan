package com.example.reciplan.ui.draft

import com.example.reciplan.data.model.*
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.repository.IngestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class DraftPreviewViewModelTest {

    @Mock
    private lateinit var mockIngestRepository: IngestRepository

    @Mock
    private lateinit var mockRecipeRepository: RecipeRepository

    private lateinit var viewModel: DraftPreviewViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DraftPreviewViewModel(mockIngestRepository, mockRecipeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Test data helpers
    private fun createTestState(
        title: String = "Test Recipe",
        description: String = "Test Description",
        prepTime: Int = 30,
        cookTime: Int = 45,
        servings: Int = 4,
        difficulty: Int = 2,
        ingredients: List<EditableIngredient> = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"),
            EditableIngredient(id = "2", name = "Sugar", quantity = "1 cup")
        ),
        instructions: List<EditableInstruction> = listOf(
            EditableInstruction(id = "1", text = "Mix ingredients", stepNumber = 1),
            EditableInstruction(id = "2", text = "Bake for 30 minutes", stepNumber = 2)
        ),
        tags: List<String> = listOf("dessert", "easy"),
        hasUnsavedChanges: Boolean = false
    ): DraftPreviewUiState {
        return DraftPreviewUiState(
            isLoading = false,
            recipeId = "recipe123",
            title = title,
            description = description,
            prepTime = prepTime,
            cookTime = cookTime,
            servings = servings,
            difficulty = difficulty,
            ingredients = ingredients,
            instructions = instructions,
            tags = tags,
            hasUnsavedChanges = hasUnsavedChanges,
            // Set original values to match current values initially
            originalTitle = title,
            originalDescription = description,
            originalPrepTime = prepTime,
            originalCookTime = cookTime,
            originalServings = servings,
            originalDifficulty = difficulty,
            originalIngredients = ingredients,
            originalInstructions = instructions,
            originalTags = tags
        )
    }

    private fun setViewModelState(state: DraftPreviewUiState) {
        // Access the private _uiState field using reflection for testing
        val field = DraftPreviewViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val mutableStateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<DraftPreviewUiState>
        mutableStateFlow.value = state
    }

    // JSON Patch Generation Tests
    @Test
    fun `generateJsonPatch returns null when no changes made`() = runTest {
        val state = createTestState(hasUnsavedChanges = false)
        setViewModelState(state)

        val patch = viewModel.generateJsonPatch()

        assertNull("Should return null when no changes are made", patch)
    }

    @Test
    fun `generateJsonPatch includes title when changed`() = runTest {
        val originalState = createTestState()
        val modifiedState = originalState.copy(
            title = "Modified Recipe Title",
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when changes exist", patch)
        assertEquals("Modified Recipe Title", patch!!.getValue("title").toString().removeSurrounding("\""))
        assertFalse("Should not include unchanged description", patch.containsKey("description"))
    }

    @Test
    fun `generateJsonPatch includes multiple basic field changes`() = runTest {
        val originalState = createTestState()
        val modifiedState = originalState.copy(
            title = "New Title",
            description = "New Description",
            prepTime = 60,
            cookTime = 90,
            servings = 6,
            difficulty = 3,
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when changes exist", patch)
        assertEquals("New Title", patch!!.getValue("title").toString().removeSurrounding("\""))
        assertEquals("New Description", patch.getValue("description").toString().removeSurrounding("\""))
        assertEquals("60", patch.getValue("prep_time").toString())
        assertEquals("90", patch.getValue("cook_time").toString())
        assertEquals("6", patch.getValue("servings").toString())
        assertEquals("3", patch.getValue("difficulty").toString())
    }

    @Test
    fun `generateJsonPatch includes ingredients when changed`() = runTest {
        val originalIngredients = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"),
            EditableIngredient(id = "2", name = "Sugar", quantity = "1 cup")
        )
        val modifiedIngredients = listOf(
            EditableIngredient(id = "1", name = "Bread Flour", quantity = "3 cups"),
            EditableIngredient(id = "2", name = "Brown Sugar", quantity = "1.5 cups"),
            EditableIngredient(id = "3", name = "Vanilla", quantity = "1 tsp")
        )
        
        val originalState = createTestState(ingredients = originalIngredients)
        val modifiedState = originalState.copy(
            ingredients = modifiedIngredients,
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when ingredients changed", patch)
        assertTrue("Should include ingredients array", patch!!.containsKey("ingredients"))
        
        val ingredientsArray = patch.getValue("ingredients").toString()
        assertTrue("Should contain modified ingredient names", 
            ingredientsArray.contains("Bread Flour") && 
            ingredientsArray.contains("Brown Sugar") && 
            ingredientsArray.contains("Vanilla"))
    }

    @Test
    fun `generateJsonPatch includes instructions when changed`() = runTest {
        val originalInstructions = listOf(
            EditableInstruction(id = "1", text = "Mix ingredients", stepNumber = 1),
            EditableInstruction(id = "2", text = "Bake for 30 minutes", stepNumber = 2)
        )
        val modifiedInstructions = listOf(
            EditableInstruction(id = "1", text = "Carefully mix all dry ingredients", stepNumber = 1),
            EditableInstruction(id = "2", text = "Bake at 350°F for 35 minutes", stepNumber = 2),
            EditableInstruction(id = "3", text = "Cool before serving", stepNumber = 3)
        )
        
        val originalState = createTestState(instructions = originalInstructions)
        val modifiedState = originalState.copy(
            instructions = modifiedInstructions,
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when instructions changed", patch)
        assertTrue("Should include instructions array", patch!!.containsKey("instructions"))
        
        val instructionsArray = patch.getValue("instructions").toString()
        assertTrue("Should contain modified instruction text", 
            instructionsArray.contains("Carefully mix all dry ingredients") && 
            instructionsArray.contains("350°F") && 
            instructionsArray.contains("Cool before serving"))
    }

    @Test
    fun `generateJsonPatch includes tags when changed`() = runTest {
        val originalTags = listOf("dessert", "easy")
        val modifiedTags = listOf("dessert", "intermediate", "chocolate")
        
        val originalState = createTestState(tags = originalTags)
        val modifiedState = originalState.copy(
            tags = modifiedTags,
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when tags changed", patch)
        assertTrue("Should include tags array", patch!!.containsKey("tags"))
        
        val tagsArray = patch.getValue("tags").toString()
        assertTrue("Should contain new tags", 
            tagsArray.contains("intermediate") && tagsArray.contains("chocolate"))
    }

    // Validation Tests
    @Test
    fun `hasIngredientsChanged returns false for identical ingredients`() = runTest {
        val ingredients1 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"),
            EditableIngredient(id = "2", name = "Sugar", quantity = "1 cup")
        )
        val ingredients2 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"),
            EditableIngredient(id = "2", name = "Sugar", quantity = "1 cup")
        )

        // Use reflection to access private method
        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasIngredientsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true
        val result = method.invoke(viewModel, ingredients1, ingredients2) as Boolean

        assertFalse("Should return false for identical ingredients", result)
    }

    @Test
    fun `hasIngredientsChanged returns true for different ingredient names`() = runTest {
        val ingredients1 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups")
        )
        val ingredients2 = listOf(
            EditableIngredient(id = "1", name = "Sugar", quantity = "2 cups")
        )

        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasIngredientsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true
        val result = method.invoke(viewModel, ingredients1, ingredients2) as Boolean

        assertTrue("Should return true for different ingredient names", result)
    }

    @Test
    fun `hasIngredientsChanged returns true for different ingredient quantities`() = runTest {
        val ingredients1 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups")
        )
        val ingredients2 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "3 cups")
        )

        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasIngredientsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true
        val result = method.invoke(viewModel, ingredients1, ingredients2) as Boolean

        assertTrue("Should return true for different ingredient quantities", result)
    }

    @Test
    fun `hasIngredientsChanged returns true for different list sizes`() = runTest {
        val ingredients1 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups")
        )
        val ingredients2 = listOf(
            EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"),
            EditableIngredient(id = "2", name = "Sugar", quantity = "1 cup")
        )

        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasIngredientsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true
        val result = method.invoke(viewModel, ingredients1, ingredients2) as Boolean

        assertTrue("Should return true for different list sizes", result)
    }

    @Test
    fun `hasInstructionsChanged returns false for identical instructions`() = runTest {
        val instructions1 = listOf(
            EditableInstruction(id = "1", text = "Mix ingredients", stepNumber = 1),
            EditableInstruction(id = "2", text = "Bake for 30 minutes", stepNumber = 2)
        )
        val instructions2 = listOf(
            EditableInstruction(id = "1", text = "Mix ingredients", stepNumber = 1),
            EditableInstruction(id = "2", text = "Bake for 30 minutes", stepNumber = 2)
        )

        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasInstructionsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true
        val result = method.invoke(viewModel, instructions1, instructions2) as Boolean

        assertFalse("Should return false for identical instructions", result)
    }

    @Test
    fun `hasInstructionsChanged returns true for different instruction text`() = runTest {
        val instructions1 = listOf(
            EditableInstruction(id = "1", text = "Mix ingredients", stepNumber = 1)
        )
        val instructions2 = listOf(
            EditableInstruction(id = "1", text = "Carefully mix ingredients", stepNumber = 1)
        )

        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasInstructionsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true
        val result = method.invoke(viewModel, instructions1, instructions2) as Boolean

        assertTrue("Should return true for different instruction text", result)
    }

    // Save Draft Error Handling Tests
    @Test
    fun `saveRecipe succeeds when repository succeeds`() = runTest {
        val mockRecipe = Recipe(
            id = "recipe123",
            title = "Test Recipe",
            description = "Test Description",
            prepTime = 30,
            cookTime = 45,
            servings = 4,
            difficulty = 2,
            ingredients = emptyList(),
            instructions = emptyList(),
            tags = emptyList(),
            userId = "user123",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z"
        )

        val state = createTestState(hasUnsavedChanges = true, title = "Modified Title")
        setViewModelState(state)

        whenever(mockRecipeRepository.saveDraft(any(), any()))
            .thenReturn(Result.success(mockRecipe))

        viewModel.saveRecipe("recipe123")

        val uiState = viewModel.uiState.first()
        assertFalse("Should clear isSaving flag", uiState.isSaving)
        assertNull("Should clear save error", uiState.saveError)
        assertFalse("Should mark as saved after successful save", uiState.hasUnsavedChanges)
        
        // Verify repository was called with correct parameters
        verify(mockRecipeRepository).saveDraft(eq("recipe123"), any())
    }

    @Test
    fun `saveRecipe handles repository failure`() = runTest {
        val errorMessage = "Network connection failed"
        val state = createTestState(hasUnsavedChanges = true, title = "Modified Title")
        setViewModelState(state)

        whenever(mockRecipeRepository.saveDraft(any(), any()))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.saveRecipe("recipe123")

        val uiState = viewModel.uiState.first()
        assertFalse("Should clear isSaving flag", uiState.isSaving)
        assertEquals("Should set save error", errorMessage, uiState.saveError)
        assertTrue("Should keep hasUnsavedChanges on failure", uiState.hasUnsavedChanges)
    }

    @Test
    fun `saveRecipe handles repository exception`() = runTest {
        val state = createTestState(hasUnsavedChanges = true, title = "Modified Title")
        setViewModelState(state)

        whenever(mockRecipeRepository.saveDraft(any(), any()))
            .thenThrow(RuntimeException("Unexpected error"))

        viewModel.saveRecipe("recipe123")

        val uiState = viewModel.uiState.first()
        assertFalse("Should clear isSaving flag", uiState.isSaving)
        assertNotNull("Should set save error", uiState.saveError)
        assertTrue("Should contain error message", uiState.saveError!!.contains("Unexpected error"))
    }

    @Test
    fun `saveRecipe skips repository call when no changes to save`() = runTest {
        val state = createTestState(hasUnsavedChanges = false)
        setViewModelState(state)

        viewModel.saveRecipe("recipe123")

        val uiState = viewModel.uiState.first()
        assertFalse("Should clear isSaving flag", uiState.isSaving)
        assertNull("Should not set save error for no changes", uiState.saveError)
        
        // Verify repository was not called when no changes
        verify(mockRecipeRepository, never()).saveDraft(any(), any())
    }

    @Test
    fun `saveRecipe sets loading state during save operation`() = runTest {
        val mockRecipe = Recipe(
            id = "recipe123",
            title = "Test Recipe",
            description = "Test Description",
            prepTime = 30,
            cookTime = 45,
            servings = 4,
            difficulty = 2,
            ingredients = emptyList(),
            instructions = emptyList(),
            tags = emptyList(),
            userId = "user123",
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z"
        )

        val state = createTestState(hasUnsavedChanges = true, title = "Modified Title")
        setViewModelState(state)

        whenever(mockRecipeRepository.saveDraft(any(), any()))
            .thenReturn(Result.success(mockRecipe))

        // Set initial state to verify loading starts
        viewModel.saveRecipe("recipe123")

        // The final state should have isSaving = false after completion
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be saving after completion", finalState.isSaving)
    }

    // State Management Tests
    @Test
    fun `markAsSaved resets original values to current values`() = runTest {
        val modifiedState = createTestState(
            title = "Modified Title",
            description = "Modified Description",
            prepTime = 60,
            ingredients = listOf(
                EditableIngredient(id = "1", name = "Modified Flour", quantity = "3 cups")
            ),
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        viewModel.markAsSaved()

        val state = viewModel.uiState.first()
        assertFalse("Should clear hasUnsavedChanges flag", state.hasUnsavedChanges)
        assertEquals("Should update original title", "Modified Title", state.originalTitle)
        assertEquals("Should update original description", "Modified Description", state.originalDescription)
        assertEquals("Should update original prep time", 60, state.originalPrepTime)
        assertEquals("Should update original ingredients", "Modified Flour", state.originalIngredients[0].name)
        assertEquals("Should update original ingredients quantity", "3 cups", state.originalIngredients[0].quantity)
    }

    // Edge Cases and Validation
    @Test
    fun `generateJsonPatch handles empty ingredient list`() = runTest {
        val originalState = createTestState(
            ingredients = listOf(EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"))
        )
        val modifiedState = originalState.copy(
            ingredients = emptyList(),
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when ingredients removed", patch)
        assertTrue("Should include empty ingredients array", patch!!.containsKey("ingredients"))
    }

    @Test
    fun `generateJsonPatch handles empty instruction list`() = runTest {
        val originalState = createTestState(
            instructions = listOf(EditableInstruction(id = "1", text = "Mix ingredients", stepNumber = 1))
        )
        val modifiedState = originalState.copy(
            instructions = emptyList(),
            hasUnsavedChanges = true
        )
        setViewModelState(modifiedState)

        val patch = viewModel.generateJsonPatch()

        assertNotNull("Should return patch when instructions removed", patch)
        assertTrue("Should include empty instructions array", patch!!.containsKey("instructions"))
    }

    @Test
    fun `hasIngredientsChanged handles empty lists correctly`() = runTest {
        val emptyList1 = emptyList<EditableIngredient>()
        val emptyList2 = emptyList<EditableIngredient>()
        val nonEmptyList = listOf(EditableIngredient(id = "1", name = "Flour", quantity = "2 cups"))

        val method = DraftPreviewViewModel::class.java.getDeclaredMethod(
            "hasIngredientsChanged", 
            List::class.java, 
            List::class.java
        )
        method.isAccessible = true

        val result1 = method.invoke(viewModel, emptyList1, emptyList2) as Boolean
        val result2 = method.invoke(viewModel, emptyList1, nonEmptyList) as Boolean
        val result3 = method.invoke(viewModel, nonEmptyList, emptyList1) as Boolean

        assertFalse("Should return false for two empty lists", result1)
        assertTrue("Should return true for empty vs non-empty", result2)
        assertTrue("Should return true for non-empty vs empty", result3)
    }
} 