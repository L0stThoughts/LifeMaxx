package com.example.lifemaxx.util

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.lifemaxx.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A comprehensive utility class that provides failsafe access to Firebase Firestore.
 * If Firestore operations fail, it falls back to local storage to ensure the app
 * works in offline mode.
 */
object FirebaseFailsafeUtil {
    private const val TAG = "FirebaseFailsafeUtil"
    private const val PREFS_NAME = "LifeMaxxLocalStore"
    private const val KEY_SUPPLEMENTS = "local_supplements"
    private const val KEY_NUTRITION = "local_nutrition"
    private const val KEY_WATER_INTAKE = "local_water_intake"
    private const val KEY_DOSES = "local_doses"
    private const val KEY_REMINDERS = "local_reminders"
    private const val KEY_SLEEP = "local_sleep"
    private const val KEY_LAST_SYNC = "last_sync_time"
    private const val KEY_OFFLINE_MODE = "offline_mode"

    private val isFirebaseInitialized = AtomicBoolean(false)
    private var firestoreInstance: FirebaseFirestore? = null
    private val gson = Gson()

    // Queue to track offline operations that need to be synced
    private data class PendingOperation(
        val operationType: String, // "add", "update", "delete"
        val collectionName: String,
        val documentId: String?,
        val data: Map<String, Any>?,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val pendingOperations = mutableListOf<PendingOperation>()

    /**
     * Initialize Firebase with error handling (non-suspend version for use in Application class)
     */
    fun initializeFirebaseSync(context: Context): Boolean {
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
                    .setPersistenceEnabled(true)  // Enable offline cache
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
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
     * Initialize Firebase with error handling (suspend version for use in coroutines)
     */
    suspend fun initializeFirebase(context: Context): Boolean {
        if (isFirebaseInitialized.get()) {
            Log.d(TAG, "Firebase already initialized")
            return true
        }

        val result = initializeFirebaseSync(context)

        // If initialization was successful, attempt to sync any pending offline operations
        if (result) {
            try {
                syncPendingOperations(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing pending operations: ${e.message}", e)
                // Continue even if sync fails - we're still initialized
            }
        }

        return result
    }

    /**
     * Check if the device has network connectivity
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
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
     * Check if app is in offline mode
     */
    fun isOfflineMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }

    /**
     * Set offline mode status
     */
    fun setOfflineMode(context: Context, offline: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_OFFLINE_MODE, offline).apply()
        Log.d(TAG, "Set offline mode to: $offline")
    }

    /**
     * Add a pending operation to the queue for later sync
     */
    private fun addPendingOperation(
        context: Context,
        operationType: String,
        collectionName: String,
        documentId: String? = null,
        data: Map<String, Any>? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingOps = loadPendingOperations(prefs)

        val newOperation = PendingOperation(
            operationType = operationType,
            collectionName = collectionName,
            documentId = documentId,
            data = data
        )

        pendingOps.add(newOperation)
        savePendingOperations(prefs, pendingOps)

        Log.d(TAG, "Added pending $operationType operation for $collectionName")
    }

    /**
     * Load pending operations from SharedPreferences
     */
    private fun loadPendingOperations(prefs: SharedPreferences): MutableList<PendingOperation> {
        val json = prefs.getString("pending_operations", null) ?: return mutableListOf()

        val type = object : TypeToken<List<PendingOperation>>() {}.type
        return try {
            gson.fromJson<MutableList<PendingOperation>>(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pending operations: ${e.message}", e)
            mutableListOf()
        }
    }

    /**
     * Save pending operations to SharedPreferences
     */
    private fun savePendingOperations(prefs: SharedPreferences, operations: List<PendingOperation>) {
        val json = gson.toJson(operations)
        prefs.edit().putString("pending_operations", json).apply()
    }

    /**
     * Attempt to sync pending operations with Firestore
     * Returns number of operations successfully synced
     */
    suspend fun syncPendingOperations(context: Context): Int {
        if (!isNetworkAvailable(context) || !isFirebaseInitialized.get()) {
            Log.d(TAG, "Cannot sync: Network unavailable or Firebase not initialized")
            return 0
        }

        val firestore = getFirestore() ?: return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingOps = loadPendingOperations(prefs)

        if (pendingOps.isEmpty()) {
            Log.d(TAG, "No pending operations to sync")
            return 0
        }

        Log.d(TAG, "Starting sync of ${pendingOps.size} pending operations")

        val completedOps = mutableListOf<PendingOperation>()

        for (operation in pendingOps) {
            try {
                val collection = firestore.collection(operation.collectionName)

                when (operation.operationType) {
                    "add" -> {
                        if (operation.data != null) {
                            val docRef = if (operation.documentId != null) {
                                collection.document(operation.documentId)
                            } else {
                                collection.document()
                            }

                            docRef.set(operation.data).await(timeout = 5000)
                            completedOps.add(operation)
                        }
                    }
                    "update" -> {
                        if (operation.documentId != null && operation.data != null) {
                            collection.document(operation.documentId)
                                .update(operation.data)
                                .await(timeout = 5000)
                            completedOps.add(operation)
                        }
                    }
                    "delete" -> {
                        if (operation.documentId != null) {
                            collection.document(operation.documentId)
                                .delete()
                                .await(timeout = 5000)
                            completedOps.add(operation)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing operation: ${e.message}", e)
                // Leave this operation in the pending list
            }
        }

        // Remove completed operations from the pending list
        pendingOps.removeAll(completedOps)
        savePendingOperations(prefs, pendingOps)

        // Update last sync time
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()

        Log.d(TAG, "Sync completed. ${completedOps.size} operations synced, ${pendingOps.size} pending")
        return completedOps.size
    }

    /**
     * Get supplements with fallback to local storage
     */
    suspend fun getSupplementsWithFallback(context: Context): List<Supplement> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to sync pending operations if possible
                syncPendingOperations(context)

                // Try Firestore first
                if (isNetworkAvailable(context)) {
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
                // Generate a local ID if needed
                val supplementToAdd = if (supplement.id.isBlank()) {
                    supplement.copy(id = "local_${System.currentTimeMillis()}")
                } else {
                    supplement
                }

                // Try Firestore first if we have connectivity
                if (isNetworkAvailable(context)) {
                    val firestore = getFirestore()
                    if (firestore != null) {
                        try {
                            // Create the doc in Firestore
                            val docRef = firestore.collection("supplements").add(supplementToAdd).await(timeout = 5000)
                            val newId = docRef.id

                            // Update with the doc ID
                            val updatedSupplement = supplementToAdd.copy(id = newId)
                            docRef.set(updatedSupplement).await(timeout = 5000)

                            // Update local storage too
                            val localSupplements = getLocalSupplements(context).toMutableList()
                            localSupplements.add(updatedSupplement)
                            saveLocalSupplements(context, localSupplements)

                            Log.d(TAG, "Successfully added supplement to Firestore: $newId")
                            return@withContext true
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore add failed, falling back to local: ${e.message}", e)
                            // Add pending operation for future sync
                            val data = convertSupplementToMap(supplementToAdd)
                            addPendingOperation(context, "add", "supplements", null, data)
                            // Fall through to local-only storage
                        }
                    }
                } else {
                    // Add pending operation for future sync
                    val data = convertSupplementToMap(supplementToAdd)
                    addPendingOperation(context, "add", "supplements", null, data)
                }

                // Add to local storage only
                val localSupplements = getLocalSupplements(context).toMutableList()
                localSupplements.add(supplementToAdd)
                saveLocalSupplements(context, localSupplements)

                Log.d(TAG, "Added supplement to local storage: ${supplementToAdd.id}")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in addSupplementWithFallback: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Convert a Supplement to a Map for Firestore
     */
    private fun convertSupplementToMap(supplement: Supplement): Map<String, Any> {
        return mapOf(
            "id" to supplement.id,
            "name" to supplement.name,
            "dailyDose" to supplement.dailyDose,
            "measureUnit" to supplement.measureUnit,
            "remainingQuantity" to supplement.remainingQuantity,
            "isTaken" to supplement.isTaken
        )
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

                // First update the local copy for immediate feedback
                val success = updateLocalSupplement(context, supplementId, updates)

                // Try Firestore if online and not a local-only document
                if (isNetworkAvailable(context) && !supplementId.startsWith("local_")) {
                    val firestore = getFirestore()
                    if (firestore != null) {
                        try {
                            firestore.collection("supplements")
                                .document(supplementId)
                                .update(updates)
                                .await(timeout = 5000)

                            Log.d(TAG, "Successfully updated supplement in Firestore: $supplementId")
                            return@withContext true
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore update failed, using local only: ${e.message}", e)
                            // Add pending operation for future sync
                            addPendingOperation(context, "update", "supplements", supplementId, updates)
                        }
                    }
                } else if (!supplementId.startsWith("local_")) {
                    // Add pending operation for future sync
                    addPendingOperation(context, "update", "supplements", supplementId, updates)
                }

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

                // First delete from local storage for immediate feedback
                val success = deleteLocalSupplement(context, supplementId)

                // Try Firestore if online and not a local-only document
                if (isNetworkAvailable(context) && !supplementId.startsWith("local_")) {
                    val firestore = getFirestore()
                    if (firestore != null) {
                        try {
                            firestore.collection("supplements")
                                .document(supplementId)
                                .delete()
                                .await(timeout = 5000)

                            Log.d(TAG, "Successfully deleted supplement from Firestore: $supplementId")
                            return@withContext true
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore delete failed, using local only: ${e.message}", e)
                            // Add pending operation for future sync
                            addPendingOperation(context, "delete", "supplements", supplementId)
                        }
                    }
                } else if (!supplementId.startsWith("local_")) {
                    // Add pending operation for future sync
                    addPendingOperation(context, "delete", "supplements", supplementId)
                }

                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in deleteSupplementWithFallback: ${e.message}", e)
                return@withContext false
            }
        }
    }

    //------------ Nutrition Entry Methods ------------//

    /**
     * Get nutrition entries with fallback
     */
    suspend fun getNutritionEntriesWithFallback(context: Context, userId: String, date: String): List<NutritionEntry> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to sync pending operations if possible
                syncPendingOperations(context)

                // Try Firestore first if we have connectivity
                if (isNetworkAvailable(context)) {
                    val firestore = getFirestore()
                    if (firestore != null) {
                        try {
                            val snapshot = firestore.collection("nutritionEntries")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("date", date)
                                .get()
                                .await(timeout = 5000)

                            val entries = snapshot.toObjects(NutritionEntry::class.java)

                            // Save to local storage as backup
                            saveLocalNutritionEntries(context, entries)

                            Log.d(TAG, "Successfully fetched ${entries.size} nutrition entries from Firestore")
                            return@withContext entries
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore fetch failed, falling back to local: ${e.message}", e)
                            // Fall through to local storage
                        }
                    }
                }

                // Fall back to local storage
                val allEntries = getLocalNutritionEntries(context)
                val filteredEntries = allEntries.filter {
                    it.userId == userId && it.date == date
                }

                Log.d(TAG, "Using ${filteredEntries.size} local nutrition entries")
                return@withContext filteredEntries
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in getNutritionEntriesWithFallback: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    /**
     * Add a nutrition entry with fallback
     */
    suspend fun addNutritionEntryWithFallback(context: Context, entry: NutritionEntry): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Generate a local ID if needed
                val entryToAdd = if (entry.id.isBlank()) {
                    entry.copy(id = "local_${System.currentTimeMillis()}")
                } else {
                    entry
                }

                // Add to local storage first
                val localEntries = getLocalNutritionEntries(context).toMutableList()
                localEntries.add(entryToAdd)
                saveLocalNutritionEntries(context, localEntries)

                // Try Firestore if we have connectivity
                if (isNetworkAvailable(context)) {
                    val firestore = getFirestore()
                    if (firestore != null) {
                        try {
                            val docRef = firestore.collection("nutritionEntries").add(entryToAdd)
                                .await(timeout = 5000)
                            val newId = docRef.id

                            // Update with the server-assigned ID
                            val updatedEntry = entryToAdd.copy(id = newId)
                            docRef.update("id", newId).await(timeout = 5000)

                            // Update local storage with the server ID
                            updateLocalNutritionEntry(context, entryToAdd.id, updatedEntry)

                            Log.d(TAG, "Successfully added nutrition entry to Firestore: $newId")
                            return@withContext true
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore add failed, using local only: ${e.message}", e)
                            // Add pending operation for future sync
                            val data = convertNutritionEntryToMap(entryToAdd)
                            addPendingOperation(context, "add", "nutritionEntries", null, data)
                        }
                    }
                } else {
                    // Add pending operation for future sync
                    val data = convertNutritionEntryToMap(entryToAdd)
                    addPendingOperation(context, "add", "nutritionEntries", null, data)
                }

                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in addNutritionEntryWithFallback: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Update nutrition entry field
     */
    suspend fun updateNutritionEntry(context: Context, entryId: String, updatedData: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Update local entry
                val localEntries = getLocalNutritionEntries(context).toMutableList()
                val index = localEntries.indexOfFirst { it.id == entryId }

                if (index != -1) {
                    var updated = localEntries[index]

                    // Apply updates
                    for ((key, value) in updatedData) {
                        updated = when (key) {
                            "foodName" -> updated.copy(foodName = value as String)
                            "calories" -> updated.copy(calories = (value as Number).toInt())
                            "proteins" -> updated.copy(proteins = (value as Number).toDouble())
                            "carbs" -> updated.copy(carbs = (value as Number).toDouble())
                            "fats" -> updated.copy(fats = (value as Number).toDouble())
                            "servingSize" -> updated.copy(servingSize = (value as Number).toDouble())
                            "mealType" -> updated.copy(mealType = value as String)
                            else -> updated // Ignore unknown fields
                        }
                    }

                    localEntries[index] = updated
                    saveLocalNutritionEntries(context, localEntries)
                    return@withContext true
                }

                return@withContext false
            } catch (e: Exception) {
                Log.e(TAG, "Error updating nutrition entry: ${e.message}", e)
                return@withContext false
            }
        }
    }

    private fun convertNutritionEntryToMap(entry: NutritionEntry): Map<String, Any> {
        return mapOf(
            "id" to entry.id,
            "userId" to entry.userId,
            "date" to entry.date,
            "foodName" to entry.foodName,
            "calories" to entry.calories,
            "proteins" to entry.proteins,
            "carbs" to entry.carbs,
            "fats" to entry.fats,
            "servingSize" to entry.servingSize,
            "mealType" to entry.mealType,
            "timestamp" to entry.timestamp
        )
    }

    //------------ Water Intake Methods ------------//

    /**
     * Get water intakes with fallback
     */
    suspend fun getWaterIntakesWithFallback(context: Context, userId: String, date: String): List<WaterIntake> {
        return withContext(Dispatchers.IO) {
            try {
                // Try to sync pending operations if possible
                syncPendingOperations(context)

                // Try Firestore first if we have connectivity
                if (isNetworkAvailable(context)) {
                    val firestore = getFirestore()
                    if (firestore != null) {
                        try {
                            val snapshot = firestore.collection("waterIntakes")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("date", date)
                                .get()
                                .await(timeout = 5000)

                            val intakes = snapshot.toObjects(WaterIntake::class.java)

                            // Save to local storage as backup
                            saveLocalWaterIntakes(context, intakes)

                            Log.d(TAG, "Successfully fetched ${intakes.size} water intakes from Firestore")
                            return@withContext intakes
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore fetch failed, falling back to local: ${e.message}", e)
                            // Fall through to local storage
                        }
                    }
                }

                // Fall back to local storage
                val allIntakes = getLocalWaterIntakes(context)
                val filteredIntakes = allIntakes.filter {
                    it.userId == userId && it.date == date
                }

                Log.d(TAG, "Using ${filteredIntakes.size} local water intakes")
                return@withContext filteredIntakes
            } catch (e: Exception) {
                Log.e(TAG, "Complete failure in getWaterIntakesWithFallback: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    private fun convertWaterIntakeToMap(intake: WaterIntake): Map<String, Any> {
        return mapOf(
            "id" to intake.id,
            "userId" to intake.userId,
            "date" to intake.date,
            "amount" to intake.amount,
            "time" to intake.time,
            "containerType" to intake.containerType,
            "timestamp" to intake.timestamp
        )
    }

    //------------ Local Storage Methods ------------//

    /**
     * Get supplements from local storage
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

    /**
     * Save supplements to local storage
     */
    private fun saveLocalSupplements(context: Context, supplements: List<Supplement>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(supplements)
        prefs.edit().putString(KEY_SUPPLEMENTS, json).apply()
    }

    /**
     * Update a supplement in local storage
     */
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

    /**
     * Delete a supplement from local storage
     */
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
     * Get nutrition entries from local storage
     */
    private fun getLocalNutritionEntries(context: Context): List<NutritionEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NUTRITION, null) ?: return emptyList()

        val type = object : TypeToken<List<NutritionEntry>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local nutrition entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Save nutrition entries to local storage
     */
    private fun saveLocalNutritionEntries(context: Context, entries: List<NutritionEntry>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(entries)
        prefs.edit().putString(KEY_NUTRITION, json).apply()
    }

    /**
     * Update a local nutrition entry after it's been synced to Firebase
     */
    private fun updateLocalNutritionEntry(context: Context, oldId: String, newEntry: NutritionEntry) {
        val entries = getLocalNutritionEntries(context).toMutableList()
        val index = entries.indexOfFirst { it.id == oldId }

        if (index != -1) {
            entries[index] = newEntry
            saveLocalNutritionEntries(context, entries)
        }
    }

    /**
     * Get water intakes from local storage
     */
    private fun getLocalWaterIntakes(context: Context): List<WaterIntake> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_WATER_INTAKE, null) ?: return emptyList()

        val type = object : TypeToken<List<WaterIntake>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local water intakes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Save water intakes to local storage
     */
    private fun saveLocalWaterIntakes(context: Context, intakes: List<WaterIntake>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(intakes)
        prefs.edit().putString(KEY_WATER_INTAKE, json).apply()
    }

    /**
     * Get reminders from local storage
     */
    private fun getLocalReminders(context: Context): List<Reminder> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()

        val type = object : TypeToken<List<Reminder>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local reminders: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Demo data fallback for supplements
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