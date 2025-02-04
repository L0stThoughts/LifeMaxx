package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = koinViewModel()
    val userSettings by viewModel.userSettings.collectAsState()

    // Example user ID
    val currentUserId = "user123"

    // Fetch on load
    LaunchedEffect(Unit) {
        viewModel.fetchUserSettings(currentUserId)
    }

    // Local switch state
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("App Settings", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            // Toggle Notifications
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Notifications", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        viewModel.updateUserSettings(currentUserId, mapOf("notificationEnabled" to it))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Preferred Dose Time: ${userSettings?.preferredDoseTime ?: "08:00"}",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
        }
    }
}
