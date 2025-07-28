package com.example.reciplan.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// Recipe Data Models
@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    @SerialName("prep_time") val prepTime: Int = 0,
    @SerialName("cook_time") val cookTime: Int = 0,
    val difficulty: Int = 1, // 1-5 scale
    val servings: Int = 1,
    val tags: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    @SerialName("source_platform") val sourcePlatform: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("video_thumbnail") val videoThumbnail: String? = null,
    @SerialName("tiktok_author") val tiktokAuthor: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Parcelize
@Serializable
data class Ingredient(
    val name: String,
    val quantity: String
) : Parcelable

@Serializable
data class Nutrition(
    val calories: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null
)

// Request Models
@Serializable
data class CreateRecipeRequest(
    val title: String,
    val description: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    @SerialName("prep_time") val prepTime: Int = 0,
    @SerialName("cook_time") val cookTime: Int = 0,
    val difficulty: Int = 1,
    val servings: Int = 1,
    val tags: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    @SerialName("source_platform") val sourcePlatform: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("video_thumbnail") val videoThumbnail: String? = null,
    @SerialName("tiktok_author") val tiktokAuthor: String? = null,
    @SerialName("is_public") val isPublic: Boolean = true
)

@Serializable
data class UpdateRecipeRequest(
    val title: String? = null,
    val description: String? = null,
    val ingredients: List<Ingredient>? = null,
    val instructions: List<String>? = null,
    @SerialName("prep_time") val prepTime: Int? = null,
    @SerialName("cook_time") val cookTime: Int? = null,
    val difficulty: Int? = null,
    val servings: Int? = null,
    val tags: List<String>? = null,
    val nutrition: Nutrition? = null,
    @SerialName("source_platform") val sourcePlatform: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("video_thumbnail") val videoThumbnail: String? = null,
    @SerialName("tiktok_author") val tiktokAuthor: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null
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
    @SerialName("total_returned") val totalReturned: Int = 0
)

@Serializable
data class DeleteRecipeResponse(
    val message: String,
    @SerialName("recipe_id") val recipeId: String
)

// Like-related DTOs
@Serializable
data class LikeResponse(
    val success: Boolean,
    val liked: Boolean,
    @SerialName("likes_count") val likesCount: Int,
    @SerialName("recipe_id") val recipeId: String
)

@Serializable
data class LikedStatusResponse(
    val liked: Boolean,
    @SerialName("likes_count") val likesCount: Int,
    @SerialName("recipe_id") val recipeId: String
)

@Serializable
data class LikeErrorResponse(
    val error: String,
    val code: String? = null,
    @SerialName("retry_after_seconds") val retryAfterSeconds: Int? = null
)

@Serializable
data class SaveRecipeResponse(
    val message: String,
    @SerialName("recipe_id") val recipeId: String
)

@Serializable
data class SavedRecipesResponse(
    val recipes: List<Recipe>,
    val page: Int,
    val limit: Int,
    @SerialName("total_returned") val totalReturned: Int = 0
) 