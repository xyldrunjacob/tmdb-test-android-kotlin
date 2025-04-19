package com.xyldrun.data.remote

import android.util.Log
import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.data.remote.KtorClient.API_VERSION
import com.xyldrun.data.remote.dto.MovieDetailsDto
import com.xyldrun.data.remote.dto.MovieListResponse
import com.xyldrun.data.remote.error.MovieApiException
import com.xyldrun.data.remote.error.MovieApiExceptionMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

interface MovieApiService {
    fun getPopularMovies(page: Int): Flow<MovieListResponse>
    fun getMovieDetails(movieId: Int): Flow<MovieDetailsDto>
    suspend fun cleanup()
    fun clearCache()
}

class MovieApi(
    private val client: HttpClient = KtorClient.client,
    private val exceptionMapper: MovieApiExceptionMapper = MovieApiExceptionMapper(),
    private val networkMonitor: NetworkMonitor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MovieApiService {
    
    private val scope = CoroutineScope(dispatcher + Job())
    private val activeRequests = ConcurrentHashMap<String, Job>()
    private val requestCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val cacheMutex = Mutex()

    init {
        observeNetworkState()
    }

    private fun observeNetworkState() {
        scope.launch {
            networkMonitor.observeNetworkState().collect { isAvailable ->
                if (!isAvailable) {
                    // Cancel all active requests when network becomes unavailable
                    activeRequests.values.forEach { it.cancel() }
                    activeRequests.clear()
                }
            }
        }
    }

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun getPopularMovies(page: Int): Flow<MovieListResponse> = 
        executeRequest("popular_$page", CACHE_DURATION_POPULAR) {
            Log.d("MovieApi", "Fetching popular movies, page: $page")
            client.get("/$API_VERSION/movie/popular") {
                parameter("page", page)
            }.body<MovieListResponse>().also {
                Log.d("MovieApi", "Received ${it.results.size} movies")
            }
        }

    override fun getMovieDetails(movieId: Int): Flow<MovieDetailsDto> = 
        executeRequest("details_$movieId", CACHE_DURATION_DETAILS) {
            Log.d("MovieApi", "Fetching movie details, id: $movieId")
            client.get("/$API_VERSION/movie/$movieId").body<MovieDetailsDto>().also {
                Log.d("MovieApi", "Received details for movie: ${it.title}")
            }
        }

    private fun <T : Any> executeRequest(
        requestId: String,
        cacheDuration: Long,
        request: suspend () -> T
    ): Flow<T> = channelFlow {
        // Check network state first
        if (!networkMonitor.isNetworkAvailable()) {
            Log.d("MovieApi", "No network available, checking cache for $requestId")
            // Try to get from cache if no network
            getCachedData<T>(requestId)?.let {
                Log.d("MovieApi", "Found cached data for $requestId")
                send(it)
                return@channelFlow
            }
            Log.d("MovieApi", "No cached data found for $requestId")
            throw MovieApiException.NetworkError("No cached data found for $requestId")
        }

        // Try to get from cache first if not expired
        getCachedData<T>(requestId)?.let {
            Log.d("MovieApi", "Found cached data for $requestId")
            send(it)
            if (!isCacheExpired(requestId, cacheDuration)) {
                Log.d("MovieApi", "Cache is still valid for $requestId")
                return@channelFlow
            }
            Log.d("MovieApi", "Cache is expired for $requestId")
        }

        // Cancel existing request if any
        activeRequests[requestId]?.cancel()
        activeRequests[requestId] = scope.launch { }

        var currentDelay = INITIAL_BACKOFF_DELAY
        var lastError: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                Log.d("MovieApi", "Attempt ${attempt + 1} for $requestId")
                val response = withContext(dispatcher) {
                    request()
                }
                // Cache successful response
                cacheMutex.withLock {
                    requestCache[requestId] = CacheEntry(response)
                }
                Log.d("MovieApi", "Request successful for $requestId")
                send(response)
                return@channelFlow
            } catch (e: CancellationException) {
                throw e
            } catch (e: ResponseException) {
                lastError = e
                Log.e("MovieApi", "Response error for $requestId: ${e.message}")
                val mappedException = exceptionMapper.map(e)
                if (!mappedException.isRetryable() || attempt == maxRetries - 1) {
                    throw mappedException
                }
                delay(currentDelay)
                currentDelay *= BACKOFF_MULTIPLIER
            } catch (e: Exception) {
                lastError = e
                Log.e("MovieApi", "Error for $requestId: ${e.message}")
                if (attempt == maxRetries - 1) {
                    throw MovieApiException.NetworkError("Error for $requestId: ${e.message}")
                }
                delay(currentDelay)
                currentDelay *= BACKOFF_MULTIPLIER
            }
        }

        throw lastError ?: MovieApiException.UnknownError("All retries failed")
    }
    .buffer(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    .onCompletion {
        activeRequests.remove(requestId)
    }
    .catch { e ->
        when (e) {
            is CancellationException -> throw e
            is MovieApiException -> throw e
            else -> throw MovieApiException.UnknownError(e.message ?: "Unknown error occurred")
        }
    }
    .flowOn(dispatcher)

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> getCachedData(requestId: String): T? {
        return cacheMutex.withLock {
            (requestCache[requestId]?.data as? T)
        }
    }

    private suspend fun isCacheExpired(requestId: String, duration: Long): Boolean {
        return cacheMutex.withLock {
            val entry = requestCache[requestId] ?: return true
            (System.currentTimeMillis() - entry.timestamp) > duration
        }
    }

    override suspend fun cleanup() {
        activeRequests.values.forEach { it.cancel() }
        activeRequests.clear()
        clearCache()
        scope.cancel()
    }

    override fun clearCache() {
        requestCache.clear()
    }

    private fun MovieApiException.isRetryable(): Boolean = when (this) {
        is MovieApiException.RateLimitExceeded,
        is MovieApiException.ServerError,
        is MovieApiException.NetworkError -> true
        else -> false
    }

    companion object {
        private val INITIAL_BACKOFF_DELAY = 1.seconds
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val maxRetries = 3

        // Cache durations
        private const val CACHE_DURATION_POPULAR = 5 * 60 * 1000L // 5 minutes
        private const val CACHE_DURATION_DETAILS = 30 * 60 * 1000L // 30 minutes
        private const val CACHE_DURATION_VIDEOS = 60 * 60 * 1000L // 1 hour
        private const val CACHE_DURATION_SIMILAR = 30 * 60 * 1000L // 30 minutes
        private const val CACHE_DURATION_RECOMMENDATIONS = 30 * 60 * 1000L // 30 minutes
        private const val CACHE_DURATION_SEARCH = 5 * 60 * 1000L // 5 minutes
        private const val CACHE_DURATION_GENRES = 24 * 60 * 60 * 1000L // 24 hours
    }
} 