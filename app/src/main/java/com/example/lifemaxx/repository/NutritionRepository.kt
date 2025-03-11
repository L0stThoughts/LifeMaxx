package com.example.lifemaxx.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifemaxx.model.NutritionEntry
import com.example.lifemaxx.util.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.*

/**
 * Repository for nutrition entries with failsafe offline functionality
 */
class NutritionRepository(private val context: Context) {
    private val TAG = "NutritionRepository"

    // Constants for local storage
    private val PREFS_NAME = "LifeMaxxNutritionStorage"
    private val KEY_ENTRIES = "nutrition_entries"
    private val TIMEOUT_MS = 5000L // 5 seconds timeout

    // Get shared preferences for local storage
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // JSON serializer
    private val gson = Gson()

    // Firestore reference - safely get with null check
    private val db: FirebaseFirestore? by lazy {
        try {
            if (FirebaseUtils.initializeFirebase(context)) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}", e)
            null
        }
    }

    // Collection reference
    private val nutritionCollection by lazy {
        db?.collection("nutritionEntries")
    }

    /**
     * Add a new nutrition entry with local fallback
     */
    suspend fun addNutritionEntry(entry: NutritionEntry): Boolean {
        return try {
            // First try Firebase if we're online
            if (!FirebaseUtils.isOfflineMode(context) && nutritionCollection != null) {
                try {
                    withTimeout(TIMEOUT_MS) {
                        // Create the doc in Firestore and get auto-generated ID
                        val docRef = nutritionCollection!!.add(entry).await()
                        val newId = docRef.id

                        // Update with the new ID
                        val updatedEntry = entry.copy(id = newId)
                        docRef.update("id", newId).await()

                        // Save to local cache too
                        saveLocalEntry(updatedEntry)

                        Log.d(TAG, "Added nutrition entry to Firestore with ID: $newId")
                        return@withTimeout true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase operation failed, falling back to local: ${e.message}", e)
                    // Switch to offline mode
                    FirebaseUtils.setOfflineMode(context, true)
                    // Continue to local fallback
                }
            }

            // Local fallback if Firebase failed or we're offline
            val localId = "local_${System.currentTimeMillis()}_${UUID.randomUUID()}"
            val localEntry = entry.copy(id = localId)
            saveLocalEntry(localEntry)
            Log.d(TAG, "Added nutrition entry to local storage: $localId")
            true
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
            // Try Firebase first if online
            if (!FirebaseUtils.isOfflineMode(context) && nutritionCollection != null) {
                try {
                    withTimeout(TIMEOUT_MS) {
                        val snapshot = nutritionCollection!!
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("date", date)
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .get()
                            .await()

                        val entries = snapshot.toObjects(NutritionEntry::class.java)

                        // Cache the results locally
                        entries.forEach { saveLocalEntry(it) }

                        Log.d(TAG, "Fetched ${entries.size} nutrition entries from Firestore")
                        return@withTimeout entries
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase operation failed, falling back to local: ${e.message}", e)
                    // Switch to offline mode
                    FirebaseUtils.setOfflineMode(context, true)
                    // Continue to local fallback
                }
            }

            // Get from local storage
            val allEntries = getAllLocalEntries()
            val filteredEntries = allEntries.filter {
                it.userId == userId && it.date == date
            }.sortedBy { it.timestamp }

            Log.d(TAG, "Retrieved ${filteredEntries.size} entries from local storage")
            filteredEntries
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nutrition entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update a nutrition entry
     */
    suspend fun updateNutritionEntry(entryId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            if (entryId.isBlank()) {
                Log.e(TAG, "Cannot update entry with blank ID")
                return false
            }

            // First try Firebase if we're online
            if (!FirebaseUtils.isOfflineMode(context) &&
                nutritionCollection != null &&
                !entryId.startsWith("local_")) {
                try {
                    withTimeout(TIMEOUT_MS) {
                        nutritionCollection!!.document(entryId).update(updatedData).await()

                        // Also update local entry
                        updateLocalEntry(entryId, updatedData)

                        Log.d(TAG, "Updated nutrition entry in Firestore: $entryId")
                        return@withTimeout true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase operation failed, falling back to local: ${e.message}", e)
                    // Switch to offline mode
                    FirebaseUtils.setOfflineMode(context, true)
                    // Continue to local fallback
                }
            }

            // Local fallback
            val success = updateLocalEntry(entryId, updatedData)
            if (success) {
                Log.d(TAG, "Updated nutrition entry in local storage: $entryId")
                return true
            } else {
                Log.e(TAG, "Failed to update entry in local storage")
                return false
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
        return try {
            if (entryId.isBlank()) {
                Log.e(TAG, "Cannot delete entry with blank ID")
                return false
            }

            // First try Firebase if we're online
            if (!FirebaseUtils.isOfflineMode(context) &&
                nutritionCollection != null &&
                !entryId.startsWith("local_")) {
                try {
                    withTimeout(TIMEOUT_MS) {
                        nutritionCollection!!.document(entryId).delete().await()

                        // Also delete from local storage
                        deleteLocalEntry(entryId)

                        Log.d(TAG, "Deleted nutrition entry from Firestore: $entryId")
                        return@withTimeout true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase operation failed, falling back to local: ${e.message}", e)
                    // Switch to offline mode
                    FirebaseUtils.setOfflineMode(context, true)
                    // Continue to local fallback
                }
            }

            // Local fallback
            val success = deleteLocalEntry(entryId)
            if (success) {
                Log.d(TAG, "Deleted nutrition entry from local storage: $entryId")
                return true
            } else {
                Log.e(TAG, "Failed to delete entry from local storage")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting nutrition entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get all local entries
     */
    private fun getAllLocalEntries(): List<NutritionEntry> {
        val json = preferences.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NutritionEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Save a single entry to local storage
     */
    private fun saveLocalEntry(entry: NutritionEntry) {
        val entries = getAllLocalEntries().toMutableList()

        // Remove entry with same ID if exists
        entries.removeIf { it.id == entry.id }

        // Add the new entry
        entries.add(entry)

        // Save back to preferences
        preferences.edit()
            .putString(KEY_ENTRIES, gson.toJson(entries))
            .apply()
    }

    /**
     * Update a local entry
     */
    private fun updateLocalEntry(entryId: String, updatedData: Map<String, Any>): Boolean {
        val entries = getAllLocalEntries().toMutableList()
        val index = entries.indexOfFirst { it.id == entryId }

        if (index == -1) return false

        val currentEntry = entries[index]
        var updatedEntry = currentEntry

        // Update each field
        updatedData.forEach { (key, value) ->
            when (key) {
                "foodName" -> updatedEntry = updatedEntry.copy(foodName = value as String)
                "calories" -> updatedEntry = updatedEntry.copy(calories = (value as Number).toInt())
                "proteins" -> updatedEntry = updatedEntry.copy(proteins = (value as Number).toDouble())
                "carbs" -> updatedEntry = updatedEntry.copy(carbs = (value as Number).toDouble())
                "fats" -> updatedEntry = updatedEntry.copy(fats = (value as Number).toDouble())
                "servingSize" -> updatedEntry = updatedEntry.copy(servingSize = (value as Number).toDouble())
                "mealType" -> updatedEntry = updatedEntry.copy(mealType = value as String)
            }
        }

        entries[index] = updatedEntry

        preferences.edit()
            .putString(KEY_ENTRIES, gson.toJson(entries))
            .apply()

        return true
    }

    /**
     * Delete a local entry
     */
    private fun deleteLocalEntry(entryId: String): Boolean {
        val entries = getAllLocalEntries().toMutableList()
        val initialSize = entries.size

        entries.removeIf { it.id == entryId }

        // If size hasn't changed, entry wasn't found
        if (entries.size == initialSize) return false

        preferences.edit()
            .putString(KEY_ENTRIES, gson.toJson(entries))
            .apply()

        return true
    }

    /**
     * Try to sync local entries to Firebase when back online
     * Returns the number of entries synced
     */
    suspend fun syncLocalEntriesToFirebase(): Int {
        // Only try if online
        if (FirebaseUtils.isOfflineMode(context) || nutritionCollection == null) {
            return 0
        }

        var syncCount = 0

        try {
            val entries = getAllLocalEntries()
            val localEntries = entries.filter { it.id.startsWith("local_") }

            if (localEntries.isEmpty()) {
                return 0
            }

            for (entry in localEntries) {
                try {
                    // Create new Firestore entry
                    val docRef = nutritionCollection!!.add(entry.copy(id = "")).await()
                    val newId = docRef.id

                    // Update with proper ID
                    val firebaseEntry = entry.copy(id = newId)
                    docRef.set(firebaseEntry).await()

                    // Remove local entry and save Firebase entry
                    deleteLocalEntry(entry.id)
                    saveLocalEntry(firebaseEntry)

                    syncCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing entry ${entry.id}: ${e.message}", e)
                    // Continue with next entry
                }
            }

            Log.d(TAG, "Synced $syncCount nutrition entries to Firebase")
            return syncCount
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync operation: ${e.message}", e)
            return syncCount
        }
    }
}