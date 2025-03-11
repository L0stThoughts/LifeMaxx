package com.example.lifemaxx.model

/**
 * Represents a sleep tracking entry.
 *
 * @param id Unique identifier for the sleep entry
 * @param userId ID of the user this entry belongs to
 * @param date Date of the sleep in YYYY-MM-DD format
 * @param sleepTime Time when the user went to sleep (in milliseconds)
 * @param wakeTime Time when the user woke up (in milliseconds)
 * @param duration Total sleep duration in minutes
 * @param quality Sleep quality rating from 1-5
 * @param notes Optional notes about sleep (factors, dreams, etc)
 * @param deepSleepMinutes Optional tracking of deep sleep duration
 * @param remSleepMinutes Optional tracking of REM sleep duration
 * @param interruptions Number of times sleep was interrupted
 * @param timestamp When this entry was recorded (in milliseconds)
 */
data class SleepEntry(
    val id: String = "",
    val userId: String = "",
    val date: String = "",  // YYYY-MM-DD format
    val sleepTime: Long = 0L,
    val wakeTime: Long = 0L,
    val duration: Int = 0,  // In minutes
    val quality: Int = 3,   // 1-5 scale
    val notes: String = "",
    val deepSleepMinutes: Int = 0,
    val remSleepMinutes: Int = 0,
    val interruptions: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val MIN_QUALITY = 1
        const val MAX_QUALITY = 5

        // Sleep quality descriptions
        val QUALITY_DESCRIPTIONS = mapOf(
            1 to "Very Poor",
            2 to "Poor",
            3 to "Average",
            4 to "Good",
            5 to "Excellent"
        )
    }
}