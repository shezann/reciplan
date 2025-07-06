package com.example.reciplan.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reciplan.data.api.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HealthState {
    object Loading : HealthState()
    object Success : HealthState()
    data class Error(val message: String) : HealthState()
}

class SplashViewModel(
    private val authApi: AuthApi
) : ViewModel() {

    private val _healthState = MutableStateFlow<HealthState>(HealthState.Loading)
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    init {
        checkHealth()
    }

    private fun checkHealth() {
        viewModelScope.launch {
            try {
                val response = authApi.healthCheck()
                if (response.isSuccessful) {
                    _healthState.value = HealthState.Success
                } else {
                    _healthState.value = HealthState.Error("Server responded with error: ${response.code()}")
                }
            } catch (e: Exception) {
                _healthState.value = HealthState.Error("Network error: ${e.message}")
            }
        }
    }
} 