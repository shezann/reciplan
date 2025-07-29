package com.example.reciplan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reciplan.ui.theme.*

/**
 * Types of empty states with context-specific messaging and illustrations
 */
enum class EmptyStateType {
    NO_RECIPES,           // Empty recipe feed
    NO_SEARCH_RESULTS,    // No search results found
    NO_INGREDIENTS,       // Empty ingredient list  
    NO_DRAFTS,           // No draft recipes
    NO_FAVORITES,        // No liked recipes
    CONNECTION_ERROR,    // Network/connection issues
    PROCESSING_ERROR,    // Failed to process content
    NO_NOTIFICATIONS     // No notifications
}

/**
 * Data class defining empty state content
 */
data class EmptyStateContent(
    val title: String,
    val subtitle: String,
    val illustration: EmptyStateIllustration,
    val primaryAction: ActionButton? = null,
    val secondaryAction: ActionButton? = null
)

/**
 * Action button data for empty states
 */
data class ActionButton(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null
)

/**
 * Food-themed illustration types
 */
enum class EmptyStateIllustration {
    CHEF_HAT,
    RECIPE_BOOK, 
    COOKING_UTENSILS,
    EMPTY_PLATE,
    SEARCH_MAGNIFIER,
    NETWORK_ERROR,
    COOKING_POT
}

/**
 * Engaging empty state component with food-themed illustrations
 * 
 * Features:
 * - Food-themed mini illustrations (Subtask 61)
 * - Encouraging contextual copy (Subtask 62)
 * - Clear call-to-action buttons (Subtask 63)
 * 
 * @param type The type of empty state to display
 * @param modifier Modifier for styling
 * @param customContent Optional custom content override
 * @param onPrimaryAction Callback for primary action
 * @param onSecondaryAction Callback for secondary action
 */
