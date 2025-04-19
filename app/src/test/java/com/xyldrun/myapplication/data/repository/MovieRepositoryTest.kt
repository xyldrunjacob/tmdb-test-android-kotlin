package com.xyldrun.myapplication.data.repository

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.data.local.MovieDao
import com.xyldrun.data.local.MovieEntity
import com.xyldrun.data.remote.MovieApi
import com.xyldrun.data.remote.dto.MovieDto
import com.xyldrun.data.remote.dto.MovieListResponse
import com.xyldrun.data.repository.MovieRepositoryImpl
import com.xyldrun.myapplication.base.BaseTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovieRepositoryTest : BaseTest() {
    private lateinit var repository: MovieRepositoryImpl
    private lateinit var api: MovieApi
    private lateinit var dao: MovieDao
    private lateinit var networkMonitor: NetworkMonitor

    @Before
    override fun setup() {
        super.setup()
        api = mockk()
        dao = mockk()
        networkMonitor = mockk {
            // Mock the function calls
            coEvery { observeNetworkState() } returns flowOf(true)
            coEvery { isNetworkAvailable() } returns true
        }
        repository = MovieRepositoryImpl(api, dao, networkMonitor)
        
        // Default mock behavior
        coEvery { dao.getAllMovies() } returns flowOf(emptyList())
        coEvery { dao.getAllMoviesAsList() } returns emptyList()
        coEvery { dao.getMovieCount() } returns 0
        coEvery { dao.updateMovies(any()) } returns Unit
        coEvery { dao.insertMovies(any()) } returns Unit
        coEvery { dao.clearMovies() } returns Unit
    }

    @Test
    fun `getPopularMovies returns movies when API call succeeds`() = runTest {
        // Given
        val response = MovieListResponse(
            page = 1,
            results = listOf(
                MovieDto(
                    id = 1,
                    title = "Test Movie",
                    overview = "Test Overview",
                    posterPath = "/test.jpg",
                    backdropPath = "/test_backdrop.jpg",
                    releaseDate = "2024-04-08",
                    voteAverage = 7.5,
                    voteCount = 100
                )
            ),
            totalPages = 1,
            totalResults = 1
        )
        coEvery { api.getPopularMovies(any()) } returns flowOf(response)
        coEvery { dao.getAllMovies() } returns flowOf(listOf(
            MovieEntity(
                id = 1,
                title = "Test Movie",
                overview = "Test Overview",
                posterPath = "/test.jpg",
                backdropPath = "/test_backdrop.jpg",
                releaseDate = "2024-04-08",
                voteAverage = 7.5,
                voteCount = 100
            )
        ))

        // When
        repository.refreshPopularMovies(1)
        val result = repository.getPopularMovies().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Movie", result[0].title)
    }

    @Test
    fun `getPopularMovies returns empty list when API call fails`() = runTest {
        // Given
        val error = Exception("Network error")
        coEvery { api.getPopularMovies(any()) } throws error
        coEvery { dao.getAllMovies() } returns flowOf(emptyList())

        // When
        try {
            repository.refreshPopularMovies(1)
        } catch (e: Exception) {
            // Expected exception
            assertEquals("Unknown error occurred: Network error", e.message)
        }
        
        val result = repository.getPopularMovies().first()

        // Then
        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { api.getPopularMovies(any()) }
        coVerify(exactly = 0) { dao.updateMovies(any()) }
    }
} 