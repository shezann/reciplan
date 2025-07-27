package com.example.reciplan.data.recipe

import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

class RecipeRepository(
    private val recipeApi: RecipeApi
) {
    
    // Get recipe feed with pagination
    suspend fun getRecipeFeed(page: Int = 1, limit: Int = 10): Result<RecipeFeedResponse> {
        return try {
            println("RecipeRepository: Getting recipe feed - page: $page, limit: $limit")
            
            val response = recipeApi.getRecipeFeed(page, limit)
            println("RecipeRepository: Feed response code: ${response.code()}")
            println("RecipeRepository: Feed response headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                println("RecipeRepository: Feed response body: $body")
                if (body != null) {
                    println("RecipeRepository: Feed returned ${body.recipes.size} recipes")
                    body.recipes.forEach { recipe ->
                        println("RecipeRepository: Recipe - ID: ${recipe.id}, Title: ${recipe.title}, UserID: ${recipe.userId}")
                    }
                    Result.success(body)
                } else {
                    println("RecipeRepository: Empty response body despite successful feed response")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("RecipeRepository: Feed error response body: $errorBody")
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            println("RecipeRepository: Exception getting recipe feed: ${e.message}")
            e.printStackTrace()
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
            println("RecipeRepository: Creating recipe with title: ${request.title}")
            println("RecipeRepository: Request data: $request")
            
            val response = recipeApi.createRecipe(request)
            println("RecipeRepository: Response code: ${response.code()}")
            println("RecipeRepository: Response headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                println("RecipeRepository: Response body: $body")
                if (body != null) {
                    println("RecipeRepository: Created recipe with ID: ${body.recipe.id}")
                    Result.success(body.recipe)
                } else {
                    println("RecipeRepository: Empty response body despite successful response")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("RecipeRepository: Error response body: $errorBody")
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            println("RecipeRepository: Exception creating recipe: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Update existing recipe
    suspend fun updateRecipe(recipeId: String, request: UpdateRecipeRequest): Result<Recipe> {
        return try {
            println("RecipeRepository: Updating recipe with ID: $recipeId")
            println("RecipeRepository: Update request data: $request")
            
            val response = recipeApi.updateRecipe(recipeId, request)
            println("RecipeRepository: Update response code: ${response.code()}")
            println("RecipeRepository: Update response headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                println("RecipeRepository: Update response body: $body")
                if (body != null) {
                    println("RecipeRepository: Updated recipe with ID: ${body.recipe.id}")
                    Result.success(body.recipe)
                } else {
                    println("RecipeRepository: Empty response body despite successful update response")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("RecipeRepository: Update error response body: $errorBody")
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            println("RecipeRepository: Exception updating recipe: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Save recipe draft with JSON patch
    suspend fun saveDraft(recipeId: String, jsonPatch: JsonObject): Result<Recipe> {
        return try {
            println("RecipeRepository: Saving draft for recipe ID: $recipeId")
            println("RecipeRepository: JSON patch: $jsonPatch")
            
            // Convert JsonObject patch to UpdateRecipeRequest
            val updateRequest = convertJsonPatchToUpdateRequest(jsonPatch)
            println("RecipeRepository: Converted to UpdateRecipeRequest: $updateRequest")
            
            val response = recipeApi.updateRecipe(recipeId, updateRequest)
            println("RecipeRepository: Save draft response code: ${response.code()}")
            println("RecipeRepository: Save draft response headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                println("RecipeRepository: Save draft response body: $body")
                if (body != null) {
                    println("RecipeRepository: Successfully saved draft for recipe ID: ${body.recipe.id}")
                    Result.success(body.recipe)
                } else {
                    println("RecipeRepository: Empty response body despite successful save draft response")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("RecipeRepository: Save draft error response body: $errorBody")
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            println("RecipeRepository: Exception saving draft: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Helper function to convert JsonObject patch to UpdateRecipeRequest
    private fun convertJsonPatchToUpdateRequest(jsonPatch: JsonObject): UpdateRecipeRequest {
        return UpdateRecipeRequest(
            title = jsonPatch["title"]?.jsonPrimitive?.content,
            description = jsonPatch["description"]?.jsonPrimitive?.content,
            prepTime = jsonPatch["prep_time"]?.jsonPrimitive?.content?.toIntOrNull(),
            cookTime = jsonPatch["cook_time"]?.jsonPrimitive?.content?.toIntOrNull(),
            servings = jsonPatch["servings"]?.jsonPrimitive?.content?.toIntOrNull(),
            difficulty = jsonPatch["difficulty"]?.jsonPrimitive?.content?.toIntOrNull(),
            ingredients = jsonPatch["ingredients"]?.jsonArray?.map { ingredientJson ->
                val ingredientObj = ingredientJson.jsonObject
                Ingredient(
                    name = ingredientObj["name"]?.jsonPrimitive?.content ?: "",
                    quantity = ingredientObj["quantity"]?.jsonPrimitive?.content ?: ""
                )
            },
            instructions = jsonPatch["instructions"]?.jsonArray?.map { instructionJson ->
                instructionJson.jsonPrimitive.content
            },
            tags = jsonPatch["tags"]?.jsonArray?.map { tagJson ->
                tagJson.jsonPrimitive.content
            },
            sourcePlatform = jsonPatch["source_platform"]?.jsonPrimitive?.content,
            sourceUrl = jsonPatch["source_url"]?.jsonPrimitive?.content,
            videoThumbnail = jsonPatch["video_thumbnail"]?.jsonPrimitive?.content,
            tiktokAuthor = jsonPatch["tiktok_author"]?.jsonPrimitive?.content,
            isPublic = jsonPatch["is_public"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
        )
    }
    
    // Delete recipe
    suspend fun deleteRecipe(recipeId: String): Result<String> {
        return try {
            println("RecipeRepository: Deleting recipe with ID: $recipeId")
            
            val response = recipeApi.deleteRecipe(recipeId)
            println("RecipeRepository: Delete response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                println("RecipeRepository: Delete response body: $body")
                if (body != null) {
                    println("RecipeRepository: Successfully deleted recipe with ID: ${body.recipeId}")
                    Result.success(body.recipeId)
                } else {
                    println("RecipeRepository: Empty response body despite successful delete response")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("RecipeRepository: Delete error response body: $errorBody")
                Result.failure(handleError(response))
            }
        } catch (e: Exception) {
            println("RecipeRepository: Exception deleting recipe: ${e.message}")
            e.printStackTrace()
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
                    Result.success(body.recipeId)
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
                    Result.success(body.recipeId)
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