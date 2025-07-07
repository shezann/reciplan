package com.example.reciplan.ui.recipe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.reciplan.ReciplanApplication
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.databinding.ActivityRecipeDetailBinding

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private lateinit var viewModel: RecipeDetailViewModel
    private lateinit var recipe: Recipe
    private lateinit var ingredientsAdapter: IngredientsAdapter
    private lateinit var instructionsAdapter: InstructionsAdapter

    companion object {
        private const val EXTRA_RECIPE_ID = "recipe_id"
        
        fun newIntent(context: Context, recipeId: String): Intent {
            return Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra(EXTRA_RECIPE_ID, recipeId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recipeId = intent.getStringExtra(EXTRA_RECIPE_ID)
        if (recipeId == null) {
            finish()
            return
        }

        setupViewModel(recipeId)
        setupRecyclerViews()
        setupClickListeners()
        setupObservers()
    }

    private fun setupViewModel(recipeId: String) {
        val application = applicationContext as ReciplanApplication
        val recipeApi = application.appContainer.recipeApi
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RecipeDetailViewModel(recipeApi, recipeId) as T
            }
        })[RecipeDetailViewModel::class.java]
    }

    private fun setupRecyclerViews() {
        ingredientsAdapter = IngredientsAdapter()
        instructionsAdapter = InstructionsAdapter()
        
        binding.recyclerViewIngredients.apply {
            adapter = ingredientsAdapter
            layoutManager = LinearLayoutManager(this@RecipeDetailActivity)
            isNestedScrollingEnabled = false
        }
        
        binding.recyclerViewInstructions.apply {
            adapter = instructionsAdapter
            layoutManager = LinearLayoutManager(this@RecipeDetailActivity)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        // Make sure the back button is properly clickable
        binding.buttonBack.setOnClickListener {
            finish()
        }
        
        binding.buttonSave.setOnClickListener {
            if (::recipe.isInitialized) {
                if (recipe.saved_by.contains("current_user_id")) {
                    viewModel.unsaveRecipe()
                } else {
                    viewModel.saveRecipe()
                }
            }
        }
        
        binding.buttonShare.setOnClickListener {
            if (::recipe.isInitialized) {
                shareRecipe()
            }
        }
        
        binding.buttonViewSource.setOnClickListener {
            if (::recipe.isInitialized) {
                openSourceUrl()
            }
        }
    }

    private fun setupObservers() {
        viewModel.recipe.observe(this) { recipe ->
            if (recipe != null) {
                this.recipe = recipe
                displayRecipe(recipe)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.scrollView.isVisible = !isLoading
        }
        
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        // Load header image
        Glide.with(this)
            .load(recipe.video_thumbnail)
            .centerCrop()
            .into(binding.imageRecipe)

        // Basic info
        binding.textTitle.text = recipe.title
        binding.textDescription.text = recipe.description
        binding.textAuthor.text = recipe.tiktok_author?.let { "@$it" } ?: "Unknown"
        binding.textSource.text = recipe.source_platform.uppercase()

        // Time and difficulty
        binding.textPrepTime.text = "${recipe.prep_time} min"
        binding.textCookTime.text = "${recipe.cook_time} min"
        binding.textServings.text = "${recipe.servings}"
        
        setDifficultyRating(recipe.difficulty)

        // Nutrition
        binding.textCalories.text = "${recipe.nutrition.calories}"
        binding.textProtein.text = "${recipe.nutrition.protein}g"
        binding.textCarbs.text = "${recipe.nutrition.carbs}g"
        binding.textFat.text = "${recipe.nutrition.fat}g"

        // Tags
        binding.textTags.text = recipe.tags.joinToString(" • ")

        // Lists
        ingredientsAdapter.submitList(recipe.ingredients)
        instructionsAdapter.submitList(recipe.instructions)

        // Update save button
        updateSaveButton(recipe.saved_by.contains("current_user_id"))
    }

    private fun setDifficultyRating(difficulty: Int) {
        val stars = listOf(
            binding.star1, binding.star2, binding.star3, 
            binding.star4, binding.star5
        )
        
        stars.forEachIndexed { index, imageView ->
            imageView.setImageResource(
                if (index < difficulty) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
        }
    }

    private fun updateSaveButton(isSaved: Boolean) {
        binding.buttonSave.setImageResource(
            if (isSaved) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    private fun shareRecipe() {
        val shareText = "${recipe.title}\n\n${recipe.description}\n\nIngredients:\n" +
                recipe.ingredients.joinToString("\n") { "• ${it.quantity} ${it.name}" } +
                "\n\nView full recipe: ${recipe.source_url}"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Recipe"))
    }

    private fun openSourceUrl() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.source_url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open source URL", Toast.LENGTH_SHORT).show()
        }
    }
} 