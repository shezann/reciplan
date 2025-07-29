package com.example.reciplan.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape language for Reciplan UI
 * Defines consistent corner radius values following 4pt grid system
 */
object AppShapes {
    // Corner radius values following 4pt grid
    val ExtraSmall = 4.dp    // Small elements: chips, small buttons
    val Small = 8.dp         // Medium elements: input fields, regular buttons
    val Medium = 12.dp       // Cards, dialogs
    val Large = 16.dp        // Main cards, containers
    val ExtraLarge = 24.dp   // Large containers, bottom sheets
    
    // Specific shape definitions
    val ExtraSmallShape = RoundedCornerShape(ExtraSmall)
    val SmallShape = RoundedCornerShape(Small)
    val MediumShape = RoundedCornerShape(Medium)
    val LargeShape = RoundedCornerShape(Large)
    val ExtraLargeShape = RoundedCornerShape(ExtraLarge)
    
    // Circular shape for avatars, FABs, etc.
    val CircularShape = CircleShape
    
    // Asymmetric shapes for specific use cases
    val TopRoundedShape = RoundedCornerShape(
        topStart = Large,
        topEnd = Large,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    val BottomRoundedShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = Large,
        bottomEnd = Large
    )
    
    // Recipe card specific shapes
    val RecipeCardShape = RoundedCornerShape(Large) // 16dp for recipe cards
    val RecipeCardImageShape = RoundedCornerShape(
        topStart = Large,
        topEnd = Large,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Button shapes
    val PrimaryButtonShape = RoundedCornerShape(Small) // 8dp for buttons
    val SecondaryButtonShape = RoundedCornerShape(Small)
    val ChipShape = RoundedCornerShape(ExtraLarge) // Fully rounded for chips
    
    // Input field shapes
    val InputFieldShape = RoundedCornerShape(Small) // 8dp for inputs
    val SearchFieldShape = RoundedCornerShape(ExtraLarge) // Fully rounded search
    
    // Dialog and modal shapes
    val DialogShape = RoundedCornerShape(Medium) // 12dp for dialogs
    val BottomSheetShape = RoundedCornerShape(
        topStart = ExtraLarge,
        topEnd = ExtraLarge,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Stepper component shapes
    val StepperCircleShape = CircleShape
    val StepperConnectorShape = RoundedCornerShape(2.dp) // Thin rounded rectangle
    
    // Like button chip shape
    val LikeChipShape = RoundedCornerShape(ExtraLarge) // Fully rounded
    
    // Empty state illustration container
    val EmptyStateShape = RoundedCornerShape(Medium)
}

/**
 * Material 3 Shapes configuration for Reciplan
 * Maps to the three primary shape categories in Material Design
 */
val AppShapeScheme = Shapes(
    // Small components: chips, buttons, text fields
    small = AppShapes.SmallShape,
    
    // Medium components: cards, dialogs
    medium = AppShapes.LargeShape, // Using 16dp for cards
    
    // Large components: bottom sheets, side sheets
    large = AppShapes.ExtraLargeShape
)

/**
 * Extended shapes for specific Reciplan components
 * Use these for consistent styling across the app
 */
object RecipeShapes {
    // Main recipe card (used in feeds, grids)
    val Card = AppShapes.RecipeCardShape
    
    // Recipe card image (rounded top only)
    val CardImage = AppShapes.RecipeCardImageShape
    
    // Recipe detail image gallery
    val DetailImage = RoundedCornerShape(AppShapes.Medium)
    
    // Ingredient item background
    val IngredientItem = RoundedCornerShape(AppShapes.Small)
    
    // Step number circle
    val StepCircle = AppShapes.CircularShape
    
    // Tags and difficulty indicators
    val Tag = RoundedCornerShape(AppShapes.ExtraSmall)
    val DifficultyChip = RoundedCornerShape(AppShapes.Small)
}

/**
 * Interactive component shapes
 */
object InteractionShapes {
    // Buttons
    val PrimaryButton = AppShapes.PrimaryButtonShape
    val SecondaryButton = AppShapes.SecondaryButtonShape
    val FAB = AppShapes.CircularShape
    
    // Input fields
    val TextField = AppShapes.InputFieldShape
    val SearchField = AppShapes.SearchFieldShape
    
    // Feedback elements
    val Chip = AppShapes.ChipShape
    val Badge = AppShapes.CircularShape
    val Tooltip = RoundedCornerShape(AppShapes.ExtraSmall)
}

/**
 * Layout component shapes
 */
object LayoutShapes {
    // Containers
    val Surface = AppShapes.MediumShape
    val Card = AppShapes.LargeShape
    val Dialog = AppShapes.DialogShape
    
    // Navigation
    val BottomSheet = AppShapes.BottomSheetShape
    val NavigationBar = RoundedCornerShape(0.dp) // Sharp edges for nav
    val Tab = RoundedCornerShape(AppShapes.Medium)
    
    // Empty states
    val EmptyStateContainer = AppShapes.EmptyStateShape
    val IllustrationBackground = AppShapes.MediumShape
} 