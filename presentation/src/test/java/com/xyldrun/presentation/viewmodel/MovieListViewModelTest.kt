package com.xyldrun.presentation.viewmodel

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.usecase.GetPopularMoviesUseCase
import com.xyldrun.domain.usecase.RefreshMoviesUseCase
import com.xyldrun.presentation.movielist.MovieListUiState
import com.xyldrun.presentation.movielist.MovieListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MovieListViewModelTest {

    private lateinit var viewModel: MovieListViewModel
    private lateinit var getPopularMovies: GetPopularMoviesUseCase
    private lateinit var refreshMovies: RefreshMoviesUseCase
    private lateinit var networkMonitor: NetworkMonitor
    private val networkStateFlow = MutableStateFlow(true)
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testMovies = listOf(
        Movie(
            id = 1,
            title = "Test Movie 1",
            overview = "Test Overview 1",
            posterPath = "/test1.jpg",
            backdropPath = "/backdrop1.jpg",
            releaseDate = "2024-04-08",
            voteAverage = 8.5,
            voteCount = 100
        ),
        Movie(
            id = 2,
            title = "Test Movie 2",
            overview = "Test Overview 2",
            posterPath = "/test2.jpg",
            backdropPath = "/backdrop2.jpg",
            releaseDate = "2024-04-09",
            voteAverage = 7.5,
            voteCount = 200
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getPopularMovies = mockk()
        refreshMovies = mockk()
        networkMonitor = mockk {
            coEvery { isNetworkAvailable() } returns true
            coEvery { observeNetworkState() } returns networkStateFlow
        }

        viewModel = MovieListViewModel(
            getPopularMovies = getPopularMovies,
            refreshMovies = refreshMovies,
            networkMonitor = networkMonitor
        )
    }

    @After
    fun tearDown() {

    }

    @Test
    fun `initial state is Loading and triggers initial load`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit
        networkStateFlow.value = true

        // Create new ViewModel to test initial state
        val newViewModel = MovieListViewModel(
            getPopularMovies = getPopularMovies,
            refreshMovies = refreshMovies,
            networkMonitor = networkMonitor
        )

        // When - check initial state before any processing
        val initialState = newViewModel.uiState.first()
        assertTrue(initialState is MovieListUiState.Loading, "Expected Loading state but got $initialState")

        // Then - wait for initial load to complete
        advanceUntilIdle()
        coVerify { refreshMovies() }
        coVerify { getPopularMovies() }

        // Verify final state
        val finalState = newViewModel.uiState.first()
        assertTrue(finalState is MovieListUiState.Success)
        with(finalState as MovieListUiState.Success) {
            assertEquals(testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
        }
    }

    @Test
    fun `loadMovies with forceRefresh resets state and loads fresh data`() = runTest {
        // Given
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit
        coEvery { networkMonitor.isNetworkAvailable() } returns true

        // When
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Success)
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
        }
        coVerify { refreshMovies() }
        coVerify { getPopularMovies() }
    }

    @Test
    fun `loadMovies without forceRefresh appends to existing data`() = runTest {
        // Given
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit
        coEvery { networkMonitor.isNetworkAvailable() } returns true

        // First load
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()

        // When loading more
        viewModel.loadMovies(forceRefresh = false)
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Success)
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies + testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
        }
    }

    @Test
    fun `loadMovies handles network unavailability`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        coEvery { getPopularMovies() } returns flowOf(emptyList())
        coEvery { refreshMovies() } throws RuntimeException("Network is unavailable")

        // When
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Error)
        with(state as MovieListUiState.Error) {
            assertEquals("Network is unavailable", message)
            assertTrue(isOffline)
            assertFalse(showingCached)
        }

        // Verify that refresh was attempted
        coVerify { refreshMovies() }
    }

    @Test
    fun `loadMovies shows cached data when network is unavailable`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit

        // First load with network
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()

        // Clear previous expectations
        coEvery { getPopularMovies() } returns flowOf(emptyList())
        coEvery { refreshMovies() } returns Unit
        coEvery { networkMonitor.isNetworkAvailable() } returns false

        // When
        viewModel.loadMovies(forceRefresh = false)
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Success)
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies, movies)
            assertFalse(isLoadingMore)
            assertFalse(canLoadMore)
            assertTrue(isShowingCached)
            assertFalse(isRefreshing)
        }
    }

    @Test
    fun `loadMovies handles errors during refresh`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { refreshMovies() } throws RuntimeException("Test error")
        coEvery { getPopularMovies() } returns flowOf(testMovies)

        // When
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is MovieListUiState.Error, "Expected Error state but got $state")
        with(state as MovieListUiState.Error) {
            assertEquals("Test error", message)
            assertFalse(isOffline)
            assertFalse(showingCached)
        }
        
        // Verify that refresh was called
        coVerify { refreshMovies() }
    }

    @Test
    fun `loadMovies handles errors during load more`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit

        // First load successful
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()

        // Then error occurs
        val errorMessage = "Test error"
        coEvery { getPopularMovies() } throws RuntimeException(errorMessage)

        // When
        viewModel.loadMovies(forceRefresh = false)
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Success)
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
            assertEquals(errorMessage, loadMoreError)
        }
    }

    @Test
    fun `network state changes trigger refresh when in error state`() = runTest {
        // Given - start with network unavailable
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        coEvery { getPopularMovies() } returns flowOf(emptyList())
        coEvery { refreshMovies() } throws RuntimeException("Network is unavailable")
        networkStateFlow.value = false

        // Create ViewModel after setting up initial state
        val newViewModel = MovieListViewModel(
            getPopularMovies = getPopularMovies,
            refreshMovies = refreshMovies,
            networkMonitor = networkMonitor
        )
        advanceUntilIdle()

        // First load fails due to network
        newViewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()

        // Verify error state
        val errorState = newViewModel.uiState.first()
        assertTrue(errorState is MovieListUiState.Error, "Expected Error state but got $errorState")
        assertEquals("Network is unavailable", (errorState as MovieListUiState.Error).message)

        // When network becomes available
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit
        
        // Update network state and wait for processing
        networkStateFlow.value = true
        advanceUntilIdle()
        
        // Wait for the ViewModel to process the state change and load movies
        advanceTimeBy(1000)
        advanceUntilIdle()

        // Then
        val state = newViewModel.uiState.first()
        assertTrue(state is MovieListUiState.Success, "Expected Success state but got $state")
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
        }
        
        // Verify that refresh and getPopularMovies were called
        coVerify { refreshMovies() }
        coVerify { getPopularMovies() }
    }

    @Test
    fun `refresh method forces refresh of data`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit

        // When
        viewModel.refresh()
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Success)
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
        }
        coVerify { refreshMovies() }
        coVerify { getPopularMovies() }
    }

    @Test
    fun `retryLoadMore retries loading more data`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        coEvery { getPopularMovies() } returns flowOf(testMovies)
        coEvery { refreshMovies() } returns Unit

        // First load
        viewModel.loadMovies(forceRefresh = true)
        advanceUntilIdle()

        // When
        viewModel.retryLoadMore()
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Then
        assertTrue(state is MovieListUiState.Success)
        with(state as MovieListUiState.Success) {
            assertEquals(testMovies + testMovies, movies)
            assertFalse(isLoadingMore)
            assertTrue(canLoadMore)
            assertFalse(isShowingCached)
            assertFalse(isRefreshing)
        }
    }
} 