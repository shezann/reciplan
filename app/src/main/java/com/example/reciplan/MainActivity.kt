package com.example.reciplan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.reciplan.ui.auth.ChooseUsernameScreen
import com.example.reciplan.ui.auth.LoginScreen
import com.example.reciplan.ui.recipe.CreateRecipeScreen
import com.example.reciplan.ui.recipe.EditRecipeScreen
import com.example.reciplan.ui.recipe.RecipeDetailScreen
import com.example.reciplan.ui.recipe.RecipeScreenDevelopment
import com.example.reciplan.ui.recipe.RecipeScreenDebug
import com.example.reciplan.ui.main.MainScreen
import com.example.reciplan.ui.splash.SplashScreen
import com.example.reciplan.ui.theme.ReciplanTheme
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.reciplan.ui.auth.AuthViewModel
import com.example.reciplan.ui.recipe.RecipeViewModel
import com.example.reciplan.ui.splash.SplashViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    
    private lateinit var backPressedCallback: OnBackPressedCallback
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Configure system UI for better splash screen experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize back button callback
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // This will be updated based on current screen
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        
        // Handle email link on app launch
        handleEmailLinkIfPresent(intent)
        
        setContent {
            ReciplanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReciplanApp { isOnSplashScreen ->
                        // Update back button behavior based on current screen
                        if (isOnSplashScreen) {
                            // On splash screen, back button exits the app
                            backPressedCallback.isEnabled = true
                        } else {
                            // On other screens, use default behavior
                            backPressedCallback.isEnabled = false
                        }
                    }
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
            RecipeViewModel::class.java -> {
                RecipeViewModel(appContainer.recipeRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}

@Composable
fun ReciplanApp(onScreenChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Track current destination to update back button behavior
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry.value?.destination?.route
    
    // Update back button behavior based on current screen
    LaunchedEffect(currentDestination) {
        onScreenChanged(currentDestination == "splash")
    }
    
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
