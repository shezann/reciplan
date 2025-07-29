package com.example.reciplan.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Food-centric color palette for Reciplan
 * Inspired by warm kitchen colors: Tomato, Basil, Cream, and Charcoal
 */
object AppColors {
    // Primary Colors - Green (accent/action color)
    val Tomato = Color(0xFF497D49)
    val TomatoLight = Color(0xFF5A8F5A)
    val TomatoDark = Color(0xFF3D6B3D)
    
    // Secondary Colors - Sage (complementary to green)
    val Basil = Color(0xFF8FBC8F)
    val BasilLight = Color(0xFFA5C9A5)
    val BasilDark = Color(0xFF7A9F7A)
    
    // Background Colors - Soft gradient backgrounds
    val Cream = Color(0xFFF8FBF8)  // Very light mint green
    val CreamLight = Color(0xFFFAFDFA)  // Lighter mint green
    val CreamDark = Color(0xFF1A1F1A)  // Dark green-tinted background
    
    // Text Colors - Green-tinted charcoal (high contrast text)
    val Charcoal = Color(0xFF2A3F2A)  // Green-tinted dark text
    val CharcoalLight = Color(0xFF4A5F4A)  // Green-tinted medium text
    val CharcoalDark = Color(0xFFE8F0E8)  // Green-tinted light text
    
    // Surface Colors
    val SurfaceLight = Color(0xFFFFFF)
    val SurfaceDark = Color(0xFF2A2F2A)
    val SurfaceVariantLight = Color(0xFFF0F5F0)  // Very light green tint
    val SurfaceVariantDark = Color(0xFF3A3F3A)  // Dark green tint
    
    // Outline Colors
    val OutlineLight = Color(0xFF7A8F7A)  // Green-tinted outline
    val OutlineDark = Color(0xFF8A9F8A)  // Lighter green outline
    val OutlineVariantLight = Color(0xFFB0C5B0)  // Very light green outline
    val OutlineVariantDark = Color(0xFF5A6F5A)  // Darker green outline
    
    // Error Colors (keeping food theme with deeper red)
    val ErrorLight = Color(0xFFBA1A1A)
    val ErrorDark = Color(0xFFFFB4AB)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val ErrorContainerDark = Color(0xFF93000A)
    
    // Success Colors (using Green variants)
    val SuccessLight = Tomato
    val SuccessDark = TomatoLight
    val SuccessContainerLight = Color(0xFFE8F5E8)
    val SuccessContainerDark = Color(0xFF1A3D1A)
    
    // Warning Colors (using warm orange)
    val WarningLight = Color(0xFFB8860B)
    val WarningDark = Color(0xFFDDAA00)
    val WarningContainerLight = Color(0xFFF3E5AB)
    val WarningContainerDark = Color(0xFF3D2D00)
}

/**
 * Light theme color scheme using food-centric palette
 */
val LightColorScheme = lightColorScheme(
    // Primary colors (Tomato)
    primary = AppColors.Tomato,
    onPrimary = Color.White,
    primaryContainer = AppColors.TomatoLight,
    onPrimaryContainer = AppColors.TomatoDark,
    
    // Secondary colors (Basil)
    secondary = AppColors.Basil,
    onSecondary = Color.White,
    secondaryContainer = AppColors.SuccessContainerLight,
    onSecondaryContainer = AppColors.BasilDark,
    
    // Tertiary colors (warm accent)
    tertiary = AppColors.WarningLight,
    onTertiary = Color.White,
    tertiaryContainer = AppColors.WarningContainerLight,
    onTertiaryContainer = AppColors.WarningLight,
    
    // Error colors
    error = AppColors.ErrorLight,
    onError = Color.White,
    errorContainer = AppColors.ErrorContainerLight,
    onErrorContainer = AppColors.ErrorLight,
    
    // Background colors (Cream)
    background = AppColors.Cream,
    onBackground = AppColors.Charcoal,
    
    // Surface colors
    surface = AppColors.SurfaceLight,
    onSurface = AppColors.Charcoal,
    surfaceVariant = AppColors.SurfaceVariantLight,
    onSurfaceVariant = AppColors.CharcoalLight,
    surfaceTint = AppColors.Tomato,
    
    // Outline colors
    outline = AppColors.OutlineLight,
    outlineVariant = AppColors.OutlineVariantLight,
    
    // Other colors
    scrim = Color.Black,
    inverseSurface = AppColors.Charcoal,
    inverseOnSurface = AppColors.Cream,
    inversePrimary = AppColors.TomatoLight
)

/**
 * Dark theme color scheme using food-centric palette
 * Maintains warmth while providing proper contrast
 */
val DarkColorScheme = darkColorScheme(
    // Primary colors (Tomato - adjusted for dark theme)
    primary = AppColors.TomatoLight,
    onPrimary = Color.Black,
    primaryContainer = AppColors.TomatoDark,
    onPrimaryContainer = AppColors.TomatoLight,
    
    // Secondary colors (Basil - adjusted for dark theme)
    secondary = AppColors.BasilLight,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.SuccessContainerDark,
    onSecondaryContainer = AppColors.BasilLight,
    
    // Tertiary colors (warm accent - adjusted)
    tertiary = AppColors.WarningDark,
    onTertiary = Color.Black,
    tertiaryContainer = AppColors.WarningContainerDark,
    onTertiaryContainer = AppColors.WarningDark,
    
    // Error colors
    error = AppColors.ErrorDark,
    onError = Color.Black,
    errorContainer = AppColors.ErrorContainerDark,
    onErrorContainer = AppColors.ErrorDark,
    
    // Background colors (Dark Cream)
    background = AppColors.CreamDark,
    onBackground = AppColors.CharcoalDark,
    
    // Surface colors
    surface = AppColors.SurfaceDark,
    onSurface = AppColors.CharcoalDark,
    surfaceVariant = AppColors.SurfaceVariantDark,
    onSurfaceVariant = AppColors.CharcoalLight,
    surfaceTint = AppColors.TomatoLight,
    
    // Outline colors
    outline = AppColors.OutlineDark,
    outlineVariant = AppColors.OutlineVariantDark,
    
    // Other colors
    scrim = Color.Black,
    inverseSurface = AppColors.CharcoalDark,
    inverseOnSurface = AppColors.CreamDark,
    inversePrimary = AppColors.Tomato
) 