package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A ViewModel for managing supplements from a repository (e.g., Firestore).
 * Holds a list of supplements in a StateFlow, supports CRUD operations.
 */
class SupplementViewModel(
    private val repository: SupplementRepository
) : ViewModel() {
    private val TAG = "SupplementViewModel"

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

    init {
        // Automatically load or refresh supplements on creation
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
                Log.d(TAG, "Fetched ${list.size} supplements")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching supplements: ${e.message}", e)
                _error.value = "Error loading supplements: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Adds a new supplement to Firestore (or your data source).
     * The repository ensures the doc ID is stored in the 'id' field.
     */
    fun addSupplement(supplement: Supplement) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = repository.addSupplement(supplement)
                if (success) {
                    // Re-fetch to see the new item
                    fetchSupplements()
                    _statusMessage.value = "Supplement added successfully"
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
     * Deletes a supplement by its Firestore doc ID, then refreshes.
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

                val success = repository.deleteSupplement(supplementId)
                if (success) {
                    // First remove from local list for immediate feedback
                    val currentList = _supplements.value.toMutableList()
                    val indexToRemove = currentList.indexOfFirst { it.id == supplementId }

                    if (indexToRemove >= 0) {
                        currentList.removeAt(indexToRemove)
                        _supplements.value = currentList
                    }

                    // Then refresh from server to ensure consistency
                    fetchSupplements()
                    _statusMessage.value = "Supplement deleted successfully"
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
     * General update for editing an existing supplement.
     * The repository updates Firestore fields, then we refresh the list.
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

                // Build a map of fields to patch in Firestore
                val updatedData = mapOf(
                    "name" to name,
                    "dailyDose" to dailyDose,
                    "measureUnit" to measureUnit,
                    "remainingQuantity" to remaining
                )

                val success = repository.updateSupplement(supplementId, updatedData)
                if (success) {
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

                    // Then refresh from server to ensure consistency
                    fetchSupplements()
                    _statusMessage.value = "Supplement updated successfully"
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
}