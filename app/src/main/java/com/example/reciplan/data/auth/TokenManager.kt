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

    fun saveTokens(
        accessToken: String,
        refreshToken: String? = null,
        expiresIn: Long = 3600 // Default 1 hour
    ) {
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + (expiresIn * 1000) // Convert to milliseconds
        
        prefs.edit()
            .putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .putLong(TOKEN_EXPIRY_KEY, expiryTime)
            .apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(REFRESH_TOKEN_KEY, null)
    }

    fun hasValidTokens(): Boolean {
        val accessToken = prefs.getString(ACCESS_TOKEN_KEY, null)
        val currentTime = System.currentTimeMillis()
        val expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0L)
        
        // Add 5 minute buffer to account for network delays
        val bufferTime = 5 * 60 * 1000L // 5 minutes in milliseconds
        val isValid = accessToken != null && currentTime < (expiryTime - bufferTime)
        
        if (!isValid) {
            // Clear invalid tokens
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