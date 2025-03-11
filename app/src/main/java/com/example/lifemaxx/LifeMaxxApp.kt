package com.example.lifemaxx

import android.app.Application
import android.util.Log
import com.example.lifemaxx.repository.*
import com.example.lifemaxx.viewmodel.*
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.util.NotificationManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LifeMaxxApp : Application() {
    private val TAG = "LifeMaxxApp"

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase first, synchronously, and with proper error handling
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                Log.d(TAG, "Firebase already initialized")
            } else {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase initialized successfully")
            }

            // Configure Firestore settings
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)  // Enable offline cache
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings

            // Initialize collections
            ensureCollectionsExist()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
        }

        // Then initialize other components
        NotificationManager.createNotificationChannel(this)
        startKoin { modules(appModule) }
    }

    private fun ensureCollectionsExist() {
        try {
            val db = FirebaseFirestore.getInstance()
            val collections = listOf("supplements", "doses", "users", "reminderSettings")

            for (collection in collections) {
                db.collection(collection)
                    .document("placeholder")
                    .set(mapOf("initialized" to true))
                    .addOnSuccessListener {
                        Log.d(TAG, "Collection initialized: $collection")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to initialize collection $collection: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ensureCollectionsExist: ${e.message}", e)
        }
    }
}

// Koin module for DI
val appModule = module {
    // 1) Provide repositories as singletons
    single { DoseRepository() }
    single { SupplementRepository() }
    single { UserRepository() }
    single { NutritionRepository() }
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