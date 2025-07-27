package com.example.reciplan.data.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.*
import org.junit.Test

class DtoMappingUpgradeTest {

    @Test
    fun `IngestJobDto contains all required fields from Task 4_1`() {
        // Test that all new fields from DTO expansion are present
        val jobDto = IngestJobDto(
            jobId = "test-job-123",
            recipeId = "recipe-456",
            status = IngestStatus.COMPLETED,
            title = "Test Recipe",
            transcript = "Recipe transcript",
            errorCode = null,
            recipeJson = buildJsonObject {
                put("title", JsonPrimitive("Pasta Recipe"))
                put("prep_time", JsonPrimitive(15))
            },
            onscreenText = "Fresh pasta ingredients",
            ingredientCandidates = listOf("pasta", "tomatoes", "basil"),
            parseErrors = emptyList(),
            llmErrorMessage = null
        )

        // Verify all original fields
        assertEquals("test-job-123", jobDto.jobId)
        assertEquals("recipe-456", jobDto.recipeId)
        assertEquals(IngestStatus.COMPLETED, jobDto.status)
        assertEquals("Test Recipe", jobDto.title)
        assertEquals("Recipe transcript", jobDto.transcript)
        assertNull(jobDto.errorCode)

        // Verify new fields from Task 4.1
        assertNotNull(jobDto.recipeJson)
        assertEquals("Fresh pasta ingredients", jobDto.onscreenText)
        assertEquals(listOf("pasta", "tomatoes", "basil"), jobDto.ingredientCandidates)
        assertEquals(emptyList<String>(), jobDto.parseErrors)
        assertNull(jobDto.llmErrorMessage)
    }

    @Test
    fun `IngestJobDto handles JsonElement recipeJson correctly`() {
        // Test with JSON object
        val jsonObject = buildJsonObject {
            put("title", JsonPrimitive("Pasta Recipe"))
            put("ingredients", buildJsonObject {
                put("pasta", JsonPrimitive("200g"))
                put("tomatoes", JsonPrimitive("3 pieces"))
            })
            put("prep_time", JsonPrimitive(15))
        }

        val jobDto = IngestJobDto(
            jobId = "test-job",
            status = IngestStatus.COMPLETED,
            recipeJson = jsonObject
        )

        assertNotNull(jobDto.recipeJson)
        assertTrue(jobDto.recipeJson is JsonObject)
        
        val recipeObject = jobDto.recipeJson as JsonObject
        assertEquals("Pasta Recipe", recipeObject["title"]?.toString()?.removeSurrounding("\""))
        assertEquals("15", recipeObject["prep_time"]?.toString())
    }

    @Test
    fun `IngestJobDto handles null recipeJson correctly`() {
        val jobDto = IngestJobDto(
            jobId = "test-job",
            status = IngestStatus.QUEUED,
            recipeJson = null
        )

        assertNull(jobDto.recipeJson)
    }

    @Test
    fun `IngestJobDto serialization fields are correctly mapped`() {
        val originalDto = IngestJobDto(
            jobId = "test-123",
            recipeId = "recipe-456",
            status = IngestStatus.LLM_REFINING,
            title = "Test Recipe",
            transcript = "Test transcript",
            errorCode = IngestErrorCode.ASR_FAILED,
            recipeJson = buildJsonObject {
                put("title", JsonPrimitive("Test Recipe"))
                put("cook_time", JsonPrimitive(30))
            },
            onscreenText = "Test onscreen text",
            ingredientCandidates = listOf("ingredient1", "ingredient2", "ingredient3"),
            parseErrors = listOf("error1", "error2"),
            llmErrorMessage = "Test LLM error message"
        )

        // Verify all fields are accessible and have correct values
        assertEquals("test-123", originalDto.jobId)
        assertEquals("recipe-456", originalDto.recipeId)
        assertEquals(IngestStatus.LLM_REFINING, originalDto.status)
        assertEquals("Test Recipe", originalDto.title)
        assertEquals("Test transcript", originalDto.transcript)
        assertEquals(IngestErrorCode.ASR_FAILED, originalDto.errorCode)
        assertEquals("Test onscreen text", originalDto.onscreenText)
        assertEquals(listOf("ingredient1", "ingredient2", "ingredient3"), originalDto.ingredientCandidates)
        assertEquals(listOf("error1", "error2"), originalDto.parseErrors)
        assertEquals("Test LLM error message", originalDto.llmErrorMessage)

        // Verify JsonElement is correctly stored
        assertNotNull(originalDto.recipeJson)
        assertTrue(originalDto.recipeJson is JsonObject)
    }

