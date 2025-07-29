package com.example.reciplan.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.repository.LikeState
import com.example.reciplan.ui.components.*
import com.example.reciplan.ui.components.EmptyStateType
import com.example.reciplan.ui.recipe.RecipeCard
import com.example.reciplan.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.reciplan.R

// Performance optimizations
import com.example.reciplan.ui.theme.PerformanceMonitor
import com.example.reciplan.ui.theme.MemoryOptimizations
import com.example.reciplan.ui.theme.OptimizedAnimations

/**
 * Task 10: Home Screen Redesign
 * 
 * Enhanced Home screen with:
 * - Enhanced RecipeCard integration (Subtask 101)
 * - Staggered loading animations (Subtask 102) 
 * - Empty state integration (Subtask 103)
 * - Branded pull-to-refresh styling
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recipeFeed = viewModel.recipeFeed.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Animation states for staggered loading
    var isInitialLoad by remember { mutableStateOf(true) }
    
    // Search and filter states - use ViewModel state
    val searchQuery = uiState.searchQuery
    val selectedFilter = uiState.selectedFilter
    
    // Startup state management
    var hasStartupCompleted by remember { mutableStateOf(false) }
    var startupRetryCount by remember { mutableStateOf(0) }
    
    // Handle startup timing with automatic retry for auth issues
    LaunchedEffect(recipeFeed.loadState.refresh) {
        val refreshState = recipeFeed.loadState.refresh
        
        // Give a brief moment for authentication to settle on first launch
        if (!hasStartupCompleted && refreshState is LoadState.Loading) {
            println("🏠 HomeScreen: Initial load detected, giving auth time to settle...")
            kotlinx.coroutines.delay(1500) // Give auth extra time on startup
            hasStartupCompleted = true
        }
        
        // Handle authentication errors with progressive retry
        if (refreshState is LoadState.Error && recipeFeed.itemCount == 0) {
            val error = refreshState.error
            val isAuthError = error.message?.contains("401") == true ||
                             error.message?.contains("Unauthorized") == true ||
                             error.message?.contains("Authentication") == true
            
            if (isAuthError && startupRetryCount < 3) {
                val retryDelay = when (startupRetryCount) {
                    0 -> 1000L  // 1 second for first retry
                    1 -> 2000L  // 2 seconds for second retry  
                    else -> 3000L // 3 seconds for third retry
                }
                
                println("🏠 HomeScreen: Auth error detected (attempt ${startupRetryCount + 1}), retrying in ${retryDelay}ms...")
                startupRetryCount++
                kotlinx.coroutines.delay(retryDelay)
                recipeFeed.retry()
                println("🏠 HomeScreen: Retry executed (attempt $startupRetryCount)")
            }
        }
        
        // Reset states when loading is successful
        if (recipeFeed.loadState.refresh is LoadState.NotLoading && recipeFeed.itemCount > 0) {
            isInitialLoad = false
            startupRetryCount = 0 // Reset retry count on success
        }
    }
    
    // Auto-clear error messages after delay
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(5000) // Clear error after 5 seconds
            viewModel.clearError()
        }
    }
    
            // Auto-clear success messages after delay
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000) // Clear success message after 3 seconds
            viewModel.clearSuccessMessage()
        }
    }

    // Refresh feed when screen becomes visible (e.g., returning from create recipe)
    var hasRefreshed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasRefreshed) {
            kotlinx.coroutines.delay(1000) // Small delay to ensure screen is loaded
            viewModel.refreshAfterRecipeCreation()
            hasRefreshed = true
        }
    }

    

    
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header - matching RecipeScreen design
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
                // Title and Mascot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mascot
                    Image(
                        painter = painterResource(id = R.drawable.reciplan_mascot),
                        contentDescription = "Reciplan Mascot",
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
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
                
                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val filterOptions = listOf("All", "Breakfast", "Lunch", "Dinner", "Dessert", "Snack")
                    
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.filterRecipesByTag(filter)
                            },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
        
        // Error Display
        AnimatedVisibility(
            visible = uiState.error != null
        ) {
            uiState.error?.let { error ->
                EnhancedErrorCard(
                    error = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
        
        // Success Message Display
        AnimatedVisibility(
            visible = uiState.successMessage != null
        ) {
            uiState.successMessage?.let { message ->
                EnhancedSuccessCard(
                    message = message,
                    onDismiss = { viewModel.clearSuccessMessage() }
                )
            }
        }
        
        // Recipe Feed
        EnhancedRecipeFeedContent(
            recipeFeed = recipeFeed,
            viewModel = viewModel,
            onRecipeClick = onRecipeClick,
            listState = listState,
            isInitialLoad = isInitialLoad,
            startupRetryCount = startupRetryCount,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Enhanced TopAppBar with gradient background and better styling
 */
