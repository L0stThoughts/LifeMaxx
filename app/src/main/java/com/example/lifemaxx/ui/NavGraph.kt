package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("medicalStudyFinder") {
            MedicalStudyFinderScreen()
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        composable("supplementList") {
            SupplementListScreen()
        }


    }
}
