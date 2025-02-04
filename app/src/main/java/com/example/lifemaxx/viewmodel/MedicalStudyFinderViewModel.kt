package com.example.lifemaxx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.MedicalStudy
import com.example.lifemaxx.repository.MedicalStudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing a list of MedicalStudy objects from Firestore.
 */
class MedicalStudyFinderViewModel(
    private val repository: MedicalStudyRepository = MedicalStudyRepository()
) : ViewModel() {

    private val _medicalStudies = MutableStateFlow<List<MedicalStudy>>(emptyList())
    val medicalStudies: StateFlow<List<MedicalStudy>> = _medicalStudies

    init {
        fetchMedicalStudies()
    }

    /**
     * Retrieves all studies from the repository and posts them to [medicalStudies].
     */
    fun fetchMedicalStudies() {
        viewModelScope.launch {
            val result = repository.getAllStudies()
            _medicalStudies.value = result
        }
    }

    /**
     * Adds a new study to Firestore.
     */
    fun addMedicalStudy(study: MedicalStudy, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addStudy(study)
            onComplete(success)
            if (success) {
                fetchMedicalStudies()
            }
        }
    }
}
