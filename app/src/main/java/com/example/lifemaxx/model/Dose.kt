package com.example.lifemaxx.model

data class Dose(
    val supplementId: String,   // ID of the supplement
    val date: String,           // Date in format: YYYY-MM-DD
    val dosesTaken: Int = 0     // How many doses have been taken
)