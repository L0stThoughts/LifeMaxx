package com.example.lifemaxx.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifemaxx.model.Dose
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling dose-related operations (CRUD) with Firestore.
 * Simplified implementation for stability.
 */
class DoseRepository(private val context: Context) {
    private val TAG = "DoseRepository"
    private val PREFS_NAME = "LifeMaxxDoseStorage"
    private val KEY_DOSES = "local_doses"

    // Firestore reference with safety
    private var db: FirebaseFirestore? = null
    private var doseCollection: CollectionReference? = null

    // JSON serializer for local storage
    private val gson = Gson()

    // Get SharedPreferences for local storage
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        try {
            db = FirebaseFirestore.getInstance()
            doseCollection = db?.collection("doses")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore: ${e.message}", e)
        }
    }

    /**
     * Adds a new Dose document to Firestore.
     * Falls back to local storage if Firebase is unavailable.
     */
    suspend fun addDose(dose: Dose): Boolean {
        return try {
            // Generate a local ID if needed
            val localId = if (dose.doseId.isEmpty()) "local_${System.currentTimeMillis()}" else dose.doseId
            val localDose = dose.copy(doseId = localId)

            // Always save to local storage first
            saveLocalDose(localDose)

            // If Firestore is available, try to save there too
            if (doseCollection != null) {
                try {
                    // If doseId is local_, Firestore will auto-generate an ID
                    val docRef = if (localDose.doseId.startsWith("local_")) {
                        val doseForFirestore = localDose.copy(doseId = "")
                        doseCollection?.add(doseForFirestore)?.await()
                    } else {
                        doseCollection?.document(localDose.doseId)?.set(localDose)?.await()
                        doseCollection?.document(localDose.doseId)
                    }

                    // If auto-generated, store the ID
                    if (localDose.doseId.startsWith("local_") && docRef != null) {
                        val newId = docRef.id
                        val updatedDose = localDose.copy(doseId = newId)
                        docRef.set(updatedDose).await()

                        // Update local storage with the new ID
                        updateLocalDoseById(localDose.doseId, updatedDose)
                    }

                    Log.d(TAG, "Added dose to Firestore: ${localDose.supplementId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed, using local storage: ${e.message}", e)
                    // Continue with local storage only
                }
            }

            Log.d(TAG, "Added dose to local storage: ${localDose.doseId}")
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
            // Try Firestore first
            if (doseCollection != null) {
                try {
                    val snapshot = doseCollection?.whereEqualTo("date", date)?.get()?.await()

                    if (snapshot != null) {
                        val doses = snapshot.toObjects(Dose::class.java)

                        // Save to local cache for offline access
                        doses.forEach { saveLocalDose(it) }

                        Log.d(TAG, "Retrieved ${doses.size} doses from Firestore")
                        return doses
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed, using local storage: ${e.message}", e)
                    // Fall back to local storage
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

            // Update local storage first
            val localSuccess = updateLocalDose(doseId, updatedData)
            if (!localSuccess) {
                Log.e(TAG, "Failed to update dose in local storage: $doseId")
                return false
            }

            // Try to update in Firestore
            if (doseCollection != null && !doseId.startsWith("local_")) {
                try {
                    doseCollection?.document(doseId)?.update(updatedData)?.await()
                    Log.d(TAG, "Updated dose in Firestore: $doseId")
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore operation failed: ${e.message}", e)
                    // Continue with local storage only
                }
            }

            // Local update succeeded
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating dose: ${e.message}", e)
            false
        }
    }

    /**
     * Sync local data with Firestore.
     */
    suspend fun syncLocalData(): Int {
        return 0 // Simplified - no sync for now
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
     * Update a dose by replacing it completely
     */
    private fun updateLocalDoseById(oldDoseId: String, newDose: Dose): Boolean {
        val doses = getAllLocalDoses().toMutableList()
        val index = doses.indexOfFirst { it.doseId == oldDoseId }

        if (index == -1) {
            // If not found, just add it
            doses.add(newDose)
        } else {
            doses[index] = newDose
        }

        prefs.edit()
            .putString(KEY_DOSES, gson.toJson(doses))
            .apply()

        return true
    }
}