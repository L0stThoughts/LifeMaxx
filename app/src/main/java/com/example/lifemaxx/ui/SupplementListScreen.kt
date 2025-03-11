package com.example.lifemaxx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.viewmodel.SupplementViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Shows a list of all supplements plus a "plus" FAB in the bottom-right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementListScreen(navController: NavController) {
    val context = LocalContext.current

    // Initialize Firebase first
    LaunchedEffect(Unit) {
        FirebaseUtils.initializeFirebase(context)
    }

    val viewModel: SupplementViewModel = koinViewModel()
    val scope = rememberCoroutineScope()

    // State collections
    val supplements by viewModel.supplements.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplement by remember { mutableStateOf<Supplement?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var supplementToDelete by remember { mutableStateOf<String?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages in snackbar
    LaunchedEffect(error) {
        error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Show status messages in snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplements") },
                actions = {
                    IconButton(onClick = { viewModel.fetchSupplements() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplement")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (supplements.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "No supplements found",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Tap the + button to add your first supplement",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Supplement")
                        }
                    }
                }
            } else {
                // Supplements list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = supplements,
                        key = { it.id }
                    ) { supplement ->
                        SupplementItem(
                            supplement = supplement,
                            onDelete = {
                                supplementToDelete = supplement.id
                                showDeleteDialog = true
                            },
                            onClick = {
                                editingSupplement = supplement
                            }
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddSupplementDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, dose, measureUnit, remaining ->
                val newSupplement = Supplement(
                    name = name,
                    dailyDose = dose,
                    measureUnit = measureUnit,
                    remainingQuantity = remaining
                )
                scope.launch {
                    viewModel.addSupplement(newSupplement)
                }
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editingSupplement?.let { supplement ->
        EditSupplementDialog(
            oldSupplement = supplement,
            onDismiss = { editingSupplement = null },
            onConfirm = { name, dose, measureUnit, remaining ->
                scope.launch {
                    viewModel.updateSupplement(
                        supplementId = supplement.id,
                        name = name,
                        dailyDose = dose,
                        measureUnit = measureUnit,
                        remaining = remaining
                    )
                }
                editingSupplement = null
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                supplementToDelete = null
            },
            title = { Text("Delete Supplement") },
            text = { Text("Are you sure you want to delete this supplement? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        supplementToDelete?.let { id ->
                            scope.launch {
                                viewModel.deleteSupplement(id)
                            }
                        }
                        showDeleteDialog = false
                        supplementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        supplementToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SupplementItem(
    supplement: Supplement,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    supplement.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "Daily Dose: ${supplement.dailyDose} ${supplement.measureUnit}"
                )

                Text(
                    "Remaining: ${supplement.remainingQuantity} ${supplement.measureUnit}"
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

@Composable
fun AddSupplementDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("1") }
    var remaining by remember { mutableStateOf("30") }
    var formType by remember { mutableStateOf("Pill") }
    var powderMeasure by remember { mutableStateOf("mg") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Supplement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily Dose") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remaining,
                    onValueChange = { remaining = it.filter { c -> c.isDigit() } },
                    label = { Text("Remaining Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Form Type:")
                Row {
                    RadioButton(
                        selected = formType == "Pill",
                        onClick = { formType = "Pill" }
                    )
                    Text("Pill")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = formType == "Powder",
                        onClick = { formType = "Powder" }
                    )
                    Text("Powder")
                }

                if (formType == "Powder") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Powder Measure:")
                    Row {
                        RadioButton(
                            selected = powderMeasure == "mg",
                            onClick = { powderMeasure = "mg" }
                        )
                        Text("mg")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = powderMeasure == "g",
                            onClick = { powderMeasure = "g" }
                        )
                        Text("g")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button

                    val doseInt = dose.toIntOrNull() ?: 1
                    val remainingInt = remaining.toIntOrNull() ?: 0
                    val measureUnit = if (formType == "Pill") "pill" else powderMeasure
                    onConfirm(name, doseInt, measureUnit, remainingInt)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditSupplementDialog(
    oldSupplement: Supplement,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(oldSupplement.name) }
    var dose by remember { mutableStateOf(oldSupplement.dailyDose.toString()) }
    var remaining by remember { mutableStateOf(oldSupplement.remainingQuantity.toString()) }

    // If measureUnit is "pill", we consider that "Pill", otherwise "Powder"
    var formType by remember {
        mutableStateOf(if (oldSupplement.measureUnit == "pill") "Pill" else "Powder")
    }

    // If formType is Powder, user picks mg/g
    var powderMeasure by remember {
        mutableStateOf(
            if (oldSupplement.measureUnit == "mg" || oldSupplement.measureUnit == "g")
                oldSupplement.measureUnit
            else
                "mg" // default
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Supplement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily Dose") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remaining,
                    onValueChange = { remaining = it.filter { c -> c.isDigit() } },
                    label = { Text("Remaining Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Form Type:")
                Row {
                    RadioButton(
                        selected = formType == "Pill",
                        onClick = { formType = "Pill" }
                    )
                    Text("Pill")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = formType == "Powder",
                        onClick = { formType = "Powder" }
                    )
                    Text("Powder")
                }

                if (formType == "Powder") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Powder Measure:")
                    Row {
                        RadioButton(
                            selected = powderMeasure == "mg",
                            onClick = { powderMeasure = "mg" }
                        )
                        Text("mg")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = powderMeasure == "g",
                            onClick = { powderMeasure = "g" }
                        )
                        Text("g")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button

                    val doseInt = dose.toIntOrNull() ?: 1
                    val remainingInt = remaining.toIntOrNull() ?: 0
                    val measureUnit = if (formType == "Pill") "pill" else powderMeasure
                    onConfirm(name, doseInt, measureUnit, remainingInt)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}