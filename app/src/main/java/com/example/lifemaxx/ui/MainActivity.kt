package com.example.lifemaxx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeMaxxApp()
        }
    }
}

@Composable
fun LifeMaxxApp() {
    // Initialize the NavController
    val navController = rememberNavController()

    // Scaffold with the navigation graph
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LifeMaxx") })
        }
    ) { innerPadding ->
        // Pass the NavController to NavGraph
        NavGraph(navController = navController, paddingValues = innerPadding)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LifeMaxxApp()
}
