package com.example.lifemaxx

import android.app.Application
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.lifemaxx.repository.*
import com.example.lifemaxx.viewmodel.*
import com.example.lifemaxx.util.FirebaseFailsafeUtil
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.util.NotificationManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LifeMaxxApp : Application() {
    private val TAG = "LifeMaxxApp"

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase using our traditional utility synchronously
        try {
            val success = FirebaseUtils.initializeFirebase(this)
            if (success) {
                Log.d(TAG, "Firebase initialized successfully")
            } else {
                Log.e(TAG, "Firebase initialization returned false")
                // Mark as offline mode if traditional init fails
                FirebaseUtils.setOfflineMode(this, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            // Mark as offline mode if any errors occur
            FirebaseUtils.setOfflineMode(this, true)
        }

        // Then initialize the failsafe utility synchronously with the non-suspend version
        try {
            val initSuccess = FirebaseFailsafeUtil.initializeFirebaseSync(this)
            Log.d(TAG, "Firebase failsafe utility initialized: $initSuccess")

            // Schedule async sync of any pending operations
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val syncCount = FirebaseFailsafeUtil.syncPendingOperations(this@LifeMaxxApp)
                    Log.d(TAG, "Synced $syncCount pending operations")
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing pending operations: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase failsafe: ${e.message}", e)
        }

        // Create notification channel
        try {
            NotificationManager.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel: ${e.message}", e)
        }

        // Start Koin for dependency injection with context
        try {
            startKoin {
                androidContext(this@LifeMaxxApp)
                modules(appModule)
            }
            Log.d(TAG, "Koin initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Koin: ${e.message}", e)
        }
    }
}

// Koin module for DI
val appModule = module {
    // 1) Provide repositories as singletons with context
    single { DoseRepository(get()) }
    single { SupplementRepository(get()) }
    single { UserRepository() }
    single { NutritionRepository(get()) }
    single { SleepRepository() }
    single { WaterIntakeRepository(get()) }
    single { BarcodeRepository(get()) } // Pass SupplementRepository to BarcodeRepository

    // 2) Provide ViewModels
    factory { DoseTrackerViewModel(get()) }
    factory { SupplementViewModel(get()) }
    factory { SettingsViewModel(get()) }
    factory { RemindersViewModel() }
    factory { NutritionViewModel(get()) }
    factory { SleepViewModel(get()) }
    factory { WaterIntakeViewModel(get()) }
    factory { BarcodeScannerViewModel(get(), get()) } // Needs both BarcodeRepository and SupplementRepository
}