package com.xyldrun.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

interface NetworkMonitor {
    fun isNetworkAvailable(): Boolean
    fun observeNetworkState(): Flow<Boolean>
}

class NetworkMonitorImpl(private val context: Context) : NetworkMonitor {
    val isConnected: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            private var activeNetworks: MutableSet<Network> = mutableSetOf()

            override fun onAvailable(network: Network) {
                activeNetworks.add(network)
                launch { send(true) }
            }

            override fun onLost(network: Network) {
                activeNetworks.remove(network)
                launch { send(activeNetworks.isNotEmpty()) }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    activeNetworks.add(network)
                } else {
                    activeNetworks.remove(network)
                }
                launch { send(activeNetworks.isNotEmpty()) }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state
        val currentState = connectivityManager.activeNetwork
        if (currentState != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(currentState)
            send(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        } else {
            send(false)
        }

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .distinctUntilChanged()
        .conflate()

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        return activeNetwork != null && connectivityManager.getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun observeNetworkState(): Flow<Boolean> {
        return isConnected
    }
} 