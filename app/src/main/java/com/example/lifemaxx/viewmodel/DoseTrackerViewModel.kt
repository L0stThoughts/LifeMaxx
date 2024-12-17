package com.example.lifemaxx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Dose
import com.example.lifemaxx.repository.DoseRepository
import com.example.lifemaxx.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DoseTrackerViewModel(private val repository: DoseRepository = DoseRepository()) : ViewModel() {
    private val _doses = MutableStateFlow<List<Dose>>(emptyList())
    val doses: StateFlow<List<Dose>> get() = _doses

    fun fetchDosesForToday() {
        val today = DateUtils.getCurrentDate()
        viewModelScope.launch {
            _doses.value = repository.getDosesByDate(today)
        }
    }

    fun addDose(supplementId: String, dosesTaken: Int) {
        val today = DateUtils.getCurrentDate()
        viewModelScope.launch {
            val dose = Dose(supplementId = supplementId, date = today, dosesTaken = dosesTaken)
            repository.addDose(dose)
            fetchDosesForToday()
        }
    }
}
