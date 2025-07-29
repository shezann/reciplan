package com.example.reciplan.ui.draft

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reciplan.ui.components.*
import com.example.reciplan.ui.theme.*

/**
 * Task 14: Draft Editor Screen Redesign
 * 
 * Enhanced Draft Editor with:
 * - Ingredient Editor Integration (Subtask 141)
 * - Enhanced Text Fields (Subtask 142)
 * - Improved Toolbar (Subtask 143)
 * - Save/Discard Transitions (Subtask 144)
 */

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
    val hapticFeedback = LocalHapticFeedback.current
    
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
    
    // Subtask 144: Save/Discard confirmation state
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var pendingSaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
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
                "approve" -> "Recipe approved successfully! 🎉"
                "save" -> "Changes saved successfully! ✓"
                else -> "Recipe saved successfully! ✓"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            // Clear the save type flag after showing the message
            viewModel.clearLastSaveType()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Subtask 143: Improved Toolbar
            EnhancedDraftToolbar(
                title = uiState.title.ifEmpty { "Recipe Draft" },
                hasUnsavedChanges = uiState.hasUnsavedChanges,
                isSaving = uiState.isSaving,
                onNavigateBack = {
                    if (uiState.hasUnsavedChanges) {
                        showDiscardConfirmation = true
                    } else {
                        onNavigateBack()
                    }
                },
                onSave = {
                    if (uiState.hasUnsavedChanges) {
                        showSaveConfirmation = true
                        pendingSaveAction = { viewModel.saveRecipe(recipeId) }
                    } else {
                            viewModel.saveRecipe(recipeId)
                    }
                },
                onApprove = {
                    showSaveConfirmation = true
                    pendingSaveAction = { viewModel.saveRecipe(recipeId) }
                }
            )
            
            // Error handling
            val errorMessage = uiState.error
            if (errorMessage != null) {
                EnhancedErrorState(
                    error = errorMessage,
                    onRetry = { viewModel.forceLoadRecipeData(recipeId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
                return@Column
            }
            
            // Enhanced Tab Row
            EnhancedTabRow(
                selectedTabIndex = selectedTabIndex,
                tabTitles = tabTitles,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.fillMaxWidth()
            )
            
                                      // Tab Content
             when (selectedTabIndex) {
                 0 -> EnhancedOverviewTab(
                     uiState = uiState,
                     viewModel = viewModel,
                     modifier = Modifier.fillMaxSize()
                 )
                 1 -> EnhancedIngredientsTab(
                     uiState = uiState,
                     viewModel = viewModel,
                     modifier = Modifier.fillMaxSize()
                 )
                 2 -> EnhancedInstructionsTab(
                     uiState = uiState,
                     viewModel = viewModel,
                     modifier = Modifier.fillMaxSize()
                 )
             }
        }
        
        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Subtask 144: Save/Discard Transitions - Confirmation Dialogs
    if (showSaveConfirmation) {
        EnhancedSaveConfirmationDialog(
            hasUnsavedChanges = uiState.hasUnsavedChanges,
            onConfirm = {
                showSaveConfirmation = false
                pendingSaveAction?.invoke()
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            },
            onDismiss = {
                showSaveConfirmation = false
                pendingSaveAction = null
            }
        )
    }
    
    if (showDiscardConfirmation) {
        EnhancedDiscardConfirmationDialog(
            onConfirm = {
                showDiscardConfirmation = false
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onNavigateBack()
            },
            onDismiss = {
                showDiscardConfirmation = false
            }
        )
    }
}

/**
 * Subtask 143: Improved Toolbar
 * Enhanced toolbar with better visual hierarchy and new button system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedDraftToolbar(
    title: String,
    hasUnsavedChanges: Boolean,
    isSaving: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onApprove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Create a subtle gradient overlay for visual separation instead of harsh shadow
    val headerAlpha by animateFloatAsState(
        targetValue = if (hasUnsavedChanges) 0.05f else 0f,
        animationSpec = MotionSpecs.emphasizedTween(),
        label = "header_overlay_alpha"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (headerAlpha > 0f) {
                    drawRect(
                        color = Color.Black.copy(alpha = headerAlpha),
                        size = size.copy(height = size.height + 4.dp.toPx())
                    )
                }
            },
        shadowElevation = 0.dp, // Remove harsh shadow
        color = MaterialTheme.colorScheme.surface
    ) {
        TopAppBar(
            title = { 
                Column {
                Text(
                        text = "Recipe Draft",
                    style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                    
                    if (title.isNotEmpty() && title != "Recipe Draft") {
                Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = hasUnsavedChanges,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Text(
                            text = "• Unsaved changes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            },
            navigationIcon = {
                Surface(
                    shape = AppShapes.SmallShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save button (when there are changes)
                    AnimatedVisibility(
                        visible = hasUnsavedChanges,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                                            EnhancedSecondaryButton(
                        onClick = onSave,
                        enabled = !isSaving,
                        size = ButtonSize.SMALL,
                        leadingIcon = if (isSaving) null else Icons.Default.Check
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Text("Save")
                        }
                    }
                    }
                    
                    // Approve/Publish button
                    EnhancedPrimaryButton(
                        onClick = if (hasUnsavedChanges) onApprove else onSave,
                        enabled = !isSaving,
                        size = ButtonSize.SMALL,
                        leadingIcon = if (isSaving) null else Icons.Default.CheckCircle
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (hasUnsavedChanges) "Approve" else "Publish")
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

/**
 * Enhanced Tab Row with improved styling
 */
@Composable
private fun EnhancedTabRow(
    selectedTabIndex: Int,
    tabTitles: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp // Remove gray border
    ) {
                 TabRow(
             selectedTabIndex = selectedTabIndex,
             containerColor = Color.Transparent,
             contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Subtask 142: Enhanced Text Fields
 * Enhanced Overview Tab with new text input styling
 */
@Composable
private fun EnhancedOverviewTab(
    uiState: DraftPreviewUiState,
    viewModel: DraftPreviewViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.LargeShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shadowElevation = 0.dp // Remove gray border for modern look
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Recipe Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Enhanced title field
                    EnhancedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = "Recipe Title",
                        placeholder = "Enter your amazing recipe title",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                    
                    // Enhanced description field
                    EnhancedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = "Description",
                        placeholder = "Describe what makes this recipe special...",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }
        }
        
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.LargeShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shadowElevation = 0.dp // Remove gray border for modern look
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Recipe Timing",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Enhanced prep time field
                        EnhancedTextField(
                            value = if (uiState.prepTime > 0) uiState.prepTime.toString() else "",
                            onValueChange = { newValue ->
                                val prepTime = newValue.toIntOrNull() ?: 0
                                viewModel.updateMetadata(prepTime = prepTime)
                            },
                            label = "Prep Time (min)",
                            placeholder = "30",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        // Enhanced cook time field
                        EnhancedTextField(
                            value = if (uiState.cookTime > 0) uiState.cookTime.toString() else "",
                            onValueChange = { newValue ->
                                val cookTime = newValue.toIntOrNull() ?: 0
                                viewModel.updateMetadata(cookTime = cookTime)
                            },
                            label = "Cook Time (min)",
                            placeholder = "45",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        // Enhanced servings field
                        EnhancedTextField(
                            value = if (uiState.servings > 0) uiState.servings.toString() else "",
                            onValueChange = { newValue ->
                                val servings = newValue.toIntOrNull() ?: 0
                                viewModel.updateMetadata(servings = servings)
                            },
                            label = "Servings",
                            placeholder = "4",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }
        
        // Enhanced metadata display
        if (uiState.prepTime > 0 || uiState.cookTime > 0 || uiState.servings > 0) {
        item {
                EnhancedMetadataPreview(
                    prepTime = uiState.prepTime,
                    cookTime = uiState.cookTime,
                    servings = uiState.servings,
                modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Subtask 141: Ingredient Editor Integration
 * Enhanced Ingredients Tab using IngredientRow components
 */
@Composable
private fun EnhancedIngredientsTab(
    uiState: DraftPreviewUiState,
    viewModel: DraftPreviewViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSectionHeader(
                title = "Ingredients",
                subtitle = "${uiState.ingredients.size} ingredients",
                onAddClick = { viewModel.addNewIngredient() }
            )
        }
        
        // Ingredient list using IngredientRow components
        if (uiState.ingredients.isNotEmpty()) {
            itemsIndexed(uiState.ingredients) { index, ingredient ->
                EnhancedEditableIngredientRow(
                    ingredient = ingredient,
                    index = index,
                    totalCount = uiState.ingredients.size,
                    onStartEditing = { viewModel.startEditingIngredient(ingredient.id) },
                    onNameChange = { newName -> 
                        viewModel.updateIngredientName(ingredient.id, newName) 
                    },
                    onQuantityChange = { newQuantity -> 
                        viewModel.updateIngredientQuantity(ingredient.id, newQuantity) 
                    },
                    onSaveChanges = { viewModel.saveIngredientChanges(ingredient.id) },
                    onCancelEditing = { originalName, originalQuantity ->
                        viewModel.cancelIngredientEditing(ingredient.id, originalName, originalQuantity)
                    },
                    onDelete = { viewModel.deleteIngredient(ingredient.id) },
                    onReorder = { fromIndex, toIndex -> 
                        viewModel.reorderIngredients(fromIndex, toIndex) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (uiState.isLoading) {
            item {
                EnhancedLoadingState(
                    message = "Loading ingredients...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                EmptyState(
                    type = EmptyStateType.NO_INGREDIENTS,
                    onPrimaryAction = { viewModel.addNewIngredient() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Helper text
        if (uiState.ingredients.isNotEmpty()) {
        item {
                EnhancedHelperCard(
                    text = "✓ Tap ingredients to edit\n✓ Long press & drag handle to reorder\n✓ Use clear, specific quantities (e.g., '2 cups', '1 lb')",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Enhanced Instructions Tab with better editing experience
 */
@Composable
private fun EnhancedInstructionsTab(
    uiState: DraftPreviewUiState,
    viewModel: DraftPreviewViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EnhancedSectionHeader(
                title = "Instructions",
                subtitle = "${uiState.instructions.size} steps",
                onAddClick = { viewModel.addNewInstruction() }
            )
        }
        
        // Instructions list
        if (uiState.instructions.isNotEmpty()) {
            itemsIndexed(uiState.instructions) { index, instruction ->
                EnhancedEditableInstructionRow(
                    instruction = instruction,
                    index = index,
                    totalCount = uiState.instructions.size,
                    onStartEditing = { viewModel.startEditingInstruction(instruction.id) },
                    onTextChange = { newText -> 
                        viewModel.updateInstructionText(instruction.id, newText) 
                    },
                    onSaveChanges = { viewModel.stopEditingInstructions() },
                    onCancelEditing = { originalText -> 
                        viewModel.cancelInstructionEditing(instruction.id, originalText) 
                    },
                    onDelete = { viewModel.deleteInstruction(instruction.id) },
                    onReorder = { fromIndex, toIndex -> 
                        viewModel.reorderInstructions(fromIndex, toIndex) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (uiState.isLoading) {
            item {
                EnhancedLoadingState(
                    message = "Loading instructions...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                EmptyState(
                    type = EmptyStateType.NO_RECIPES,
                    onPrimaryAction = { viewModel.addNewInstruction() },
                    modifier = Modifier.fillMaxWidth(),
                    customContent = EmptyStateContent(
                        title = "No instructions yet",
                        subtitle = "Add your first cooking instruction to get started",
                        illustration = EmptyStateIllustration.RECIPE_BOOK,
                        primaryAction = ActionButton(
                            text = "Add Instruction",
                            onClick = { viewModel.addNewInstruction() },
                            icon = Icons.Default.Add
                        )
                    )
                )
            }
        }
        
        // Helper text
        if (uiState.instructions.isNotEmpty()) {
        item {
                EnhancedHelperCard(
                    text = "✓ Write clear, step-by-step instructions\n✓ Include timing and temperatures\n✓ Long press & drag to reorder steps",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Enhanced section header with add button
 */
@Composable
private fun EnhancedSectionHeader(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.MediumShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
                ) {
            Column {
                    Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add $title",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Subtask 141: Enhanced Editable Ingredient Row
 * Using IngredientRow component as base with editing capabilities
 */
@Composable
private fun EnhancedEditableIngredientRow(
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
    
    Surface(
        modifier = modifier,
        shape = AppShapes.LargeShape,
        color = if (ingredient.isEditing) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 0.dp // Remove gray borders for modern look
    ) {
        if (ingredient.isEditing) {
            // Edit mode using enhanced text fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Ingredient",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Enhanced ingredient name field
                EnhancedTextField(
                    value = ingredient.name,
                    onValueChange = onNameChange,
                    label = "Ingredient Name",
                    placeholder = "e.g., Chicken breast, Olive oil",
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
                
                // Enhanced quantity field
                EnhancedTextField(
                    value = ingredient.quantity,
                    onValueChange = onQuantityChange,
                    label = "Quantity & Unit",
                    placeholder = "e.g., 2 cups, 1 lb, 3 tbsp",
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
                
                // Action buttons using enhanced button system
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    // Cancel button
                    EnhancedSecondaryButton(
                        onClick = {
                            keyboardController?.hide()
                            onCancelEditing(originalName, originalQuantity)
                        },
                        size = ButtonSize.SMALL,
                        leadingIcon = Icons.Default.Close
                    ) {
                        Text("Cancel")
                    }
                    
                    // Save button
                    EnhancedPrimaryButton(
                        onClick = {
                            keyboardController?.hide()
                            onSaveChanges()
                        },
                        enabled = ingredient.name.trim().isNotEmpty() || ingredient.quantity.trim().isNotEmpty(),
                        size = ButtonSize.SMALL,
                        leadingIcon = Icons.Default.Check
                    ) {
                        Text("Save")
                    }
                }
            }
        } else {
            // Display mode using IngredientRow concept with editing capabilities
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartEditing() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step number with better contrast
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                // Ingredient content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ingredient.name.ifEmpty { "Unnamed ingredient" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (ingredient.name.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    if (ingredient.quantity.isNotEmpty()) {
                        Text(
                            text = ingredient.quantity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Drag handle (for reordering)
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { },
                                onDragEnd = { },
                                onDrag = { _, _ -> }
                            )
                        }
                )
                
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

/**
 * Enhanced Editable Instruction Row
 */
@Composable
private fun EnhancedEditableInstructionRow(
    instruction: EditableInstruction,
    index: Int,
    totalCount: Int,
    onStartEditing: () -> Unit,
    onTextChange: (String) -> Unit,
    onSaveChanges: () -> Unit,
    onCancelEditing: (String) -> Unit,
    onDelete: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Store original value for cancel functionality
    val originalText = remember(instruction.id) { instruction.text }
    
    // Focus management
    val textFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Request focus when entering edit mode
    LaunchedEffect(instruction.isEditing) {
        if (instruction.isEditing) {
            textFocusRequester.requestFocus()
        }
    }
    
    Surface(
        modifier = modifier,
        shape = AppShapes.LargeShape,
        color = if (instruction.isEditing) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 0.dp // Remove gray borders for modern look
    ) {
        if (instruction.isEditing) {
            // Edit mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Step ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Enhanced instruction text area
                EnhancedTextField(
                    value = instruction.text,
                    onValueChange = onTextChange,
                    label = "Instruction",
                    placeholder = "Describe this step in detail...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(textFocusRequester),
                    minLines = 2,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onSaveChanges()
                        }
                    )
                )
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    // Cancel button
                    EnhancedSecondaryButton(
                        onClick = {
                            keyboardController?.hide()
                            onCancelEditing(originalText)
                        },
                        size = ButtonSize.SMALL,
                        leadingIcon = Icons.Default.Close
                    ) {
                        Text("Cancel")
                    }
                    
                    // Save button
                    EnhancedPrimaryButton(
                        onClick = {
                            keyboardController?.hide()
                            onSaveChanges()
                        },
                        enabled = instruction.text.trim().isNotEmpty(),
                        size = ButtonSize.SMALL,
                        leadingIcon = Icons.Default.Check
                    ) {
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step number with better contrast
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
                
                // Instruction text
                Text(
                    text = instruction.text.ifEmpty { "Empty instruction" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (instruction.text.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // Action icons - properly aligned
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drag handle - now using IconButton for consistent sizing
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { },
                                    onDragEnd = { },
                                    onDrag = { _, _ -> }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Delete button - consistent sizing
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete instruction",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced metadata preview card
 */
@Composable
private fun EnhancedMetadataPreview(
    prepTime: Int,
    cookTime: Int,
    servings: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.LargeShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recipe Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (prepTime > 0) {
                    MetadataItem(
                        icon = Icons.Default.Info,
                        value = "$prepTime min",
                        label = "Prep"
                    )
                }
                
                if (cookTime > 0) {
                    MetadataItem(
                        icon = Icons.Default.Info,
                        value = "$cookTime min",
                        label = "Cook"
                    )
                }
                
                if (servings > 0) {
                    MetadataItem(
                        icon = Icons.Default.Person,
                        value = "$servings",
                        label = "Serves"
                    )
                }
                
                if (prepTime > 0 && cookTime > 0) {
                    MetadataItem(
                        icon = Icons.Default.Info,
                        value = "${prepTime + cookTime} min",
                        label = "Total"
                    )
                }
            }
        }
    }
}

/**
 * Metadata item for the preview
 */
@Composable
private fun MetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * Enhanced helper card with tips
 */
@Composable
private fun EnhancedHelperCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.MediumShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Enhanced loading state
 */
@Composable
private fun EnhancedLoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.LargeShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
                )
                
                Text(
                text = message,
                    style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Enhanced error state
 */
@Composable
private fun EnhancedErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
                    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            type = EmptyStateType.CONNECTION_ERROR,
            onPrimaryAction = onRetry,
            customContent = EmptyStateContent(
                title = "Error loading recipe",
                subtitle = error,
                illustration = EmptyStateIllustration.NETWORK_ERROR,
                primaryAction = ActionButton(
                    text = "Retry",
                    onClick = onRetry,
                    icon = Icons.Default.Refresh
                )
            )
        )
    }
}

/**
 * Subtask 144: Save/Discard Transitions
 * Enhanced Save Confirmation Dialog with smooth animations
 */
@Composable
private fun EnhancedSaveConfirmationDialog(
    hasUnsavedChanges: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.LargeShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp, // Reduce shadow for modern look
            modifier = Modifier
                .scale(
                    animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dialog_scale"
                    ).value
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                        imageVector = if (hasUnsavedChanges) Icons.Default.Check else Icons.Default.CheckCircle,
                                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                
                // Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (hasUnsavedChanges) "Save Changes?" else "Publish Recipe?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = if (hasUnsavedChanges) {
                            "Your changes will be saved and the recipe will be updated."
                        } else {
                            "This recipe will be published and available to everyone."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedSecondaryButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Close
                    ) {
                        Text("Cancel")
                    }
                    
                    EnhancedPrimaryButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (hasUnsavedChanges) Icons.Default.Check else Icons.Default.CheckCircle
                    ) {
                        Text(if (hasUnsavedChanges) "Save" else "Publish")
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Discard Confirmation Dialog
 */
@Composable
private fun EnhancedDiscardConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.LargeShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp, // Reduce shadow for modern look
            modifier = Modifier
                .scale(
                    animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dialog_scale"
                    ).value
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Icon(
                        imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                
                // Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Discard Changes?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "You have unsaved changes. If you leave now, your changes will be lost.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedSecondaryButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Edit
                    ) {
                        Text("Stay")
                    }
                    
                            Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = AppShapes.MediumShape,
                        modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                            imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Discard")
                    }
                }
            }
        }
    }
}

/**
 * Preview composables for the enhanced draft editor
 */
@Preview(name = "Enhanced Draft Editor - Light")
@Composable
private fun EnhancedDraftEditorPreview() {
    ReciplanTheme {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enhanced Draft Editor Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(name = "Enhanced Save Confirmation - Dark")
@Composable
private fun EnhancedSaveConfirmationPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EnhancedSaveConfirmationDialog(
                    hasUnsavedChanges = true,
                    onConfirm = { },
                    onDismiss = { }
                )
            }
        }
    }
} 