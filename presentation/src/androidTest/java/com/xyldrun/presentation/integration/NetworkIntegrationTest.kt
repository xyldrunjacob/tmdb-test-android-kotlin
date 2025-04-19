package com.xyldrun.presentation.integration

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.core.util.NetworkUtils
import com.xyldrun.data.api.MovieApi
import com.xyldrun.data.repository.MovieRepositoryImpl
import com.xyldrun.domain.model.Movie
import com.xyldrun.presentation.movielist.MovieListViewModel
import com.xyldrun.presentation.util.NetworkState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NetworkIntegrationTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var connectivityManager: ConnectivityManager

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var movieApi: MovieApi
    private lateinit var movieRepository: MovieRepositoryImpl
    private lateinit var movieListViewModel: MovieListViewModel
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager = mockk(relaxed = true)
        network = mockk(relaxed = true)
        networkCapabilities = mockk(relaxed = true)
        movieApi = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities

        networkMonitor = NetworkUtils(context)
        movieRepository = MovieRepositoryImpl(movieApi)
        movieListViewModel = MovieListViewModel(movieRepository)
    }

    @Test
    fun testNetworkStateChanges() = runTest {
        // Initial state
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        assertTrue(networkMonitor.isNetworkAvailable())
        assertEquals(NetworkState.Available, networkMonitor.networkState.first())

        // Simulate network loss
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        val callback = slot<ConnectivityManager.NetworkCallback>()
        verify { connectivityManager.registerDefaultNetworkCallback(capture(callback)) }

        callback.captured.onLost(network)

        assertFalse(networkMonitor.isNetworkAvailable())
        assertEquals(NetworkState.Unavailable, networkMonitor.networkState.first())
    }

    @Test
    fun testRepositoryNetworkIntegration() = runTest {
        val testMovies = List(3) { index ->
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

        // Test with network available
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        coEvery { movieApi.getPopularMovies(any()) } returns kotlinx.coroutines.flow.flowOf(testMovies)

        val result = movieRepository.getPopularMovies(1).first()
        assertEquals(testMovies, result)

        // Test with network unavailable
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        val callback = slot<ConnectivityManager.NetworkCallback>()
        verify { connectivityManager.registerDefaultNetworkCallback(capture(callback)) }

        callback.captured.onLost(network)

        // Should return cached data
        val cachedResult = movieRepository.getPopularMovies(1).first()
        assertEquals(testMovies, cachedResult)
    }

    @Test
    fun testViewModelNetworkIntegration() = runTest {
        // Test initial load with network
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        movieListViewModel.refresh()

        // Verify loading state
        assertTrue(movieListViewModel.uiState.first() is com.xyldrun.presentation.movielist.MovieListUiState.Loading)

        // Test network loss during load
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        val callback = slot<ConnectivityManager.NetworkCallback>()
        verify { connectivityManager.registerDefaultNetworkCallback(capture(callback)) }

        callback.captured.onLost(network)

        // Should show error state
        assertTrue(movieListViewModel.uiState.first() is com.xyldrun.presentation.movielist.MovieListUiState.Error)
    }

    @Test
    fun testNetworkReconnection() = runTest {
        // Start with no network
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        val callback = slot<ConnectivityManager.NetworkCallback>()
        verify { connectivityManager.registerDefaultNetworkCallback(capture(callback)) }

        callback.captured.onLost(network)

        assertEquals(NetworkState.Unavailable, networkMonitor.networkState.first())

        // Simulate network reconnection
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        callback.captured.onAvailable(network)
        callback.captured.onCapabilitiesChanged(network, networkCapabilities)

        assertEquals(NetworkState.Available, networkMonitor.networkState.first())
        assertTrue(networkMonitor.isNetworkAvailable())
    }

    @Test
    fun testNetworkQualityChanges() = runTest {
        val callback = slot<ConnectivityManager.NetworkCallback>()
        verify { connectivityManager.registerDefaultNetworkCallback(capture(callback)) }

        // Test different network types
        val wifiCapabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        }

        callback.captured.onCapabilitiesChanged(network, wifiCapabilities)
        assertTrue(networkMonitor.isNetworkAvailable())

        val cellularCapabilities = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        }

        callback.captured.onCapabilitiesChanged(network, cellularCapabilities)
        assertTrue(networkMonitor.isNetworkAvailable())
    }
} 