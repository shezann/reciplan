package com.example.reciplan.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseUsernameScreen(
    onUsernameSet: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: AuthViewModel = viewModel(factory = viewModelFactory)
) {
    val usernameState by viewModel.usernameState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    
    var username by remember { mutableStateOf("") }
    var showValidation by remember { mutableStateOf(false) }
    var userAlreadyHasUsername by remember { mutableStateOf(false) }
    var existingUsername by remember { mutableStateOf("") }
    
    // Check if user already has a username
    LaunchedEffect(authState) {
        val currentAuthState = authState
        if (currentAuthState is com.example.reciplan.data.auth.AuthResult.Success) {
            val user = currentAuthState.user
            if (!user.username.isNullOrBlank()) {
                userAlreadyHasUsername = true
                existingUsername = user.username
            } else {
                userAlreadyHasUsername = false
                existingUsername = ""
            }
        }
    }

    // Debounced username availability check
    LaunchedEffect(username) {
        if (username.isNotBlank() && username.length >= 3 && !userAlreadyHasUsername) {
            delay(500) // Debounce for 500ms
            if (username.isNotBlank()) {
                viewModel.checkUsernameAvailability(username)
                showValidation = true
            }
        } else {
            showValidation = false
        }
    }

    // Handle username set success
    LaunchedEffect(usernameState) {
        if (usernameState is UsernameState.Set) {
            onUsernameSet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show different UI based on whether user already has a username
        if (userAlreadyHasUsername) {
            // User already has a username - show continuation screen
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You already have a username:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "@$existingUsername",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You can continue to the app with your existing username.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onUsernameSet() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to App")
            }
        } else {
            // User needs to set a username - show the normal username setup UI
            Text(
                text = "Choose Your Username",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Pick a unique username that others can use to find you",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Username input
            OutlinedTextField(
                value = username,
                onValueChange = { newValue ->
                    // Only allow alphanumeric characters and underscores
                    val filteredValue = newValue.filter { it.isLetterOrDigit() || it == '_' }
                    username = filteredValue
                },
                label = { Text("Username") },
                placeholder = { Text("e.g., john_doe") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    when {
                        username.isEmpty() -> Text("Username must be at least 3 characters")
                        username.length < 3 -> Text("Username must be at least 3 characters")
                        showValidation -> {
                            when (usernameState) {
                                is UsernameState.Checking -> Text("Checking availability...")
                                is UsernameState.Available -> Text("Username is available!", color = Color.Green)
                                is UsernameState.Unavailable -> Text("Username is taken", color = Color.Red)
                                is UsernameState.Error -> Text("Error checking availability", color = Color.Red)
                                else -> Text("")
                            }
                        }
                        else -> Text("Only letters, numbers, and underscores allowed")
                    }
                },
                trailingIcon = {
                    if (showValidation) {
                        when (usernameState) {
                            is UsernameState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is UsernameState.Available -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Available",
                                    tint = Color.Green
                                )
                            }
                            is UsernameState.Unavailable -> {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Unavailable",
                                    tint = Color.Red
                                )
                            }
                            else -> null
                        }
                    }
                },
                isError = showValidation && usernameState is UsernameState.Unavailable
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Set username button
            Button(
                onClick = {
                    viewModel.setupUser(username)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.length >= 3 && 
                         usernameState is UsernameState.Available && 
                         usernameState !is UsernameState.Setting
            ) {
                when (usernameState) {
                    is UsernameState.Setting -> {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Setting Username...")
                        }
                    }
                    else -> {
                        Text("Set Username")
                    }
                }
            }

            // Error display
            if (usernameState is UsernameState.Error) {
                val errorState = usernameState as UsernameState.Error
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = errorState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        // If the error indicates the user already has an account/username, provide an option to skip
                        if (errorState.message.contains("already has an account", ignoreCase = true) ||
                            errorState.message.contains("already taken", ignoreCase = true) ||
                            errorState.message.contains("Username already taken", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "It looks like you already have an account with a username. You can skip this step and go directly to the app.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onUsernameSet() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Skip to App")
                            }
                        }
                    }
                }
            }

            // Username requirements
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Username Requirements:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• At least 3 characters long",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Only letters, numbers, and underscores",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Must be unique across all users",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 