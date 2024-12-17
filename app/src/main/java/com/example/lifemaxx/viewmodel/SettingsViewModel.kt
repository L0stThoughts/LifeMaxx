package com.example.lifemaxx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.User
import com.example.lifemaxx.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {
    private val _userSettings = MutableStateFlow<User?>(null)
    val userSettings: StateFlow<User?> get() = _userSettings

    fun fetchUserSettings(userId: String) {
        viewModelScope.launch {
            _userSettings.value = repository.getUser(userId)
        }
    }

    fun updateUserSettings(userId: String, updatedData: Map<String, Any>) {
        viewModelScope.launch {
            repository.updateUser(userId, updatedData)
            fetchUserSettings(userId) // Refresh after update
        }
    }
}
