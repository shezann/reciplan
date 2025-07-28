package com.example.reciplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reciplan.ui.theme.*

// Performance optimizations
import com.example.reciplan.ui.theme.OptimizedAnimations
import com.example.reciplan.ui.theme.RecompositionOptimizations

/**
 * Button size variants
 */
enum class ButtonSize {
    SMALL,      // Compact buttons for inline actions
    MEDIUM,     // Standard buttons for most use cases
    LARGE       // Prominent buttons for primary actions
}

/**
 * Comprehensive button system with proper styling and states
 * 
 * Features:
 * - Tomato primary buttons (Subtask 81)
 * - Basil outline secondary buttons (Subtask 82) 
 * - Proper disabled states with Charcoal at 10% alpha (Subtask 83)
 * 
 * @param onClick Callback when button is clicked
 * @param modifier Modifier for styling
 * @param enabled Whether the button is enabled
 * @param size Button size variant
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param interactionSource Interaction source for touch handling
 * @param content Button content (typically text)
 */
@Composable
fun EnhancedPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Subtask 181: Optimized micro-interaction animation for 60fps
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = OptimizedAnimations.performantSpring(
            dampingRatio = 0.8f, // Higher damping for stability
            stiffness = 500f,    // Slightly higher stiffness for snappier feel
            visibilityThreshold = 0.001f
        ),
        label = "optimized_primary_button_scale"
    )
    
    // Subtask 81: Primary Button (Tomato)
    // Subtask 181: Optimized color animations
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) // Charcoal at 10% alpha
            else -> MaterialTheme.colorScheme.primary // Tomato color
        },
        animationSpec = OptimizedAnimations.performantTween<Color>(
            baseDurationMs = 200
        ),
        label = "optimized_primary_button_background"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> Color.White
        },
        animationSpec = OptimizedAnimations.performantTween<Color>(
            baseDurationMs = 200
        ),
        label = "optimized_primary_button_content"
    )
    
    Button(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = when (size) {
            ButtonSize.SMALL -> AppShapes.SmallShape
            ButtonSize.MEDIUM -> AppShapes.MediumShape
            ButtonSize.LARGE -> AppShapes.LargeShape
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = contentColor
        ),
        contentPadding = when (size) {
            ButtonSize.SMALL -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ButtonSize.MEDIUM -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ButtonSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        },
        interactionSource = interactionSource
    ) {
        ButtonContent(
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            size = size,
            content = content
        )
    }
}

/**
 * Subtask 82: Secondary Button (Basil Outline)
 * Outlined button with Basil color scheme
 */
@Composable
fun EnhancedSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "secondary_button_scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) // Charcoal at 10% alpha
            else -> MaterialTheme.colorScheme.secondary // Basil color
        },
        animationSpec = tween(200),
        label = "secondary_button_border"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.secondary // Basil color
        },
        animationSpec = tween(200),
        label = "secondary_button_content"
    )
    
    OutlinedButton(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = when (size) {
            ButtonSize.SMALL -> AppShapes.SmallShape
            ButtonSize.MEDIUM -> AppShapes.MediumShape
            ButtonSize.LARGE -> AppShapes.LargeShape
        },
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        contentPadding = when (size) {
            ButtonSize.SMALL -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ButtonSize.MEDIUM -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ButtonSize.LARGE -> PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        },
        interactionSource = interactionSource
    ) {
        ButtonContent(
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            size = size,
            content = content
        )
    }
}

/**
 * Text button variant for subtle actions
 */
@Composable
fun EnhancedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "text_button_scale"
    )
    
    // Subtask 83: Disabled States
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(200),
        label = "text_button_content"
    )
    
    TextButton(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = when (size) {
            ButtonSize.SMALL -> AppShapes.SmallShape
            ButtonSize.MEDIUM -> AppShapes.MediumShape
            ButtonSize.LARGE -> AppShapes.LargeShape
        },
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor
        ),
        contentPadding = when (size) {
            ButtonSize.SMALL -> PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ButtonSize.MEDIUM -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ButtonSize.LARGE -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        },
        interactionSource = interactionSource
    ) {
        ButtonContent(
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            size = size,
            content = content
        )
    }
}

/**
 * Floating action button with design system styling
 */
@Composable
fun EnhancedFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM,
    icon: ImageVector,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 500f
        ),
        label = "fab_scale_animation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(200),
        label = "fab_background"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> Color.White
        },
        animationSpec = tween(200),
        label = "fab_content"
    )
    
    val fabSize = when (size) {
        ButtonSize.SMALL -> 40.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 72.dp
    }
    
    FloatingActionButton(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .size(fabSize)
            .scale(scale),
        shape = CircleShape,
        containerColor = backgroundColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(
                when (size) {
                    ButtonSize.SMALL -> 18.dp
                    ButtonSize.MEDIUM -> 24.dp
                    ButtonSize.LARGE -> 32.dp
                }
            )
        )
    }
}

/**
 * Icon button with design system styling
 */
