package com.example.reciplan.data.api

import com.example.reciplan.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface RecipeApi {
    
    @GET("api/recipes/feed")
    suspend fun getRecipeFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<RecipeFeedResponse>
    
    @GET("api/recipes/{id}")
    suspend fun getRecipe(
        @Path("id") recipeId: String
    ): Response<RecipeResponse>
    
    @POST("api/recipes")
    suspend fun createRecipe(
        @Body request: CreateRecipeRequest
    ): Response<RecipeResponse>
    
    @PUT("api/recipes/{id}")
    suspend fun updateRecipe(
        @Path("id") recipeId: String,
        @Body request: UpdateRecipeRequest
    ): Response<RecipeResponse>
    
    @DELETE("api/recipes/{id}")
    suspend fun deleteRecipe(
        @Path("id") recipeId: String
    ): Response<DeleteRecipeResponse>
    
    // Additional recipe operations
    @POST("api/recipes/{id}/save")
    suspend fun saveRecipe(
        @Path("id") recipeId: String
    ): Response<SaveRecipeResponse>
    
    @DELETE("api/recipes/{id}/save")
    suspend fun unsaveRecipe(
        @Path("id") recipeId: String
    ): Response<SaveRecipeResponse>
    
    @GET("api/recipes/saved")
    suspend fun getSavedRecipes(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<SavedRecipesResponse>
    
    // Development/Testing endpoint
    @POST("api/recipes/seed")
    suspend fun seedRecipes(): Response<Unit>
} 