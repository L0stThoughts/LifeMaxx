package com.example.lifemaxx.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lifemaxx.util.NotificationManager

class ReminderReceiver : BroadcastReceiver() {
    private val TAG = "ReminderReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        Log.d(TAG, "Received reminder broadcast")

        val customMessage = intent?.getStringExtra("NOTIF_MESSAGE")
            ?: "Don't forget to take your supplements today!"

        val notifId = intent?.getIntExtra("NOTIF_ID", 0) ?: 0
        val notifTitle = "LifeMaxx Reminder"

        // Show the notification using our unified manager
        NotificationManager.showNotification(
            context = context,
            title = notifTitle,
            message = customMessage,
            notificationId = notifId
        )
    }
}