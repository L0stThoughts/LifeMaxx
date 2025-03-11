package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.User
import com.example.lifemaxx.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user settings.
 */
class SettingsViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {
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
                val user = repository.getUser(userId)

                // If user doesn't exist yet, create default settings
                if (user == null) {
                    val defaultUser = User(
                        id = userId,
                        name = "User",
                        email = "",
                        notificationEnabled = true,
                        preferredDoseTime = "08:00"
                    )
                    repository.addUser(defaultUser)
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
                val success = repository.updateUser(userId, updatedData)
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