package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.SleepEntry
import com.example.lifemaxx.repository.SleepRepository
import com.example.lifemaxx.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * ViewModel for sleep tracking functionality.
 */
class SleepViewModel(
    private val repository: SleepRepository
) : ViewModel() {
    private val TAG = "SleepViewModel"

    // Selected date for tracking
    private val _currentDate = MutableStateFlow(DateUtils.getCurrentDate())
    val currentDate: StateFlow<String> get() = _currentDate

    // Sleep entries for the current date
    private val _sleepEntries = MutableStateFlow<List<SleepEntry>>(emptyList())
    val sleepEntries: StateFlow<List<SleepEntry>> get() = _sleepEntries

    // Sleep entry being edited
    private val _editingSleepEntry = MutableStateFlow<SleepEntry?>(null)
    val editingSleepEntry: StateFlow<SleepEntry?> get() = _editingSleepEntry

    // Recent sleep entries (past week)
    private val _recentSleepEntries = MutableStateFlow<List<SleepEntry>>(emptyList())
    val recentSleepEntries: StateFlow<List<SleepEntry>> get() = _recentSleepEntries

    // Sleep statistics
    private val _averageSleepDuration = MutableStateFlow(0.0)
    val averageSleepDuration: StateFlow<Double> get() = _averageSleepDuration

    private val _averageSleepQuality = MutableStateFlow(0.0)
    val averageSleepQuality: StateFlow<Double> get() = _averageSleepQuality

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    // Hard-coded user ID for now - in a real app, this would come from authentication
    private val currentUserId = "user123"

    init {
        fetchSleepEntriesForCurrentDate()
        fetchRecentSleepEntries()
        calculateSleepStatistics()
    }

    /**
     * Change the currently viewed date.
     */
    fun changeDate(date: String) {
        viewModelScope.launch {
            _currentDate.value = date
            fetchSleepEntriesForCurrentDate()
        }
    }

    /**
     * Fetch sleep entries for the current date.
     */
    private fun fetchSleepEntriesForCurrentDate() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entries = repository.getSleepEntriesByDate(
                    userId = currentUserId,
                    date = _currentDate.value
                )
                _sleepEntries.value = entries
                Log.d(TAG, "Fetched ${entries.size} sleep entries for ${_currentDate.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sleep entries: ${e.message}", e)
                _statusMessage.value = "Failed to load sleep entries: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch recent sleep entries (past week).
     */
    private fun fetchRecentSleepEntries() {
        viewModelScope.launch {
            try {
                val entries = repository.getRecentSleepEntries(currentUserId)
                _recentSleepEntries.value = entries
                Log.d(TAG, "Fetched ${entries.size} recent sleep entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching recent sleep entries: ${e.message}", e)
            }
        }
    }

    /**
     * Calculate sleep statistics.
     */
    private fun calculateSleepStatistics() {
        viewModelScope.launch {
            try {
                val avgDuration = repository.getAverageSleepDuration(currentUserId, 30)
                _averageSleepDuration.value = avgDuration

                val avgQuality = repository.getAverageSleepQuality(currentUserId, 30)
                _averageSleepQuality.value = avgQuality

                Log.d(TAG, "Calculated sleep statistics: duration=$avgDuration, quality=$avgQuality")
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating sleep statistics: ${e.message}", e)
            }
        }
    }

    /**
     * Add a new sleep entry.
     */
    fun addSleepEntry(
        sleepTime: Long,
        wakeTime: Long,
        quality: Int,
        notes: String,
        deepSleepMinutes: Int = 0,
        remSleepMinutes: Int = 0,
        interruptions: Int = 0
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Calculate sleep duration in minutes
                val durationMinutes = ((wakeTime - sleepTime) / (1000 * 60)).toInt()

                // Create sleep entry object
                val sleepEntry = SleepEntry(
                    userId = currentUserId,
                    date = _currentDate.value,
                    sleepTime = sleepTime,
                    wakeTime = wakeTime,
                    duration = durationMinutes,
                    quality = quality.coerceIn(SleepEntry.MIN_QUALITY, SleepEntry.MAX_QUALITY),
                    notes = notes,
                    deepSleepMinutes = deepSleepMinutes,
                    remSleepMinutes = remSleepMinutes,
                    interruptions = interruptions
                )

                // Save to repository
                val success = repository.addSleepEntry(sleepEntry)
                if (success) {
                    _statusMessage.value = "Sleep entry added successfully"
                    fetchSleepEntriesForCurrentDate()
                    fetchRecentSleepEntries()
                    calculateSleepStatistics()
                } else {
                    _statusMessage.value = "Failed to add sleep entry"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding sleep entry: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing sleep entry.
     */
    fun updateSleepEntry(
        entryId: String,
        sleepTime: Long,
        wakeTime: Long,
        quality: Int,
        notes: String,
        deepSleepMinutes: Int = 0,
        remSleepMinutes: Int = 0,
        interruptions: Int = 0
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Calculate sleep duration in minutes
                val durationMinutes = ((wakeTime - sleepTime) / (1000 * 60)).toInt()

                // Create updated data map
                val updatedData = mapOf(
                    "sleepTime" to sleepTime,
                    "wakeTime" to wakeTime,
                    "duration" to durationMinutes,
                    "quality" to quality.coerceIn(SleepEntry.MIN_QUALITY, SleepEntry.MAX_QUALITY),
                    "notes" to notes,
                    "deepSleepMinutes" to deepSleepMinutes,
                    "remSleepMinutes" to remSleepMinutes,
                    "interruptions" to interruptions
                )

                // Update in repository
                val success = repository.updateSleepEntry(entryId, updatedData)
                if (success) {
                    _statusMessage.value = "Sleep entry updated successfully"
                    fetchSleepEntriesForCurrentDate()
                    fetchRecentSleepEntries()
                    calculateSleepStatistics()
                } else {
                    _statusMessage.value = "Failed to update sleep entry"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating sleep entry: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                _editingSleepEntry.value = null
            }
        }
    }

    /**
     * Delete a sleep entry.
     */
    fun deleteSleepEntry(entryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteSleepEntry(entryId)
                if (success) {
                    _statusMessage.value = "Sleep entry deleted successfully"
                    fetchSleepEntriesForCurrentDate()
                    fetchRecentSleepEntries()
                    calculateSleepStatistics()
                } else {
                    _statusMessage.value = "Failed to delete sleep entry"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting sleep entry: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set a sleep entry for editing.
     */
    fun editSleepEntry(entry: SleepEntry) {
        _editingSleepEntry.value = entry
    }

    /**
     * Cancel editing a sleep entry.
     */
    fun cancelEditingSleepEntry() {
        _editingSleepEntry.value = null
    }

    /**
     * Format a timestamp to display time.
     */
    fun formatTime(timeInMillis: Long): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeInMillis))
    }

    /**
     * Format sleep duration in minutes to a readable string.
     */
    fun formatDuration(durationMinutes: Int): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        return "$hours hr $minutes min"
    }

    /**
     * Clear the status message after it has been consumed.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}