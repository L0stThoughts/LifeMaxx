package com.example.lifemaxx.model

data class MedicalStudy(
    val id: String = "",          // Unique identifier for the study
    val title: String = "",       // Study title
    val description: String = "", // A short description of the study
    val link: String = ""         // Link to the full study
)