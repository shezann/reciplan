package com.example.reciplan

import android.app.Application
import com.example.reciplan.di.AppContainer
import com.example.reciplan.data.auth.TokenManager

class ReciplanApplication : Application() {
    
    // Manual dependency injection container
    lateinit var appContainer: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        val tokenManager = TokenManager(this)
        appContainer = AppContainer(this, tokenManager)
    }
} 