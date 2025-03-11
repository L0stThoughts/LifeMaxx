package com.example.lifemaxx.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.User
import com.example.lifemaxx.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user settings.
 */
class SettingsViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val supplementRepository: SupplementRepository? = null,
    private val doseRepository: DoseRepository? = null,
    private val nutritionRepository: NutritionRepository? = null,
    private val waterIntakeRepository: WaterIntakeRepository? = null,
    private val sleepRepository: SleepRepository? = null
) : ViewModel() {
    private val TAG = "SettingsViewModel"

    private val _userSettings = MutableStateFlow<User?>(null)
    val userSettings: StateFlow<User?> get() = _userSettings

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> get() = _statusMessage

    /**
     * Fetches the user's settings from Firestore.
     */
    fun fetchUserSettings(userId: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUser(userId)

                // If user doesn't exist yet, create default settings
                if (user == null) {
                    val defaultUser = User(
                        id = userId,
                        name = "User",
                        email = "",
                        notificationEnabled = true,
                        preferredDoseTime = "08:00"
                    )
                    userRepository.addUser(defaultUser)
                    _userSettings.value = defaultUser
                    Log.d(TAG, "Created default user settings")
                } else {
                    _userSettings.value = user
                    Log.d(TAG, "Fetched user settings: $user")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user settings: ${e.message}", e)
                _statusMessage.value = "Error loading settings"
            }
        }
    }

    /**
     * Updates the user's settings in Firestore.
     */
    fun updateUserSettings(userId: String, updatedData: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val success = userRepository.updateUser(userId, updatedData)
                if (success) {
                    fetchUserSettings(userId) // Refresh after update
                    _statusMessage.value = "Settings updated successfully"
                } else {
                    _statusMessage.value = "Failed to update settings"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user settings: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Sync all repository data with Firestore
     */
    fun syncAllData(context: Context) {
        viewModelScope.launch {
            var syncCount = 0
            _statusMessage.value = "Syncing data..."

            try {
                // Try to sync each repository
                supplementRepository?.let {
                    try {
                        val count = it.syncLocalData()
                        if (count > 0) syncCount += count
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing supplements: ${e.message}", e)
                    }
                }

                doseRepository?.let {
                    try {
                        val count = it.syncLocalData()
                        if (count > 0) syncCount += count
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing doses: ${e.message}", e)
                    }
                }

                nutritionRepository?.let {
                    try {
                        val count = it.syncLocalData()
                        if (count is Int && count > 0) syncCount += count
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing nutrition: ${e.message}", e)
                    }
                }

                waterIntakeRepository?.let {
                    try {
                        val count = it.syncPendingOperations()
                        if (count > 0) syncCount += count
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing water intake: ${e.message}", e)
                    }
                }

                // Show result
                if (syncCount > 0) {
                    _statusMessage.value = "Synced $syncCount items with cloud"
                } else {
                    _statusMessage.value = "No items needed syncing"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during data sync: ${e.message}", e)
                _statusMessage.value = "Error during sync: ${e.message}"
            }
        }
    }

    /**
     * Sets a status message to be displayed to the user.
     */
    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }

    /**
     * Clears the status message after it has been consumed.
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}