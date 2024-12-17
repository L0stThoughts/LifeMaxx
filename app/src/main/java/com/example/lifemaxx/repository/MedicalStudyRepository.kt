package com.example.lifemaxx.repository

import com.example.lifemaxx.model.MedicalStudy
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MedicalStudyRepository {
    private val db = FirebaseFirestore.getInstance()
    private val studyCollection = db.collection("medicalStudies")

    suspend fun getAllStudies(): List<MedicalStudy> {
        return try {
            val snapshot = studyCollection.get().await()
            snapshot.toObjects(MedicalStudy::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addStudy(study: MedicalStudy): Boolean {
        return try {
            studyCollection.add(study).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}