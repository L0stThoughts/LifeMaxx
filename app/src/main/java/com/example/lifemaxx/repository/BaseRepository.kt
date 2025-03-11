package com.example.lifemaxx.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base repository class with offline mode handling
 */
abstract class BaseRepository(protected val context: Context) {
    private val TAG = "BaseRepository"

    protected val PREFS_NAME = "LifeMaxxStorage"

    /**
     * Check if offline mode is enabled
     */
    protected fun isOfflineMode(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("offline_mode", false)
    }

    /**
     * Execute a database operation safely
     * Will use local storage if offline or if Firestore fails
     */
    protected suspend fun <T> safeOperation(
        firebaseOperation: suspend () -> T,
        localOperation: suspend () -> T,
        fallbackValue: T
    ): T = withContext(Dispatchers.IO) {
        try {
            // If offline mode is enabled, use local storage directly
            if (isOfflineMode()) {
                return@withContext try {
                    localOperation()
                } catch (e: Exception) {
                    Log.e(TAG, "Local operation failed: ${e.message}", e)
                    fallbackValue
                }
            }

            // Try Firebase first
            try {
                firebaseOperation()
            } catch (e: Exception) {
                Log.e(TAG, "Firebase operation failed: ${e.message}", e)

                // Fall back to local operation
                try {
                    localOperation()
                } catch (e2: Exception) {
                    Log.e(TAG, "Local fallback failed: ${e2.message}", e2)
                    fallbackValue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Operation completely failed: ${e.message}", e)
            fallbackValue
        }
    }
}