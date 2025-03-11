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

/**
 * Enhanced utility object for Firebase with offline detection and shared preferences backup
 */
object FirebaseUtils {
    private const val TAG = "FirebaseUtils"

    // Shared preferences constants
    private const val PREFS_NAME = "LifeMaxxAppPrefs"
    private const val KEY_OFFLINE_MODE = "offline_mode"

    // Offline mode flow that can be observed by components
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

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

        // Mark that we've attempted initialization
        initializationAttempted = true

        // Check current network connectivity
        val isConnected = isNetworkAvailable(context)
        if (!isConnected) {
            Log.w(TAG, "No network connection, setting offline mode")
            setOfflineMode(context, true)
            return false
        }

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
     * Check if app is in offline mode
     */
    fun isOfflineMode(context: Context): Boolean {
        // Check stored preference first
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
                "nutritionEntries"
            )

            for (collection in collections) {
                firestore.collection(collection)
                    .document("placeholder")
                    .set(mapOf("initialized" to true))
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