@Composable
private fun EnhancedTopAppBar(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recipe Feed",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Discover delicious recipes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Enhanced success card with better styling and animations
 */
@Composable
private fun EnhancedSuccessCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardScale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = MotionSpecs.emphasizedSpring(),
        label = "success_card_entrance"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .scale(cardScale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = AppShapes.LargeShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Enhanced error card with better styling and animations
 */
@Composable
private fun EnhancedErrorCard(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardScale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = MotionSpecs.emphasizedSpring(),
        label = "error_card_entrance"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .scale(cardScale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = AppShapes.LargeShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * Subtask 101: RecipeCard Integration
 * Subtask 102: Staggered Loading Animation
 * Subtask 103: Empty State Integration
 * 
 * Enhanced recipe feed content with staggered animations and empty states
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedRecipeFeedContent(
    recipeFeed: LazyPagingItems<Recipe>,
    viewModel: HomeViewModel,
    onRecipeClick: (String) -> Unit,
    listState: LazyListState,
    isInitialLoad: Boolean,
    startupRetryCount: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = recipeFeed.itemCount,
            key = recipeFeed.itemKey { it.id }
        ) { index ->
            val recipe = recipeFeed[index]
            if (recipe != null) {
                // Subtask 102: Staggered Loading Animation
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
                
                // Get like state for this recipe
                val likeState by viewModel.getLikeState(recipe.id).collectAsState()
                
                // Subtask 182: Performance monitoring and memory optimization
                PerformanceMonitor.TrackRecompositions("RecipeCard_${recipe.id}") {
                    RecipeCard(
                        recipe = recipe,
                        likeState = likeState,
                        onRecipeClick = onRecipeClick,
                        onLikeClick = { recipeId, currentlyLiked ->
                            viewModel.toggleLike(recipeId, currentlyLiked)
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                alpha = animatedAlpha
                            }
                            .animateItemPlacement()
                    )
                }
                
                // Subtask 183: Memory-efficient state preloading
                LaunchedEffect(recipe.id) { // Use stable key to prevent unnecessary recomposition
                    viewModel.preloadLikeStates(listOf(recipe))
                }
            }
        }
        
        // Enhanced loading state for pagination
        when (recipeFeed.loadState.append) {
            is LoadState.Loading -> {
                item {
                    EnhancedLoadingItem()
                }
            }
            is LoadState.Error -> {
                item {
                    EnhancedPaginationErrorItem(
                        onRetry = { recipeFeed.retry() }
                    )
                }
            }
            else -> {}
        }
        
        // Handle initial loading state
        when (recipeFeed.loadState.refresh) {
            is LoadState.Loading -> {
                if (recipeFeed.itemCount == 0) {
                    item {
                        EnhancedInitialLoadingState()
                    }
                }
            }
            is LoadState.Error -> {
                if (recipeFeed.itemCount == 0) {
                    val error = recipeFeed.loadState.refresh as LoadState.Error
                    val isAuthError = error.error.message?.contains("401") == true ||
                                     error.error.message?.contains("Unauthorized") == true ||
                                     error.error.message?.contains("Authentication") == true
                    
                    // Show friendly loading state if we're still in startup retry phase
                    val isStartupPhase = startupRetryCount > 0 && startupRetryCount < 3
                    
                    item {
                        if (isAuthError && isStartupPhase) {
                            // Show a more user-friendly loading state for auth errors during startup
                            println("🏠 HomeScreen: Auth error during startup retry, showing loading state")
                            EnhancedInitialLoadingState(
                                message = "Setting up your recipes..."
                            )
                        } else if (isAuthError) {
                            // After multiple retries, show a more specific auth error
                            println("🏠 HomeScreen: Auth error after retries, showing auth-specific error")
                            EmptyState(
                                type = EmptyStateType.CONNECTION_ERROR,
                                onPrimaryAction = { recipeFeed.retry() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            )
                        } else {
                            // Show connection error for actual network issues
                            println("🏠 HomeScreen: Network error, showing connection error")
                            EmptyState(
                                type = EmptyStateType.CONNECTION_ERROR,
                                onPrimaryAction = { recipeFeed.retry() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            )
                        }
                    }
                }
            }
            else -> {}
        }
        
        // Subtask 103: Empty State Integration (No recipes case)
        if (recipeFeed.loadState.refresh is LoadState.NotLoading && recipeFeed.itemCount == 0) {
            item {
                EmptyState(
                    type = EmptyStateType.NO_RECIPES,
                    onPrimaryAction = { /* Navigate to add recipe */ },
                    onSecondaryAction = { /* Navigate to browse recipes */ },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            }
        }
    }
}

/**
 * Enhanced loading item for pagination with branded styling
 */
@Composable
private fun EnhancedLoadingItem(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_alpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = AppShapes.LargeShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Loading more recipes...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced pagination error item with better styling
 */
@Composable
private fun EnhancedPaginationErrorItem(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = AppShapes.LargeShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Couldn't load more recipes",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Check your connection and try again",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FilledTonalButton(
                onClick = onRetry,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                shape = AppShapes.MediumShape
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Enhanced initial loading state with branded styling
 */
@Composable
private fun EnhancedInitialLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading recipes..."
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
            // Animated loading indicator
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
                text = message,
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
 * Preview composables for the enhanced home screen
 */
@Preview(name = "Enhanced Home Screen - Light")
@Composable
private fun EnhancedHomeScreenPreview() {
    ReciplanTheme {
        Surface {
            // Preview would show the enhanced home screen layout
            // In a real implementation, this would use sample data
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
                    text = "Enhanced Home Screen Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(name = "Enhanced Home Screen - Dark")
@Composable
private fun EnhancedHomeScreenDarkPreview() {
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
                    text = "Enhanced Home Screen Dark Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
} 