package com.example.lifemaxx.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.repository.RobustSupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A robust ViewModel for dose tracking functionality.
 */
class RobustDoseTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RobustDoseTrackerVM"

    // Use direct dependency instead of Koin to avoid injection issues
    private val repository = RobustSupplementRepository(application)

    // StateFlow of supplements for the dose tracker
    private val _supplements = MutableStateFlow<List<Supplement>>(emptyList())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    // Status message for operation feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    // Operation mode (online/offline)
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> get() = _isOfflineMode

    init {
        fetchSupplements()
    }

    /**
     * Reads all supplements from the repository.
     */
    fun fetchSupplements() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val list = repository.getSupplements()
                _supplements.value = list

                _isOfflineMode.value = list.any { it.id.startsWith("local_") || it.id.startsWith("demo_") }

                Log.d(TAG, "Fetched ${list.size} supplements, offline mode: ${_isOfflineMode.value}")
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
    fun updateSupplementTaken(supplementId: String, isTaken: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Safety check
                if (supplementId.isEmpty()) {
                    _error.value = "Cannot update supplement with empty ID"
                    return@launch
                }

                // First update the local state for immediate UI feedback
                val currentList = _supplements.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == supplementId }

                if (index != -1) {
                    val updatedSupplement = currentList[index].copy(isTaken = isTaken)
                    currentList[index] = updatedSupplement
                    _supplements.value = currentList

                    Log.d(TAG, "Locally updated supplement $supplementId taken status")
                }

                // Now perform the actual update
                val success = repository.updateSupplement(supplementId, mapOf("isTaken" to isTaken))

                if (success) {
                    _statusMessage.value = "Supplement status updated" +
                            if (_isOfflineMode.value) " (offline mode)" else ""
                } else {
                    _error.value = "Failed to update supplement status"
                    fetchSupplements() // Refresh to restore correct state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement taken: ${e.message}", e)
                _error.value = "Error updating supplement: ${e.message}"
                fetchSupplements() // Refresh to restore correct state
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mark ALL supplements as taken or untaken.
     */
    fun updateAllSupplementsTaken(taken: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Get the current list of supplements
                val current = _supplements.value
                if (current.isEmpty()) {
                    _statusMessage.value = "No supplements to update"
                    _isLoading.value = false
                    return@launch
                }

                // First, update the UI optimistically
                val updatedList = current.map { it.copy(isTaken = taken) }
                _supplements.value = updatedList

                // Then update all supplements
                val successCount = repository.updateAllSupplementsTaken(taken)

                if (successCount == current.size) {
                    _statusMessage.value = "All supplements marked as ${if (taken) "taken" else "untaken"}" +
                            if (_isOfflineMode.value) " (offline mode)" else ""
                } else {
                    _statusMessage.value = "Updated $successCount/${current.size} supplements"
                    fetchSupplements() // Refresh to get the accurate state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating all supplements: ${e.message}", e)
                _error.value = "Error updating supplements: ${e.message}"
                fetchSupplements() // Refresh to restore correct state
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
     * Factory for creating this ViewModel with the Application context.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RobustDoseTrackerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RobustDoseTrackerViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}