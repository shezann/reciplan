package com.example.reciplan.data.api

import com.example.reciplan.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    
    @POST("api/auth/firebase-login")
    suspend fun firebaseLogin(
        @Body request: FirebaseLoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/check-username")
    suspend fun checkUsernameAvailability(
        @Body request: CheckUsernameRequest
    ): Response<UsernameAvailabilityResponse>

    @POST("api/auth/setup")
    suspend fun setupUser(
        @Body request: UserSetupRequest
    ): Response<User>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<CurrentUserResponse>

    @PUT("api/auth/update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<User>

    @GET("api/auth/check-token")
    suspend fun validateToken(): Response<TokenValidationResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    @GET("api/status")
    suspend fun getApiStatus(): Response<ApiStatusResponse>
} 