package com.example.lifemaxx.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A utility to manage offline mode and network connectivity changes.
 * Handles manual offline mode setting and auto-detection of network status.
 */
object OfflineModeManager {
    private const val TAG = "OfflineModeManager"
    private const val PREFS_NAME = "LifeMaxxOfflinePrefs"
    private const val KEY_MANUAL_OFFLINE = "manual_offline_mode"

    // State flows for observing connectivity status
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private val _isManualOfflineMode = MutableStateFlow(false)
    val isManualOfflineMode: StateFlow<Boolean> = _isManualOfflineMode

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isInitialized = false

    /**
     * Initialize the manager and start monitoring network status
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        // Initialize state from preferences
        _isManualOfflineMode.value = getManualOfflineMode(context)

        // Set initial network status
        _isNetworkAvailable.value = checkNetworkAvailability(context)

        // Update overall offline mode
        updateOfflineMode()

        // Register network callback
        registerNetworkCallback(context)

        isInitialized = true
        Log.d(TAG, "OfflineModeManager initialized, manual mode: ${_isManualOfflineMode.value}, network available: ${_isNetworkAvailable.value}")
    }

    /**
     * Set manual offline mode (user preference)
     */
    fun setManualOfflineMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MANUAL_OFFLINE, enabled).apply()
        _isManualOfflineMode.value = enabled

        // Update overall offline mode
        updateOfflineMode()

        Log.d(TAG, "Manual offline mode set to: $enabled")

        // If turning off manual mode, try to sync data
        if (!enabled) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncRepositories(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing repositories: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Get the current manual offline mode setting
     */
    fun getManualOfflineMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MANUAL_OFFLINE, false)
    }

    /**
     * Check if the device currently has network connectivity
     */
    fun checkNetworkAvailability(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    /**
     * Register a callback to monitor network changes
     */
    private fun registerNetworkCallback(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Create network callback
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available")
                    _isNetworkAvailable.value = true
                    updateOfflineMode()

                    // Try to sync data if not in manual offline mode
                    if (!_isManualOfflineMode.value) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                syncRepositories(context)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing repositories on network available: ${e.message}", e)
                            }
                        }
                    }
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network lost")
                    _isNetworkAvailable.value = false
                    updateOfflineMode()
                }
            }

            // Register the callback
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            Log.d(TAG, "Network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback: ${e.message}", e)
        }
    }

    /**
     * Update the overall offline mode based on manual setting and network availability
     */
    private fun updateOfflineMode() {
        _isOfflineMode.value = _isManualOfflineMode.value || !_isNetworkAvailable.value
        // Update FirebaseUtils to match
        FirebaseUtils.setOfflineModeInternal(_isOfflineMode.value)
        Log.d(TAG, "Overall offline mode updated to: ${_isOfflineMode.value}")
    }

    /**
     * Sync all repositories when coming back online
     */
    private suspend fun syncRepositories(context: Context) {
        // This would be implemented to call sync methods on all repositories
        // For now, just a placeholder that will be expanded based on DI
        Log.d(TAG, "syncRepositories called, would sync all repositories here")
    }

    /**
     * Clean up when app is destroyed
     */
    fun cleanup(context: Context) {
        try {
            networkCallback?.let {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
                Log.d(TAG, "Network callback unregistered")
            }
            networkCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback: ${e.message}", e)
        }
    }
}