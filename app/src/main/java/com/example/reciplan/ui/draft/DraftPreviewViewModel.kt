package com.example.reciplan.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.model.*
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.repository.IngestRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.example.reciplan.ui.ingest.AddFromTikTokViewModel

// Navigation events for the draft preview screen
sealed class DraftPreviewNavigationEvent {
    data class NavigateToRecipeDetail(val recipeId: String) : DraftPreviewNavigationEvent()
    data class ShowSaveSuccess(val message: String) : DraftPreviewNavigationEvent()
    data class ShowSaveError(val error: String) : DraftPreviewNavigationEvent()
}

// UI State models for editable recipe data
data class DraftPreviewUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val recipeId: String = "",
    val title: String = "",
    val description: String = "",
    val prepTime: Int = 0,
    val cookTime: Int = 0,
    val servings: Int = 1,
    val difficulty: Int = 1,
    val ingredients: List<EditableIngredient> = emptyList(),
    val instructions: List<EditableInstruction> = emptyList(),
    val sourceUrl: String? = null,
    val sourcePlatform: String? = null,
    val tiktokAuthor: String? = null,
    val videoThumbnail: String? = null,
    val tags: List<String> = emptyList(),
    val transcript: String? = null,
    val onscreenText: String? = null,
    val ingredientCandidates: List<String> = emptyList(),
    val hasUnsavedChanges: Boolean = false,
    val lastSaveType: String? = null, // "approve" or "save" - tracks the type of last save operation
    // Original data for patch generation
    val originalTitle: String = "",
    val originalDescription: String = "",
    val originalPrepTime: Int = 0,
    val originalCookTime: Int = 0,
    val originalServings: Int = 1,
    val originalDifficulty: Int = 1,
    val originalIngredients: List<EditableIngredient> = emptyList(),
    val originalInstructions: List<EditableInstruction> = emptyList(),
    val originalTags: List<String> = emptyList()
)

data class EditableIngredient(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val isEditing: Boolean = false
)

data class EditableInstruction(
    val id: String = "",
    val text: String = "",
    val stepNumber: Int = 1,
    val isEditing: Boolean = false
)

