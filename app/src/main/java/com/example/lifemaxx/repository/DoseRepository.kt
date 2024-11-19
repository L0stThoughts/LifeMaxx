package com.example.lifemaxx.repository

import com.example.lifemaxx.models.Dose
import com.google.firebase.firestore.FirebaseFirestore

class DoseRepository {
    private val db = FirebaseFirestore.getInstance()

    fun addDose(userId: String, dose: Dose) {
        db.collection("users").document(userId)
            .collection("doses")
            .document(dose.date)
            .set(dose)
    }

    fun getDoses(userId: String, callback: (List<Dose>) -> Unit) {
        db.collection("users").document(userId)
            .collection("doses")
            .get()
            .addOnSuccessListener { result ->
                val doses = result.toObjects(Dose::class.java)
                callback(doses)
            }
    }
}
