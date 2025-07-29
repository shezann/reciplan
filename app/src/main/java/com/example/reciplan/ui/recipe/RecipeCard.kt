package com.example.reciplan.ui.recipe

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.reciplan.R
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.repository.LikeState
import com.example.reciplan.ui.components.LikeButton
import com.example.reciplan.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

// Performance optimizations
import com.example.reciplan.ui.theme.OptimizedAnimations
import com.example.reciplan.ui.theme.RecompositionOptimizations
import com.example.reciplan.ui.theme.MemoryOptimizations
import com.example.reciplan.ui.theme.PerformanceMonitor

/**
 * Enhanced RecipeCard with full-bleed design, gradient overlay, and micro-interactions
 * Now uses the new food-centric design system with Tomato, Basil, Cream, and Charcoal colors
 * 
 * Features:
 * - Full-bleed thumbnail with proper aspect ratio (Subtask 21)
 * - Gradient overlay for text readability (Subtask 22) 
 * - Likes chip positioned bottom-right (Subtask 23)
 * - Card micro-interactions with haptic feedback (Subtask 24)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    onRecipeClick: (String) -> Unit,
    onLikeClick: (String, Boolean) -> Unit = { _, _ -> },
    likeState: LikeState = LikeState(),
    onEditClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    isOwner: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Performance optimization: Create stable data structures to prevent recomposition
    val stableLikeState = RecompositionOptimizations.StableLikeState(
        isLiked = likeState.liked,
        likesCount = likeState.likesCount,
        isLoading = likeState.isLoading,
        hasError = likeState.error != null
    )
    
    val stableRecipeItem = RecompositionOptimizations.StableRecipeItem(
        id = recipe.id,
        title = recipe.title,
        thumbnail = recipe.videoThumbnail,
        likeState = stableLikeState
    )
    
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showDropdownMenu by remember { mutableStateOf(false) }
    
    // Subtask 181: Optimized animations for 60fps performance
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = OptimizedAnimations.performantSpring(
            dampingRatio = 0.8f, // Increased damping for stability
            stiffness = 400f,   // Optimal stiffness for 60fps
            visibilityThreshold = 0.001f
        ),
        label = "optimized_card_scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        animationSpec = OptimizedAnimations.performantTween(
            baseDurationMs = 150
        ),
        label = "optimized_card_elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null // Custom animation replaces ripple
            ) { 
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onRecipeClick(recipe.id) 
            },
        shape = RecipeShapes.Card, // Using new design system shape (16dp)
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CreamLight, // Use warmer surface color
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = null // Explicitly remove any border
    ) {
        // Remove default Card content padding to enable full-bleed image
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        // Subtasks 21 & 22: Full-bleed image with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // Increased for better visual impact
        ) {
            // Full-bleed image with proper content scaling
            SubcomposeAsyncImage(
                model = stableRecipeItem.thumbnail,
                contentDescription = "Recipe: ${stableRecipeItem.title}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RecipeShapes.CardImage), // Top-rounded shape from design system
                contentScale = ContentScale.Crop // Crop to fill width completely
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
                                    text = "Loading recipe...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        // Friendly default thumbnail with food icon
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
                        // Friendly default thumbnail when no image is provided
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
            
            // Subtask 22: Gradient Overlay System for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Recipe Title in white over gradient
            Text(
                text = recipe.title,
                style = AppTypographyExtended.recipeCardTitle.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(0.7f), // Leave space for likes chip
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Subtask 23: Likes Chip Integration (bottom-right)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = AppShapes.LikeChipShape, // Fully rounded chip
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                LikeButton(
                    isLiked = stableLikeState.isLiked,
                    likesCount = stableLikeState.likesCount,
                    isLoading = stableLikeState.isLoading,
                    onClick = { 
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick(stableRecipeItem.id, stableLikeState.isLiked) 
                    },
                    showCount = stableLikeState.likesCount > 0,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // Owner actions menu (enhanced with design system)
            if (isOwner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = AppShapes.CircularShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Recipe options",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false },
                        shape = AppShapes.DialogShape
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEditClick(recipe.id)
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit recipe"
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                ) 
                            },
                            onClick = {
                                onDeleteClick(recipe.id)
                                showDropdownMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete recipe",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
            
            // Difficulty stars badge using design system colors
            if (recipe.difficulty > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), // Tomato color
                    shape = RecipeShapes.DifficultyChip
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        repeat(recipe.difficulty) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Recipe metadata section with design system typography
        if (!recipe.description.isNullOrEmpty() || recipe.tags.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recipe description
                if (!recipe.description.isNullOrEmpty()) {
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Recipe metadata using design system typography
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱ ${recipe.prepTime + recipe.cookTime} min",
                        style = AppTypographyExtended.recipeMetadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${recipe.servings} servings",
                        style = AppTypographyExtended.recipeMetadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Recipe tags using design system shapes and colors
                if (recipe.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.tags.take(3).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer, // Basil color
                                shape = RecipeShapes.Tag,
                                modifier = Modifier.animateContentSize()
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (recipe.tags.size > 3) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RecipeShapes.Tag
                            ) {
                                Text(
                                    text = "+${recipe.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Enhanced error display using design system
        likeState.error?.let { error ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = AppShapes.SmallShape
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        } // Close Column for full-bleed layout
    }
}

/**
 * Preview composables showcasing the new design system
 */
@Preview(name = "Recipe Card - New Design System")
@Composable
private fun RecipeCardPreview() {
    ReciplanTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RecipeCard(
                recipe =                 Recipe(
                    id = "1",
                    title = "Tomato Basil Pasta with Fresh Herbs",
                    description = "A warm and comforting pasta dish featuring our signature tomato and basil combination",
                    videoThumbnail = null,
                    prepTime = 15,
                    cookTime = 25,
                    servings = 4,
                    difficulty = 2,
                    tags = listOf("Italian", "Pasta", "Vegetarian", "Quick"),
                    ingredients = emptyList(),
                    instructions = emptyList(),
                    userId = "test",
                    createdAt = "2024-01-20T10:00:00Z",
                    updatedAt = "2024-01-20T10:00:00Z"
                ),
                onRecipeClick = {},
                likeState = LikeState(liked = false, likesCount = 24)
            )
        }
    }
}

@Preview(name = "Recipe Card - Dark Theme")
@Composable
private fun RecipeCardDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            RecipeCard(
                recipe =                 Recipe(
                    id = "2",
                    title = "Spicy Korean Kimchi Fried Rice",
                    description = "Bold flavors with fermented kimchi and perfectly seasoned rice",
                    videoThumbnail = null,
                    prepTime = 10,
                    cookTime = 15,
                    servings = 2,
                    difficulty = 3,
                    tags = listOf("Korean", "Spicy", "Rice"),
                    ingredients = emptyList(),
                    instructions = emptyList(),
                    userId = "test",
                    createdAt = "2024-01-20T10:00:00Z",
                    updatedAt = "2024-01-20T10:00:00Z"
                ),
                onRecipeClick = {},
                likeState = LikeState(liked = true, likesCount = 127),
                isOwner = true
            )
        }
    }
} 