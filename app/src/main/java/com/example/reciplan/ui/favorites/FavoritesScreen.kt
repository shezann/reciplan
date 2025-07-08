package com.example.reciplan.ui.favorites

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.ui.recipe.RecipeCard
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToRecipeDetail: (String) -> Unit = {},
    onNavigateToEditRecipe: (String) -> Unit = {},
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { 
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                ) 
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
// Favorites List Content
        FavoritesListContent(
            uiState = uiState,
            onRecipeClick = onNavigateToRecipeDetail,
            onRecipeUnsave = { recipe -> viewModel.unsaveRecipe(recipe.id) },
            onEditRecipe = onNavigateToEditRecipe,
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMoreSavedRecipes() },
            onRetry = { 
                viewModel.clearError()
                viewModel.loadSavedRecipes()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FavoritesListContent(
    uiState: FavoritesUiState,
    onRecipeClick: (String) -> Unit,
    onRecipeUnsave: (Recipe) -> Unit,
    onEditRecipe: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)
    val listState = rememberLazyListState()
    
    // Detect when user scrolls to bottom for pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { lastVisibleIndex ->
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= uiState.favoriteRecipes.size - 3 && 
                    uiState.hasMorePages && 
                    !uiState.isLoadingMore && 
                    !uiState.isLoading) {
                    onLoadMore()
                }
            }
    }
    
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        when {
            uiState.isLoading -> {
                // Loading State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading your favorites...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            uiState.error != null -> {
                // Error State
                ErrorStateContent(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            uiState.favoriteRecipes.isEmpty() -> {
                // Empty State
                EmptyStateContent(
                    title = "No saved recipes",
                    subtitle = "Save recipes to view them here",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                // Favorites List
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
items(uiState.favoriteRecipes) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onRecipeClick = { recipeId -> onRecipeClick(recipeId) },
                            onSaveClick = { /* Already saved */ },
                            onUnsaveClick = { recipeId -> onRecipeUnsave(recipe) },
                            onEditClick = { recipeId -> onEditRecipe(recipeId) },
                            onDeleteClick = { recipeId -> /* Delete functionality can be added if needed */ },
                            isSaved = true,
                            isOwner = recipe.user_id == FirebaseAuth.getInstance().currentUser?.uid
                        )
                    }
                    
                    // Loading More Indicator
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Discover recipes and tap the ❤️ icon to save them here!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorStateContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unable to load favorites",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getFriendlyErrorMessage(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

private fun getFriendlyErrorMessage(error: String): String {
    return when {
        error.contains("Network error", ignoreCase = true) ||
        error.contains("Connection", ignoreCase = true) ||
        error.contains("timeout", ignoreCase = true) -> 
            "Check your internet connection and try again"
        error.contains("404", ignoreCase = true) -> 
            "Favorites not found. Please try again later"
        error.contains("500", ignoreCase = true) || 
        error.contains("server", ignoreCase = true) ->
            "Server is temporarily unavailable. Please try again later"
        error.contains("Unauthorized", ignoreCase = true) ||
        error.contains("401", ignoreCase = true) ->
            "Please log in again to view your favorites"
        else -> "Unable to load your favorites. Please try again"
    }
}

 