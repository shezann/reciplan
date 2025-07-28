package com.example.reciplan.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Design System Preview - showcases all design tokens
 * Use this to validate the new theme system
 */
@Composable
fun DesignSystemPreview() {
    ReciplanTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Reciplan Design System",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item { ColorPaletteSection() }
            item { TypographySection() }
            item { ShapeSection() }
            item { ComponentSection() }
        }
    }
}

@Composable
private fun ColorPaletteSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Primary colors (Tomato)
        ColorSwatch(
            name = "Primary (Tomato)",
            color = MaterialTheme.colorScheme.primary,
            onColor = MaterialTheme.colorScheme.onPrimary
        )
        
        // Secondary colors (Basil)
        ColorSwatch(
            name = "Secondary (Basil)",
            color = MaterialTheme.colorScheme.secondary,
            onColor = MaterialTheme.colorScheme.onSecondary
        )
        
        // Background (Cream)
        ColorSwatch(
            name = "Background (Cream)",
            color = MaterialTheme.colorScheme.background,
            onColor = MaterialTheme.colorScheme.onBackground
        )
        
        // Surface
        ColorSwatch(
            name = "Surface",
            color = MaterialTheme.colorScheme.surface,
            onColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: Color,
    onColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        color = color,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = name,
                color = onColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TypographySection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Typography",
            style = MaterialTheme.typography.headlineMedium
        )
        
        val typographyStyles = listOf(
            "Display Large" to MaterialTheme.typography.displayLarge,
            "Headline Large" to MaterialTheme.typography.headlineLarge,
            "Headline Medium" to MaterialTheme.typography.headlineMedium,
            "Title Large" to MaterialTheme.typography.titleLarge,
            "Body Large" to MaterialTheme.typography.bodyLarge,
            "Body Medium" to MaterialTheme.typography.bodyMedium,
            "Label Large" to MaterialTheme.typography.labelLarge
        )
        
        typographyStyles.forEach { (name, style) ->
            Text(
                text = "$name - The quick brown fox",
                style = style,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ShapeSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Shapes",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShapeExample("Small (8dp)", MaterialTheme.shapes.small)
            ShapeExample("Medium (16dp)", MaterialTheme.shapes.medium)
            ShapeExample("Large (24dp)", MaterialTheme.shapes.large)
        }
    }
}

@Composable
private fun RowScope.ShapeExample(name: String, shape: androidx.compose.ui.graphics.Shape) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(80.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = shape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ComponentSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Components",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {}) {
                Text("Primary")
            }
            OutlinedButton(onClick = {}) {
                Text("Secondary")
            }
        }
        
        // Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recipe Card Preview",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "This is how cards will look with the new design system",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Design System - Light")
@Composable
private fun DesignSystemPreviewLight() {
    DesignSystemPreview()
}

@Preview(name = "Design System - Dark")
@Composable
private fun DesignSystemPreviewDark() {
    ReciplanTheme(darkTheme = true) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DesignSystemPreview()
        }
    }
} 