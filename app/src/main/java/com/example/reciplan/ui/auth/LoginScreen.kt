package com.example.reciplan.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.reciplan.R
import com.example.reciplan.data.auth.AuthResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    viewModel: AuthViewModel = viewModel(factory = viewModelFactory)
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var showEmailLinkSent by remember { mutableStateOf(false) }
    
    // Google Sign-In launcher using One Tap
    val signInClient = remember { Identity.getSignInClient(context) }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = signInClient.getSignInCredentialFromIntent(result.data)
            viewModel.handleGoogleSignInResult(credential)
        } catch (e: ApiException) {
            // Handle sign-in failure
            viewModel.setLoading()
        }
    }

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthResult.Success -> {
                val successState = authState as AuthResult.Success
                // Check if this is an email sent confirmation
                if (successState.user.id.isEmpty() && successState.user.name == "Email sent successfully") {
                    showEmailLinkSent = true
                } else {
                    // Check if user has completed setup (has username and setup_complete)
                    if (successState.user.username != null && successState.user.setup_complete) {
                        onLoginSuccess()
                    } else {
                        // User needs to set up username
                        onNavigateToRegister()
                    }
                }
            }
            is AuthResult.Error -> {
                // Handle error - reset email sent state
                showEmailLinkSent = false
            }
            is AuthResult.Loading -> {
                // Handle loading
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
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.reciplan_logo),
            contentDescription = "Reciplan Logo",
            modifier = Modifier
                .size(300.dp)
                .padding(bottom = 48.dp)
        )

        if (showEmailLinkSent) {
            // Email link sent confirmation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Check your email!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "We've sent a sign-in link to $email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Click the link in your email to sign in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Email input form
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("Enter your email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    viewModel.setLoading()
                    viewModel.sendEmailLink(email)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotEmpty() && authState !is AuthResult.Loading
            ) {
                if (authState is AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Sign-in Link")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "or",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign In Button
            OutlinedButton(
                onClick = { 
                    viewModel.setLoading()
                    // Launch One Tap sign-in
                    val signInRequest = BeginSignInRequest.builder()
                        .setGoogleIdTokenRequestOptions(
                            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("771971890295-5gbmmottvhno7sg422jplfeatn6usnsf.apps.googleusercontent.com")
                                .build()
                        )
                        .build()
                    
                    signInClient.beginSignIn(signInRequest)
                        .addOnSuccessListener { result ->
                            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                                result.pendingIntent.intentSender
                            ).build()
                            googleSignInLauncher.launch(intentSenderRequest)
                        }
                        .addOnFailureListener {
                            // Handle failure
                            viewModel.setLoading()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is AuthResult.Loading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google icon placeholder - you can replace with actual Google icon
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_email),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google")
                }
            }
        }

        // Error display
        if (authState is AuthResult.Error) {
            val errorState = authState as AuthResult.Error
            // Don't show "User not authenticated" error as it's the normal initial state
            if (!errorState.message.contains("User not authenticated", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (errorState.message.contains("quota", ignoreCase = true)) {
                                "Email sign-in temporarily unavailable. Please use Google Sign-In instead."
                            } else {
                                errorState.message
                            },
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (errorState.message.contains("quota", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Email link authentication is temporarily disabled. Please use Google Sign-In instead.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Back to email form
        if (showEmailLinkSent) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { showEmailLinkSent = false }
            ) {
                Text("Try different email")
            }
        }
    }
} 