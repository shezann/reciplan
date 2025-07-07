package com.example.reciplan.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenManager(
    private val context: Context
) {
    
    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        EncryptedSharedPreferences.create(
            "auth_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
    }

    fun saveTokens(accessToken: String, refreshToken: String = "") {
        println("TokenManager: Saving tokens - accessToken: ${accessToken.take(20)}...")
        prefs.edit().apply {
            putString(ACCESS_TOKEN_KEY, accessToken)
            if (refreshToken.isNotEmpty()) {
                putString(REFRESH_TOKEN_KEY, refreshToken)
            }
            // Set expiry time to 1 hour from now (JWT tokens typically expire in 1 hour)
            val expiryTime = System.currentTimeMillis() + (60 * 60 * 1000)
            putLong(TOKEN_EXPIRY_KEY, expiryTime)
            apply()
        }
        println("TokenManager: Tokens saved successfully. Expiry: ${java.util.Date(prefs.getLong(TOKEN_EXPIRY_KEY, 0L))}")
    }

    fun getAccessToken(): String? {
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(REFRESH_TOKEN_KEY, null)
    }

    fun hasValidTokens(): Boolean {
        val accessToken = getAccessToken()
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0L)
        val currentTime = System.currentTimeMillis()
        
        // Add a buffer of 5 minutes to prevent using tokens that are about to expire
        val bufferTime = 5 * 60 * 1000 // 5 minutes in milliseconds
        val isValid = accessToken != null && (currentTime + bufferTime) < expiryTime
        
        println("TokenManager: hasValidTokens() check:")
        println("  - Access token exists: ${accessToken != null}")
        println("  - Access token preview: ${accessToken?.take(20)}...")
        println("  - Current time: ${java.util.Date(currentTime)}")
        println("  - Expiry time: ${java.util.Date(expiryTime)}")
        println("  - Buffer time: 5 minutes")
        println("  - Is valid (with buffer): $isValid")
        
        // If token is considered invalid, clear it
        if (!isValid && accessToken != null) {
            println("TokenManager: Clearing invalid/expired tokens")
            clearTokens()
        }
        
        return isValid
    }

    fun isTokenExpired(): Boolean {
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0L)
        return System.currentTimeMillis() >= expiryTime
    }

    fun clearTokens() {
        prefs.edit().apply {
            remove(ACCESS_TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            remove(TOKEN_EXPIRY_KEY)
            apply()
        }
    }
} 