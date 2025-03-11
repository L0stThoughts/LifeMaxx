package com.example.lifemaxx.repository

import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for handling supplement-related operations (CRUD) with Firestore.
 */
class SupplementRepository {
    private val TAG = "SupplementRepository"

    // Use lazy initialization to ensure Firestore is properly initialized
    private val db by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}", e)
            null
        }
    }

    private val supplementCollection by lazy {
        db?.collection("supplements")
    }

    /**
     * Add a new supplement to Firestore, then store the auto-generated ID
     * back into the doc's "id" field so updates/deletes can work.
     */
    suspend fun addSupplement(supplement: Supplement): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Safety check
                val collection = supplementCollection ?: return@withContext false

                // 1) Create the doc in Firestore
                val docRef = collection.add(supplement).await()

                // 2) Retrieve the auto-generated ID
                val newId = docRef.id
                Log.d(TAG, "Generated new document ID: $newId")

                // 3) Write that ID back into the doc
                docRef.update("id", newId).await()
                Log.d(TAG, "Successfully added supplement: ${supplement.name}")
                true
            } catch (e: Exception) {
                handleFirestoreException(e, "adding supplement")
                false
            }
        }
    }

    /**
     * Fetch all supplements from Firestore, mapping them to [Supplement].
     */
    suspend fun getSupplements(): List<Supplement> {
        return withContext(Dispatchers.IO) {
            try {
                // Safety check
                val collection = supplementCollection ?: return@withContext emptyList()

                val snapshot = collection.get().await()
                val supplements = snapshot.toObjects(Supplement::class.java)
                Log.d(TAG, "Successfully fetched ${supplements.size} supplements")
                supplements
            } catch (e: Exception) {
                handleFirestoreException(e, "fetching supplements")
                emptyList()
            }
        }
    }

    /**
     * Update fields in a supplement doc, by its [supplementId].
     */
    suspend fun updateSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Safety check
                val collection = supplementCollection ?: return@withContext false

                if (supplementId.isBlank()) {
                    Log.e(TAG, "Cannot update supplement with blank ID")
                    return@withContext false
                }

                collection.document(supplementId).update(updatedData).await()
                Log.d(TAG, "Successfully updated supplement: $supplementId")
                true
            } catch (e: Exception) {
                handleFirestoreException(e, "updating supplement $supplementId")
                false
            }
        }
    }

    /**
     * Delete a supplement doc from Firestore by its [supplementId].
     */
    suspend fun deleteSupplement(supplementId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Safety check
                val collection = supplementCollection ?: return@withContext false

                if (supplementId.isBlank()) {
                    Log.e(TAG, "Cannot delete supplement with blank ID")
                    return@withContext false
                }

                Log.d(TAG, "Attempting to delete supplement with ID: $supplementId")
                collection.document(supplementId).delete().await()
                Log.d(TAG, "Successfully deleted supplement: $supplementId")
                true
            } catch (e: Exception) {
                handleFirestoreException(e, "deleting supplement $supplementId")
                false
            }
        }
    }

    /**
     * Handle Firestore exceptions with appropriate logging
     */
    private fun handleFirestoreException(e: Exception, operation: String) {
        when (e) {
            is FirebaseFirestoreException -> {
                Log.e(TAG, "Firestore error while $operation: ${e.code} - ${e.message}", e)
            }
            else -> {
                Log.e(TAG, "Error $operation: ${e.message}", e)
            }
        }
    }
}