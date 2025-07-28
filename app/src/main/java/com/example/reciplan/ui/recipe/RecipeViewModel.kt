package com.example.reciplan.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.auth.AuthRepository
import com.example.reciplan.data.model.*
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.repository.LikeRepository
import com.example.reciplan.data.repository.LikeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeRepository: RecipeRepository,
    private val likeRepository: LikeRepository,
    private val authRepository: AuthRepository
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
    
    // Current user data for filtering
    private var currentUser: User? = null
    
    // Pagination state
    private var currentPage = 1
    private var isLastPage = false
    
    init {
        loadCurrentUser()
        loadRecipes()
        
        // Update filtered recipes when like states change
        viewModelScope.launch {
            likeRepository.likeStates.collect { _ ->
                // Refresh filtering when any like state changes
                updateFilteredRecipes()
            }
        }
    }
    
    // Load current user data for filtering
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUser = authRepository.getCurrentUserData()
            println("🔧 LOADED CURRENT USER: ${currentUser?.id} (${currentUser?.email})")
            
            // Refresh filtering after user data is loaded
            updateFilteredRecipes()
        }
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
                    
                    // Apply filtering after loading recipes
                    updateFilteredRecipes()
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
        val currentUserId = getCurrentUserId()
        
        // Get real-time like state (not static API field)
        val currentLikeState = likeRepository.likeStates.value[recipe.id]
        val isCurrentlyLiked = currentLikeState?.liked ?: recipe.liked // Fallback to API field if no live state
        
        // Debug logging for "All" filter (key debugging only)
        if (state.selectedFilter == "All" || state.selectedFilter.isEmpty()) {
            // Special debugging for the problematic recipe only
            if (recipe.id == "fcd08e90-8cf6-4a8f-a0de-11b1484d6f57") {
                val isCreatedByUser = recipe.userId == currentUserId
                val shouldShow = isCreatedByUser || isCurrentlyLiked
                println("🚨 PROBLEMATIC RECIPE - Should show: $shouldShow (created: $isCreatedByUser, liked: $isCurrentlyLiked)")
            }
        }
        
        val filterMatch = when (state.selectedFilter) {
            "All", "" -> {
                // Show created OR currently liked recipes
                // Handle null/empty userIds safely
                val isCreated = !currentUserId.isNullOrEmpty() && recipe.userId.isNotEmpty() && recipe.userId == currentUserId
                val result = isCreated || isCurrentlyLiked
                
                // Extra debug for problematic recipe
                if (recipe.id == "fcd08e90-8cf6-4a8f-a0de-11b1484d6f57") {
                    println("🚨 FILTER MATCH CALCULATION:")
                    println("  currentUserId.isNullOrEmpty(): ${currentUserId.isNullOrEmpty()}")
                    println("  recipe.userId.isEmpty(): ${recipe.userId.isEmpty()}")
                    println("  isCreated: $isCreated")
                    println("  isCurrentlyLiked: $isCurrentlyLiked")
                    println("  Final result: $result")
                }
                
                result
            }
            "My Recipes" -> {
                // Show ONLY created recipes
                val result = !currentUserId.isNullOrEmpty() && recipe.userId.isNotEmpty() && recipe.userId == currentUserId
                
                // Debug logging for My Recipes filter
                if (!result) {
                    println("📝 MY RECIPES - FILTERED OUT: '${recipe.title}' (owner: '${recipe.userId}', you: '$currentUserId')")
                }
                
                result
            }
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
        println("🔧 FILTERING: '${state.selectedFilter}' → ${_recipeFeed.value.size} recipes")
        
        // Always apply filtering logic (including "All" for personalized feed)
        val newFiltered = _recipeFeed.value.filter { recipeMatchesCurrentFilter(it) }
        
        println("🔧 RESULT: ${newFiltered.size} recipes shown: ${newFiltered.map { it.title }}")
        
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
        println("🎯 FILTER BY TAG CALLED: '$tag'")
        
        // Update the selected filter first
        _uiState.value = _uiState.value.copy(selectedFilter = tag)
        
        // Use the new filtering logic instead of old hardcoded logic
        updateFilteredRecipes()
    }
    
    // Search recipes
    fun searchRecipes(query: String) {
        println("🔍 SEARCH RECIPES CALLED: '$query'")
        
        // Update the search query first
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Use the new filtering logic (which handles both filter and search)
        updateFilteredRecipes()
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
    
    // Helper method to get current backend user ID (not Firebase ID)
    private fun getCurrentUserId(): String? {
        return currentUser?.id?.takeIf { it.isNotEmpty() }
    }
    
    // Get like state for a specific recipe (real-time)
    fun getLikeState(recipeId: String): StateFlow<LikeState> {
        return likeRepository.getLikeState(recipeId)
    }
    
    // Toggle like status for a recipe
    fun toggleLike(recipeId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            println("🔧 RECIPE SCREEN - TOGGLE LIKE: $recipeId, currently: $currentlyLiked")
            
            val result = likeRepository.toggleLike(recipeId, currentlyLiked)
            result.fold(
                onSuccess = { likeResponse ->
                    println("🔧 RECIPE SCREEN - LIKE SUCCESS: ${likeResponse.liked}, count: ${likeResponse.likesCount}")
                    // The real-time like state will automatically update via StateFlow
                    // Refresh filtered recipes to ensure consistency
                    updateFilteredRecipes()
                },
                onFailure = { error ->
                    println("🔧 RECIPE SCREEN - LIKE ERROR: ${error.message}")
                    _uiState.value = _uiState.value.copy(error = "Failed to ${if (currentlyLiked) "unlike" else "like"} recipe: ${error.message}")
                }
            )
        }
    }
    
    // Preload like states for better performance (similar to HomeViewModel)
    fun preloadLikeStates(recipes: List<Recipe>) {
        // The LikeRepository automatically manages like states, so we just need to ensure
        // they're loaded. This could trigger loading if needed.
        recipes.forEach { recipe ->
            // This will create the StateFlow if it doesn't exist
            getLikeState(recipe.id)
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