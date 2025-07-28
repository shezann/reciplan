package com.example.reciplan.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.Recipe
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction

// Import motion system for animations
import com.example.reciplan.ui.theme.StaggeredMotion
import com.example.reciplan.ui.theme.MotionSpecs
import com.example.reciplan.ui.theme.MotionStates

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecipeScreen(
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToEditRecipe: (String) -> Unit,
    onNavigateToAddFromTikTok: () -> Unit = {},
    viewModelFactory: ViewModelProvider.Factory,
    showCreateButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val recipeFeed by viewModel.recipeFeed.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Get auth viewmodel to access current user
    val authViewModel: com.example.reciplan.ui.auth.AuthViewModel = viewModel(factory = viewModelFactory)
    val authState by authViewModel.authState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    var showCreateMenuDropdown by remember { mutableStateOf(false) }
    
    // Animation states
    var isInitialLoad by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") }
    
    // Get current user ID from auth state
    val currentUserId = when (val state = authState) {
        is com.example.reciplan.data.auth.AuthResult.Success -> state.user.id
        else -> null
    }
    
    // Filter options
    val filterOptions = listOf("All", "My Recipes", "Breakfast", "Lunch", "Dinner", "Dessert", "Snack")
    
    // Always use filtered recipes since all filters (including "All") are now applied
    val displayRecipes = uiState.filteredRecipes
    
    // Track when recipes are loaded for staggered animations
    LaunchedEffect(displayRecipes.size) {
        if (displayRecipes.isNotEmpty() && isInitialLoad) {
            isInitialLoad = false
        }
    }
    
    // Auto-clear messages after delay
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(5000) // Clear error after 5 seconds
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000) // Clear success message after 3 seconds
            viewModel.clearSuccessMessage()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recipes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (showCreateButton) {
                        Box {
                            FloatingActionButton(
                                onClick = { 
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCreateMenuDropdown = true 
                                },
                                modifier = Modifier.size(48.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 12.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Recipe",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            
                            // Enhanced Dropdown Menu
                            EnhancedCreateDropdownMenu(
                                expanded = showCreateMenuDropdown,
                                onDismissRequest = { showCreateMenuDropdown = false },
                                onCreateRecipeClick = {
                                    showCreateMenuDropdown = false
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateToCreateRecipe()
                                },
                                onAddFromTikTokClick = {
                                    showCreateMenuDropdown = false
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateToAddFromTikTok()
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchRecipes(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search recipes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Enhanced Filter Tags with Animations
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(filterOptions) { index, filter ->
                        // Staggered animation for filter chips
                        val chipScale by StaggeredMotion.staggeredAnimatedFloat(
                            targetValue = 1f,
                            index = index,
                            label = "filter_chip_scale",
                            baseDelayMs = 50,
                            maxDelayMs = 150
                        )
                        
                        val chipAlpha by StaggeredMotion.staggeredAnimatedFloat(
                            targetValue = 1f,
                            index = index,
                            label = "filter_chip_alpha",
                            baseDelayMs = 50,
                            maxDelayMs = 150
                        )
                        
                        // Selection animation
                        val isSelected = uiState.selectedFilter == filter
                        val selectionScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1f,
                            animationSpec = MotionSpecs.emphasizedSpring(),
                            label = "filter_selection_scale"
                        )
                        
                        FilterChip(
                            onClick = { 
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedFilter = filter
                                viewModel.filterRecipesByTag(filter) 
                            },
                            label = { Text(filter) },
                            selected = isSelected,
                            modifier = Modifier
                                .scale(chipScale * selectionScale)
                                .alpha(chipAlpha)
                                .animateItemPlacement(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
        
        // Animated Error Message
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
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
        }
        
        // Animated Success Message
        AnimatedVisibility(
            visible = uiState.successMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            uiState.successMessage?.let { successMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Recipe Feed
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshRecipes() }
        ) {
            if (displayRecipes.isEmpty() && uiState.isLoading) {
                // Enhanced initial loading state with emoji animation
                EnhancedInitialLoadingState()
            } else if (displayRecipes.isEmpty() && !uiState.isLoading) {
                // Enhanced Empty State with Animations
                EnhancedEmptyState(
                    onCreateRecipeClick = onNavigateToCreateRecipe,
                    selectedFilter = selectedFilter
                )
            } else {
                // Enhanced Recipe List with Staggered Animations
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = displayRecipes,
                        key = { _, recipe -> recipe.id }
                    ) { index, recipe ->
                        // Staggered entrance animation for recipe cards
                        val animatedScale by StaggeredMotion.staggeredAnimatedFloat(
                            targetValue = if (isInitialLoad) 0.8f else 1.0f,
                            index = index,
                            label = "recipe_card_scale",
                            baseDelayMs = 80,
                            maxDelayMs = 300
                        )
                        
                        val animatedAlpha by StaggeredMotion.staggeredAnimatedFloat(
                            targetValue = if (isInitialLoad) 0.0f else 1.0f,
                            index = index,
                            label = "recipe_card_alpha",
                            baseDelayMs = 80,
                            maxDelayMs = 300
                        )
                        
                        // Slide animation for new recipes
                        val slideOffset by animateFloatAsState(
                            targetValue = if (isInitialLoad) 50f else 0f,
                            animationSpec = MotionSpecs.emphasizedSpring(),
                            label = "recipe_slide_offset"
                        )
                        
                        // Get real-time like state for this recipe
                        val likeState by viewModel.getLikeState(recipe.id).collectAsState()
                        
                        RecipeCard(
                            recipe = recipe,
                            onRecipeClick = onNavigateToRecipeDetail,
                            onLikeClick = { recipeId, currentlyLiked -> 
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleLike(recipeId, currentlyLiked)
                            },
                            likeState = likeState,
                            onEditClick = onNavigateToEditRecipe,
                            onDeleteClick = { recipeId ->
                                recipeToDelete = displayRecipes.find { it.id == recipeId }
                                showDeleteDialog = true
                            },
                            isOwner = currentUserId != null && recipe.userId == currentUserId,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = animatedAlpha
                                    translationY = slideOffset
                                }
                                .animateItemPlacement()
                        )
                        
                        // Preload like states for performance optimization
                        LaunchedEffect(recipe.id) {
                            viewModel.preloadLikeStates(listOf(recipe))
                        }
                    }
                    
                    // Enhanced load more indicator
                    if (uiState.isLoading && displayRecipes.isNotEmpty()) {
                        item {
                            EnhancedLoadMoreIndicator()
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                recipeToDelete = null
            },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete '${recipeToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recipeToDelete?.let { viewModel.deleteRecipe(it.id) }
                        showDeleteDialog = false
                        recipeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        recipeToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Development helper - you can remove this in production
@Composable
fun RecipeScreenDevelopment(
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToEditRecipe: (String) -> Unit,
    onNavigateToAddFromTikTok: () -> Unit = {},
    viewModelFactory: ViewModelProvider.Factory,
    showCreateButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Development Tools
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.seedRecipes() }
                ) {
                    Text("Seed Recipes")
                }
                Button(
                    onClick = { viewModel.refreshRecipes() }
                ) {
                    Text("Refresh")
                }
            }
        }
        
        // Main Recipe Screen
        RecipeScreen(
            onNavigateToCreateRecipe = onNavigateToCreateRecipe,
            onNavigateToRecipeDetail = onNavigateToRecipeDetail,
            onNavigateToEditRecipe = onNavigateToEditRecipe,
            onNavigateToAddFromTikTok = onNavigateToAddFromTikTok,
            viewModelFactory = viewModelFactory,
            showCreateButton = showCreateButton,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Enhanced initial loading state with emoji animation from HomeScreen
 */
@Composable
private fun EnhancedInitialLoadingState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "initial_loading")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "loading_rotation"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated loading indicator with emoji
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍽️",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            Text(
                text = "Loading delicious recipes...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Finding the perfect dishes for you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced load more indicator with smooth animations
 */
@Composable
private fun EnhancedLoadMoreIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "load_more_animation")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "load_more_alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "load_more_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(alpha)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Loading more recipes...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Finding delicious dishes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
} 

/**
 * Enhanced empty state with context-aware messaging and animations
 */
@Composable
private fun EnhancedEmptyState(
    onCreateRecipeClick: () -> Unit,
    selectedFilter: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_animation")
    
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_offset"
    )
    
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_rotation"
    )
    
    // Context-aware messaging based on selected filter
    val (emoji, title, subtitle) = when (selectedFilter) {
        "My Recipes" -> Triple("👨‍🍳", "No personal recipes yet", "Start creating your culinary masterpieces!")
        "Breakfast" -> Triple("🥞", "No breakfast recipes", "Add some morning magic to your collection")
        "Lunch" -> Triple("🥗", "No lunch recipes", "Perfect time to add some midday favorites")
        "Dinner" -> Triple("🍽️", "No dinner recipes", "Create memorable evening meals")
        "Dessert" -> Triple("🧁", "No dessert recipes", "Sweet treats are waiting to be added")
        "Snack" -> Triple("🍿", "No snack recipes", "Add some quick bites to your collection")
        else -> Triple("🔍", "No recipes found", "Try a different filter or create your first recipe")
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated emoji icon
        Card(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    translationY = floatingOffset
                    rotationZ = iconRotation
                },
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 48.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Animated title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Animated subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Animated action button
        Button(
            onClick = onCreateRecipeClick,
            modifier = Modifier
                .scale(1.05f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Create Recipe",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Helper text with animation
        val helperAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "helper_alpha"
        )
        
        Text(
            text = "Tap filters above to explore different categories",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = helperAlpha),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Enhanced create dropdown menu with modern styling
 */
@Composable
private fun EnhancedCreateDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCreateRecipeClick: () -> Unit,
    onAddFromTikTokClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 8.dp,
        offset = DpOffset(x = (-8).dp, y = 8.dp)
    ) {
        // Header section
        Text(
            text = "Create Content",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
        
        // Create Recipe option
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Create Recipe",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Build from scratch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            onClick = onCreateRecipeClick,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        // Add from TikTok option
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add from TikTok",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Import from video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            onClick = onAddFromTikTokClick,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
} 