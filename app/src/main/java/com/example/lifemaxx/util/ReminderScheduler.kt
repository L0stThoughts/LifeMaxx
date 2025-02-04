package com.example.lifemaxx.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.example.lifemaxx.ui.ReminderReceiver

/**
 * Schedules or cancels reminders using the system AlarmManager.
 */
object ReminderScheduler {

    /**
     * Schedules an exact alarm at [timeInMillis].
     * If running on Android S+ (API 31+), checks SCHEDULE_EXACT_ALARM permission.
     *
     * @param context       Application context.
     * @param timeInMillis  The exact time to trigger the alarm (in millis).
     * @param requestCode   Unique request code for this alarm.
     * @param supplementName Optional name of the supplement to be included in the reminder.
     */
    fun scheduleReminder(
        context: Context,
        timeInMillis: Long,
        requestCode: Int,
        supplementName: String = "your supplement"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create an Intent with extra data to show in the notification
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("SUPPLEMENT_NAME", supplementName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                redirectToAlarmPermissionSettings(context)
                return
            }
        }

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }

    /**
     * Redirects user to grant SCHEDULE_EXACT_ALARM permission on Android S+.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun redirectToAlarmPermissionSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Cancels a scheduled reminder for the given [requestCode].
     */
    fun cancelReminder(
        context: Context,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
