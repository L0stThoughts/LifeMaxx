package com.example.lifemaxx.util

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.lifemaxx.ui.ReminderReceiver

object ReminderScheduler {

    fun scheduleReminder(
        context: Context,
        timeInMillis: Long,
        requestCode: Int,
        receiverClass: Class<*> // Add this parameter
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check if the app has SCHEDULE_EXACT_ALARM permission
            if (!alarmManager.canScheduleExactAlarms()) {
                // Redirect the user to grant the permission
                redirectToAlarmPermissionSettings(context)
                return
            }
        }

        // Schedule the exact alarm
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun redirectToAlarmPermissionSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        }
        context.startActivity(intent)
    }

    fun cancelReminder(
        context: Context,
        requestCode: Int,
        receiverClass: Class<*> // Add this parameter here too
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, receiverClass)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