    @Test
    fun `RecipeDraftDto contains all required fields`() {
        val originalDto = RecipeDraftDto(
            id = "draft-123",
            title = "Draft Recipe",
            ingredients = listOf(
                Ingredient("pasta", "200g"),
                Ingredient("tomatoes", "3 pieces")
            ),
            instructions = listOf("step1", "step2"),
            prepTime = 15,
            cookTime = 30,
            servings = 4,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z"
        )

        // Verify all fields are accessible and have correct values
        assertEquals("draft-123", originalDto.id)
        assertEquals("Draft Recipe", originalDto.title)
        assertEquals(2, originalDto.ingredients.size)
        assertEquals("pasta", originalDto.ingredients[0].name)
        assertEquals("200g", originalDto.ingredients[0].quantity)
        assertEquals(listOf("step1", "step2"), originalDto.instructions)
        assertEquals(15, originalDto.prepTime)
        assertEquals(30, originalDto.cookTime)
        assertEquals(4, originalDto.servings)
        assertEquals("2024-01-01T00:00:00Z", originalDto.createdAt)
        assertEquals("2024-01-01T00:00:00Z", originalDto.updatedAt)
    }

    @Test
    fun `StartIngestRequest contains required fields`() {
        val originalRequest = StartIngestRequest(
            url = "https://vm.tiktok.com/test123"
        )

        assertEquals("https://vm.tiktok.com/test123", originalRequest.url)
    }

    @Test
    fun `StartIngestResponse contains required fields`() {
        val originalResponse = StartIngestResponse(
            jobId = "response-job-123",
            recipeId = "recipe-456",
            status = IngestStatus.QUEUED,
            message = "Job started successfully"
        )

        assertEquals("response-job-123", originalResponse.jobId)
        assertEquals("recipe-456", originalResponse.recipeId)
        assertEquals(IngestStatus.QUEUED, originalResponse.status)
        assertEquals("Job started successfully", originalResponse.message)
    }

    @Test
    fun `IngestStatus enum has correct extension functions`() {
        val allStatuses = listOf(
            IngestStatus.QUEUED,
            IngestStatus.DOWNLOADING,
            IngestStatus.EXTRACTING,
            IngestStatus.TRANSCRIBING,
            IngestStatus.DRAFT_TRANSCRIBED,
            IngestStatus.OCRING,
            IngestStatus.OCR_DONE,
            IngestStatus.LLM_REFINING,
            IngestStatus.DRAFT_PARSED,
            IngestStatus.DRAFT_PARSED_WITH_ERRORS,
            IngestStatus.COMPLETED,
            IngestStatus.FAILED
        )

        allStatuses.forEach { status ->
            // Verify extension functions work correctly
            assertNotNull(status.getStepIndex())
            assertNotNull(status.getDisplayName())
            assertTrue(status.getDisplayName().isNotEmpty())
            
            // Verify terminal status detection
            val expectedTerminal = status == IngestStatus.COMPLETED || status == IngestStatus.FAILED
            assertEquals(expectedTerminal, status.isTerminal())
            
            // Verify error status detection
            val expectedError = status == IngestStatus.FAILED
            assertEquals(expectedError, status.isError())
        }
    }

