package com.example.lifemaxx.repository

import com.example.lifemaxx.model.Dose
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling dose-related operations (CRUD) with Firestore.
 */
class DoseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val doseCollection = db.collection("doses")

    /**
     * Adds a new Dose document to Firestore.
     */
    suspend fun addDose(dose: Dose): Boolean {
        return try {
            // If doseId is empty, Firestore will auto-generate an ID.
            // Otherwise, you can use .document(dose.doseId).set(dose)
            doseCollection.add(dose).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves all Dose documents that match the given date.
     */
    suspend fun getDosesByDate(date: String): List<Dose> {
        return try {
            val snapshot = doseCollection
                .whereEqualTo("date", date)
                .get()
                .await()
            snapshot.toObjects(Dose::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Updates fields of a Dose document by doseId.
     */
    suspend fun updateDose(doseId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            doseCollection.document(doseId).update(updatedData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves a Dose by its ID (optional function).
     */
    suspend fun getDoseById(doseId: String): Dose? {
        return try {
            val snapshot = doseCollection.document(doseId).get().await()
            snapshot.toObject(Dose::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
