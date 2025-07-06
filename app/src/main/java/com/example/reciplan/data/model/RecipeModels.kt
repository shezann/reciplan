package com.example.reciplan.data.model

import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val prep_time: Int, // minutes
    val cook_time: Int, // minutes
    val difficulty: Int, // 1-5
    val servings: Int,
    val tags: List<String>,
    val nutrition: Nutrition,
    val source_platform: String, // 'tiktok', 'instagram', 'youtube'
    val source_url: String, // Original video URL
    val video_thumbnail: String, // URL to thumbnail
    val tiktok_author: String?, // Author username
    val is_public: Boolean,
    val user_id: String, // User who imported it
    val saved_by: List<String>, // List of user IDs who saved it
    val created_at: String, // ISO date string
    val updated_at: String // ISO date string
)

@Serializable
data class Ingredient(
    val name: String,
    val quantity: String
)

@Serializable
data class Nutrition(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float
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