package com.example.lifemaxx.model

/**
 * Represents a reminder that fires a local notification at [timeInMillis].
 * [id] is unique for AlarmManager scheduling.
 * [message] is the notification text.
 * [isEnabled] indicates if it's currently scheduled or not.
 */
data class Reminder(
    val id: Int,                 // Unique ID for scheduling
    val timeInMillis: Long,      // The exact time (in millis) for the notification
    val message: String = "Don't forget to take your supplements!",
    val isEnabled: Boolean = true
)
