package com.example.reciplan.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.model.*
import com.example.reciplan.data.recipe.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

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
        println("RecipeViewModel: loadRecipes called - refresh: $refresh")
        
        if (refresh) {
            println("RecipeViewModel: Refreshing - resetting page to 1")
            currentPage = 1
            isLastPage = false
            _recipeFeed.value = emptyList()
        }
        
        if (_uiState.value.isLoading || isLastPage) {
            println("RecipeViewModel: Skipping load - isLoading: ${_uiState.value.isLoading}, isLastPage: $isLastPage")
            return
        }
        
        viewModelScope.launch {
            println("RecipeViewModel: Starting recipe load for page: $currentPage")
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            recipeRepository.getRecipeFeed(currentPage, 10).fold(
                onSuccess = { response ->
                    println("RecipeViewModel: Successfully loaded ${response.recipes.size} recipes")
                    
                    // Debug: Print details of each recipe
                    response.recipes.forEachIndexed { index, recipe ->
                        println("RecipeViewModel: Recipe $index:")
                        println("RecipeViewModel:   - ID: ${recipe.id}")
                        println("RecipeViewModel:   - Title: ${recipe.title}")
                        println("RecipeViewModel:   - UserId: ${recipe.user_id}")
                        println("RecipeViewModel:   - SourcePlatform: '${recipe.source_platform}' (null? ${recipe.source_platform == null}, blank? ${recipe.source_platform.isNullOrBlank()})")
                    }
                    
                    val newRecipes = if (refresh) {
                        response.recipes
                    } else {
                        _recipeFeed.value + response.recipes
                    }
                    
                    println("RecipeViewModel: Total recipes in feed: ${newRecipes.size}")
                    
                    // Debug: Count user-created recipes
                    val userCreatedCount = newRecipes.count { it.source_platform.isNullOrBlank() }
                    println("RecipeViewModel: User-created recipes (sourcePlatform null/blank): $userCreatedCount")
                    
                    _recipeFeed.value = newRecipes
                    currentPage++
                    isLastPage = response.recipes.size < 10
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    println("RecipeViewModel: Failed to load recipes: ${error.message}")
                    error.printStackTrace()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
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
        val currentUiState = _uiState.value
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        return when (currentUiState.selectedFilter) {
            "All", "" -> true
            "My Recipes" -> recipe.user_id == currentUserId
            else -> recipe.tags.any { it.equals(currentUiState.selectedFilter, ignoreCase = true) }
        } && (currentUiState.searchQuery.isEmpty() || 
               recipe.title.contains(currentUiState.searchQuery, ignoreCase = true) ||
               recipe.description?.contains(currentUiState.searchQuery, ignoreCase = true) == true ||
               recipe.tags.any { it.contains(currentUiState.searchQuery, ignoreCase = true) })
    }

    // Create new recipe
    fun createRecipe(request: CreateRecipeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            recipeRepository.createRecipe(request).fold(
                onSuccess = { recipe ->
                    // Add the new recipe to the feed
                    _recipeFeed.value = listOf(recipe) + _recipeFeed.value
                    
                    // Also add to filtered recipes if it matches current filter criteria
                    val currentUiState = _uiState.value
                    val updatedFilteredRecipes = if (currentUiState.filteredRecipes.isNotEmpty() || 
                                                     currentUiState.searchQuery.isNotEmpty() || 
                                                     currentUiState.selectedFilter != "All") {
                        if (recipeMatchesCurrentFilter(recipe)) {
                            listOf(recipe) + currentUiState.filteredRecipes
                        } else {
                            currentUiState.filteredRecipes
                        }
                    } else {
                        currentUiState.filteredRecipes
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Recipe created successfully!",
                        filteredRecipes = updatedFilteredRecipes
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
    
    // Update existing recipe
    fun updateRecipe(recipeId: String, request: UpdateRecipeRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            recipeRepository.updateRecipe(recipeId, request).fold(
                onSuccess = { updatedRecipe ->
                    // Update the recipe in the feed
                    _recipeFeed.value = _recipeFeed.value.map { recipe ->
                        if (recipe.id == recipeId) updatedRecipe else recipe
                    }
                    
                    // Also update in filtered recipes if any filters are active
                    val currentUiState = _uiState.value
                    val updatedFilteredRecipes = if (currentUiState.filteredRecipes.isNotEmpty() || 
                                                     currentUiState.searchQuery.isNotEmpty() || 
                                                     currentUiState.selectedFilter != "All") {
                        val filteredWithoutOldRecipe = currentUiState.filteredRecipes.filter { it.id != recipeId }
                        if (recipeMatchesCurrentFilter(updatedRecipe)) {
                            // Recipe still matches filter, update it in the list
                            filteredWithoutOldRecipe + updatedRecipe
                        } else {
                            // Recipe no longer matches filter, remove it from the list
                            filteredWithoutOldRecipe
                        }
                    } else {
                        currentUiState.filteredRecipes
                    }
                    
                    // Update selected recipe if it's the one being edited
                    if (_selectedRecipe.value?.id == recipeId) {
                        _selectedRecipe.value = updatedRecipe
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Recipe updated successfully!",
                        filteredRecipes = updatedFilteredRecipes
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
    
    // Delete recipe
    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            recipeRepository.deleteRecipe(recipeId).fold(
                onSuccess = { deletedRecipeId ->
                    // Remove the recipe from the feed
                    _recipeFeed.value = _recipeFeed.value.filter { it.id != deletedRecipeId }
                    
                    // Also remove from filtered recipes if any filters are active
                    val currentUiState = _uiState.value
                    val updatedFilteredRecipes = if (currentUiState.filteredRecipes.isNotEmpty() || 
                                                     currentUiState.searchQuery.isNotEmpty() || 
                                                     currentUiState.selectedFilter != "All") {
                        currentUiState.filteredRecipes.filter { it.id != deletedRecipeId }
                    } else {
                        currentUiState.filteredRecipes
                    }
                    
                    // Clear selected recipe if it was deleted
                    if (_selectedRecipe.value?.id == deletedRecipeId) {
                        _selectedRecipe.value = null
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        successMessage = "Recipe deleted successfully!",
                        filteredRecipes = updatedFilteredRecipes
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete recipe: ${error.message}"
                    )
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
        println("RecipeViewModel: Filtering by tag: '$tag'")
        println("RecipeViewModel: Total recipes available: ${_recipeFeed.value.size}")
        
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val filteredRecipes = if (tag.isEmpty() || tag == "All") {
            _recipeFeed.value
        } else if (tag == "My Recipes") {
            // Filter for user-created recipes (userId matches current user)
            val userRecipes = _recipeFeed.value.filter { recipe ->
                recipe.user_id == currentUserId
            }
            println("RecipeViewModel: My Recipes filter - found ${userRecipes.size} recipes")
            userRecipes.forEach { recipe ->
                println("  - ${recipe.title} (sourcePlatform: '${recipe.source_platform}')")
            }
            userRecipes
        } else {
            _recipeFeed.value.filter { recipe ->
                recipe.tags.any { it.equals(tag, ignoreCase = true) }
            }
        }
        
        println("RecipeViewModel: Filtered recipes count: ${filteredRecipes.size}")
        
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