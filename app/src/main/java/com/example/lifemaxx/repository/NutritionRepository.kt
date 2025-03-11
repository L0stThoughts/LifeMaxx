package com.example.lifemaxx.repository

import android.content.Context
import android.util.Log
import com.example.lifemaxx.model.NutritionEntry
import com.example.lifemaxx.util.FirebaseFailsafeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for nutrition entries with failsafe offline functionality
 */
class NutritionRepository(private val context: Context) {
    private val TAG = "NutritionRepository"

    /**
     * Add a new nutrition entry with local fallback
     */
    suspend fun addNutritionEntry(entry: NutritionEntry): Boolean {
        return try {
            // Use the failsafe utility to handle offline mode
            val success = FirebaseFailsafeUtil.addNutritionEntryWithFallback(context, entry)
            Log.d(TAG, "Added nutrition entry with failsafe: ${entry.foodName}")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error adding nutrition entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get all nutrition entries for a user on a specific date
     */
    suspend fun getNutritionEntriesByDate(userId: String, date: String): List<NutritionEntry> {
        return try {
            // Use the failsafe utility to handle offline mode
            val entries = FirebaseFailsafeUtil.getNutritionEntriesWithFallback(context, userId, date)
            Log.d(TAG, "Fetched ${entries.size} nutrition entries with failsafe")
            entries
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nutrition entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update a nutrition entry
     */
    suspend fun updateNutritionEntry(entryId: String, updatedData: Map<String, Any>): Boolean {
        if (entryId.isBlank()) {
            Log.e(TAG, "Cannot update entry with blank ID")
            return false
        }

        return try {
            // For entries created locally (with local_ prefix), we only update locally
            // For normal entries, we update in Firebase when possible, but always update locally
            if (entryId.startsWith("local_")) {
                // Just update local storage
                val success = FirebaseFailsafeUtil.updateNutritionEntry(context, entryId, updatedData)
                Log.d(TAG, "Updated local nutrition entry: $entryId")
                success
            } else {
                // Try Firebase but fallback to local
                val success = FirebaseFailsafeUtil.updateSupplementWithFallback(context, entryId, updatedData)
                if (!success) {
                    Log.e(TAG, "Failed to update nutrition entry: $entryId")
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating nutrition entry: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a nutrition entry
     */
    suspend fun deleteNutritionEntry(entryId: String): Boolean {
        if (entryId.isBlank()) {
            Log.e(TAG, "Cannot delete entry with blank ID")
            return false
        }

        return try {
            val success = FirebaseFailsafeUtil.deleteSupplementWithFallback(context, entryId)
            Log.d(TAG, "Deleted nutrition entry: $entryId")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting nutrition entry: ${e.message}", e)
            false
        }
    }

    /**
     * Try to sync locally stored nutrition entries with Firebase if we're back online.
     */
    suspend fun syncLocalData(): Any {
        return withContext(Dispatchers.IO) {
            try {
                if (FirebaseFailsafeUtil.isNetworkAvailable(context)) {
                    val syncCount = FirebaseFailsafeUtil.syncPendingOperations(context)
                    Log.d(TAG, "Synced $syncCount pending operations")
                    syncCount
                } else {
                    0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing local data: ${e.message}", e)
                0
            }
        }
    }
}