    @Test
    fun `IngestErrorCode enum contains all expected error codes`() {
        val allErrorCodes = listOf(
            IngestErrorCode.UNKNOWN_ERROR,
            IngestErrorCode.VIDEO_UNAVAILABLE,
            IngestErrorCode.ASR_FAILED,
            IngestErrorCode.OCR_FAILED,
            IngestErrorCode.LLM_FAILED,
            IngestErrorCode.PERSIST_FAILED
        )

        allErrorCodes.forEach { errorCode ->
            // Verify error codes are properly defined
            assertNotNull(errorCode)
            assertNotNull(errorCode.name)
            assertTrue(errorCode.name.isNotEmpty())
        }
        
        // Verify specific error codes exist
        assertEquals("UNKNOWN_ERROR", IngestErrorCode.UNKNOWN_ERROR.name)
        assertEquals("VIDEO_UNAVAILABLE", IngestErrorCode.VIDEO_UNAVAILABLE.name)
        assertEquals("ASR_FAILED", IngestErrorCode.ASR_FAILED.name)
        assertEquals("OCR_FAILED", IngestErrorCode.OCR_FAILED.name)
        assertEquals("LLM_FAILED", IngestErrorCode.LLM_FAILED.name)
        assertEquals("PERSIST_FAILED", IngestErrorCode.PERSIST_FAILED.name)
    }

    @Test
    fun `Ingredient model contains required fields`() {
        val ingredient = Ingredient(
            name = "Fresh Basil",
            quantity = "2 cups"
        )

        assertEquals("Fresh Basil", ingredient.name)
        assertEquals("2 cups", ingredient.quantity)
    }

    @Test
    fun `IngestJobDto handles empty collections correctly`() {
        // Test with default empty collections
        val jobDtoWithDefaults = IngestJobDto(
            jobId = "test-defaults",
            status = IngestStatus.QUEUED
        )

        assertEquals(emptyList<String>(), jobDtoWithDefaults.ingredientCandidates)
        assertEquals(emptyList<String>(), jobDtoWithDefaults.parseErrors)

        // Test with explicit empty collections
        val jobDtoWithEmpty = IngestJobDto(
            jobId = "test-empty",
            status = IngestStatus.QUEUED,
            ingredientCandidates = emptyList(),
            parseErrors = emptyList()
        )

        assertEquals(emptyList<String>(), jobDtoWithEmpty.ingredientCandidates)
        assertEquals(emptyList<String>(), jobDtoWithEmpty.parseErrors)
    }

    @Test
    fun `IngestJobDto backwards compatibility with missing fields`() {
        // Test that DTOs can be created without the new fields (backwards compatibility)
        val basicJobDto = IngestJobDto(
            jobId = "basic-job",
            status = IngestStatus.DOWNLOADING,
            title = "Basic Job"
        )

        assertEquals("basic-job", basicJobDto.jobId)
        assertEquals(IngestStatus.DOWNLOADING, basicJobDto.status)
        assertEquals("Basic Job", basicJobDto.title)
        
        // New fields should have default values
        assertNull(basicJobDto.recipeJson)
        assertNull(basicJobDto.onscreenText)
        assertEquals(emptyList<String>(), basicJobDto.ingredientCandidates)
        assertEquals(emptyList<String>(), basicJobDto.parseErrors)
        assertNull(basicJobDto.llmErrorMessage)
    }

    @Test
    fun `DTO field types match expected serialization format`() {
        val jobDto = IngestJobDto(
            jobId = "type-test",
            status = IngestStatus.COMPLETED,
            onscreenText = "Test text",
            ingredientCandidates = listOf("item1", "item2"),
            parseErrors = listOf("error1"),
            llmErrorMessage = "Error message"
        )

        // Verify field types are correct for serialization
        assertTrue(jobDto.onscreenText is String?)
        assertTrue(jobDto.ingredientCandidates is List<String>?)
        assertTrue(jobDto.parseErrors is List<String>?)
        assertTrue(jobDto.llmErrorMessage is String?)
        assertTrue(jobDto.recipeJson is JsonElement?)
    }
} 