package com.example.reciplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

/**
 * A reusable like button component with heart icon, animations, and accessibility support
 * 
 * @param isLiked Current like state
 * @param likesCount Number of likes to display
 * @param isLoading Whether the button is in loading state
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for styling
 * @param enabled Whether the button is enabled
 * @param showCount Whether to show the likes count
 * @param contentDescription Custom content description for accessibility
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
    contentDescription: String? = null
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // Animation states
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "scale_animation"
    )
    
    val heartColor by animateColorAsState(
        targetValue = when {
            isLoading -> MaterialTheme.colorScheme.outline
            isLiked -> Color(0xFFE91E63) // Pink/Red color for liked state
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 200),
        label = "color_animation"
    )
    
    // Content description for accessibility
    val accessibilityDescription = contentDescription ?: if (isLiked) {
        "Unlike. Currently $likesCount likes"
    } else {
        "Like. Currently $likesCount likes"
    }
    
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled && !isLoading,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                indication = null, // Use default Material 3 ripple
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics {
                this.contentDescription = accessibilityDescription
                this.role = Role.Button
                if (!enabled || isLoading) {
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
                else -> {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null, // Handled by parent semantics
                        modifier = Modifier.scale(scale),
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
                color = if (enabled && !isLoading) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline
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
 * Animated like button that triggers a bounce effect when clicked
 */
@Composable
fun AnimatedLikeButton(
    isLiked: Boolean,
    likesCount: Int,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCount: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val bounceScale by animateFloatAsState(
        targetValue = when {
            isLoading -> 0.9f
            isPressed -> 1.2f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = 600f
        ),
        finishedListener = {
            if (isPressed) {
                isPressed = false
            }
        },
        label = "bounce_animation"
    )
    
    LikeButton(
        isLiked = isLiked,
        likesCount = likesCount,
        isLoading = isLoading,
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.scale(bounceScale),
        enabled = enabled,
        showCount = showCount
    )
}

@Preview(name = "Like Button - Unliked")
@Composable
private fun LikeButtonUnlikedPreview() {
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

@Preview(name = "Like Button - Liked")
@Composable
private fun LikeButtonLikedPreview() {
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

@Preview(name = "Like Button - Loading")
@Composable
private fun LikeButtonLoadingPreview() {
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

@Preview(name = "Like Button - Variations")
@Composable
private fun LikeButtonVariationsPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Regular Like Button")
                LikeButton(
                    isLiked = false,
                    likesCount = 1247,
                    onClick = {}
                )
                
                Text("Animated Like Button")
                AnimatedLikeButton(
                    isLiked = true,
                    likesCount = 1248,
                    onClick = {}
                )
                
                Text("Without Count")
                LikeButton(
                    isLiked = true,
                    likesCount = 0,
                    showCount = false,
                    onClick = {}
                )
                
                Text("Disabled")
                LikeButton(
                    isLiked = false,
                    likesCount = 42,
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Like Button - Dark Theme")
@Composable
private fun LikeButtonDarkPreview() {
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
            }
        }
    }
} 