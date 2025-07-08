package com.example.reciplan.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Recipe Data Models
@Serializable
data class Recipe(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("ingredients") val ingredients: List<Ingredient> = emptyList(),
    @SerialName("instructions") val instructions: List<String> = emptyList(),
    @SerialName("prep_time") val prep_time: Int = 0, // minutes
    @SerialName("cook_time") val cook_time: Int = 0, // minutes
    @SerialName("difficulty") val difficulty: Int = 1, // 1-5
    @SerialName("servings") val servings: Int = 1,
    @SerialName("rating") val rating: Float? = null, // 1-5 stars
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("nutrition") val nutrition: Nutrition? = null,
    @SerialName("source_platform") val source_platform: String? = null, // 'tiktok', 'instagram', 'youtube'
    @SerialName("source_url") val source_url: String? = null, // Original video URL
    @SerialName("video_thumbnail") val video_thumbnail: String? = null, // URL to thumbnail
    @SerialName("tiktok_author") val tiktok_author: String? = null, // Author username
    @SerialName("is_public") val is_public: Boolean = true,
    @SerialName("user_id") val user_id: String, // User who imported it
    @SerialName("saved_by") val saved_by: List<String> = emptyList(), // List of user IDs who saved it
    @SerialName("created_at") val created_at: String, // ISO date string
    @SerialName("updated_at") val updated_at: String // ISO date string
)

@Serializable
data class Ingredient(
    @SerialName("name") val name: String,
    @SerialName("quantity") val quantity: String
)

@Serializable
data class Nutrition(
    @SerialName("calories") val calories: Double? = null,
    @SerialName("protein") val protein: Double? = null,
    @SerialName("carbs") val carbs: Double? = null,
    @SerialName("fat") val fat: Double? = null
)

// Request Models
@Serializable
data class CreateRecipeRequest(
    val title: String,
    val description: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    @SerialName("prep_time") val prep_time: Int = 0,
    @SerialName("cook_time") val cook_time: Int = 0,
    val difficulty: Int = 1,
    val servings: Int = 1,
    val tags: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    @SerialName("source_platform") val source_platform: String? = null,
    @SerialName("source_url") val source_url: String? = null,
    @SerialName("video_thumbnail") val video_thumbnail: String? = null,
    @SerialName("tiktok_author") val tiktok_author: String? = null,
    @SerialName("is_public") val is_public: Boolean = true
)

@Serializable
data class UpdateRecipeRequest(
    val title: String? = null,
    val description: String? = null,
    val ingredients: List<Ingredient>? = null,
    val instructions: List<String>? = null,
    @SerialName("prep_time") val prep_time: Int? = null,
    @SerialName("cook_time") val cook_time: Int? = null,
    val difficulty: Int? = null,
    val servings: Int? = null,
    val tags: List<String>? = null,
    val nutrition: Nutrition? = null,
    @SerialName("source_platform") val source_platform: String? = null,
    @SerialName("source_url") val source_url: String? = null,
    @SerialName("video_thumbnail") val video_thumbnail: String? = null,
    @SerialName("tiktok_author") val tiktok_author: String? = null,
    @SerialName("is_public") val is_public: Boolean? = null
)

@Serializable
data class RecipeSearchRequest(
    val query: String,
    val page: Int = 1,
    val limit: Int = 10,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val difficulty: Int? = null,
    val max_cook_time: Int? = null
)

// Response Models
@Serializable
data class RecipeResponse(
    val message: String? = null,
    val recipe: Recipe
)

@Serializable
data class RecipeFeedResponse(
    val recipes: List<Recipe>,
    val page: Int,
    val limit: Int,
    @SerialName("total_returned") val totalReturned: Int = 0,
    val total_count: Int? = null,
    val has_next: Boolean? = null
)

@Serializable
data class DeleteRecipeResponse(
    val message: String,
    @SerialName("recipe_id") val recipeId: String
)

@Serializable
data class SaveRecipeResponse(
    val message: String,
    @SerialName("recipe_id") val recipeId: String? = null,
    val saved: Boolean? = null
)

@Serializable
data class SavedRecipesResponse(
    val recipes: List<Recipe>,
    val page: Int,
    val limit: Int,
    @SerialName("total_returned") val totalReturned: Int = 0,
    val has_next: Boolean? = null
)

@Serializable
data class RecipeDetailsResponse(
    @SerialName("recipe") val recipe: Recipe
) 