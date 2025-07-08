package com.example.reciplan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.model.Recipe
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMorePages: Boolean = true,
    val searchQuery: String = "",
    val selectedTab: Int = 0
)

class HomeViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    // StateFlow for Compose - primary state management
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var isSearchMode = false
    private var currentSearchQuery = ""
    private var currentCategory = ""

    private val categories = listOf("", "Breakfast", "Lunch", "Dinner")

    init {
        loadRecipes()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.trim().isEmpty()) {
            isSearchMode = false
            val category = categories[_uiState.value.selectedTab]
            loadRecipes(category)
        } else {
            searchRecipes(query, categories[_uiState.value.selectedTab])
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        val category = categories[tabIndex]
        
        if (_uiState.value.searchQuery.trim().isEmpty()) {
            loadRecipes(category)
        } else {
            searchRecipes(_uiState.value.searchQuery, category)
        }
    }

    fun loadRecipes(category: String = "") {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        currentCategory = category
        isSearchMode = false

        viewModelScope.launch {
            recipeRepository.getRecipeFeed(page = 1, limit = 20).fold(
                onSuccess = { response ->
                    val filteredRecipes = if (category.isNotEmpty()) {
                        response.recipes.filter { recipe ->
                            recipe.tags.any { tag -> tag.contains(category, ignoreCase = true) }
                        }
                    } else {
                        response.recipes
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        recipes = filteredRecipes,
                        isLoading = false,
                        hasMorePages = response.has_next ?: (response.recipes.size >= 10)
                    )
                    
                    currentPage = 1
                    isSearchMode = false
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load recipes"
                    )
                }
            )
        }
    }

    fun searchRecipes(query: String, category: String = "") {
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        currentSearchQuery = query
        currentCategory = category
        isSearchMode = true

        viewModelScope.launch {
            recipeRepository.searchRecipes(query, category).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        recipes = response.recipes,
                        isLoading = false,
                        hasMorePages = response.has_next ?: false
                    )
                    
                    currentPage = 1
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to search recipes"
                    )
                }
            )
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            val result = if (isSearchMode) {
                recipeRepository.searchRecipes(currentSearchQuery, currentCategory)
            } else {
                recipeRepository.getRecipeFeed(page = 1, limit = 20)
            }
            
            result.fold(
                onSuccess = { response ->
                    val filteredRecipes = if (currentCategory.isNotEmpty() && !isSearchMode) {
                        response.recipes.filter { recipe ->
                            recipe.tags.any { tag -> tag.contains(currentCategory, ignoreCase = true) }
                        }
                    } else {
                        response.recipes
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        recipes = filteredRecipes,
                        isRefreshing = false,
                        hasMorePages = response.has_next ?: (response.recipes.size >= 10)
                    )
                    
                    currentPage = 1
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh recipes"
                    )
                }
            )
        }
    }

    fun loadMoreRecipes() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return

        _uiState.value = _uiState.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            val nextPage = currentPage + 1
            val result = if (isSearchMode) {
                recipeRepository.searchRecipes(currentSearchQuery, currentCategory, nextPage)
            } else {
                recipeRepository.getRecipeFeed(page = nextPage, limit = 20)
            }
            
            result.fold(
                onSuccess = { response ->
                    val filteredRecipes = if (currentCategory.isNotEmpty() && !isSearchMode) {
                        response.recipes.filter { recipe ->
                            recipe.tags.any { tag -> tag.contains(currentCategory, ignoreCase = true) }
                        }
                    } else {
                        response.recipes
                    }
                    
                    val updatedRecipes = _uiState.value.recipes + filteredRecipes
                    
                    _uiState.value = _uiState.value.copy(
                        recipes = updatedRecipes,
                        isLoadingMore = false,
                        hasMorePages = response.has_next ?: (response.recipes.size >= 10)
                    )
                    
                    currentPage = nextPage
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = error.message ?: "Failed to load more recipes"
                    )
                }
            )
        }
    }

    fun saveRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.saveRecipe(recipeId).fold(
                onSuccess = {
                    // Recipe saved successfully
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to save recipe"
                    )
                }
            )
        }
    }

    fun unsaveRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.unsaveRecipe(recipeId).fold(
                onSuccess = {
                    // Recipe unsaved successfully
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
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