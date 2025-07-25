package com.example.reciplan.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reciplan.data.model.Recipe
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreenDebug(
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToEditRecipe: (String) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val recipeFeed by viewModel.recipeFeed.collectAsState()
    
    // Get auth viewmodel to access current user
    val authViewModel: com.example.reciplan.ui.auth.AuthViewModel = viewModel(factory = viewModelFactory)
    val authState by authViewModel.authState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    
    // Get current user ID from auth state
    val currentUserId = when (val state = authState) {
        is com.example.reciplan.data.auth.AuthResult.Success -> state.user.id
        else -> null
    }
    
    // Also check Firebase Auth directly as fallback
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val firebaseUserId = firebaseUser?.uid
    
    // Use Firebase user ID if auth state user ID is null
    val effectiveUserId = currentUserId ?: firebaseUserId
    
    // Filter options
    val filterOptions = listOf("All", "My Recipes", "Breakfast", "Lunch", "Dinner", "Dessert", "Snack")
    
    // Display recipes - use filtered if available, otherwise use main feed
    val displayRecipes = if (uiState.filteredRecipes.isNotEmpty() || uiState.searchQuery.isNotEmpty() || uiState.selectedFilter != "All") {
        uiState.filteredRecipes
    } else {
        recipeFeed
    }
    
    // Auto-clear messages after delay
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(5000) // Clear error after 5 seconds
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000) // Clear success message after 3 seconds
            viewModel.clearSuccessMessage()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Debug Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🐛 DEBUG MODE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Auth State User ID: ${currentUserId ?: "NULL"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Firebase User ID: ${firebaseUserId ?: "NULL"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Effective User ID: ${effectiveUserId ?: "NOT LOGGED IN"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Auth State: ${authState.javaClass.simpleName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Recipe Count: ${displayRecipes.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                // Show first few recipe IDs for debugging
                if (displayRecipes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sample Recipe IDs & Owners:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    displayRecipes.take(3).forEach { recipe ->
                        val isOwner = effectiveUserId != null && recipe.userId == effectiveUserId
                        Text(
                            text = "• ${recipe.title}: userId=${recipe.userId}, isOwner=$isOwner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // Debug buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                                    Button(
                    onClick = { 
                        println("RecipeScreenDebug: Adding test recipes with effective user ID: $effectiveUserId")
                        viewModel.seedRecipes() 
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Test Recipes")
                }
                    Button(
                        onClick = { viewModel.refreshRecipes() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refresh")
                    }
                }
            }
        }
        
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recipes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    FloatingActionButton(
                        onClick = onNavigateToCreateRecipe,
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Recipe",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchRecipes(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search recipes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Filter Tags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            onClick = { viewModel.filterRecipesByTag(filter) },
                            label = { Text(filter) },
                            selected = uiState.selectedFilter == filter,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
        
        // Error Message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Success Message
        uiState.successMessage?.let { successMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = successMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Recipe Feed
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isLoading),
            onRefresh = { viewModel.refreshRecipes() }
        ) {
            if (displayRecipes.isEmpty() && !uiState.isLoading) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No recipes found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try adjusting your search or add a new recipe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToCreateRecipe
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Recipe")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayRecipes) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onRecipeClick = onNavigateToRecipeDetail,
                            onSaveClick = { viewModel.saveRecipe(it) },
                            onUnsaveClick = { viewModel.unsaveRecipe(it) },
                            onEditClick = onNavigateToEditRecipe,
                            onDeleteClick = { recipeId ->
                                println("RecipeScreenDebug: Delete clicked for recipe ID: $recipeId")
                                recipeToDelete = displayRecipes.find { it.id == recipeId }
                                println("RecipeScreenDebug: Found recipe to delete: ${recipeToDelete?.title}")
                                showDeleteDialog = true
                                println("RecipeScreenDebug: Delete dialog should be shown: $showDeleteDialog")
                            },
                            isSaved = false, // You would check this against saved recipes
                            // Show actual ownership status for debugging
                            isOwner = effectiveUserId != null && recipe.userId == effectiveUserId
                        )
                    }
                    
                    // Load more indicator
                    if (uiState.isLoading && displayRecipes.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                recipeToDelete = null
            },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete '${recipeToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        println("RecipeScreenDebug: Delete confirmed for recipe: ${recipeToDelete?.title}")
                        recipeToDelete?.let { recipe ->
                            println("RecipeScreenDebug: Calling viewModel.deleteRecipe with ID: ${recipe.id}")
                            viewModel.deleteRecipe(recipe.id)
                        }
                        showDeleteDialog = false
                        recipeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        recipeToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 