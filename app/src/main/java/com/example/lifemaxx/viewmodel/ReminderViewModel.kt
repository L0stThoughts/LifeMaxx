package com.example.lifemaxx.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.lifemaxx.model.Reminder
import com.example.lifemaxx.util.ReminderScheduler
import java.util.*

class ReminderViewModel : ViewModel() {
    fun scheduleReminder(context: Context, reminder: Reminder) {
        val timeParts = reminder.reminderTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        ReminderScheduler.scheduleReminder(
            context = context,
            timeInMillis = calendar.timeInMillis,
            requestCode = reminder.hashCode(),
            receiverClass = com.example.lifemaxx.ui.ReminderReceiver::class.java
        )
    }
}
