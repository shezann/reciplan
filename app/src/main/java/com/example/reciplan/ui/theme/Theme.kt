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
    primary = Teal80,
    onPrimary = Color.Black,
    primaryContainer = Teal40,
    onPrimaryContainer = Teal80,
    secondary = TealGrey80,
    onSecondary = Color.Black,
    secondaryContainer = TealGrey40,
    onSecondaryContainer = TealGrey80,
    tertiary = Green80,
    onTertiary = Color.Black,
    tertiaryContainer = Green40,
    onTertiaryContainer = Green80,
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal80,
    onPrimaryContainer = Teal40,
    secondary = TealGrey40,
    onSecondary = Color.White,
    secondaryContainer = TealGrey80,
    onSecondaryContainer = TealGrey40,
    tertiary = Green40,
    onTertiary = Color.White,
    tertiaryContainer = Green80,
    onTertiaryContainer = Green40,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C7)
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