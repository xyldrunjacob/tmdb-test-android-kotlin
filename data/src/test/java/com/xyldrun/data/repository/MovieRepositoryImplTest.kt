package com.xyldrun.data.repository

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.data.local.MovieDao
import com.xyldrun.data.local.MovieEntity
import com.xyldrun.data.remote.MovieApi
import com.xyldrun.data.remote.dto.MovieDto
import com.xyldrun.data.remote.dto.MovieListResponse
import com.xyldrun.data.remote.error.MovieApiException
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.fail

@ExperimentalCoroutinesApi
class MovieRepositoryImplTest {
    private lateinit var repository: MovieRepositoryImpl
    private lateinit var api: MovieApi
    private lateinit var dao: MovieDao
    private lateinit var networkMonitor: NetworkMonitor
    private val networkStateFlow = MutableStateFlow(true)

    private val testMovieDto = MovieDto(
        id = 1,
        title = "Test Movie",
        overview = "Test Overview",
        posterPath = "/test.jpg",
        backdropPath = "/backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 8.5,
        voteCount = 100
    )

    private val testMovieEntity = MovieEntity(
        id = 1,
        title = "Test Movie",
        overview = "Test Overview",
        posterPath = "/test.jpg",
        backdropPath = "/backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 8.5,
        voteCount = 100
    )

    @Before
    fun setup() {
        api = mockk()
        dao = mockk()
        networkMonitor = mockk {
            coEvery { isNetworkAvailable() } returns true
            coEvery { observeNetworkState() } returns networkStateFlow
        }
        repository = MovieRepositoryImpl(api, dao, networkMonitor)
        
        coEvery { dao.getAllMovies() } returns flowOf(emptyList())
        coEvery { dao.getAllMoviesAsList() } returns emptyList()
        coEvery { dao.getMovieCount() } returns 0
        coEvery { dao.updateMovies(any()) } returns Unit
        coEvery { dao.clearMovies() } returns Unit
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getPopularMovies returns mapped movies from dao with correct mapping`() = runTest {
        // Given
        coEvery { dao.getAllMovies() } returns flowOf(listOf(testMovieEntity))

        // When
        val result = repository.getPopularMovies().first()

        // Then
        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals(testMovieEntity.id, id)
            assertEquals(testMovieEntity.title, title)
            assertEquals(testMovieEntity.overview, overview)
            assertEquals(testMovieEntity.posterPath, posterPath)
            assertEquals(testMovieEntity.backdropPath, backdropPath)
            assertEquals(testMovieEntity.releaseDate, releaseDate)
            assertEquals(testMovieEntity.voteAverage, voteAverage, 0.01)
            assertEquals(testMovieEntity.voteCount, voteCount)
        }
        coVerify { dao.getAllMovies() }
    }

    @Test
    fun `refreshPopularMovies handles concurrent requests properly`() = runTest {
        // Given
        var requestCount = 0
        val apiResponse = MovieListResponse(
            page = 1,
            results = listOf(testMovieDto),
            totalPages = 1,
            totalResults = 1
        )
        coEvery { api.getPopularMovies(any()) } answers {
            requestCount++
            flowOf(apiResponse)
        }
        coEvery { dao.getAllMoviesAsList() } returns emptyList()
        coEvery { dao.updateMovies(any()) } returns Unit

        // When - Make concurrent requests
        val requests = List(3) {
            async { repository.refreshPopularMovies(1) }
        }
        requests.forEach { it.await() }

        // Then
        assertEquals(3, requestCount) // Each request should make an API call
        coVerify(exactly = 3) { api.getPopularMovies(1) }
        coVerify(exactly = 3) { dao.updateMovies(any()) }
    }

    @Test
    fun `refreshPopularMovies handles server error with cached data`() = runTest {
        // Given
        coEvery { api.getPopularMovies(1) } throws MovieApiException.ServerError("Test error")
        coEvery { dao.getMovieCount() } returns 5
        coEvery { dao.getAllMoviesAsList() } returns List(5) { testMovieEntity }

        // When/Then
        try {
            repository.refreshPopularMovies(1)
            fail("Expected MovieApiException.ServerError")
        } catch (e: MovieApiException.ServerError) {
            assertEquals("Service is temporarily unavailable. Showing cached data.", e.message)
        }
    }

    @Test
    fun `refreshPopularMovies handles server error without cached data`() = runTest {
        // Given
        coEvery { api.getPopularMovies(1) } throws MovieApiException.ServerError("Test error")
        coEvery { dao.getMovieCount() } returns 0

        // When/Then
        try {
            repository.refreshPopularMovies(1)
            fail("Expected MovieApiException.ServerError")
        } catch (e: MovieApiException.ServerError) {
            assertEquals("Service is temporarily unavailable. Please try again later.", e.message)
        }
    }

    @Test
    fun `refreshPopularMovies handles offline state with cached data`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        coEvery { dao.getMovieCount() } returns 5
        coEvery { dao.getAllMoviesAsList() } returns List(5) { testMovieEntity }

        // When/Then
        try {
            repository.refreshPopularMovies(1)
            fail("Expected MovieApiException.NetworkError")
        } catch (e: MovieApiException.NetworkError) {
            assertEquals("Network is unavailable", e.message)
        }
    }

    @Test
    fun `refreshPopularMovies handles offline state without cached data`() = runTest {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        coEvery { dao.getMovieCount() } returns 0

        // When/Then
        try {
            repository.refreshPopularMovies(1)
            fail("Expected MovieApiException.NoInternetConnection")
        } catch (e: MovieApiException.NoInternetConnection) {
            assertEquals("No internet connection available", e.message)
        }
    }

    @Test
    fun `refreshPopularMovies handles network transitions during refresh`() = runTest {
        // Given
        var callCount = 0
        coEvery { api.getPopularMovies(1) } answers {
            callCount++
            if (callCount == 1) {
                networkStateFlow.value = false
                throw MovieApiException.NetworkError("Network error")
            } else {
                flowOf(MovieListResponse(1, listOf(testMovieDto), 1, 1))
            }
        }
        coEvery { dao.getAllMoviesAsList() } returns emptyList()
        coEvery { dao.updateMovies(any()) } returns Unit
        coEvery { dao.getMovieCount() } returns 5 // Ensure we have cached data

        // When/Then
        // First attempt - should fail with NetworkError due to cached data
        try {
            repository.refreshPopularMovies(1)
            fail("Expected MovieApiException.NetworkError")
        } catch (e: MovieApiException.NetworkError) {
            assertEquals("Network is unavailable", e.message)
        }

        // Restore network
        networkStateFlow.value = true
        coEvery { networkMonitor.isNetworkAvailable() } returns true

        // Second attempt - should succeed
        repository.refreshPopularMovies(1)
        
        coVerify(exactly = 2) { api.getPopularMovies(1) }
    }

    @Test
    fun `refreshPopularMovies maintains data consistency during errors`() = runTest {
        // Given
        val existingMovies = List(5) { index ->
            testMovieEntity.copy(id = index)
        }
        coEvery { dao.getAllMovies() } returns flowOf(existingMovies)
        coEvery { api.getPopularMovies(1) } throws MovieApiException.NetworkError("Network error")
        coEvery { dao.getMovieCount() } returns existingMovies.size

        // When
        try {
            repository.refreshPopularMovies(1)
        } catch (e: MovieApiException) {
            // Expected
        }

        // Then
        val currentMovies = repository.getPopularMovies().first()
        assertEquals(existingMovies.size, currentMovies.size)
        assertTrue(currentMovies.map { it.id }.containsAll(existingMovies.map { it.id }))
    }
} 