class DraftPreviewViewModel(
    private val ingestRepository: IngestRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    
    // Navigation events channel
    private val _navigationEvents = Channel<DraftPreviewNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    private val _uiState = MutableStateFlow(DraftPreviewUiState())
    val uiState: StateFlow<DraftPreviewUiState> = _uiState.asStateFlow()
    
    /**
     * Load recipe data by recipe ID
     * First tries to fetch from recipe detail API (for completed recipes)
     * Falls back to ingest job data (for drafts still in progress)
     */
    fun loadRecipeData(recipeId: String) {
        loadRecipeDataInternal(recipeId, respectUnsavedChanges = true)
    }
    
    /**
     * Force reload recipe data, ignoring unsaved changes
     * Used for retry operations where user explicitly wants to reload
     */
    fun forceLoadRecipeData(recipeId: String) {
        loadRecipeDataInternal(recipeId, respectUnsavedChanges = false)
    }
    
    private fun loadRecipeDataInternal(recipeId: String, respectUnsavedChanges: Boolean) {
        // Don't reload if there are unsaved changes to prevent overwriting user edits
        if (respectUnsavedChanges && _uiState.value.hasUnsavedChanges) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                recipeId = recipeId
            )
            
            try {
                // First try to get recipe data (for completed recipes)
                val recipeResult = recipeRepository.getRecipe(recipeId)
                
                recipeResult.fold(
                    onSuccess = { recipe ->
                        // If we have recipe data, use it (complete data)
                        convertRecipeToUiState(recipe)
                    },
                    onFailure = { recipeError ->
                        // Fall back to ingest job data (try using recipeId as jobId)
                        ingestRepository.pollJob(recipeId).fold(
                            onSuccess = { ingestJob ->
                                convertIngestJobToUiState(ingestJob)
                            },
                            onFailure = { ingestError ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load recipe: Recipe not found (${recipeError.message}) and no ingest job found (${ingestError.message})"
                                )
                            }
                        )
                    }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading recipe: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Convert IngestJobDto to editable UI state
     * This provides the richest data for newly ingested recipes
     */
    private fun convertIngestJobToUiState(job: IngestJobDto) {
        val recipeData = job.recipeJson?.let { parseRecipeJson(it) }
        
        // Extract ingredients from recipe JSON or fall back to candidates
        val ingredients = recipeData?.ingredients?.mapIndexed { index, ingredient ->
            EditableIngredient(
                id = "ing_$index",
                name = ingredient.name,
                quantity = ingredient.quantity
            )
        } ?: job.ingredientCandidates.mapIndexed { index, candidate ->
            EditableIngredient(
                id = "ing_candidate_$index",
                name = candidate,
                quantity = "To taste" // Default quantity for candidates
            )
        }
        
        // Extract instructions from recipe JSON
        val instructions = recipeData?.instructions?.mapIndexed { index, instruction ->
            EditableInstruction(
                id = "inst_$index",
                text = instruction,
                stepNumber = index + 1
            )
        } ?: emptyList()
        
        val title = recipeData?.title ?: job.title ?: "Untitled Recipe"
        val description = recipeData?.description ?: "Recipe from TikTok"
        val prepTime = recipeData?.prepTime ?: 0
        val cookTime = recipeData?.cookTime ?: 0
        val servings = recipeData?.servings ?: 1
        val difficulty = recipeData?.difficulty ?: 1
        val tags = recipeData?.tags ?: emptyList()
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            title = title,
            description = description,
            prepTime = prepTime,
            cookTime = cookTime,
            servings = servings,
            difficulty = difficulty,
            ingredients = ingredients,
            instructions = instructions,
            sourceUrl = null, // sourceUrl not available in IngestJobDto
            sourcePlatform = "TikTok",
            tiktokAuthor = recipeData?.tiktokAuthor,
            videoThumbnail = recipeData?.videoThumbnail,
            tags = tags,
            transcript = job.transcript,
            onscreenText = job.onscreenText,
            ingredientCandidates = job.ingredientCandidates,
            hasUnsavedChanges = false,
            // Store original values for patch generation
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
    
    /**
     * Convert Recipe to editable UI state
     * Used when loading existing saved recipes
     */
    private fun convertRecipeToUiState(recipe: Recipe) {
        val ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
            EditableIngredient(
                id = "ing_$index",
                name = ingredient.name,
                quantity = ingredient.quantity
            )
        }
        
        val instructions = recipe.instructions.mapIndexed { index, instruction ->
            EditableInstruction(
                id = "inst_$index",
                text = instruction,
                stepNumber = index + 1
            )
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            title = recipe.title,
            description = recipe.description ?: "",
            prepTime = recipe.prepTime,
            cookTime = recipe.cookTime,
            servings = recipe.servings,
            difficulty = recipe.difficulty,
            ingredients = ingredients,
            instructions = instructions,
            sourceUrl = recipe.sourceUrl,
            sourcePlatform = recipe.sourcePlatform,
            tiktokAuthor = recipe.tiktokAuthor,
            videoThumbnail = recipe.videoThumbnail,
            tags = recipe.tags,
            transcript = null, // Not available in saved recipes
            onscreenText = null, // Not available in saved recipes
            ingredientCandidates = emptyList(), // Not available in saved recipes
            hasUnsavedChanges = false,
            // Store original values for patch generation
            originalTitle = recipe.title,
            originalDescription = recipe.description ?: "",
            originalPrepTime = recipe.prepTime,
            originalCookTime = recipe.cookTime,
            originalServings = recipe.servings,
            originalDifficulty = recipe.difficulty,
            originalIngredients = ingredients,
            originalInstructions = instructions,
            originalTags = recipe.tags
        )
    }
    
    /**
     * Parse recipe JSON from ingest job
     * Handles both JsonObject and other JsonElement types
     */
    private fun parseRecipeJson(jsonElement: JsonElement): ParsedRecipeData? {
        return try {
            when (jsonElement) {
                is JsonObject -> {
                    val obj = jsonElement.jsonObject
                    ParsedRecipeData(
                        title = obj["title"]?.jsonPrimitive?.content,
                        description = obj["description"]?.jsonPrimitive?.content,
                        prepTime = obj["prep_time"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        cookTime = obj["cook_time"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        servings = obj["servings"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                        difficulty = obj["difficulty"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                        ingredients = parseIngredientsFromJson(obj["ingredients"]),
                        instructions = parseInstructionsFromJson(obj["instructions"]),
                        tiktokAuthor = obj["tiktok_author"]?.jsonPrimitive?.content,
                        videoThumbnail = obj["video_thumbnail"]?.jsonPrimitive?.content,
                        tags = parseTagsFromJson(obj["tags"])
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseIngredientsFromJson(element: JsonElement?): List<Ingredient> {
        // TODO: Implement JSON parsing for ingredients array
        // For now return empty list - will be populated from ingredient_candidates
        return emptyList()
    }
    
    private fun parseInstructionsFromJson(element: JsonElement?): List<String> {
        // TODO: Implement JSON parsing for instructions array
        return emptyList()
    }
    
    private fun parseTagsFromJson(element: JsonElement?): List<String> {
        // TODO: Implement JSON parsing for tags array
        return emptyList()
    }
    
    /**
     * Update recipe title
     */
    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            title = newTitle,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Update recipe description
     */
    fun updateDescription(newDescription: String) {
        _uiState.value = _uiState.value.copy(
            description = newDescription,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Update recipe metadata (prep time, cook time, servings, difficulty)
     */
    fun updateMetadata(prepTime: Int? = null, cookTime: Int? = null, servings: Int? = null, difficulty: Int? = null) {
        _uiState.value = _uiState.value.copy(
            prepTime = prepTime ?: _uiState.value.prepTime,
            cookTime = cookTime ?: _uiState.value.cookTime,
            servings = servings ?: _uiState.value.servings,
            difficulty = difficulty ?: _uiState.value.difficulty,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Update ingredient list
     */
    fun updateIngredients(newIngredients: List<EditableIngredient>) {
        _uiState.value = _uiState.value.copy(
            ingredients = newIngredients,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Start editing a specific ingredient
     */
    fun startEditingIngredient(ingredientId: String) {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.map { ingredient ->
            if (ingredient.id == ingredientId) {
                ingredient.copy(isEditing = true)
            } else {
                ingredient.copy(isEditing = false) // Only one ingredient can be edited at a time
            }
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients
        )
    }
    
    /**
     * Stop editing ingredients
     */
    fun stopEditingIngredients() {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.map { ingredient ->
            ingredient.copy(isEditing = false)
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients
        )
    }
    
    /**
     * Update a specific ingredient's name
     */
    fun updateIngredientName(ingredientId: String, newName: String) {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.map { ingredient ->
            if (ingredient.id == ingredientId) {
                ingredient.copy(name = newName)
            } else {
                ingredient
            }
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Update a specific ingredient's quantity
     */
    fun updateIngredientQuantity(ingredientId: String, newQuantity: String) {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.map { ingredient ->
            if (ingredient.id == ingredientId) {
                ingredient.copy(quantity = newQuantity)
            } else {
                ingredient
            }
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Save ingredient changes and exit edit mode
     */
    fun saveIngredientChanges(ingredientId: String) {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.map { ingredient ->
            if (ingredient.id == ingredientId) {
                // Validate: remove if both name and quantity are empty
                if (ingredient.name.trim().isEmpty() && ingredient.quantity.trim().isEmpty()) {
                    return@map null // Will be filtered out
                }
                ingredient.copy(
                    isEditing = false,
                    name = ingredient.name.trim(),
                    quantity = ingredient.quantity.trim()
                )
            } else {
                ingredient
            }
        }.filterNotNull()
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Cancel ingredient editing and revert changes
     */
    fun cancelIngredientEditing(ingredientId: String, originalName: String, originalQuantity: String) {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.map { ingredient ->
            if (ingredient.id == ingredientId) {
                ingredient.copy(
                    isEditing = false,
                    name = originalName,
                    quantity = originalQuantity
                )
            } else {
                ingredient
            }
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients
        )
    }
    
    /**
     * Add a new ingredient
     */
    fun addNewIngredient() {
        val currentIngredients = _uiState.value.ingredients
        val newId = "ing_new_${System.currentTimeMillis()}"
        val newIngredient = EditableIngredient(
            id = newId,
            name = "",
            quantity = "",
            isEditing = true
        )
        
        // Stop editing other ingredients
        val updatedExistingIngredients = currentIngredients.map { ingredient ->
            ingredient.copy(isEditing = false)
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedExistingIngredients + newIngredient,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Delete an ingredient
     */
    fun deleteIngredient(ingredientId: String) {
        val currentIngredients = _uiState.value.ingredients
        val updatedIngredients = currentIngredients.filter { ingredient ->
            ingredient.id != ingredientId
        }
        
        _uiState.value = _uiState.value.copy(
            ingredients = updatedIngredients,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Reorder ingredients by moving an ingredient from one position to another
     */
    fun reorderIngredients(fromIndex: Int, toIndex: Int) {
        val currentIngredients = _uiState.value.ingredients.toMutableList()
        
        if (fromIndex in currentIngredients.indices && toIndex in currentIngredients.indices) {
            // Stop editing all ingredients during reorder
            val ingredientsWithoutEditing = currentIngredients.map { it.copy(isEditing = false) }.toMutableList()
            val item = ingredientsWithoutEditing.removeAt(fromIndex)
            ingredientsWithoutEditing.add(toIndex, item)
            
            _uiState.value = _uiState.value.copy(
                ingredients = ingredientsWithoutEditing,
                hasUnsavedChanges = true
            )
        }
    }
    
    /**
     * Update instruction list
     */
    fun updateInstructions(newInstructions: List<EditableInstruction>) {
        _uiState.value = _uiState.value.copy(
            instructions = newInstructions,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Start editing a specific instruction
     */
    fun startEditingInstruction(instructionId: String) {
        val currentInstructions = _uiState.value.instructions
        val updatedInstructions = currentInstructions.map { instruction ->
            if (instruction.id == instructionId) {
                instruction.copy(isEditing = true)
            } else {
                instruction.copy(isEditing = false) // Only one instruction can be edited at a time
            }
        }
        
        _uiState.value = _uiState.value.copy(
            instructions = updatedInstructions
        )
    }
    
    /**
     * Update a specific instruction's text
     */
    fun updateInstructionText(instructionId: String, newText: String) {
        val currentInstructions = _uiState.value.instructions
        val updatedInstructions = currentInstructions.map { instruction ->
            if (instruction.id == instructionId) {
                instruction.copy(text = newText)
            } else {
                instruction
            }
        }
        
        _uiState.value = _uiState.value.copy(
            instructions = updatedInstructions,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Stop editing instructions
     */
    fun stopEditingInstructions() {
        val currentInstructions = _uiState.value.instructions
        val updatedInstructions = currentInstructions.map { instruction ->
            instruction.copy(isEditing = false)
        }
        
        _uiState.value = _uiState.value.copy(
            instructions = updatedInstructions
        )
    }
    
    /**
     * Cancel instruction editing and revert to original text
     */
    fun cancelInstructionEditing(instructionId: String, originalText: String) {
        val currentInstructions = _uiState.value.instructions
        val updatedInstructions = currentInstructions.map { instruction ->
            if (instruction.id == instructionId) {
                instruction.copy(
                    isEditing = false,
                    text = originalText
                )
            } else {
                instruction
            }
        }
        
        _uiState.value = _uiState.value.copy(
            instructions = updatedInstructions
        )
    }
    
    /**
     * Add a new instruction
     */
    fun addNewInstruction() {
        val currentInstructions = _uiState.value.instructions
        val newId = "inst_new_${System.currentTimeMillis()}"
        val newStepNumber = currentInstructions.size + 1
        val newInstruction = EditableInstruction(
            id = newId,
            text = "",
            stepNumber = newStepNumber,
            isEditing = true
        )
        
        // Stop editing other instructions
        val updatedExistingInstructions = currentInstructions.map { instruction ->
            instruction.copy(isEditing = false)
        }
        
        _uiState.value = _uiState.value.copy(
            instructions = updatedExistingInstructions + newInstruction,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Delete an instruction
     */
    fun deleteInstruction(instructionId: String) {
        val currentInstructions = _uiState.value.instructions
        val updatedInstructions = currentInstructions.filter { instruction ->
            instruction.id != instructionId
        }
        
        // Update step numbers to maintain sequence
        val reorderedInstructions = updatedInstructions.mapIndexed { index, instruction ->
            instruction.copy(stepNumber = index + 1)
        }
        
        _uiState.value = _uiState.value.copy(
            instructions = reorderedInstructions,
            hasUnsavedChanges = true
        )
    }
    
    /**
     * Reorder instructions by moving an instruction from one position to another
     */
    fun reorderInstructions(fromIndex: Int, toIndex: Int) {
        val currentInstructions = _uiState.value.instructions.toMutableList()
        
        if (fromIndex in currentInstructions.indices && toIndex in currentInstructions.indices) {
            // Stop editing all instructions during reorder
            val instructionsWithoutEditing = currentInstructions.map { it.copy(isEditing = false) }.toMutableList()
            val item = instructionsWithoutEditing.removeAt(fromIndex)
            instructionsWithoutEditing.add(toIndex, item)
            
            // Update step numbers to match new positions
            val reorderedInstructions = instructionsWithoutEditing.mapIndexed { index, instruction ->
                instruction.copy(stepNumber = index + 1)
            }
            
            _uiState.value = _uiState.value.copy(
                instructions = reorderedInstructions,
                hasUnsavedChanges = true
            )
        }
    }
    
    /**
     * Get formatted prep time string
     */
    fun getFormattedPrepTime(): String {
        val prepTime = _uiState.value.prepTime
        return if (prepTime > 0) "$prepTime min" else "-- min"
    }
    
    /**
     * Get formatted cook time string
     */
    fun getFormattedCookTime(): String {
        val cookTime = _uiState.value.cookTime
        return if (cookTime > 0) "$cookTime min" else "-- min"
    }
    
    /**
     * Get formatted servings string
     */
    fun getFormattedServings(): String {
        val servings = _uiState.value.servings
        return if (servings > 0) servings.toString() else "--"
    }
    
    /**
     * Check if recipe has any content to display
     */
    fun hasRecipeContent(): Boolean {
        val state = _uiState.value
        return state.ingredients.isNotEmpty() || state.instructions.isNotEmpty() || 
               state.transcript?.isNotEmpty() == true || state.ingredientCandidates.isNotEmpty()
    }
    
    /**
     * Get source information for display
     */
    fun getSourceInfo(): String {
        val state = _uiState.value
        return buildString {
            append("Recipe ID: ${state.recipeId}")
            if (state.sourcePlatform != null) {
                append("\nSource: ${state.sourcePlatform}")
            }
            if (state.tiktokAuthor != null) {
                append("\nAuthor: ${state.tiktokAuthor}")
            }
        }
    }
    
    /**
     * Generate a minimal JSON patch containing only the fields that have changed
     * Returns null if no changes have been made
     */
    fun generateJsonPatch(): JsonObject? {
        val state = _uiState.value
        
        if (!state.hasUnsavedChanges) {
            return null
        }
        
        return buildJsonObject {
            // Compare basic fields
            if (state.title != state.originalTitle) {
                put("title", state.title)
            }
            
            if (state.description != state.originalDescription) {
                put("description", state.description)
            }
            
            if (state.prepTime != state.originalPrepTime) {
                put("prep_time", state.prepTime)
            }
            
            if (state.cookTime != state.originalCookTime) {
                put("cook_time", state.cookTime)
            }
            
            if (state.servings != state.originalServings) {
                put("servings", state.servings)
            }
            
            if (state.difficulty != state.originalDifficulty) {
                put("difficulty", state.difficulty)
            }
            
            // Compare ingredients
            if (hasIngredientsChanged(state.ingredients, state.originalIngredients)) {
                putJsonArray("ingredients") {
                    state.ingredients.forEach { ingredient ->
                        add(buildJsonObject {
                            put("name", ingredient.name)
                            put("quantity", ingredient.quantity)
                        })
                    }
                }
            }
            
            // Compare instructions
            if (hasInstructionsChanged(state.instructions, state.originalInstructions)) {
                putJsonArray("instructions") {
                    state.instructions.forEach { instruction ->
                        add(JsonPrimitive(instruction.text))
                    }
                }
            }
            
            // Compare tags
            if (state.tags != state.originalTags) {
                putJsonArray("tags") {
                    state.tags.forEach { tag ->
                        add(JsonPrimitive(tag))
                    }
                }
            }
        }
    }
    
    /**
     * Check if ingredients have changed by comparing content, not just references
     */
    private fun hasIngredientsChanged(
        current: List<EditableIngredient>,
        original: List<EditableIngredient>
    ): Boolean {
        if (current.size != original.size) return true
        
        return current.zip(original).any { (curr, orig) ->
            curr.name != orig.name || curr.quantity != orig.quantity
        }
    }
    
    /**
     * Check if instructions have changed by comparing content, not just references
     */
    private fun hasInstructionsChanged(
        current: List<EditableInstruction>,
        original: List<EditableInstruction>
    ): Boolean {
        if (current.size != original.size) return true
        
        return current.zip(original).any { (curr, orig) ->
            curr.text != orig.text
        }
    }
    
    /**
     * Get the complete recipe JSON for saving (includes all fields)
     * This is used when we need to send the full recipe data
     */
    fun getCompleteRecipeJson(): JsonObject {
        val state = _uiState.value
        
        return buildJsonObject {
            put("title", state.title)
            put("description", state.description)
            put("prep_time", state.prepTime)
            put("cook_time", state.cookTime)
            put("servings", state.servings)
            put("difficulty", state.difficulty)
            
            putJsonArray("ingredients") {
                state.ingredients.forEach { ingredient ->
                    add(buildJsonObject {
                        put("name", ingredient.name)
                        put("quantity", ingredient.quantity)
                    })
                }
            }
            
            putJsonArray("instructions") {
                state.instructions.forEach { instruction ->
                    add(JsonPrimitive(instruction.text))
                }
            }
            
            putJsonArray("tags") {
                state.tags.forEach { tag ->
                    add(JsonPrimitive(tag))
                }
            }
            
            // Include source information if available
            state.sourceUrl?.let { put("source_url", it) }
            state.sourcePlatform?.let { put("source_platform", it) }
            state.tiktokAuthor?.let { put("tiktok_author", it) }
            state.videoThumbnail?.let { put("video_thumbnail", it) }
        }
    }
    
    /**
     * Save the current recipe draft using the JSON patch (with UI state management)
     */
    fun saveRecipe(recipeId: String) {
        viewModelScope.launch {
            // Capture the save type based on current state
            val saveType = if (_uiState.value.hasUnsavedChanges) "save" else "approve"
            
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                saveError = null,
                lastSaveType = saveType
            )
            
            val result = saveDraftInternal(recipeId)
            
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveError = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
            
            // Emit navigation events based on result
            when {
                result.isSuccess -> {
                    val message = result.getOrNull() ?: "Recipe saved successfully"
                    _navigationEvents.send(DraftPreviewNavigationEvent.ShowSaveSuccess(message))
                    _navigationEvents.send(DraftPreviewNavigationEvent.NavigateToRecipeDetail(recipeId))
                    
                    // Clear the shared AddFromTikTok instance after successful save
                    // This ensures the next TikTok import starts with a clean state
                    AddFromTikTokViewModel.clearSharedInstance()
                }
                result.isFailure -> {
                    val error = result.exceptionOrNull()?.message ?: "Failed to save recipe"
                    _navigationEvents.send(DraftPreviewNavigationEvent.ShowSaveError(error))
                }
            }
        }
    }
    
    /**
     * Save the current recipe draft using the JSON patch (internal implementation)
     */
    private suspend fun saveDraftInternal(recipeId: String): Result<String> {
        val jsonPatch = generateJsonPatch()
        
        return if (jsonPatch != null) {
            try {
                val result = recipeRepository.saveDraft(recipeId, jsonPatch)
                
                if (result.isSuccess) {
                    // Mark as saved and reset original values
                    markAsSaved()
                    Result.success("Recipe saved successfully")
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to save recipe"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // No changes to save
            Result.success("No changes to save")
        }
    }
    
    /**
     * Clear the last save type flag
     */
    fun clearLastSaveType() {
        _uiState.value = _uiState.value.copy(lastSaveType = null)
    }
    
    /**
     * Reset the original values to current values (call after successful save)
     */
    fun markAsSaved() {
        val state = _uiState.value
        
        _uiState.value = state.copy(
            hasUnsavedChanges = false,
            originalTitle = state.title,
            originalDescription = state.description,
            originalPrepTime = state.prepTime,
            originalCookTime = state.cookTime,
            originalServings = state.servings,
            originalDifficulty = state.difficulty,
            originalIngredients = state.ingredients.map { it.copy(isEditing = false) },
            originalInstructions = state.instructions.map { it.copy(isEditing = false) },
            originalTags = state.tags
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        _navigationEvents.close()
    }
}

/**
 * Helper data class for parsed recipe JSON
 */
private data class ParsedRecipeData(
    val title: String? = null,
    val description: String? = null,
    val prepTime: Int = 0,
    val cookTime: Int = 0,
    val servings: Int = 1,
    val difficulty: Int = 1,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val tiktokAuthor: String? = null,
    val videoThumbnail: String? = null,
    val tags: List<String> = emptyList()
) 