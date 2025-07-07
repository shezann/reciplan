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
            url.contains("/health") ||
            url.contains("/api/status")) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenManager.getAccessToken()
        
        println("AuthInterceptor: Request to ${originalRequest.url}")
        println("AuthInterceptor: Access token available: ${accessToken != null}")
        println("AuthInterceptor: Token preview: ${accessToken?.take(20)}...")
        
        // If no token, proceed without authorization
        if (accessToken == null) {
            println("AuthInterceptor: No token available, proceeding without auth")
            return chain.proceed(originalRequest)
        }

        // Add authorization header
        val requestWithAuth = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        println("AuthInterceptor: Added Authorization header")
        val response = chain.proceed(requestWithAuth)
        println("AuthInterceptor: Response code: ${response.code}")

        // If we get 401, clear tokens (no refresh token endpoint available)
        if (response.code == 401) {
            tokenManager.clearTokens()
        }

        return response
    }
} 