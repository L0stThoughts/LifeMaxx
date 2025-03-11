package com.example.lifemaxx.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifemaxx.model.Dose
import com.example.lifemaxx.util.FirebaseFailsafeUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling dose-related operations (CRUD) with Firestore.
 * Now with offline capability.
 */
class DoseRepository(private val context: Context) {
    private val TAG = "DoseRepository"
    private val PREFS_NAME = "LifeMaxxDoseStorage"
    private val KEY_DOSES = "local_doses"
    private val KEY_PENDING_OPS = "pending_dose_operations"

    // Firestore reference - now using FirebaseFailsafeUtil for safety
    private val db
        get() = FirebaseFailsafeUtil.getFirestore()

    private val doseCollection
        get() = db?.collection("doses")

    // JSON serializer for local storage
    private val gson = Gson()

    // Get SharedPreferences for local storage
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Adds a new Dose document to Firestore.
     * Falls back to local storage if Firebase is unavailable.
     */
    suspend fun addDose(dose: Dose): Boolean {
        return try {
            // First check if we're online and can use Firestore
            if (!FirebaseFailsafeUtil.isOfflineMode(context) && doseCollection != null) {
                try {
                    // If doseId is empty, Firestore will auto-generate an ID.
                    val docRef = if (dose.doseId.isEmpty()) {
                        doseCollection!!.add(dose).await()
                    } else {
                        doseCollection!!.document(dose.doseId).set(dose).await()
                        doseCollection!!.document(dose.doseId)
                    }

                    // If auto-generated, store the ID
                    if (dose.doseId.isEmpty()) {
                        val newId = docRef.id
                        val updatedDose = dose.copy(doseId = newId)
                        docRef.set(updatedDose).await()

                        // Also save locally for offline access
                        saveLocalDose(updatedDose)
                    } else {
                        // Save the dose as-is locally
                        saveLocalDose(dose)
                    }

                    Log.d(TAG, "Added dose to Firestore: ${dose.supplementId}")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed, using local storage: ${e.message}", e)
                    // Continue to local storage
                }
            }

            // Local storage fallback
            val localId = if (dose.doseId.isEmpty()) "local_${System.currentTimeMillis()}" else dose.doseId
            val localDose = dose.copy(doseId = localId)
            saveLocalDose(localDose)

            // Add to pending operations for future sync
            addPendingOperation("add", localDose)

            Log.d(TAG, "Added dose to local storage: $localId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding dose: ${e.message}", e)
            false
        }
    }

