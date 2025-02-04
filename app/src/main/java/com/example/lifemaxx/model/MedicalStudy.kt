package com.example.lifemaxx.model

/**
 * Represents a medical study pulled from a database or external source.
 *
 * @param studyId      Unique identifier for the study (for Firestore or other DB usage).
 * @param title        The title or name of the study.
 * @param description  A short description or summary of the study.
 * @param link         A URL link to the full study or external resource.
 * @param category     Optional category to group or filter studies (e.g., "Nutrition", "Cardiology", etc.).
 * @param tags         A list of keywords or tags that further classify the study (e.g., ["Vitamin D", "Bone Health"]).
 */
data class MedicalStudy(
    val studyId: String = "",
    val title: String = "",
    val description: String = "",
    val link: String = "",
    val category: String = "",
    val tags: List<String> = emptyList()
)
