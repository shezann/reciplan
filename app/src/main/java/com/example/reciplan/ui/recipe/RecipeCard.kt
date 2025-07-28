package com.example.reciplan.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reciplan.R
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.data.repository.LikeState
import com.example.reciplan.ui.components.LikeButton
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    onRecipeClick: (String) -> Unit,
    onLikeClick: (String, Boolean) -> Unit = { _, _ -> },
    likeState: LikeState = LikeState(),
    onEditClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    isOwner: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onRecipeClick(recipe.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Recipe Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(recipe.videoThumbnail ?: "https://via.placeholder.com/400x200/E0E0E0/757575?text=Recipe+Image")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Recipe image for ${recipe.title}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground), // Fallback image
                    placeholder = painterResource(R.drawable.ic_launcher_foreground)
                )
                
                // Like Button (replacing the old save/bookmark button)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        LikeButton(
                            isLiked = likeState.liked,
                            likesCount = likeState.likesCount,
                            isLoading = likeState.isLoading,
                            onClick = { onLikeClick(recipe.id, likeState.liked) },
                            showCount = false, // Don't show count on overlay
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                
                // Owner Actions Menu
                // Debug logging
                val firebaseUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                println("RecipeCard DEBUG: Recipe '${recipe.title}' - isOwner: $isOwner, recipe.userId: ${recipe.userId}, firebaseUserId: $firebaseUserId")
                
                if (isOwner) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color.White.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Recipe options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEditClick(recipe.id)
                                    showDropdownMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit recipe"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDeleteClick(recipe.id)
                                    showDropdownMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete recipe",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
                
                // Difficulty Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            repeat(recipe.difficulty) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Recipe Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Recipe Title and Like Count Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Show like count here (visible when not overlaying)
                    if (likeState.likesCount > 0) {
                        Text(
                            text = "${likeState.likesCount} likes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Recipe Description
                if (!recipe.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recipe Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱ ${recipe.prepTime + recipe.cookTime} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${recipe.servings} servings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Recipe Tags
                if (recipe.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.tags.take(3).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (recipe.tags.size > 3) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "+${recipe.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                // Show like error if any
                likeState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
} 