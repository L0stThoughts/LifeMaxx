package com.example.lifemaxx.model

/**
 * Represents a daily dose record for a specific supplement on a specific date.
 *
 * @param doseId          Unique identifier for this dose record (for Firestore or other DB usage).
 * @param supplementId    ID of the supplement (links to Supplement).
 * @param date            Date in format: YYYY-MM-DD.
 * @param dailyRequired   How many doses are required for this supplement on this date.
 * @param dosesTaken      How many doses have been taken so far on this date.
 */
data class Dose(
    val doseId: String = "",
    val supplementId: String,
    val date: String,
    val dailyRequired: Int = 0,
    val dosesTaken: Int = 0
)
