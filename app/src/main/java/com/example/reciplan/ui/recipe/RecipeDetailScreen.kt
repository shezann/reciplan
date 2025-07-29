package com.example.reciplan.ui.recipe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.reciplan.R
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.ui.components.*
import com.example.reciplan.ui.theme.*

/**
 * Task 11: Recipe Detail Screen Redesign
 * 
 * Enhanced Recipe Detail screen with:
 * - Enhanced Image Gallery (Subtask 111)
 * - Ingredient Checklist Integration (Subtask 112)
 * - Typography Hierarchy (Subtask 113)
 * - Enhanced Action Buttons (Subtask 114)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onNavigateBack: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    
    // Animation and interaction states
    var isImageExpanded by remember { mutableStateOf(false) }
    var ingredientCheckStates by remember { mutableStateOf(mapOf<String, Boolean>()) }
    
    // Navigation protection state - shared between system back and UI back button
    var isNavigating by remember { mutableStateOf(false) }
    
    // Debounced navigation function
    val performNavigation = remember {
        {
            if (!isNavigating) {
                println("Navigation triggered - starting navigation")
                isNavigating = true
                try {
                    onNavigateBack()
                    println("onNavigateBack() call completed successfully")
                } catch (e: Exception) {
                    println("Error in onNavigateBack(): ${e.message}")
                    // Reset navigation state on error so user can try again
                    isNavigating = false
                }
            } else {
                println("Navigation triggered - already in progress, ignoring")
            }
        }
    }
    
    // Timeout to reset navigation state if it gets stuck
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            kotlinx.coroutines.delay(3000) // 3 second timeout
            if (isNavigating) {
                println("Navigation timeout - resetting navigation state")
                isNavigating = false
            }
        }
    }
    
    // Load the recipe when screen opens
    LaunchedEffect(recipeId) {
        viewModel.getRecipe(recipeId)
    }
    
    // Initialize ingredient check states
    LaunchedEffect(selectedRecipe?.ingredients) {
        selectedRecipe?.ingredients?.let { ingredients ->
            ingredientCheckStates = ingredients.associate { "${it.quantity} ${it.name}" to false }
        }
    }
    
    // Handle system back button with same debouncing protection
    BackHandler {
        println("System back button pressed")
        performNavigation()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Main content layer
        if (uiState.isLoading) {
            EnhancedLoadingState()
        } else if (selectedRecipe != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp) // Reduced padding since FAB was removed
            ) {
                item {
                    // Subtask 111: Enhanced Image Gallery
                    EnhancedImageGallery(
                        recipe = selectedRecipe!!,
                        isExpanded = isImageExpanded,
                        onExpandToggle = { isImageExpanded = !isImageExpanded }
                    )
                }
                
                item {
                    // Recipe Header with enhanced typography
                    EnhancedRecipeHeader(
                        recipe = selectedRecipe!!,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                
                item {
                    // Recipe Metadata with improved design
                    EnhancedRecipeMetadata(
                        recipe = selectedRecipe!!,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Subtask 112: Enhanced Ingredient Checklist
                item {
                    EnhancedIngredientsSection(
                        ingredients = selectedRecipe!!.ingredients,
                        checkStates = ingredientCheckStates,
                        onCheckChanged = { ingredient, checked ->
                            ingredientCheckStates = ingredientCheckStates.toMutableMap().apply {
                                put(ingredient, checked)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Instructions Section
                item {
                    EnhancedInstructionsSection(
                        instructions = selectedRecipe!!.instructions,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                
                // Tags Section (if any)
                if (selectedRecipe!!.tags.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        EnhancedTagsSection(
                            tags = selectedRecipe!!.tags,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                
                // Nutrition Section (if available)
                selectedRecipe!!.nutrition?.let { nutrition ->
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        EnhancedNutritionSection(
                            nutrition = nutrition,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else if (uiState.error != null) {
            EnhancedErrorState(
                error = uiState.error!!,
                onRetry = { viewModel.getRecipe(recipeId) }
            )
        }
    }
    
    // Separate overlay container for the back button - completely independent layer
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Enhanced back button overlay - in its own composition layer
        EnhancedBackButton(
            onNavigateBack = performNavigation,
            isNavigating = isNavigating,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
        // Temporary debug area to verify touch isolation (remove after testing)
        if (uiState.error != null) { // Only show when there's an error to avoid cluttering
            Surface(
                onClick = {
                    println("DEBUG: Touch area verification - back button area is responsive")
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Debug touch test",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(14.dp)
                )
            }
        }
    }
}

/**
 * Subtask 111: Enhanced Image Gallery
 * Improved recipe image display with navigation and zoom capabilities
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedImageGallery(
    recipe: Recipe,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // For now, we'll show the main image. In a real app, this would be a list of images
    val images = listOfNotNull(recipe.videoThumbnail).take(3)
    val pagerState = rememberPagerState(pageCount = { images.size })
    
    val imageHeight by animateDpAsState(
        targetValue = if (isExpanded) 400.dp else 280.dp,
        animationSpec = MotionSpecs.emphasizedTween(MotionDurations.LONG),
        label = "image_height"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(imageHeight)
    ) {
        if (images.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[page])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Recipe image ${page + 1} for ${recipe.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Loading image...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                is AsyncImagePainter.State.Error -> {
                                    // Friendly default image
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Recipe",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "🍽️",
                                                style = MaterialTheme.typography.headlineMedium,
                                                modifier = Modifier.scale(1.5f)
                                            )
                                        }
                                    }
                                }
                                is AsyncImagePainter.State.Success -> {
                                    SubcomposeAsyncImageContent()
                                }
                                else -> {
                                    // Default state when no image
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Recipe",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "👨‍🍳",
                                                style = MaterialTheme.typography.headlineMedium,
                                                modifier = Modifier.scale(1.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
            }
            
            // Image overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Image indicators (if multiple images)
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(images.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .background(
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
            
            // Expand/Collapse indicator removed as requested
        }
    }
}

/**
 * Enhanced Recipe Header with better typography hierarchy
 */
