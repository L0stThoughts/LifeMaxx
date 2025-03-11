package com.example.lifemaxx

import android.app.Application
import android.util.Log
import com.example.lifemaxx.repository.*
import com.example.lifemaxx.viewmodel.*
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.util.NotificationManager
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LifeMaxxApp : Application() {
    private val TAG = "LifeMaxxApp"

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase using our utility
        try {
            val success = FirebaseUtils.initializeFirebase(this)
            if (success) {
                Log.d(TAG, "Firebase initialized successfully")
            } else {
                Log.e(TAG, "Firebase initialization returned false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
        }

        // Create notification channel
        try {
            NotificationManager.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel: ${e.message}", e)
        }

        // Start Koin for dependency injection
        try {
            startKoin { modules(appModule) }
            Log.d(TAG, "Koin initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Koin: ${e.message}", e)
        }
    }
}

// Koin module for DI
val appModule = module {
    // 1) Provide repositories as singletons
    single { DoseRepository() }
    single { SupplementRepository() }
    single { UserRepository() }
    single { NutritionRepository(get()) }
    single { SleepRepository() }
    single { WaterIntakeRepository() }
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