package com.example.lifemaxx.models

data class Supplement(
    val id: String = "",
    val name: String = "",
    val dosePerDay: Int = 0,
    val totalStock: Int = 0,
    val reminders: List<String> = listOf()
)
