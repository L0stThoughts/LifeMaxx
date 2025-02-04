package com.example.lifemaxx.util

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utility object for initializing and accessing Firebase components.
 */
object FirebaseUtils {
    private const val TAG = "FirebaseUtils"

    // Firebase Firestore instance
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    // Firebase Auth instance
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    /**
     * Initializes Firebase, typically called in Application.onCreate().
     */
    fun initializeFirebase(appContext: android.content.Context) {
        FirebaseApp.initializeApp(appContext)
        Log.d(TAG, "Firebase Initialized")
    }
}
