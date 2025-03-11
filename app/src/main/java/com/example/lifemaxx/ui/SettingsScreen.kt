package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.util.NotificationManager
import com.example.lifemaxx.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = koinViewModel()
    val userSettings by viewModel.userSettings.collectAsState()
    val context = LocalContext.current

    // Local states for UI
    var notificationsEnabled by remember { mutableStateOf(true) }
    var useDarkTheme by remember { mutableStateOf(false) }
    var useLargeText by remember { mutableStateOf(false) }
    var showDataClearDialog by remember { mutableStateOf(false) }

    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when status message changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    // Example user ID - in a real app, get this from authentication
    val currentUserId = "user123"

    // Fetch user settings on load
    LaunchedEffect(Unit) {
        viewModel.fetchUserSettings(currentUserId)
    }

    // Update local states when userSettings changes
    LaunchedEffect(userSettings) {
        userSettings?.let { settings ->
            notificationsEnabled = settings.notificationEnabled
            // Other settings would be loaded here
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App info card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LifeMaxx",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your personal supplement and nutrition tracker",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Notification settings
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    SettingsSectionHeader(
                        icon = Icons.Default.Notifications,
                        title = "Notification Settings"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle for enabling/disabling notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Notifications")
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { isEnabled ->
                                notificationsEnabled = isEnabled

                                // Check for permission if enabling
                                if (isEnabled && !NotificationManager.checkNotificationPermission(context)) {
                                    viewModel.setStatusMessage("Please grant notification permission in system settings")
                                } else {
                                    // Update the setting
                                    viewModel.updateUserSettings(
                                        currentUserId,
                                        mapOf("notificationEnabled" to isEnabled)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Appearance settings
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    SettingsSectionHeader(
                        icon = Icons.Default.Palette,
                        title = "Appearance"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dark theme toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Theme")
                        Switch(
                            checked = useDarkTheme,
                            onCheckedChange = {
                                useDarkTheme = it
                                viewModel.setStatusMessage("Theme changes will apply on app restart")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Large text toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Larger Text")
                        Switch(
                            checked = useLargeText,
                            onCheckedChange = {
                                useLargeText = it
                                viewModel.setStatusMessage("Text size changes will apply on app restart")
                            }
                        )
                    }
                }
            }

            // Data management settings
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    SettingsSectionHeader(
                        icon = Icons.Default.Storage,
                        title = "Data Management"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Backup data button
                    OutlinedButton(
                        onClick = { viewModel.setStatusMessage("Backup feature coming soon") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup Data")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Import data button
                    OutlinedButton(
                        onClick = { viewModel.setStatusMessage("Import feature coming soon") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Data")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clear data button
                    Button(
                        onClick = { showDataClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Data")
                    }
                }
            }

            // About section
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    SettingsSectionHeader(
                        icon = Icons.Default.Help,
                        title = "About & Help"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.setStatusMessage("Visit website feature coming soon") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visit Website")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.setStatusMessage("Send feedback feature coming soon") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Feedback, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Feedback")
                    }
                }
            }
        }

        // Show data clear confirmation dialog
        if (showDataClearDialog) {
            AlertDialog(
                onDismissRequest = { showDataClearDialog = false },
                title = { Text("Clear All Data?") },
                text = {
                    Text("This will permanently delete all your supplements, nutrition data, and settings. This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDataClearDialog = false
                            viewModel.setStatusMessage("All data cleared successfully")
                            // In a real app, you would implement actual data clearing here
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDataClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
    }
}