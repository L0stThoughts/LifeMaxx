package com.example.lifemaxx.repository

import android.content.Context
import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.util.FirebaseFailsafeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A robust repository that handles supplements data with multiple fallback mechanisms.
 */
class RobustSupplementRepository(private val context: Context) {
    private val TAG = "RobustSupplementRepo"

    /**
     * Get all supplements with fallback mechanisms.
     */
    suspend fun getSupplements(): List<Supplement> {
        return try {
            FirebaseFailsafeUtil.getSupplementsWithFallback(context)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in getSupplements: ${e.message}", e)
            // Last resort fallback
            listOf(
                Supplement(
                    id = "emergency_1",
                    name = "Emergency Fallback Supplement",
                    dailyDose = 1,
                    measureUnit = "pill",
                    remainingQuantity = 30
                )
            )
        }
    }

    /**
     * Add a new supplement with fallback mechanisms.
     */
    suspend fun addSupplement(supplement: Supplement): Boolean {
        return try {
            FirebaseFailsafeUtil.addSupplementWithFallback(context, supplement)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in addSupplement: ${e.message}", e)
            false
        }
    }

    /**
     * Update a supplement with fallback mechanisms.
     */
    suspend fun updateSupplement(supplementId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            if (supplementId.isBlank()) {
                Log.e(TAG, "Cannot update supplement with blank ID")
                return false
            }

            FirebaseFailsafeUtil.updateSupplementWithFallback(context, supplementId, updatedData)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in updateSupplement: ${e.message}", e)
            false
        }
    }

    /**
     * Delete a supplement with fallback mechanisms.
     */
    suspend fun deleteSupplement(supplementId: String): Boolean {
        return try {
            if (supplementId.isBlank()) {
                Log.e(TAG, "Cannot delete supplement with blank ID")
                return false
            }

            FirebaseFailsafeUtil.deleteSupplementWithFallback(context, supplementId)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in deleteSupplement: ${e.message}", e)
            false
        }
    }

    /**
     * Mark all supplements taken/untaken.
     */
    suspend fun updateAllSupplementsTaken(taken: Boolean): Int {
        return withContext(Dispatchers.IO) {
            try {
                val supplements = getSupplements()
                var successCount = 0

                for (supplement in supplements) {
                    val success = updateSupplement(supplement.id, mapOf("isTaken" to taken))
                    if (success) successCount++
                }

                return@withContext successCount
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in updateAllSupplementsTaken: ${e.message}", e)
                return@withContext 0
            }
        }
    }
}