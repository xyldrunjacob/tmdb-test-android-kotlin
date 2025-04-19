package com.xyldrun.presentation.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xyldrun.data.api.MovieApi
import com.xyldrun.data.repository.MovieRepositoryImpl
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.repository.MovieRepository
import com.xyldrun.presentation.MainActivity
import com.xyldrun.presentation.movielist.MovieListViewModel
import com.xyldrun.presentation.ui.common.MovieImageLoader
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
class MovieListIntegrationTest : KoinTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val movieApi = mockk<MovieApi>()
    private val movieImageLoader = mockk<MovieImageLoader>()
    private lateinit var movieRepository: MovieRepository
    private lateinit var movieListViewModel: MovieListViewModel

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

    @Before
    fun setup() {
        stopKoin()
        
        movieRepository = MovieRepositoryImpl(movieApi)
        movieListViewModel = MovieListViewModel(movieRepository)

        val testModule = module {
            single { movieApi }
            single { movieRepository }
            single { movieImageLoader }
            viewModel { movieListViewModel }
        }

        startKoin {
            modules(testModule)
        }
    }

    @Test
    fun testMovieListEndToEnd() {
        // Mock API response
        coEvery { movieApi.getPopularMovies(any()) } returns flowOf(testMovies)

        // Wait for the initial load
        composeTestRule.waitForIdle()

        // Verify loading state is shown initially
        composeTestRule.onNodeWithTag("shimmer_loading").assertExists()

        // Wait for data to load
        composeTestRule.waitForIdle()

        // Verify movies are displayed
        testMovies.forEach { movie ->
            composeTestRule.onNodeWithText(movie.title).assertExists()
        }

        // Verify grid exists
        composeTestRule.onNodeWithTag("movie_grid").assertExists()

        // Test scroll and load more
        composeTestRule.onNodeWithTag("movie_grid").performScrollToIndex(9)
        composeTestRule.waitForIdle()

        // Verify loading more indicator appears
        composeTestRule.onNodeWithTag("loading_more_indicator").assertExists()
    }

    @Test
    fun testErrorHandling() {
        // Mock API error
        coEvery { movieApi.getPopularMovies(any()) } throws Exception("Network error")

        // Wait for the initial load
        composeTestRule.waitForIdle()

        // Verify error state
        composeTestRule.onNodeWithTag("error_message").assertExists()
        composeTestRule.onNodeWithTag("retry_button").assertExists()

        // Test retry functionality
        composeTestRule.onNodeWithTag("retry_button").performClick()
        composeTestRule.waitForIdle()

        // Verify loading state after retry
        composeTestRule.onNodeWithTag("shimmer_loading").assertExists()
    }

    @Test
    fun testEmptyState() {
        // Mock empty response
        coEvery { movieApi.getPopularMovies(any()) } returns flowOf(emptyList())

        // Wait for the initial load
        composeTestRule.waitForIdle()

        // Verify empty state
        composeTestRule.onNodeWithTag("empty_state").assertExists()
        composeTestRule.onNodeWithTag("empty_state_animation").assertExists()
    }

    @Test
    fun testNavigationToDetails() {
        // Mock API response
        coEvery { movieApi.getPopularMovies(any()) } returns flowOf(testMovies)

        // Wait for data to load
        composeTestRule.waitForIdle()

        // Click on first movie
        composeTestRule.onNodeWithText(testMovies[0].title).performClick()

        // Verify navigation to details screen
        composeTestRule.onNodeWithTag("movie_details_screen").assertExists()
        composeTestRule.onNodeWithText(testMovies[0].title).assertExists()
        composeTestRule.onNodeWithText(testMovies[0].overview).assertExists()
    }

    @Test
    fun testSearchFunctionality() {
        // Mock search API response
        val searchQuery = "Test"
        val searchResults = testMovies.take(2)
        coEvery { movieApi.searchMovies(searchQuery, any()) } returns flowOf(searchResults)

        // Wait for initial load
        composeTestRule.waitForIdle()

        // Open search
        composeTestRule.onNodeWithTag("search_button").performClick()

        // Enter search query
        composeTestRule.onNodeWithTag("search_field")
            .performTextInput(searchQuery)

        // Wait for search results
        composeTestRule.waitForIdle()

        // Verify search results
        searchResults.forEach { movie ->
            composeTestRule.onNodeWithText(movie.title).assertExists()
        }

        // Verify other movies are not visible
        testMovies.drop(2).forEach { movie ->
            composeTestRule.onNodeWithText(movie.title).assertDoesNotExist()
        }
    }

    @Test
    fun testRefreshFunctionality() {
        // Mock initial response
        coEvery { movieApi.getPopularMovies(any()) } returns flowOf(testMovies)

        // Wait for initial load
        composeTestRule.waitForIdle()

        // Mock refresh response with updated data
        val updatedMovies = testMovies.map { it.copy(title = "Updated ${it.title}") }
        coEvery { movieApi.getPopularMovies(any()) } returns flowOf(updatedMovies)

        // Perform refresh
        composeTestRule.onNodeWithTag("refresh_indicator").performTouchInput {
            swipeDown()
        }

        // Wait for refresh to complete
        composeTestRule.waitForIdle()

        // Verify updated data is displayed
        updatedMovies.forEach { movie ->
            composeTestRule.onNodeWithText(movie.title).assertExists()
        }
    }
} 