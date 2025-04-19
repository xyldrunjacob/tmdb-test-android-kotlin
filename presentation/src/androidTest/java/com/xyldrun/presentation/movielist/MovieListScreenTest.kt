package com.xyldrun.presentation.movielist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.xyldrun.domain.model.Movie
import com.xyldrun.presentation.ui.common.MovieImageLoader
import com.xyldrun.presentation.ui.theme.MovieAppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MovieListScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val mockViewModel = mockk<MovieListViewModel>(relaxed = true)
    private val mockImageLoader = mockk<MovieImageLoader>(relaxed = true)
    private val mockNavigateToDetails = mockk<(Int) -> Unit>(relaxed = true)
    
    private val testMovies = List(10) { index ->
        Movie(
            id = index,
            title = "Movie $index",
            overview = "Overview $index",
            posterPath = "/poster$index.jpg",
            backdropPath = "/backdrop$index.jpg",
            releaseDate = "2024-01-0$index",
            voteAverage = 4.5f + index
        )
    }
    
    @Test
    fun loadingState_showsShimmerEffect() {
        every { mockViewModel.uiState } returns MutableStateFlow(MovieListUiState.Loading)
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("shimmer_loading").assertExists()
        composeTestRule.onNodeWithTag("movie_grid").assertDoesNotExist()
    }
    
    @Test
    fun successState_showsMovieGrid() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Success(
                movies = testMovies,
                isLoadingMore = false,
                canLoadMore = true,
                isShowingCached = false,
                isRefreshing = false,
                loadMoreError = null
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("movie_grid").assertExists()
        composeTestRule.onNodeWithTag("shimmer_loading").assertDoesNotExist()
        
        // Verify all movies are displayed
        testMovies.forEach { movie ->
            composeTestRule.onNodeWithText(movie.title).assertExists()
        }
    }
    
    @Test
    fun errorState_showsErrorMessage() {
        val errorMessage = "Network Error"
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Error(
                message = errorMessage,
                isOffline = false,
                showingCached = false
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("error_message").assertExists()
        composeTestRule.onNodeWithText(errorMessage).assertExists()
        composeTestRule.onNodeWithTag("retry_button").assertExists()
    }
    
    @Test
    fun retryButton_clickTriggersRefresh() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Error("Error")
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("retry_button").performClick()
        
        verify { mockViewModel.refresh() }
    }
    
    @Test
    fun movieCard_clickNavigatesToDetails() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Success(
                movies = testMovies,
                isLoadingMore = false
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        // Click the first movie card
        composeTestRule.onNodeWithText(testMovies[0].title).performClick()
        
        verify { mockNavigateToDetails(testMovies[0].id) }
    }
    
    @Test
    fun loadingMore_showsLoadingIndicator() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Success(
                movies = testMovies,
                isLoadingMore = true
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("loading_more_indicator").assertExists()
    }
    
    @Test
    fun scrollToBottom_triggersLoadMore() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Success(
                movies = testMovies,
                isLoadingMore = false
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("movie_grid")
            .performScrollToIndex(testMovies.size - 1)
        
        verify { mockViewModel.loadMore() }
    }
    
    @Test
    fun emptyState_showsEmptyMessage() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Success(
                movies = emptyList(),
                isLoadingMore = false
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithTag("empty_state").assertExists()
        composeTestRule.onNodeWithTag("empty_state_animation").assertExists()
    }
    
    @Test
    fun `offline error state shows offline message`() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Error(
                message = "Network is unavailable",
                isOffline = true,
                showingCached = false
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithText("Network is unavailable").assertExists()
    }
    
    @Test
    fun `error state with cached data shows both error and cached data`() {
        val movies = listOf(
            Movie(
                id = 1,
                title = "Test Movie",
                overview = "Test Overview",
                posterPath = "/test.jpg",
                backdropPath = "/backdrop.jpg",
                releaseDate = "2024-04-08",
                voteAverage = 8.5,
                voteCount = 100
            )
        )
        every { mockViewModel.uiState } returns MutableStateFlow(
            MovieListUiState.Success(
                movies = movies,
                isLoadingMore = false,
                canLoadMore = false,
                isShowingCached = true,
                isRefreshing = false,
                loadMoreError = "Network error"
            )
        )
        
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    viewModel = mockViewModel,
                    imageLoader = mockImageLoader,
                    navigateToDetails = mockNavigateToDetails
                )
            }
        }
        
        composeTestRule.onNodeWithText("Test Movie").assertExists()
        composeTestRule.onNodeWithText("Network error").assertExists()
    }
} 