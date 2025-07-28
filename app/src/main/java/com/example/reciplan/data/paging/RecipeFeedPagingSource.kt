package com.example.reciplan.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.reciplan.data.api.RecipeApi
import com.example.reciplan.data.model.Recipe
import retrofit2.HttpException
import java.io.IOException

/**
 * PagingSource for Recipe feed with like fields support
 * Handles pagination and integrates liked/likesCount fields efficiently
 */
class RecipeFeedPagingSource(
    private val recipeApi: RecipeApi
) : PagingSource<Int, Recipe>() {
    
    companion object {
        private const val STARTING_PAGE_INDEX = 1
    }
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Recipe> {
        return try {
            val page = params.key ?: STARTING_PAGE_INDEX
            val pageSize = params.loadSize
            
            // Call API with like fields included
            val response = recipeApi.getRecipeFeed(page, pageSize)
            
            if (response.isSuccessful) {
                val recipeFeedResponse = response.body()!!
                val recipes = recipeFeedResponse.recipes
                
                // Performance optimization: pre-validate like fields and ensure data consistency
                val validatedRecipes = recipes.map { recipe ->
                    recipe.copy(
                        // Ensure like fields have proper defaults if missing from API
                        liked = recipe.liked,
                        likesCount = maxOf(0, recipe.likesCount) // Ensure non-negative count
                    )
                }
                
                LoadResult.Page(
                    data = validatedRecipes,
                    prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1,
                    nextKey = if (recipes.isEmpty() || recipes.size < pageSize) null else page + 1
                )
            } else {
                LoadResult.Error(HttpException(response))
            }
            
        } catch (exception: IOException) {
            // Network error
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            // HTTP error
            LoadResult.Error(exception)
        } catch (exception: Exception) {
            // Other errors
            LoadResult.Error(exception)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, Recipe>): Int? {
        // Find the page key of the closest page to anchorPosition
        // This ensures smooth refresh behavior when data changes
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
