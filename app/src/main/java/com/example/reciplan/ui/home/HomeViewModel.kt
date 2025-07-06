package com.example.reciplan.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.model.RecipeSearchRequest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val recipeApi: RecipeApi
) : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _hasMorePages = MutableLiveData<Boolean>()
    val hasMorePages: LiveData<Boolean> = _hasMorePages

    private var currentPage = 1
    private var isSearchMode = false
    private var currentSearchQuery = ""

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        if (_isLoading.value == true || _isRefreshing.value == true) return

        _isLoading.value = true
        _error.value = ""

        viewModelScope.launch {
            try {
                val response = recipeApi.getRecipeFeed(page = 1, limit = 10)
                if (response.isSuccessful) {
                    val feedResponse = response.body()
                    if (feedResponse != null) {
                        _recipes.value = feedResponse.recipes
                        _hasMorePages.value = feedResponse.has_next
                        currentPage = 1
                        isSearchMode = false
                    }
                } else {
                    _error.value = "Failed to load recipes: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshRecipes() {
        if (_isLoading.value == true || _isRefreshing.value == true) return

        _isRefreshing.value = true
        _error.value = ""

        viewModelScope.launch {
            try {
                val response = if (isSearchMode) {
                    recipeApi.searchRecipes(RecipeSearchRequest(query = currentSearchQuery, page = 1, limit = 10))
                } else {
                    recipeApi.getRecipeFeed(page = 1, limit = 10)
                }

                if (response.isSuccessful) {
                    val feedResponse = response.body()
                    if (feedResponse != null) {
                        _recipes.value = feedResponse.recipes
                        _hasMorePages.value = feedResponse.has_next
                        currentPage = 1
                    }
                } else {
                    _error.value = "Failed to refresh recipes: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadMoreRecipes() {
        if (_isLoadingMore.value == true || _hasMorePages.value == false) return

        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val response = if (isSearchMode) {
                    recipeApi.searchRecipes(RecipeSearchRequest(query = currentSearchQuery, page = nextPage, limit = 10))
                } else {
                    recipeApi.getRecipeFeed(page = nextPage, limit = 10)
                }

                if (response.isSuccessful) {
                    val feedResponse = response.body()
                    if (feedResponse != null) {
                        val currentList = _recipes.value ?: emptyList()
                        _recipes.value = currentList + feedResponse.recipes
                        _hasMorePages.value = feedResponse.has_next
                        currentPage = nextPage
                    }
                } else {
                    _error.value = "Failed to load more recipes: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun searchRecipes(query: String) {
        if (query.trim().isEmpty()) {
            // If search is empty, load regular feed
            isSearchMode = false
            loadRecipes()
            return
        }

        isSearchMode = true
        currentSearchQuery = query.trim()
        _isLoading.value = true
        _error.value = ""

        viewModelScope.launch {
            try {
                val response = recipeApi.searchRecipes(
                    RecipeSearchRequest(query = currentSearchQuery, page = 1, limit = 10)
                )

                if (response.isSuccessful) {
                    val feedResponse = response.body()
                    if (feedResponse != null) {
                        _recipes.value = feedResponse.recipes
                        _hasMorePages.value = feedResponse.has_next
                        currentPage = 1
                    }
                } else {
                    _error.value = "Failed to search recipes: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                val response = recipeApi.saveRecipe(recipe.id)
                if (response.isSuccessful) {
                    // Update local state
                    val currentList = _recipes.value ?: emptyList()
                    val updatedList = currentList.map { 
                        if (it.id == recipe.id) {
                            it.copy(saved_by = it.saved_by + "current_user_id")
                        } else it
                    }
                    _recipes.value = updatedList
                } else {
                    _error.value = "Failed to save recipe"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun unsaveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                val response = recipeApi.unsaveRecipe(recipe.id)
                if (response.isSuccessful) {
                    // Update local state
                    val currentList = _recipes.value ?: emptyList()
                    val updatedList = currentList.map { 
                        if (it.id == recipe.id) {
                            it.copy(saved_by = it.saved_by - "current_user_id")
                        } else it
                    }
                    _recipes.value = updatedList
                } else {
                    _error.value = "Failed to unsave recipe"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }
}