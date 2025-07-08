package com.example.reciplan.di

import android.content.Context
import com.example.reciplan.data.api.AuthApi
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.auth.AuthRepository
import com.example.reciplan.data.auth.TokenManager
import com.example.reciplan.data.network.AuthInterceptor
import com.example.reciplan.data.recipe.RecipeRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.example.reciplan.BuildConfig

/**
 * Simple manual dependency injection container
 * This replaces Hilt temporarily to get the app running
 */
class AppContainer(private val context: Context) {
    
    // Lazy initialization for dependencies
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
            allowStructuredMapKeys = true
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = false
        }
    }
    
    val tokenManager: TokenManager by lazy {
        TokenManager(context)
    }
    
    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(tokenManager)
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
    
    val recipeApi: RecipeApi by lazy {
        retrofit.create(RecipeApi::class.java)
    }
    
    val authRepository: AuthRepository by lazy {
        AuthRepository(context, authApi, tokenManager)
    }
    
    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(recipeApi)
    }
} 