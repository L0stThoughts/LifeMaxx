package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.repository.SupplementRepository
import com.example.lifemaxx.util.FirebaseUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling the supplement list, marking them taken/untaken,
 * and deleting, etc.
 */
class DoseTrackerViewModel(
    private val repository: SupplementRepository
) : ViewModel() {
    private val TAG = "DoseTrackerViewModel"

    private val _supplements = MutableStateFlow<List<Supplement>>(emptyList())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    init {
        fetchSupplements()
    }

    /**
     * Read all supplements from Firestore and update state.
     */
    fun fetchSupplements() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val list = repository.getSupplements()
                _supplements.value = list
                Log.d(TAG, "Fetched ${list.size} supplements")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching supplements: ${e.message}", e)
                _error.value = "Failed to load supplements: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mark a single supplement as taken or untaken.
     */
    suspend fun updateSupplementTaken(supplementId: String, isTaken: Boolean) {
        try {
            _isLoading.value = true
            Log.d(TAG, "Updating supplement $supplementId taken status to $isTaken")

            // Safety check
            if (supplementId.isEmpty()) {
                _error.value = "Cannot update supplement with empty ID"
                return
            }

            // Optimistic update - update the local state immediately
            val currentList = _supplements.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == supplementId }

            if (index != -1) {
                val updatedSupplement = currentList[index].copy(isTaken = isTaken)
                currentList[index] = updatedSupplement
                _supplements.value = currentList
                Log.d(TAG, "Locally updated supplement $supplementId taken status")
            }

            // Update in Firestore
            val success = repository.updateSupplement(supplementId, mapOf("isTaken" to isTaken))

            if (!success) {
                _error.value = "Failed to update supplement in database"
                fetchSupplements() // Refresh to get the correct state
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating supplement taken: ${e.message}", e)
            _error.value = "Error updating supplement: ${e.message}"
            fetchSupplements() // Refresh to get the correct state
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Mark ALL supplements as [taken] or not. Loops through the current list
     * and updates each doc's "isTaken" field.
     */
    suspend fun updateAllSupplementsTaken(taken: Boolean) {
        try {
            _isLoading.value = true
            Log.d(TAG, "Marking all supplements as ${if (taken) "taken" else "untaken"}")

            // Get the current list of supplements
            val current = _supplements.value
            if (current.isEmpty()) {
                Log.d(TAG, "No supplements to update")
                _statusMessage.value = "No supplements to update"
                _isLoading.value = false
                return
            }

            // First, update the UI optimistically
            val updatedList = current.map { it.copy(isTaken = taken) }
            _supplements.value = updatedList

            // Then update Firestore for each supplement
            var successCount = 0
            for (supplement in current) {
                if (supplement.id.isNotEmpty()) {
                    val success = repository.updateSupplement(supplement.id, mapOf("isTaken" to taken))
                    if (success) successCount++
                }
            }

            Log.d(TAG, "Successfully updated $successCount/${current.size} supplements")

            if (successCount == current.size) {
                _statusMessage.value = "All supplements marked as ${if (taken) "taken" else "untaken"}"
            } else {
                _statusMessage.value = "Updated $successCount/${current.size} supplements"
                // Refresh to get the accurate state
                fetchSupplements()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating all supplements: ${e.message}", e)
            _error.value = "Error updating supplements: ${e.message}"
            fetchSupplements() // Refresh to get the correct state
        } finally {
            _isLoading.value = false
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
}