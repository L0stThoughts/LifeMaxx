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
 * ViewModel for managing supplements.
 * Simplified implementation for stability.
 */
class SupplementViewModel(
    private val repository: SupplementRepository
) : ViewModel() {
    private val TAG = "SupplementViewModel"

    private val _supplements = MutableStateFlow<List<Supplement>>(emptyList())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    // Global exception handler to avoid crashes
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Exception in SupplementViewModel: ${exception.message}", exception)
        _error.value = "An error occurred: ${exception.message}"
        _isLoading.value = false
    }

    init {
        // Fetch supplements on initialization
        fetchSupplements()
    }

    /**
     * Fetch all supplements from the repository.
     */
    fun fetchSupplements() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true
                val result = repository.getSupplements()
                _supplements.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching supplements: ${e.message}", e)
                _error.value = "Error loading supplements: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a new supplement.
     */
    suspend fun addSupplement(supplement: Supplement) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true
                val success = repository.addSupplement(supplement)
                if (success) {
                    _statusMessage.value = "Supplement added successfully"
                    fetchSupplements() // Refresh the list
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
     * Update an existing supplement.
     */
    suspend fun updateSupplement(
        supplementId: String,
        name: String,
        dailyDose: Int,
        measureUnit: String,
        remaining: Int
    ) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true

                // Create update map
                val updatedData = mapOf(
                    "name" to name,
                    "dailyDose" to dailyDose,
                    "measureUnit" to measureUnit,
                    "remainingQuantity" to remaining
                )

                val success = repository.updateSupplement(supplementId, updatedData)
                if (success) {
                    _statusMessage.value = "Supplement updated successfully"
                    fetchSupplements() // Refresh the list
                } else {
                    _error.value = "Failed to update supplement"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating supplement: ${e.message}", e)
                _error.value = "Error updating supplement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a supplement.
     */
    suspend fun deleteSupplement(supplementId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                _isLoading.value = true
                val success = repository.deleteSupplement(supplementId)
                if (success) {
                    _statusMessage.value = "Supplement deleted successfully"
                    fetchSupplements() // Refresh the list
                } else {
                    _error.value = "Failed to delete supplement"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting supplement: ${e.message}", e)
                _error.value = "Error deleting supplement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear status message.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}