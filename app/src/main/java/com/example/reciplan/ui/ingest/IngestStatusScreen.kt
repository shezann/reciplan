package com.example.reciplan.ui.ingest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.IngestStatus

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
    
    // Start polling when screen opens
    LaunchedEffect(jobId) {
        if (uiState.jobId != jobId || !uiState.isPolling) {
            viewModel.startJobPolling(jobId)
        }
    }
    
    // Navigate to draft preview when job completes
    LaunchedEffect(uiState.jobStatus, uiState.jobDetails) {
        if (uiState.jobStatus == IngestStatus.COMPLETED && uiState.jobDetails?.recipeId != null) {
            onNavigateToDraftPreview(uiState.jobDetails!!.recipeId!!)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Processing Recipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Creating Recipe from TikTok",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "We're processing your video to extract recipe information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Progress stepper (Task 4.5)
            ProgressStepper(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                isComplete = uiState.isComplete,
                hasError = uiState.hasError,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            
            // Progress text
            Text(
                text = viewModel.getProgressText(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Current step information
            if (!uiState.hasError && !uiState.isComplete) {
                Text(
                    text = uiState.stepTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = uiState.stepDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Animated loader for active processing
                AnimatedLoader()
            }
            
            // Completion state
            if (uiState.isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Recipe created successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Redirecting to recipe preview...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Error state
            if (uiState.hasError) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = uiState.stepTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
                
                // Show error message if available
                uiState.errorSnackbarMessage?.let { errorMessage ->
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                // Retry button if available
                if (uiState.canRetry) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.retryJob() }
                    ) {
                        Text(uiState.retryActionText ?: "Retry")
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Polling indicator
            if (uiState.isPolling) {
                Text(
                    text = "Checking status...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Horizontal progress stepper component (Task 4.5)
 * Shows animated progress through pipeline steps with icons
 */
@Composable
fun ProgressStepper(
    currentStep: Int,
    totalSteps: Int,
    isComplete: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val progressPercentage = if (totalSteps > 0) {
        currentStep.toFloat() / totalSteps.toFloat()
    } else {
        0f
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar with animated fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = progressPercentage,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = EaseInOutCubic
                ),
                label = "progress"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasError -> MaterialTheme.colorScheme.error
                            isComplete -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Step indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show key milestone steps (1, 3, 5, 7, 10)
            val milestoneSteps = listOf(1, 3, 5, 7, 10)
            
            milestoneSteps.forEach { step ->
                StepIndicator(
                    stepNumber = step,
                    isActive = currentStep >= step && !hasError,
                    isComplete = currentStep > step || (isComplete && step <= totalSteps),
                    hasError = hasError && currentStep <= step,
                    isCurrentStep = currentStep == step && !isComplete && !hasError
                )
            }
        }
    }
}

/**
 * Individual step indicator with animated states
 */
@Composable
fun StepIndicator(
    stepNumber: Int,
    isActive: Boolean,
    isComplete: Boolean,
    hasError: Boolean,
    isCurrentStep: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            hasError -> MaterialTheme.colorScheme.error
            isComplete -> MaterialTheme.colorScheme.primary
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            hasError -> MaterialTheme.colorScheme.onError
            isComplete -> MaterialTheme.colorScheme.onPrimary
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "contentColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isCurrentStep) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size((32 * scale).dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            isComplete -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Step $stepNumber complete",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            hasError -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Step $stepNumber error",
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            isCurrentStep -> {
                // Pulsing animation for current step
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(
                        color = contentColor,
                        radius = (size.minDimension / 4f) * alpha,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            else -> {
                Text(
                    text = stepNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AnimatedLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Loading",
        modifier = Modifier
            .size(64.dp)
            .rotate(rotation),
        tint = MaterialTheme.colorScheme.primary
    )
} 