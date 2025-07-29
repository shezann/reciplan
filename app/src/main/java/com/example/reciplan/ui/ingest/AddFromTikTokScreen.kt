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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.ui.components.*
import com.example.reciplan.ui.theme.*

/**
 * Task 12: Add-from-TikTok Screen Redesign
 * 
 * Enhanced TikTok import screen with:
 * - Modern Input Styling (Subtask 121)
 * - Enhanced Loading States (Subtask 122)
 * - Better Error Handling (Subtask 123)
 * - Success Celebrations (Subtask 124)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFromTikTokScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStatus: (String) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: AddFromTikTokViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Animation and interaction states
    var showSuccessCelebration by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf("") }
    
    // Clear any previous job state when entering the screen
    LaunchedEffect(Unit) {
        viewModel.clearCurrentJob()
    }
    
    // Validation function
    fun validateUrl(): Boolean {
        urlError = when {
            url.isBlank() -> "URL is required"
            !uiState.isValidUrl -> "Please enter a valid TikTok URL"
            else -> ""
        }
        return urlError.isEmpty()
    }
    
    // Handle form submission
    fun submitUrl() {
        if (validateUrl()) {
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            viewModel.startIngest(url)
        }
    }
    
    // Trigger URL validation when URL changes
    LaunchedEffect(url) {
        viewModel.validateTikTokUrl(url)
    }
    
    // Handle navigation to status screen when job starts
    LaunchedEffect(uiState.jobId, uiState.isLoading, uiState.isPolling, uiState.isComplete, uiState.hasError) {
        uiState.jobId?.let { jobId ->
            if ((uiState.isLoading || uiState.isPolling) && !uiState.isComplete && !uiState.hasError) {
                onNavigateToStatus(jobId)
            }
        }
    }
    
    // Subtask 124: Success Celebrations - Handle success state
    LaunchedEffect(uiState.isComplete, uiState.hasError) {
        if (uiState.isComplete && !uiState.hasError) {
            showSuccessCelebration = true
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(3000) // Show celebration for 3 seconds
            showSuccessCelebration = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Header
            EnhancedTikTokHeader(
                onNavigateBack = onNavigateBack,
                isProcessing = uiState.isLoading
            )
            
            // Main Content
            AnimatedContent(
                targetState = when {
                    showSuccessCelebration -> TikTokScreenState.Success
                    uiState.isLoading -> TikTokScreenState.Loading
                    uiState.error != null -> TikTokScreenState.Error
                    else -> TikTokScreenState.Input
                },
                label = "tiktok_screen_content"
            ) { screenState ->
                when (screenState) {
                    TikTokScreenState.Input -> {
                        EnhancedTikTokInputContent(
                            url = url,
                            onUrlChange = { 
                                url = it
                                if (urlError.isNotEmpty()) {
                                    validateUrl()
                                }
                            },
                            urlError = urlError,
                            uiState = uiState,
                            onSubmit = { submitUrl() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    TikTokScreenState.Loading -> {
                        // Subtask 122: Enhanced Loading States
                        EnhancedTikTokLoadingState(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    TikTokScreenState.Error -> {
                        // Subtask 123: Better Error Handling
                        EnhancedTikTokErrorState(
                            error = uiState.error ?: "An unexpected error occurred",
                            onRetry = { 
                                viewModel.dismissErrorSnackbar()
                                submitUrl()
                            },
                            onStartOver = {
                                viewModel.dismissErrorSnackbar()
                                url = ""
                                urlError = ""
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    TikTokScreenState.Success -> {
                        // Subtask 124: Success Celebrations
                        EnhancedTikTokSuccessState(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen state enum for better state management
 */
private enum class TikTokScreenState {
    Input, Loading, Error, Success
}

/**
 * Enhanced TikTok Header with better styling and animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedTikTokHeader(
    onNavigateBack: () -> Unit,
    isProcessing: Boolean,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Create from TikTok",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
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
 * Enhanced TikTok Input Content with modern styling
 */
