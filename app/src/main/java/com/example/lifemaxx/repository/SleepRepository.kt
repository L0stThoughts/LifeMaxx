package com.example.lifemaxx.repository

import android.util.Log
import com.example.lifemaxx.model.SleepEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for handling sleep tracking data operations with Firestore.
 */
class SleepRepository {
    private val TAG = "SleepRepository"
    private val db = FirebaseFirestore.getInstance()
    private val sleepCollection = db.collection("sleepEntries")

    /**
     * Add a new sleep entry to Firestore.
     */
    suspend fun addSleepEntry(entry: SleepEntry): Boolean {
        return try {
            // Create the doc in Firestore and get auto-generated ID
            val docRef = sleepCollection.add(entry).await()
            val newId = docRef.id

            // Update the document with its ID
            docRef.update("id", newId).await()
            Log.d(TAG, "Added sleep entry with ID: $newId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sleep entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get a sleep entry by its ID.
     */
    suspend fun getSleepEntryById(entryId: String): SleepEntry? {
        return try {
            val document = sleepCollection.document(entryId).get().await()
            document.toObject(SleepEntry::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sleep entry: ${e.message}", e)
            null
        }
    }

    /**
     * Get all sleep entries for a specific user on a date.
     */
    suspend fun getSleepEntriesByDate(userId: String, date: String): List<SleepEntry> {
        return try {
            val snapshot = sleepCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .get()
                .await()

            snapshot.toObjects(SleepEntry::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sleep entries by date: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get all sleep entries for a specific user within a date range.
     */
    suspend fun getSleepEntriesInRange(userId: String, startDate: String, endDate: String): List<SleepEntry> {
        return try {
            val snapshot = sleepCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.toObjects(SleepEntry::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sleep entries in range: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update a sleep entry.
     */
    suspend fun updateSleepEntry(entryId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            sleepCollection.document(entryId).update(updatedData).await()
            Log.d(TAG, "Updated sleep entry: $entryId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sleep entry: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a sleep entry.
     */
    suspend fun deleteSleepEntry(entryId: String): Boolean {
        return try {
            sleepCollection.document(entryId).delete().await()
            Log.d(TAG, "Deleted sleep entry: $entryId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting sleep entry: ${e.message}", e)
            false
        }
    }

    /**
     * Get sleep entries for the past week.
     */
    suspend fun getRecentSleepEntries(userId: String): List<SleepEntry> {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val sevenDaysAgo = today.minusDays(7)

            val startDate = sevenDaysAgo.format(formatter)
            val endDate = today.format(formatter)

            getSleepEntriesInRange(userId, startDate, endDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent sleep entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get the average sleep quality for a user over a specific period.
     */
    suspend fun getAverageSleepQuality(userId: String, days: Int): Double {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val startDate = today.minusDays(days.toLong())

            val entries = getSleepEntriesInRange(
                userId,
                startDate.format(formatter),
                today.format(formatter)
            )

            if (entries.isEmpty()) return 0.0

            entries.map { it.quality }.average()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating average sleep quality: ${e.message}", e)
            0.0
        }
    }

    /**
     * Get the average sleep duration for a user over a specific period.
     */
    suspend fun getAverageSleepDuration(userId: String, days: Int): Double {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val startDate = today.minusDays(days.toLong())

            val entries = getSleepEntriesInRange(
                userId,
                startDate.format(formatter),
                today.format(formatter)
            )

            if (entries.isEmpty()) return 0.0

            entries.map { it.duration }.average()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating average sleep duration: ${e.message}", e)
            0.0
        }
    }
}