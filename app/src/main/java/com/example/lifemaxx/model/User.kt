package com.example.lifemaxx.model

data class User(
    val id: String = "",            // Unique user ID
    val name: String = "",          // User's name
    val email: String = "",         // User's email
    val notificationEnabled: Boolean = true, // Whether notifications are on/off
    val preferredDoseTime: String = "08:00"   // Default reminder time
)