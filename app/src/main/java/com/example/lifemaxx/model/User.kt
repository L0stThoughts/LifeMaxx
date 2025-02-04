package com.example.lifemaxx.model

/**
 * Represents a user in the LifeMaxx system.
 *
 * @param id                   Unique user ID (matches the Firestore document ID).
 * @param name                 User's display name.
 * @param email                User's email address.
 * @param notificationEnabled  Whether notifications are enabled for this user.
 * @param preferredDoseTime    Default daily reminder time in "HH:MM" format.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val notificationEnabled: Boolean = true,
    val preferredDoseTime: String = "08:00"
)
