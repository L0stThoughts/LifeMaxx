package com.example.lifemaxx.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Reminder
import com.example.lifemaxx.util.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Holds a list of Reminder objects in-memory for demo.
 * You could store them in a local DB or Firestore if needed.
 */
class RemindersViewModel : ViewModel() {

    // Our list of reminders
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> get() = _reminders

    // e.g., we might load from DB in init, but here is an empty list
    init {
        // loadRemindersIfNeeded()
    }

    /**
     * Adds a new reminder to the list and schedules it if isEnabled == true.
     */
    fun addReminder(reminder: Reminder, context: Context) {
        viewModelScope.launch {
            val list = _reminders.value.toMutableList()
            list.add(reminder)
            _reminders.value = list

            // If it's enabled, schedule
            if (reminder.isEnabled) {
                NotificationScheduler.scheduleReminder(
                    context,
                    reminder.timeInMillis,
                    reminder.id,
                    reminder.message
                )
            }
        }
    }

    /**
     * Update an existing reminder. If old alarm was enabled, cancel it first.
     * Then if new version is enabled, schedule again.
     */
    fun updateReminder(
        context: Context,
        id: Int,
        newTimeInMillis: Long,
        newMessage: String,
        newIsEnabled: Boolean
    ) {
        viewModelScope.launch {
            val list = _reminders.value.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                val oldReminder = list[index]
                // Cancel old alarm
                NotificationScheduler.cancelReminder(context, oldReminder.id)

                val updated = oldReminder.copy(
                    timeInMillis = newTimeInMillis,
                    message = newMessage,
                    isEnabled = newIsEnabled
                )
                list[index] = updated
                _reminders.value = list

                if (updated.isEnabled) {
                    NotificationScheduler.scheduleReminder(
                        context,
                        updated.timeInMillis,
                        updated.id,
                        updated.message
                    )
                }
            }
        }
    }

    /**
     * Delete a reminder entirely.
     */
    fun deleteReminder(context: Context, reminderId: Int) {
        viewModelScope.launch {
            val list = _reminders.value.toMutableList()
            val r = list.find { it.id == reminderId } ?: return@launch

            // If it was enabled, cancel
            if (r.isEnabled) {
                NotificationScheduler.cancelReminder(context, r.id)
            }
            list.remove(r)
            _reminders.value = list
        }
    }

    /**
     * Toggle on/off. If turning off, cancel. If turning on, schedule.
     */
    fun toggleReminder(context: Context, reminderId: Int, newEnabled: Boolean) {
        viewModelScope.launch {
            val list = _reminders.value.toMutableList()
            val index = list.indexOfFirst { it.id == reminderId }
            if (index != -1) {
                val old = list[index]
                // Cancel old
                NotificationScheduler.cancelReminder(context, old.id)

                val updated = old.copy(isEnabled = newEnabled)
                list[index] = updated
                _reminders.value = list

                // If newly enabled, schedule
                if (newEnabled) {
                    NotificationScheduler.scheduleReminder(
                        context,
                        updated.timeInMillis,
                        updated.id,
                        updated.message
                    )
                }
            }
        }
    }
}
