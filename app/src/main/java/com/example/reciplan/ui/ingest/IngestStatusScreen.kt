package com.example.reciplan.ui.ingest

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.IngestStatus
import com.example.reciplan.ui.components.*
import com.example.reciplan.ui.theme.*

/**
 * Task 13: Pipeline Status Screen Redesign
 * 
 * Enhanced Pipeline Status screen with:
 * - Stepper Integration (Subtask 131)
 * - Real-time Progress (Subtask 132)
 * - Status-specific Iconography (Subtask 133)
 * - Enhanced Error States (Subtask 134)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestStatusScreen(
    jobId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDraftPreview: (String) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: AddFromTikTokViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Pipeline step definitions with enhanced iconography
    val pipelineSteps = remember {
        createPipelineSteps()
    }
    
    // Start polling when screen opens
    LaunchedEffect(jobId) {
        if (uiState.jobId != jobId || !uiState.isPolling) {
            viewModel.startJobPolling(jobId)
        }
    }
    
    // Navigate to draft preview when job completes
    LaunchedEffect(uiState.jobStatus, uiState.jobDetails) {
        if (uiState.jobStatus == IngestStatus.COMPLETED && uiState.jobDetails?.recipeId != null) {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onNavigateToDraftPreview(uiState.jobDetails!!.recipeId!!)
        }
    }
    
    // Haptic feedback for step changes
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep > 0) {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Header
            EnhancedPipelineHeader(
                onNavigateBack = onNavigateBack,
                isProcessing = uiState.isPolling,
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                hasError = uiState.hasError,
                isComplete = uiState.isComplete
            )
            
            // Main Content with Scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Section
                EnhancedPipelineHeroSection(
                    stepTitle = uiState.stepTitle,
                    stepDescription = uiState.stepDescription,
                    isComplete = uiState.isComplete,
                    hasError = uiState.hasError,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Status Content
                AnimatedContent(
                    targetState = when {
                        uiState.hasError -> PipelineState.Error
                        uiState.isComplete -> PipelineState.Success
                        uiState.isPolling -> PipelineState.Processing
                        else -> PipelineState.Idle
                    },
                    label = "pipeline_content"
                ) { pipelineState ->
                    when (pipelineState) {
                        PipelineState.Processing -> {
                            // Subtask 132: Real-time Progress
                            EnhancedProcessingState(
                                currentStepData = pipelineSteps.getOrNull(uiState.currentStep - 1),
                                progressPercentage = viewModel.getProgressPercentage(),
                                progressText = viewModel.getProgressText(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        PipelineState.Success -> {
                            EnhancedSuccessState(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        PipelineState.Error -> {
                            // Subtask 134: Enhanced Error States
                            EnhancedPipelineErrorState(
                                error = uiState.errorSnackbarMessage ?: "Processing failed",
                                stepTitle = uiState.stepTitle,
                                canRetry = uiState.canRetry,
                                retryText = uiState.retryActionText ?: "Retry",
                                onRetry = { viewModel.retryJob() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        PipelineState.Idle -> {
                            EnhancedIdleState(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Polling Status Indicator
                AnimatedVisibility(
                    visible = uiState.isPolling && !uiState.hasError && !uiState.isComplete
                ) {
                    EnhancedPollingIndicator()
                }
            }
        }
    }
}

/**
 * Pipeline state enum for better state management
 */
private enum class PipelineState {
    Idle, Processing, Success, Error
}

/**
 * Enhanced Pipeline Step Data with status-specific iconography
 */
data class PipelineStepData(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val processingIcon: ImageVector? = null,
    val estimatedDuration: String
)

/**
 * Subtask 133: Status-specific Iconography
 * Create pipeline steps with appropriate icons for each stage
 */
private fun createPipelineSteps(): List<PipelineStepData> {
    return listOf(
        PipelineStepData(
            id = "initialize",
            title = "Initializing",
            description = "Setting up video processing pipeline",
            icon = Icons.Default.PlayArrow,
            processingIcon = Icons.Default.Settings,
            estimatedDuration = "5s"
        ),
        PipelineStepData(
            id = "download",
            title = "Downloading",
            description = "Fetching TikTok video content",
            icon = Icons.Default.ArrowForward,
            processingIcon = Icons.Default.Refresh,
            estimatedDuration = "10s"
        ),
        PipelineStepData(
            id = "extract_audio",
            title = "Audio Extraction",
            description = "Extracting audio track from video",
            icon = Icons.Default.Menu,
            processingIcon = Icons.Default.Search,
            estimatedDuration = "8s"
        ),
        PipelineStepData(
            id = "transcribe",
            title = "Transcribing",
            description = "Converting speech to text using AI",
            icon = Icons.Default.Create,
            processingIcon = Icons.Default.Edit,
            estimatedDuration = "15s"
        ),
        PipelineStepData(
            id = "analyze_content",
            title = "Content Analysis",
            description = "Identifying recipe elements and structure",
            icon = Icons.Default.Search,
            processingIcon = Icons.Default.Info,
            estimatedDuration = "12s"
        ),
        PipelineStepData(
            id = "extract_ingredients",
            title = "Ingredient Extraction",
            description = "Identifying ingredients and quantities",
            icon = Icons.Default.Favorite,
            processingIcon = Icons.Default.Add,
            estimatedDuration = "10s"
        ),
        PipelineStepData(
            id = "structure_steps",
            title = "Step Structuring",
            description = "Organizing cooking instructions",
            icon = Icons.Default.List,
            processingIcon = Icons.Default.Check,
            estimatedDuration = "8s"
        ),
        PipelineStepData(
            id = "enhance_recipe",
            title = "Recipe Enhancement",
            description = "Adding timing, tips, and metadata",
            icon = Icons.Default.Edit,
            processingIcon = Icons.Default.Star,
            estimatedDuration = "12s"
        ),
        PipelineStepData(
            id = "generate_image",
            title = "Image Processing",
            description = "Extracting and optimizing recipe images",
            icon = Icons.Default.Share,
            processingIcon = Icons.Default.Person,
            estimatedDuration = "7s"
        ),
        PipelineStepData(
            id = "finalize",
            title = "Finalizing",
            description = "Creating your recipe draft",
            icon = Icons.Default.CheckCircle,
            processingIcon = Icons.Default.Done,
            estimatedDuration = "3s"
        )
    )
}

