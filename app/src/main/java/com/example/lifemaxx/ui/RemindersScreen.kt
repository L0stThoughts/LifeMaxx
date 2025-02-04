package com.example.lifemaxx.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.Reminder
import com.example.lifemaxx.viewmodel.RemindersViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(navController: NavController) {
    val viewModel: RemindersViewModel = koinViewModel()
    val reminders by viewModel.reminders.collectAsState()

    // Adding new
    var isAddDialogOpen by remember { mutableStateOf(false) }
    // Editing existing
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reminders") })
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            val context = LocalContext.current

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onToggle = { newEnabled ->
                            viewModel.toggleReminder(context, reminder.id, newEnabled)
                        },
                        onClick = {
                            editingReminder = reminder
                        },
                        onDelete = {
                            viewModel.deleteReminder(context, reminder.id)
                        }
                    )
                }
            }

            // Floating button
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }

            // Add Dialog
            if (isAddDialogOpen) {
                val context = LocalContext.current
                AddReminderDialog(
                    onDismiss = { isAddDialogOpen = false },
                    onConfirm = { timeMillis, msg ->
                        val newId = (100000..999999).random() // or real ID from DB
                        val reminder = Reminder(
                            id = newId,
                            timeInMillis = timeMillis,
                            message = msg,
                            isEnabled = true
                        )
                        viewModel.addReminder(reminder, context)
                        isAddDialogOpen = false
                    }
                )
            }

            // Edit Dialog
            editingReminder?.let { old ->
                val context = LocalContext.current
                EditReminderDialog(
                    oldReminder = old,
                    onDismiss = { editingReminder = null },
                    onConfirm = { newTime, newMsg ->
                        viewModel.updateReminder(
                            context,
                            old.id,
                            newTime,
                            newMsg,
                            old.isEnabled
                        )
                        editingReminder = null
                    }
                )
            }
        }
    }
}

/**
 * A single row for the reminder. Tappable for editing, plus a toggle switch and delete icon.
 */
@Composable
fun ReminderItem(
    reminder: Reminder,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Time: ${formatTime(reminder.timeInMillis)}")
                Text("Message: ${reminder.message}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Reminder")
                }
            }
        }
    }
}

@Composable
fun formatTime(timeInMillis: Long): String {
    val cal = Calendar.getInstance().apply { setTimeInMillis(timeInMillis) }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    return String.format("%02d:%02d", h, m)
}

/**
 * Dialog to add a new reminder. User picks hour/minute and a message.
 */
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("Time to take your supplements!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hour.toString(),
                    onValueChange = { hour = it.toIntOrNull() ?: 8 },
                    label = { Text("Hour (0-23)") }
                )
                OutlinedTextField(
                    value = minute.toString(),
                    onValueChange = { minute = it.toIntOrNull() ?: 0 },
                    label = { Text("Minute (0-59)") }
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                onConfirm(cal.timeInMillis, message)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to edit an existing reminder's time and message.
 */
@Composable
fun EditReminderDialog(
    oldReminder: Reminder,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = oldReminder.timeInMillis }
    val initialHour = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)

    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var message by remember { mutableStateOf(oldReminder.message) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hour.toString(),
                    onValueChange = { hour = it.toIntOrNull() ?: initialHour },
                    label = { Text("Hour (0-23)") }
                )
                OutlinedTextField(
                    value = minute.toString(),
                    onValueChange = { minute = it.toIntOrNull() ?: initialMinute },
                    label = { Text("Minute (0-59)") }
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                onConfirm(newCal.timeInMillis, message)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
