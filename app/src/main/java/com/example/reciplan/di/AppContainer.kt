package com.example.reciplan.di

import android.content.Context
import com.example.reciplan.BuildConfig
import com.example.reciplan.data.api.AuthApi
import com.example.reciplan.data.api.IngestApi
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.auth.AuthRepository
import com.example.reciplan.data.auth.TokenManager
import com.example.reciplan.data.network.AuthInterceptor
import com.example.reciplan.data.repository.IngestRepository
import com.example.reciplan.data.repository.LikeRepository
import com.example.reciplan.data.repository.PagingRecipeRepository
import com.example.reciplan.data.recipe.RecipeRepository
import com.example.reciplan.ui.ingest.AddFromTikTokViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Application-level dependency injection container
 * Provides singleton instances of repositories, APIs, and ViewModels
 */
class AppContainer(private val context: Context) {
    
    // Application-scoped coroutine scope for long-lived operations
    private val applicationScope = CoroutineScope(SupervisorJob())
    
    // Authentication and security
    private val tokenManager: TokenManager by lazy {
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
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }
    
    // APIs
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
    
    val recipeApi: RecipeApi by lazy {
        retrofit.create(RecipeApi::class.java)
    }
    
    val ingestApi: IngestApi by lazy {
        retrofit.create(IngestApi::class.java)
    }
    
    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepository(context, authApi, tokenManager)
    }
    
    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(recipeApi)
    }
    
    val ingestRepository: IngestRepository by lazy {
        IngestRepository(ingestApi)
    }
    
    val likeRepository: LikeRepository by lazy {
        LikeRepository(recipeApi)
    }
    
    val pagingRecipeRepository: PagingRecipeRepository by lazy {
        PagingRecipeRepository(recipeApi, likeRepository, applicationScope)
    }
    
    // ViewModels
    fun createHomeViewModel(): com.example.reciplan.ui.home.HomeViewModel {
        return com.example.reciplan.ui.home.HomeViewModel(pagingRecipeRepository, likeRepository)
    }
    
    fun createAddFromTikTokViewModel(): AddFromTikTokViewModel {
        return AddFromTikTokViewModel.getSharedInstance(ingestRepository)
    }
    
    fun createDraftPreviewViewModel(): com.example.reciplan.ui.draft.DraftPreviewViewModel {
        return com.example.reciplan.ui.draft.DraftPreviewViewModel(ingestRepository, recipeRepository)
    }
    
    // For debug/testing purposes
    fun provideTokenManager(): TokenManager = tokenManager
} 
