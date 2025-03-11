package com.example.lifemaxx.repository

import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.model.SupplementBarcode
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Repository for handling barcode-related operations with Firestore.
 * Includes looking up supplements by barcode and storing barcode information.
 */
class BarcodeRepository(
    private val supplementRepository: SupplementRepository
) {
    private val TAG = "BarcodeRepository"
    private val TIMEOUT_MS = 10000L // 10 seconds timeout

    private val db = FirebaseFirestore.getInstance()
    private val barcodeCollection = db.collection("supplementBarcodes")

    /**
     * Look up a supplement by its barcode.
     * First checks the Firestore database, then falls back to a placeholder if not found.
     */
    suspend fun lookupBarcode(barcode: String): SupplementBarcode {
        return try {
            withTimeout(TIMEOUT_MS) {
                // First, check if the barcode exists in our database
                val snapshot = barcodeCollection
                    .whereEqualTo("barcode", barcode)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    // Barcode found in database
                    val barcodeData = snapshot.documents.first().toObject(SupplementBarcode::class.java)
                    Log.d(TAG, "Barcode found in database: $barcode")

                    // Check if this supplement already exists in the user's list
                    val existingSupplements = supplementRepository.getSupplements()
                    val exists = existingSupplements.any { it.name == barcodeData?.name }

                    barcodeData?.copy(exists = exists) ?: SupplementBarcode.createPlaceholder(barcode)
                } else {
                    // If not found, we could theoretically query an external API here
                    // For now, just create a placeholder
                    Log.d(TAG, "Barcode not found, creating placeholder: $barcode")
                    SupplementBarcode.createPlaceholder(barcode)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up barcode: ${e.message}", e)
            SupplementBarcode.createPlaceholder(barcode)
        }
    }

    /**
     * Save barcode information to Firestore for future lookups.
     * This helps build a community database of supplement barcodes.
     */
    suspend fun saveBarcodeInfo(barcodeInfo: SupplementBarcode): Boolean {
        return try {
            withTimeout(TIMEOUT_MS) {
                // First check if this barcode already exists
                val snapshot = barcodeCollection
                    .whereEqualTo("barcode", barcodeInfo.barcode)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    // Add new barcode entry
                    barcodeCollection.add(barcodeInfo).await()
                    Log.d(TAG, "Saved new barcode info: ${barcodeInfo.barcode}")
                } else {
                    // Update existing barcode entry
                    val docId = snapshot.documents.first().id
                    barcodeCollection.document(docId).set(barcodeInfo).await()
                    Log.d(TAG, "Updated existing barcode info: ${barcodeInfo.barcode}")
                }

                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving barcode info: ${e.message}", e)
            false
        }
    }

    /**
     * Get all barcodes in the database - useful for admin functions
     */
    suspend fun getAllBarcodes(): List<SupplementBarcode> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val snapshot = barcodeCollection.get().await()
                snapshot.toObjects(SupplementBarcode::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all barcodes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Delete a barcode from the database
     */
    suspend fun deleteBarcode(barcode: String): Boolean {
        return try {
            withTimeout(TIMEOUT_MS) {
                val snapshot = barcodeCollection
                    .whereEqualTo("barcode", barcode)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents.first().id
                    barcodeCollection.document(docId).delete().await()
                    Log.d(TAG, "Deleted barcode: $barcode")
                    true
                } else {
                    Log.d(TAG, "Barcode not found for deletion: $barcode")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting barcode: ${e.message}", e)
            false
        }
    }
}