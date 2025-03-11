package com.example.lifemaxx.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.lifemaxx.model.WaterIntake
import com.example.lifemaxx.util.FirebaseFailsafeUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Repository for handling water intake data operations with Firestore.
 * Now with offline capability.
 */
class WaterIntakeRepository(private val context: Context) {
    private val TAG = "WaterIntakeRepository"

    // Constants for local storage
    private val PREFS_NAME = "LifeMaxxWaterStorage"
    private val KEY_WATER_INTAKES = "local_water_intakes"
    private val KEY_PENDING_OPS = "pending_water_operations"

    // Get shared preferences for local storage
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // JSON serializer for local storage
    private val gson = Gson()

    // Firestore reference - safely get with null check
    private val db
        get() = FirebaseFailsafeUtil.getFirestore()

    private val waterIntakeCollection
        get() = db?.collection("waterIntakes")

    /**
     * Add a new water intake entry to Firestore with local fallback.
     */
    suspend fun addWaterIntake(entry: WaterIntake): Boolean {
        return try {
            // Generate a local ID if needed
            val entryToAdd = if (entry.id.isEmpty()) {
                entry.copy(id = "local_${System.currentTimeMillis()}")
            } else {
                entry
            }

            // Save to local storage first for immediate feedback
            val localEntries = getAllLocalIntakes().toMutableList()
            localEntries.add(entryToAdd)
            saveLocalIntakes(localEntries)

            // Try Firestore if online
            if (!FirebaseFailsafeUtil.isOfflineMode(context) && waterIntakeCollection != null) {
                try {
                    // Create the doc in Firestore and get auto-generated ID
                    val docRef = waterIntakeCollection!!.add(entryToAdd).await()
                    val newId = docRef.id

                    // Update the document with its ID
                    val updatedEntry = entryToAdd.copy(id = newId)
                    docRef.update("id", newId).await()

                    // Update local storage with the new ID
                    updateLocalIntake(entryToAdd.id, updatedEntry)

                    Log.d(TAG, "Added water intake entry with ID: $newId")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding water intake entry to Firestore: ${e.message}", e)
                    // Fall back to local storage only
                    // Add pending operation for future sync
                    addPendingOperation("add", entryToAdd)
                }
            } else {
                // Add pending operation for future sync
                addPendingOperation("add", entryToAdd)
            }

            // Already added to local storage above
            Log.d(TAG, "Added water intake entry to local storage: ${entryToAdd.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding water intake entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get a water intake entry by its ID with local fallback.
     */
    suspend fun getWaterIntakeById(entryId: String): WaterIntake? {
        return try {
            // Check local storage first for faster response
            val localEntry = getLocalIntakeById(entryId)
            if (localEntry != null) {
                return localEntry
            }

            // Only check Firestore if online and for non-local IDs
            if (!entryId.startsWith("local_") &&
                !FirebaseFailsafeUtil.isOfflineMode(context) &&
                waterIntakeCollection != null) {
                try {
                    val document = waterIntakeCollection!!.document(entryId).get().await()
                    val entry = document.toObject(WaterIntake::class.java)

                    // Cache result locally
                    if (entry != null) {
                        addToLocalCache(entry)
                    }

                    Log.d(TAG, "Retrieved water intake entry from Firestore: $entryId")
                    return entry
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting water intake entry from Firestore: ${e.message}", e)
                    // Fall through to return null
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water intake entry: ${e.message}", e)
            null
        }
    }

    /**
     * Get all water intake entries for a specific user on a date with local fallback.
     */
    suspend fun getWaterIntakesByDate(userId: String, date: String): List<WaterIntake> {
        return try {
            // Try to sync pending operations
            syncPendingOperations()

            // Try Firestore first if online
            if (!FirebaseFailsafeUtil.isOfflineMode(context) && waterIntakeCollection != null) {
                try {
                    val snapshot = waterIntakeCollection!!
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("date", date)
                        .orderBy("time", Query.Direction.ASCENDING)
                        .get()
                        .await()

                    val entries = snapshot.toObjects(WaterIntake::class.java)

                    // Cache results locally
                    entries.forEach { addToLocalCache(it) }

                    Log.d(TAG, "Retrieved ${entries.size} water intakes from Firestore")
                    return entries
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting water intake entries from Firestore: ${e.message}", e)
                    // Fall back to local storage
                }
            }

            // Get from local storage
            val localEntries = getAllLocalIntakes()
            val filteredEntries = localEntries.filter {
                it.userId == userId && it.date == date
            }.sortedBy { it.time }

            Log.d(TAG, "Retrieved ${filteredEntries.size} water intakes from local storage")
            filteredEntries
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water intake entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get all water intake entries for a specific user within a date range.
     */
    suspend fun getWaterIntakesInRange(userId: String, startDate: String, endDate: String): List<WaterIntake> {
        return try {
            // Try Firestore first if online
            if (!FirebaseFailsafeUtil.isOfflineMode(context) && waterIntakeCollection != null) {
                try {
                    val snapshot = waterIntakeCollection!!
                        .whereEqualTo("userId", userId)
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                        .orderBy("date", Query.Direction.ASCENDING)
                        .orderBy("time", Query.Direction.ASCENDING)
                        .get()
                        .await()

                    val entries = snapshot.toObjects(WaterIntake::class.java)

                    // Cache results locally
                    entries.forEach { addToLocalCache(it) }

                    Log.d(TAG, "Retrieved ${entries.size} water intakes in range from Firestore")
                    return entries
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting water intake entries in range from Firestore: ${e.message}", e)
                    // Fall back to local storage
                }
            }

            // Get from local storage
            val localEntries = getAllLocalIntakes()
            val filteredEntries = localEntries.filter { entry ->
                entry.userId == userId &&
                        entry.date >= startDate &&
                        entry.date <= endDate
            }.sortedWith(compareBy({ it.date }, { it.time }))

            Log.d(TAG, "Retrieved ${filteredEntries.size} water intakes in range from local storage")
            filteredEntries
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water intake entries in range: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update a water intake entry with local fallback.
     */
    suspend fun updateWaterIntake(entryId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            if (entryId.isEmpty()) {
                Log.e(TAG, "Cannot update water intake with empty ID")
                return false
            }

            // Update locally first for immediate feedback
            val success = updateLocalIntakeData(entryId, updatedData)
            if (!success) {
                Log.e(TAG, "Failed to update water intake in local storage: $entryId")
                return false
            }

            // Try Firestore if online and not a local-only ID
            if (!entryId.startsWith("local_") &&
                !FirebaseFailsafeUtil.isOfflineMode(context) &&
                waterIntakeCollection != null) {
                try {
                    waterIntakeCollection!!.document(entryId).update(updatedData).await()
                    Log.d(TAG, "Updated water intake entry in Firestore: $entryId")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating water intake entry in Firestore: ${e.message}", e)
                    // Add pending operation for future sync
                    addPendingOperation("update", null, entryId, updatedData)
                }
            } else if (!entryId.startsWith("local_")) {
                // Add pending operation for future sync
                addPendingOperation("update", null, entryId, updatedData)
            }

            // Return the local update success
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error updating water intake entry: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a water intake entry with local fallback.
     */
    suspend fun deleteWaterIntake(entryId: String): Boolean {
        return try {
            if (entryId.isEmpty()) {
                Log.e(TAG, "Cannot delete water intake with empty ID")
                return false
            }

            // Delete locally first for immediate feedback
            val success = deleteLocalIntake(entryId)
            if (!success) {
                Log.e(TAG, "Failed to delete water intake from local storage: $entryId")
                return false
            }

            // Try Firestore if online and not a local-only ID
            if (!entryId.startsWith("local_") &&
                !FirebaseFailsafeUtil.isOfflineMode(context) &&
                waterIntakeCollection != null) {
                try {
                    waterIntakeCollection!!.document(entryId).delete().await()
                    Log.d(TAG, "Deleted water intake entry from Firestore: $entryId")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting water intake entry from Firestore: ${e.message}", e)
                    // Add pending operation for future sync
                    addPendingOperation("delete", null, entryId)
                }
            } else if (!entryId.startsWith("local_")) {
                // Add pending operation for future sync
                addPendingOperation("delete", null, entryId)
            }

            // Return the local delete success
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting water intake entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get total water intake for a specific date.
     */
    suspend fun getTotalWaterIntakeForDate(userId: String, date: String): Int {
        return try {
            val entries = getWaterIntakesByDate(userId, date)
            entries.sumOf { it.amount }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total water intake: ${e.message}", e)
            0
        }
    }

    /**
     * Get water intake entries for the past week.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getRecentWaterIntakes(userId: String): List<WaterIntake> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val sevenDaysAgo = today.minusDays(7)

            val startDate = sevenDaysAgo.format(formatter)
            val endDate = today.format(formatter)

            getWaterIntakesInRange(userId, startDate, endDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent water intakes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get daily totals for the past week.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getWeeklyWaterIntakeTotals(userId: String): Map<String, Int> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val sevenDaysAgo = today.minusDays(7)

            val startDate = sevenDaysAgo.format(formatter)
            val endDate = today.format(formatter)

            val entries = getWaterIntakesInRange(userId, startDate, endDate)

            // Group by date and sum amounts
            entries.groupBy { it.date }
                .mapValues { (_, entries) -> entries.sumOf { it.amount } }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating weekly totals: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * Try to sync pending operations with Firestore.
     */
    suspend fun syncPendingOperations(): Int {
        if (FirebaseFailsafeUtil.isOfflineMode(context) || waterIntakeCollection == null) {
            return 0
        }

        var syncCount = 0
        val pendingOps = getPendingOperations()

        if (pendingOps.isEmpty()) {
            return 0
        }

        val completedOps = mutableListOf<PendingOperation>()

        for (op in pendingOps) {
            try {
                when (op.type) {
                    "add" -> {
                        if (op.waterIntake != null) {
                            // If it's a local ID, let Firestore generate a new one
                            val entryToAdd = if (op.waterIntake.id.startsWith("local_")) {
                                op.waterIntake.copy(id = "")
                            } else {
                                op.waterIntake
                            }

                            val docRef = waterIntakeCollection!!.add(entryToAdd).await()
                            val newId = docRef.id

                            val updatedEntry = entryToAdd.copy(id = newId)
                            docRef.update("id", newId).await()

                            // Update local storage
                            if (op.waterIntake.id.startsWith("local_")) {
                                updateLocalIntake(op.waterIntake.id, updatedEntry)
                            }

                            completedOps.add(op)
                            syncCount++
                        }
                    }
                    "update" -> {
                        if (op.entryId != null && op.data != null) {
                            waterIntakeCollection!!.document(op.entryId).update(op.data).await()
                            completedOps.add(op)
                            syncCount++
                        }
                    }
                    "delete" -> {
                        if (op.entryId != null) {
                            waterIntakeCollection!!.document(op.entryId).delete().await()
                            completedOps.add(op)
                            syncCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing operation: ${e.message}", e)
                // Skip this operation and continue
            }
        }

        // Remove completed operations
        if (completedOps.isNotEmpty()) {
            val updatedOps = pendingOps.toMutableList()
            updatedOps.removeAll(completedOps)
            savePendingOperations(updatedOps)
        }

        Log.d(TAG, "Synced $syncCount water intake operations")
        return syncCount
    }

    //------------ Local Storage Methods ------------//

    /**
     * Data class to represent pending operations
     */
    private data class PendingOperation(
        val type: String, // "add", "update", "delete"
        val waterIntake: WaterIntake? = null,
        val entryId: String? = null,
        val data: Map<String, Any>? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Add a pending operation for future sync
     */
    private fun addPendingOperation(
        type: String,
        waterIntake: WaterIntake? = null,
        entryId: String? = null,
        data: Map<String, Any>? = null
    ) {
        val pendingOps = getPendingOperations().toMutableList()
        pendingOps.add(PendingOperation(type, waterIntake, entryId, data))
        savePendingOperations(pendingOps)
    }

    /**
     * Get all pending operations
     */
    private fun getPendingOperations(): List<PendingOperation> {
        val json = prefs.getString(KEY_PENDING_OPS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PendingOperation>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pending operations: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Save pending operations
     */
    private fun savePendingOperations(operations: List<PendingOperation>) {
        prefs.edit().putString(KEY_PENDING_OPS, gson.toJson(operations)).apply()
    }

    /**
     * Get all water intakes from local storage
     */
    private fun getAllLocalIntakes(): List<WaterIntake> {
        val json = prefs.getString(KEY_WATER_INTAKES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WaterIntake>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local water intakes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Save all water intakes to local storage
     */
    private fun saveLocalIntakes(intakes: List<WaterIntake>) {
        prefs.edit().putString(KEY_WATER_INTAKES, gson.toJson(intakes)).apply()
    }

    /**
     * Get a specific water intake by ID from local storage
     */
    private fun getLocalIntakeById(entryId: String): WaterIntake? {
        return getAllLocalIntakes().find { it.id == entryId }
    }

    /**
     * Add a water intake to local cache
     */
    private fun addToLocalCache(intake: WaterIntake) {
        val intakes = getAllLocalIntakes().toMutableList()
        // Remove if already exists
        intakes.removeIf { it.id == intake.id }
        // Add the new/updated one
        intakes.add(intake)
        saveLocalIntakes(intakes)
    }

    /**
     * Update a water intake in local storage after it's been synced
     */
    private fun updateLocalIntake(oldId: String, newIntake: WaterIntake) {
        val intakes = getAllLocalIntakes().toMutableList()
        val index = intakes.indexOfFirst { it.id == oldId }

        if (index != -1) {
            intakes[index] = newIntake
            saveLocalIntakes(intakes)
        } else {
            // If not found, add it
            addToLocalCache(newIntake)
        }
    }

    /**
     * Update water intake fields in local storage
     */
    private fun updateLocalIntakeData(entryId: String, updatedData: Map<String, Any>): Boolean {
        val intakes = getAllLocalIntakes().toMutableList()
        val index = intakes.indexOfFirst { it.id == entryId }

        if (index == -1) return false

        var updated = intakes[index]

        // Apply each update
        for ((key, value) in updatedData) {
            updated = when (key) {
                "amount" -> updated.copy(amount = (value as Number).toInt())
                "time" -> updated.copy(time = value as Long)
                "containerType" -> updated.copy(containerType = value as String)
                else -> updated // Ignore other fields
            }
        }

        intakes[index] = updated
        saveLocalIntakes(intakes)
        return true
    }

    /**
     * Delete a water intake from local storage
     */
    private fun deleteLocalIntake(entryId: String): Boolean {
        val intakes = getAllLocalIntakes().toMutableList()
        val initialSize = intakes.size

        intakes.removeIf { it.id == entryId }

        if (intakes.size == initialSize) {
            return false // Nothing was removed
        }

        saveLocalIntakes(intakes)
        return true
    }
}