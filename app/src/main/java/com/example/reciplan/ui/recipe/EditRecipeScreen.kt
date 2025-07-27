package com.example.reciplan.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecipeScreen(
    recipeId: String,
    onNavigateBack: () -> Unit,
    onRecipeUpdated: () -> Unit,
    onRecipeDeleted: () -> Unit = { onNavigateBack() },
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    
    // Load the recipe when screen opens
    LaunchedEffect(recipeId) {
        viewModel.getRecipe(recipeId)
    }
    
    // Form state - initialize with existing recipe data
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf(listOf<Ingredient>()) }
    var instructions by remember { mutableStateOf(listOf<String>()) }
    var prepTime by remember { mutableStateOf("") }
    var cookTime by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(1) }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var currentTag by remember { mutableStateOf("") }
    var sourcePlatform by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var videoThumbnail by remember { mutableStateOf("") }
    var tiktokAuthor by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    
    // Nutrition fields
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    
    // New ingredient/instruction fields
    var newIngredientName by remember { mutableStateOf("") }
    var newIngredientQuantity by remember { mutableStateOf("") }
    var newInstruction by remember { mutableStateOf("") }
    
    // Error handling
    var titleError by remember { mutableStateOf("") }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Initialize form with existing recipe data
    LaunchedEffect(selectedRecipe) {
        selectedRecipe?.let { recipe ->
            title = recipe.title
            description = recipe.description ?: ""
            ingredients = recipe.ingredients
            instructions = recipe.instructions
            prepTime = recipe.prepTime.toString()
            cookTime = recipe.cookTime.toString()
            servings = recipe.servings.toString()
            difficulty = recipe.difficulty
            tags = recipe.tags
            sourcePlatform = recipe.sourcePlatform ?: ""
            sourceUrl = recipe.sourceUrl ?: ""
            videoThumbnail = recipe.videoThumbnail ?: ""
            tiktokAuthor = recipe.tiktokAuthor ?: ""
            isPublic = recipe.isPublic
            
            // Initialize nutrition if available
            recipe.nutrition?.let { nutrition ->
                calories = nutrition.calories?.toString() ?: ""
                protein = nutrition.protein?.toString() ?: ""
                carbs = nutrition.carbs?.toString() ?: ""
                fat = nutrition.fat?.toString() ?: ""
            }
        }
    }
    
    // Handle success - navigate back when recipe is updated or deleted
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("updated") == true) {
            onRecipeUpdated()
            viewModel.clearSuccessMessage()
        } else if (uiState.successMessage?.contains("deleted") == true) {
            onRecipeDeleted()
            viewModel.clearSuccessMessage()
        }
    }
    
    // Validation
    fun validateForm(): Boolean {
        titleError = if (title.isBlank()) "Title is required" else ""
        return titleError.isEmpty()
    }
    
    // Update recipe
    fun updateRecipe() {
        if (validateForm()) {
            val nutrition = if (calories.isNotBlank() || protein.isNotBlank() || carbs.isNotBlank() || fat.isNotBlank()) {
                Nutrition(
                    calories = calories.toDoubleOrNull(),
                    protein = protein.toDoubleOrNull(),
                    carbs = carbs.toDoubleOrNull(),
                    fat = fat.toDoubleOrNull()
                )
            } else null
            
            val request = UpdateRecipeRequest(
                title = title,
                description = description.ifBlank { null },
                ingredients = ingredients,
                instructions = instructions,
                prepTime = prepTime.toIntOrNull(),
                cookTime = cookTime.toIntOrNull(),
                difficulty = difficulty,
                servings = servings.toIntOrNull(),
                tags = tags,
                nutrition = nutrition,
                sourcePlatform = sourcePlatform.ifBlank { null },
                sourceUrl = sourceUrl.ifBlank { null },
                videoThumbnail = videoThumbnail.ifBlank { null },
                tiktokAuthor = tiktokAuthor.ifBlank { null },
                isPublic = isPublic
            )
            
            viewModel.updateRecipe(recipeId, request)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = { Text("Edit Recipe") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Delete Button
                IconButton(
                    onClick = { 
                        // Show confirmation dialog
                        showDeleteDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Recipe",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // Save Button
                Button(
                    onClick = { updateRecipe() },
                    enabled = !uiState.isLoading && title.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        )
        
        // Error Message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Loading state while fetching recipe
        if (selectedRecipe == null && uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Form Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Basic Information",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Title
                            OutlinedTextField(
                                value = title,
                                onValueChange = { 
                                    title = it
                                    titleError = ""
                                },
                                label = { Text("Recipe Title *") },
                                isError = titleError.isNotEmpty(),
                                supportingText = if (titleError.isNotEmpty()) {
                                    { Text(titleError) }
                                } else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Description
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Time and Servings Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = prepTime,
                                    onValueChange = { prepTime = it },
                                    label = { Text("Prep Time (min)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                OutlinedTextField(
                                    value = cookTime,
                                    onValueChange = { cookTime = it },
                                    label = { Text("Cook Time (min)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                OutlinedTextField(
                                    value = servings,
                                    onValueChange = { servings = it },
                                    label = { Text("Servings") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Difficulty
                            Text(
                                text = "Difficulty Level: $difficulty",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Slider(
                                value = difficulty.toFloat(),
                                onValueChange = { difficulty = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Public Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Make recipe public")
                                Switch(
                                    checked = isPublic,
                                    onCheckedChange = { isPublic = it }
                                )
                            }
                        }
                    }
                }
                
                // Ingredients
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Ingredients",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Add Ingredient
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newIngredientName,
                                    onValueChange = { newIngredientName = it },
                                    label = { Text("Ingredient") },
                                    modifier = Modifier.weight(2f)
                                )
                                
                                OutlinedTextField(
                                    value = newIngredientQuantity,
                                    onValueChange = { newIngredientQuantity = it },
                                    label = { Text("Quantity") },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (newIngredientName.isNotBlank() && newIngredientQuantity.isNotBlank()) {
                                            ingredients = ingredients + Ingredient(
                                                name = newIngredientName,
                                                quantity = newIngredientQuantity
                                            )
                                            newIngredientName = ""
                                            newIngredientQuantity = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Ingredients List
                            ingredients.forEachIndexed { index, ingredient ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ingredient.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = ingredient.quantity,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            ingredients = ingredients.filterIndexed { i, _ -> i != index }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Ingredient",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Instructions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Instructions",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Add Instruction
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newInstruction,
                                    onValueChange = { newInstruction = it },
                                    label = { Text("Instruction") },
                                    modifier = Modifier.weight(1f),
                                    minLines = 2
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (newInstruction.isNotBlank()) {
                                            instructions = instructions + newInstruction
                                            newInstruction = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Instruction")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Instructions List
                            instructions.forEachIndexed { index, instruction ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Step ${index + 1}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = instruction,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            instructions = instructions.filterIndexed { i, _ -> i != index }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Instruction",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Tags
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Add Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentTag,
                                    onValueChange = { currentTag = it },
                                    label = { Text("Add Tag") },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (currentTag.isNotBlank() && !tags.contains(currentTag)) {
                                            tags = tags + currentTag
                                            currentTag = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Tag")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Tags List
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(tags) { tag ->
                                    AssistChip(
                                        onClick = {
                                            tags = tags.filter { it != tag }
                                        },
                                        label = { Text(tag) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove Tag",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Optional: Nutrition Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Nutrition Information (Optional)",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = calories,
                                    onValueChange = { calories = it },
                                    label = { Text("Calories") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                OutlinedTextField(
                                    value = protein,
                                    onValueChange = { protein = it },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = carbs,
                                    onValueChange = { carbs = it },
                                    label = { Text("Carbs (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                OutlinedTextField(
                                    value = fat,
                                    onValueChange = { fat = it },
                                    label = { Text("Fat (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Optional: Source Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Source Information (Optional)",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = sourcePlatform,
                                onValueChange = { sourcePlatform = it },
                                label = { Text("Platform (e.g., Instagram, TikTok)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = sourceUrl,
                                onValueChange = { sourceUrl = it },
                                label = { Text("Source URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = videoThumbnail,
                                onValueChange = { videoThumbnail = it },
                                label = { Text("Video Thumbnail URL") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = tiktokAuthor,
                                onValueChange = { tiktokAuthor = it },
                                label = { Text("TikTok Author") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete this recipe? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecipe(recipeId)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 