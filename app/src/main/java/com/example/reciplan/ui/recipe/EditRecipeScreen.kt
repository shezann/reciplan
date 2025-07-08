package com.example.reciplan.ui.recipe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Nutrition fields
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var showNutrition by remember { mutableStateOf(false) }
    
    // New ingredient/instruction fields
    var newIngredientName by remember { mutableStateOf("") }
    var newIngredientQuantity by remember { mutableStateOf("") }
    var newInstruction by remember { mutableStateOf("") }
    
    // Form validation
    var titleError by remember { mutableStateOf("") }
    var ingredientsError by remember { mutableStateOf("") }
    var instructionsError by remember { mutableStateOf("") }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Image picker
    var showImageOptions by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Initialize form with existing recipe data
    LaunchedEffect(selectedRecipe) {
        selectedRecipe?.let { recipe ->
            title = recipe.title
            description = recipe.description ?: ""
            ingredients = recipe.ingredients
            instructions = recipe.instructions
            prepTime = recipe.prep_time.toString()
            cookTime = recipe.cook_time.toString()
            servings = recipe.servings.toString()
            difficulty = recipe.difficulty
            tags = recipe.tags
            sourcePlatform = recipe.source_platform ?: ""
            sourceUrl = recipe.source_url ?: ""
            videoThumbnail = recipe.video_thumbnail ?: ""
            tiktokAuthor = recipe.tiktok_author ?: ""
            isPublic = recipe.is_public
            existingImageUrl = recipe.video_thumbnail
            
            // Initialize nutrition if available
            recipe.nutrition?.let { nutrition ->
                calories = nutrition.calories?.toString() ?: ""
                protein = nutrition.protein?.toString() ?: ""
                carbs = nutrition.carbs?.toString() ?: ""
                fat = nutrition.fat?.toString() ?: ""
                showNutrition = (calories.isNotEmpty() || protein.isNotEmpty() || carbs.isNotEmpty() || fat.isNotEmpty())
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
    
    // Validation functions
    fun validateTitle(): Boolean {
        titleError = when {
            title.isBlank() -> "Recipe title is required"
            title.length < 3 -> "Title must be at least 3 characters"
            title.length > 100 -> "Title must be less than 100 characters"
            else -> ""
        }
        return titleError.isEmpty()
    }
    
    fun validateIngredients(): Boolean {
        ingredientsError = when {
            ingredients.isEmpty() -> "At least one ingredient is required"
            else -> ""
        }
        return ingredientsError.isEmpty()
    }
    
    fun validateInstructions(): Boolean {
        instructionsError = when {
            instructions.isEmpty() -> "At least one instruction is required"
            else -> ""
        }
        return instructionsError.isEmpty()
    }
    
    fun validateForm(): Boolean {
        return validateTitle() && validateIngredients() && validateInstructions()
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
            
            val imageUrl = selectedImageUri?.toString() ?: existingImageUrl
            
            val request = UpdateRecipeRequest(
                title = title.trim(),
                description = description.trim().ifBlank { null },
                ingredients = ingredients,
                instructions = instructions,
                prep_time = prepTime.toIntOrNull(),
                cook_time = cookTime.toIntOrNull(),
                difficulty = difficulty,
                servings = servings.toIntOrNull(),
                tags = tags,
                nutrition = nutrition,
                source_platform = sourcePlatform.ifBlank { null },
                source_url = sourceUrl.ifBlank { null },
                video_thumbnail = imageUrl,
                tiktok_author = tiktokAuthor.ifBlank { null },
                is_public = isPublic
            )
            
            viewModel.updateRecipe(recipeId, request)
        }
    }
    
    // Delete recipe
    fun deleteRecipe() {
        viewModel.deleteRecipe(recipeId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Recipe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Delete Button
                    IconButton(
                        onClick = { showDeleteDialog = true }
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
                        enabled = !uiState.isLoading && title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
        
        // Error Message
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Loading State
        if (uiState.isLoading && selectedRecipe == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        "Loading recipe...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (selectedRecipe != null) {
            // Form Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Recipe Image Section
                item {
                    ModernCard(
                        title = "Recipe Photo",
                        subtitle = "Images stored locally"
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showImageOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageToShow = selectedImageUri?.toString() ?: existingImageUrl
                            
                            if (imageToShow != null) {
                                AsyncImage(
                                    model = imageToShow,
                                    contentDescription = "Recipe Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Change/Remove buttons overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { showImageOptions = true },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Change")
                                        }
                                        
                                        FilledTonalButton(
                                            onClick = { 
                                                selectedImageUri = null
                                                existingImageUrl = null
                                            },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove")
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Add Photo",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Tap to select or take a photo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Basic Information (same as CreateRecipeScreen)
                item {
                    ModernCard(title = "Basic Information") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Title
                            OutlinedTextField(
                                value = title,
                                onValueChange = { 
                                    title = it
                                    validateTitle()
                                },
                                label = { Text("Recipe Title *") },
                                isError = titleError.isNotEmpty(),
                                supportingText = if (titleError.isNotEmpty()) {
                                    { Text(titleError) }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Description
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                placeholder = { Text("Tell us about this recipe...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Time and Servings Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = prepTime,
                                    onValueChange = { prepTime = it.filter { char -> char.isDigit() } },
                                    label = { Text("Prep Time") },
                                    placeholder = { Text("15") },
                                    suffix = { Text("min") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                
                                OutlinedTextField(
                                    value = cookTime,
                                    onValueChange = { cookTime = it.filter { char -> char.isDigit() } },
                                    label = { Text("Cook Time") },
                                    placeholder = { Text("30") },
                                    suffix = { Text("min") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                
                                OutlinedTextField(
                                    value = servings,
                                    onValueChange = { servings = it.filter { char -> char.isDigit() } },
                                    label = { Text("Servings") },
                                    placeholder = { Text("4") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            
                            // Difficulty with stars
                            Column {
                                Text(
                                    text = "Difficulty Level",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    (1..5).forEach { level ->
                                        IconButton(
                                            onClick = { difficulty = level },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = "Difficulty $level",
                                                tint = if (level <= difficulty) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = when (difficulty) {
                                            1 -> "Very Easy"
                                            2 -> "Easy"
                                            3 -> "Medium"
                                            4 -> "Hard"
                                            5 -> "Very Hard"
                                            else -> "Easy"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Public Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Make recipe public",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Others can discover and save your recipe",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPublic,
                                    onCheckedChange = { isPublic = it }
                                )
                            }
                        }
                    }
                }
                
                // Ingredients (same structure as CreateRecipeScreen)
                item {
                    ModernCard(title = "Ingredients") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add Ingredient
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                OutlinedTextField(
                                    value = newIngredientQuantity,
                                    onValueChange = { newIngredientQuantity = it },
                                    label = { Text("Amount") },
                                    placeholder = { Text("1 cup") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = newIngredientName,
                                    onValueChange = { newIngredientName = it },
                                    label = { Text("Ingredient") },
                                    placeholder = { Text("flour") },
                                    modifier = Modifier.weight(2f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (newIngredientName.isNotBlank() && newIngredientQuantity.isNotBlank()) {
                                                ingredients = ingredients + Ingredient(
                                                    name = newIngredientName.trim(),
                                                    quantity = newIngredientQuantity.trim()
                                                )
                                                newIngredientName = ""
                                                newIngredientQuantity = ""
                                                validateIngredients()
                                            }
                                        }
                                    )
                                )
                                
                                FilledTonalIconButton(
                                    onClick = {
                                        if (newIngredientName.isNotBlank() && newIngredientQuantity.isNotBlank()) {
                                            ingredients = ingredients + Ingredient(
                                                name = newIngredientName.trim(),
                                                quantity = newIngredientQuantity.trim()
                                            )
                                            newIngredientName = ""
                                            newIngredientQuantity = ""
                                            validateIngredients()
                                        }
                                    },
                                    enabled = newIngredientName.isNotBlank() && newIngredientQuantity.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
                                }
                            }
                            
                            if (ingredientsError.isNotEmpty()) {
                                Text(
                                    text = ingredientsError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Ingredients List
                            ingredients.forEachIndexed { index, ingredient ->
                                IngredientItem(
                                    ingredient = ingredient,
                                    onRemove = {
                                        ingredients = ingredients.filterIndexed { i, _ -> i != index }
                                        validateIngredients()
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Instructions (same structure as CreateRecipeScreen)
                item {
                    ModernCard(title = "Instructions") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add Instruction
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                OutlinedTextField(
                                    value = newInstruction,
                                    onValueChange = { newInstruction = it },
                                    label = { Text("Step ${instructions.size + 1}") },
                                    placeholder = { Text("Describe this step...") },
                                    modifier = Modifier.weight(1f),
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (newInstruction.isNotBlank()) {
                                                instructions = instructions + newInstruction.trim()
                                                newInstruction = ""
                                                validateInstructions()
                                            }
                                        }
                                    )
                                )
                                
                                FilledTonalIconButton(
                                    onClick = {
                                        if (newInstruction.isNotBlank()) {
                                            instructions = instructions + newInstruction.trim()
                                            newInstruction = ""
                                            validateInstructions()
                                        }
                                    },
                                    enabled = newInstruction.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Instruction")
                                }
                            }
                            
                            if (instructionsError.isNotEmpty()) {
                                Text(
                                    text = instructionsError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Instructions List
                            instructions.forEachIndexed { index, instruction ->
                                InstructionItem(
                                    instruction = instruction,
                                    stepNumber = index + 1,
                                    onRemove = {
                                        instructions = instructions.filterIndexed { i, _ -> i != index }
                                        validateInstructions()
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Tags (same structure as CreateRecipeScreen)
                item {
                    ModernCard(title = "Tags") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentTag,
                                    onValueChange = { currentTag = it },
                                    label = { Text("Add Tag") },
                                    placeholder = { Text("e.g., vegetarian, quick") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val trimmedTag = currentTag.trim().lowercase()
                                            if (trimmedTag.isNotBlank() && !tags.contains(trimmedTag)) {
                                                tags = tags + trimmedTag
                                                currentTag = ""
                                            }
                                        }
                                    )
                                )
                                
                                FilledTonalIconButton(
                                    onClick = {
                                        val trimmedTag = currentTag.trim().lowercase()
                                        if (trimmedTag.isNotBlank() && !tags.contains(trimmedTag)) {
                                            tags = tags + trimmedTag
                                            currentTag = ""
                                        }
                                    },
                                    enabled = currentTag.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Tag")
                                }
                            }
                            
                            // Common Tags
                            Text(
                                "Suggested:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val suggestedTags = listOf(
                                    "breakfast", "lunch", "dinner", "dessert", "vegetarian", 
                                    "vegan", "gluten-free", "quick", "easy", "healthy", "comfort"
                                )
                                items(suggestedTags.filter { !tags.contains(it) }) { tag ->
                                    SuggestionChip(
                                        onClick = { 
                                            if (!tags.contains(tag)) {
                                                tags = tags + tag
                                            }
                                        },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                            
                            // Selected Tags
                            if (tags.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(tags) { tag ->
                                        AssistChip(
                                            onClick = { tags = tags.filter { it != tag } },
                                            label = { Text(tag) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove $tag",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Nutrition (Optional, Expandable)
                item {
                    ModernCard(
                        title = "Nutrition Information",
                        subtitle = "Optional"
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNutrition = !showNutrition },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (showNutrition) "Hide nutrition details" else "Add nutrition details",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    if (showNutrition) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = showNutrition,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = calories,
                                            onValueChange = { calories = it.filter { char -> char.isDigit() } },
                                            label = { Text("Calories") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        
                                        OutlinedTextField(
                                            value = protein,
                                            onValueChange = { protein = it.filter { char -> char.isDigit() || char == '.' } },
                                            label = { Text("Protein") },
                                            suffix = { Text("g") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = carbs,
                                            onValueChange = { carbs = it.filter { char -> char.isDigit() || char == '.' } },
                                            label = { Text("Carbs") },
                                            suffix = { Text("g") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        
                                        OutlinedTextField(
                                            value = fat,
                                            onValueChange = { fat = it.filter { char -> char.isDigit() || char == '.' } },
                                            label = { Text("Fat") },
                                            suffix = { Text("g") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Image Options Bottom Sheet
    if (showImageOptions) {
        ModalBottomSheet(
            onDismissRequest = { showImageOptions = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Change Recipe Photo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Camera",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                "Coming Soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                                showImageOptions = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Gallery",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Recipe")
            },
            text = {
                Text("Are you sure you want to delete this recipe? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        deleteRecipe()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
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