package com.example.lifemaxx.viewmodel

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
    private val _userSettings = MutableStateFlow<User?>(null)
    val userSettings: StateFlow<User?> get() = _userSettings

    /**
     * Fetches the user's settings from Firestore.
     */
    fun fetchUserSettings(userId: String) {
        viewModelScope.launch {
            _userSettings.value = repository.getUser(userId)
        }
    }

    /**
     * Updates the user's settings in Firestore.
     */
    fun updateUserSettings(userId: String, updatedData: Map<String, Any>) {
        viewModelScope.launch {
            repository.updateUser(userId, updatedData)
            fetchUserSettings(userId) // Refresh after update
        }
    }
}
