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
        prefs.edit().apply {
            putString(ACCESS_TOKEN_KEY, accessToken)
            if (refreshToken.isNotEmpty()) {
                putString(REFRESH_TOKEN_KEY, refreshToken)
            }
            // Set expiry time to 1 hour from now (JWT tokens typically expire in 1 hour)
            putLong(TOKEN_EXPIRY_KEY, System.currentTimeMillis() + (60 * 60 * 1000))
            apply()
        }
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
        
        return accessToken != null && 
               System.currentTimeMillis() < expiryTime
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