@Composable
fun EnhancedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.MEDIUM,
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 600f
        ),
        label = "icon_button_scale"
    )
    
    val iconTint by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> tint
        },
        animationSpec = tween(200),
        label = "icon_button_tint"
    )
    
    val buttonSize = when (size) {
        ButtonSize.SMALL -> 32.dp
        ButtonSize.MEDIUM -> 40.dp
        ButtonSize.LARGE -> 48.dp
    }
    
    IconButton(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .size(buttonSize)
            .scale(scale),
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(
                when (size) {
                    ButtonSize.SMALL -> 16.dp
                    ButtonSize.MEDIUM -> 20.dp
                    ButtonSize.LARGE -> 24.dp
                }
            )
        )
    }
}

/**
 * Helper composable for button content layout
 */
@Composable
private fun RowScope.ButtonContent(
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    size: ButtonSize,
    content: @Composable RowScope.() -> Unit
) {
    val iconSize = when (size) {
        ButtonSize.SMALL -> 16.dp
        ButtonSize.MEDIUM -> 18.dp
        ButtonSize.LARGE -> 20.dp
    }
    
    val spacing = when (size) {
        ButtonSize.SMALL -> 6.dp
        ButtonSize.MEDIUM -> 8.dp
        ButtonSize.LARGE -> 10.dp
    }
    
    leadingIcon?.let { icon ->
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(spacing))
    }
    
    content()
    
    trailingIcon?.let { icon ->
        Spacer(modifier = Modifier.width(spacing))
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Convenience composables for common button text styles
 */
@Composable
fun ButtonText(
    text: String,
    size: ButtonSize = ButtonSize.MEDIUM,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    Text(
        text = text,
        style = when (size) {
            ButtonSize.SMALL -> MaterialTheme.typography.labelMedium
            ButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
            ButtonSize.LARGE -> MaterialTheme.typography.titleSmall
        }.copy(
            fontWeight = fontWeight,
            fontSize = when (size) {
                ButtonSize.SMALL -> 12.sp
                ButtonSize.MEDIUM -> 14.sp
                ButtonSize.LARGE -> 16.sp
            }
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Preview composables for the button system
 */
@Preview(name = "Enhanced Buttons - Primary")
@Composable
private fun EnhancedPrimaryButtonPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedPrimaryButton(
                    onClick = {},
                    size = ButtonSize.SMALL
                ) {
                    ButtonText("Small", ButtonSize.SMALL)
                }
                
                EnhancedPrimaryButton(
                    onClick = {},
                    size = ButtonSize.MEDIUM,
                    leadingIcon = Icons.Default.Add
                ) {
                    ButtonText("Add Recipe", ButtonSize.MEDIUM)
                }
                
                EnhancedPrimaryButton(
                    onClick = {},
                    size = ButtonSize.LARGE,
                    trailingIcon = Icons.Default.ArrowForward
                ) {
                    ButtonText("Get Started", ButtonSize.LARGE)
                }
                
                EnhancedPrimaryButton(
                    onClick = {},
                    enabled = false
                ) {
                    ButtonText("Disabled", ButtonSize.MEDIUM)
                }
            }
        }
    }
}

@Preview(name = "Enhanced Buttons - Secondary")
@Composable
private fun EnhancedSecondaryButtonPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedSecondaryButton(
                    onClick = {},
                    size = ButtonSize.SMALL
                ) {
                    ButtonText("Small", ButtonSize.SMALL)
                }
                
                EnhancedSecondaryButton(
                    onClick = {},
                    size = ButtonSize.MEDIUM,
                    leadingIcon = Icons.Default.Favorite
                ) {
                    ButtonText("Save Recipe", ButtonSize.MEDIUM)
                }
                
                EnhancedSecondaryButton(
                    onClick = {},
                    size = ButtonSize.LARGE
                ) {
                    ButtonText("Browse Recipes", ButtonSize.LARGE)
                }
                
                EnhancedSecondaryButton(
                    onClick = {},
                    enabled = false
                ) {
                    ButtonText("Disabled", ButtonSize.MEDIUM)
                }
            }
        }
    }
}

@Preview(name = "Enhanced Buttons - All Types")
@Composable
private fun EnhancedButtonTypesPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Primary Buttons", style = MaterialTheme.typography.titleSmall)
                EnhancedPrimaryButton(onClick = {}) {
                    ButtonText("Primary")
                }
                
                Text("Secondary Buttons", style = MaterialTheme.typography.titleSmall)
                EnhancedSecondaryButton(onClick = {}) {
                    ButtonText("Secondary")
                }
                
                Text("Text Buttons", style = MaterialTheme.typography.titleSmall)
                EnhancedTextButton(onClick = {}) {
                    ButtonText("Text Button")
                }
                
                Text("Action Buttons", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EnhancedFAB(
                        onClick = {},
                        icon = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                    
                    EnhancedIconButton(
                        onClick = {},
                        icon = Icons.Default.Favorite,
                        contentDescription = "Like"
                    )
                    
                    EnhancedIconButton(
                        onClick = {},
                        icon = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Preview(name = "Enhanced Buttons - Dark Theme")
@Composable
private fun EnhancedButtonDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedPrimaryButton(onClick = {}) {
                    ButtonText("Primary")
                }
                
                EnhancedSecondaryButton(onClick = {}) {
                    ButtonText("Secondary")
                }
                
                EnhancedTextButton(onClick = {}) {
                    ButtonText("Text Button")
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedFAB(
                        onClick = {},
                        icon = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                    
                    EnhancedIconButton(
                        onClick = {},
                        icon = Icons.Default.Favorite,
                        contentDescription = "Like"
                    )
                }
            }
        }
    }
} 