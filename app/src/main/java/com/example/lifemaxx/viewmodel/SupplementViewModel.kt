package com.example.lifemaxx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SupplementViewModel(private val repository: SupplementRepository = SupplementRepository()) : ViewModel() {
    private val _supplements = MutableStateFlow<List<Supplement>>(emptyList())
    val supplements: StateFlow<List<Supplement>> get() = _supplements

    fun fetchSupplements() {
        viewModelScope.launch {
            _supplements.value = repository.getSupplements()
        }
    }

    fun addSupplement(supplement: Supplement) {
        viewModelScope.launch {
            repository.addSupplement(supplement)
            fetchSupplements() // Refresh the list after adding
        }
    }

    fun deleteSupplement(supplementId: String) {
        viewModelScope.launch {
            repository.deleteSupplement(supplementId)
            fetchSupplements()
        }
    }
}
