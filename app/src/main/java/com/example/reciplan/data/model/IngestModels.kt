package com.example.reciplan.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

// Status enum for ingest job states
@Serializable
enum class IngestStatus {
    @SerialName("QUEUED")
    QUEUED,
    
    @SerialName("DOWNLOADING")
    DOWNLOADING,
    
    @SerialName("EXTRACTING")
    EXTRACTING,
    
    @SerialName("TRANSCRIBING")
    TRANSCRIBING,
    
    @SerialName("DRAFT_TRANSCRIBED")
    DRAFT_TRANSCRIBED,
    
    @SerialName("OCRING")
    OCRING,
    
    @SerialName("OCR_DONE")
    OCR_DONE,
    
    @SerialName("LLM_REFINING")
    LLM_REFINING,
    
    @SerialName("DRAFT_PARSED")
    DRAFT_PARSED,
    
    @SerialName("DRAFT_PARSED_WITH_ERRORS")
    DRAFT_PARSED_WITH_ERRORS,
    
    @SerialName("COMPLETED")
    COMPLETED,
    
    @SerialName("FAILED")
    FAILED
}

// Error codes for ingest failures
@Serializable
enum class IngestErrorCode {
    @SerialName("VIDEO_UNAVAILABLE")
    VIDEO_UNAVAILABLE,
    
    @SerialName("ASR_FAILED")
    ASR_FAILED,
    
    @SerialName("OCR_FAILED")
    OCR_FAILED,
    
    @SerialName("LLM_FAILED")
    LLM_FAILED,
    
    @SerialName("PERSIST_FAILED")
    PERSIST_FAILED,
    
    @SerialName("UNKNOWN_ERROR")
    UNKNOWN_ERROR
}

// Main ingest job DTO
@Serializable
data class IngestJobDto(
    @SerialName("job_id")
    val jobId: String? = null,
    
    @SerialName("recipe_id")
    val recipeId: String? = null,
    
    val status: IngestStatus,
    
    val title: String? = null,
    
    val transcript: String? = null,
    
    @SerialName("error_code")
    val errorCode: IngestErrorCode? = null,
    
    @SerialName("recipe_json")
    val recipeJson: JsonElement? = null,
    
    @SerialName("onscreen_text")
    val onscreenText: String? = null,
    
    @SerialName("ingredient_candidates")
    val ingredientCandidates: List<String> = emptyList(),
    
    @SerialName("parse_errors")
    val parseErrors: List<String> = emptyList(),
    
    @SerialName("llm_error_message")
    val llmErrorMessage: String? = null
)

// Recipe draft DTO for the extracted recipe data
@Serializable
data class RecipeDraftDto(
    val id: String,
    val title: String,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val description: String? = null,
    @SerialName("prep_time")
    val prepTime: Int = 0,
    @SerialName("cook_time")
    val cookTime: Int = 0,
    val difficulty: Int = 1,
    val servings: Int = 1,
    val tags: List<String> = emptyList(),
    @SerialName("source_platform")
    val sourcePlatform: String = "tiktok",
    @SerialName("source_url")
    val sourceUrl: String? = null,
    @SerialName("video_thumbnail")
    val videoThumbnail: String? = null,
    @SerialName("tiktok_author")
    val tiktokAuthor: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

// Request DTO for starting an ingest job
@Serializable
data class StartIngestRequest(
    val url: String
)

// Response DTO for starting an ingest job
@Serializable
data class StartIngestResponse(
    @SerialName("job_id")
    val jobId: String,
    
    @SerialName("recipe_id")
    val recipeId: String? = null,
    
    val status: IngestStatus,
    
    val message: String? = null
)

// Helper extension functions for status checking
fun IngestStatus.isTerminal(): Boolean {
    return this == IngestStatus.COMPLETED || this == IngestStatus.FAILED
}

fun IngestStatus.isError(): Boolean {
    return this == IngestStatus.FAILED
}

fun IngestStatus.getStepIndex(): Int {
    return when (this) {
        IngestStatus.QUEUED -> 0
        IngestStatus.DOWNLOADING -> 1
        IngestStatus.EXTRACTING -> 2
        IngestStatus.TRANSCRIBING -> 3
        IngestStatus.DRAFT_TRANSCRIBED -> 4
        IngestStatus.OCRING -> 5
        IngestStatus.OCR_DONE -> 6
        IngestStatus.LLM_REFINING -> 7
        IngestStatus.DRAFT_PARSED -> 8
        IngestStatus.DRAFT_PARSED_WITH_ERRORS -> 8
        IngestStatus.COMPLETED -> 9
        IngestStatus.FAILED -> -1
    }
}

fun IngestStatus.getDisplayName(): String {
    return when (this) {
        IngestStatus.QUEUED -> "Queued"
        IngestStatus.DOWNLOADING -> "Downloading"
        IngestStatus.EXTRACTING -> "Extracting"
        IngestStatus.TRANSCRIBING -> "Transcribing"
        IngestStatus.DRAFT_TRANSCRIBED -> "Draft Transcribed"
        IngestStatus.OCRING -> "OCR Processing"
        IngestStatus.OCR_DONE -> "OCR Complete"
        IngestStatus.LLM_REFINING -> "AI Refining"
        IngestStatus.DRAFT_PARSED -> "Draft Parsed"
        IngestStatus.DRAFT_PARSED_WITH_ERRORS -> "Draft Parsed (with errors)"
        IngestStatus.COMPLETED -> "Completed"
        IngestStatus.FAILED -> "Failed"
    }
} 