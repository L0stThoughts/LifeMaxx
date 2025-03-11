package com.example.lifemaxx.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Repository for handling supplement-related operations (CRUD) with 100% crash protection.
 * Will NEVER throw exceptions to higher layers and always return sensible fallbacks.
 */
class SupplementRepository(private val context: Context) {
    private val TAG = "SupplementRepository"

    // Constants for local storage
    private val PREFS_NAME = "LifeMaxxSupplementStorage"
    private val KEY_SUPPLEMENTS = "local_supplements"
    private val TIMEOUT_MS = 5000L // 5 seconds timeout

    // Get shared preferences for local storage
    private val prefs: SharedPreferences by lazy {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SharedPreferences: ${e.message}", e)
            // Create an in-memory fallback if we can't get shared prefs
            object : SharedPreferences {
                private val map = mutableMapOf<String, Any?>()

                override fun getAll(): MutableMap<String, *> = map
                override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
                override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
                    map[key] as? MutableSet<String> ?: defValues
                override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
                override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
                override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
                override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
                override fun contains(key: String): Boolean = map.containsKey(key)
                override fun edit(): SharedPreferences.Editor =
                    object : SharedPreferences.Editor {
                        private val edits = mutableMapOf<String, Any?>()

                        override fun putString(key: String, value: String?): SharedPreferences.Editor {
                            edits[key] = value
                            return this
                        }
                        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                            edits[key] = values
                            return this
                        }
                        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                            edits[key] = value
                            return this
                        }
                        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                            edits[key] = value
                            return this
                        }
                        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                            edits[key] = value
                            return this
                        }
                        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                            edits[key] = value
                            return this
                        }
                        override fun remove(key: String): SharedPreferences.Editor {
                            edits[key] = null
                            return this
                        }
                        override fun clear(): SharedPreferences.Editor {
                            edits.clear()
                            return this
                        }
                        override fun commit(): Boolean {
                            for ((k, v) in edits) {
                                if (v == null) map.remove(k) else map[k] = v
                            }
                            return true
                        }
                        override fun apply() {
                            commit()
                        }
                    }
                override fun registerOnSharedPreferenceChangeListener(
                    listener: SharedPreferences.OnSharedPreferenceChangeListener?
                ) {}
                override fun unregisterOnSharedPreferenceChangeListener(
                    listener: SharedPreferences.OnSharedPreferenceChangeListener?
                ) {}
            }
        }
    }

    // JSON serializer
    private val gson: Gson by lazy {
        Gson()
    }

    // Firestore database reference - with safety
    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}", e)
            null
        }
    }

    // Firestore collection reference
    private val supplementCollection by lazy {
        try {
            db?.collection("supplements")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get supplements collection: ${e.message}", e)
            null
        }
    }

    /**
     * GUARANTEED to never throw an exception.
     * Add a new supplement to Firestore, then store the auto-generated ID
     * back into the doc's "id" field so updates/deletes can work.
     * Falls back to local storage if Firestore is unavailable.
     */
    suspend fun addSupplement(supplement: Supplement): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Generate a local ID if needed
                val supplementToAdd = if (supplement.id.isBlank()) {
                    supplement.copy(id = "local_${System.currentTimeMillis()}")
                } else {
                    supplement
                }

                // First save to local storage
                saveLocalSupplement(supplementToAdd)

                // Try to save to Firestore if available
                if (supplementCollection != null) {
                    try {
                        val docRef = if (supplementToAdd.id.startsWith("local_")) {
                            // Let Firestore generate a new ID
                            withTimeoutOrNull(TIMEOUT_MS) {
                                supplementCollection?.add(mapOf(
                                    "name" to supplementToAdd.name,
                                    "dailyDose" to supplementToAdd.dailyDose,
                                    "measureUnit" to supplementToAdd.measureUnit,
                                    "remainingQuantity" to supplementToAdd.remainingQuantity,
                                    "isTaken" to supplementToAdd.isTaken
                                ))?.await()
                            }
                        } else {
                            // Use the provided ID
                            withTimeoutOrNull(TIMEOUT_MS) {
                                supplementCollection?.document(supplementToAdd.id)?.set(supplementToAdd)?.await()
                                supplementCollection?.document(supplementToAdd.id)
                            }
                        }

                        if (supplementToAdd.id.startsWith("local_") && docRef != null) {
                            // Get the Firestore-generated ID
                            val firestoreId = docRef.id

                            // Update with the ID
                            val updatedSupplement = supplementToAdd.copy(id = firestoreId)
                            withTimeoutOrNull(TIMEOUT_MS) {
                                docRef.update("id", firestoreId).await()
                            }

                            // Update local storage
                            replaceLocalSupplement(supplementToAdd.id, updatedSupplement)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding supplement to Firestore: ${e.message}", e)
                        // Continue with local storage only
                    }
                }

                Log.d(TAG, "Added supplement: ${supplementToAdd.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error adding supplement: ${e.message}", e)
                false
            }
        }
    }

    /**
     * GUARANTEED to never throw an exception or return null.
     * Fetch all supplements. Falls back to local storage and defaults.
     */
    suspend fun getSupplements(): List<Supplement> {
        return withContext(Dispatchers.IO) {
            try {
                // Try Firestore first
                if (supplementCollection != null) {
                    try {
                        val snapshot = withTimeoutOrNull(TIMEOUT_MS) {
                            supplementCollection?.get()?.await()
                        }

                        if (snapshot != null) {
                            val supplements = snapshot.toObjects(Supplement::class.java)

                            // Save to local storage
                            saveLocalSupplements(supplements)

                            Log.d(TAG, "Fetched ${supplements.size} supplements from Firestore")
                            return@withContext supplements
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching supplements from Firestore: ${e.message}", e)
                        // Fall back to local storage
                    }
                }

                // Get from local storage
                val localSupplements = getLocalSupplements()
                if (localSupplements.isNotEmpty()) {
                    Log.d(TAG, "Fetched ${localSupplements.size} supplements from local storage")
                    return@withContext localSupplements
                }

                // Final fallback: default data
                val defaultSupplements = getDemoSupplements()
                Log.d(TAG, "Using ${defaultSupplements.size} demo supplements as fallback")
                return@withContext defaultSupplements
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in getSupplements: ${e.message}", e)
                getDemoSupplements()
            }
        }
    }

    /**
     * GUARANTEED to never throw an exception.
     * Update fields in a supplement doc by its [supplementId].
     * Falls back to local storage if Firestore is unavailable.
     */
    suspend fun updateSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (supplementId.isBlank()) {
                    Log.e(TAG, "Cannot update supplement with blank ID")
                    return@withContext false
                }

                // First update local storage
                val localSuccess = updateLocalSupplement(supplementId, updatedData)

                // Try to update Firestore if available
                if (!supplementId.startsWith("local_") && supplementCollection != null) {
                    try {
                        withTimeoutOrNull(TIMEOUT_MS) {
                            supplementCollection?.document(supplementId)?.update(updatedData)?.await()
                        }
                        Log.d(TAG, "Updated supplement in Firestore: $supplementId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating supplement in Firestore: ${e.message}", e)
                        // Continue with local update only
                    }
                }

                return@withContext localSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement: ${e.message}", e)
                false
            }
        }
    }

    /**
     * GUARANTEED to never throw an exception.
     * Delete a supplement doc. Falls back to local storage if Firestore is unavailable.
     */
    suspend fun deleteSupplement(supplementId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (supplementId.isBlank()) {
                    Log.e(TAG, "Cannot delete supplement with blank ID")
                    return@withContext false
                }

                // First delete from local storage
                val localSuccess = deleteLocalSupplement(supplementId)

                // Try to delete from Firestore if available
                if (!supplementId.startsWith("local_") && supplementCollection != null) {
                    try {
                        withTimeoutOrNull(TIMEOUT_MS) {
                            supplementCollection?.document(supplementId)?.delete()?.await()
                        }
                        Log.d(TAG, "Deleted supplement from Firestore: $supplementId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting supplement from Firestore: ${e.message}", e)
                        // Continue with local deletion only
                    }
                }

                return@withContext localSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting supplement: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Try to sync locally stored supplements with Firebase.
     * GUARANTEED to never throw an exception.
     */
    suspend fun syncLocalData(): Int {
        return withContext(Dispatchers.IO) {
            if (supplementCollection == null) return@withContext 0

            try {
                var syncCount = 0
                val localSupplements = getLocalSupplements()
                val localOnlySupplements = localSupplements.filter { it.id.startsWith("local_") }

                for (supplement in localOnlySupplements) {
                    try {
                        // Create new document in Firestore
                        val docRef = withTimeoutOrNull(TIMEOUT_MS) {
                            supplementCollection?.add(mapOf(
                                "name" to supplement.name,
                                "dailyDose" to supplement.dailyDose,
                                "measureUnit" to supplement.measureUnit,
                                "remainingQuantity" to supplement.remainingQuantity,
                                "isTaken" to supplement.isTaken
                            ))?.await()
                        }

                        if (docRef != null) {
                            // Update with proper ID
                            val firestoreId = docRef.id
                            val updatedSupplement = supplement.copy(id = firestoreId)

                            withTimeoutOrNull(TIMEOUT_MS) {
                                docRef.update("id", firestoreId).await()
                            }

                            // Update local storage
                            replaceLocalSupplement(supplement.id, updatedSupplement)

                            syncCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing supplement ${supplement.id}: ${e.message}", e)
                        // Continue with next supplement
                    }
                }

                Log.d(TAG, "Synced $syncCount supplements to Firestore")
                syncCount
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing local data: ${e.message}", e)
                0
            }
        }
    }

    /**
     * Get all supplements from local storage.
     * GUARANTEED to never throw an exception or return null.
     */
    private fun getLocalSupplements(): List<Supplement> {
        return try {
            val json = prefs.getString(KEY_SUPPLEMENTS, null)
            if (json.isNullOrEmpty()) {
                return emptyList()
            }

            val type = object : TypeToken<List<Supplement>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local supplements: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Save a list of supplements to local storage.
     * GUARANTEED to never throw an exception.
     */
    private fun saveLocalSupplements(supplements: List<Supplement>) {
        try {
            val json = gson.toJson(supplements)
            prefs.edit().putString(KEY_SUPPLEMENTS, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local supplements: ${e.message}", e)
        }
    }

    /**
     * Save a single supplement to local storage.
     * GUARANTEED to never throw an exception.
     */
    private fun saveLocalSupplement(supplement: Supplement) {
        try {
            val supplements = getLocalSupplements().toMutableList()

            // Remove if already exists
            supplements.removeIf { it.id == supplement.id }

            // Add the new one
            supplements.add(supplement)

            saveLocalSupplements(supplements)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local supplement: ${e.message}", e)
        }
    }

    /**
     * Update a supplement in local storage.
     * GUARANTEED to never throw an exception.
     */
    private fun updateLocalSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        try {
            val supplements = getLocalSupplements().toMutableList()
            val index = supplements.indexOfFirst { it.id == supplementId }

            if (index == -1) return false

            var updated = supplements[index]

            // Apply each update
            for ((key, value) in updatedData) {
                updated = when (key) {
                    "name" -> updated.copy(name = value as String)
                    "dailyDose" -> updated.copy(dailyDose = (value as Number).toInt())
                    "measureUnit" -> updated.copy(measureUnit = value as String)
                    "remainingQuantity" -> updated.copy(remainingQuantity = (value as Number).toInt())
                    "isTaken" -> updated.copy(isTaken = value as Boolean)
                    else -> updated // Ignore unknown fields
                }
            }

            supplements[index] = updated
            saveLocalSupplements(supplements)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local supplement: ${e.message}", e)
            return false
        }
    }

    /**
     * Replace a supplement in local storage with a new one.
     * GUARANTEED to never throw an exception.
     */
    private fun replaceLocalSupplement(oldId: String, newSupplement: Supplement): Boolean {
        try {
            val supplements = getLocalSupplements().toMutableList()
            val index = supplements.indexOfFirst { it.id == oldId }

            if (index == -1) {
                // If not found, just add the new one
                supplements.add(newSupplement)
            } else {
                // Replace the old one
                supplements[index] = newSupplement
            }

            saveLocalSupplements(supplements)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing local supplement: ${e.message}", e)
            return false
        }
    }

    /**
     * Delete a supplement from local storage.
     * GUARANTEED to never throw an exception.
     */
    private fun deleteLocalSupplement(supplementId: String): Boolean {
        try {
            val supplements = getLocalSupplements().toMutableList()
            val initialSize = supplements.size

            supplements.removeIf { it.id == supplementId }

            if (supplements.size == initialSize) {
                return false // Nothing was removed
            }

            saveLocalSupplements(supplements)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local supplement: ${e.message}", e)
            return false
        }
    }

    /**
     * Get demo supplements as a fallback.
     * GUARANTEED to never throw an exception or return null.
     */
    private fun getDemoSupplements(): List<Supplement> {
        return listOf(
            Supplement(
                id = "demo_1",
                name = "Vitamin D (Demo)",
                dailyDose = 1,
                measureUnit = "pill",
                remainingQuantity = 30
            ),
            Supplement(
                id = "demo_2",
                name = "Magnesium (Demo)",
                dailyDose = 2,
                measureUnit = "pill",
                remainingQuantity = 60
            ),
            Supplement(
                id = "demo_3",
                name = "Fish Oil (Demo)",
                dailyDose = 1,
                measureUnit = "pill",
                remainingQuantity = 45
            )
        )
    }
}