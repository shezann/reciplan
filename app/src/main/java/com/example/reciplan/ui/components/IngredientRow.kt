package com.example.reciplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reciplan.ui.theme.*

/**
 * Data class representing an ingredient item
 */
data class IngredientData(
    val id: String,
    val name: String,
    val amount: String? = null,
    val unit: String? = null,
    val isChecked: Boolean = false
) {
    val displayText: String
        get() = buildString {
            if (!amount.isNullOrEmpty()) {
                append(amount)
                if (!unit.isNullOrEmpty()) {
                    append(" $unit")
                }
                append(" ")
            }
            append(name)
        }
}

/**
 * Interactive ingredient checklist component with strike-through animation
 * 
 * Features:
 * - Custom checkbox styling with consistent theme (Subtask 51)
 * - Dynamic strike-through animation when checked (Subtask 52)
 * - Subtle color fade transition to indicate completion (Subtask 53)
 * 
 * @param ingredient The ingredient data to display
 * @param onCheckedChange Callback when checkbox state changes
 * @param modifier Modifier for styling
 * @param enabled Whether the ingredient row is enabled for interaction
 * @param animationDuration Duration for animations in milliseconds
 */
@Composable
fun IngredientRow(
    ingredient: IngredientData,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animationDuration: Int = 400
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    // Subtask 53: Color Fade Transition
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ingredient.isChecked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "text_color_animation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            ingredient.isChecked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "background_color_animation"
    )
    
    // Subtask 52: Strike-through Animation
    val strikethroughProgress by animateFloatAsState(
        targetValue = if (ingredient.isChecked) 1f else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "strikethrough_animation"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.SmallShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Use Material 3 default ripple
                enabled = enabled
            ) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!ingredient.isChecked)
            }
            .semantics {
                role = Role.Checkbox
                stateDescription = if (ingredient.isChecked) "Checked" else "Unchecked"
                contentDescription = "${ingredient.displayText}, ${if (ingredient.isChecked) "completed" else "not completed"}"
            },
        color = backgroundColor,
        shape = AppShapes.SmallShape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtask 51: Custom Checkbox Styling
            CustomCheckbox(
                checked = ingredient.isChecked,
                onCheckedChange = null, // Handled by parent click
                enabled = enabled,
                animationDuration = animationDuration
            )
            
            // Ingredient text with strike-through animation
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ingredient.displayText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (ingredient.isChecked) FontWeight.Normal else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Subtask 52: Strike-through line animation
                if (strikethroughProgress > 0f) {
                    StrikethroughLine(
                        progress = strikethroughProgress,
                        color = textColor,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}

/**
 * Subtask 51: Custom Checkbox Styling
 * Custom checkbox component with design system styling
 */
@Composable
private fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animationDuration: Int = 300
) {
    val checkboxScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "checkbox_scale_animation"
    )
    
    val checkmarkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 600f
        ),
        label = "checkmark_scale_animation"
    )
    
    val checkboxColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
            checked -> MaterialTheme.colorScheme.primary // Tomato color
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "checkbox_color_animation"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            checked -> checkboxColor
            else -> Color.Transparent
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "checkbox_background_animation"
    )
    
    Box(
        modifier = modifier
            .size(24.dp)
            .scale(checkboxScale),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(20.dp),
            shape = RoundedCornerShape(4.dp),
            color = backgroundColor,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = checkboxColor
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(12.dp)
                            .scale(checkmarkScale)
                    )
                }
            }
        }
    }
}

/**
 * Subtask 52: Strike-through Animation
 * Custom strike-through line that animates across text
 */
@Composable
private fun StrikethroughLine(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Canvas(
        modifier = modifier
    ) {
        if (progress > 0f) {
            val strokeWidth = with(density) { 1.5.dp.toPx() }
            val startX = 0f
            val endX = size.width * progress
            val lineY = size.height * 0.5f
            
            drawLine(
                color = color,
                start = Offset(startX, lineY),
                end = Offset(endX, lineY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Bulk ingredient list component for recipe screens
 */
@Composable
fun IngredientList(
    ingredients: List<IngredientData>,
    onIngredientChecked: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showProgress: Boolean = true
) {
    val checkedCount = ingredients.count { it.isChecked }
    val totalCount = ingredients.size
    val progressPercentage = if (totalCount > 0) (checkedCount.toFloat() / totalCount) else 0f
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Progress indicator (optional)
        if (showProgress && totalCount > 0) {
            IngredientProgress(
                checkedCount = checkedCount,
                totalCount = totalCount,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Ingredient rows
        ingredients.forEach { ingredient ->
            IngredientRow(
                ingredient = ingredient,
                onCheckedChange = { checked ->
                    onIngredientChecked(ingredient.id, checked)
                },
                enabled = enabled
            )
        }
    }
}

/**
 * Progress indicator for ingredient completion
 */
@Composable
private fun IngredientProgress(
    checkedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) (checkedCount.toFloat() / totalCount) else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "progress_animation"
    )
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "$checkedCount of $totalCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Preview composables for the IngredientRow component
 */
@Preview(name = "Ingredient Row - Unchecked")
@Composable
private fun IngredientRowPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IngredientRow(
                    ingredient = IngredientData(
                        id = "1",
                        name = "Fresh basil leaves",
                        amount = "1/4",
                        unit = "cup"
                    ),
                    onCheckedChange = {}
                )
            }
        }
    }
}

@Preview(name = "Ingredient Row - Checked")
@Composable
private fun IngredientRowCheckedPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IngredientRow(
                    ingredient = IngredientData(
                        id = "1",
                        name = "Tomatoes, diced",
                        amount = "2",
                        unit = "large",
                        isChecked = true
                    ),
                    onCheckedChange = {}
                )
            }
        }
    }
}

@Preview(name = "Ingredient List - Mixed States")
@Composable
private fun IngredientListPreview() {
    ReciplanTheme {
        Surface {
            IngredientList(
                ingredients = listOf(
                    IngredientData("1", "Pasta", "1", "lb"),
                    IngredientData("2", "Olive oil", "2", "tbsp", isChecked = true),
                    IngredientData("3", "Garlic, minced", "3", "cloves", isChecked = true),
                    IngredientData("4", "Fresh tomatoes, chopped", "4", "large"),
                    IngredientData("5", "Fresh basil leaves", "1/4", "cup"),
                    IngredientData("6", "Parmesan cheese, grated", "1/2", "cup")
                ),
                onIngredientChecked = { _, _ -> },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Ingredient Row - Dark Theme")
@Composable
private fun IngredientRowDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IngredientRow(
                    ingredient = IngredientData(
                        id = "1",
                        name = "Fresh mozzarella cheese",
                        amount = "8",
                        unit = "oz"
                    ),
                    onCheckedChange = {}
                )
                
                IngredientRow(
                    ingredient = IngredientData(
                        id = "2",
                        name = "Salt and pepper to taste",
                        isChecked = true
                    ),
                    onCheckedChange = {}
                )
            }
        }
    }
}

@Preview(name = "Custom Checkbox - All States")
@Composable
private fun CustomCheckboxPreview() {
    ReciplanTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CustomCheckbox(
                        checked = false,
                        onCheckedChange = null
                    )
                    Text("Unchecked", style = MaterialTheme.typography.labelSmall)
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CustomCheckbox(
                        checked = true,
                        onCheckedChange = null
                    )
                    Text("Checked", style = MaterialTheme.typography.labelSmall)
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CustomCheckbox(
                        checked = false,
                        onCheckedChange = null,
                        enabled = false
                    )
                    Text("Disabled", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
} 