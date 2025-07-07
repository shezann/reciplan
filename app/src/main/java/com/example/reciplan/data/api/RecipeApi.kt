package com.example.reciplan.data.api

import com.example.reciplan.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface RecipeApi {
    
    @GET("api/recipes/feed")
    suspend fun getRecipeFeed(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 10
    ): Response<RecipeFeedResponse>
    
    @GET("api/recipes/{id}")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: String
    ): Response<RecipeDetailsResponse>
    
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
    ): Response<RecipeFeedResponse>
    
    @POST("api/recipes")
    suspend fun createRecipe(
        @Body request: CreateRecipeRequest
    ): Response<Recipe>
    
    @POST("api/recipes/search")
    suspend fun searchRecipes(
        @Body request: RecipeSearchRequest
    ): Response<RecipeFeedResponse>
    
    @POST("api/recipes/seed")
    suspend fun seedRecipes(): Response<Map<String, String>>
} 