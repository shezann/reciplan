package com.example.reciplan.data.recipe

import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

class RecipeRepository(
    private val recipeApi: RecipeApi
) {
    
    // Get recipe feed with pagination
    suspend fun getRecipeFeed(page: Int = 1, limit: Int = 10): Result<RecipeFeedResponse> {
        return try {
            val response = recipeApi.getRecipeFeed(page, limit)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return Result.success(body)
                } else {
                    return Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                return Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get single recipe by ID
    suspend fun getRecipe(recipeId: String): Result<Recipe> {
        return try {
            val response = recipeApi.getRecipe(recipeId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.recipe)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create a new recipe
    suspend fun createRecipe(request: CreateRecipeRequest): Result<Recipe> {
        return try {
            val response = recipeApi.createRecipe(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return Result.success(body.recipe)
                } else {
                    return Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                return Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update existing recipe
    suspend fun updateRecipe(recipeId: String, request: UpdateRecipeRequest): Result<Recipe> {
        return try {
            val response = recipeApi.updateRecipe(recipeId, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return Result.success(body.recipe)
                } else {
                    return Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                return Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete recipe
    suspend fun deleteRecipe(recipeId: String): Result<String> {
        return try {
            val response = recipeApi.deleteRecipe(recipeId)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return Result.success(body.recipeId)
                } else {
                    return Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                return Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Save/bookmark recipe
    suspend fun saveRecipe(recipeId: String): Result<String> {
        return try {
            val response = recipeApi.saveRecipe(recipeId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.recipeId ?: recipeId)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Unsave/unbookmark recipe
    suspend fun unsaveRecipe(recipeId: String): Result<String> {
        return try {
            val response = recipeApi.unsaveRecipe(recipeId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.recipeId ?: recipeId)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Search recipes
    suspend fun searchRecipes(query: String, category: String = "", page: Int = 1, limit: Int = 10): Result<RecipeFeedResponse> {
        return try {
            val searchRequest = RecipeSearchRequest(
                query = query,
                category = category.takeIf { it.isNotEmpty() },
                page = page,
                limit = limit
            )
            
            val response = recipeApi.searchRecipes(searchRequest)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get saved recipes
    suspend fun getSavedRecipes(page: Int = 1, limit: Int = 10): Result<SavedRecipesResponse> {
        return try {
            val response = recipeApi.getSavedRecipes(page, limit)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Flow-based recipe feed for UI consumption
    fun getRecipeFeedFlow(page: Int = 1, limit: Int = 10): Flow<Result<RecipeFeedResponse>> = flow {
        emit(getRecipeFeed(page, limit))
    }
    
    // Flow-based single recipe for UI consumption
    fun getRecipeFlow(recipeId: String): Flow<Result<Recipe>> = flow {
        emit(getRecipe(recipeId))
    }
    
    // Seed recipes for development/testing
    suspend fun seedRecipes(): Result<Unit> {
        return try {
            val response = recipeApi.seedRecipes()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper function to handle API errors
    private fun handleError(response: Response<*>): Exception {
        val errorMessage = when (response.code()) {
            400 -> "Bad Request: Invalid data provided"
            401 -> "Unauthorized: Please log in again"
            403 -> "Forbidden: You don't have permission to perform this action"
            404 -> "Not Found: Recipe not found"
            409 -> "Conflict: Recipe with this data already exists"
            422 -> "Validation Error: Please check your input"
            500 -> "Server Error: Please try again later"
            else -> "Network Error: ${response.message()}"
        }
        return Exception(errorMessage)
    }
} 