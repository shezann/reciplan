package com.example.reciplan.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reciplan.ui.home.HomeScreen
import com.example.reciplan.ui.profile.ProfileScreen
import com.example.reciplan.ui.recipe.RecipeScreen

sealed class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Recipes : BottomNavItem("recipes", Icons.Default.Favorite, "Recipes")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToEditRecipe: (String) -> Unit,
    onNavigateToAddFromTikTok: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Recipes,
        BottomNavItem.Profile
    )
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToAddFromTikTok = onNavigateToAddFromTikTok
                )
            }
            
            composable(BottomNavItem.Recipes.route) {
                RecipeScreen(
                    onNavigateToCreateRecipe = onNavigateToCreateRecipe,
                    onNavigateToRecipeDetail = onNavigateToRecipeDetail,
                    onNavigateToEditRecipe = onNavigateToEditRecipe,
                    viewModelFactory = viewModelFactory,
                    showCreateButton = true // Show the plus button only in recipes tab
                )
            }
            
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin = onNavigateToLogin,
                    viewModelFactory = viewModelFactory
                )
            }
        }
    }
} 