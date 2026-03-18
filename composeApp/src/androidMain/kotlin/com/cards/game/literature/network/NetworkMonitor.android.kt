package com.cards.game.literature.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object NetworkMonitor {
    private var appContext: Context? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _isNetworkAvailable = MutableStateFlow(true)
    actual val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initial check
        val cm = connectivityManager ?: return
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        _isNetworkAvailable.value = capabilities
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        startMonitoring()
    }

    actual fun startMonitoring() {
        val cm = connectivityManager ?: return
        if (networkCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
            }

            override fun onLost(network: Network) {
                _isNetworkAvailable.value = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                _isNetworkAvailable.value =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        networkCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    actual fun stopMonitoring() {
        val cm = connectivityManager ?: return
        val callback = networkCallback ?: return
        cm.unregisterNetworkCallback(callback)
        networkCallback = null
    }
}
