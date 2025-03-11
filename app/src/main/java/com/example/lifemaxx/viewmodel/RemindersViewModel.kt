package com.example.lifemaxx.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Reminder
import com.example.lifemaxx.util.NotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Reminder objects with persistence in Firestore.
 */
class RemindersViewModel : ViewModel() {
    private val TAG = "RemindersViewModel"

    // Our list of reminders
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> get() = _reminders

    // Status message for operation feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    init {
        loadReminders()
    }

    /**
     * Load reminders from Firestore
     */
    private fun loadReminders() {
        viewModelScope.launch {
            try {
                val loadedReminders = NotificationManager.loadRemindersFromFirestore()
                _reminders.value = loadedReminders
                Log.d(TAG, "Loaded ${loadedReminders.size} reminders")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reminders: ${e.message}", e)
                _statusMessage.value = "Failed to load reminders"
            }
        }
    }

    /**
     * Adds a new reminder and schedules it if enabled
     */
    fun addReminder(reminder: Reminder, context: Context) {
        viewModelScope.launch {
            try {
                // First update the UI with optimistic update
                val updatedList = _reminders.value.toMutableList()
                updatedList.add(reminder)
                _reminders.value = updatedList

                // Schedule the reminder
                if (reminder.isEnabled) {
                    val success = NotificationManager.scheduleReminder(context, reminder)
                    if (success) {
                        _statusMessage.value = "Reminder scheduled"
                    } else {
                        _statusMessage.value = "Failed to schedule reminder"
                    }
                } else {
                    _statusMessage.value = "Reminder added (not enabled)"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding reminder: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Update an existing reminder
     */
    fun updateReminder(
        context: Context,
        id: Int,
        newTimeInMillis: Long,
        newMessage: String,
        newIsEnabled: Boolean
    ) {
        viewModelScope.launch {
            try {
                // First cancel the old reminder
                NotificationManager.cancelReminder(context, id)

                // Create updated reminder
                val updatedReminder = Reminder(
                    id = id,
                    timeInMillis = newTimeInMillis,
                    message = newMessage,
                    isEnabled = newIsEnabled
                )

                // Update the in-memory list
                val list = _reminders.value.toMutableList()
                val index = list.indexOfFirst { it.id == id }
                if (index != -1) {
                    list[index] = updatedReminder
                    _reminders.value = list
                }

                // Schedule if enabled
                if (newIsEnabled) {
                    val success = NotificationManager.scheduleReminder(context, updatedReminder)
                    if (success) {
                        _statusMessage.value = "Reminder updated and scheduled"
                    } else {
                        _statusMessage.value = "Reminder updated but scheduling failed"
                    }
                } else {
                    _statusMessage.value = "Reminder updated (not enabled)"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reminder: ${e.message}", e)
                _statusMessage.value = "Error updating reminder: ${e.message}"
            }
        }
    }

    /**
     * Delete a reminder
     */
    fun deleteReminder(context: Context, reminderId: Int) {
        viewModelScope.launch {
            try {
                // Cancel the reminder first
                val success = NotificationManager.cancelReminder(context, reminderId)

                // Update UI state
                val list = _reminders.value.toMutableList()
                list.removeIf { it.id == reminderId }
                _reminders.value = list

                if (success) {
                    _statusMessage.value = "Reminder deleted"
                } else {
                    _statusMessage.value = "Reminder deleted from list but cancellation failed"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting reminder: ${e.message}", e)
                _statusMessage.value = "Error deleting reminder: ${e.message}"
            }
        }
    }

    /**
     * Toggle a reminder's enabled state
     */
    fun toggleReminder(context: Context, reminderId: Int, newEnabled: Boolean) {
        viewModelScope.launch {
            try {
                // Find the reminder
                val list = _reminders.value.toMutableList()
                val index = list.indexOfFirst { it.id == reminderId }
                if (index == -1) return@launch

                val oldReminder = list[index]

                // Cancel existing if it was enabled
                if (oldReminder.isEnabled) {
                    NotificationManager.cancelReminder(context, reminderId)
                }

                // Create updated reminder
                val updatedReminder = oldReminder.copy(isEnabled = newEnabled)
                list[index] = updatedReminder
                _reminders.value = list

                // Schedule if newly enabled
                if (newEnabled) {
                    val success = NotificationManager.scheduleReminder(context, updatedReminder)
                    if (success) {
                        _statusMessage.value = "Reminder enabled and scheduled"
                    } else {
                        _statusMessage.value = "Failed to schedule reminder"
                    }
                } else {
                    _statusMessage.value = "Reminder disabled"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling reminder: ${e.message}", e)
                _statusMessage.value = "Error toggling reminder: ${e.message}"
            }
        }
    }

    /**
     * Clear the status message after consumption
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}