@Composable
private fun EnhancedTikTokInputContent(
    url: String,
    onUrlChange: (String) -> Unit,
    urlError: String,
    uiState: AddFromTikTokUiState,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
        Column(
            modifier = modifier
                .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Section
        EnhancedTikTokHeroSection(
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Subtask 121: Modern Input Styling
        EnhancedTikTokUrlInput(
            url = url,
            onUrlChange = onUrlChange,
            urlError = urlError,
            isValid = uiState.isValidUrl,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit Button
        EnhancedSubmitButton(
            isEnabled = uiState.isValidUrl && !uiState.isLoading && !uiState.isJobLimitReached,
            isLoading = uiState.isLoading,
            onSubmit = onSubmit,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Job Status Information
        AnimatedVisibility(
            visible = uiState.isJobLimitReached || uiState.activeJobCount > 0
        ) {
            EnhancedJobStatusCard(
                activeJobCount = uiState.activeJobCount,
                isJobLimitReached = uiState.isJobLimitReached,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Help Section
        EnhancedHelpSection(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Enhanced Hero Section with TikTok branding and animations
 */
@Composable
private fun EnhancedTikTokHeroSection(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_scale"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TikTok Icon Placeholder (animated)
        Surface(
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            shape = AppShapes.LargeShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "TikTok Videos",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            )
        }
            
            Spacer(modifier = Modifier.height(16.dp))
            
        // Title
        Text(
            text = "Transform TikTok Videos into Recipes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
        // Subtitle
        Text(
            text = "Paste any TikTok cooking video URL and we'll extract the recipe automatically",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

/**
 * Subtask 121: Modern Input Styling
 * Enhanced URL input with new design system styling
 */
@Composable
private fun EnhancedTikTokUrlInput(
    url: String,
    onUrlChange: (String) -> Unit,
    urlError: String,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Input field using EnhancedTextField
        EnhancedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = "TikTok Video URL",
            placeholder = "https://www.tiktok.com/@user/video/123...",
            isError = urlError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Enhanced validation feedback
        AnimatedVisibility(
            visible = urlError.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = urlError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Success indicator
        AnimatedVisibility(
            visible = url.isNotEmpty() && urlError.isEmpty() && isValid,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Valid URL",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Valid TikTok URL detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * Enhanced Submit Button with better styling and animations
 */
@Composable
private fun EnhancedSubmitButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSubmit,
        enabled = isEnabled,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        shape = AppShapes.MediumShape
    ) {
        if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create Recipe Draft",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Enhanced Job Status Card with better visual design
 */
@Composable
private fun EnhancedJobStatusCard(
    activeJobCount: Int,
    isJobLimitReached: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.LargeShape,
        color = if (isJobLimitReached) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isJobLimitReached) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = if (isJobLimitReached) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isJobLimitReached) {
                        "Job Limit Reached"
                    } else {
                        "Active Jobs"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isJobLimitReached) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Text(
                    text = if (isJobLimitReached) {
                        "Please wait for a job to complete before starting a new one"
                    } else {
                        "You can process up to 3 videos simultaneously"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isJobLimitReached) {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                )
            }
            
            // Job count indicator
            Surface(
                shape = AppShapes.SmallShape,
                color = if (isJobLimitReached) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                Text(
                    text = "$activeJobCount/3",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Enhanced Help Section with better visual hierarchy
 */
@Composable
private fun EnhancedHelpSection(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.LargeShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val helpItems = listOf(
                "Paste any TikTok cooking video URL",
                "We extract recipe information automatically",
                "Videos must contain cooking content",
                "Processing typically takes 30-60 seconds"
            )
            
            helpItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item != helpItems.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Subtask 122: Enhanced Loading States
 * Improved processing feedback during import
 */
@Composable
private fun EnhancedTikTokLoadingState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_rotation"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_pulse"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Animated processing icon
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale),
                shape = AppShapes.LargeShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Processing",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
            
            // Progress indicator
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Loading text with animation
                                Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                    text = "Analyzing TikTok Video",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Extracting ingredients, instructions, and cooking tips...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                                    Text(
                    text = "This usually takes 30-60 seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Subtask 123: Better Error Handling
 * Improved error messages and recovery using EmptyState patterns
 */
@Composable
private fun EnhancedTikTokErrorState(
    error: String,
    onRetry: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            type = EmptyStateType.CONNECTION_ERROR,
            onPrimaryAction = onRetry,
            onSecondaryAction = onStartOver,
            customContent = EmptyStateContent(
                title = "Couldn't Process Video",
                subtitle = error,
                illustration = EmptyStateIllustration.NETWORK_ERROR,
                primaryAction = ActionButton(
                    text = "Try Again",
                    onClick = onRetry,
                    icon = Icons.Default.Refresh
                ),
                secondaryAction = ActionButton(
                    text = "Start Over",
                    onClick = onStartOver,
                    icon = Icons.Default.Refresh
                )
            )
        )
    }
}

/**
 * Subtask 124: Success Celebrations
 * Celebration animations for successful imports
 */
@Composable
private fun EnhancedTikTokSuccessState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "success_celebration")
    
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebration_scale"
    )
    
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success celebration icon
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(celebrationScale),
                shape = AppShapes.LargeShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                shadowElevation = 12.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp)
                        .graphicsLayer { rotationZ = sparkleRotation }
                )
            }
            
            // Success message
                Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                    text = "Recipe Created Successfully! 🎉",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Your TikTok video has been transformed into a delicious recipe draft",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                    Text(
                    text = "Redirecting you to review and edit...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Preview composables for the enhanced TikTok screen
 */
@Preview(name = "Enhanced TikTok Input - Light")
@Composable
private fun EnhancedTikTokInputPreview() {
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
                    text = "Enhanced TikTok Input Preview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(name = "Enhanced TikTok Loading - Dark")
@Composable
private fun EnhancedTikTokLoadingPreview() {
    ReciplanTheme(darkTheme = true) {
        Surface {
            EnhancedTikTokLoadingState(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(name = "Enhanced TikTok Success")
@Composable
private fun EnhancedTikTokSuccessPreview() {
    ReciplanTheme {
        Surface {
            EnhancedTikTokSuccessState(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
} 