    /**
     * Retrieves all Dose documents that match the given date.
     * Falls back to local storage if Firebase is unavailable.
     */
    suspend fun getDosesByDate(date: String): List<Dose> {
        return try {
            // First check if we're online and can use Firestore
            if (!FirebaseFailsafeUtil.isOfflineMode(context) && doseCollection != null) {
                try {
                    val snapshot = doseCollection!!
                        .whereEqualTo("date", date)
                        .get()
                        .await()

                    val doses = snapshot.toObjects(Dose::class.java)

                    // Save to local cache for offline access
                    doses.forEach { saveLocalDose(it) }

                    Log.d(TAG, "Retrieved ${doses.size} doses from Firestore")
                    return doses
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed, using local storage: ${e.message}", e)
                    // Continue to local storage
                }
            }

            // Local storage fallback
            val allDoses = getAllLocalDoses()
            val dateFiltered = allDoses.filter { it.date == date }

            Log.d(TAG, "Retrieved ${dateFiltered.size} doses from local storage")
            dateFiltered
        } catch (e: Exception) {
            Log.e(TAG, "Error getting doses: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Updates fields of a Dose document by doseId.
     * Falls back to local storage if Firebase is unavailable.
     */
    suspend fun updateDose(doseId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            if (doseId.isEmpty()) {
                Log.e(TAG, "Cannot update dose with empty ID")
                return false
            }

            // First check if we're online and can use Firestore
            // Don't attempt Firebase operations for local_ prefixed IDs
            if (!doseId.startsWith("local_") && !FirebaseFailsafeUtil.isOfflineMode(context) && doseCollection != null) {
                try {
                    doseCollection!!.document(doseId).update(updatedData).await()

                    // Also update local copy
                    updateLocalDose(doseId, updatedData)

                    Log.d(TAG, "Updated dose in Firestore: $doseId")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed, using local storage: ${e.message}", e)
                    // Continue to local storage

                    // Add to pending operations for future sync
                    if (!doseId.startsWith("local_")) {
                        addPendingOperation("update", null, doseId, updatedData)
                    }
                }
            } else if (!doseId.startsWith("local_")) {
                // Add to pending operations for future sync
                addPendingOperation("update", null, doseId, updatedData)
            }

            // Local storage fallback
            val success = updateLocalDose(doseId, updatedData)
            if (success) {
                Log.d(TAG, "Updated dose in local storage: $doseId")
            } else {
                Log.e(TAG, "Failed to update dose in local storage: $doseId")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error updating dose: ${e.message}", e)
            false
        }
    }

    /**
     * Retrieves a Dose by its ID (optional function).
     * Falls back to local storage if Firebase is unavailable.
     */
    suspend fun getDoseById(doseId: String): Dose? {
        return try {
            if (doseId.isEmpty()) {
                Log.e(TAG, "Cannot get dose with empty ID")
                return null
            }

            // First check if we're online and can use Firestore
            if (!doseId.startsWith("local_") && !FirebaseFailsafeUtil.isOfflineMode(context) && doseCollection != null) {
                try {
                    val snapshot = doseCollection!!.document(doseId).get().await()
                    val dose = snapshot.toObject(Dose::class.java)

                    if (dose != null) {
                        // Save to local cache
                        saveLocalDose(dose)
                        Log.d(TAG, "Retrieved dose from Firestore: $doseId")
                        return dose
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed, using local storage: ${e.message}", e)
                    // Continue to local storage
                }
            }

            // Local storage fallback
            val localDose = getLocalDoseById(doseId)
            if (localDose != null) {
                Log.d(TAG, "Retrieved dose from local storage: $doseId")
            } else {
                Log.d(TAG, "Dose not found in local storage: $doseId")
            }
            localDose
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dose: ${e.message}", e)
            null
        }
    }

    /**
     * Try to sync locally stored doses with Firebase if we're back online.
     */
    suspend fun syncLocalData(): Int {
        return try {
            if (!FirebaseFailsafeUtil.isNetworkAvailable(context) || doseCollection == null) {
                return 0
            }

            var syncCount = 0
            val pendingOps = getPendingOperations()

            for (op in pendingOps) {
                try {
                    when (op.type) {
                        "add" -> {
                            if (op.dose != null) {
                                // Skip if it's still a local ID and we're adding to Firestore
                                val doseToAdd = if (op.dose.doseId.startsWith("local_")) {
                                    op.dose.copy(doseId = "") // Let Firestore generate an ID
                                } else {
                                    op.dose
                                }

                                val docRef = doseCollection!!.add(doseToAdd).await()
                                val newId = docRef.id

                                // Update with the ID
                                val updatedDose = doseToAdd.copy(doseId = newId)
                                docRef.set(updatedDose).await()

                                // Update local storage
                                if (op.dose.doseId.startsWith("local_")) {
                                    removeLocalDose(op.dose.doseId)
                                    saveLocalDose(updatedDose)
                                }

                                syncCount++
                            }
                        }
                        "update" -> {
                            if (op.doseId != null && op.data != null) {
                                doseCollection!!.document(op.doseId).update(op.data).await()
                                syncCount++
                            }
                        }
                        "delete" -> {
                            if (op.doseId != null) {
                                doseCollection!!.document(op.doseId).delete().await()
                                syncCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing operation: ${e.message}", e)
                    // Continue with next operation
                }
            }

            // Clear successful operations
            if (syncCount > 0) {
                clearPendingOperations()
            }

            Log.d(TAG, "Synced $syncCount dose operations")
            syncCount
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing dose data: ${e.message}", e)
            0
        }
    }

    /**
     * Save a dose to local storage
     */
    private fun saveLocalDose(dose: Dose) {
        val doses = getAllLocalDoses().toMutableList()

        // Remove any existing dose with the same ID
        doses.removeIf { it.doseId == dose.doseId }

        // Add the new dose
        doses.add(dose)

        // Save back to preferences
        prefs.edit()
            .putString(KEY_DOSES, gson.toJson(doses))
            .apply()
    }

    /**
     * Get all doses from local storage
     */
    private fun getAllLocalDoses(): List<Dose> {
        val json = prefs.getString(KEY_DOSES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Dose>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local doses: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get a specific dose by ID from local storage
     */
    private fun getLocalDoseById(doseId: String): Dose? {
        return getAllLocalDoses().find { it.doseId == doseId }
    }

    /**
     * Update a local dose
     */
    private fun updateLocalDose(doseId: String, updatedData: Map<String, Any>): Boolean {
        val doses = getAllLocalDoses().toMutableList()
        val index = doses.indexOfFirst { it.doseId == doseId }

        if (index == -1) return false

        // Get the current dose
        val current = doses[index]

        // Apply all updates
        var updated = current
        for ((key, value) in updatedData) {
            updated = when (key) {
                "supplementId" -> updated.copy(supplementId = value as String)
                "date" -> updated.copy(date = value as String)
                "dailyRequired" -> updated.copy(dailyRequired = (value as Number).toInt())
                "dosesTaken" -> updated.copy(dosesTaken = (value as Number).toInt())
                else -> updated // Ignore unknown fields
            }
        }

        doses[index] = updated
        prefs.edit()
            .putString(KEY_DOSES, gson.toJson(doses))
            .apply()

        return true
    }

    /**
     * Remove a dose from local storage
     */
    private fun removeLocalDose(doseId: String): Boolean {
        val doses = getAllLocalDoses().toMutableList()
        val initialSize = doses.size

        doses.removeIf { it.doseId == doseId }

        if (doses.size == initialSize) {
            return false // Nothing removed
        }

        prefs.edit()
            .putString(KEY_DOSES, gson.toJson(doses))
            .apply()

        return true
    }

    /**
     * Data class to represent a pending operation
     */
    private data class PendingOperation(
        val type: String, // "add", "update", "delete"
        val dose: Dose? = null,
        val doseId: String? = null,
        val data: Map<String, Any>? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Add a pending operation for future sync
     */
    private fun addPendingOperation(
        type: String,
        dose: Dose? = null,
        doseId: String? = null,
        data: Map<String, Any>? = null
    ) {
        val operations = getPendingOperations().toMutableList()
        operations.add(PendingOperation(type, dose, doseId, data))

        prefs.edit()
            .putString(KEY_PENDING_OPS, gson.toJson(operations))
            .apply()
    }

    /**
     * Get all pending operations
     */
    private fun getPendingOperations(): List<PendingOperation> {
        val json = prefs.getString(KEY_PENDING_OPS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingOperation>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pending operations: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Clear all pending operations
     */
    private fun clearPendingOperations() {
        prefs.edit()
            .putString(KEY_PENDING_OPS, "[]")
            .apply()
    }
}