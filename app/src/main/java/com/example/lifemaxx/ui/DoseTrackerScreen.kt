package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun DoseTrackerScreen(navController: NavController) {
    // Avoid null or invalid states
    var doseCount by remember { mutableStateOf(5) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dose Tracker") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // Use Scaffold padding
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Remaining Doses: $doseCount",
                style = MaterialTheme.typography.h5
            )
            Button(onClick = { if (doseCount > 0) doseCount-- }) {
                Text("Take a Dose")
            }
            Button(onClick = { doseCount++ }) {
                Text("Add Dose")
            }
        }
    }
}
