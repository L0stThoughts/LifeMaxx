package com.example.lifemaxx.repository

import com.example.lifemaxx.model.MedicalStudy
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling medical study operations with Firestore.
 */
class MedicalStudyRepository {
    private val db = FirebaseFirestore.getInstance()
    private val studyCollection = db.collection("medicalStudies")

    /**
     * Retrieves all medical studies in the 'medicalStudies' collection.
     */
    suspend fun getAllStudies(): List<MedicalStudy> {
        return try {
            val snapshot = studyCollection.get().await()
            snapshot.toObjects(MedicalStudy::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Adds a new MedicalStudy document to Firestore.
     */
    suspend fun addStudy(study: MedicalStudy): Boolean {
        return try {
            // If studyId is empty, Firestore will auto-generate an ID.
            studyCollection.add(study).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Optional: Retrieve a single MedicalStudy by its document ID if needed.
     */
    suspend fun getStudyById(studyId: String): MedicalStudy? {
        return try {
            val snapshot = studyCollection.document(studyId).get().await()
            snapshot.toObject(MedicalStudy::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
