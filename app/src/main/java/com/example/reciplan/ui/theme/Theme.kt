package com.example.reciplan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Orange80,
    onPrimary = Orange30,
    primaryContainer = Orange30,
    onPrimaryContainer = Orange90,
    secondary = Sage80,
    onSecondary = Sage30,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Brown80,
    onTertiary = Brown30,
    tertiaryContainer = Brown30,
    onTertiaryContainer = Brown90,
    error = Error80,
    onError = Grey20,
    errorContainer = ErrorContainer40,
    onErrorContainer = ErrorContainer80,
    background = Grey10,
    onBackground = Grey90,
    surface = SurfaceDark,
    onSurface = Grey90,
    surfaceVariant = Grey30,
    onSurfaceVariant = Grey80,
    outline = Grey60,
    outlineVariant = Grey30,
    scrim = Grey0,
    inverseSurface = Grey90,
    inverseOnSurface = Grey20,
    inversePrimary = Orange40,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = Orange40,
    onPrimary = Color.White,
    primaryContainer = Orange90,
    onPrimaryContainer = Orange30,
    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage30,
    tertiary = Brown40,
    onTertiary = Color.White,
    tertiaryContainer = Brown90,
    onTertiaryContainer = Brown30,
    error = Error40,
    onError = Color.White,
    errorContainer = ErrorContainer80,
    onErrorContainer = ErrorContainer40,
    background = Grey99,
    onBackground = Grey10,
    surface = SurfaceLight,
    onSurface = Grey10,
    surfaceVariant = Grey90,
    onSurfaceVariant = Grey30,
    outline = Grey50,
    outlineVariant = Grey80,
    scrim = Grey0,
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    inversePrimary = Orange80,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

@Composable
fun ReciplanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 