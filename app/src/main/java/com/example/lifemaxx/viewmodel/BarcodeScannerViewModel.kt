package com.example.lifemaxx.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.model.SupplementBarcode
import com.example.lifemaxx.repository.BarcodeRepository
import com.example.lifemaxx.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for barcode scanning and supplement lookup functionality.
 */
class BarcodeScannerViewModel(
    private val barcodeRepository: BarcodeRepository,
    private val supplementRepository: SupplementRepository
) : ViewModel() {
    private val TAG = "BarcodeScannerViewModel"

    // Scanning state
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> get() = _isScanning

    // Current scanned barcode
    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> get() = _scannedBarcode

    // Supplement info for the scanned barcode
    private val _supplementInfo = MutableStateFlow<SupplementBarcode?>(null)
    val supplementInfo: StateFlow<SupplementBarcode?> get() = _supplementInfo

    // Editing state for the supplement details
    private val _isEditingDetails = MutableStateFlow(false)
    val isEditingDetails: StateFlow<Boolean> get() = _isEditingDetails

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    /**
     * Handle a detected barcode by looking up supplement information.
     */
    fun onBarcodeDetected(barcode: String) {
        if (_scannedBarcode.value == barcode) {
            // Already processing this barcode
            return
        }

        _scannedBarcode.value = barcode
        _isScanning.value = false
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val supplementInfo = barcodeRepository.lookupBarcode(barcode)
                _supplementInfo.value = supplementInfo
                Log.d(TAG, "Found supplement info for barcode: $barcode")
            } catch (e: Exception) {
                Log.e(TAG, "Error looking up barcode: ${e.message}", e)
                _error.value = "Error looking up barcode: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add the scanned supplement to the user's list.
     */
    fun addSupplementFromBarcode(
        name: String,
        dailyDose: Int,
        measureUnit: String,
        remainingQuantity: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create supplement from form data
                val supplement = Supplement(
                    name = name,
                    dailyDose = dailyDose,
                    measureUnit = measureUnit,
                    remainingQuantity = remainingQuantity
                )

                // Add to user's supplements
                val success = supplementRepository.addSupplement(supplement)

                if (success) {
                    // Also save the barcode info for future lookups
                    _supplementInfo.value?.let { info ->
                        val updatedInfo = info.copy(
                            name = name,
                            dailyDose = dailyDose,
                            measureUnit = measureUnit
                        )
                        barcodeRepository.saveBarcodeInfo(updatedInfo)
                    }

                    // Reset scanning state
                    resetScanState()
                } else {
                    _error.value = "Failed to add supplement"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding supplement: ${e.message}", e)
                _error.value = "Error adding supplement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start or resume scanning.
     */
    fun startScanning() {
        _isScanning.value = true
        _scannedBarcode.value = null
        _supplementInfo.value = null
        _error.value = null
    }

    /**
     * Reset the scanning state.
     */
    fun resetScanState() {
        _isScanning.value = true
        _scannedBarcode.value = null
        _supplementInfo.value = null
        _isEditingDetails.value = false
        _error.value = null
    }

    /**
     * Toggle editing state for the supplement details.
     */
    fun toggleEditingDetails() {
        _isEditingDetails.value = !_isEditingDetails.value
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _error.value = null
    }
}