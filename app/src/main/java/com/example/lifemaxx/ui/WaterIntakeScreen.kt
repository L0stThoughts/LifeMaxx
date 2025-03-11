package com.example.lifemaxx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.WaterIntake
import com.example.lifemaxx.util.FirebaseUtils
import com.example.lifemaxx.viewmodel.WaterIntakeViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeScreen(navController: NavController) {
    val context = LocalContext.current

    // Initialize Firebase first
    LaunchedEffect(Unit) {
        FirebaseUtils.initializeFirebase(context)
    }

    val viewModel: WaterIntakeViewModel = koinViewModel()
    val scope = rememberCoroutineScope()

    val currentDate by viewModel.currentDate.collectAsState()
    val waterIntakes by viewModel.waterIntakes.collectAsState()
    val totalWaterIntake by viewModel.totalWaterIntake.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val editingWaterIntake by viewModel.editingWaterIntake.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // UI state
    var showCustomDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<String?>(null) }

    // Calculate progress
    val progress = viewModel.calculateProgressPercentage()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "WaterProgressAnimation"
    )

    // SnackBar state
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Water Intake") },
                actions = {
                    // Daily goal setting
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Set Goal")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCustomDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Water Intake")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Progress indicator
                WaterProgressCard(
                    currentAmount = totalWaterIntake,
                    goal = dailyGoal,
                    progress = animatedProgress,
                    formatAmount = { viewModel.formatWaterAmount(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick add buttons
                QuickAddSection(
                    onAddWater = { containerType ->
                        scope.launch {
                            viewModel.addQuickWaterIntake(containerType)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Today's intake list
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                                "Today's Intake",
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (waterIntakes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        "No water intake recorded today\nTap the buttons above to add water",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            LazyColumn {
                                items(waterIntakes) { entry ->
                                    WaterIntakeItem(
                                        entry = entry,
                                        formatTime = { viewModel.formatTime(it) },
                                        formatAmount = { viewModel.formatWaterAmount(it) },
                                        onEdit = {
                                            viewModel.editWaterIntake(entry)
                                        },
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
        }
    }

    // Custom water intake dialog
    if (showCustomDialog) {
        AddCustomWaterIntakeDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { amount ->
                scope.launch {
                    viewModel.addCustomWaterIntake(amount)
                }
                showCustomDialog = false
            }
        )
    }

    // Set goal dialog
    if (showGoalDialog) {
        SetWaterGoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { goal ->
                viewModel.setDailyGoal(goal)
                showGoalDialog = false
            }
        )
    }

    // Edit water intake dialog
    editingWaterIntake?.let { entry ->
        EditWaterIntakeDialog(
            entry = entry,
            onDismiss = { viewModel.cancelEditingWaterIntake() },
            onConfirm = { amount, containerType ->
                scope.launch {
                    viewModel.updateWaterIntake(entry.id, amount, containerType)
                }
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
            title = { Text("Delete Water Intake") },
            text = { Text("Are you sure you want to delete this water intake entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        entryToDelete?.let { id ->
                            scope.launch {
                                viewModel.deleteWaterIntake(id)
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
fun WaterProgressCard(
    currentAmount: Int,
    goal: Int,
    progress: Float,
    formatAmount: (Int) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Daily Progress",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // Progress circle
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 16.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )

                // Water drop icon in center
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current amount / goal
            Text(
                "${formatAmount(currentAmount)} / ${formatAmount(goal)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Progress percentage
            Text(
                "${(progress * 100).toInt()}% of daily goal",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun QuickAddSection(
    onAddWater: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Quick Add",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WaterButton(
                    containerType = WaterIntake.ContainerType.GLASS,
                    icon = Icons.Default.LocalDrink,
                    amount = WaterIntake.GLASS_SIZE,
                    onClick = { onAddWater(WaterIntake.ContainerType.GLASS) }
                )
            }

            item {
                WaterButton(
                    containerType = WaterIntake.ContainerType.BOTTLE,
                    icon = Icons.Default.LocalDrink,
                    amount = WaterIntake.BOTTLE_SIZE,
                    onClick = { onAddWater(WaterIntake.ContainerType.BOTTLE) }
                )
            }

            item {
                WaterButton(
                    containerType = WaterIntake.ContainerType.MUG,
                    icon = Icons.Default.Coffee,
                    amount = WaterIntake.MUG_SIZE,
                    onClick = { onAddWater(WaterIntake.ContainerType.MUG) }
                )
            }

            item {
                WaterButton(
                    containerType = WaterIntake.ContainerType.SMALL_BOTTLE,
                    icon = Icons.Default.LocalDrink,
                    amount = WaterIntake.SMALL_BOTTLE_SIZE,
                    onClick = { onAddWater(WaterIntake.ContainerType.SMALL_BOTTLE) }
                )
            }

            item {
                WaterButton(
                    containerType = WaterIntake.ContainerType.LARGE_BOTTLE,
                    icon = Icons.Default.LocalDrink,
                    amount = WaterIntake.LARGE_BOTTLE_SIZE,
                    onClick = { onAddWater(WaterIntake.ContainerType.LARGE_BOTTLE) }
                )
            }
        }
    }
}

@Composable
fun WaterButton(
    containerType: String,
    icon: ImageVector,
    amount: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = containerType,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            containerType,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "${amount}ml",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun WaterIntakeItem(
    entry: WaterIntake,
    formatTime: (Long) -> String,
    formatAmount: (Int) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Water icon
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Water info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    formatAmount(entry.amount),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    "${entry.containerType} at ${formatTime(entry.time)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Delete button
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
fun AddCustomWaterIntakeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf("250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Water Intake") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (ml)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Preset buttons for common amounts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (preset in listOf(100, 200, 250, 500, 1000)) {
                        OutlinedButton(
                            onClick = { amount = preset.toString() }
                        ) {
                            Text("${preset}ml")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toIntOrNull() ?: 0
                    if (amountValue > 0) {
                        onConfirm(amountValue)
                    }
                }
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
fun SetWaterGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var goal by remember { mutableStateOf(currentGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Water Goal") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it.filter { c -> c.isDigit() } },
                    label = { Text("Goal (ml)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Preset goal amounts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (preset in listOf(1500, 2000, 2500, 3000, 3500)) {
                        OutlinedButton(
                            onClick = { goal = preset.toString() }
                        ) {
                            Text("${preset}ml")
                        }
                    }
                }

                // Recommendation message
                Text(
                    "Recommended water intake is typically 2000-3000ml per day, depending on your weight and activity level.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goalValue = goal.toIntOrNull() ?: 0
                    if (goalValue > 0) {
                        onConfirm(goalValue)
                    }
                }
            ) {
                Text("Set Goal")
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
fun EditWaterIntakeDialog(
    entry: WaterIntake,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var amount by remember { mutableStateOf(entry.amount.toString()) }
    var containerType by remember { mutableStateOf(entry.containerType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Water Intake") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (ml)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Container type selection
                Text("Container Type")

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WaterIntake.ContainerType.ALL) { type ->
                        FilterChip(
                            selected = containerType == type,
                            onClick = { containerType = type },
                            label = { Text(type) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toIntOrNull() ?: 0
                    if (amountValue > 0) {
                        onConfirm(amountValue, containerType)
                    }
                }
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