package com.example.reciplan.ui.ingest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel


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
    
    // Clear any previous job state when entering the screen
    LaunchedEffect(Unit) {
        viewModel.clearCurrentJob()
    }
    
    // Form state - starts empty each time screen is opened
    var url by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf("") }
    
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
            viewModel.startIngest(url)
        }
    }
    
    // Trigger URL validation when URL changes
    LaunchedEffect(url) {
        viewModel.validateTikTokUrl(url)
    }
    
    // Handle navigation to status screen when job starts
    // Only navigate if we have a jobId AND we're actively processing (not completed/failed)
    LaunchedEffect(uiState.jobId, uiState.isLoading, uiState.isPolling, uiState.isComplete, uiState.hasError) {
        uiState.jobId?.let { jobId ->
            // Only navigate if we have a valid job that's actively being processed
            // This prevents navigation on stale jobId from previous completed sessions
            if ((uiState.isLoading || uiState.isPolling) && !uiState.isComplete && !uiState.hasError) {
                onNavigateToStatus(jobId)
            }
        }
    }
    
    // Handle error display
    LaunchedEffect(uiState.showErrorSnackbar, uiState.errorSnackbarMessage) {
        if (uiState.showErrorSnackbar && uiState.errorSnackbarMessage != null) {
            kotlinx.coroutines.delay(5000)
            viewModel.dismissErrorSnackbar()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add from TikTok") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Create Recipe from TikTok",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Paste a TikTok video URL to automatically extract recipe information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // URL Input Field
            OutlinedTextField(
                value = url,
                onValueChange = { 
                    url = it
                    if (urlError.isNotEmpty()) {
                        validateUrl() // Clear error when user types
                    }
                },
                label = { Text("TikTok URL") },
                placeholder = { Text("https://www.tiktok.com/@user/video/1234567890") },
                isError = urlError.isNotEmpty(),
                supportingText = {
                    if (urlError.isNotEmpty()) {
                        Text(urlError, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            

            
            // Submit Button
            val isButtonEnabled = uiState.isValidUrl && !uiState.isLoading && !uiState.isJobLimitReached
            
            Button(
                onClick = { submitUrl() },
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text("Create Draft")
                }
            }
            
            // Error message
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
                                    // Job limit warning
                        if (uiState.isJobLimitReached) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "You have reached the maximum number of active jobs (3). Please wait for one to complete.",
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Current active jobs: ${uiState.activeJobCount}/3",
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        } else if (uiState.activeJobCount > 0) {
                            // Show current job count even when not at limit
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Active jobs: ${uiState.activeJobCount}/3",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Help text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How it works:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Paste any TikTok video URL\n" +
                              "• We'll extract recipe information automatically\n" +
                              "• Videos must be under 10 minutes\n" +
                              "• You can have up to 3 active jobs at once",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 