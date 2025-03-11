package com.example.lifemaxx.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.lifemaxx.model.WaterIntake
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for handling water intake data operations with Firestore.
 */
class WaterIntakeRepository {
    private val TAG = "WaterIntakeRepository"
    private val db = FirebaseFirestore.getInstance()
    private val waterIntakeCollection = db.collection("waterIntakes")

    /**
     * Add a new water intake entry to Firestore.
     */
    suspend fun addWaterIntake(entry: WaterIntake): Boolean {
        return try {
            // Create the doc in Firestore and get auto-generated ID
            val docRef = waterIntakeCollection.add(entry).await()
            val newId = docRef.id

            // Update the document with its ID
            docRef.update("id", newId).await()
            Log.d(TAG, "Added water intake entry with ID: $newId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding water intake entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get a water intake entry by its ID.
     */
    suspend fun getWaterIntakeById(entryId: String): WaterIntake? {
        return try {
            val document = waterIntakeCollection.document(entryId).get().await()
            document.toObject(WaterIntake::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water intake entry: ${e.message}", e)
            null
        }
    }

    /**
     * Get all water intake entries for a specific user on a date.
     */
    suspend fun getWaterIntakesByDate(userId: String, date: String): List<WaterIntake> {
        return try {
            val snapshot = waterIntakeCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .orderBy("time", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.toObjects(WaterIntake::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water intake entries by date: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get all water intake entries for a specific user within a date range.
     */
    suspend fun getWaterIntakesInRange(userId: String, startDate: String, endDate: String): List<WaterIntake> {
        return try {
            val snapshot = waterIntakeCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .orderBy("time", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.toObjects(WaterIntake::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water intake entries in range: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update a water intake entry.
     */
    suspend fun updateWaterIntake(entryId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            waterIntakeCollection.document(entryId).update(updatedData).await()
            Log.d(TAG, "Updated water intake entry: $entryId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating water intake entry: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a water intake entry.
     */
    suspend fun deleteWaterIntake(entryId: String): Boolean {
        return try {
            waterIntakeCollection.document(entryId).delete().await()
            Log.d(TAG, "Deleted water intake entry: $entryId")
            true
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
}