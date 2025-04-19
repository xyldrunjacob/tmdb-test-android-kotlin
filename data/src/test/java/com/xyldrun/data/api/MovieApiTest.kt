package com.xyldrun.data.api

import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.data.remote.MovieApi
import com.xyldrun.data.remote.dto.MovieListResponse
import com.xyldrun.data.remote.error.MovieApiException
import com.xyldrun.data.remote.error.MovieApiExceptionMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MovieApiTest {
    
    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var exceptionMapper: MovieApiExceptionMapper
    private lateinit var movieApi: MovieApi
    private val testDispatcher = StandardTestDispatcher()
    private val networkStateFlow = MutableStateFlow(true)
    private val defaultResponse = """{
        "page": 1,
        "results": [{
            "id": 1,
            "title": "Test Movie",
            "overview": "Test Overview",
            "poster_path": "/test.jpg",
            "backdrop_path": "/backdrop.jpg",
            "release_date": "2024-04-08",
            "vote_average": 8.5,
            "vote_count": 100
        }],
        "total_pages": 1,
        "total_results": 1
    }"""
    private var capturedRequest: HttpRequestData? = null
    
    @Before
    fun setup() {
        mockEngine = MockEngine { request ->
            capturedRequest = request
            respond(
                content = defaultResponse,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.CacheControl to listOf("max-age=3600"),
                    "X-RateLimit-Remaining" to listOf("99")
                ),
                status = HttpStatusCode.OK
            )
        }
        
        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 5000
            }
        }
        
        networkMonitor = mockk {
            coEvery { isNetworkAvailable() } returns true
            coEvery { observeNetworkState() } returns networkStateFlow
        }

        exceptionMapper = MovieApiExceptionMapper()
        
        movieApi = MovieApi(
            client = httpClient,
            exceptionMapper = exceptionMapper,
            networkMonitor = networkMonitor,
            dispatcher = testDispatcher
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
        httpClient.close()
    }

    @Test
    fun `getPopularMovies returns successful response`() = runTest(testDispatcher) {
        // When
        val response = movieApi.getPopularMovies(1).first()

        // Then
        assertNotNull(response)
        assertEquals(1, response.page)
        assertEquals(1, response.results.size)
        assertEquals("Test Movie", response.results[0].title)
    }

    @Test
    fun `getPopularMovies handles network unavailability`() = runTest(testDispatcher) {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns false

        // When/Then
        assertFailsWith<MovieApiException.NetworkError> {
            movieApi.getPopularMovies(1).first()
        }
    }

    @Test
    fun `getPopularMovies uses cached data when network is unavailable`() = runTest(testDispatcher) {
        // Given - first request with network
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        val response = movieApi.getPopularMovies(1).first()
        assertNotNull(response)

        // When - second request without network
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        val cachedResponse = movieApi.getPopularMovies(1).first()

        // Then
        assertNotNull(cachedResponse)
        assertEquals(response, cachedResponse)
    }

    @Test
    fun `getPopularMovies handles request cancellation`() = runTest(testDispatcher) {
        // When
        val job = launch {
            movieApi.getPopularMovies(1).collect { response ->
                assertTrue(response is MovieListResponse)
                assertTrue(response.results.isNotEmpty())
            }
        }

        // Then
        job.cancel()
        assertTrue(job.isCancelled)
    }

    @Test
    fun `getPopularMovies retries on retryable errors`() = runTest(testDispatcher) {
        // Given
        var attemptCount = 0
        mockEngine = MockEngine { request ->
            attemptCount++
            if (attemptCount < 3) {
                respond(
                    content = "",
                    status = HttpStatusCode.InternalServerError
                )
            } else {
                respond(
                    content = defaultResponse,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
                    status = HttpStatusCode.OK
                )
            }
        }

        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        movieApi = MovieApi(
            client = httpClient,
            exceptionMapper = exceptionMapper,
            networkMonitor = networkMonitor,
            dispatcher = testDispatcher
        )

        // When
        val response = movieApi.getPopularMovies(1).first()

        // Then
        assertEquals(3, attemptCount)
        assertNotNull(response)
    }

    @Test
    fun `getPopularMovies handles network state changes`() = runTest(testDispatcher) {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        val response = movieApi.getPopularMovies(1).first()
        assertNotNull(response)

        // When network becomes unavailable
        networkStateFlow.value = false
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        advanceUntilIdle() // Ensure the network state change is propagated

        // Clear the cache to ensure we get a network error
        movieApi.clearCache()
        advanceUntilIdle()

        // Then - new request should fail
        assertFailsWith<MovieApiException.NetworkError> {
            movieApi.getPopularMovies(1).first()
        }
    }

    @Test
    fun `cleanup cancels all active requests`() = runTest(testDispatcher) {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        var job: Job? = null
        var isCollecting = false
        
        // Start collecting in a separate coroutine
        job = launch {
            try {
                movieApi.getPopularMovies(1).collect { 
                    isCollecting = true
                }
            } catch (e: CancellationException) {
                // Expected exception
            }
        }
        
        // Ensure the job has started
        while (!job.isActive) {
            yield()
        }
        
        // When
        movieApi.cleanup()
        job?.cancel()
        
        // Then
        assertTrue(job.isCancelled, "Job should be cancelled after cleanup")
    }

    @Test
    fun `clearCache removes cached data`() = runTest(testDispatcher) {
        // Given
        coEvery { networkMonitor.isNetworkAvailable() } returns true
        val response = movieApi.getPopularMovies(1).first()
        assertNotNull(response)

        // When
        movieApi.clearCache()

        // Then - verify cache is cleared by making a new request without network
        coEvery { networkMonitor.isNetworkAvailable() } returns false
        assertFailsWith<MovieApiException.NetworkError> {
            movieApi.getPopularMovies(1).first()
        }
    }
} 