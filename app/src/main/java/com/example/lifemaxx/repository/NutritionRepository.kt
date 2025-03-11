package com.example.lifemaxx.repository

import android.util.Log
import com.example.lifemaxx.model.NutritionEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling nutrition entry operations (CRUD) with Firestore.
 */
class NutritionRepository {
    private val TAG = "NutritionRepository"
    private val db = FirebaseFirestore.getInstance()
    private val nutritionCollection = db.collection("nutritionEntries")

    /**
     * Add a new nutrition entry to Firestore.
     */
    suspend fun addNutritionEntry(entry: NutritionEntry): Boolean {
        return try {
            // Create the doc in Firestore and get auto-generated ID
            val docRef = nutritionCollection.add(entry).await()
            val newId = docRef.id

            // Update the document with its ID
            docRef.update("id", newId).await()
            Log.d(TAG, "Added nutrition entry with ID: $newId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding nutrition entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get all nutrition entries for a user on a specific date.
     */
    suspend fun getNutritionEntriesByDate(userId: String, date: String): List<NutritionEntry> {
        return try {
            val snapshot = nutritionCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.toObjects(NutritionEntry::class.java).also {
                Log.d(TAG, "Fetched ${it.size} nutrition entries for $date")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching nutrition entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update a nutrition entry by ID.
     */
    suspend fun updateNutritionEntry(entryId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            nutritionCollection.document(entryId).update(updatedData).await()
            Log.d(TAG, "Updated nutrition entry: $entryId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating nutrition entry $entryId: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a nutrition entry by ID.
     */
    suspend fun deleteNutritionEntry(entryId: String): Boolean {
        return try {
            nutritionCollection.document(entryId).delete().await()
            Log.d(TAG, "Deleted nutrition entry: $entryId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting nutrition entry $entryId: ${e.message}", e)
            false
        }
    }

    /**
     * Get nutrition summary data for the last week.
     */
    suspend fun getWeeklyNutritionSummary(userId: String, endDate: String): List<Map<String, Any>> {
        // This would typically involve more complex queries that would calculate
        // sums per day for the week. For simplicity, we'll implement a basic version.
        // In a real app, you might use Firestore aggregation or do the calculation client-side.
        return try {
            // Implementation details would depend on your date handling and requirements
            // This is a placeholder for the concept
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly nutrition summary: ${e.message}", e)
            emptyList()
        }
    }
}