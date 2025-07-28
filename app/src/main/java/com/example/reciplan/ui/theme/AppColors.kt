package com.example.reciplan.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Food-centric color palette for Reciplan
 * Inspired by warm kitchen colors: Tomato, Basil, Cream, and Charcoal
 */
object AppColors {
    // Primary Colors - Tomato (accent/action color)
    val Tomato = Color(0xFFFF5A5F)
    val TomatoLight = Color(0xFFFF7B7F)
    val TomatoDark = Color(0xFFE8524F)
    
    // Secondary Colors - Basil (success/positive actions)
    val Basil = Color(0xFF34C759)
    val BasilLight = Color(0xFF48D167)
    val BasilDark = Color(0xFF2E8B45)
    
    // Background Colors - Cream (warm neutral backgrounds)
    val Cream = Color(0xFFFFF8F2)
    val CreamLight = Color(0xFFFFFBF8)
    val CreamDark = Color(0xFF1A1612)
    
    // Text Colors - Charcoal (high contrast text)
    val Charcoal = Color(0xFF333333)
    val CharcoalLight = Color(0xFF4A4A4A)
    val CharcoalDark = Color(0xFFE8E3DD)
    
    // Surface Colors
    val SurfaceLight = Color(0xFFFFFF)
    val SurfaceDark = Color(0xFF2A2420)
    val SurfaceVariantLight = Color(0xFFF5F0EA)
    val SurfaceVariantDark = Color(0xFF3A342E)
    
    // Outline Colors
    val OutlineLight = Color(0xFF8A7F73)
    val OutlineDark = Color(0xFF9A8F83)
    val OutlineVariantLight = Color(0xFFBBB0A4)
    val OutlineVariantDark = Color(0xFF6A5F53)
    
    // Error Colors (keeping food theme with deeper red)
    val ErrorLight = Color(0xFFBA1A1A)
    val ErrorDark = Color(0xFFFFB4AB)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val ErrorContainerDark = Color(0xFF93000A)
    
    // Success Colors (using Basil variants)
    val SuccessLight = Basil
    val SuccessDark = BasilLight
    val SuccessContainerLight = Color(0xFFD1F2EB)
    val SuccessContainerDark = Color(0xFF0D4F21)
    
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