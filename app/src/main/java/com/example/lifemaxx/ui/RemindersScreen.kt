package com.example.lifemaxx.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.Reminder
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.viewmodel.RemindersViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(navController: NavController) {
    val context = LocalContext.current

    // Initialize Firebase first
    LaunchedEffect(Unit) {
        FirebaseUtils.initializeFirebase(context)
    }

    val viewModel: RemindersViewModel = koinViewModel()
    val reminders by viewModel.reminders.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()

    // Adding new
    var isAddDialogOpen by remember { mutableStateOf(false) }
    // Editing existing
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    // For delete confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }

    // SnackBar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when status message changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                actions = {
                    IconButton(onClick = { viewModel.refreshReminders() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (reminders.isEmpty()) {
                // Empty state
                EmptyRemindersState()
            } else {
                // Reminders list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            onToggle = { newEnabled ->
                                scope.launch {
                                    viewModel.toggleReminder(context, reminder.id, newEnabled)
                                }
                            },
                            onClick = {
                                editingReminder = reminder
                            },
                            onDelete = {
                                reminderToDelete = reminder
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            // Add Dialog
            if (isAddDialogOpen) {
                ImprovedAddReminderDialog(
                    onDismiss = { isAddDialogOpen = false },
                    onConfirm = { timeMillis, msg ->
                        val newId = (100000..999999).random() // or real ID from DB
                        val reminder = Reminder(
                            id = newId,
                            timeInMillis = timeMillis,
                            message = msg,
                            isEnabled = true
                        )
                        scope.launch {
                            viewModel.addReminder(reminder, context)
                        }
                        isAddDialogOpen = false
                    }
                )
            }

            // Edit Dialog
            editingReminder?.let { old ->
                ImprovedEditReminderDialog(
                    oldReminder = old,
                    onDismiss = { editingReminder = null },
                    onConfirm = { newTime, newMsg ->
                        scope.launch {
                            viewModel.updateReminder(
                                context,
                                old.id,
                                newTime,
                                newMsg,
                                old.isEnabled
                            )
                        }
                        editingReminder = null
                    }
                )
            }

            // Delete confirmation dialog
            if (showDeleteDialog) {
                reminderToDelete?.let { reminder ->
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false
                            reminderToDelete = null
                        },
                        title = { Text("Delete Reminder") },
                        text = { Text("Are you sure you want to delete this reminder?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.deleteReminder(context, reminder.id)
                                    }
                                    showDeleteDialog = false
                                    reminderToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    reminderToDelete = null
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyRemindersState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No reminders yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap the + button to add your first reminder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Time and Enabled Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        formatTime(reminder.timeInMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    reminder.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
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
    val amPm = if (h < 12) "AM" else "PM"
    val hour12 = if (h == 0) 12 else if (h > 12) h - 12 else h
    return String.format("%d:%02d %s", hour12, m, amPm)
}

/**
 * An improved dialog to add a new reminder with a visual time picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedAddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    // Initial values
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    var hour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var message by remember { mutableStateOf("Time to take your supplements!") }
    var timePickerVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Selection Button
                Button(
                    onClick = { timePickerVisible = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formatHourMinute(hour, minute),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Message field
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Reminder Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Common messages
                Text("Quick Messages:", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { message = "Take your supplements!" },
                        label = { Text("Supplements") }
                    )

                    SuggestionChip(
                        onClick = { message = "Drink some water!" },
                        label = { Text("Water") }
                    )

                    SuggestionChip(
                        onClick = { message = "Log your food!" },
                        label = { Text("Food") }
                    )
                }
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

    // Time Picker Dialog
    if (timePickerVisible) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onTimeSelected = { newHour, newMinute ->
                hour = newHour
                minute = newMinute
                timePickerVisible = false
            },
            onDismiss = { timePickerVisible = false }
        )
    }
}

/**
 * An improved dialog to edit an existing reminder with a visual time picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedEditReminderDialog(
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
    var timePickerVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Reminder") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Selection Button
                Button(
                    onClick = { timePickerVisible = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formatHourMinute(hour, minute),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Message field
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Reminder Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Common messages
                Text("Quick Messages:", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { message = "Take your supplements!" },
                        label = { Text("Supplements") }
                    )

                    SuggestionChip(
                        onClick = { message = "Drink some water!" },
                        label = { Text("Water") }
                    )

                    SuggestionChip(
                        onClick = { message = "Log your food!" },
                        label = { Text("Food") }
                    )
                }
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

    // Time Picker Dialog
    if (timePickerVisible) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onTimeSelected = { newHour, newMinute ->
                hour = newHour
                minute = newMinute
                timePickerVisible = false
            },
            onDismiss = { timePickerVisible = false }
        )
    }
}

@Composable
fun formatHourMinute(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%d:%02d %s", hour12, minute, amPm)
}

/**
 * A visual time picker dialog
 */
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var isPmSelected by remember { mutableStateOf(initialHour >= 12) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time display
                Text(
                    formatHourMinute(selectedHour, selectedMinute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hour picker
                Text("Hour", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.height(8.dp))

                // Hour selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Create a 4x3 grid for hours (1-12)
                    Column {
                        for (row in 0..3) {
                            Row {
                                for (col in 0..2) {
                                    val hourValue = row * 3 + col + 1
                                    val displayHour = if (hourValue == 12) 12 else hourValue

                                    val hour24 = if (isPmSelected) {
                                        if (displayHour == 12) 12 else displayHour + 12
                                    } else {
                                        if (displayHour == 12) 0 else displayHour
                                    }

                                    val isSelected = selectedHour == hour24

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .padding(4.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .clickable {
                                                selectedHour = hour24
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            displayHour.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Minute picker (simplified to common 5-minute intervals)
                Text("Minute", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.height(8.dp))

                // Minute selector (0, 5, 10, ..., 55)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Create a grid for minutes
                    Column {
                        for (row in 0..3) {
                            Row {
                                for (col in 0..2) {
                                    val minute = (row * 3 + col) * 5
                                    val isSelected = selectedMinute == minute

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .padding(4.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .clickable {
                                                selectedMinute = minute
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            String.format("%02d", minute),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AM/PM selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // AM button
                    Button(
                        onClick = {
                            isPmSelected = false
                            // Adjust hour accordingly
                            if (selectedHour >= 12) {
                                selectedHour -= 12
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isPmSelected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isPmSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("AM")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // PM button
                    Button(
                        onClick = {
                            isPmSelected = true
                            // Adjust hour accordingly
                            if (selectedHour < 12) {
                                selectedHour += 12
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPmSelected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isPmSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("PM")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(selectedHour, selectedMinute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}