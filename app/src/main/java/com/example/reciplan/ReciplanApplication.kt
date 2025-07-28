package com.example.reciplan

import android.app.Application
import com.example.reciplan.di.AppContainer

class ReciplanApplication : Application() {
    
    // Application-level dependency injection container
    lateinit var appContainer: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
} 