package com.example.reciplan.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.auth.AuthRepository
import com.example.reciplan.data.auth.AuthResult
import com.google.android.gms.auth.api.identity.SignInCredential
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
            
                            _authState.value = AuthResult.Success(userWithSetupFlag)
                        } else {

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

    fun getSignInClient() = authRepository.getSignInClient()

    fun setLoading() {
        _authState.value = AuthResult.Loading
    }

    fun handleGoogleSignInResult(credential: SignInCredential) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(credential).collect { result ->
                _authState.value = result
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _usernameState.value = UsernameState.Checking
            
            try {
                val isAvailable = authRepository.checkUsernameAvailability(username)
                _usernameState.value = if (isAvailable) {
                    UsernameState.Available
                } else {
                    UsernameState.Taken
                }
            } catch (e: Exception) {
                _usernameState.value = UsernameState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setupUser(username: String, dietaryRestrictions: List<String> = emptyList(), preferences: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            
            try {
                val user = authRepository.setupUser(username, dietaryRestrictions, preferences)
                _authState.value = AuthResult.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthResult.Error(e.message ?: "Failed to setup user")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthResult.Error("User not authenticated")
        }
    }

    fun clearUsernameState() {
        _usernameState.value = UsernameState.Idle
    }
}

sealed class UsernameState {
    object Idle : UsernameState()
    object Checking : UsernameState()
    object Available : UsernameState()
    object Taken : UsernameState()
    data class Error(val message: String) : UsernameState()
} 