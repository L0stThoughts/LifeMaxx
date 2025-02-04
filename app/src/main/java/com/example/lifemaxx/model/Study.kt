package com.example.lifemaxx.model

/**
 * A simple data class representing a study preview, possibly used for mock data or local UI.
 * May differ from [MedicalStudy] if you want a lightweight version for display.
 *
 * @param title       Title of the study (short).
 * @param description A brief description or summary of the study.
 */
data class Study(
    val title: String,
    val description: String
)
