package com.example.reciplan.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.model.*
import com.example.reciplan.data.recipe.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()
    
    // Recipe feed state
    private val _recipeFeed = MutableStateFlow<List<Recipe>>(emptyList())
    val recipeFeed: StateFlow<List<Recipe>> = _recipeFeed.asStateFlow()
    
    // Selected recipe state
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()
    
    // Pagination state
    private var currentPage = 1
    private var isLastPage = false
    
    init {
        loadRecipes()
    }
    
    // Load recipes from the feed
    fun loadRecipes(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            isLastPage = false
            _recipeFeed.value = emptyList()
        }
        if (_uiState.value.isLoading || isLastPage) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            recipeRepository.getRecipeFeed(currentPage, 10).fold(
                onSuccess = { response ->
                    val newRecipes = if (refresh) response.recipes else _recipeFeed.value + response.recipes
                    _recipeFeed.value = newRecipes
                    currentPage++
                    isLastPage = response.recipes.size < 10
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }
    
    // Load more recipes for pagination
    fun loadMoreRecipes() {
        loadRecipes(refresh = false)
    }
    
    // Refresh recipes
    fun refreshRecipes() {
        loadRecipes(refresh = true)
    }
    
    // Get single recipe by ID
    fun getRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            recipeRepository.getRecipe(recipeId).fold(
                onSuccess = { recipe ->
                    _selectedRecipe.value = recipe
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    // Helper function to check if a recipe matches the current filter criteria
    private fun recipeMatchesCurrentFilter(recipe: Recipe): Boolean {
        val state = _uiState.value
        val filterMatch = when (state.selectedFilter) {
            "All", "" -> true
            "My Recipes" -> recipe.userId == "IbMRwrirqeyObTyqc9Aa"
            else -> recipe.tags.any { it.equals(state.selectedFilter, ignoreCase = true) }
        }
        val searchMatch = state.searchQuery.isEmpty() ||
            recipe.title.contains(state.searchQuery, ignoreCase = true) ||
            recipe.description?.contains(state.searchQuery, ignoreCase = true) == true ||
            recipe.tags.any { it.contains(state.searchQuery, ignoreCase = true) }
        return filterMatch && searchMatch
    }

    // Helper to update filteredRecipes after any change
    private fun updateFilteredRecipes() {
        val state = _uiState.value
        val shouldShowFiltered = state.filteredRecipes.isNotEmpty() || state.searchQuery.isNotEmpty() || state.selectedFilter != "All"
        val newFiltered = if (shouldShowFiltered) {
            _recipeFeed.value.filter { recipeMatchesCurrentFilter(it) }
        } else {
            emptyList()
        }
        _uiState.value = state.copy(filteredRecipes = newFiltered)
    }

    // Create new recipe
    fun createRecipe(request: CreateRecipeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            recipeRepository.createRecipe(request).fold(
                onSuccess = { recipe ->
                    _recipeFeed.value = listOf(recipe) + _recipeFeed.value
                    updateFilteredRecipes()
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null, successMessage = "Recipe created successfully!")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    // Update existing recipe
    fun updateRecipe(recipeId: String, request: UpdateRecipeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            recipeRepository.updateRecipe(recipeId, request).fold(
                onSuccess = { updatedRecipe ->
                    _recipeFeed.value = _recipeFeed.value.map { if (it.id == recipeId) updatedRecipe else it }
                    if (_selectedRecipe.value?.id == recipeId) _selectedRecipe.value = updatedRecipe
                    updateFilteredRecipes()
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null, successMessage = "Recipe updated successfully!")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    // Delete recipe
    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            recipeRepository.deleteRecipe(recipeId).fold(
                onSuccess = { deletedRecipeId ->
                    val newFeed = _recipeFeed.value.filter { it.id != deletedRecipeId }
                    _recipeFeed.value = newFeed
                    if (_selectedRecipe.value?.id == deletedRecipeId) _selectedRecipe.value = null
                    updateFilteredRecipes()
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null, successMessage = "Recipe deleted successfully!")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to delete recipe: ${error.message}")
                }
            )
        }
    }
    
    // Save/bookmark recipe
    fun saveRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.saveRecipe(recipeId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Recipe saved to your collection!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
    
    // Unsave/unbookmark recipe
    fun unsaveRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.unsaveRecipe(recipeId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Recipe removed from your collection!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
    
    // Clear error message
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Clear success message
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    // Clear both messages
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
    
    // Filter recipes by tags
    fun filterRecipesByTag(tag: String) {
        val filteredRecipes = if (tag.isEmpty() || tag == "All") {
            _recipeFeed.value
        } else if (tag == "My Recipes") {
            // Filter for user-created recipes (userId matches current user)
            val userRecipes = _recipeFeed.value.filter { recipe ->
                recipe.userId == "IbMRwrirqeyObTyqc9Aa" // Current user's ID
            }
            userRecipes
        } else {
            _recipeFeed.value.filter { recipe ->
                recipe.tags.any { it.equals(tag, ignoreCase = true) }
            }
        }
        
        _uiState.value = _uiState.value.copy(
            filteredRecipes = filteredRecipes,
            selectedFilter = tag
        )
    }
    
    // Search recipes
    fun searchRecipes(query: String) {
        val searchResults = if (query.isEmpty()) {
            _recipeFeed.value
        } else {
            _recipeFeed.value.filter { recipe ->
                recipe.title.contains(query, ignoreCase = true) ||
                recipe.description?.contains(query, ignoreCase = true) == true ||
                recipe.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
        
        _uiState.value = _uiState.value.copy(
            filteredRecipes = searchResults,
            searchQuery = query
        )
    }
    
    // Seed recipes for development
    fun seedRecipes() {
        viewModelScope.launch {
            recipeRepository.seedRecipes().fold(
                onSuccess = {
                    refreshRecipes()
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Sample recipes added!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
}

// UI State data class
data class RecipeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val filteredRecipes: List<Recipe> = emptyList(),
    val selectedFilter: String = "All",
    val searchQuery: String = ""
) 