package com.example.reciplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reciplan.ui.theme.ReciplanTheme
import kotlinx.coroutines.delay

// Performance optimizations
import com.example.reciplan.ui.theme.OptimizedAnimations
import com.example.reciplan.ui.theme.RecompositionOptimizations

/**
 * Enhanced LikeButton component with improved animations, haptic feedback, and performance
 * 
 * Features:
 * - Heart scaling animation with spring physics (Subtask 31)
 * - Enhanced haptic feedback for different states (Subtask 32) 
 * - Performance optimization with debouncing (Subtask 33)
 * 
 * @param isLiked Current like state
 * @param likesCount Number of likes to display
 * @param isLoading Whether the button is in loading state
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for styling
 * @param enabled Whether the button is enabled
 * @param showCount Whether to show the likes count
 * @param contentDescription Custom content description for accessibility
 * @param hasError Whether there's an error state to trigger error haptics
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    likesCount: Int,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCount: Boolean = true,
    contentDescription: String? = null,
    hasError: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // Subtask 33: Performance Optimization - Debouncing state
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }
    val debounceTime = 300L // Prevent rapid taps within 300ms
    
    // Subtask 181: Optimized heart scaling animation for 60fps performance
    val heartScale by animateFloatAsState(
        targetValue = when {
            isLoading -> 0.8f
            isPressed -> 1.3f // Larger scale for more satisfying bounce
            else -> 1f
        },
        animationSpec = OptimizedAnimations.performantSpring(
            dampingRatio = 0.6f, // Optimized damping for stability and bounce
            stiffness = 500f,    // Higher stiffness for better performance
            visibilityThreshold = 0.01f
        ),
        finishedListener = { finalValue ->
            // Reset pressed state after animation completes
            if (finalValue == 1.3f) {
                isPressed = false
            }
        },
        label = "optimized_heart_scale"
    )
    
    // Subtask 181: Optimized color animation with frame-aligned timing
    val heartColor by animateColorAsState(
        targetValue = when {
            hasError -> MaterialTheme.colorScheme.error
            isLoading -> MaterialTheme.colorScheme.outline
            isLiked -> Color(0xFFE91E63) // Pink/Red color for liked state
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = OptimizedAnimations.performantTween<Color>(
            baseDurationMs = 250
        ),
        label = "optimized_heart_color"
    )
    
    // Subtask 32: Enhanced Haptic Feedback - Different haptics for different states
    LaunchedEffect(hasError) {
        if (hasError) {
            // Error haptic feedback
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    // Content description for accessibility
    val accessibilityDescription = contentDescription ?: when {
        hasError -> "Error occurred. Try again."
        isLoading -> "Loading likes..."
        isLiked -> "Unlike. Currently $likesCount likes"
        else -> "Like. Currently $likesCount likes"
    }
    
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled && !isLoading && !hasError,
                onClick = {
                    // Subtask 33: Performance Optimization - Debouncing logic
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime >= debounceTime) {
                        lastClickTime = currentTime
                        isPressed = true
                        
                        // Subtask 32: Haptic Feedback Integration - Different haptics for like/unlike
                        val hapticType = if (isLiked) {
                            HapticFeedbackType.LongPress // Stronger haptic for unlike
                        } else {
                            HapticFeedbackType.TextHandleMove // Lighter haptic for like
                        }
                        hapticFeedback.performHapticFeedback(hapticType)
                        
                        onClick()
                    }
                },
                indication = null, // Custom animation replaces ripple
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics {
                this.contentDescription = accessibilityDescription
                this.role = Role.Button
                if (!enabled || isLoading || hasError) {
                    disabled()
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                hasError -> {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.scale(heartScale),
                        tint = heartColor
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null, // Handled by parent semantics
                        modifier = Modifier.scale(heartScale),
                        tint = heartColor
                    )
                }
            }
        }
        
        if (showCount) {
            Text(
                text = formatLikesCount(likesCount),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isLiked) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 12.sp
                ),
                color = when {
                    hasError -> MaterialTheme.colorScheme.error
                    enabled && !isLoading -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

/**
 * Formats the likes count for display
 * Examples: 0, 1, 42, 1.2K, 15K, 1.5M
 */
private fun formatLikesCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 10000 -> "${(count / 100) / 10.0}K".replace(".0", "")
        count < 1000000 -> "${count / 1000}K"
        else -> "${(count / 100000) / 10.0}M".replace(".0", "")
    }
}

/**
 * Advanced animated like button with enhanced spring physics and performance optimizations
 * This component demonstrates the full enhanced feature set
 */
@Composable
fun EnhancedAnimatedLikeButton(
    isLiked: Boolean,
    likesCount: Int,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCount: Boolean = true,
    hasError: Boolean = false
) {
    // Performance optimization: Stable reference for onClick callback
    val stableOnClick by rememberUpdatedState(onClick)
    
    LikeButton(
        isLiked = isLiked,
        likesCount = likesCount,
        isLoading = isLoading,
        onClick = stableOnClick,
        modifier = modifier,
        enabled = enabled,
        showCount = showCount,
        hasError = hasError
    )
}

@Preview(name = "Enhanced Like Button - Unliked")
@Composable
private fun EnhancedLikeButtonUnlikedPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Enhanced Like Button - Liked")
@Composable
private fun EnhancedLikeButtonLikedPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LikeButton(
                    isLiked = true,
                    likesCount = 43,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Enhanced Like Button - Error State")
@Composable
private fun EnhancedLikeButtonErrorPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    hasError = true,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Enhanced Like Button - Loading")
@Composable
private fun EnhancedLikeButtonLoadingPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LikeButton(
                    isLiked = true,
                    likesCount = 43,
                    isLoading = true,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Enhanced Like Button - Comprehensive")
@Composable
private fun EnhancedLikeButtonComprehensivePreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enhanced Heart Scaling Animation")
                LikeButton(
                    isLiked = false,
                    likesCount = 1247,
                    onClick = {}
                )
                
                Text("Enhanced with Error State")
                LikeButton(
                    isLiked = true,
                    likesCount = 1248,
                    hasError = true,
                    onClick = {}
                )
                
                Text("Enhanced Animated Version")
                EnhancedAnimatedLikeButton(
                    isLiked = true,
                    likesCount = 999,
                    onClick = {}
                )
                
                Text("Without Count - Enhanced")
                LikeButton(
                    isLiked = true,
                    likesCount = 0,
                    showCount = false,
                    onClick = {}
                )
                
                Text("Loading State - Enhanced")
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    isLoading = true,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Enhanced Like Button - Dark Theme")
@Composable
private fun EnhancedLikeButtonDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    onClick = {}
                )
                LikeButton(
                    isLiked = true,
                    likesCount = 43,
                    onClick = {}
                )
                LikeButton(
                    isLiked = false,
                    likesCount = 44,
                    hasError = true,
                    onClick = {}
                )
            }
        }
    }
} 