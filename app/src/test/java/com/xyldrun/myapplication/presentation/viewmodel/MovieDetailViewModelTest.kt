package com.xyldrun.myapplication.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.usecase.GetMovieDetailsUseCase
import com.xyldrun.myapplication.base.BaseTest
import com.xyldrun.presentation.moviedetail.MovieDetailViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest : BaseTest() {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var getMovieDetails: GetMovieDetailsUseCase
    private lateinit var viewModel: MovieDetailViewModel
    private val movieId = 1
    private val testDispatcher = StandardTestDispatcher()

    private fun createTestMovie(
        id: Int = movieId,
        title: String = "Test Movie"
    ) = Movie(
        id = id,
        title = title,
        overview = "Test overview",
        posterPath = "/test.jpg",
        backdropPath = "/backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 8.5,
        voteCount = 100
    )

    @Before
    override fun setup() {
        super.setup()
        Dispatchers.setMain(testDispatcher)
        getMovieDetails = mockk()
    }

    @After
    override fun tearDown() {
        Dispatchers.resetMain()
        super.tearDown()
    }

    @Test
    fun `init should load movie details`() = runTest {
        // Given
        val movie = createTestMovie()
        coEvery { getMovieDetails(movieId) } returns movie
        viewModel = MovieDetailViewModel(getMovieDetails, movieId)

        // Then
        viewModel.state.test(timeout = 5.seconds) {
            // Initial state
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertNull(initialState.movie)
            assertNull(initialState.error)

            // Success state
            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(movie, successState.movie)
            assertNull(successState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init should handle error when loading movie details`() = runTest {
        // Given
        val errorMessage = "Failed to load movie details"
        coEvery { getMovieDetails(movieId) } throws RuntimeException(errorMessage)
        viewModel = MovieDetailViewModel(getMovieDetails, movieId)

        // Then
        viewModel.state.test(timeout = 5.seconds) {
            // Initial state
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertNull(initialState.movie)
            assertNull(initialState.error)

            // Error state
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNull(errorState.movie)
            assertEquals(errorMessage, errorState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry should reload movie details`() = runTest {
        // Given
        val movie = createTestMovie()
        var callCount = 0
        coEvery { getMovieDetails(movieId) } answers {
            callCount++
            if (callCount == 1) throw RuntimeException("First error")
            movie
        }
        viewModel = MovieDetailViewModel(getMovieDetails, movieId)

        // When - First load fails
        viewModel.state.test(timeout = 5.seconds) {
            // Initial state
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            assertNull(initialState.movie)
            assertNull(initialState.error)

            // Error state
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNull(errorState.movie)
            assertEquals("First error", errorState.error)

            // When - Retry succeeds
            viewModel.retry()

            // Loading state after retry
            val retryLoadingState = awaitItem()
            assertTrue(retryLoadingState.isLoading)
            assertNull(retryLoadingState.movie)
            assertEquals("First error", retryLoadingState.error)

            // Success state after retry
            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(movie, successState.movie)
            assertEquals("First error", successState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
} 