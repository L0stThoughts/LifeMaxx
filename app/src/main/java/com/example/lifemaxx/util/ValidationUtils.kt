package com.example.lifemaxx.util

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isFieldNotEmpty(input: String): Boolean {
        return input.trim().isNotEmpty()
    }
}
