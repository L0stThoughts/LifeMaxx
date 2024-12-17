package com.example.lifemaxx.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifemaxx.util.NotificationUtils

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            NotificationUtils.showNotification(
                context,
                title = "Reminder!",
                message = "It's time to take your supplement.",
                notificationId = 1
            )
        }
    }
}
