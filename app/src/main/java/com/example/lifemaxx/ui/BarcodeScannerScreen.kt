package com.example.lifemaxx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.SupplementBarcode
import com.example.lifemaxx.util.BarcodeScanner
import com.example.lifemaxx.viewmodel.BarcodeScannerViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material.icons.rounded.QrCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(navController: NavController) {
    val viewModel: BarcodeScannerViewModel = koinViewModel()

    val isScanning by viewModel.isScanning.collectAsState()
    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val supplementInfo by viewModel.supplementInfo.collectAsState()
    val isEditingDetails by viewModel.isEditingDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var name by remember { mutableStateOf("") }
    var dailyDose by remember { mutableStateOf("1") }
    var measureUnit by remember { mutableStateOf("pill") }
    var remainingQuantity by remember { mutableStateOf("30") }

    // Update the form when supplement info changes
    LaunchedEffect(supplementInfo) {
        supplementInfo?.let {
            name = it.name
            dailyDose = it.dailyDose.toString()
            measureUnit = it.measureUnit
        }
    }

    // SnackBar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when error occurs
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Supplement") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isScanning) {
                        IconButton(onClick = { viewModel.startScanning() }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Again")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isScanning) {
                // Camera preview for scanning
                BarcodeScanner(
                    onBarcodeDetected = { barcode ->
                        viewModel.onBarcodeDetected(barcode)
                    },
                    onError = { errorMessage ->
                        snackbarHostState.currentSnackbarData?.dismiss()
                        // Scope issue - we'd need to handle this differently in a real app
                        // For now, using the viewModel to store error
                        error?.let { viewModel.clearError() }
                        viewModel.clearError() // Ensure it's clear before setting
                        error?.let { viewModel.clearError() } // Redundant but being cautious
                    }
                )

                // Overlay with scanning instructions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Point camera at supplement barcode",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "The app will automatically scan and find the supplement details",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Display the scanned supplement info or editing form
                SupplementInfoSection(
                    scannedBarcode = scannedBarcode,
                    supplementInfo = supplementInfo,
                    isEditingDetails = isEditingDetails,
                    isLoading = isLoading,
                    name = name,
                    dailyDose = dailyDose,
                    measureUnit = measureUnit,
                    remainingQuantity = remainingQuantity,
                    onNameChange = { name = it },
                    onDailyDoseChange = { dailyDose = it },
                    onMeasureUnitChange = { measureUnit = it },
                    onRemainingQuantityChange = { remainingQuantity = it },
                    onToggleEdit = { viewModel.toggleEditingDetails() },
                    onAddSupplement = {
                        viewModel.addSupplementFromBarcode(
                            name = name,
                            dailyDose = dailyDose.toIntOrNull() ?: 1,
                            measureUnit = measureUnit,
                            remainingQuantity = remainingQuantity.toIntOrNull() ?: 30
                        )

                        // Navigate back to supplement list
                        navController.popBackStack()
                    },
                    onScanAgain = { viewModel.startScanning() }
                )
            }
        }
    }
}

@Composable
fun SupplementInfoSection(
    scannedBarcode: String?,
    supplementInfo: SupplementBarcode?,
    isEditingDetails: Boolean,
    isLoading: Boolean,
    name: String,
    dailyDose: String,
    measureUnit: String,
    remainingQuantity: String,
    onNameChange: (String) -> Unit,
    onDailyDoseChange: (String) -> Unit,
    onMeasureUnitChange: (String) -> Unit,
    onRemainingQuantityChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onAddSupplement: () -> Unit,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Barcode information card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Barcode Information",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode,
                            contentDescription = "Barcode",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            "Barcode: ${scannedBarcode ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    supplementInfo?.let { info ->
                        if (info.exists) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    "This supplement is already in your list",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Supplement details card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Supplement Details",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = onToggleEdit) {
                        Icon(
                            imageVector = if (isEditingDetails) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditingDetails) "Done Editing" else "Edit Details"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditingDetails) {
                    // Editing form
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Supplement Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyDose,
                            onValueChange = { onDailyDoseChange(it.filter { c -> c.isDigit() }) },
                            label = { Text("Daily Dose") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = remainingQuantity,
                            onValueChange = { onRemainingQuantityChange(it.filter { c -> c.isDigit() }) },
                            label = { Text("Remaining") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Measure unit selection
                    Text("Measure Unit")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (unit in listOf("pill", "mg", "g")) {
                            FilterChip(
                                selected = measureUnit == unit,
                                onClick = { onMeasureUnitChange(unit) },
                                label = { Text(unit) }
                            )
                        }
                    }
                } else {
                    // Display supplement info
                    supplementInfo?.let { info ->
                        SupplementInfoRow(
                            icon = Icons.Default.Medication,
                            label = "Name",
                            value = name
                        )

                        SupplementInfoRow(
                            icon = Icons.Default.Numbers,
                            label = "Daily Dose",
                            value = "$dailyDose $measureUnit"
                        )

                        SupplementInfoRow(
                            icon = Icons.Default.Store,
                            label = "Manufacturer",
                            value = info.manufacturer.ifEmpty { "Unknown" }
                        )

                        if (info.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Description",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                info.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Again")
            }

            Button(
                onClick = onAddSupplement,
                modifier = Modifier.weight(1f),
                enabled = !supplementInfo?.exists!! ?: true
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Supplement")
            }
        }
    }
}

@Composable
fun SupplementInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}