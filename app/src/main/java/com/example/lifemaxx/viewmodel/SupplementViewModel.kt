package com.example.lifemaxx.viewmodel

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
    private val repository: SupplementRepository // Provide your repository implementation here
) : ViewModel() {

    // StateFlow of the current list of supplements
    private val _supplements = MutableStateFlow<List<Supplement>>(emptyList())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    init {
        // Automatically load or refresh supplements on creation
        fetchSupplements()
    }

    /**
     * Reads all supplements from the repository and updates the flow.
     */
    fun fetchSupplements() {
        viewModelScope.launch {
            val list = repository.getSupplements()
            _supplements.value = list
        }
    }

    /**
     * Adds a new supplement to Firestore (or your data source).
     * The repository ensures the doc ID is stored in the 'id' field.
     */
    fun addSupplement(supplement: Supplement) {
        viewModelScope.launch {
            val success = repository.addSupplement(supplement)
            if (success) {
                // Re-fetch to see the new item
                fetchSupplements()
            }
        }
    }

    /**
     * Deletes a supplement by its Firestore doc ID, then refreshes.
     */
    fun deleteSupplement(supplementId: String) {
        viewModelScope.launch {
            val success = repository.deleteSupplement(supplementId)
            if (success) {
                fetchSupplements()
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
            // Build a map of fields to patch in Firestore
            val updatedData = mapOf(
                "name" to name,
                "dailyDose" to dailyDose,
                "measureUnit" to measureUnit,
                "remainingQuantity" to remaining
            )
            val success = repository.updateSupplement(supplementId, updatedData)
            if (success) {
                fetchSupplements()
            }
        }
    }
}
