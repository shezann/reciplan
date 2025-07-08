package com.example.reciplan.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteRecipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMorePages: Boolean = true
)

class FavoritesViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // StateFlow for Compose - primary state management
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        loadSavedRecipes()
    }

    fun loadSavedRecipes() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            recipeRepository.getSavedRecipes(page = 1, limit = 10).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        favoriteRecipes = response.recipes,
                        isLoading = false,
                        hasMorePages = response.has_next ?: (response.recipes.size >= 10)
                    )
                    currentPage = 1
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load saved recipes"
                    )
                }
            )
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            recipeRepository.getSavedRecipes(page = 1, limit = 10).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        favoriteRecipes = response.recipes,
                        isRefreshing = false,
                        hasMorePages = response.has_next ?: (response.recipes.size >= 10)
                    )
                    currentPage = 1
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh saved recipes"
                    )
                }
            )
        }
    }

    fun loadMoreSavedRecipes() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return

        _uiState.value = _uiState.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            val nextPage = currentPage + 1
            recipeRepository.getSavedRecipes(page = nextPage, limit = 10).fold(
                onSuccess = { response ->
                    val updatedRecipes = _uiState.value.favoriteRecipes + response.recipes
                    _uiState.value = _uiState.value.copy(
                        favoriteRecipes = updatedRecipes,
                        isLoadingMore = false,
                        hasMorePages = response.has_next ?: (response.recipes.size >= 10)
                    )
                    currentPage = nextPage
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = error.message ?: "Failed to load more saved recipes"
                    )
                }
            )
        }
    }

    fun unsaveRecipe(recipeId: String) {
        viewModelScope.launch {
            // Optimistic update - remove the recipe immediately
            val currentRecipes = _uiState.value.favoriteRecipes
            val updatedRecipes = currentRecipes.filter { it.id != recipeId }
            _uiState.value = _uiState.value.copy(favoriteRecipes = updatedRecipes)

            recipeRepository.unsaveRecipe(recipeId).fold(
                onSuccess = {
                    // Recipe was successfully unsaved, local state is already updated
                },
                onFailure = { error ->
                    // Revert the optimistic update on failure
                    _uiState.value = _uiState.value.copy(
                        favoriteRecipes = currentRecipes,
                        error = error.message ?: "Failed to unsave recipe"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
} 