package com.example.lifemaxx.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.lifemaxx.R
import com.example.lifemaxx.model.Reminder
import com.example.lifemaxx.ui.ReminderReceiver
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * A consolidated utility for managing notifications and reminders.
 * Handles creating channels, scheduling, permissions, and storage.
 */
object NotificationManager {
    private const val TAG = "NotificationManager"
    private const val CHANNEL_ID = "lifemaxx_reminders"
    private const val CHANNEL_NAME = "LifeMaxx Reminders"
    private const val CHANNEL_DESCRIPTION = "Reminders for taking supplements and other activities"

    // Firebase collection for persistent storage of reminders
    private val db = FirebaseFirestore.getInstance()
    private val reminderCollection = db.collection("reminders")

    /**
     * Creates the notification channel. Call once at app startup.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    /**
     * Shows a notification immediately.
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        if (checkNotificationPermission(context)) {
            try {
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, builder.build())
                }
                Log.d(TAG, "Notification shown: $title - $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Notification permission not granted")
        }
    }

    /**
     * Checks if notification permission is granted.
     */
    fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not needed for Android < 13
        }
    }

    /**
     * Request notification permission. Call from an Activity.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationPermission(activity: AppCompatActivity): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "Notification permission granted: $isGranted")
        }
    }

    /**
     * Checks if exact alarm permission is granted (Android S+).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Permission not needed for Android < 12
        }
    }

    /**
     * Opens system settings to request exact alarm permission.
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Schedules a reminder notification at the specified time.
     */
    fun scheduleReminder(
        context: Context,
        reminder: Reminder
    ): Boolean {
        return try {
            if (!canScheduleExactAlarms(context)) {
                Log.w(TAG, "Cannot schedule exact alarms")
                return false
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("NOTIF_MESSAGE", reminder.message)
                putExtra("NOTIF_ID", reminder.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.timeInMillis,
                pendingIntent
            )

            // Save to Firestore for persistence
            saveReminderToFirestore(reminder)

            Log.d(TAG, "Reminder scheduled: ID=${reminder.id}, time=${reminder.timeInMillis}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder: ${e.message}", e)
            false
        }
    }

    /**
     * Cancels a scheduled reminder.
     */
    fun cancelReminder(context: Context, reminderId: Int): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)

            // Also remove from Firestore
            deleteReminderFromFirestore(reminderId)

            Log.d(TAG, "Reminder canceled: ID=$reminderId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling reminder: ${e.message}", e)
            false
        }
    }

    /**
     * Save a reminder to Firestore for persistence.
     */
    private fun saveReminderToFirestore(reminder: Reminder) {
        reminderCollection.document(reminder.id.toString())
            .set(reminder)
            .addOnSuccessListener { Log.d(TAG, "Reminder saved to Firestore: ID=${reminder.id}") }
            .addOnFailureListener { e -> Log.e(TAG, "Error saving reminder: ${e.message}", e) }
    }

    /**
     * Delete a reminder from Firestore.
     */
    private fun deleteReminderFromFirestore(reminderId: Int) {
        reminderCollection.document(reminderId.toString())
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Reminder deleted from Firestore: ID=$reminderId") }
            .addOnFailureListener { e -> Log.e(TAG, "Error deleting reminder: ${e.message}", e) }
    }

    /**
     * Load all reminders from Firestore.
     */
    suspend fun loadRemindersFromFirestore(): List<Reminder> {
        return try {
            val snapshot = reminderCollection.get().await()
            snapshot.toObjects(Reminder::class.java).also {
                Log.d(TAG, "Loaded ${it.size} reminders from Firestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading reminders: ${e.message}", e)
            emptyList()
        }
    }
}