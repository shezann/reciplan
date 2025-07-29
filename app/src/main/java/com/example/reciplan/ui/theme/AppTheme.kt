package com.example.reciplan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Main theme composable for Reciplan
 * Integrates food-centric colors, typography, and shapes into a cohesive design system
 *
 * @param darkTheme Whether to use dark theme colors
 * @param dynamicColor Whether to use dynamic colors (Android 12+)
 * @param content The content to be themed
 */
@Composable
fun ReciplanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to maintain food-centric branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color support for Android 12+ (optional)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        
        // Use our custom food-centric color schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Update system bars to match our theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Set status bar color to match background
            window.statusBarColor = colorScheme.background.toArgb()
            
            // Set navigation bar color to match surface
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            // Adjust system bar icon colors based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapeScheme,
        content = content
    )
}

/**
 * Preview-friendly theme composable for development
 * Forces light theme for consistent preview rendering
 */
@Composable
fun ReciplanThemePreview(
    content: @Composable () -> Unit
) {
    ReciplanTheme(
        darkTheme = false,
        dynamicColor = false,
        content = content
    )
}

/**
 * Dark theme variant for previews
 */
@Composable
fun ReciplanThemeDarkPreview(
    content: @Composable () -> Unit
) {
    ReciplanTheme(
        darkTheme = true,
        dynamicColor = false,
        content = content
    )
}

/**
 * Extension properties to access our custom design tokens
 * Use these throughout the app for consistency
 */
object ReciplanThemeTokens {
    /**
     * Access to Reciplan-specific colors beyond Material 3 scheme
     */
    @Composable
    fun colors() = AppColors
    
    /**
     * Access to extended typography styles
     */
    @Composable
    fun typography() = AppTypographyExtended
    
    /**
     * Access to component-specific shapes
     */
    @Composable
    fun shapes() = AppShapes
    
    /**
     * Access to recipe-specific shapes
     */
    @Composable
    fun recipeShapes() = RecipeShapes
    
    /**
     * Access to interaction shapes
     */
    @Composable
    fun interactionShapes() = InteractionShapes
    
    /**
     * Access to layout shapes
     */
    @Composable
    fun layoutShapes() = LayoutShapes
}

/**
 * Convenience extensions for commonly used theme tokens
 */
object ThemeExtensions {
    /**
     * Get the appropriate Tomato color for current theme
     */
    @Composable
    fun tomatoColor() = MaterialTheme.colorScheme.primary
    
    /**
     * Get the appropriate Basil color for current theme
     */
    @Composable
    fun basilColor() = MaterialTheme.colorScheme.secondary
    
    /**
     * Get recipe card title style with proper contrast
     */
    @Composable
    fun recipeCardTitleStyle() = AppTypographyExtended.recipeCardTitle
    
    /**
     * Get recipe metadata style
     */
    @Composable
    fun recipeMetadataStyle() = AppTypographyExtended.recipeMetadata
    
    /**
     * Get ingredient text style
     */
    @Composable
    fun ingredientTextStyle() = AppTypographyExtended.ingredientText
    
    /**
     * Get instruction text style
     */
    @Composable
    fun instructionTextStyle() = AppTypographyExtended.instructionText
} 