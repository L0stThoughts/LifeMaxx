package com.example.lifemaxx.ui

import android.util.Log
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
import com.example.lifemaxx.viewmodel.DoseTrackerViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Shows a list of today's supplements and lets the user mark them
 * as taken or untaken with switches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseTrackerScreen(navController: NavController) {
    val TAG = "DoseTrackerScreen"
    val context = LocalContext.current

    // Initialize Firebase first
    LaunchedEffect(Unit) {
        FirebaseUtils.initializeFirebase(context)
    }

    val viewModel: DoseTrackerViewModel = koinViewModel()
    val scope = rememberCoroutineScope()

    // State collections
    val supplements by viewModel.supplements.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Count how many are taken
    val takenCount = supplements.count { it.isTaken }

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
                title = { Text("Dose Tracker") },
                actions = {
                    IconButton(onClick = { viewModel.fetchSupplements() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            if (isLoading) {
                // Loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Title and progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Today's Supplements",
                            style = MaterialTheme.typography.titleLarge
                        )

                        if (supplements.isNotEmpty()) {
                            Text(
                                "$takenCount/${supplements.size} taken",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mark All buttons
                    if (supplements.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.updateAllSupplementsTaken(true)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark All Taken")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.updateAllSupplementsTaken(false)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mark All Untaken")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // No supplements state
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
                                    "Add supplements in the Supplements screen first",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { navController.navigate("supplementList") }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Supplements")
                                }
                            }
                        }
                    } else {
                        // List each supplement with a Switch for isTaken
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = supplements,
                                key = { it.id }
                            ) { supplement ->
                                SupplementDoseItem(
                                    supplement = supplement,
                                    onTakenChange = { newVal ->
                                        scope.launch {
                                            viewModel.updateSupplementTaken(supplement.id, newVal)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupplementDoseItem(
    supplement: Supplement,
    onTakenChange: (Boolean) -> Unit
) {
    // Local state for immediate UI feedback, but the true source is Firestore
    var localIsTaken by remember { mutableStateOf(supplement.isTaken) }

    // Update local state when supplement changes
    LaunchedEffect(supplement.isTaken) {
        localIsTaken = supplement.isTaken
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (localIsTaken) "Taken" else "To Take",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = localIsTaken,
                    onCheckedChange = {
                        localIsTaken = it // Optimistic update
                        onTakenChange(it)
                    }
                )
            }
        }
    }
}