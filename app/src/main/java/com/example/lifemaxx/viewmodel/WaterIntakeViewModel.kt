package com.example.lifemaxx.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.WaterIntake
import com.example.lifemaxx.repository.WaterIntakeRepository
import com.example.lifemaxx.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * ViewModel for water intake tracking functionality.
 */
class WaterIntakeViewModel(
    private val repository: WaterIntakeRepository
) : ViewModel() {
    private val TAG = "WaterIntakeViewModel"

    // Selected date for tracking
    private val _currentDate = MutableStateFlow(DateUtils.getCurrentDate())
    val currentDate: StateFlow<String> get() = _currentDate

    // Water intake entries for the current date
    private val _waterIntakes = MutableStateFlow<List<WaterIntake>>(emptyList())
    val waterIntakes: StateFlow<List<WaterIntake>> get() = _waterIntakes

    // Total water intake for the current date (in ml)
    private val _totalWaterIntake = MutableStateFlow(0)
    val totalWaterIntake: StateFlow<Int> get() = _totalWaterIntake

    // Daily goal for water intake (in ml)
    private val _dailyGoal = MutableStateFlow(2500)
    val dailyGoal: StateFlow<Int> get() = _dailyGoal

    // Water intake entry being edited
    private val _editingWaterIntake = MutableStateFlow<WaterIntake?>(null)
    val editingWaterIntake: StateFlow<WaterIntake?> get() = _editingWaterIntake

    // Weekly water intake totals
    private val _weeklyTotals = MutableStateFlow<Map<String, Int>>(emptyMap())
    val weeklyTotals: StateFlow<Map<String, Int>> get() = _weeklyTotals

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    // Hard-coded user ID for now - in a real app, this would come from authentication
    private val currentUserId = "user123"

    init {
        fetchWaterIntakesForCurrentDate()
        fetchWeeklyTotals()
    }

    /**
     * Change the currently viewed date.
     */
    fun changeDate(date: String) {
        viewModelScope.launch {
            _currentDate.value = date
            fetchWaterIntakesForCurrentDate()
        }
    }

    /**
     * Set the daily water intake goal.
     */
    fun setDailyGoal(goal: Int) {
        _dailyGoal.value = goal
    }

    /**
     * Fetch water intake entries for the current date.
     */
    private fun fetchWaterIntakesForCurrentDate() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entries = repository.getWaterIntakesByDate(
                    userId = currentUserId,
                    date = _currentDate.value
                )
                _waterIntakes.value = entries

                // Calculate total water intake
                _totalWaterIntake.value = entries.sumOf { it.amount }

                Log.d(TAG, "Fetched ${entries.size} water intake entries for ${_currentDate.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching water intake entries: ${e.message}", e)
                _statusMessage.value = "Failed to load water intake entries: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch weekly water intake totals.
     */
    private fun fetchWeeklyTotals() {
        viewModelScope.launch {
            try {
                val totals = repository.getWeeklyWaterIntakeTotals(currentUserId)
                _weeklyTotals.value = totals
                Log.d(TAG, "Fetched weekly totals: $totals")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weekly totals: ${e.message}", e)
            }
        }
    }

    /**
     * Add a quick water intake entry using a predefined container.
     */
    fun addQuickWaterIntake(containerType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get default amount for container type
                val amount = WaterIntake.ContainerType.getDefaultSize(containerType)

                // Create water intake entry
                val waterIntake = WaterIntake(
                    userId = currentUserId,
                    date = _currentDate.value,
                    amount = amount,
                    time = System.currentTimeMillis(),
                    containerType = containerType
                )

                // Save to repository
                val success = repository.addWaterIntake(waterIntake)
                if (success) {
                    _statusMessage.value = "$containerType of water added"
                    fetchWaterIntakesForCurrentDate()
                    fetchWeeklyTotals()
                } else {
                    _statusMessage.value = "Failed to add water intake"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding water intake: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a custom water intake entry.
     */
    fun addCustomWaterIntake(amount: Int, containerType: String = WaterIntake.ContainerType.CUSTOM) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create water intake entry
                val waterIntake = WaterIntake(
                    userId = currentUserId,
                    date = _currentDate.value,
                    amount = amount,
                    time = System.currentTimeMillis(),
                    containerType = containerType
                )

                // Save to repository
                val success = repository.addWaterIntake(waterIntake)
                if (success) {
                    _statusMessage.value = "$amount ml of water added"
                    fetchWaterIntakesForCurrentDate()
                    fetchWeeklyTotals()
                } else {
                    _statusMessage.value = "Failed to add water intake"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding water intake: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing water intake entry.
     */
    fun updateWaterIntake(entryId: String, amount: Int, containerType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create updated data map
                val updatedData = mapOf(
                    "amount" to amount,
                    "containerType" to containerType
                )

                // Update in repository
                val success = repository.updateWaterIntake(entryId, updatedData)
                if (success) {
                    _statusMessage.value = "Water intake updated successfully"
                    fetchWaterIntakesForCurrentDate()
                    fetchWeeklyTotals()
                } else {
                    _statusMessage.value = "Failed to update water intake"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating water intake: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                _editingWaterIntake.value = null
            }
        }
    }

    /**
     * Delete a water intake entry.
     */
    fun deleteWaterIntake(entryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteWaterIntake(entryId)
                if (success) {
                    _statusMessage.value = "Water intake deleted successfully"
                    fetchWaterIntakesForCurrentDate()
                    fetchWeeklyTotals()
                } else {
                    _statusMessage.value = "Failed to delete water intake"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting water intake: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set a water intake entry for editing.
     */
    fun editWaterIntake(entry: WaterIntake) {
        _editingWaterIntake.value = entry
    }

    /**
     * Cancel editing a water intake entry.
     */
    fun cancelEditingWaterIntake() {
        _editingWaterIntake.value = null
    }

    /**
     * Format a timestamp to display time.
     */
    fun formatTime(timeInMillis: Long): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeInMillis))
    }

    /**
     * Calculate progress percentage towards daily goal.
     */
    fun calculateProgressPercentage(): Float {
        val goal = _dailyGoal.value
        if (goal <= 0) return 0f

        val current = _totalWaterIntake.value
        return (current.toFloat() / goal).coerceIn(0f, 1f)
    }

    /**
     * Format a date for UI display.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateForUI(date: String): String {
        // Convert from YYYY-MM-DD to more readable format
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = LocalDate.parse(date, formatter)

            val outputFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            return localDate.format(outputFormatter)
        } catch (e: Exception) {
            return date
        }
    }

    /**
     * Format water amount for UI display.
     */
    fun formatWaterAmount(amount: Int): String {
        return when {
            amount < 1000 -> "$amount ml"
            else -> String.format("%.1f L", amount / 1000.0)
        }
    }

    /**
     * Clear the status message after it has been consumed.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}