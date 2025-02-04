package com.example.lifemaxx.repository

import com.example.lifemaxx.model.Supplement
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling supplement-related operations (CRUD) with Firestore.
 */
class SupplementRepository {
    private val db = FirebaseFirestore.getInstance()
    private val supplementCollection = db.collection("supplements")

    /**
     * Add a new supplement to Firestore, then store the auto-generated ID
     * back into the doc's "id" field so updates/deletes can work.
     */
    suspend fun addSupplement(supplement: Supplement): Boolean {
        return try {
            // 1) Create the doc in Firestore
            val docRef = supplementCollection.add(supplement).await()
            // 2) Retrieve the auto-generated ID
            val newId = docRef.id
            // 3) Write that ID back into the doc
            docRef.update("id", newId).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch all supplements from Firestore, mapping them to [Supplement].
     */
    suspend fun getSupplements(): List<Supplement> {
        return try {
            val snapshot = supplementCollection.get().await()
            snapshot.toObjects(Supplement::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update fields in a supplement doc, by its [supplementId].
     */
    suspend fun updateSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            supplementCollection.document(supplementId).update(updatedData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a supplement doc from Firestore by its [supplementId].
     */
    suspend fun deleteSupplement(supplementId: String): Boolean {
        return try {
            supplementCollection.document(supplementId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
