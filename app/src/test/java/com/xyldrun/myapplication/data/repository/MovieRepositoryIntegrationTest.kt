package com.xyldrun.myapplication.data.repository

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.data.local.MovieDao
import com.xyldrun.data.local.MovieEntity
import com.xyldrun.data.remote.MovieApi
import com.xyldrun.data.remote.dto.MovieDto
import com.xyldrun.data.remote.dto.MovieListResponse
import com.xyldrun.data.repository.MovieRepositoryImpl
import com.xyldrun.domain.model.Movie
import com.xyldrun.domain.repository.MovieRepository
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

class MovieRepositoryIntegrationTest : BaseTest() {
    private lateinit var repository: MovieRepository
    private lateinit var api: MovieApi
    private lateinit var dao: MovieDao
    private lateinit var networkMonitor: NetworkMonitor

    private fun createTestMovie(id: Int, title: String) = Movie(
        id = id,
        title = title,
        overview = "Test Overview",
        posterPath = "/test.jpg",
        backdropPath = "/test_backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 7.5,
        voteCount = 100
    )

    private fun createTestMovieDto(id: Int, title: String) = MovieDto(
        id = id,
        title = title,
        overview = "Test Overview",
        posterPath = "/test.jpg",
        backdropPath = "/test_backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 7.5,
        voteCount = 100
    )

    private fun createTestMovieEntity(id: Int, title: String) = MovieEntity(
        id = id,
        title = title,
        overview = "Test Overview",
        posterPath = "/test.jpg",
        backdropPath = "/test_backdrop.jpg",
        releaseDate = "2024-04-08",
        voteAverage = 7.5,
        voteCount = 100
    )

    @Before
    override fun setup() {
        super.setup()
        api = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        networkMonitor = mockk {
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
    fun `repository correctly handles pagination`() = runTest {
        // Given
        val page1Response = MovieListResponse(
            page = 1,
            results = listOf(createTestMovieDto(1, "Movie 1")),
            totalPages = 2,
            totalResults = 2
        )
        val page2Response = MovieListResponse(
            page = 2,
            results = listOf(createTestMovieDto(2, "Movie 2")),
            totalPages = 2,
            totalResults = 2
        )
        
        coEvery { api.getPopularMovies(page = 1) } returns flowOf(page1Response)
        coEvery { api.getPopularMovies(page = 2) } returns flowOf(page2Response)
        coEvery { dao.getAllMovies() } returns flowOf(listOf(createTestMovieEntity(1, "Movie 1"))) andThen flowOf(listOf(createTestMovieEntity(2, "Movie 2")))

        // When
        repository.refreshPopularMovies(1)
        val result1 = repository.getPopularMovies().first()
        repository.refreshPopularMovies(2)
        val result2 = repository.getPopularMovies().first()

        // Then
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
        assertEquals("Movie 1", result1[0].title)
        assertEquals("Movie 2", result2[0].title)
        
        coVerify(exactly = 1) { api.getPopularMovies(page = 1) }
        coVerify(exactly = 1) { api.getPopularMovies(page = 2) }
    }

    @Test
    fun `repository handles API errors gracefully`() = runTest {
        // Given
        val networkError = Exception("Network error")
        coEvery { api.getPopularMovies(any()) } throws networkError
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

    @Test
    fun `repository caches successful responses`() = runTest {
        // Given
        val response = MovieListResponse(
            page = 1,
            results = listOf(createTestMovieDto(1, "Movie 1")),
            totalPages = 1,
            totalResults = 1
        )
        coEvery { api.getPopularMovies(page = 1) } returns flowOf(response)
        coEvery { dao.getAllMovies() } returns flowOf(listOf(createTestMovieEntity(1, "Movie 1")))

        // When
        repository.refreshPopularMovies(1)
        val result1 = repository.getPopularMovies().first()
        val result2 = repository.getPopularMovies().first()

        // Then
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
        assertEquals("Movie 1", result1[0].title)
        assertEquals("Movie 1", result2[0].title)
        coVerify(exactly = 1) { api.getPopularMovies(page = 1) }
    }

    @Test
    fun `repository handles empty API response`() = runTest {
        // Given
        val emptyResponse = MovieListResponse(
            page = 1,
            results = emptyList(),
            totalPages = 0,
            totalResults = 0
        )
        coEvery { api.getPopularMovies(any()) } returns flowOf(emptyResponse)
        coEvery { dao.getAllMovies() } returns flowOf(emptyList())

        // When
        repository.refreshPopularMovies(1)
        val result = repository.getPopularMovies().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `repository handles concurrent requests`() = runTest {
        // Given
        val response = MovieListResponse(
            page = 1,
            results = listOf(createTestMovieDto(1, "Movie 1")),
            totalPages = 1,
            totalResults = 1
        )
        coEvery { api.getPopularMovies(any()) } returns flowOf(response)
        coEvery { dao.getAllMovies() } returns flowOf(listOf(createTestMovieEntity(1, "Movie 1")))

        // When
        repository.refreshPopularMovies(1)
        val results = listOf(
            repository.getPopularMovies(),
            repository.getPopularMovies(),
            repository.getPopularMovies()
        ).map { it.first() }

        // Then
        results.forEach { result ->
            assertEquals(1, result.size)
            assertEquals("Movie 1", result[0].title)
        }
        coVerify(exactly = 1) { api.getPopularMovies(any()) }
    }

    @Test
    fun `refreshPopularMovies calls API and updates DAO on success`() = runTest {
        // Given
        val apiResponse = MovieListResponse(page = 1, results = listOf(MovieDto(id = 1, title = "API Movie", overview = "", posterPath = null, backdropPath = null, releaseDate = "", voteAverage = 0.0, voteCount = 0)), totalPages = 1, totalResults = 1)
        coEvery { api.getPopularMovies(1) } returns flowOf(apiResponse)
        coEvery { dao.updateMovies(any()) } returns Unit

        // When
        repository.refreshPopularMovies(1)

        // Then
        coVerify(exactly = 1) { api.getPopularMovies(1) }
        coVerify(exactly = 1) { dao.updateMovies(any()) }
    }
} 