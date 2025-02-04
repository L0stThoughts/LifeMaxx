package com.example.lifemaxx.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility object for date formatting and parsing within LifeMaxx.
 */
object DateUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd"

    /**
     * Returns the current date string (YYYY-MM-DD) in the device's default locale.
     */
    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Formats a given Date object into a string (YYYY-MM-DD).
     */
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return dateFormat.format(date)
    }

    /**
     * Parses a date string (YYYY-MM-DD) into a Date object.
     * Returns null if parsing fails.
     */
    fun parseDate(dateString: String): Date? {
        return try {
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
