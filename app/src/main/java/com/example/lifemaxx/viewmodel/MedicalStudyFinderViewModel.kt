package com.example.lifemaxx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.MedicalStudy
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MedicalStudyFinderViewModel : ViewModel() {
    private val _medicalStudies = MutableStateFlow<List<MedicalStudy>>(emptyList())
    val medicalStudies: StateFlow<List<MedicalStudy>> = _medicalStudies

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchMedicalStudies()
    }

    private fun fetchMedicalStudies() {
        viewModelScope.launch {
            db.collection("medicalStudies").get()
                .addOnSuccessListener { result ->
                    val studies = result.map { it.toObject(MedicalStudy::class.java) }
                    _medicalStudies.value = studies
                }
                .addOnFailureListener {
                    _medicalStudies.value = emptyList()
                }
        }
    }

    fun addMedicalStudy(study: MedicalStudy, onComplete: (Boolean) -> Unit) {
        db.collection("medicalStudies").add(study)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
