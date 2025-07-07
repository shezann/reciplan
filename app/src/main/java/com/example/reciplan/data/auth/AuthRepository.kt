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
            // First, authenticate with Firebase using Google credentials
            val googleToken = account.idToken
            if (googleToken != null) {
                // Sign into Firebase with Google credentials
                val credential = GoogleAuthProvider.getCredential(googleToken, null)
                val firebaseResult = firebaseAuth.signInWithCredential(credential).await()
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
            println("AuthRepository: Sending Google token to backend...")
            val response = authApi.googleLogin(GoogleLoginRequest(googleToken))
            println("AuthRepository: Backend response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                println("AuthRepository: Backend response body: $authResponse")
                
                if (authResponse != null) {
                    println("AuthRepository: Saving access token: ${authResponse.access_token?.take(20)}...")
                    tokenManager.saveTokens(authResponse.access_token, "") // No refresh token in this API
                    
                    // Use the user data from the auth response and consider setup_required flag
                    val user = authResponse.user.copy(
                        setup_complete = !authResponse.setup_required // If setup not required, then it's complete
                    )
                    println("AuthRepository: User authenticated successfully: ${user.id}")
                    AuthResult.Success(user)
                } else {
                    println("AuthRepository: Null response body from backend")
                    AuthResult.Error("Invalid response from server")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("AuthRepository: Backend error: ${response.code()} - $errorBody")
                AuthResult.Error("Authentication failed: ${response.message()}")
            }
        } catch (e: Exception) {
            println("AuthRepository: Exception during Google auth: ${e.message}")
            e.printStackTrace()
            AuthResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            println("AuthRepository: Checking username availability for: $username")
            val response = authApi.checkUsernameAvailability(CheckUsernameRequest(username))
            println("AuthRepository: Username check response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val availability = response.body()
                println("AuthRepository: Username availability response: $availability")
                Result.success(availability?.available ?: false)
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
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("AuthRepository: Exception during username check: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun setupUser(
        username: String,
        dietaryRestrictions: List<String> = emptyList(),
        preferences: Map<String, String> = emptyMap()
    ): Result<User> {
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
                    Result.success(user)
                } else {
                    Result.failure(Exception("Invalid response"))
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
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("AuthRepository: Exception during user setup: ${e.message}")
            e.printStackTrace()
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
        
        println("AuthRepository: getAuthState() - Firebase user: ${currentUser?.email}, hasValidTokens: $hasValidTokens")
        
        when {
            currentUser != null && hasValidTokens -> {
                emit(AuthState.Loading)
                try {
                    // Validate tokens with backend by trying to get current user data
                    val response = authApi.getCurrentUser()
                    println("AuthRepository: Backend user data response code: ${response.code()}")
                    
                    if (response.isSuccessful) {
                        val userResponse = response.body()
                        val user = userResponse?.user
                        if (user != null) {
                            println("AuthRepository: Backend authentication valid - user: ${user.email}")
                            emit(AuthState.Authenticated)
                        } else {
                            println("AuthRepository: Backend returned null user, signing out")
                            signOut()
                            emit(AuthState.Unauthenticated)
                        }
                    } else {
                        println("AuthRepository: Backend authentication failed with code: ${response.code()}")
                        // Backend authentication failed - clear tokens and sign out
                        signOut()
                        emit(AuthState.Unauthenticated)
                    }
                } catch (e: Exception) {
                    println("AuthRepository: Network error during auth validation: ${e.message}")
                    // Network error - clear tokens and sign out to force fresh authentication
                    signOut()
                    emit(AuthState.Unauthenticated)
                }
            }
            currentUser != null && !hasValidTokens -> {
                println("AuthRepository: Firebase user exists but no valid tokens")
                emit(AuthState.Loading)
                // User is signed into Firebase but doesn't have valid backend tokens
                // Try to authenticate with Google token
                try {
                    val googleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                    if (googleAccount != null && googleAccount.idToken != null) {
                        println("AuthRepository: Attempting authentication with Google token")
                        val authResult = authenticateWithGoogle(googleAccount.idToken!!)
                        if (authResult is AuthResult.Success) {
                            println("AuthRepository: Authentication successful")
                            emit(AuthState.Authenticated)
                        } else {
                            println("AuthRepository: Authentication failed, signing out")
                            signOut()
                            emit(AuthState.Unauthenticated)
                        }
                    } else {
                        println("AuthRepository: No Google account available, signing out")
                        signOut()
                        emit(AuthState.Unauthenticated)
                    }
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
                println("AuthRepository: getCurrentUserData() - failed, attempting re-authentication")
                // Backend call failed, try to re-authenticate with Google token
                val googleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                if (googleAccount != null && googleAccount.idToken != null) {
                    println("AuthRepository: Re-authenticating with Google token")
                    val authResult = authenticateWithGoogle(googleAccount.idToken!!)
                    if (authResult is AuthResult.Success) {
                        println("AuthRepository: Re-authentication successful, returning user")
                        authResult.user
                    } else {
                        println("AuthRepository: Re-authentication failed")
                        null
                    }
                } else {
                    println("AuthRepository: No Google account available for re-authentication")
                    null
                }
            }
        } catch (e: Exception) {
            println("AuthRepository: getCurrentUserData() - exception: ${e.message}")
            // Network error - try to re-authenticate with Google token
            try {
                val googleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                if (googleAccount != null && googleAccount.idToken != null) {
                    println("AuthRepository: Re-authenticating with Google token after network error")
                    val authResult = authenticateWithGoogle(googleAccount.idToken!!)
                    if (authResult is AuthResult.Success) {
                        println("AuthRepository: Re-authentication successful after network error")
                        authResult.user
                    } else {
                        println("AuthRepository: Re-authentication failed after network error")
                        null
                    }
                } else {
                    println("AuthRepository: No Google account available for re-authentication after network error")
                    null
                }
            } catch (reAuthException: Exception) {
                println("AuthRepository: Re-authentication failed with exception: ${reAuthException.message}")
                null
            }
        }
    }
} 