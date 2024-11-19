package com.example.lifemaxx.repository

import com.example.lifemaxx.models.Supplement
import com.google.firebase.firestore.FirebaseFirestore

class SupplementRepository {
    private val db = FirebaseFirestore.getInstance()

    fun addSupplement(userId: String, supplement: Supplement) {
        db.collection("users").document(userId)
            .collection("supplements")
            .document(supplement.id)
            .set(supplement)
    }

    fun getSupplements(userId: String, callback: (List<Supplement>) -> Unit) {
        db.collection("users").document(userId)
            .collection("supplements")
            .get()
            .addOnSuccessListener { result ->
                val supplements = result.toObjects(Supplement::class.java)
                callback(supplements)
            }
    }
}
