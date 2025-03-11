package com.example.lifemaxx

import android.app.Application
import android.util.Log
import com.example.lifemaxx.repository.*
import com.example.lifemaxx.viewmodel.*
import com.example.lifemaxx.util.NotificationManager
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LifeMaxxApp : Application() {
    private val TAG = "LifeMaxxApp"

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase - simplest possible approach
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
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
            startKoin {
                androidContext(this@LifeMaxxApp)
                modules(appModule)
            }

            // Set up repository references after Koin is started
            try {
                val barcodeRepo = org.koin.java.KoinJavaComponent.getKoin().get<BarcodeRepository>()
                val supplementRepo = org.koin.java.KoinJavaComponent.getKoin().get<SupplementRepository>()
                barcodeRepo.setSupplementRepository(supplementRepo)
                Log.d(TAG, "Repository references configured")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure repository references: ${e.message}", e)
            }

            Log.d(TAG, "Koin initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Koin: ${e.message}", e)
        }
    }
}

// Koin module for dependency injection
val appModule = module {
    // Repositories - each gets proper context
    single { SupplementRepository(get()) }
    single { DoseRepository(get()) }
    single { UserRepository() }
    single { NutritionRepository(get()) }
    single { SleepRepository() }
    single { WaterIntakeRepository(get()) }

    // BarcodeRepository needs to be created first and then supplementRepository will be set later
    single { BarcodeRepository(get()) }

    // ViewModels
    viewModel { SupplementViewModel(get()) }
    viewModel { DoseTrackerViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { RemindersViewModel() }
    viewModel { NutritionViewModel(get()) }
    viewModel { SleepViewModel(get()) }
    viewModel { WaterIntakeViewModel(get()) }

    // Fix: Pass BarcodeRepository but not SupplementRepository to avoid circular dependency
    viewModel { BarcodeScannerViewModel(get(), get()) }
}