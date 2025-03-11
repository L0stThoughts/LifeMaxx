package com.example.lifemaxx.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.lifemaxx.ui.theme.LifeMaxxTheme
import com.example.lifemaxx.ui.theme.SparkTopAppBar
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.util.NotificationManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission launcher
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val status = if (isGranted) "granted" else "denied"
            Log.d(TAG, "Notification permission $status")
        }

        // Initialize Firebase first - do this synchronously to ensure it's ready
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                Log.d(TAG, "Firebase already initialized")
            } else {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase initialized manually")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }

        // Create notification channel
        try {
            NotificationManager.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel: ${e.message}", e)
        }

        // Request notifications permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManager.checkNotificationPermission(this)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Set content with a simpler, time-based splash screen
        setContent {
            SimpleSplashScreen()
        }
    }

    @Composable
    fun SimpleSplashScreen() {
        var showSplash by remember { mutableStateOf(true) }

        // Auto-dismiss splash screen after 2 seconds
        LaunchedEffect(Unit) {
            delay(2000)
            showSplash = false
        }

        if (showSplash) {
            // Splash screen content
            LifeMaxxTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "LifeMaxx",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Initializing...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // Main app content
            LifeMaxxApp()
        }
    }

    @Composable
    fun LifeMaxxApp() {
        val navController = rememberNavController()

        LifeMaxxTheme {
            Scaffold(
                topBar = {
                    SparkTopAppBar()
                }
            ) { innerPadding ->
                NavGraph(navController = navController, paddingValues = innerPadding)
            }
        }
    }
}