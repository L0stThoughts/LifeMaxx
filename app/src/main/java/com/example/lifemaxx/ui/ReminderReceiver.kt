package com.example.lifemaxx.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifemaxx.utils.NotificationUtils

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            NotificationUtils.showNotification(
                context,
                "Lifemaxx Reminder",
                "Time to take your supplement!",
                1001
            )
        }
    }
}
