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
import com.example.reciplan.ui.draft.DraftPreviewScreen
import com.example.reciplan.ui.draft.DraftPreviewViewModel
import com.example.reciplan.ui.ingest.AddFromTikTokScreen
import com.example.reciplan.ui.ingest.IngestStatusScreen
import com.example.reciplan.ui.theme.ReciplanTheme
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.reciplan.ui.auth.AuthViewModel
import com.example.reciplan.ui.recipe.RecipeViewModel
import com.example.reciplan.ui.splash.SplashViewModel
import com.example.reciplan.ui.ingest.AddFromTikTokViewModel
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
    
    override fun onPause() {
        super.onPause()
        // Clear shared instance when app is paused to prevent stale state issues
        AddFromTikTokViewModel.clearSharedInstance()
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
                RecipeViewModel(appContainer.recipeRepository, appContainer.likeRepository, appContainer.authRepository) as T
            }
            AddFromTikTokViewModel::class.java -> {
                AddFromTikTokViewModel.getSharedInstance(appContainer.ingestRepository) as T
            }
            DraftPreviewViewModel::class.java -> {
                appContainer.createDraftPreviewViewModel() as T
            }
            com.example.reciplan.ui.profile.ProfileViewModel::class.java -> {
                appContainer.createProfileViewModel() as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}

@Composable
fun ReciplanApp() {
    val navController = rememberNavController()
    
    // Get the app container from the application
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as ReciplanApplication
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
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
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
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
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
                    navController.navigate("main") {
                        popUpTo("username") { inclusive = true }
                    }
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable("main") {
            MainScreen(
                onNavigateToCreateRecipe = {
                    navController.navigate("create_recipe")
                },
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate("recipe_detail/$recipeId")
                },
                onNavigateToEditRecipe = { recipeId ->
                    navController.navigate("edit_recipe/$recipeId")
                },
                onNavigateToAddFromTikTok = {
                    navController.navigate("add_from_tiktok")
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable("add_from_tiktok") {
            AddFromTikTokScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStatus = { jobId ->
                    navController.navigate("ingest_status/$jobId")
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable(
            route = "ingest_status/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            IngestStatusScreen(
                jobId = jobId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDraftPreview = { recipeId ->
                    navController.navigate("draftPreview/$recipeId") {
                        popUpTo("add_from_tiktok") { inclusive = true }
                    }
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable("create_recipe") {
            CreateRecipeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRecipeCreated = {
                    navController.popBackStack()
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable(
            route = "draftPreview/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            DraftPreviewScreen(
                recipeId = recipeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRecipeDetail = { finalRecipeId ->
                    navController.navigate("recipe_detail/$finalRecipeId") {
                        popUpTo("draftPreview/$recipeId") { inclusive = true }
                    }
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable(
            route = "recipe_detail/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            RecipeDetailScreen(
                recipeId = recipeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModelFactory = viewModelFactory
            )
        }
        
        composable(
            route = "edit_recipe/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
            EditRecipeScreen(
                recipeId = recipeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRecipeUpdated = {
                    navController.popBackStack()
                },
                onRecipeDeleted = {
                    navController.popBackStack()
                },
                viewModelFactory = viewModelFactory
            )
        }
    }
}

