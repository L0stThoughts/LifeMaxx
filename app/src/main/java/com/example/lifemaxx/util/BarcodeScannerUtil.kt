package com.example.lifemaxx.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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
 */
@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                androidx.camera.view.PreviewView(ctx).apply {
                    implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                    setupCameraAndBarcodeScan(
                        context = ctx,
                        cameraProviderFuture = cameraProviderFuture,
                        lifecycleOwner = lifecycleOwner,
                        previewView = this,
                        cameraExecutor = cameraExecutor,
                        onBarcodeDetected = onBarcodeDetected,
                        onError = onError
                    )
                }
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
 */
private fun setupCameraAndBarcodeScan(
    context: Context,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    lifecycleOwner: LifecycleOwner,
    previewView: androidx.camera.view.PreviewView,
    cameraExecutor: java.util.concurrent.Executor,
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview use case
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Image analysis use case
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(
                            onBarcodeDetected = onBarcodeDetected,
                            onError = onError
                        ))
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind any existing use cases before binding new ones
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("BarcodeScannerUtil", "Camera binding failed", e)
                onError("Failed to initialize camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    } catch (e: Exception) {
        Log.e("BarcodeScannerUtil", "Camera setup failed", e)
        onError("Camera initialization error: ${e.message}")
    }
}

/**
 * Analyzer class for processing camera frames and detecting barcodes.
 */
private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
    private val onError: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerUtil.buildBarcodeScannerOptions()
    private val scanner = BarcodeScanning.getClient(options)

    // Throttle detection to prevent rapid-fire events
    private var lastAnalyzedTimeStamp = 0L

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        // Throttle analysis to once every 500ms
        if (currentTimestamp - lastAnalyzedTimeStamp >= 500) {
            imageProxy.image?.let { image ->
                val inputImage = InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

                processImage(scanner, inputImage, imageProxy)
                lastAnalyzedTimeStamp = currentTimestamp
            } ?: run {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    private fun processImage(
        scanner: BarcodeScanner,
        image: InputImage,
        imageProxy: ImageProxy
    ) {
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Take the first detected barcode
                    val barcode = barcodes.first()
                    barcode.rawValue?.let { value ->
                        onBarcodeDetected(value)
                    }
                }
            }
            .addOnFailureListener { e ->
                onError("Barcode scanning failed: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}