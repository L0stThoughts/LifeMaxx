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
 * A robust ViewModel for managing supplements with error handling
 * and offline support.
 */
class RobustSupplementViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RobustSupplementVM"

    // Use direct dependency instead of Koin to avoid injection issues
    private val repository = RobustSupplementRepository(application)

    // StateFlow of the current list of supplements
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
        // Automatically load supplements on creation
        fetchSupplements()
    }

    /**
     * Reads all supplements from the repository and updates the flow.
     */
    fun fetchSupplements() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val list = repository.getSupplements()
                _supplements.value = list

                _isOfflineMode.value = list.any { it.id.startsWith("local_") || it.id.startsWith("demo_") }

                if (_isOfflineMode.value) {
                    _statusMessage.value = "Working in offline mode. Some features may be limited."
                }

                Log.d(TAG, "Fetched ${list.size} supplements, offline mode: ${_isOfflineMode.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching supplements: ${e.message}", e)
                _error.value = "Error loading supplements: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Adds a new supplement to the repository.
     */
    fun addSupplement(supplement: Supplement) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = repository.addSupplement(supplement)
                if (success) {
                    fetchSupplements()
                    _statusMessage.value = "Supplement added successfully" +
                            if (_isOfflineMode.value) " (offline mode)" else ""
                } else {
                    _error.value = "Failed to add supplement"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding supplement: ${e.message}", e)
                _error.value = "Error adding supplement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a supplement by its ID.
     */
    fun deleteSupplement(supplementId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Safety check
                if (supplementId.isEmpty()) {
                    _error.value = "Cannot delete supplement with empty ID"
                    return@launch
                }

                // First remove from local list for immediate feedback
                val currentList = _supplements.value.toMutableList()
                val indexToRemove = currentList.indexOfFirst { it.id == supplementId }

                if (indexToRemove >= 0) {
                    currentList.removeAt(indexToRemove)
                    _supplements.value = currentList
                }

                // Then perform the actual deletion
                val success = repository.deleteSupplement(supplementId)

                if (success) {
                    fetchSupplements() // Refresh to ensure consistency
                    _statusMessage.value = "Supplement deleted successfully" +
                            if (_isOfflineMode.value) " (offline mode)" else ""
                } else {
                    _error.value = "Failed to delete supplement"
                    fetchSupplements() // Refresh to restore if delete failed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting supplement: ${e.message}", e)
                _error.value = "Error deleting supplement: ${e.message}"
                fetchSupplements() // Refresh to restore state
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates an existing supplement.
     */
    fun updateSupplement(
        supplementId: String,
        name: String,
        dailyDose: Int,
        measureUnit: String,
        remaining: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Safety check
                if (supplementId.isEmpty()) {
                    _error.value = "Cannot update supplement with empty ID"
                    return@launch
                }

                // Build a map of fields to update
                val updatedData = mapOf(
                    "name" to name,
                    "dailyDose" to dailyDose,
                    "measureUnit" to measureUnit,
                    "remainingQuantity" to remaining
                )

                // First update the local copy for immediate feedback
                val currentList = _supplements.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == supplementId }

                if (index != -1) {
                    val updatedSupplement = currentList[index].copy(
                        name = name,
                        dailyDose = dailyDose,
                        measureUnit = measureUnit,
                        remainingQuantity = remaining
                    )
                    currentList[index] = updatedSupplement
                    _supplements.value = currentList
                }

                // Then perform the actual update
                val success = repository.updateSupplement(supplementId, updatedData)

                if (success) {
                    fetchSupplements() // Refresh to ensure consistency
                    _statusMessage.value = "Supplement updated successfully" +
                            if (_isOfflineMode.value) " (offline mode)" else ""
                } else {
                    _error.value = "Failed to update supplement"
                    fetchSupplements() // Refresh to restore if update failed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement: ${e.message}", e)
                _error.value = "Error updating supplement: ${e.message}"
                fetchSupplements() // Refresh to restore state
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear the status message after it has been consumed.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Clear the error message after it has been consumed.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Factory for creating this ViewModel with the Application context.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RobustSupplementViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RobustSupplementViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}