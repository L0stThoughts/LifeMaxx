package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController, paddingValues: androidx.compose.foundation.layout.PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.padding(paddingValues) // Apply Scaffold padding
    ) {
        composable("home") { HomeScreen(navController) }
        composable("doseTracker") { DoseTrackerScreen(navController) }
        composable("medicalStudyFinder") { MedicalStudyFinderScreen() }
        composable("settings") { SettingsScreen(navController) }
        composable("supplementTracking") { SupplementTrackingScreen(navController) }
    }
}
