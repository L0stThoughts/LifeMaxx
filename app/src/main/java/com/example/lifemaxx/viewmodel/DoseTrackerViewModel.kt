package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.repository.SupplementRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling the supplement list, marking them taken/untaken,
 * and deleting, etc. With EXTREME error handling and crash protection.
 */
class DoseTrackerViewModel(
    private val repository: SupplementRepository
) : ViewModel() {
    private val TAG = "DoseTrackerViewModel"

    private val _supplements = MutableStateFlow<List<Supplement>>(getDemoSupplements())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    // Global exception handler to prevent crashes
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Unhandled exception: ${exception.message}", exception)
        _error.value = "An unexpected error occurred: ${exception.message ?: "Unknown error"}"
        _isLoading.value = false
    }

    init {
        // Set up with demo data immediately
        _supplements.value = getDemoSupplements()

        // Then fetch real data
        fetchSupplements()
    }

    /**
     * Always returns something usable - will never crash!
     * Reads all supplements from Firestore and updates the state flow.
     */
    fun fetchSupplements() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true

                // Call repository - will NEVER return null due to our safeguards
                val list = repository.getSupplements()

                if (list.isNotEmpty()) {
                    _supplements.value = list
                    Log.d(TAG, "Fetched ${list.size} supplements")
                } else {
                    Log.d(TAG, "No supplements found, using demo data")
                    // Leave demo data in place
                }
            } catch (e: Exception) {
                // This should never happen due to repository safeguards, but just in case
                Log.e(TAG, "Error fetching supplements: ${e.message}", e)
                _error.value = "Error loading supplements: ${e.message ?: "Unknown error"}"
                // Leave demo data in place
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mark a single supplement as taken or untaken with ultra-robust error handling.
     */
    fun updateSupplementTaken(supplementId: String, isTaken: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true
                Log.d(TAG, "Updating supplement $supplementId taken status to $isTaken")

                // Safety check
                if (supplementId.isEmpty()) {
                    _error.value = "Cannot update supplement with empty ID"
                    _isLoading.value = false
                    return@launch
                }

                // Optimistic update - update the local state immediately
                val currentList = _supplements.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == supplementId }

                if (index != -1) {
                    val updatedSupplement = currentList[index].copy(isTaken = isTaken)
                    currentList[index] = updatedSupplement
                    _supplements.value = currentList
                    Log.d(TAG, "Locally updated supplement $supplementId taken status")
                } else {
                    Log.e(TAG, "Supplement $supplementId not found in local list")
                    _error.value = "Supplement not found"
                    _isLoading.value = false
                    return@launch
                }

                // Update in repository
                try {
                    val success = repository.updateSupplement(supplementId, mapOf("isTaken" to isTaken))
                    if (success) {
                        _statusMessage.value = "Supplement ${if (isTaken) "taken" else "untaken"}"
                    } else {
                        _error.value = "Failed to update supplement in database"
                        // Optimistic update already done - don't refresh
                    }
                } catch (e: Exception) {
                    // This should never happen due to repository safeguards
                    Log.e(TAG, "Error updating supplement: ${e.message}", e)
                    _error.value = "Error updating supplement: ${e.message ?: "Unknown error"}"
                    // Optimistic update already done - don't refresh
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement taken: ${e.message}", e)
                _error.value = "Error updating supplement: ${e.message ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mark ALL supplements as [taken] or not with ultra-robust error handling.
     */
    fun updateAllSupplementsTaken(taken: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true
                Log.d(TAG, "Marking all supplements as ${if (taken) "taken" else "untaken"}")

                // Get the current list of supplements
                val current = _supplements.value
                if (current.isEmpty()) {
                    Log.d(TAG, "No supplements to update")
                    _statusMessage.value = "No supplements to update"
                    _isLoading.value = false
                    return@launch
                }

                // First, update the UI optimistically
                val updatedList = current.map { it.copy(isTaken = taken) }
                _supplements.value = updatedList

                // Then update each supplement
                var successCount = 0
                var failCount = 0

                for (supplement in current) {
                    if (supplement.id.isNotEmpty()) {
                        try {
                            val success = repository.updateSupplement(supplement.id, mapOf("isTaken" to taken))
                            if (success) successCount++ else failCount++
                        } catch (e: Exception) {
                            // This should never happen due to repository safeguards
                            Log.e(TAG, "Error updating supplement ${supplement.id}: ${e.message}", e)
                            failCount++
                        }
                    }
                }

                Log.d(TAG, "Updated $successCount/${current.size} supplements (failed: $failCount)")

                if (failCount == 0) {
                    _statusMessage.value = "All supplements marked as ${if (taken) "taken" else "untaken"}"
                } else if (successCount > 0) {
                    _statusMessage.value = "Updated $successCount supplements, $failCount failed"
                } else {
                    _error.value = "Failed to update supplements"
                    // But UI already shows the optimistic update
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating all supplements: ${e.message}", e)
                _error.value = "Error updating supplements: ${e.message ?: "Unknown error"}"
                // UI already shows the optimistic update
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear the error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear the status message
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Get demo supplements as fallback if repository fails
     * So we ALWAYS show SOMETHING even if Firebase fails completely
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
}