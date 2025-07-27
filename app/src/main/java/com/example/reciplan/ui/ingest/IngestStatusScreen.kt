package com.example.reciplan.ui.ingest

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status display
            when (uiState.jobStatus) {
                null, IngestStatus.QUEUED -> {
                    AnimatedLoader()
                    Text(
                        text = "Queued for processing",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.DOWNLOADING -> {
                    AnimatedLoader()
                    Text(
                        text = "Downloading video",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.EXTRACTING -> {
                    AnimatedLoader()
                    Text(
                        text = "Extracting audio",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.TRANSCRIBING -> {
                    AnimatedLoader()
                    Text(
                        text = "Transcribing audio",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.DRAFT_TRANSCRIBED -> {
                    AnimatedLoader()
                    Text(
                        text = "Processing transcript",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.OCRING -> {
                    AnimatedLoader()
                    Text(
                        text = "Extracting text from video",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.OCR_DONE -> {
                    AnimatedLoader()
                    Text(
                        text = "Processing extracted text",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.LLM_REFINING -> {
                    AnimatedLoader()
                    Text(
                        text = "AI is analyzing recipe",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.DRAFT_PARSED -> {
                    AnimatedLoader()
                    Text(
                        text = "Finalizing recipe",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.DRAFT_PARSED_WITH_ERRORS -> {
                    AnimatedLoader()
                    Text(
                        text = "Finalizing recipe (with minor issues)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                IngestStatus.COMPLETED -> {
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
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Redirecting to recipe preview...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                IngestStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Failed",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Processing failed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
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