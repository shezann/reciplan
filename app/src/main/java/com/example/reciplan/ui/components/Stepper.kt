package com.example.reciplan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reciplan.ui.theme.*

/**
 * Represents the status of a step in the stepper
 */
enum class StepStatus {
    PENDING,    // Not started yet
    ACTIVE,     // Currently in progress
    COMPLETED,  // Successfully completed
    ERROR       // Failed with error
}

/**
 * Data class representing a single step in the stepper
 */
data class StepData(
    val id: String,
    val title: String,
    val status: StepStatus = StepStatus.PENDING,
    val description: String? = null
)

/**
 * Horizontal progress stepper component for pipeline status visualization
 * 
 * Features:
 * - Circle step indicators with fill animation (Subtask 41)
 * - Animated connecting lines showing progress (Subtask 42)
 * - Status labels and completion checkmarks (Subtask 43)
 * 
 * @param steps List of step data to display
 * @param modifier Modifier for styling
 * @param showLabels Whether to show step labels below indicators
 * @param animationDuration Duration for step transition animations
 */
@Composable
fun Stepper(
    steps: List<StepData>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    animationDuration: Int = 300
) {
    if (steps.isEmpty()) return
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step indicators and connecting lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Connecting lines (drawn behind step indicators)
            if (steps.size > 1) {
                StepperConnectingLines(
                    steps = steps,
                    animationDuration = animationDuration
                )
            }
            
            // Step indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    StepIndicator(
                        step = step,
                        stepNumber = index + 1,
                        animationDuration = animationDuration,
                        modifier = Modifier.semantics {
                            contentDescription = buildStepContentDescription(step, index + 1, steps.size)
                            role = Role.Image
                        }
                    )
                }
            }
        }
        
        // Step labels (if enabled)
        if (showLabels) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                steps.forEach { step ->
                    StepLabel(
                        step = step,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Subtask 41: Circle Step Indicators
 * Individual step indicator with circle outline and fill animation
 */
@Composable
private fun StepIndicator(
    step: StepData,
    stepNumber: Int,
    animationDuration: Int,
    modifier: Modifier = Modifier
) {
    // Animation states for smooth transitions
    val fillProgress by animateFloatAsState(
        targetValue = when (step.status) {
            StepStatus.COMPLETED -> 1f
            StepStatus.ACTIVE -> 0.5f
            StepStatus.ERROR -> 1f
            StepStatus.PENDING -> 0f
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "fill_progress_animation"
    )
    
    val checkmarkScale by animateFloatAsState(
        targetValue = if (step.status == StepStatus.COMPLETED) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "checkmark_scale_animation"
    )
    
    val indicatorColor by animateColorAsState(
        targetValue = when (step.status) {
            StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary // Tomato
            StepStatus.ACTIVE -> MaterialTheme.colorScheme.primary
            StepStatus.ERROR -> MaterialTheme.colorScheme.error
            StepStatus.PENDING -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "indicator_color_animation"
    )
    
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = when (step.status) {
                StepStatus.COMPLETED -> indicatorColor
                StepStatus.ACTIVE -> indicatorColor.copy(alpha = 0.1f)
                StepStatus.ERROR -> indicatorColor.copy(alpha = 0.1f)
                StepStatus.PENDING -> Color.Transparent
            },
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = indicatorColor
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                when (step.status) {
                    StepStatus.COMPLETED -> {
                        // Subtask 43: Completion checkmark
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .scale(checkmarkScale)
                        )
                    }
                    StepStatus.ACTIVE -> {
                        // Loading indicator for active step
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = indicatorColor
                        )
                    }
                    StepStatus.ERROR -> {
                        // Error indicator (X or exclamation)
                        Text(
                            text = "!",
                            color = indicatorColor,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    StepStatus.PENDING -> {
                        // Step number for pending steps
                        Text(
                            text = stepNumber.toString(),
                            color = indicatorColor,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subtask 42: Connecting Line Animation
 * Animated connecting lines between step indicators
 */
@Composable
private fun StepperConnectingLines(
    steps: List<StepData>,
    animationDuration: Int
) {
    val density = LocalDensity.current
    
    // Pre-calculate all animated progress values outside Canvas
    val progressAnimations = remember(steps) {
        steps.indices.map { i ->
            if (i < steps.lastIndex) {
                val currentStep = steps[i]
                when {
                    currentStep.status == StepStatus.COMPLETED -> 1f
                    currentStep.status == StepStatus.ACTIVE -> 0.7f
                    else -> 0f
                }
            } else 0f
        }
    }
    
    val animatedProgresses = progressAnimations.mapIndexed { index, progress ->
        animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            ),
            label = "line_progress_$index"
        ).value
    }
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val stepWidth = size.width / steps.size
        val lineY = size.height / 2
        val lineHeight = with(density) { 2.dp.toPx() }
        
        for (i in 0 until steps.lastIndex) {
            val currentStep = steps[i]
            
            // Calculate line positions
            val startX = stepWidth * (i + 1) - stepWidth * 0.15f
            val endX = stepWidth * (i + 1) + stepWidth * 0.15f
            
            // Determine line color and progress based on step states
            val lineColor = when {
                currentStep.status == StepStatus.COMPLETED -> 
                    Color(0xFF4CAF50) // Success green
                currentStep.status == StepStatus.ACTIVE -> 
                    Color(0xFF4CAF50)
                currentStep.status == StepStatus.ERROR -> 
                    Color(0xFFF44336) // Error red
                else -> 
                    Color(0xFFE0E0E0) // Inactive gray
            }
            
            val animatedProgress = animatedProgresses[i]
            
            // Draw background line (inactive)
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(startX, lineY),
                end = Offset(endX, lineY),
                strokeWidth = lineHeight,
                cap = StrokeCap.Round
            )
            
            // Draw progress line (animated)
            if (animatedProgress > 0f) {
                val progressEndX = startX + (endX - startX) * animatedProgress
                drawLine(
                    color = lineColor,
                    start = Offset(startX, lineY),
                    end = Offset(progressEndX, lineY),
                    strokeWidth = lineHeight,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Subtask 43: Status Labels & Checkmarks  
 * Step label with title and optional description
 */
@Composable
private fun StepLabel(
    step: StepData,
    modifier: Modifier = Modifier
) {
    val labelColor by animateColorAsState(
        targetValue = when (step.status) {
            StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            StepStatus.ACTIVE -> MaterialTheme.colorScheme.onSurface
            StepStatus.ERROR -> MaterialTheme.colorScheme.error
            StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "label_color_animation"
    )
    
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (step.status == StepStatus.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Optional description for active or error steps
        if (!step.description.isNullOrEmpty() && (step.status == StepStatus.ACTIVE || step.status == StepStatus.ERROR)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = labelColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Helper function to build accessibility content description
 */
private fun buildStepContentDescription(step: StepData, stepNumber: Int, totalSteps: Int): String {
    val statusText = when (step.status) {
        StepStatus.PENDING -> "pending"
        StepStatus.ACTIVE -> "in progress"
        StepStatus.COMPLETED -> "completed"
        StepStatus.ERROR -> "failed"
    }
    
    return "Step $stepNumber of $totalSteps: ${step.title}, $statusText"
}

/**
 * Preview composables for the Stepper component
 */
@Preview(name = "Stepper - Pipeline Progress")
@Composable
private fun StepperPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Pipeline Status - In Progress")
                Stepper(
                    steps = listOf(
                        StepData("1", "Upload", StepStatus.COMPLETED),
                        StepData("2", "Processing", StepStatus.ACTIVE, "Analyzing video..."),
                        StepData("3", "Extract", StepStatus.PENDING),
                        StepData("4", "Complete", StepStatus.PENDING)
                    )
                )
            }
        }
    }
}

@Preview(name = "Stepper - All States")
@Composable
private fun StepperAllStatesPreview() {
    ReciplanTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("All Completed")
                Stepper(
                    steps = listOf(
                        StepData("1", "Upload", StepStatus.COMPLETED),
                        StepData("2", "Process", StepStatus.COMPLETED),
                        StepData("3", "Extract", StepStatus.COMPLETED),
                        StepData("4", "Done", StepStatus.COMPLETED)
                    )
                )
                
                Text("With Error")
                Stepper(
                    steps = listOf(
                        StepData("1", "Upload", StepStatus.COMPLETED),
                        StepData("2", "Process", StepStatus.ERROR, "Failed to parse"),
                        StepData("3", "Extract", StepStatus.PENDING),
                        StepData("4", "Done", StepStatus.PENDING)
                    )
                )
                
                Text("Without Labels")
                Stepper(
                    steps = listOf(
                        StepData("1", "Step 1", StepStatus.COMPLETED),
                        StepData("2", "Step 2", StepStatus.ACTIVE),
                        StepData("3", "Step 3", StepStatus.PENDING)
                    ),
                    showLabels = false
                )
            }
        }
    }
}

@Preview(name = "Stepper - Dark Theme")
@Composable
private fun StepperDarkPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Pipeline Status - Dark Theme")
                Stepper(
                    steps = listOf(
                        StepData("1", "Upload", StepStatus.COMPLETED),
                        StepData("2", "Analyzing", StepStatus.ACTIVE, "Processing video..."),
                        StepData("3", "Extract Recipe", StepStatus.PENDING),
                        StepData("4", "Generate Draft", StepStatus.PENDING)
                    )
                )
            }
        }
    }
} 