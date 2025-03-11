package com.example.lifemaxx.ui

import android.util.Log
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.viewmodel.RobustSupplementViewModel
import kotlinx.coroutines.launch

/**
 * A supplements list screen that uses a robust implementation
 * that works even if Firebase has issues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailsafeSupplementScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: RobustSupplementViewModel = viewModel(
        factory = RobustSupplementViewModel.Factory(context.applicationContext as android.app.Application)
    )

    // State collections
    val supplements by viewModel.supplements.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplement by remember { mutableStateOf<Supplement?>(null) }

    // Snackbar
    val scope = rememberCoroutineScope()
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Offline mode indicator
            if (isOfflineMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Working in offline mode. Changes will sync when connection is restored.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Supplement list or empty state
            if (supplements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = supplements,
                        key = { it.id }
                    ) { supplement ->
                        SupplementItem(
                            supplement = supplement,
                            onDelete = {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Delete ${supplement.name}?",
                                        actionLabel = "Confirm",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.deleteSupplement(supplement.id)
                                    }
                                }
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
                viewModel.addSupplement(newSupplement)
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
                viewModel.updateSupplement(
                    supplementId = supplement.id,
                    name = name,
                    dailyDose = dose,
                    measureUnit = measureUnit,
                    remaining = remaining
                )
                editingSupplement = null
            }
        )
    }
}