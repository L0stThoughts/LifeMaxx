package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        // Use failsafe screens instead of the original ones
        composable("supplementList") {
            FailsafeSupplementScreen(navController)
        }
        composable("doseTracker") {
            FailsafeDoseTrackerScreen(navController)
        }
        // Regular screens for other features
        composable("reminders") {
            RemindersScreen(navController)
        }
        composable("nutritionTracker") {
            NutritionTrackerScreen(navController)
        }
        composable("sleepTracker") {
            SleepTrackerScreen(navController)
        }
        composable("waterTracker") {
            WaterIntakeScreen(navController)
        }
        composable("barcodeScanner") {
            BarcodeScannerScreen(navController)
        }
    }
}