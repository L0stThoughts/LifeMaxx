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
 * ViewModel for the Dose Tracker screen.
 * Simplified implementation that only tracks supplement taken status.
 */
class DoseTrackerViewModel(
    private val supplementRepository: SupplementRepository
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

    // Exception handler to catch and display errors
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Exception in DoseTrackerViewModel: ${exception.message}", exception)
        _error.value = "An error occurred: ${exception.message}"
        _isLoading.value = false
    }

    /**
     * Fetch supplements from the repository.
     */
    fun fetchSupplements() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true
                val result = supplementRepository.getSupplements()
                _supplements.value = result
                if (result.isEmpty()) {
                    _statusMessage.value = "No supplements found. Add some in the Supplements screen."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching supplements: ${e.message}", e)
                _error.value = "Error loading supplements: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mark a supplement as taken/untaken.
     */
    fun updateSupplementTaken(supplementId: String, isTaken: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Find the supplement in the current list
                val supplement = _supplements.value.find { it.id == supplementId }
                if (supplement == null) {
                    _error.value = "Supplement not found"
                    return@launch
                }

                // Update the local list optimistically
                val updatedList = _supplements.value.map {
                    if (it.id == supplementId) it.copy(isTaken = isTaken) else it
                }
                _supplements.value = updatedList

                // Update in repository
                val success = supplementRepository.updateSupplement(
                    supplementId,
                    mapOf("isTaken" to isTaken)
                )

                if (success) {
                    _statusMessage.value = if (isTaken) "Marked as taken" else "Marked as not taken"
                } else {
                    _error.value = "Failed to update supplement"
                    // Revert if failed
                    fetchSupplements()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement: ${e.message}", e)
                _error.value = "Error updating: ${e.message}"
                // Refresh to ensure UI consistency
                fetchSupplements()
            }
        }
    }

    /**
     * Mark all supplements as taken/untaken.
     */
    fun updateAllSupplementsTaken(taken: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                if (_supplements.value.isEmpty()) {
                    _statusMessage.value = "No supplements to update"
                    return@launch
                }

                // Optimistically update all supplements
                val updatedList = _supplements.value.map { it.copy(isTaken = taken) }
                _supplements.value = updatedList

                // Update each supplement in the repository
                var successCount = 0
                var failCount = 0

                for (supplement in updatedList) {
                    val success = supplementRepository.updateSupplement(
                        supplement.id,
                        mapOf("isTaken" to taken)
                    )
                    if (success) successCount++ else failCount++
                }

                if (failCount == 0) {
                    _statusMessage.value = "All supplements ${if (taken) "taken" else "untaken"}"
                } else {
                    _statusMessage.value = "Updated $successCount, failed $failCount"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating all supplements: ${e.message}", e)
                _error.value = "Error updating all: ${e.message}"
                // Refresh to ensure UI consistency
                fetchSupplements()
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear status message.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}