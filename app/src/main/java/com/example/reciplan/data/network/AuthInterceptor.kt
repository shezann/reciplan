package com.example.reciplan.data.network

import com.example.reciplan.data.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip authentication for certain endpoints
        val url = originalRequest.url.toString()
        if (url.contains("/api/auth/firebase-login") || 
            url.contains("/api/auth/google") || 
            url.contains("/api/auth/check-username") ||
            url.contains("/health") ||
            url.contains("/api/status")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenManager.getAccessToken()
        
        // If no token, proceed without authorization
        if (accessToken == null) {
            return chain.proceed(originalRequest)
        }

        // Add authorization header
        val requestWithAuth = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(requestWithAuth)

        // If we get 401, clear tokens (no refresh token endpoint available)
        if (response.code == 401) {
            tokenManager.clearTokens()
        }

        return response
    }
} 