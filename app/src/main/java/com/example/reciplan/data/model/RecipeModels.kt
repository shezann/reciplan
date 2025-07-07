package com.example.reciplan.data.model

import kotlinx.serialization.Serializable
import java.util.Date
import kotlinx.serialization.SerialName

@Serializable
data class Recipe(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("ingredients") val ingredients: List<Ingredient>,
    @SerialName("instructions") val instructions: List<String>,
    @SerialName("prep_time") val prep_time: Int, // minutes
    @SerialName("cook_time") val cook_time: Int, // minutes
    @SerialName("difficulty") val difficulty: Int, // 1-5
    @SerialName("servings") val servings: Int,
    @SerialName("tags") val tags: List<String>,
    @SerialName("nutrition") val nutrition: Nutrition,
    @SerialName("source_platform") val source_platform: String, // 'tiktok', 'instagram', 'youtube'
    @SerialName("source_url") val source_url: String, // Original video URL
    @SerialName("video_thumbnail") val video_thumbnail: String, // URL to thumbnail
    @SerialName("tiktok_author") val tiktok_author: String?, // Author username
    @SerialName("is_public") val is_public: Boolean,
    @SerialName("user_id") val user_id: String, // User who imported it
    @SerialName("saved_by") val saved_by: List<String>, // List of user IDs who saved it
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
    @SerialName("calories") val calories: Int = 0,
    @SerialName("protein") val protein: Float = 0.0f,
    @SerialName("carbs") val carbs: Float = 0.0f,
    @SerialName("fat") val fat: Float = 0.0f
)

// API Response Models
@Serializable
data class RecipeFeedResponse(
    val recipes: List<Recipe>,
    val total_count: Int,
    val page: Int,
    val limit: Int,
    val has_next: Boolean
)

@Serializable
data class SaveRecipeResponse(
    val message: String,
    val saved: Boolean
)

@Serializable
data class CreateRecipeRequest(
    val title: String,
    val description: String,
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val prep_time: Int,
    val cook_time: Int,
    val difficulty: Int,
    val servings: Int,
    val tags: List<String>,
    val nutrition: Nutrition,
    val source_platform: String,
    val source_url: String,
    val video_thumbnail: String,
    val tiktok_author: String? = null,
    val is_public: Boolean = true
)

@Serializable
data class RecipeSearchRequest(
    val query: String,
    val page: Int = 1,
    val limit: Int = 10,
    val tags: List<String> = emptyList(),
    val difficulty: Int? = null,
    val max_cook_time: Int? = null
)

@Serializable
data class RecipeDetailsResponse(
    @SerialName("recipe") val recipe: Recipe
) 