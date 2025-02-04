package com.example.lifemaxx.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.lifemaxx.ui.theme.LifeMaxxTheme
import com.example.lifemaxx.ui.theme.SparkTitle
import com.example.lifemaxx.ui.theme.SparkTopAppBar
import com.example.lifemaxx.util.NotificationUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create or update the notification channel
        NotificationUtils.createNotificationChannel(this)

        setContent {
            LifeMaxxApp()
        }
    }
}

@Composable
fun LifeMaxxApp() {
    val navController = rememberNavController()

    // Wrap everything in our custom theme
    LifeMaxxTheme {
        // Use SparkTopAppBar() as the top bar
        Scaffold(
            topBar = {
                SparkTopAppBar()
            }
        ) { innerPadding ->
            NavGraph(navController = navController, paddingValues = innerPadding)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LifeMaxxApp()
}

