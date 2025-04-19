package com.xyldrun.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.xyldrun.core.util.NetworkMonitor
import com.xyldrun.core.util.NetworkUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.Ignore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class NetworkUtilsTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var networkRequest: NetworkRequest

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        networkRequest = mockk(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        networkMonitor = NetworkUtils(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `isNetworkAvailable returns true when network is available`() = runTest {
        // Given
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // When
        val isAvailable = networkMonitor.isNetworkAvailable()

        // Then
        assertTrue(isAvailable)
        verify { connectivityManager.activeNetwork }
        verify { connectivityManager.getNetworkCapabilities(network) }
        verify { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
    }

    @Test
    fun `isNetworkAvailable returns false when network is unavailable`() = runTest {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val isAvailable = networkMonitor.isNetworkAvailable()

        // Then
        assertFalse(isAvailable)
        verify { connectivityManager.activeNetwork }
    }

    @SuppressLint("IgnoreWithoutReason")
    @Test
    @Ignore("TODO")
    fun `observeNetworkState emits network state changes`() = runTest {
        // Given
        every {
            connectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                any<ConnectivityManager.NetworkCallback>()
            )
        } returns Unit

        // When
        val currentState = networkMonitor.observeNetworkState().first()

        // Then
        assertTrue(currentState)
        verify {
            connectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                ofType<ConnectivityManager.NetworkCallback>()
            )
        }
    }

    @Test
    fun `isNetworkAvailable handles null active network`() = runTest {
        // Given
        every { connectivityManager.activeNetwork } returns null

        // When
        val isAvailable = networkMonitor.isNetworkAvailable()

        // Then
        assertFalse(isAvailable)
        verify { connectivityManager.activeNetwork }
    }

    @Test
    fun `isNetworkAvailable returns false when NetworkCapabilities is null`() = runTest {
        // Given
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        // When
        val isAvailable = networkMonitor.isNetworkAvailable()

        // Then
        assertFalse(isAvailable)
        verify { connectivityManager.activeNetwork }
        verify { connectivityManager.getNetworkCapabilities(network) }
    }

    @Test
    fun `isNetworkAvailable returns false when network has no internet capability`() = runTest {
        // Given
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        // When
        val isAvailable = networkMonitor.isNetworkAvailable()

        // Then
        assertFalse(isAvailable)
        verify { connectivityManager.activeNetwork }
        verify { connectivityManager.getNetworkCapabilities(network) }
        verify { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
    }

    @Test
    fun `isNetworkAvailable validates network capabilities properly`() = runTest {
        // Given
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        // When
        val isAvailable = networkMonitor.isNetworkAvailable()

        // Then
        assertTrue(isAvailable)
        verify { connectivityManager.activeNetwork }
        verify { connectivityManager.getNetworkCapabilities(network) }
        verify { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
    }
} 