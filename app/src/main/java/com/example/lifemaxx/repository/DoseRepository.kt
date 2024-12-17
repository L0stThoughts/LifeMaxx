package com.example.lifemaxx.repository

import com.example.lifemaxx.model.Dose
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DoseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val doseCollection = db.collection("doses")

    suspend fun addDose(dose: Dose): Boolean {
        return try {
            doseCollection.add(dose).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getDosesByDate(date: String): List<Dose> {
        return try {
            val snapshot = doseCollection.whereEqualTo("date", date).get().await()
            snapshot.toObjects(Dose::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateDose(doseId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            doseCollection.document(doseId).update(updatedData).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
