package com.example.lifemaxx.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling supplement-related operations (CRUD).
 * Minimal implementation focused on stability.
 */
class SupplementRepository(private val context: Context) {
    private val TAG = "SupplementRepository"
    private val PREFS_NAME = "LifeMaxxSupplementStorage"
    private val KEY_SUPPLEMENTS = "local_supplements"

    // Get shared preferences for local storage
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Gson for JSON serialization
    private val gson = Gson()

    /**
     * Add a new supplement to local storage.
     */
    suspend fun addSupplement(supplement: Supplement): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Generate a local ID if needed
                val supplementToAdd = if (supplement.id.isEmpty()) {
                    supplement.copy(id = "local_${System.currentTimeMillis()}")
                } else {
                    supplement
                }

                val supplements = getLocalSupplements().toMutableList()
                supplements.add(supplementToAdd)
                saveLocalSupplements(supplements)

                Log.d(TAG, "Added supplement: ${supplementToAdd.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error adding supplement: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Get all supplements from local storage.
     */
    suspend fun getSupplements(): List<Supplement> {
        return withContext(Dispatchers.IO) {
            try {
                val supplements = getLocalSupplements()
                Log.d(TAG, "Retrieved ${supplements.size} supplements")
                supplements
            } catch (e: Exception) {
                Log.e(TAG, "Error getting supplements: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Update a supplement in local storage.
     */
    suspend fun updateSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (supplementId.isEmpty()) {
                    Log.e(TAG, "Cannot update supplement with empty ID")
                    return@withContext false
                }

                val supplements = getLocalSupplements().toMutableList()
                val index = supplements.indexOfFirst { it.id == supplementId }

                if (index == -1) {
                    Log.e(TAG, "Supplement not found with ID: $supplementId")
                    return@withContext false
                }

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

                Log.d(TAG, "Updated supplement: $supplementId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Delete a supplement from local storage.
     */
    suspend fun deleteSupplement(supplementId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (supplementId.isEmpty()) {
                    Log.e(TAG, "Cannot delete supplement with empty ID")
                    return@withContext false
                }

                val supplements = getLocalSupplements().toMutableList()
                val removed = supplements.removeIf { it.id == supplementId }

                if (!removed) {
                    Log.e(TAG, "Supplement not found with ID: $supplementId")
                    return@withContext false
                }

                saveLocalSupplements(supplements)

                Log.d(TAG, "Deleted supplement: $supplementId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting supplement: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Placeholder for sync functionality - empty implementation for stability.
     */
    suspend fun syncLocalData(): Int = 0

    /**
     * Get supplements from local storage.
     */
    private fun getLocalSupplements(): List<Supplement> {
        return try {
            val json = prefs.getString(KEY_SUPPLEMENTS, null)
            if (json.isNullOrEmpty()) {
                // Return demo data if no stored supplements
                return getDemoSupplements()
            }

            val type = object : TypeToken<List<Supplement>>() {}.type
            gson.fromJson(json, type) ?: getDemoSupplements()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading local supplements: ${e.message}", e)
            getDemoSupplements()
        }
    }

    /**
     * Save supplements to local storage.
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
     * Get demo supplements data.
     */
    private fun getDemoSupplements(): List<Supplement> {
        return listOf(
            Supplement(
                id = "demo_1",
                name = "Vitamin D",
                dailyDose = 1,
                measureUnit = "pill",
                remainingQuantity = 30
            ),
            Supplement(
                id = "demo_2",
                name = "Magnesium",
                dailyDose = 2,
                measureUnit = "pill",
                remainingQuantity = 60
            ),
            Supplement(
                id = "demo_3",
                name = "Fish Oil",
                dailyDose = 1,
                measureUnit = "pill",
                remainingQuantity = 45
            )
        )
    }
}