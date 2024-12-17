package com.example.lifemaxx.model

data class Reminder(
    val id: String = "",
    val supplementName: String = "",
    val reminderTime: String = "08:00", // Expected format: "HH:MM"
    val isCompleted: Boolean = false
)