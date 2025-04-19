package com.xyldrun.myapplication.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.xyldrun.domain.model.Movie
import com.xyldrun.presentation.movielist.MovieListScreen
import com.xyldrun.presentation.movielist.MovieListUiState
import com.xyldrun.presentation.theme.MovieAppTheme
import org.junit.Rule
import org.junit.Test

class MovieListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testMovie = Movie(
        id = 1,
        title = "Test Movie",
        overview = "Test Overview",
        posterPath = "/test.jpg",
        backdropPath = "/backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 8.5,
        voteCount = 100
    )

    @Test
    fun movieListDisplaysCorrectly() {
        // Given
        val movies = listOf(
            testMovie.copy(id = 1, title = "Test Movie 1"),
            testMovie.copy(id = 2, title = "Test Movie 2")
        )
        val uiState = MovieListUiState.Success(
            movies = movies,
            isRefreshing = false,
            isLoadingMore = false
        )

        // When
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = {},
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Test Movie 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Movie 2").assertIsDisplayed()
    }

    @Test
    fun errorStateDisplaysCorrectly() {
        // Given
        val errorMessage = "Network Error"
        val uiState = MovieListUiState.Error(
            message = errorMessage,
            isOffline = false
        )

        // When
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = {},
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun loadingStateDisplaysCorrectly() {
        // Given
        val uiState = MovieListUiState.Loading

        // When
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = {},
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithTag("loading_screen").assertIsDisplayed()
    }

    @Test
    fun clickingMovieItemTriggersCallback() {
        // Given
        var clickedMovieId = -1
        val movie = testMovie.copy(id = 1, title = "Test Movie")
        val uiState = MovieListUiState.Success(
            movies = listOf(movie),
            isRefreshing = false,
            isLoadingMore = false
        )

        // When
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = { clickedMovieId = it },
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Test Movie").performClick()
        assert(clickedMovieId == 1)
    }

    @Test
    fun pullToRefreshTriggersCallback() {
        // Given
        var refreshCalled = false
        val uiState = MovieListUiState.Success(
            movies = emptyList(),
            isRefreshing = false,
            isLoadingMore = false
        )

        // When
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = {},
                    onRetry = { refreshCalled = true }
                )
            }
        }

        // Then
        composeTestRule.onNodeWithTag("refresh_container").performTouchInput {
            swipeDown(startY = 100f, endY = 600f)
        }
        assert(refreshCalled)
    }

    @Test
    fun emptyStateDisplaysCorrectly() {
        // Given
        val uiState = MovieListUiState.Success(
            movies = emptyList(),
            isRefreshing = false,
            isLoadingMore = false
        )

        // When
        composeTestRule.setContent {
            MovieAppTheme {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = {},
                    onRetry = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("No movies found").assertIsDisplayed()
    }
} 