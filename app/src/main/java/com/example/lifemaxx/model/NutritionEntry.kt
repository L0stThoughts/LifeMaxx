package com.example.lifemaxx.model

/**
 * Represents a single nutrition entry, tracking macros and calories.
 *
 * @param id The unique identifier for this entry
 * @param userId The user who created this entry
 * @param date The date of this entry in format YYYY-MM-DD
 * @param foodName The name of the food
 * @param calories Total calories
 * @param proteins Protein in grams
 * @param carbs Carbohydrates in grams
 * @param fats Fat in grams
 * @param servingSize The serving size in grams or ml
 * @param mealType Which meal (Breakfast, Lunch, Dinner, Snack)
 * @param timestamp When this entry was created
 */
data class NutritionEntry(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val foodName: String = "",
    val calories: Int = 0,
    val proteins: Double = 0.0,
    val carbs: Double = 0.0,
    val fats: Double = 0.0,
    val servingSize: Double = 0.0,
    val mealType: String = MealType.OTHER,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Nested class for meal type constants
    object MealType {
        const val BREAKFAST = "Breakfast"
        const val LUNCH = "Lunch"
        const val DINNER = "Dinner"
        const val SNACK = "Snack"
        const val OTHER = "Other"

        // List of all meal types for UI dropdowns
        val ALL = listOf(BREAKFAST, LUNCH, DINNER, SNACK, OTHER)
    }
}