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
 * ViewModel for handling the supplement list, marking them taken/untaken,
 * and deleting, etc. In a bigger app, you might split responsibilities, but
 * here we'll keep them together.
 */
class DoseTrackerViewModel(
    private val repository: SupplementRepository = SupplementRepository()
) : ViewModel() {

    private val _supplements = MutableStateFlow<List<Supplement>>(emptyList())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    init {
        fetchSupplements()
    }

    /**
     * Read all supplements from Firestore and update state.
     */
    fun fetchSupplements() {
        viewModelScope.launch {
            val list = repository.getSupplements()
            _supplements.value = list
            Log.d("DoseTrackerVM", "Fetched: $list")
        }
    }

    /**
     * Add a new supplement (doc ID is assigned automatically by Firestore).
     */
    fun addSupplement(supplement: Supplement) {
        viewModelScope.launch {
            val success = repository.addSupplement(supplement)
            Log.d("DoseTrackerVM", "Add supplement => $success")
            if (success) {
                fetchSupplements()
            }
        }
    }

    /**
     * Mark a single supplement as taken or untaken.
     */
    fun updateSupplementTaken(supplementId: String, isTaken: Boolean) {
        viewModelScope.launch {
            val success = repository.updateSupplement(supplementId, mapOf("isTaken" to isTaken))
            Log.d("DoseTrackerVM", "updateSupplementTaken($supplementId) => $success")
            if (success) {
                fetchSupplements()
            }
        }
    }

    /**
     * Mark ALL supplements as [taken] or not. Loops through the current list
     * and updates each doc's "isTaken" field.
     */
    fun updateAllSupplementsTaken(taken: Boolean) {
        viewModelScope.launch {
            val current = _supplements.value
            current.forEach { supplement ->
                repository.updateSupplement(supplement.id, mapOf("isTaken" to taken))
            }
            fetchSupplements()
        }
    }

    /**
     * Delete a supplement doc by its ID.
     */
    fun deleteSupplement(supplementId: String) {
        viewModelScope.launch {
            val success = repository.deleteSupplement(supplementId)
            Log.d("DoseTrackerVM", "deleteSupplement($supplementId) => $success")
            if (success) {
                fetchSupplements()
            }
        }
    }
}
