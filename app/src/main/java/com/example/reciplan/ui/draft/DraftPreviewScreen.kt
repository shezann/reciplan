package com.example.reciplan.ui.draft

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftPreviewScreen(
    recipeId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    // Create ViewModel
    val viewModel: DraftPreviewViewModel = remember {
        viewModelFactory.create(DraftPreviewViewModel::class.java)
    }
    
    // Collect UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Load data when recipeId changes
    LaunchedEffect(recipeId) {
        viewModel.loadRecipeData(recipeId)
    }
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is DraftPreviewNavigationEvent.NavigateToRecipeDetail -> {
                    onNavigateToRecipeDetail(event.recipeId)
                }
                is DraftPreviewNavigationEvent.ShowSaveSuccess -> {
                    // Success message will be handled by snackbar
                }
                is DraftPreviewNavigationEvent.ShowSaveError -> {
                    // Error message will be handled by existing error state
                }
            }
        }
    }
    
    // Tab state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Ingredients", "Instructions")
    
    // Snackbar state for messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar when save fails
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { error ->
            snackbarHostState.showSnackbar(
                message = "Failed to save: $error",
                duration = SnackbarDuration.Long
            )
        }
    }
    
    // Show success snackbar when save succeeds
    LaunchedEffect(uiState.isSaving, uiState.hasUnsavedChanges, uiState.lastSaveType) {
        if (!uiState.isSaving && !uiState.hasUnsavedChanges && uiState.lastSaveType != null && uiState.title.isNotEmpty()) {
            // This indicates a successful save/approve operation (not initial load)
            val message = when (uiState.lastSaveType) {
                "approve" -> "Recipe approved successfully!"
                "save" -> "Changes saved successfully!"
                else -> "Recipe saved successfully!"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            // Clear the save type flag after showing the message
            viewModel.clearLastSaveType()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recipe Draft") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Save/Approve button
                    IconButton(
                        onClick = {
                            viewModel.saveRecipe(recipeId)
                        },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = if (uiState.hasUnsavedChanges) "Save Changes" else "Approve Recipe"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // Error handling
        val errorMessage = uiState.error
        if (errorMessage != null) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error loading recipe",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.forceLoadRecipeData(recipeId) }
                ) {
                    Text("Retry")
                }
            }
            return@Scaffold
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Tab Content
            when (selectedTabIndex) {
                0 -> OverviewTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> IngredientsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> InstructionsTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: DraftPreviewUiState,
    viewModel: DraftPreviewViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Editable title field
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Recipe Title") },
                        placeholder = { Text("Enter recipe title") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                    
                    // Editable description field
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("Description") },
                        placeholder = { Text("Enter recipe description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        )
                    )
                    
                    HorizontalDivider()
                    
                    // Recipe metadata - editable fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Prep Time
                        OutlinedTextField(
                            value = if (uiState.prepTime > 0) uiState.prepTime.toString() else "",
                            onValueChange = { newValue ->
                                val prepTime = newValue.toIntOrNull() ?: 0
                                viewModel.updateMetadata(prepTime = prepTime)
                            },
                            label = { Text("Prep Time") },
                            placeholder = { Text("30") },
                            suffix = { Text("min") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        // Cook Time
                        OutlinedTextField(
                            value = if (uiState.cookTime > 0) uiState.cookTime.toString() else "",
                            onValueChange = { newValue ->
                                val cookTime = newValue.toIntOrNull() ?: 0
                                viewModel.updateMetadata(cookTime = cookTime)
                            },
                            label = { Text("Cook Time") },
                            placeholder = { Text("45") },
                            suffix = { Text("min") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        // Servings
                        OutlinedTextField(
                            value = if (uiState.servings > 0) uiState.servings.toString() else "",
                            onValueChange = { newValue ->
                                val servings = newValue.toIntOrNull() ?: 1
                                viewModel.updateMetadata(servings = servings)
                            },
                            label = { Text("Servings") },
                            placeholder = { Text("4") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Source Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = viewModel.getSourceInfo(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientsTab(
    uiState: DraftPreviewUiState,
    viewModel: DraftPreviewViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        viewModel.addNewIngredient()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Ingredient"
                    )
                }
            }
        }
        
        // Real ingredients from ViewModel with inline editing and drag-and-drop
        if (uiState.ingredients.isNotEmpty()) {
            itemsIndexed(uiState.ingredients) { index, ingredient ->
                DraggableIngredientRow(
                    ingredient = ingredient,
                    index = index,
                    totalCount = uiState.ingredients.size,
                    onStartEditing = { viewModel.startEditingIngredient(ingredient.id) },
                    onNameChange = { newName -> viewModel.updateIngredientName(ingredient.id, newName) },
                    onQuantityChange = { newQuantity -> viewModel.updateIngredientQuantity(ingredient.id, newQuantity) },
                    onSaveChanges = { viewModel.saveIngredientChanges(ingredient.id) },
                    onCancelEditing = { originalName, originalQuantity ->
                        viewModel.cancelIngredientEditing(ingredient.id, originalName, originalQuantity)
                    },
                    onDelete = { viewModel.deleteIngredient(ingredient.id) },
                    onReorder = { fromIndex, toIndex -> viewModel.reorderIngredients(fromIndex, toIndex) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ingredients found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap ingredients to edit • Long press & drag handle to reorder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionsTab(
    uiState: DraftPreviewUiState,
    viewModel: DraftPreviewViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        viewModel.addNewInstruction()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Instruction"
                    )
                }
            }
        }
        
        // Real instructions from ViewModel with drag-and-drop
        if (uiState.instructions.isNotEmpty()) {
            itemsIndexed(uiState.instructions) { index, instruction ->
                DraggableInstructionRow(
                    instruction = instruction,
                    index = index,
                    totalCount = uiState.instructions.size,
                    onReorder = { fromIndex, toIndex -> viewModel.reorderInstructions(fromIndex, toIndex) },
                    onStartEditing = { instructionId ->
                        viewModel.startEditingInstruction(instructionId)
                    },
                    onTextChange = { instructionId, newText ->
                        viewModel.updateInstructionText(instructionId, newText)
                    },
                    onSaveChanges = { instructionId ->
                        viewModel.stopEditingInstructions()
                    },
                    onCancelEditing = { instructionId, originalText ->
                        viewModel.cancelInstructionEditing(instructionId, originalText)
                    },
                    onDelete = { instructionId ->
                        viewModel.deleteInstruction(instructionId)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No instructions found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                                                    text = "Tap instructions to edit • Long press & drag handle to reorder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableIngredientRow(
    ingredient: EditableIngredient,
    onStartEditing: () -> Unit,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSaveChanges: () -> Unit,
    onCancelEditing: (String, String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = false,
    onDragStart: (() -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null
) {
    // Store original values for cancel functionality
    val originalName = remember(ingredient.id) { ingredient.name }
    val originalQuantity = remember(ingredient.id) { ingredient.quantity }
    
    // Focus management
    val nameFocusRequester = remember { FocusRequester() }
    val quantityFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Request focus when entering edit mode
    LaunchedEffect(ingredient.isEditing) {
        if (ingredient.isEditing) {
            nameFocusRequester.requestFocus()
        }
    }
    
    Card(
        modifier = modifier
    ) {
        if (ingredient.isEditing) {
            // Edit mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name TextField
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = onNameChange,
                    label = { Text("Ingredient Name") },
                    placeholder = { Text("e.g., Chicken breast") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            quantityFocusRequester.requestFocus()
                        }
                    ),
                    isError = ingredient.name.trim().isEmpty() && ingredient.quantity.trim().isNotEmpty(),
                    supportingText = if (ingredient.name.trim().isEmpty() && ingredient.quantity.trim().isNotEmpty()) {
                        { Text("Ingredient name is required") }
                    } else null
                )
                
                // Quantity TextField
                OutlinedTextField(
                    value = ingredient.quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Quantity") },
                    placeholder = { Text("e.g., 2 cups, 1 lb") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(quantityFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onSaveChanges()
                        }
                    ),
                    isError = ingredient.quantity.trim().isEmpty() && ingredient.name.trim().isNotEmpty(),
                    supportingText = if (ingredient.quantity.trim().isEmpty() && ingredient.name.trim().isNotEmpty()) {
                        { Text("Quantity is required") }
                    } else null
                )
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            onCancelEditing(originalName, originalQuantity)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                    
                    // Save button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onSaveChanges()
                        },
                        enabled = ingredient.name.trim().isNotEmpty() || ingredient.quantity.trim().isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        } else {
            // Display mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartEditing() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag handle (if enabled)
                if (showDragHandle) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        onDragStart?.invoke()
                                    },
                                    onDragEnd = { 
                                        onDragEnd?.invoke()
                                    },
                                    onDrag = { _, change ->
                                        onDrag?.invoke(change)
                                    }
                                )
                            }
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ingredient.name.ifEmpty { "Unnamed ingredient" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ingredient.name.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    if (ingredient.quantity.isNotEmpty()) {
                        Text(
                            text = ingredient.quantity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Delete button
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ingredient",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableIngredientRow(
    ingredient: EditableIngredient,
    index: Int,
    totalCount: Int,
    onStartEditing: () -> Unit,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSaveChanges: () -> Unit,
    onCancelEditing: (String, String) -> Unit,
    onDelete: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = dragOffset.y
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .then(
                if (isDragging) {
                    Modifier.shadow(8.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Long press and drag to reorder",
                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { 
                                isDragging = true
                            },
                            onDragEnd = { 
                                isDragging = false
                                dragOffset = Offset.Zero
                            },
                            onDrag = { _, change ->
                                dragOffset += change
                                
                                // Simple reorder logic with bounds checking
                                val threshold = 60f // Pixels to drag before reordering (reduced for better responsiveness)
                                
                                when {
                                    dragOffset.y < -threshold && index > 0 -> {
                                        onReorder(index, index - 1)
                                        dragOffset = Offset.Zero
                                    }
                                    dragOffset.y > threshold && index < totalCount - 1 -> {
                                        onReorder(index, index + 1)
                                        dragOffset = Offset.Zero
                                    }
                                }
                            }
                        )
                    }
            )
            
            // Ingredient content
            EditableIngredientRow(
                ingredient = ingredient,
                onStartEditing = onStartEditing,
                onNameChange = onNameChange,
                onQuantityChange = onQuantityChange,
                onSaveChanges = onSaveChanges,
                onCancelEditing = onCancelEditing,
                onDelete = onDelete,
                modifier = Modifier.weight(1f),
                showDragHandle = false // We handle the drag handle ourselves
            )
        }
    }
}

@Composable
private fun DraggableInstructionRow(
    instruction: EditableInstruction,
    index: Int,
    totalCount: Int,
    onReorder: (Int, Int) -> Unit,
    onStartEditing: (String) -> Unit = {},
    onTextChange: (String, String) -> Unit = { _, _ -> },
    onSaveChanges: (String) -> Unit = {},
    onCancelEditing: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = dragOffset.y
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .then(
                if (isDragging) {
                    Modifier.shadow(8.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Drag handle
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Long press and drag to reorder",
                    tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { 
                                    isDragging = true
                                },
                                onDragEnd = { 
                                    isDragging = false
                                    dragOffset = Offset.Zero
                                },
                                                onDrag = { _, change ->
                    dragOffset += change
                    
                    // Simple reorder logic with bounds checking
                    val threshold = 60f // Pixels to drag before reordering (reduced for better responsiveness)
                    
                    when {
                        dragOffset.y < -threshold && index > 0 -> {
                            onReorder(index, index - 1)
                            dragOffset = Offset.Zero
                        }
                        dragOffset.y > threshold && index < totalCount - 1 -> {
                            onReorder(index, index + 1)
                            dragOffset = Offset.Zero
                        }
                    }
                }
                            )
                        }
                )
                
                Text(
                    text = "${instruction.stepNumber}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                if (instruction.isEditing) {
                    // Edit mode
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val originalText = remember { instruction.text }
                        val keyboardController = LocalSoftwareKeyboardController.current
                        
                        OutlinedTextField(
                            value = instruction.text,
                            onValueChange = { newText ->
                                onTextChange(instruction.id, newText)
                            },
                            label = { Text("Instruction") },
                            placeholder = { Text("Enter cooking instruction...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    onSaveChanges(instruction.id)
                                }
                            )
                        )
                        
                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Delete button (for existing instructions)
                            if (instruction.id.startsWith("inst_new_").not()) {
                                OutlinedButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        onDelete(instruction.id)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                            
                            // Cancel button
                            OutlinedButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onCancelEditing(instruction.id, originalText)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                            
                            // Save button  
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    onSaveChanges(instruction.id)
                                },
                                enabled = instruction.text.trim().isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }
                } else {
                    // View mode
                    Text(
                        text = instruction.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onStartEditing(instruction.id)
                            }
                    )
                }
            }
        }
    }
} 