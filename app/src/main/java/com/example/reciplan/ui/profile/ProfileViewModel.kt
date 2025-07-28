package com.example.reciplan.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.data.repository.LikeRepository
import com.example.reciplan.data.repository.LikeState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for ProfileScreen
 * Handles user recipes, loading states, and like interactions
 */
class ProfileViewModel(
    private val recipeRepository: RecipeRepository,
    private val likeRepository: LikeRepository
) : ViewModel() {
    
    private val _userRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val userRecipes: StateFlow<List<Recipe>> = _userRecipes.asStateFlow()
    
    private val _isLoadingRecipes = MutableStateFlow(false)
    val isLoadingRecipes: StateFlow<Boolean> = _isLoadingRecipes.asStateFlow()
    
    private val _recipeLikeStates = MutableStateFlow<Map<String, LikeState>>(emptyMap())
    val recipeLikeStates: StateFlow<Map<String, LikeState>> = _recipeLikeStates.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Observe like states from repository
        viewModelScope.launch {
            likeRepository.likeStates.collect { likeStates ->
                _recipeLikeStates.value = likeStates
            }
        }
    }
    
    /**
     * Load user's recipes (recipes created OR liked by the user)
     */
    fun loadUserRecipes() {
        viewModelScope.launch {
            _isLoadingRecipes.value = true
            _errorMessage.value = null
            
            try {
                println("ProfileViewModel: Loading user recipes...")
                val result = recipeRepository.getMyRecipes(page = 1, limit = 50)
                
                result.fold(
                    onSuccess = { response ->
                        println("ProfileViewModel: Successfully loaded ${response.recipes.size} user recipes")
                        _userRecipes.value = response.recipes
                        
                        // Initialize like states for these recipes
                        response.recipes.forEach { recipe ->
                            likeRepository.updateLikeStateOptimistically(recipe.id, recipe.liked, recipe.likesCount)
                        }
                    },
                    onFailure = { error ->
                        println("ProfileViewModel: Error loading user recipes: ${error.message}")
                        _errorMessage.value = "Failed to load your recipes: ${error.message}"
                        _userRecipes.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                println("ProfileViewModel: Exception loading user recipes: ${e.message}")
                _errorMessage.value = "Failed to load your recipes: ${e.message}"
                _userRecipes.value = emptyList()
            } finally {
                _isLoadingRecipes.value = false
            }
        }
    }
    
    /**
     * Toggle like state for a recipe
     */
    fun toggleLike(recipeId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            try {
                println("ProfileViewModel: Toggling like for recipe $recipeId, currently liked: $currentlyLiked")
                
                likeRepository.toggleLike(recipeId, currentlyLiked)
                
                // Update the recipe in our local list to reflect the new like state
                val updatedRecipes = _userRecipes.value.map { recipe ->
                    if (recipe.id == recipeId) {
                        recipe.copy(
                            liked = !currentlyLiked,
                            likesCount = if (currentlyLiked) recipe.likesCount - 1 else recipe.likesCount + 1
                        )
                    } else {
                        recipe
                    }
                }
                _userRecipes.value = updatedRecipes
                
            } catch (e: Exception) {
                println("ProfileViewModel: Error toggling like: ${e.message}")
                _errorMessage.value = "Failed to update like: ${e.message}"
            }
        }
    }
    
    /**
     * Refresh user recipes
     */
    fun refreshRecipes() {
        loadUserRecipes()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
} 