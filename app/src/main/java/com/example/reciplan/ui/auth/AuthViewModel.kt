package com.example.reciplan.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.auth.AuthRepository
import com.example.reciplan.data.auth.AuthResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Loading)
    val authState: StateFlow<AuthResult> = _authState.asStateFlow()

    private val _usernameState = MutableStateFlow<UsernameState>(UsernameState.Idle)
    val usernameState: StateFlow<UsernameState> = _usernameState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.getAuthState().collect { state ->
                when (state) {
                    is com.example.reciplan.data.auth.AuthState.Authenticated -> {
                        // Fetch the actual user data from backend
                        val userData = authRepository.getCurrentUserData()
                        if (userData != null) {
                            // Set setup_complete based on whether user has a username
                            val userWithSetupFlag = userData.copy(
                                setup_complete = !userData.username.isNullOrBlank()
                            )
                            println("AuthViewModel: User data retrieved - username: ${userData.username}, setup_complete: ${userWithSetupFlag.setup_complete}")
                            _authState.value = AuthResult.Success(userWithSetupFlag)
                        } else {
                            println("AuthViewModel: Backend data not available, this shouldn't happen if authenticated")
                            // If we're authenticated but can't get user data, there's a problem
                            // Sign out and let the user re-authenticate
                            authRepository.signOut()
                            _authState.value = AuthResult.Error("Authentication failed, please sign in again")
                        }
                    }
                    is com.example.reciplan.data.auth.AuthState.Unauthenticated -> {
                        _authState.value = AuthResult.Error("User not authenticated")
                    }
                    is com.example.reciplan.data.auth.AuthState.Loading -> {
                        _authState.value = AuthResult.Loading
                    }
                }
            }
        }
    }

    fun sendEmailLink(email: String) {
        viewModelScope.launch {
            authRepository.sendEmailLink(email).collect { result ->
                _authState.value = result
            }
        }
    }

    fun getGoogleSignInIntent(): android.content.Intent? {
        return try {
            val googleSignInClient = authRepository.getGoogleSignInClient()
            googleSignInClient.signInIntent
        } catch (e: Exception) {
            _authState.value = AuthResult.Error("Google sign-in failed: ${e.message}")
            null
        }
    }

    fun setLoading() {
        _authState.value = AuthResult.Loading
    }

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                
                authRepository.signInWithGoogle(account).collect { result ->
                    _authState.value = result
                }
            } catch (e: ApiException) {
                _authState.value = AuthResult.Error("Google sign-in failed: ${e.message}")
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _usernameState.value = UsernameState.Checking
            
            val result = authRepository.checkUsernameAvailability(username)
            result.fold(
                onSuccess = { available ->
                    _usernameState.value = if (available) {
                        UsernameState.Available
                    } else {
                        UsernameState.Unavailable
                    }
                },
                onFailure = { error ->
                    _usernameState.value = UsernameState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun setupUser(
        username: String,
        dietaryRestrictions: List<String> = emptyList(),
        preferences: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            _usernameState.value = UsernameState.Setting
            
            val result = authRepository.setupUser(username, dietaryRestrictions, preferences)
            result.fold(
                onSuccess = { user ->
                    _usernameState.value = UsernameState.Set
                    _authState.value = AuthResult.Success(user)
                },
                onFailure = { error ->
                    _usernameState.value = UsernameState.Error(error.message ?: "Failed to setup user")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthResult.Loading
        }
    }

    // Method to clear all authentication state and force fresh login
    fun clearAuthenticationState() {
        viewModelScope.launch {
            println("AuthViewModel: Clearing all authentication state")
            authRepository.signOut()
            _authState.value = AuthResult.Error("Please sign in again")
        }
    }
}

sealed class UsernameState {
    object Idle : UsernameState()
    object Checking : UsernameState()
    object Available : UsernameState()
    object Unavailable : UsernameState()
    object Setting : UsernameState()
    object Set : UsernameState()
    data class Error(val message: String) : UsernameState()
} 