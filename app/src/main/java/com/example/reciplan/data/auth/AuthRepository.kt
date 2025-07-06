package com.example.reciplan.data.auth

import android.content.Context
import com.example.reciplan.data.api.AuthApi
import com.example.reciplan.data.model.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("771971890295-5gbmmottvhno7sg422jplfeatn6usnsf.apps.googleusercontent.com")
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        
        try {
            // Use Google token directly with backend
            val googleToken = account.idToken
            if (googleToken != null) {
                val backendResult = authenticateWithGoogle(googleToken)
                emit(backendResult)
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
            val result = firebaseAuth.signInWithEmailLink(email, emailLink).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                val idToken = firebaseUser.getIdToken(false).await().token
                if (idToken != null) {
                    val backendResult = authenticateWithFirebase(idToken)
                    emit(backendResult)
                } else {
                    emit(AuthResult.Error("Failed to get Firebase ID token"))
                }
            } else {
                emit(AuthResult.Error("Email link authentication failed"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Email link sign-in failed: ${e.message}"))
        }
    }

    private suspend fun authenticateWithFirebase(idToken: String): AuthResult {
        return try {
            val response = authApi.firebaseLogin(FirebaseLoginRequest(idToken))
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    tokenManager.saveTokens(authResponse.access_token, "") // No refresh token in this API
                    
                    // Use the user data from the auth response and consider setup_required flag
                    val user = authResponse.user.copy(
                        setup_complete = !authResponse.setup_required // If setup not required, then it's complete
                    )
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Invalid response from server")
                }
            } else {
                AuthResult.Error("Authentication failed: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    private suspend fun authenticateWithGoogle(googleToken: String): AuthResult {
        return try {
            val response = authApi.googleLogin(GoogleLoginRequest(googleToken))
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    tokenManager.saveTokens(authResponse.access_token, "") // No refresh token in this API
                    
                    // Use the user data from the auth response and consider setup_required flag
                    val user = authResponse.user.copy(
                        setup_complete = !authResponse.setup_required // If setup not required, then it's complete
                    )
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Invalid response from server")
                }
            } else {
                AuthResult.Error("Authentication failed: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val response = authApi.checkUsernameAvailability(CheckUsernameRequest(username))
            if (response.isSuccessful) {
                val availability = response.body()
                Result.success(availability?.available ?: false)
            } else {
                Result.failure(Exception("Failed to check username availability"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setupUser(
        username: String,
        dietaryRestrictions: List<String> = emptyList(),
        preferences: Map<String, String> = emptyMap()
    ): Result<User> {
        return try {
            val response = authApi.setupUser(
                UserSetupRequest(
                    username = username,
                    dietary_restrictions = dietaryRestrictions,
                    preferences = preferences
                )
            )
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Invalid response"))
                }
            } else {
                // Try to parse error response for better error messages
                val errorMessage = when (response.code()) {
                    409 -> "Username already taken or user already has an account"
                    400 -> "Invalid username or request data"
                    401 -> "Authentication required"
                    500 -> "Server error occurred"
                    else -> "Failed to setup user: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            tokenManager.clearTokens()
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun getAuthState(): Flow<AuthState> = flow {
        val currentUser = firebaseAuth.currentUser
        val hasValidTokens = tokenManager.hasValidTokens()
        
        when {
            currentUser != null && hasValidTokens -> {
                emit(AuthState.Loading)
                try {
                    // Fetch the actual user data from backend
                    val response = authApi.getCurrentUser()
                    if (response.isSuccessful) {
                        val user = response.body()
                        if (user != null) {
                            // Update the auth state with the actual user data
                            emit(AuthState.Authenticated)
                        } else {
                            emit(AuthState.Unauthenticated)
                        }
                    } else {
                        // If we can't get user data, but tokens are valid, still consider authenticated
                        // but with limited data
                        emit(AuthState.Authenticated)
                    }
                } catch (e: Exception) {
                    // Network error - assume authenticated if we have valid tokens
                    emit(AuthState.Authenticated)
                }
            }
            else -> emit(AuthState.Unauthenticated)
        }
    }

    suspend fun getCurrentUserData(): User? {
        return try {
            val response = authApi.getCurrentUser()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
} 