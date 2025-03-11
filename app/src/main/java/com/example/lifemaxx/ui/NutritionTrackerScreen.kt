package com.example.lifemaxx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.NutritionEntry
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.viewmodel.NutritionViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionTrackerScreen(navController: NavController) {
    val context = LocalContext.current

    // Initialize Firebase first
    LaunchedEffect(Unit) {
        FirebaseUtils.initializeFirebase(context)
    }

    val viewModel: NutritionViewModel = koinViewModel()
    val nutritionEntries by viewModel.nutritionEntries.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val dailyTotals by viewModel.dailyTotals.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<NutritionEntry?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<String?>(null) }

    // SnackBar for status messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar when status message changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Tracker") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food Entry")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Date selector
            DateSelector(
                currentDate = currentDate,
                onPreviousDay = { viewModel.previousDay() },
                onNextDay = { viewModel.nextDay() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Daily totals card
            DailyNutritionSummary(dailyTotals = dailyTotals)

            Spacer(modifier = Modifier.height(16.dp))

            // List of nutrition entries or loading/empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (nutritionEntries.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No food entries for this day.\nTap + to add one!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = nutritionEntries,
                            key = { it.id }
                        ) { entry ->
                            NutritionEntryItem(
                                entry = entry,
                                onEdit = { editingEntry = entry },
                                onDelete = {
                                    entryToDelete = entry.id
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show add dialog when requested
    if (showAddDialog) {
        AddNutritionEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { entry ->
                scope.launch {
                    viewModel.addNutritionEntry(entry)
                }
                showAddDialog = false
            }
        )
    }

    // Show edit dialog when an entry is selected
    editingEntry?.let { entry ->
        EditNutritionEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onConfirm = { updatedEntry ->
                scope.launch {
                    viewModel.updateNutritionEntry(
                        entryId = updatedEntry.id,
                        foodName = updatedEntry.foodName,
                        calories = updatedEntry.calories,
                        proteins = updatedEntry.proteins,
                        carbs = updatedEntry.carbs,
                        fats = updatedEntry.fats,
                        servingSize = updatedEntry.servingSize,
                        mealType = updatedEntry.mealType
                    )
                }
                editingEntry = null
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                entryToDelete = null
            },
            title = { Text("Delete Food Entry") },
            text = { Text("Are you sure you want to delete this food entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        entryToDelete?.let { id ->
                            scope.launch {
                                viewModel.deleteNutritionEntry(id)
                            }
                        }
                        showDeleteDialog = false
                        entryToDelete = null
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
                        entryToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateSelector(
    currentDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
        }

        Text(
            text = currentDate,
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(onClick = onNextDay) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
        }
    }
}

@Composable
fun DailyNutritionSummary(dailyTotals: NutritionViewModel.DailyTotals) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem(
                    name = "Calories",
                    value = "${dailyTotals.calories}",
                    color = Color(0xFFE57373)
                )

                MacroItem(
                    name = "Protein",
                    value = "${dailyTotals.proteins.toInt()}g",
                    color = Color(0xFF81C784)
                )

                MacroItem(
                    name = "Carbs",
                    value = "${dailyTotals.carbs.toInt()}g",
                    color = Color(0xFF64B5F6)
                )

                MacroItem(
                    name = "Fats",
                    value = "${dailyTotals.fats.toInt()}g",
                    color = Color(0xFFFFD54F)
                )
            }
        }
    }
}

@Composable
fun MacroItem(
    name: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun NutritionEntryItem(
    entry: NutritionEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
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
                    text = entry.foodName,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${entry.calories} cal | ${entry.mealType}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "P: ${entry.proteins.toInt()}g | C: ${entry.carbs.toInt()}g | F: ${entry.fats.toInt()}g",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Entry")
            }
        }
    }
}