package com.example.lifemaxx.model

data class Supplement(
    val id: String = "",
    val name: String = "",
    val dailyDose: Int = 0,   // How many doses per day
    val remainingQuantity: Int = 0 // Remaining amount of supplement
)