@Composable
fun EmptyState(
    type: EmptyStateType,
    modifier: Modifier = Modifier,
    customContent: EmptyStateContent? = null,
    onPrimaryAction: (() -> Unit)? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val content = customContent ?: getEmptyStateContent(type)
    
    // Subtle animation for the entire empty state
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "empty_state_fade_in"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Subtask 61: Food-themed Illustrations
        FoodIllustration(
            illustration = content.illustration,
            modifier = Modifier.size(120.dp)
        )
        
        // Subtask 62: Encouraging Copy System
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = content.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        
        // Subtask 63: Call-to-Action Integration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary action button
            content.primaryAction?.let { action ->
                Button(
                    onClick = onPrimaryAction ?: action.onClick,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = AppShapes.MediumShape
                ) {
                    action.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = action.text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            
            // Secondary action button (if provided)
            content.secondaryAction?.let { action ->
                OutlinedButton(
                    onClick = onSecondaryAction ?: action.onClick,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    shape = AppShapes.MediumShape
                ) {
                    action.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = action.text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * Subtask 61: Food-themed Illustrations
 * Creates simple, flat line-art food illustrations
 */
@Composable
private fun FoodIllustration(
    illustration: EmptyStateIllustration,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    
    // Subtle floating animation
    val offsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_animation"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offsetY.dp)
        ) {
            when (illustration) {
                EmptyStateIllustration.CHEF_HAT -> drawChefHat(primaryColor, outlineColor)
                EmptyStateIllustration.RECIPE_BOOK -> drawRecipeBook(primaryColor, secondaryColor, outlineColor)
                EmptyStateIllustration.COOKING_UTENSILS -> drawCookingUtensils(primaryColor, secondaryColor, outlineColor)
                EmptyStateIllustration.EMPTY_PLATE -> drawEmptyPlate(primaryColor, outlineColor)
                EmptyStateIllustration.SEARCH_MAGNIFIER -> drawSearchMagnifier(primaryColor, outlineColor)
                EmptyStateIllustration.NETWORK_ERROR -> drawNetworkError(primaryColor, outlineColor)
                EmptyStateIllustration.COOKING_POT -> drawCookingPot(primaryColor, secondaryColor, outlineColor)
            }
        }
    }
}

/**
 * Drawing functions for each food-themed illustration
 */
private fun DrawScope.drawChefHat(primaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 3f
    
    // Hat base (rectangle)
    drawRect(
        color = primaryColor,
        topLeft = androidx.compose.ui.geometry.Offset(centerX - radius * 0.8f, centerY + radius * 0.3f),
        size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 0.4f)
    )
    
    // Hat top (circle)
    drawCircle(
        color = primaryColor,
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY - radius * 0.2f)
    )
    
    // Outline
    drawCircle(
        color = outlineColor,
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY - radius * 0.2f),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawRecipeBook(primaryColor: Color, secondaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val width = size.width * 0.6f
    val height = size.height * 0.7f
    
    // Book cover
    drawRect(
        color = primaryColor,
        topLeft = androidx.compose.ui.geometry.Offset(centerX - width/2f, centerY - height/2f),
        size = androidx.compose.ui.geometry.Size(width, height)
    )
    
    // Book spine
    drawRect(
        color = secondaryColor,
        topLeft = androidx.compose.ui.geometry.Offset(centerX - width/2f, centerY - height/2f),
        size = androidx.compose.ui.geometry.Size(width * 0.1f, height)
    )
    
    // Book pages (lines)
    val lineSpacing = height / 6f
    repeat(4) { i ->
        drawLine(
            color = outlineColor,
            start = androidx.compose.ui.geometry.Offset(centerX - width * 0.3f, centerY - height * 0.3f + lineSpacing * i),
            end = androidx.compose.ui.geometry.Offset(centerX + width * 0.3f, centerY - height * 0.3f + lineSpacing * i),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCookingUtensils(primaryColor: Color, secondaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val scale = size.minDimension / 120f
    
    // Fork (left)
    val forkX = centerX - 20f * scale
    drawLine(
        color = primaryColor,
        start = androidx.compose.ui.geometry.Offset(forkX, centerY - 30f * scale),
        end = androidx.compose.ui.geometry.Offset(forkX, centerY + 30f * scale),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
    
    // Fork tines
    repeat(3) { i ->
        drawLine(
            color = primaryColor,
            start = androidx.compose.ui.geometry.Offset(forkX - 8f * scale + i * 8f * scale, centerY - 30f * scale),
            end = androidx.compose.ui.geometry.Offset(forkX - 8f * scale + i * 8f * scale, centerY - 15f * scale),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    
    // Spoon (right)
    val spoonX = centerX + 20f * scale
    drawLine(
        color = secondaryColor,
        start = androidx.compose.ui.geometry.Offset(spoonX, centerY - 10f * scale),
        end = androidx.compose.ui.geometry.Offset(spoonX, centerY + 30f * scale),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
    
    // Spoon bowl
    drawCircle(
        color = secondaryColor,
        radius = 12f * scale,
        center = androidx.compose.ui.geometry.Offset(spoonX, centerY - 20f * scale)
    )
}

private fun DrawScope.drawEmptyPlate(primaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 2.5f
    
    // Plate
    drawCircle(
        color = primaryColor.copy(alpha = 0.3f),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
    
    // Plate rim
    drawCircle(
        color = outlineColor,
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Inner circle
    drawCircle(
        color = outlineColor,
        radius = radius * 0.7f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawSearchMagnifier(primaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 4f
    
    // Magnifier lens
    drawCircle(
        color = primaryColor.copy(alpha = 0.2f),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX - radius * 0.3f, centerY - radius * 0.3f)
    )
    
    drawCircle(
        color = outlineColor,
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
        style = Stroke(width = 4.dp.toPx())
    )
    
    // Handle
    drawLine(
        color = outlineColor,
        start = androidx.compose.ui.geometry.Offset(centerX + radius * 0.4f, centerY + radius * 0.4f),
        end = androidx.compose.ui.geometry.Offset(centerX + radius * 0.9f, centerY + radius * 0.9f),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawNetworkError(primaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 3f
    
    // WiFi-like arcs (disconnected)
    repeat(3) { i ->
        val arcRadius = radius * (0.4f + i * 0.3f)
        val startAngle = 225f
        val sweepAngle = 90f
        
        drawArc(
            color = if (i == 0) outlineColor else outlineColor.copy(alpha = 0.4f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(centerX - arcRadius, centerY - arcRadius),
            size = androidx.compose.ui.geometry.Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    
    // X mark over it
    val xSize = radius * 0.3f
    drawLine(
        color = primaryColor,
        start = androidx.compose.ui.geometry.Offset(centerX - xSize, centerY - xSize),
        end = androidx.compose.ui.geometry.Offset(centerX + xSize, centerY + xSize),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = primaryColor,
        start = androidx.compose.ui.geometry.Offset(centerX + xSize, centerY - xSize),
        end = androidx.compose.ui.geometry.Offset(centerX - xSize, centerY + xSize),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCookingPot(primaryColor: Color, secondaryColor: Color, outlineColor: Color) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val potWidth = size.width * 0.5f
    val potHeight = size.height * 0.4f
    
    // Pot body
    drawRect(
        color = primaryColor,
        topLeft = androidx.compose.ui.geometry.Offset(centerX - potWidth/2f, centerY - potHeight/2f),
        size = androidx.compose.ui.geometry.Size(potWidth, potHeight)
    )
    
    // Pot handles
    drawLine(
        color = outlineColor,
        start = androidx.compose.ui.geometry.Offset(centerX - potWidth/2f - 10f, centerY - potHeight/4f),
        end = androidx.compose.ui.geometry.Offset(centerX - potWidth/2f, centerY - potHeight/4f),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = outlineColor,
        start = androidx.compose.ui.geometry.Offset(centerX + potWidth/2f, centerY - potHeight/4f),
        end = androidx.compose.ui.geometry.Offset(centerX + potWidth/2f + 10f, centerY - potHeight/4f),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    
    // Steam lines
    repeat(3) { i ->
        val steamX = centerX - 15f + i * 15f
        drawLine(
            color = secondaryColor,
            start = androidx.compose.ui.geometry.Offset(steamX, centerY - potHeight/2f - 20f),
            end = androidx.compose.ui.geometry.Offset(steamX, centerY - potHeight/2f - 5f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Subtask 62: Encouraging Copy System
 * Returns contextual empty state content based on type
 */
private fun getEmptyStateContent(type: EmptyStateType): EmptyStateContent {
    return when (type) {
        EmptyStateType.NO_RECIPES -> EmptyStateContent(
            title = "No recipes yet? Let's cook up something amazing!",
            subtitle = "Start your culinary journey by adding your first recipe or discovering new ones from our community.",
            illustration = EmptyStateIllustration.CHEF_HAT,
            primaryAction = ActionButton(
                text = "Add Recipe",
                onClick = {},
                icon = Icons.Default.Add
            ),
            secondaryAction = ActionButton(
                text = "Browse Recipes",
                onClick = {},
                icon = Icons.Default.Search
            )
        )
        
        EmptyStateType.NO_SEARCH_RESULTS -> EmptyStateContent(
            title = "No recipes found",
            subtitle = "Try adjusting your search terms or browse our featured recipes for some delicious inspiration.",
            illustration = EmptyStateIllustration.SEARCH_MAGNIFIER,
            primaryAction = ActionButton(
                text = "Clear Search",
                onClick = {},
                icon = Icons.Default.Clear
            ),
            secondaryAction = ActionButton(
                text = "Browse All Recipes",
                onClick = {},
                icon = Icons.Default.List
            )
        )
        
        EmptyStateType.NO_INGREDIENTS -> EmptyStateContent(
            title = "Missing ingredients?",
            subtitle = "Add some ingredients to start building your recipe. Every great dish starts with quality ingredients!",
            illustration = EmptyStateIllustration.COOKING_UTENSILS,
            primaryAction = ActionButton(
                text = "Add Ingredients",
                onClick = {},
                icon = Icons.Default.Add
            )
        )
        
        EmptyStateType.NO_DRAFTS -> EmptyStateContent(
            title = "No drafts cooking yet!",
            subtitle = "Your saved recipe drafts will appear here. Start creating something delicious!",
            illustration = EmptyStateIllustration.RECIPE_BOOK,
            primaryAction = ActionButton(
                text = "Create Recipe",
                onClick = {},
                icon = Icons.Default.Create
            )
        )
        
        EmptyStateType.NO_FAVORITES -> EmptyStateContent(
            title = "No favorites yet",
            subtitle = "Heart the recipes you love to save them here for quick access later.",
            illustration = EmptyStateIllustration.EMPTY_PLATE,
            primaryAction = ActionButton(
                text = "Discover Recipes",
                onClick = {},
                icon = Icons.Default.Search
            )
        )
        
        EmptyStateType.CONNECTION_ERROR -> EmptyStateContent(
            title = "Connection trouble",
            subtitle = "We're having trouble connecting to our servers. Please check your internet connection and try again.",
            illustration = EmptyStateIllustration.NETWORK_ERROR,
            primaryAction = ActionButton(
                text = "Try Again",
                onClick = {},
                icon = Icons.Default.Refresh
            )
        )
        
        EmptyStateType.PROCESSING_ERROR -> EmptyStateContent(
            title = "Something went wrong",
            subtitle = "We couldn't process your request right now. Don't worry, your recipes are safe!",
            illustration = EmptyStateIllustration.COOKING_POT,
            primaryAction = ActionButton(
                text = "Try Again",
                onClick = {},
                icon = Icons.Default.Refresh
            ),
            secondaryAction = ActionButton(
                text = "Go Back",
                onClick = {},
                icon = Icons.Default.ArrowBack
            )
        )
        
        EmptyStateType.NO_NOTIFICATIONS -> EmptyStateContent(
            title = "All caught up!",
            subtitle = "No new notifications right now. We'll let you know when there's something delicious to share.",
            illustration = EmptyStateIllustration.CHEF_HAT,
            primaryAction = ActionButton(
                text = "Explore Recipes",
                onClick = {},
                icon = Icons.Default.List
            )
        )
    }
}

/**
 * Preview composables for the EmptyState component
 */
@Preview(name = "Empty State - No Recipes")
@Composable
private fun EmptyStateNoRecipesPreview() {
    ReciplanTheme {
        Surface {
            EmptyState(
                type = EmptyStateType.NO_RECIPES,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Empty State - No Search Results")
@Composable
private fun EmptyStateNoSearchPreview() {
    ReciplanTheme {
        Surface {
            EmptyState(
                type = EmptyStateType.NO_SEARCH_RESULTS,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Empty State - Connection Error")
@Composable
private fun EmptyStateConnectionErrorPreview() {
    ReciplanTheme {
        Surface {
            EmptyState(
                type = EmptyStateType.CONNECTION_ERROR,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Empty State - Dark Theme")
@Composable
private fun EmptyStateDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            EmptyState(
                type = EmptyStateType.NO_DRAFTS,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Empty State - Custom Content")
@Composable
private fun EmptyStateCustomPreview() {
    ReciplanTheme {
        Surface {
            EmptyState(
                type = EmptyStateType.NO_RECIPES, // Base type (ignored due to custom content)
                customContent = EmptyStateContent(
                    title = "Welcome to Reciplan!",
                    subtitle = "Your personal recipe companion is ready to help you create amazing dishes.",
                    illustration = EmptyStateIllustration.COOKING_POT,
                    primaryAction = ActionButton(
                        text = "Get Started",
                        onClick = {},
                        icon = Icons.Default.PlayArrow
                    )
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
} 