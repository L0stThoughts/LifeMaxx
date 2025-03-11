package com.example.lifemaxx.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifemaxx.model.Supplement
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A utility class that provides failsafe access to Firebase Firestore.
 * If Firestore operations fail, it falls back to local storage.
 */
object FirebaseFailsafeUtil {
    private const val TAG = "FirebaseSafeUtil"
    private const val PREFS_NAME = "LifeMaxxLocalStore"
    private const val KEY_SUPPLEMENTS = "local_supplements"

    private val isFirebaseInitialized = AtomicBoolean(false)
    private var firestoreInstance: FirebaseFirestore? = null
    private val gson = Gson()

    /**
     * Initialize Firebase with error handling
     */
    fun initializeFirebase(context: Context): Boolean {
        if (isFirebaseInitialized.get()) {
            Log.d(TAG, "Firebase already initialized")
            return true
        }

        return try {
            // Attempt to initialize Firebase
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            // Get Firestore instance and configure it
            firestoreInstance = FirebaseFirestore.getInstance().also { db ->
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            }

            isFirebaseInitialized.set(true)
            Log.d(TAG, "Firebase successfully initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            false
        }
    }

    /**
     * Safely get the Firestore instance, or null if it's not available
     */
    fun getFirestore(): FirebaseFirestore? {
        return if (isFirebaseInitialized.get()) {
            firestoreInstance
        } else {
            Log.w(TAG, "Attempted to access Firestore before initialization")
            null
        }
    }

    /**
     * Get supplements with fallback to local storage
     */
    suspend fun getSupplementsWithFallback(context: Context): List<Supplement> {
        return withContext(Dispatchers.IO) {
            try {
                // Try Firestore first
                val firestore = getFirestore()
                if (firestore != null) {
                    try {
                        val snapshot = firestore.collection("supplements")
                            .get()
                            .await(timeout = 5000)

                        val supplements = snapshot.toObjects(Supplement::class.java)

                        // Save to local storage as backup
                        if (supplements.isNotEmpty()) {
                            saveLocalSupplements(context, supplements)
                        }

                        Log.d(TAG, "Successfully fetched ${supplements.size} supplements from Firestore")
                        return@withContext supplements
                    } catch (e: Exception) {
                        Log.e(TAG, "Firestore fetch failed, falling back to local: ${e.message}", e)
                        // Fall through to local storage
                    }
                }

                // Fall back to local storage
                val localSupplements = getLocalSupplements(context)
                if (localSupplements.isNotEmpty()) {
                    Log.d(TAG, "Using ${localSupplements.size} local supplements")
                    return@withContext localSupplements
                }

                // Last resort: return demo data
                Log.d(TAG, "Using demo supplements data")
                return@withContext getDemoSupplements()
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in getSupplementsWithFallback: ${e.message}", e)
                return@withContext getDemoSupplements()
            }
        }
    }

    /**
     * Add a supplement with fallback
     */
    suspend fun addSupplementWithFallback(context: Context, supplement: Supplement): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try Firestore first
                val firestore = getFirestore()
                if (firestore != null) {
                    try {
                        // Create the doc in Firestore
                        val docRef = firestore.collection("supplements").add(supplement).await(timeout = 5000)
                        val newId = docRef.id

                        // Update with the doc ID
                        val updatedSupplement = supplement.copy(id = newId)
                        docRef.set(updatedSupplement).await(timeout = 5000)

                        // Update local storage too
                        val localSupplements = getLocalSupplements(context).toMutableList()
                        localSupplements.add(updatedSupplement)
                        saveLocalSupplements(context, localSupplements)

                        Log.d(TAG, "Successfully added supplement to Firestore: $newId")
                        return@withContext true
                    } catch (e: Exception) {
                        Log.e(TAG, "Firestore add failed, falling back to local: ${e.message}", e)
                        // Fall through to local-only storage
                    }
                }

                // Add to local storage only
                val localId = "local_${System.currentTimeMillis()}"
                val localSupplement = supplement.copy(id = localId)
                val localSupplements = getLocalSupplements(context).toMutableList()
                localSupplements.add(localSupplement)
                saveLocalSupplements(context, localSupplements)

                Log.d(TAG, "Added supplement to local storage only: $localId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in addSupplementWithFallback: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Update a supplement with fallback
     */
    suspend fun updateSupplementWithFallback(context: Context, supplementId: String, updates: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (supplementId.isBlank()) {
                    Log.e(TAG, "Cannot update supplement with blank ID")
                    return@withContext false
                }

                // Try Firestore first
                val firestore = getFirestore()
                if (firestore != null && !supplementId.startsWith("local_")) {
                    try {
                        firestore.collection("supplements")
                            .document(supplementId)
                            .update(updates)
                            .await(timeout = 5000)

                        // Also update local copy
                        updateLocalSupplement(context, supplementId, updates)

                        Log.d(TAG, "Successfully updated supplement in Firestore: $supplementId")
                        return@withContext true
                    } catch (e: Exception) {
                        Log.e(TAG, "Firestore update failed, falling back to local: ${e.message}", e)
                        // Fall through to local-only update
                    }
                }

                // Update in local storage only
                val success = updateLocalSupplement(context, supplementId, updates)
                Log.d(TAG, "Updated supplement in local storage only: $supplementId, success=$success")
                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in updateSupplementWithFallback: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Delete a supplement with fallback
     */
    suspend fun deleteSupplementWithFallback(context: Context, supplementId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (supplementId.isBlank()) {
                    Log.e(TAG, "Cannot delete supplement with blank ID")
                    return@withContext false
                }

                // Try Firestore first
                val firestore = getFirestore()
                if (firestore != null && !supplementId.startsWith("local_")) {
                    try {
                        firestore.collection("supplements")
                            .document(supplementId)
                            .delete()
                            .await(timeout = 5000)

                        // Also delete from local storage
                        deleteLocalSupplement(context, supplementId)

                        Log.d(TAG, "Successfully deleted supplement from Firestore: $supplementId")
                        return@withContext true
                    } catch (e: Exception) {
                        Log.e(TAG, "Firestore delete failed, falling back to local: ${e.message}", e)
                        // Fall through to local-only delete
                    }
                }

                // Delete from local storage only
                val success = deleteLocalSupplement(context, supplementId)
                Log.d(TAG, "Deleted supplement from local storage only: $supplementId, success=$success")
                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in deleteSupplementWithFallback: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Local storage functions
     */
    private fun getLocalSupplements(context: Context): List<Supplement> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SUPPLEMENTS, null) ?: return emptyList()

        val type = object : TypeToken<List<Supplement>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local supplements: ${e.message}", e)
            emptyList()
        }
    }

    private fun saveLocalSupplements(context: Context, supplements: List<Supplement>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(supplements)
        prefs.edit().putString(KEY_SUPPLEMENTS, json).apply()
    }

    private fun updateLocalSupplement(context: Context, supplementId: String, updates: Map<String, Any>): Boolean {
        val supplements = getLocalSupplements(context).toMutableList()
        val index = supplements.indexOfFirst { it.id == supplementId }

        if (index == -1) return false

        // Get the current supplement
        val current = supplements[index]

        // Create an updated copy with all changes applied
        var updated = current

        // Apply each update
        for ((key, value) in updates) {
            updated = when (key) {
                "name" -> updated.copy(name = value as String)
                "dailyDose" -> updated.copy(dailyDose = (value as Number).toInt())
                "measureUnit" -> updated.copy(measureUnit = value as String)
                "remainingQuantity" -> updated.copy(remainingQuantity = (value as Number).toInt())
                "isTaken" -> updated.copy(isTaken = value as Boolean)
                else -> updated
            }
        }

        // Replace the old supplement with the updated one
        supplements[index] = updated
        saveLocalSupplements(context, supplements)
        return true
    }

    private fun deleteLocalSupplement(context: Context, supplementId: String): Boolean {
        val supplements = getLocalSupplements(context).toMutableList()
        val initialSize = supplements.size
        supplements.removeIf { it.id == supplementId }

        if (supplements.size == initialSize) {
            return false // Nothing was removed
        }

        saveLocalSupplements(context, supplements)
        return true
    }

    /**
     * Demo data for worst-case scenario
     */
    private fun getDemoSupplements(): List<Supplement> {
        return listOf(
            Supplement(
                id = "demo_1",
                name = "Vitamin D (Demo)",
                dailyDose = 1,
                measureUnit = "pill",
                remainingQuantity = 30
            ),
            Supplement(
                id = "demo_2",
                name = "Magnesium (Demo)",
                dailyDose = 2,
                measureUnit = "pill",
                remainingQuantity = 60
            ),
            Supplement(
                id = "demo_3",
                name = "Fish Oil (Demo)",
                dailyDose = 1,
                measureUnit = "pill",
                remainingQuantity = 45
            )
        )
    }

    /**
     * Helper function for awaiting Tasks with timeout
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(timeout: Long): T {
        var result: T? = null
        var exception: Exception? = null
        var isComplete = false

        this.addOnSuccessListener {
            result = it
            isComplete = true
        }.addOnFailureListener {
            exception = it as Exception
            isComplete = true
        }

        // Wait for completion or timeout
        val startTime = System.currentTimeMillis()
        while (!isComplete) {
            if (System.currentTimeMillis() - startTime > timeout) {
                throw Exception("Operation timed out after $timeout ms")
            }
            kotlinx.coroutines.delay(50)
        }

        if (exception != null) {
            throw exception!!
        }

        return result!!
    }
}