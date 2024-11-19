package com.example.lifemaxx.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    /**
     * Formats a given date to a specified format.
     *
     * @param date The date to format.
     * @param format The desired date format, e.g., "dd/MM/yyyy".
     * @return A string representation of the date in the specified format.
     */
    fun formatDate(date: Date, format: String = "dd/MM/yyyy"): String {
        val formatter = SimpleDateFormat(format, Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Converts a string to a Date object.
     *
     * @param dateString The date string to convert.
     * @param format The format of the date string, e.g., "dd/MM/yyyy".
     * @return The parsed Date object, or null if the parsing fails.
     */
    fun parseDate(dateString: String, format: String = "dd/MM/yyyy"): Date? {
        return try {
            val formatter = SimpleDateFormat(format, Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the current date in a specified format.
     *
     * @param format The desired date format, e.g., "dd/MM/yyyy".
     * @return A string representation of the current date in the specified format.
     */
    fun getCurrentDate(format: String = "dd/MM/yyyy"): String {
        val currentDate = Date()
        return formatDate(currentDate, format)
    }

    /**
     * Calculates the difference between two dates in days.
     *
     * @param startDate The start date.
     * @param endDate The end date.
     * @return The difference in days between the two dates.
     */
    fun calculateDaysBetween(startDate: Date, endDate: Date): Long {
        val diffInMillis = endDate.time - startDate.time
        return diffInMillis / (1000 * 60 * 60 * 24)
    }
}
