package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to LifeMaxx",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Supplements section
            Text(
                "Supplements",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Supplements Card
                FeatureCard(
                    icon = Icons.Default.Medication,
                    title = "Supplements",
                    description = "Manage your supplements",
                    onClick = { navController.navigate("supplementList") },
                    modifier = Modifier.weight(1f)
                )

                // Dose Tracker Card
                FeatureCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Dose Tracker",
                    description = "Track your daily doses",
                    onClick = { navController.navigate("doseTracker") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barcode Scanner Button
            Button(
                onClick = { navController.navigate("barcodeScanner") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Supplement Barcode")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Health Tracking section
            Text(
                "Health Tracking",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nutrition Tracker Card
                FeatureCard(
                    icon = Icons.Default.RestaurantMenu,
                    title = "Nutrition",
                    description = "Track calories and macros",
                    onClick = { navController.navigate("nutritionTracker") },
                    modifier = Modifier.weight(1f)
                )

                // Water Intake Card
                FeatureCard(
                    icon = Icons.Default.WaterDrop,
                    title = "Water",
                    description = "Track water intake",
                    onClick = { navController.navigate("waterTracker") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sleep Tracker Card
                FeatureCard(
                    icon = Icons.Default.Nightlight,
                    title = "Sleep",
                    description = "Track sleep quality",
                    onClick = { navController.navigate("sleepTracker") },
                    modifier = Modifier.weight(1f)
                )

                // Reminders Card
                FeatureCard(
                    icon = Icons.Default.Notifications,
                    title = "Reminders",
                    description = "Set reminders",
                    onClick = { navController.navigate("reminders") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings button
            OutlinedButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings")
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}