package com.example.lifemaxx.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.lifemaxx.R

object NotificationUtils {
    private const val CHANNEL_ID = "lifeMaxx_channel"
    private const val CHANNEL_NAME = "Supplement Reminders"
    private const val CHANNEL_DESCRIPTION = "Reminders for taking supplements"

    /**
     * Creates a notification channel on Android 8.0+.
     * Call this once (e.g., in Application.onCreate()) or before scheduling reminders.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a notification with the given message. Also checks POST_NOTIFICATIONS permission on Android 13+.
     */
    fun showNotification(
        context: Context,
        title: String = "Reminder!",
        message: String = "Don't forget to take your supplements today!",
        notificationId: Int
    ) {
        // For Android 13+ (TIRAMISU), check runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted; can't show notification
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // adjust the icon resource
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
