package com.example.lifemaxx.repository

import android.content.Context
import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.model.SupplementBarcode
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Repository for handling barcode-related operations with Firestore.
 * Includes looking up supplements by barcode and storing barcode information.
 * Simplified implementation that won't interfere with other repositories.
 */
class BarcodeRepository(
    private val context: Context
) {
    private val TAG = "BarcodeRepository"
    private val TIMEOUT_MS = 5000L // 5 seconds timeout

    // Firestore database reference - with safety
    private var db: FirebaseFirestore? = null
    private var barcodeCollection: com.google.firebase.firestore.CollectionReference? = null

    // Reference to SupplementRepository will be set after initialization
    private var supplementRepository: SupplementRepository? = null

    init {
        try {
            db = FirebaseFirestore.getInstance()
            barcodeCollection = db?.collection("supplementBarcodes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore: ${e.message}", e)
        }
    }

    /**
     * Set the SupplementRepository after initialization to avoid circular dependency
     */
    fun setSupplementRepository(repository: SupplementRepository) {
        this.supplementRepository = repository
    }

    /**
     * Look up a supplement by its barcode.
     * First checks the Firestore database, then falls back to a placeholder if not found.
     */
    suspend fun lookupBarcode(barcode: String): SupplementBarcode {
        return withContext(Dispatchers.IO) {
            try {
                // Check if barcode collection exists
                if (barcodeCollection == null) {
                    Log.d(TAG, "Barcode collection not initialized, returning placeholder")
                    return@withContext SupplementBarcode.createPlaceholder(barcode)
                }

                // Try to find barcode in database with timeout
                val snapshot = withTimeoutOrNull(TIMEOUT_MS) {
                    try {
                        barcodeCollection?.whereEqualTo("barcode", barcode)?.get()?.await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error querying Firestore: ${e.message}", e)
                        null
                    }
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Barcode found in database
                    val barcodeData = snapshot.documents.first().toObject(SupplementBarcode::class.java)
                    Log.d(TAG, "Barcode found in database: $barcode")

                    // Check if this supplement already exists in the user's list
                    val exists = supplementRepository?.let { repo ->
                        try {
                            val existingSupplements = repo.getSupplements()
                            existingSupplements.any { it.name == barcodeData?.name }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking existing supplements: ${e.message}", e)
                            false
                        }
                    } ?: false

                    barcodeData?.copy(exists = exists) ?: SupplementBarcode.createPlaceholder(barcode)
                } else {
                    // If not found, create a placeholder
                    Log.d(TAG, "Barcode not found, creating placeholder: $barcode")
                    SupplementBarcode.createPlaceholder(barcode)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error looking up barcode: ${e.message}", e)
                SupplementBarcode.createPlaceholder(barcode)
            }
        }
    }

    /**
     * Save barcode information to Firestore for future lookups.
     * This helps build a community database of supplement barcodes.
     */
    suspend fun saveBarcodeInfo(barcodeInfo: SupplementBarcode): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if barcode collection exists
                if (barcodeCollection == null) {
                    Log.e(TAG, "Barcode collection not initialized")
                    return@withContext false
                }

                // Try with timeout
                withTimeoutOrNull(TIMEOUT_MS) {
                    try {
                        // First check if this barcode already exists
                        val snapshot = barcodeCollection?.whereEqualTo("barcode", barcodeInfo.barcode)?.get()?.await()

                        if (snapshot != null && snapshot.isEmpty) {
                            // Add new barcode entry
                            barcodeCollection?.add(barcodeInfo)?.await()
                            Log.d(TAG, "Saved new barcode info: ${barcodeInfo.barcode}")
                        } else if (snapshot != null) {
                            // Update existing barcode entry
                            val docId = snapshot.documents.first().id
                            barcodeCollection?.document(docId)?.set(barcodeInfo)?.await()
                            Log.d(TAG, "Updated existing barcode info: ${barcodeInfo.barcode}")
                        }
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving barcode: ${e.message}", e)
                        false
                    }
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error saving barcode info: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Get all barcodes in the database - useful for admin functions
     */
    suspend fun getAllBarcodes(): List<SupplementBarcode> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if barcode collection exists
                if (barcodeCollection == null) {
                    Log.e(TAG, "Barcode collection not initialized")
                    return@withContext emptyList()
                }

                // Try with timeout
                withTimeoutOrNull(TIMEOUT_MS) {
                    try {
                        val snapshot = barcodeCollection?.get()?.await()
                        snapshot?.toObjects(SupplementBarcode::class.java) ?: emptyList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting barcodes: ${e.message}", e)
                        emptyList<SupplementBarcode>()
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all barcodes: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Delete a barcode from the database
     */
    suspend fun deleteBarcode(barcode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if barcode collection exists
                if (barcodeCollection == null) {
                    Log.e(TAG, "Barcode collection not initialized")
                    return@withContext false
                }

                // Try with timeout
                withTimeoutOrNull(TIMEOUT_MS) {
                    try {
                        val snapshot = barcodeCollection?.whereEqualTo("barcode", barcode)?.get()?.await()

                        if (snapshot != null && !snapshot.isEmpty) {
                            val docId = snapshot.documents.first().id
                            barcodeCollection?.document(docId)?.delete()?.await()
                            Log.d(TAG, "Deleted barcode: $barcode")
                            true
                        } else {
                            Log.d(TAG, "Barcode not found for deletion: $barcode")
                            false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting barcode: ${e.message}", e)
                        false
                    }
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting barcode: ${e.message}", e)
                false
            }
        }
    }
}