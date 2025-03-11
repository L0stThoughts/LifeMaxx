package com.example.lifemaxx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.SleepEntry
import com.example.lifemaxx.viewmodel.SleepViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerScreen(navController: NavController) {
    val viewModel: SleepViewModel = koinViewModel()

    val currentDate by viewModel.currentDate.collectAsState()
    val sleepEntries by viewModel.sleepEntries.collectAsState()
    val recentSleepEntries by viewModel.recentSleepEntries.collectAsState()
    val averageSleepDuration by viewModel.averageSleepDuration.collectAsState()
    val averageSleepQuality by viewModel.averageSleepQuality.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val editingSleepEntry by viewModel.editingSleepEntry.collectAsState()

    var showAddSleepDialog by remember { mutableStateOf(false) }

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
                title = { Text("Sleep Tracker") },
                actions = {
                    // Calendar picker for selecting date
                    IconButton(onClick = {
                        // Show date picker (implemented elsewhere)
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSleepDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Sleep Entry")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Sleep Summary Card
                SleepSummaryCard(
                    averageDuration = averageSleepDuration,
                    averageQuality = averageSleepQuality,
                    formatDuration = { viewModel.formatDuration(it.toInt()) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recent Sleep History
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Sleep",
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (recentSleepEntries.isEmpty()) {
                            Text(
                                "No recent sleep data available",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                items(recentSleepEntries) { entry ->
                                    SleepEntryItem(
                                        entry = entry,
                                        formatTime = { viewModel.formatTime(it) },
                                        formatDuration = { viewModel.formatDuration(it) },
                                        onEdit = { viewModel.editSleepEntry(entry) },
                                        onDelete = { viewModel.deleteSleepEntry(entry.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Today's Sleep
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Today's Sleep",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (sleepEntries.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No sleep data for today\nTap + to add your sleep",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn {
                                items(sleepEntries) { entry ->
                                    SleepEntryItem(
                                        entry = entry,
                                        formatTime = { viewModel.formatTime(it) },
                                        formatDuration = { viewModel.formatDuration(it) },
                                        onEdit = { viewModel.editSleepEntry(entry) },
                                        onDelete = { viewModel.deleteSleepEntry(entry.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Sleep Dialog
    if (showAddSleepDialog) {
        AddSleepEntryDialog(
            onDismiss = { showAddSleepDialog = false },
            onConfirm = { sleepTime, wakeTime, quality, notes, deepSleep, remSleep, interruptions ->
                viewModel.addSleepEntry(
                    sleepTime = sleepTime,
                    wakeTime = wakeTime,
                    quality = quality,
                    notes = notes,
                    deepSleepMinutes = deepSleep,
                    remSleepMinutes = remSleep,
                    interruptions = interruptions
                )
                showAddSleepDialog = false
            }
        )
    }

    // Edit Sleep Dialog
    editingSleepEntry?.let { entry ->
        EditSleepEntryDialog(
            entry = entry,
            onDismiss = { viewModel.cancelEditingSleepEntry() },
            onConfirm = { sleepTime, wakeTime, quality, notes, deepSleep, remSleep, interruptions ->
                viewModel.updateSleepEntry(
                    entryId = entry.id,
                    sleepTime = sleepTime,
                    wakeTime = wakeTime,
                    quality = quality,
                    notes = notes,
                    deepSleepMinutes = deepSleep,
                    remSleepMinutes = remSleep,
                    interruptions = interruptions
                )
            }
        )
    }
}

@Composable
fun SleepSummaryCard(
    averageDuration: Double,
    averageQuality: Double,
    formatDuration: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Sleep Summary",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Average Duration
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        formatDuration(averageDuration),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Avg. Duration",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Average Quality
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        String.format("%.1f", averageQuality),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Avg. Quality",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun SleepEntryItem(
    entry: SleepEntry,
    formatTime: (Long) -> String,
    formatDuration: (Int) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getSleepQualityColor(entry.quality))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.quality.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Sleep info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${formatTime(entry.sleepTime)} - ${formatTime(entry.wakeTime)}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    formatDuration(entry.duration),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (entry.notes.isNotEmpty()) {
                    Text(
                        entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

/**
 * Returns a color based on sleep quality.
 */
@Composable
fun getSleepQualityColor(quality: Int): Color {
    return when (quality) {
        1 -> Color.Red
        2 -> Color(0xFFFF6D00) // Orange
        3 -> Color(0xFFFFB300) // Amber
        4 -> Color(0xFF00897B) // Teal
        5 -> Color(0xFF43A047) // Green
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun AddSleepEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        sleepTime: Long,
        wakeTime: Long,
        quality: Int,
        notes: String,
        deepSleep: Int,
        remSleep: Int,
        interruptions: Int
    ) -> Unit
) {
    // Get current time as default
    val calendar = Calendar.getInstance()

    // Default sleep time - 10 PM last night
    val defaultSleepCalendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -1)
        set(Calendar.HOUR_OF_DAY, 22)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Default wake time - 6 AM today
    val defaultWakeCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 6)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var sleepTimeMillis by remember { mutableStateOf(defaultSleepCalendar.timeInMillis) }
    var wakeTimeMillis by remember { mutableStateOf(defaultWakeCalendar.timeInMillis) }
    var quality by remember { mutableStateOf(3) }
    var notes by remember { mutableStateOf("") }
    var deepSleepMinutes by remember { mutableStateOf("0") }
    var remSleepMinutes by remember { mutableStateOf("0") }
    var interruptions by remember { mutableStateOf("0") }

    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sleep Entry") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sleep Time
                Text("Sleep Time")
                Button(
                    onClick = {
                        // Show time picker (simplified implementation)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timeFormatter.format(Date(sleepTimeMillis)))
                }

                // Wake Time
                Text("Wake Time")
                Button(
                    onClick = {
                        // Show time picker (simplified implementation)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timeFormatter.format(Date(wakeTimeMillis)))
                }

                // Sleep Quality
                Text("Sleep Quality")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= quality) getSleepQualityColor(i)
                                    else Color.Gray.copy(alpha = 0.3f)
                                )
                                .clickable { quality = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                i.toString(),
                                color = if (i <= quality) Color.White else Color.Black
                            )
                        }
                    }
                }

                // Sleep Quality Description
                Text(
                    SleepEntry.QUALITY_DESCRIPTIONS[quality] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Advanced fields - Deep Sleep, REM, Interruptions
                Text("Advanced Tracking (Optional)")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = deepSleepMinutes,
                        onValueChange = { deepSleepMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Deep Sleep (min)") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = remSleepMinutes,
                        onValueChange = { remSleepMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("REM Sleep (min)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = interruptions,
                    onValueChange = { interruptions = it.filter { c -> c.isDigit() } },
                    label = { Text("Interruptions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        sleepTimeMillis,
                        wakeTimeMillis,
                        quality,
                        notes,
                        deepSleepMinutes.toIntOrNull() ?: 0,
                        remSleepMinutes.toIntOrNull() ?: 0,
                        interruptions.toIntOrNull() ?: 0
                    )
                }
            ) {
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

@Composable
fun EditSleepEntryDialog(
    entry: SleepEntry,
    onDismiss: () -> Unit,
    onConfirm: (
        sleepTime: Long,
        wakeTime: Long,
        quality: Int,
        notes: String,
        deepSleep: Int,
        remSleep: Int,
        interruptions: Int
    ) -> Unit
) {
    // Initialize values from entry
    var sleepTimeMillis by remember { mutableStateOf(entry.sleepTime) }
    var wakeTimeMillis by remember { mutableStateOf(entry.wakeTime) }
    var quality by remember { mutableStateOf(entry.quality) }
    var notes by remember { mutableStateOf(entry.notes) }
    var deepSleepMinutes by remember { mutableStateOf(entry.deepSleepMinutes.toString()) }
    var remSleepMinutes by remember { mutableStateOf(entry.remSleepMinutes.toString()) }
    var interruptions by remember { mutableStateOf(entry.interruptions.toString()) }

    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sleep Entry") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sleep Time
                Text("Sleep Time")
                Button(
                    onClick = {
                        // Show time picker (simplified implementation)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timeFormatter.format(Date(sleepTimeMillis)))
                }

                // Wake Time
                Text("Wake Time")
                Button(
                    onClick = {
                        // Show time picker (simplified implementation)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timeFormatter.format(Date(wakeTimeMillis)))
                }

                // Sleep Quality
                Text("Sleep Quality")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= quality) getSleepQualityColor(i)
                                    else Color.Gray.copy(alpha = 0.3f)
                                )
                                .clickable { quality = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                i.toString(),
                                color = if (i <= quality) Color.White else Color.Black
                            )
                        }
                    }
                }

                // Sleep Quality Description
                Text(
                    SleepEntry.QUALITY_DESCRIPTIONS[quality] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Advanced fields - Deep Sleep, REM, Interruptions
                Text("Advanced Tracking (Optional)")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = deepSleepMinutes,
                        onValueChange = { deepSleepMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Deep Sleep (min)") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = remSleepMinutes,
                        onValueChange = { remSleepMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("REM Sleep (min)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = interruptions,
                    onValueChange = { interruptions = it.filter { c -> c.isDigit() } },
                    label = { Text("Interruptions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        sleepTimeMillis,
                        wakeTimeMillis,
                        quality,
                        notes,
                        deepSleepMinutes.toIntOrNull() ?: 0,
                        remSleepMinutes.toIntOrNull() ?: 0,
                        interruptions.toIntOrNull() ?: 0
                    )
                }
            ) {
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