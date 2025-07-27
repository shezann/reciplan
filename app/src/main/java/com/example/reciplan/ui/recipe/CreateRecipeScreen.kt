package com.example.reciplan.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.CreateRecipeRequest
import com.example.reciplan.data.model.Ingredient
import com.example.reciplan.data.model.Nutrition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    onNavigateBack: () -> Unit,
    onRecipeCreated: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    
    // Form state
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
    
    // Handle success - navigate back when recipe is created
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("created") == true) {
            onRecipeCreated()
            viewModel.clearSuccessMessage()
        }
    }
    
    // Validation
    fun validateForm(): Boolean {
        titleError = if (title.isBlank()) "Title is required" else ""
        return titleError.isEmpty()
    }
    
    // Create recipe
    fun createRecipe() {
        if (validateForm()) {
            val nutrition = if (calories.isNotBlank() || protein.isNotBlank() || carbs.isNotBlank() || fat.isNotBlank()) {
                Nutrition(
                    calories = calories.toDoubleOrNull(),
                    protein = protein.toDoubleOrNull(),
                    carbs = carbs.toDoubleOrNull(),
                    fat = fat.toDoubleOrNull()
                )
            } else null
            
            val request = CreateRecipeRequest(
                title = title,
                description = description.ifBlank { null },
                ingredients = ingredients,
                instructions = instructions,
                prepTime = prepTime.toIntOrNull() ?: 0,
                cookTime = cookTime.toIntOrNull() ?: 0,
                difficulty = difficulty,
                servings = servings.toIntOrNull() ?: 1,
                tags = tags,
                nutrition = nutrition,
                sourcePlatform = null, // Always null for user-created recipes
                sourceUrl = null,
                videoThumbnail = null,
                tiktokAuthor = null,
                isPublic = isPublic
            )
            
            viewModel.createRecipe(request)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        TopAppBar(
            title = { Text("Create Recipe") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Button(
                    onClick = { createRecipe() },
                    enabled = !uiState.isLoading && title.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create")
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
            

        }
    }
} 