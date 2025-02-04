package com.example.lifemaxx.model

/**
 * Represents a supplement in Firestore.
 *
 * @param id             The Firestore doc ID (must be non-empty to update/delete).
 * @param name           Supplement name.
 * @param dailyDose      How much to take daily (in measureUnit).
 * @param remainingQuantity The current supply left.
 * @param isTaken        Whether the user has taken it for the day.
 * @param measureUnit    "pill" or "mg" or any other unit.
 */
data class Supplement(
    val id: String = "",
    val name: String = "",
    val dailyDose: Int = 0,
    val remainingQuantity: Int = 0,
    val isTaken: Boolean = false,
    val measureUnit: String = "pill" // "pill", "mg", or "g"
)

