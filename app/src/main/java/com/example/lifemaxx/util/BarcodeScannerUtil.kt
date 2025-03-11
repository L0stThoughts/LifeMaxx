package com.example.lifemaxx.util

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Utility class for barcode scanning functionality.
 */
class BarcodeScannerUtil {
    companion object {
        private const val TAG = "BarcodeScannerUtil"

        /**
         * Check if the camera permission is granted.
         */
        fun hasCameraPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Build the barcode scanner options.
         */
        fun buildBarcodeScannerOptions(): BarcodeScannerOptions {
            return BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_QR_CODE
                )
                .build()
        }
    }
}

/**
 * A composable function that shows a camera preview and scans barcodes.
 * Improved implementation with proper resource management.
 */
@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Store the camera executor in a remember so it survives composition
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Store the camera provider future in a remember
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasCameraPermission by remember {
        mutableStateOf(BarcodeScannerUtil.hasCameraPermission(context))
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onError("Camera permission is required for barcode scanning")
        }
    }

    // Request camera permission if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        // Use AndroidView to embed the camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx).apply {
                    implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                }

                try {
                    // Use the future to get the camera provider
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            bindCameraUseCases(
                                context = ctx,
                                cameraProvider = cameraProvider,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                cameraExecutor = cameraExecutor,
                                onBarcodeDetected = onBarcodeDetected,
                                onError = onError
                            )
                            bindCameraUseCases(
                                context = ctx,
                                cameraProvider = cameraProvider,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                cameraExecutor = cameraExecutor,
                                onBarcodeDetected = onBarcodeDetected,
                                onError = onError
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Camera binding failed", e)
                            onError("Failed to initialize camera: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                } catch (e: Exception) {
                    Log.e(TAG, "Camera setup failed", e)
                    onError("Camera initialization error: ${e.message}")
                }

                previewView
            }
        )
    }

    // Cleanup camera executor when component is destroyed
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * Set up the camera and barcode scanning process.
 * Improved implementation with proper resource management.
 */
private fun bindCameraUseCases(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: androidx.camera.view.PreviewView,
    cameraExecutor: java.util.concurrent.Executor,
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Unbind any existing use cases before binding new ones
        cameraProvider.unbindAll()

        // Preview use case
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Camera selector
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Image analysis use case
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(
                    onBarcodeDetected = { barcode ->
                        // Only pass one barcode to avoid repeated callbacks
                        onBarcodeDetected(barcode)
                    },
                    onError = { errorMessage ->
                        onError(errorMessage)
                    }
                ))
            }

        // Bind all use cases to camera
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )

        Log.d("BarcodeScannerUtil", "Camera use cases bound successfully")
    } catch (e: Exception) {
        Log.e("BarcodeScannerUtil", "Camera binding failed", e)
        onError("Failed to initialize camera: ${e.message}")
    }
}

/**
 * Analyzer class for processing camera frames and detecting barcodes.
 * Improved implementation with better resource management.
 */
private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
    private val onError: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerUtil.buildBarcodeScannerOptions()
    private val scanner = BarcodeScanning.getClient(options)

    // Keep track of when we detected a barcode to avoid duplicate detections
    private var lastAnalyzedTimeStamp = 0L
    private var lastDetectedBarcode: String? = null

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        // Check if we need to analyze this frame
        if (currentTimestamp - lastAnalyzedTimeStamp >= 500) {
            imageProxy.image?.let { image ->
                try {
                    val inputImage = InputImage.fromMediaImage(
                        image,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    // Process the image
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                // Take the first detected barcode
                                val barcode = barcodes.first()
                                barcode.rawValue?.let { value ->
                                    // Only notify if it's a new barcode
                                    if (value != lastDetectedBarcode) {
                                        lastDetectedBarcode = value
                                        onBarcodeDetected(value)
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            onError("Barcode scanning failed: ${e.message}")
                        }
                        .addOnCompleteListener {
                            // Make sure to close the imageProxy in all cases
                            imageProxy.close()
                        }

                    lastAnalyzedTimeStamp = currentTimestamp
                } catch (e: Exception) {
                    Log.e("BarcodeAnalyzer", "Error processing image: ${e.message}", e)
                    onError("Error processing image: ${e.message}")
                    imageProxy.close()
                }
            } ?: run {
                // No image available, close the proxy
                imageProxy.close()
            }
        } else {
            // Skip this frame but still need to close
            imageProxy.close()
        }
    }
}