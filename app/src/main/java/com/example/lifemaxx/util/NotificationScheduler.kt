package com.example.lifemaxx.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.lifemaxx.ui.ReminderReceiver
import java.util.*

object NotificationScheduler {

    /**
     * Schedules a single reminder at [timeInMillis], with a [message].
     * If you need multiple, just call it multiple times with different times and messages.
     *
     * [notificationId] should be unique per reminder, so they're not overridden.
     */
    fun scheduleReminder(
        context: Context,
        timeInMillis: Long,
        notificationId: Int,
        message: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Build the Intent with custom extras
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("NOTIF_MESSAGE", message)
            putExtra("NOTIF_ID", notificationId)
        }

        // Use distinct requestCode = notificationId to avoid collisions
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId, // requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // For Android S+ (API 31+), check SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Prompt user to grant schedule exact alarms
                redirectToAlarmPermissionSettings(context)
                // or fallback to inexact repeating
            }
        }

        // Alarm setExact for an exact time
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }

    /**
     * If on Android S+ we need special permission for exact alarms.
     */
    private fun redirectToAlarmPermissionSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Cancel a previously scheduled reminder if needed.
     */
    fun cancelReminder(context: Context, notificationId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
