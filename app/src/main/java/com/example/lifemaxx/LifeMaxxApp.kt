package com.example.lifemaxx

import android.app.Application
import com.example.lifemaxx.repository.*
import com.example.lifemaxx.viewmodel.*
import com.example.lifemaxx.util.FirebaseUtils
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LifemaxxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseUtils.initializeFirebase(this)
        startKoin { modules(appModule) }
    }
}

// Example Koin module for DI
val appModule = module {
    // 1) Provide repositories as singletons
    single { DoseRepository() }
    single { MedicalStudyRepository() }
    single { SupplementRepository() }
    single { UserRepository() }

    // 2) Provide ViewModels
    // Use 'factory' if you want a new instance each time it's injected,
    // or 'single' if you want a shared instance across the app.
    factory { DoseTrackerViewModel(get()) }             // gets DoseRepository
    factory { MedicalStudyFinderViewModel(get()) }      // gets MedicalStudyRepository
    factory { SupplementViewModel(get()) }              // gets SupplementRepository
    factory { SettingsViewModel(get()) }                // gets UserRepository
    factory { ReminderViewModel() }                     // doesn't need a repo
}
