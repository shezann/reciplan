package com.example.reciplan.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reciplan.ReciplanApplication
import com.example.reciplan.data.model.Recipe
import com.example.reciplan.databinding.FragmentHomeBinding
import com.example.reciplan.ui.recipe.RecipeDetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recipeAdapter: RecipeAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupSearchBar()
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val application = requireActivity().application as ReciplanApplication
        val recipeApi = application.appContainer.recipeApi
        
        homeViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(recipeApi) as T
            }
        })[HomeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                openRecipeDetail(recipe)
            },
            onSaveClick = { recipe ->
                handleSaveRecipe(recipe)
            },
            onShareClick = { recipe ->
                shareRecipe(recipe)
            }
        )

        binding.recyclerViewRecipes.apply {
            adapter = recipeAdapter
            layoutManager = LinearLayoutManager(requireContext())
            
            // Add infinite scroll listener
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 5) {
                        homeViewModel.loadMoreRecipes()
                    }
                }
            })
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous search
                searchJob?.cancel()
                
                // Debounce search
                searchJob = MainScope().launch {
                    delay(300) // Wait 300ms after user stops typing
                    val query = s?.toString()?.trim() ?: ""
                    homeViewModel.searchRecipes(query)
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.refreshRecipes()
        }
    }

    private fun setupObservers() {
        homeViewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)
            updateEmptyState(recipes.isEmpty())
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        homeViewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefreshLayout.isRefreshing = isRefreshing
        }

        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddRecipe.setOnClickListener {
            // TODO: Navigate to AddRecipeFragment
            Toast.makeText(requireContext(), "Add Recipe coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.isVisible = isEmpty && homeViewModel.isLoading.value != true
        binding.recyclerViewRecipes.isVisible = !isEmpty
    }

    private fun openRecipeDetail(recipe: Recipe) {
        val intent = RecipeDetailActivity.newIntent(requireContext(), recipe.id)
        startActivity(intent)
    }

    private fun handleSaveRecipe(recipe: Recipe) {
        // Check if recipe is already saved
        val isSaved = recipe.saved_by.contains("current_user_id")
        
        if (isSaved) {
            homeViewModel.unsaveRecipe(recipe)
            Toast.makeText(requireContext(), "Recipe unsaved", Toast.LENGTH_SHORT).show()
        } else {
            homeViewModel.saveRecipe(recipe)
            Toast.makeText(requireContext(), "Recipe saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareRecipe(recipe: Recipe) {
        val shareText = "${recipe.title}\n\n${recipe.description}\n\nView full recipe at: ${recipe.source_url}"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share Recipe"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}