/**
 * Enhanced Pipeline Header with dynamic styling and progress indication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedPipelineHeader(
    onNavigateBack: () -> Unit,
    isProcessing: Boolean,
    currentStep: Int,
    totalSteps: Int,
    hasError: Boolean,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    // Create a subtle gradient overlay for smooth blending instead of harsh shadow
    val headerAlpha by animateFloatAsState(
        targetValue = if (isProcessing) 0.03f else 0f,
        animationSpec = MotionSpecs.emphasizedTween(),
        label = "header_overlay_alpha"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Add subtle gradient overlay that blends smoothly
            .drawBehind {
                if (headerAlpha > 0f) {
                    drawRect(
                        color = Color.Black.copy(alpha = headerAlpha),
                        size = size.copy(height = size.height + 4.dp.toPx())
                    )
                }
            },
        shadowElevation = 0.dp, // Remove harsh shadow
        color = MaterialTheme.colorScheme.surface
    ) {
        TopAppBar(
            title = { 
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                Text(
                            text = when {
                                hasError -> "Processing Failed"
                                isComplete -> "Recipe Ready"
                                isProcessing -> "Creating Recipe"
                                else -> "Processing Recipe"
                            },
                    style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                hasError -> MaterialTheme.colorScheme.error
                                isComplete -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        AnimatedVisibility(
                            visible = isProcessing,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (totalSteps > 0 && isProcessing) {
                    Text(
                            text = "Step $currentStep of $totalSteps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                Surface(
                    shape = AppShapes.SmallShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}

/**
 * Enhanced Pipeline Hero Section with dynamic content
 */
@Composable
private fun EnhancedPipelineHeroSection(
    stepTitle: String,
    stepDescription: String,
    isComplete: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                hasError -> "Processing encountered an issue"
                isComplete -> "Your recipe is ready! 🎉"
                stepTitle.isNotEmpty() -> stepTitle
                else -> "Processing your TikTok video..."
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                hasError -> MaterialTheme.colorScheme.error
                isComplete -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        
        if (stepDescription.isNotEmpty() || !isComplete) {
            Spacer(modifier = Modifier.height(12.dp))
                Text(
                text = when {
                    hasError -> "Don't worry, you can try again or start with a different video"
                    isComplete -> "We've extracted all the recipe information from your TikTok video"
                    stepDescription.isNotEmpty() -> stepDescription
                    else -> "We're analyzing your video to extract recipe information automatically"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}



/**
 * Subtask 132: Real-time Progress
 * Enhanced Processing State with real-time updates
 */
@Composable
private fun EnhancedProcessingState(
    currentStepData: PipelineStepData?,
    progressPercentage: Float,
    progressText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing_animation")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "icon_rotation"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Current Step Icon
        currentStepData?.let { stepData ->
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Icon(
                    imageVector = stepData.processingIcon ?: stepData.icon,
                    contentDescription = "Processing ${stepData.title}",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(25.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
        
        // Progress Information
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = progressText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            currentStepData?.let { stepData ->
                Text(
                    text = "Estimated time: ${stepData.estimatedDuration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progressPercentage * 100).toInt()}% complete",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced Success State with celebration
 */
@Composable
private fun EnhancedSuccessState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "success_celebration")
    
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
        label = "celebration_scale"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(100.dp)
                .scale(celebrationScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 12.dp
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(25.dp)
            )
        }
        
        Text(
            text = "Recipe created successfully!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Redirecting to recipe preview...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Subtask 134: Enhanced Error States
 * Improved error handling with retry options
 */
@Composable
private fun EnhancedPipelineErrorState(
    error: String,
    stepTitle: String,
    canRetry: Boolean,
    retryText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            type = EmptyStateType.CONNECTION_ERROR,
            onPrimaryAction = if (canRetry) onRetry else null,
            customContent = EmptyStateContent(
                title = stepTitle.ifEmpty { "Processing Failed" },
                subtitle = error,
                illustration = EmptyStateIllustration.NETWORK_ERROR,
                primaryAction = if (canRetry) {
                    ActionButton(
                        text = retryText,
                        onClick = onRetry,
                        icon = Icons.Default.Refresh
                    )
                } else null
            )
        )
    }
}

/**
 * Enhanced Idle State
 */
@Composable
private fun EnhancedIdleState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Waiting",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            )
        }
        
        Text(
            text = "Initializing...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Enhanced Polling Indicator
 */
@Composable
private fun EnhancedPollingIndicator(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.MediumShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Text(
                text = "Checking status...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Preview composables for the enhanced pipeline screen
 */
@Preview(name = "Enhanced Pipeline Processing - Light")
@Composable
private fun EnhancedPipelineProcessingPreview() {
    ReciplanTheme {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enhanced Pipeline Processing Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(name = "Enhanced Pipeline Success - Dark")
@Composable
private fun EnhancedPipelineSuccessPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            EnhancedSuccessState(
        modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
    )
        }
    }
} 