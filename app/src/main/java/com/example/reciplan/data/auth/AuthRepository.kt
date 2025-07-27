package com.example.reciplan.data.auth

import android.content.Context
import com.example.reciplan.data.api.AuthApi
import com.example.reciplan.data.model.*
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ActionCodeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
}

class AuthRepository(
    private val context: Context,
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val signInClient: SignInClient = Identity.getSignInClient(context)

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun getSignInClient(): SignInClient = signInClient

    suspend fun signInWithGoogle(credential: SignInCredential): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        
        try {
            // First, authenticate with Firebase using Google credentials
            val googleToken = credential.googleIdToken
            if (googleToken != null) {
                // Sign into Firebase with Google credentials
                val firebaseCredential = GoogleAuthProvider.getCredential(googleToken, null)
                val firebaseResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = firebaseResult.user
                
                if (firebaseUser != null) {
                    // Now authenticate with backend using Google token
                    val backendResult = authenticateWithGoogle(googleToken)
                    emit(backendResult)
                } else {
                    emit(AuthResult.Error("Failed to sign into Firebase"))
                }
            } else {
                emit(AuthResult.Error("Failed to get Google token"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Google sign-in failed: ${e.message}"))
        }
    }

    suspend fun sendEmailLink(email: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        
        try {
            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://reciplan-c3e17.firebaseapp.com/__/auth/action") // Use correct Firebase domain
                .setHandleCodeInApp(true)
                .setAndroidPackageName("com.example.reciplan", true, null)
                .build()

            firebaseAuth.sendSignInLinkToEmail(email, actionCodeSettings).await()
            
            // Save email for later use in email link verification
            val sharedPrefs = context.getSharedPreferences("email_auth", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("pending_email", email).apply()
            
            emit(AuthResult.Success(
                User(
                    id = "",
                    email = email,
                    name = "Email sent successfully"
                )
            ))
        } catch (e: Exception) {
            emit(AuthResult.Error("Failed to send email link: ${e.message}"))
        }
    }

    suspend fun signInWithEmailLink(email: String, emailLink: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        
        try {
            // Sign in with Firebase using email link
            val result = firebaseAuth.signInWithEmailLink(email, emailLink).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Get the ID token for backend authentication
                val idToken = firebaseUser.getIdToken(false).await()
                val token = idToken.token
                
                if (token != null) {
                    // Authenticate with backend using Firebase token
                    val backendResult = authenticateWithFirebaseToken(token)
                    emit(backendResult)
                } else {
                    emit(AuthResult.Error("Failed to get Firebase token"))
                }
            } else {
                emit(AuthResult.Error("Failed to sign in with email link"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Email link sign-in failed: ${e.message}"))
        }
    }

    private suspend fun authenticateWithGoogle(googleToken: String): AuthResult {
        return try {
            val response = authApi.googleLogin(GoogleLoginRequest(googleToken))
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    // Store tokens
                    tokenManager.saveAccessToken(authResponse.access_token)
                    
                    AuthResult.Success(authResponse.user)
                } else {
                    AuthResult.Error("Empty response from server")
                }
            } else {
                AuthResult.Error("Authentication failed: ${response.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun authenticateWithFirebaseToken(firebaseToken: String): AuthResult {
        return try {
            val response = authApi.firebaseLogin(FirebaseLoginRequest(firebaseToken))
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    // Store tokens
                    tokenManager.saveAccessToken(authResponse.access_token)
                    
                    AuthResult.Success(authResponse.user)
                } else {
                    AuthResult.Error("Empty response from server")
                }
            } else {
                AuthResult.Error("Authentication failed: ${response.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun signOut() {
        try {
            // Sign out from Firebase
            firebaseAuth.signOut()
            
            // Sign out from Google Identity Services
            signInClient.signOut()
            
            // Clear stored tokens
            tokenManager.clearTokens()
            
            println("AuthRepository: Sign out completed")
        } catch (e: Exception) {
            println("AuthRepository: Error during sign out: ${e.message}")
        }
    }

    suspend fun getAuthState(): Flow<AuthState> = flow {
        val currentUser = firebaseAuth.currentUser
        val hasValidTokens = tokenManager.hasValidAccessToken()
        
        when {
            currentUser != null && hasValidTokens -> {
                println("AuthRepository: User is authenticated with valid tokens")
                emit(AuthState.Authenticated)
            }
            currentUser != null && !hasValidTokens -> {
                println("AuthRepository: Firebase user exists but no valid tokens")
                emit(AuthState.Loading)
                // User is signed into Firebase but doesn't have valid backend tokens
                // Try to authenticate with Google token
                try {
                    // For GIS, we need to handle this differently since we don't have a persistent Google account
                    // We'll need to re-authenticate through the UI flow
                    println("AuthRepository: No persistent Google account available, signing out")
                    signOut()
                    emit(AuthState.Unauthenticated)
                } catch (e: Exception) {
                    println("AuthRepository: Authentication attempt failed: ${e.message}")
                    signOut()
                    emit(AuthState.Unauthenticated)
                }
            }
            else -> {
                println("AuthRepository: No Firebase user or tokens, user is unauthenticated")
                emit(AuthState.Unauthenticated)
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return try {
            println("AuthRepository: Checking username availability for: $username")
            val response = authApi.checkUsernameAvailability(CheckUsernameRequest(username))
            println("AuthRepository: Username check response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val availability = response.body()
                println("AuthRepository: Username availability response: $availability")
                availability?.available ?: false
            } else {
                val errorBody = response.errorBody()?.string()
                println("AuthRepository: Username check error: ${response.code()} - $errorBody")
                val errorMessage = when (response.code()) {
                    400 -> "Invalid username format"
                    401 -> "Authentication required"
                    409 -> "Username already taken"
                    500 -> "Server error occurred"
                    else -> "Failed to check username availability: ${response.message()}"
                }
                throw Exception(errorMessage)
            }
        } catch (e: Exception) {
            println("AuthRepository: Exception during username check: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun setupUser(
        username: String,
        dietaryRestrictions: List<String> = emptyList(),
        preferences: Map<String, String> = emptyMap()
    ): User {
        return try {
            println("AuthRepository: Setting up user with username: $username")
            val response = authApi.setupUser(
                UserSetupRequest(
                    username = username,
                    dietary_restrictions = dietaryRestrictions,
                    preferences = preferences
                )
            )
            println("AuthRepository: Setup user response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val user = response.body()
                println("AuthRepository: Setup user successful: $user")
                if (user != null) {
                    user
                } else {
                    throw Exception("Invalid response")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("AuthRepository: Setup user error: ${response.code()} - $errorBody")
                
                // Try to parse error response for better error messages
                val errorMessage = when (response.code()) {
                    409 -> "Username already taken or user already has an account"
                    400 -> "Invalid username or request data"
                    401 -> "Authentication required"
                    500 -> "Server error occurred"
                    else -> "Failed to setup user: ${response.message()}"
                }
                throw Exception(errorMessage)
            }
        } catch (e: Exception) {
            println("AuthRepository: Exception during user setup: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getCurrentUserData(): User? {
        return try {
            println("AuthRepository: getCurrentUserData() - attempting to get user data")
            val response = authApi.getCurrentUser()
            println("AuthRepository: getCurrentUserData() - response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val userResponse = response.body()
                val user = userResponse?.user
                println("AuthRepository: getCurrentUserData() - user: ${user?.email}")
                user
            } else {
                println("AuthRepository: getCurrentUserData() - failed, user needs to re-authenticate")
                // Backend call failed, user needs to re-authenticate through the UI
                null
            }
        } catch (e: Exception) {
            println("AuthRepository: getCurrentUserData() - exception: ${e.message}")
            // Network error - user needs to re-authenticate through the UI
            null
        }
    }
} 