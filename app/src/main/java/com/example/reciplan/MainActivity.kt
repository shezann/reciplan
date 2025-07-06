package com.example.reciplan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.reciplan.ui.auth.ChooseUsernameScreen
import com.example.reciplan.ui.auth.LoginScreen
import com.example.reciplan.ui.splash.SplashScreen
import com.example.reciplan.ui.theme.ReciplanTheme
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.reciplan.ui.auth.AuthViewModel
import com.example.reciplan.ui.splash.SplashViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle email link on app launch
        handleEmailLinkIfPresent(intent)
        
        setContent {
            ReciplanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReciplanApp()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle email link when app is already running
        handleEmailLinkIfPresent(intent)
    }
    
    private fun handleEmailLinkIfPresent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "https" && data.host == "reciplan-c3e17.firebaseapp.com") {
            // This is an email link, handle it
            val emailLink = data.toString()
            
            // Get the saved email from SharedPreferences
            val sharedPrefs = getSharedPreferences("email_auth", MODE_PRIVATE)
            val email = sharedPrefs.getString("pending_email", null)
            
            if (email != null && FirebaseAuth.getInstance().isSignInWithEmailLink(emailLink)) {
                // Process the email link sign-in
                val application = applicationContext as ReciplanApplication
                val authRepository = application.appContainer.authRepository
                
                CoroutineScope(Dispatchers.Main).launch {
                    authRepository.signInWithEmailLink(email, emailLink).collect { result ->
                        // The result will be handled by the auth flow
                    }
                }
                
                // Clear the pending email
                sharedPrefs.edit().remove("pending_email").apply()
            }
        }
    }
}

// Custom ViewModelFactory for manual DI
class ViewModelFactory(private val appContainer: com.example.reciplan.di.AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            AuthViewModel::class.java -> {
                AuthViewModel(appContainer.authRepository) as T
            }
            SplashViewModel::class.java -> {
                SplashViewModel(appContainer.authApi) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}

@Composable
fun ReciplanApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Get the app container from the application
    val application = context.applicationContext as ReciplanApplication
    val viewModelFactory = ViewModelFactory(application.appContainer)
    
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    // Navigate to MainFragmentActivity instead of placeholder
                    val intent = Intent(context, MainFragmentActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                },
                onNavigateToUsername = {
                    navController.navigate("username") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate to MainFragmentActivity instead of placeholder
                    val intent = Intent(context, MainFragmentActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                },
                onNavigateToRegister = {
                    navController.navigate("username") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable("username") {
            ChooseUsernameScreen(
                onUsernameSet = {
                    // Navigate to MainFragmentActivity instead of placeholder
                    val intent = Intent(context, MainFragmentActivity::class.java)
                    context.startActivity(intent)
                    (context as ComponentActivity).finish()
                },
                viewModelFactory = viewModelFactory
            )
        }
    }
}