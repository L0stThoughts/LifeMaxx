package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.NutritionEntry
import com.example.lifemaxx.repository.NutritionRepository
import com.example.lifemaxx.util.DateUtils
import com.example.lifemaxx.util.FirebaseUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for the nutrition tracker feature with complete functionality.
 */
class NutritionViewModel(
    private val repository: NutritionRepository = NutritionRepository(
        context = TODO()
    )
) : ViewModel() {
    private val TAG = "NutritionViewModel"

    // Current date being viewed
    private val _currentDate = MutableStateFlow(DateUtils.getCurrentDate())
    val currentDate: StateFlow<String> get() = _currentDate

    // Nutrition entries for the current date
    private val _nutritionEntries = MutableStateFlow<List<NutritionEntry>>(emptyList())
    val nutritionEntries: StateFlow<List<NutritionEntry>> get() = _nutritionEntries

    // Daily totals (calories and macros)
    private val _dailyTotals = MutableStateFlow(DailyTotals())
    val dailyTotals: StateFlow<DailyTotals> get() = _dailyTotals

    // Status messages
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    // Hard-coded user ID for now - in a real app, this would come from authentication
    private val currentUserId = "user123"

    init {
        // Load entries for today on initialization
        loadEntriesForCurrentDate()
    }

    /**
     * Change the currently viewed date and reload entries.
     */
    fun changeDate(newDate: String) {
        viewModelScope.launch {
            _currentDate.value = newDate
            loadEntriesForCurrentDate()
        }
    }

    /**
     * Move to the previous day.
     */
    fun previousDay() {
        viewModelScope.launch {
            val date = DateUtils.parseDate(_currentDate.value) ?: return@launch
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val newDate = DateUtils.formatDate(calendar.time)
            _currentDate.value = newDate
            loadEntriesForCurrentDate()
        }
    }

    /**
     * Move to the next day.
     */
    fun nextDay() {
        viewModelScope.launch {
            val date = DateUtils.parseDate(_currentDate.value) ?: return@launch
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val newDate = DateUtils.formatDate(calendar.time)
            _currentDate.value = newDate
            loadEntriesForCurrentDate()
        }
    }

    /**
     * Load nutrition entries for the current date.
     */
    private fun loadEntriesForCurrentDate() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entries = repository.getNutritionEntriesByDate(
                    userId = currentUserId,
                    date = _currentDate.value
                )
                _nutritionEntries.value = entries
                calculateDailyTotals(entries)
                Log.d(TAG, "Loaded ${entries.size} entries for ${_currentDate.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading entries: ${e.message}", e)
                _statusMessage.value = "Failed to load nutrition entries"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate totals for the day based on entries.
     */
    private fun calculateDailyTotals(entries: List<NutritionEntry>) {
        var totalCalories = 0
        var totalProteins = 0.0
        var totalCarbs = 0.0
        var totalFats = 0.0

        entries.forEach { entry ->
            totalCalories += entry.calories
            totalProteins += entry.proteins
            totalCarbs += entry.carbs
            totalFats += entry.fats
        }

        _dailyTotals.value = DailyTotals(
            calories = totalCalories,
            proteins = totalProteins,
            carbs = totalCarbs,
            fats = totalFats
        )

        Log.d(TAG, "Calculated daily totals: $totalCalories calories")
    }

    /**
     * Add a new nutrition entry.
     */
    fun addNutritionEntry(entry: NutritionEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create a complete entry with user ID and date
                val completeEntry = entry.copy(
                    userId = currentUserId,
                    date = _currentDate.value
                )

                val success = repository.addNutritionEntry(completeEntry)
                if (success) {
                    _statusMessage.value = "Entry added successfully"
                    loadEntriesForCurrentDate() // Refresh the list
                } else {
                    _statusMessage.value = "Failed to add entry"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding entry: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing nutrition entry.
     */
    fun updateNutritionEntry(
        entryId: String,
        foodName: String,
        calories: Int,
        proteins: Double,
        carbs: Double,
        fats: Double,
        servingSize: Double,
        mealType: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedData = mapOf(
                    "foodName" to foodName,
                    "calories" to calories,
                    "proteins" to proteins,
                    "carbs" to carbs,
                    "fats" to fats,
                    "servingSize" to servingSize,
                    "mealType" to mealType
                )

                val success = repository.updateNutritionEntry(entryId, updatedData)
                if (success) {
                    _statusMessage.value = "Entry updated successfully"
                    loadEntriesForCurrentDate() // Refresh the list
                } else {
                    _statusMessage.value = "Failed to update entry"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating entry: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a nutrition entry.
     */
    fun deleteNutritionEntry(entryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteNutritionEntry(entryId)
                if (success) {
                    _statusMessage.value = "Entry deleted successfully"
                    loadEntriesForCurrentDate() // Refresh the list
                } else {
                    _statusMessage.value = "Failed to delete entry"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting entry: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear the status message after it has been consumed.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Data class to hold the calculated daily totals.
     */
    data class DailyTotals(
        val calories: Int = 0,
        val proteins: Double = 0.0,
        val carbs: Double = 0.0,
        val fats: Double = 0.0
    )
}