package com.example.reciplan.ui.splash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.reciplan.data.auth.AuthResult
import com.example.reciplan.ui.auth.AuthViewModel

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToUsername: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: SplashViewModel = viewModel(factory = viewModelFactory),
    authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
) {
    val healthState by viewModel.healthState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    // Handle navigation based on auth state
    LaunchedEffect(authState, healthState) {
        when {
            healthState is HealthState.Error -> {
                // Health check failed, but we can still proceed
                handleAuthNavigation(authState, onNavigateToAuth, onNavigateToMain, onNavigateToUsername)
            }
            healthState is HealthState.Success -> {
                // Health check passed, proceed based on auth
                handleAuthNavigation(authState, onNavigateToAuth, onNavigateToMain, onNavigateToUsername)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo/Title
        Text(
            text = "Reciplan",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Loading indicator
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status text
        Text(
            text = when {
                healthState is HealthState.Loading -> "Connecting to server..."
                healthState is HealthState.Error -> "Connection failed, continuing..."
                authState is AuthResult.Loading -> "Checking authentication..."
                else -> "Loading..."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Show error if health check fails
        if (healthState is HealthState.Error) {
            val errorState = healthState as HealthState.Error
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Server connection failed: ${errorState.message}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun handleAuthNavigation(
    authState: AuthResult,
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToUsername: () -> Unit
) {
    when (authState) {
        is AuthResult.Success -> {
            val successState = authState as AuthResult.Success
            println("SplashScreen: Navigation check - User ID: ${successState.user.id}")
            println("SplashScreen: Navigation check - Username: ${successState.user.username}")
            println("SplashScreen: Navigation check - Setup complete: ${successState.user.setup_complete}")
            
            // Check if user has completed setup (has username and setup_complete)
            if (successState.user.username != null && successState.user.setup_complete) {
                println("SplashScreen: Navigating to main screen")
                onNavigateToMain()
            } else if (successState.user.username != null && !successState.user.setup_complete) {
                // User has a username but setup_complete is false - this might be a data inconsistency
                // Navigate to main screen anyway since they have a username
                println("SplashScreen: User has username but setup_complete is false, navigating to main anyway")
                onNavigateToMain()
            } else {
                println("SplashScreen: Navigating to username setup screen")
                // User needs to set up username
                onNavigateToUsername()
            }
        }
        is AuthResult.Error -> {
            println("SplashScreen: Auth error - ${authState.message}, navigating to auth")
            onNavigateToAuth()
        }
        is AuthResult.Loading -> {
            println("SplashScreen: Auth loading, staying on splash")
            // Stay on splash screen
        }
    }
} 