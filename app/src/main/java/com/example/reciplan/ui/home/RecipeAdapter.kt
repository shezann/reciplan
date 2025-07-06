package com.example.reciplan.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.reciplan.R
import com.example.reciplan.data.model.Recipe
import com.google.android.material.button.MaterialButton

class RecipeAdapter(
        private val onRecipeClick: (Recipe) -> Unit,
        private val onSaveClick: (Recipe) -> Unit,
        private val onShareClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageRecipe: ImageView = itemView.findViewById(R.id.imageRecipe)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textCookTime: TextView = itemView.findViewById(R.id.textCookTime)
        private val textAuthor: TextView = itemView.findViewById(R.id.textAuthor)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val buttonSave: ImageButton = itemView.findViewById(R.id.buttonSave)
        private val buttonShare: ImageButton = itemView.findViewById(R.id.buttonShare)
        private val buttonViewDetails: MaterialButton =
                itemView.findViewById(R.id.buttonViewDetails)

        // Difficulty indicators
        private val difficultyViews =
                listOf(
                        itemView.findViewById<TextView>(R.id.textDifficulty1),
                        itemView.findViewById<TextView>(R.id.textDifficulty2),
                        itemView.findViewById<TextView>(R.id.textDifficulty3),
                        itemView.findViewById<TextView>(R.id.textDifficulty4),
                        itemView.findViewById<TextView>(R.id.textDifficulty5)
                )

        fun bind(recipe: Recipe) {
            // Load image with Glide
            Glide.with(itemView.context)
                    .load(recipe.video_thumbnail)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(imageRecipe)

            // Set text content
            textTitle.text = recipe.title
            textCookTime.text = "${recipe.cook_time} min"
            textAuthor.text = recipe.tiktok_author?.let { "@$it" } ?: "Unknown"
            textDescription.text = recipe.description

            // Set difficulty indicators
            setDifficultyIndicators(recipe.difficulty)

            // Update save button state
            updateSaveButtonState(recipe)

            // Set click listeners
            itemView.setOnClickListener { onRecipeClick(recipe) }
            buttonViewDetails.setOnClickListener { onRecipeClick(recipe) }
            buttonSave.setOnClickListener { onSaveClick(recipe) }
            buttonShare.setOnClickListener { onShareClick(recipe) }
        }

        private fun setDifficultyIndicators(difficulty: Int) {
            val primaryColor =
                    ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
            val onSurfaceColor =
                    ContextCompat.getColor(itemView.context, android.R.color.darker_gray)

            difficultyViews.forEachIndexed { index, textView ->
                textView.setTextColor(if (index < difficulty) primaryColor else onSurfaceColor)
            }
        }

        private fun updateSaveButtonState(recipe: Recipe) {
            // TODO: Check if recipe is saved by current user
            // For now, we'll use a simple placeholder
            val isSaved =
                    false // This should be determined by checking if user ID is in recipe.saved_by

            buttonSave.setImageResource(
                    if (isSaved) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
            )
        }
    }
}

class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
    override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
        return oldItem == newItem
    }
}