@Composable
private fun EnhancedRecipeHeader(
    recipe: Recipe,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Subtask 113: Typography Hierarchy - Recipe Title
                                Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 36.sp
        )
        
        // Recipe Description with improved hierarchy
        if (!recipe.description.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
                                Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp
                ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
    }
}

/**
 * Enhanced Recipe Metadata with improved visual design
 */
@Composable
private fun EnhancedRecipeMetadata(
    recipe: Recipe,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.LargeShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Total Time
            EnhancedMetadataItem(
                icon = Icons.Default.Info,
                value = "${recipe.prepTime + recipe.cookTime} min",
                label = "Total Time",
                color = MaterialTheme.colorScheme.primary
            )
            
            // Servings
            EnhancedMetadataItem(
                icon = Icons.Default.Person,
                value = "${recipe.servings}",
                label = "Servings",
                color = MaterialTheme.colorScheme.secondary
            )
                            
            // Difficulty
            EnhancedMetadataItem(
                icon = Icons.Default.Star,
                value = when (recipe.difficulty) {
                    1 -> "Easy"
                    2 -> "Medium"
                    3 -> "Hard"
                    else -> "Easy"
                },
                label = "Difficulty",
                color = MaterialTheme.colorScheme.tertiary,
                customContent = {
                    Row {
                        repeat(recipe.difficulty) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun EnhancedMetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    customContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.9f),
                modifier = Modifier.padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (customContent != null) {
            customContent()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Subtask 112: Ingredient Checklist Integration
 * Replace ingredient list with IngredientRow components
 */
@Composable
private fun EnhancedIngredientsSection(
    ingredients: List<com.example.reciplan.data.model.Ingredient>,
    checkStates: Map<String, Boolean>,
    onCheckChanged: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
                        modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ingredients",
                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Progress indicator
            val completedCount = checkStates.values.count { it }
            val totalCount = ingredients.size
            
            if (totalCount > 0) {
                Surface(
                    shape = AppShapes.SmallShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$completedCount/$totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress bar
        if (ingredients.isNotEmpty()) {
            val progress = checkStates.values.count { it }.toFloat() / ingredients.size
            LinearProgressIndicator(
                progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                    .height(4.dp)
                    .clip(AppShapes.SmallShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Ingredient List using IngredientRow components
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.LargeShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                ingredients.forEachIndexed { index, ingredient ->
                    val ingredientKey = "${ingredient.quantity} ${ingredient.name}"
                    val isChecked = checkStates[ingredientKey] ?: false
                    
                    IngredientRow(
                        ingredient = IngredientData(
                            id = ingredientKey,
                            name = "${ingredient.quantity} ${ingredient.name}",
                            isChecked = isChecked
                        ),
                        onCheckedChange = { onCheckChanged(ingredientKey, !isChecked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (index < ingredients.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced Instructions Section with better visual hierarchy
 */
@Composable
private fun EnhancedInstructionsSection(
    instructions: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
                            Text(
            text = "Instructions",
            style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
        
        Spacer(modifier = Modifier.height(16.dp))
        
                // Instructions List
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            instructions.forEachIndexed { index, instruction ->
                Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = AppShapes.LargeShape,
                    color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                            .padding(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                        // Step Number
                                Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.wrapContentSize(Alignment.Center)
                                    )
                                }
                                
                        Spacer(modifier = Modifier.width(16.dp))
                                
                        // Instruction Text
                                Text(
                                    text = instruction,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 22.sp
                            ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                    }
                }
                
                if (index < instructions.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Enhanced Tags Section with better visual design
 */
@Composable
private fun EnhancedTagsSection(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
                                Text(
                                    text = "Tags",
            style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
        Spacer(modifier = Modifier.height(16.dp))
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        items(tags) { tag ->
                                            Surface(
                    shape = AppShapes.MediumShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                                }
                            }
                        }
                    }
                }
                
/**
 * Enhanced Nutrition Section with better layout
 */
@Composable
private fun EnhancedNutritionSection(
    nutrition: com.example.reciplan.data.model.Nutrition,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
                                Text(
            text = "Nutrition (per serving)",
            style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.LargeShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                nutrition.calories?.let { calories ->
                    NutritionItem("${calories.toInt()}", "Calories", MaterialTheme.colorScheme.primary)
                }
                
                nutrition.protein?.let { protein ->
                    NutritionItem("${protein.toInt()}g", "Protein", MaterialTheme.colorScheme.secondary)
                }
                
                nutrition.carbs?.let { carbs ->
                    NutritionItem("${carbs.toInt()}g", "Carbs", MaterialTheme.colorScheme.tertiary)
                }
                
                nutrition.fat?.let { fat ->
                    NutritionItem("${fat.toInt()}g", "Fat", MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun NutritionItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
                                            Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
                                            )
                                            Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    


/**
 * Enhanced Back Button with debouncing and navigation protection
 */
@Composable
private fun EnhancedBackButton(
    onNavigateBack: () -> Unit,
    isNavigating: Boolean,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // Click handler with haptic feedback
    val handleClick = remember(onNavigateBack) {
        {
            if (!isNavigating) {
                println("🔙 Enhanced back button clicked - performing navigation")
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigateBack()
            } else {
                println("🔙 Enhanced back button clicked - navigation already in progress, ignoring")
            }
        }
    }
    
    Surface(
        onClick = handleClick,
        enabled = !isNavigating,
        modifier = modifier
            .size(64.dp), // Even larger touch target
        shape = CircleShape,
        color = if (isNavigating) 
            Color.Black.copy(alpha = 0.4f) 
        else 
            Color.Black.copy(alpha = 0.8f), // More opaque for better visibility
        shadowElevation = 8.dp // Add shadow for prominence
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isNavigating) {
                // Show loading indicator when navigation is in progress
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp) // Larger icon for better visibility
                )
            }
        }
    }
}

/**
 * Enhanced Loading State
 */
@Composable
private fun EnhancedLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            
                                            Text(
                text = "Loading recipe...",
                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

/**
 * Enhanced Error State
 */
@Composable
private fun EnhancedErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            type = EmptyStateType.CONNECTION_ERROR,
            onPrimaryAction = onRetry,
            customContent = EmptyStateContent(
                title = "Couldn't load recipe",
                subtitle = error,
                illustration = EmptyStateIllustration.NETWORK_ERROR,
                primaryAction = ActionButton(
                    text = "Try Again",
                    onClick = onRetry,
                    icon = Icons.Default.Refresh
                )
            )
        )
    }
}

/**
 * Preview composables for the enhanced recipe detail screen
 */
@Preview(name = "Enhanced Recipe Detail - Light")
@Composable
private fun EnhancedRecipeDetailPreview() {
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
                    text = "Enhanced Recipe Detail Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(name = "Enhanced Recipe Detail - Dark")
@Composable
private fun EnhancedRecipeDetailDarkPreview() {
    ReciplanTheme(darkTheme = true) {
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
                    text = "Enhanced Recipe Detail Dark Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
} 