package com.example.lifemaxx.util

import android.util.Patterns

/**
 * Collection of validation methods for common user input.
 */
object ValidationUtils {

    /**
     * Checks if the provided [email] is a valid email format.
     */
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Checks if the provided [input] is not empty after trimming.
     */
    fun isFieldNotEmpty(input: String): Boolean {
        return input.trim().isNotEmpty()
    }
}
