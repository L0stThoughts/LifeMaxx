package com.example.lifemaxx.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController

@Composable
fun SupplementTrackingScreen(navController: NavController) {
    var supplements by remember { mutableStateOf(listOf("Vitamin C", "Iron")) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Supplement Tracking") }) }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            Text("Your Supplements", style = MaterialTheme.typography.h5)
            supplements.forEach { supplement ->
                Text("- $supplement", style = MaterialTheme.typography.body1)
            }
        }
    }
}

