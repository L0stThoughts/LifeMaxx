package com.example.lifemaxx.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
    ) { paddingValues ->  // Add the padding parameter
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) { // Apply additional padding
            Text("Welcome to LifeMaxx", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("doseTracker") }) {
                Text("Go to Dose Tracker")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("medicalStudyFinder") }) {
                Text("Go to Medical Study Finder")
            }
            Button(onClick = { navController.navigate("supplementTracking") }) {
                Text("Go to Supplement Tracking")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { navController.navigate("settings") }) {
                Text("Go to Settings")
            }
        }
    }
}

