package com.example.reciplan.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.model.Recipe
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isSaved: Boolean = false
)

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val recipeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadRecipe()
    }

    private fun loadRecipe() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            recipeRepository.getRecipe(recipeId).fold(
                onSuccess = { recipe ->
                    val isSaved = currentUserId?.let { userId ->
                        recipe.saved_by.contains(userId)
                    } ?: false
                    
                    _uiState.value = _uiState.value.copy(
                        recipe = recipe,
                        isLoading = false,
                        isSaved = isSaved,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipe"
                    )
                }
            )
        }
    }

    fun saveRecipe() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                error = "Please sign in to save recipes"
            )
            return
        }
        
        viewModelScope.launch {
            recipeRepository.saveRecipe(recipeId).fold(
                onSuccess = {
                    // Update local state optimistically
                    val currentRecipe = _uiState.value.recipe
                    if (currentRecipe != null) {
                        val updatedSavedBy = currentRecipe.saved_by.toMutableList()
                        if (!updatedSavedBy.contains(userId)) {
                            updatedSavedBy.add(userId)
                        }
                        val updatedRecipe = currentRecipe.copy(saved_by = updatedSavedBy)
                        
                        _uiState.value = _uiState.value.copy(
                            recipe = updatedRecipe,
                            isSaved = true,
                            successMessage = "Recipe saved to your collection!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to save recipe"
                    )
                }
            )
        }
    }

    fun unsaveRecipe() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                error = "Please sign in to manage saved recipes"
            )
            return
        }
        
        viewModelScope.launch {
            recipeRepository.unsaveRecipe(recipeId).fold(
                onSuccess = {
                    // Update local state optimistically
                    val currentRecipe = _uiState.value.recipe
                    if (currentRecipe != null) {
                        val updatedSavedBy = currentRecipe.saved_by.toMutableList()
                        updatedSavedBy.remove(userId)
                        val updatedRecipe = currentRecipe.copy(saved_by = updatedSavedBy)
                        
                        _uiState.value = _uiState.value.copy(
                            recipe = updatedRecipe,
                            isSaved = false,
                            successMessage = "Recipe removed from your collection!"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to unsave recipe"
                    )
                }
            )
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        
        viewModelScope.launch {
            recipeRepository.getRecipe(recipeId).fold(
                onSuccess = { recipe ->
                    val isSaved = currentUserId?.let { userId ->
                        recipe.saved_by.contains(userId)
                    } ?: false
                    
                    _uiState.value = _uiState.value.copy(
                        recipe = recipe,
                        isRefreshing = false,
                        isSaved = isSaved,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh recipe"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
} 