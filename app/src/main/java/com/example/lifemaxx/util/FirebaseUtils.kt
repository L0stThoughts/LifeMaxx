// 1. First, let's fix the FirebaseUtils.kt file to properly handle offline mode

package com.example.lifemaxx.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Central utility object for Firebase with offline detection and shared preferences backup
 */
object FirebaseUtils {
    private const val TAG = "FirebaseUtils"

    // Shared preferences constants
    private const val PREFS_NAME = "LifeMaxxAppPrefs"
    private const val KEY_OFFLINE_MODE = "offline_mode"
    private const val KEY_MANUAL_OFFLINE_MODE = "manual_offline_mode"

    // Offline mode flow that can be observed by components
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    // Manual offline mode set by user
    private val _isManualOfflineMode = MutableStateFlow(false)
    val isManualOfflineMode: StateFlow<Boolean> = _isManualOfflineMode

    // Track initialization status
    private var isInitialized = false
    private var initializationAttempted = false

    // Gson instance for JSON serialization
    val gson = Gson()

    // Get shared preferences
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Firebase Firestore instance with safe initialization
    val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance().apply {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)  // Enable offline cache
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                firestoreSettings = settings
                Log.d(TAG, "Firestore initialized with persistence enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore: ${e.message}", e)
            // Return a dummy instance that will be replaced when init succeeds
            FirebaseFirestore.getInstance()
        }
    }

    // Firebase Auth instance with safe initialization
    val auth: FirebaseAuth by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Auth: ${e.message}", e)
            // Return a dummy instance that will be replaced when init succeeds
            FirebaseAuth.getInstance()
        }
    }

    /**
     * Initializes Firebase with offline detection
     * Returns true if initialization was successful
     */
    fun initializeFirebase(context: Context): Boolean {
        // Don't try to initialize again if we've already done it successfully
        if (isInitialized) {
            Log.d(TAG, "Firebase already initialized successfully")
            return true
        }

        // Initialize the offline mode manager
        OfflineModeManager.initialize(context)

        // Start observing offline mode changes
        CoroutineScope(Dispatchers.Main).launch {
            OfflineModeManager.isOfflineMode.collect { isOffline ->
                _isOfflineMode.value = isOffline
                Log.d(TAG, "Offline mode updated from manager: $isOffline")
            }
        }

        // Check if we're in offline mode
        if (OfflineModeManager.isOfflineMode.value) {
            Log.d(TAG, "Offline mode is active, skipping Firebase initialization")
            return false
        }

        // Mark that we've attempted initialization
        initializationAttempted = true

        return try {
            // Attempt to initialize Firebase
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Firebase initialized successfully")
            } else {
                Log.d(TAG, "Firebase was already initialized")
            }

            // Configure Firestore settings
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Enable offline cache
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings

            // Initialize collections
            try {
                ensureCollectionsExist()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing collections: ${e.message}", e)
                // Continue even if collection initialization fails
            }

            isInitialized = true
            setOfflineMode(context, false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            setOfflineMode(context, true)
            false
        }
    }

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
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
     * Set offline mode status and store in preferences
     */
    fun setOfflineMode(context: Context, offline: Boolean) {
        _isOfflineMode.value = offline
        getPrefs(context).edit().putBoolean(KEY_OFFLINE_MODE, offline).apply()
        Log.d(TAG, "Offline mode set to: $offline")
    }

    /**
     * Set manual offline mode (user preference)
     */
    fun setManualOfflineMode(context: Context, enabled: Boolean) {
        // Use the OfflineModeManager to set manual offline mode
        OfflineModeManager.setManualOfflineMode(context, enabled)

        // Update our internal state to match
        _isManualOfflineMode.value = enabled
        Log.d(TAG, "Manual offline mode set to: $enabled through manager")
    }

    /**
     * For internal use by OfflineModeManager only
     */
    internal fun setOfflineModeInternal(offlineMode: Boolean) {
        _isOfflineMode.value = offlineMode
        Log.d(TAG, "Offline mode internally set to: $offlineMode")
    }

    /**
     * Check if user has enabled manual offline mode
     */
    fun isManualOfflineModeEnabled(context: Context): Boolean {
        val enabled = getPrefs(context).getBoolean(KEY_MANUAL_OFFLINE_MODE, false)
        _isManualOfflineMode.value = enabled
        return enabled
    }

    /**
     * Check if app is in offline mode
     */
    fun isOfflineMode(context: Context): Boolean {
        // If manual offline mode is enabled, we're definitely offline
        if (isManualOfflineModeEnabled(context)) {
            return true
        }

        // Check stored preference
        val storedOfflineMode = getPrefs(context).getBoolean(KEY_OFFLINE_MODE, false)

        // Also update the flow value to match
        if (_isOfflineMode.value != storedOfflineMode) {
            _isOfflineMode.value = storedOfflineMode
        }

        return storedOfflineMode
    }

    /**
     * Try to go online and sync data if network is available
     */
    fun tryGoOnline(context: Context): Boolean {
        // Don't go online if manual offline mode is enabled
        if (isManualOfflineModeEnabled(context)) {
            return false
        }

        if (isNetworkAvailable(context)) {
            // Try to initialize Firebase if it wasn't before
            if (!isInitialized) {
                val success = initializeFirebase(context)
                if (success) {
                    setOfflineMode(context, false)
                    return true
                }
            } else {
                setOfflineMode(context, false)
                return true
            }
        }
        return false
    }

    /**
     * Ensure required collections exist
     */
    private fun ensureCollectionsExist() {
        try {
            val collections = listOf(
                "supplements",
                "doses",
                "users",
                "reminderSettings",
                "waterIntakes",
                "nutritionEntries",
                "supplementBarcodes",
                "sleepEntries"
            )

            // Create necessary data structure for placeholder
            val placeholderData = mapOf("placeholder" to true, "timestamp" to System.currentTimeMillis())

            for (collection in collections) {
                firestore.collection(collection)
                    .document("placeholder")
                    .set(placeholderData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Collection initialized: $collection")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to initialize collection $collection: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureCollectionsExist: ${e.message}", e)
        }
    }
}