package com.example.reciplan.ui.recipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.Recipe
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val recipeApi: RecipeApi,
    private val recipeId: String
) : ViewModel() {

    private val _recipe = MutableLiveData<Recipe?>()
    val recipe: LiveData<Recipe?> = _recipe

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        loadRecipe()
    }

    private fun loadRecipe() {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val response = recipeApi.getRecipeDetails(recipeId)
                if (response.isSuccessful) {
                    _recipe.value = response.body()?.recipe
                } else {
                    _error.value = "Failed to load recipe: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveRecipe() {
        val currentRecipe = _recipe.value ?: return
        
        viewModelScope.launch {
            try {
                val response = recipeApi.saveRecipe(recipeId)
                if (response.isSuccessful) {
                    // Update local state
                    _recipe.value = currentRecipe.copy(
                        saved_by = currentRecipe.saved_by + "current_user_id"
                    )
                } else {
                    _error.value = "Failed to save recipe: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun unsaveRecipe() {
        val currentRecipe = _recipe.value ?: return
        
        viewModelScope.launch {
            try {
                val response = recipeApi.unsaveRecipe(recipeId)
                if (response.isSuccessful) {
                    // Update local state
                    _recipe.value = currentRecipe.copy(
                        saved_by = currentRecipe.saved_by - "current_user_id"
                    )
                } else {
                    _error.value = "Failed to unsave recipe: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }

    fun refresh() {
        loadRecipe()
    }
} 