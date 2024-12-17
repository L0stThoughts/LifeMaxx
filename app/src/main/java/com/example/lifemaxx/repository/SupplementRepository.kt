package com.example.lifemaxx.repository

import com.example.lifemaxx.model.Supplement
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SupplementRepository {
    private val db = FirebaseFirestore.getInstance()
    private val supplementCollection = db.collection("supplements")

    suspend fun addSupplement(supplement: Supplement): Boolean {
        return try {
            supplementCollection.add(supplement).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSupplements(): List<Supplement> {
        return try {
            val snapshot = supplementCollection.get().await()
            snapshot.toObjects(Supplement::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            supplementCollection.document(supplementId).update(updatedData).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSupplement(supplementId: String): Boolean {
        return try {
            supplementCollection.document(supplementId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
