package com.example.reciplan.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reciplan.R
import com.example.reciplan.data.model.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onNavigateBack: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier
) {
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()
    
    // Load the recipe when screen opens
    LaunchedEffect(recipeId) {
        viewModel.getRecipe(recipeId)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with Back Button
        TopAppBar(
            title = { Text("Recipe Details") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        if (uiState.isLoading) {
            // Loading State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (selectedRecipe != null) {
            // Recipe Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Recipe Image and Title
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Recipe Image
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedRecipe?.videoThumbnail ?: "https://via.placeholder.com/400x200/E0E0E0/757575?text=Recipe+Image")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Recipe image for ${selectedRecipe?.title}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_launcher_foreground),
                            placeholder = painterResource(R.drawable.ic_launcher_foreground)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Recipe Title
                        Text(
                            text = selectedRecipe?.title ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Recipe Description
                        if (!selectedRecipe?.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedRecipe?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Recipe Metadata Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Prep Time
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Prep Time",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(selectedRecipe?.prepTime ?: 0) + (selectedRecipe?.cookTime ?: 0)} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Total Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Servings
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Servings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${selectedRecipe?.servings}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Servings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Difficulty
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row {
                                    repeat(selectedRecipe?.difficulty ?: 1) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Difficulty",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Ingredients Section Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Ingredients",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Ingredients Items
                selectedRecipe?.ingredients?.let { ingredients ->
                    items(ingredients) { ingredient ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ingredient.quantity} ${ingredient.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Steps Section Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Steps",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Steps Items
                selectedRecipe?.instructions?.let { instructions ->
                    itemsIndexed(instructions) { index, instruction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.wrapContentSize(Alignment.Center)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = instruction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                // Tags Section (if any)
                if (selectedRecipe?.tags?.isNotEmpty() == true) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedRecipe?.tags?.let { tags ->
                                        items(tags) { tag ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Nutrition Section (if available)
                if (selectedRecipe?.nutrition != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Nutrition",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val nutrition = selectedRecipe?.nutrition
                                    
                                    // Calories
                                    nutrition?.calories?.let { calories ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${calories.toInt()}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Calories",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    // Protein
                                    nutrition?.protein?.let { protein ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${protein.toInt()}g",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Protein",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    // Carbs
                                    nutrition?.carbs?.let { carbs ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${carbs.toInt()}g",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Carbs",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    // Fat
                                    nutrition?.fat?.let { fat ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${fat.toInt()}g",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Fat",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (uiState.error != null) {
            // Error State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error loading recipe",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
} 