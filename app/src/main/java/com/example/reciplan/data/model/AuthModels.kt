package com.example.reciplan.data.model

import kotlinx.serialization.Serializable

// Authentication Requests
@Serializable
data class FirebaseLoginRequest(
    val firebase_token: String
)

@Serializable
data class GoogleLoginRequest(
    val google_token: String
)

@Serializable
data class CheckUsernameRequest(
    val username: String
)

@Serializable
data class UserSetupRequest(
    val username: String,
    val dietary_restrictions: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap()
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val dietary_restrictions: List<String>? = null,
    val preferences: Map<String, String>? = null
)

// Authentication Responses
@Serializable
data class AuthResponse(
    val user: User,
    val access_token: String,
    val setup_required: Boolean
)

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val name: String? = null,
    val username: String? = null,
    val photoUrl: String? = null,
    val emailVerified: Boolean = false,
    val dietary_restrictions: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    @kotlinx.serialization.SerialName("setup_completed")
    val setup_complete: Boolean = false,
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class CurrentUserResponse(
    val user: User
)

@Serializable
data class UsernameAvailabilityResponse(
    val available: Boolean
)

@Serializable
data class TokenValidationResponse(
    val valid: Boolean,
    val user: User? = null
)

@Serializable
data class LogoutResponse(
    val message: String
)

// System Responses
@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val features: List<String>
)

@Serializable
data class ApiStatusResponse(
    val api_version: String,
    val service: String,
    val status: String,
    val endpoints: Map<String, String>,
    val features: Map<String, Boolean>
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null
)