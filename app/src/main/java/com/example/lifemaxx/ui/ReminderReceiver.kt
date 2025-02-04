package com.example.lifemaxx.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifemaxx.util.NotificationUtils

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        val customMessage = intent?.getStringExtra("NOTIF_MESSAGE")
            ?: "Don't forget to take your supplements today!"

        val notifId = intent?.getIntExtra("NOTIF_ID", 0) ?: 0
        val notifTitle = "Reminder!"

        // Show the notification
        NotificationUtils.showNotification(
            context = context,
            title = notifTitle,
            message = customMessage,
            notificationId = notifId
        )
    }
}
