package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                .padding(16.dp)
        ) {
            Text("Welcome to LifeMaxx", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { navController.navigate("medicalStudyFinder") }) {
                Text("Go to Medical Study Finder")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // "View All Supplements" replaces the old supplementTracking
            Button(onClick = { navController.navigate("supplementList") }) {
                Text("View All Supplements")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { navController.navigate("settings") }) {
                Text("Go to Settings")
            }



        }
    }
}
