package com.example.lifemaxx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.viewmodel.SupplementViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Shows a list of all supplements plus a "plus" FAB in the bottom-left.
 * Clicking the FAB opens an AddSupplementDialog to add a new supplement.
 * Now, tapping each supplement opens an EditSupplementDialog to edit that item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementListScreen() {
    val viewModel: SupplementViewModel = koinViewModel()
    val supplements by viewModel.supplements.collectAsState()

    // Control whether we show the "Add" dialog
    var isAddDialogOpen by remember { mutableStateOf(false) }

    // Track which supplement is being edited, if any
    var editingSupplement by remember { mutableStateOf<Supplement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("All Supplements") })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Main list of supplements
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(supplements) { supplement ->
                    SupplementItem(
                        supplement = supplement,
                        onDelete = { viewModel.deleteSupplement(supplement.id) },
                        onClick = {
                            // User clicked on the item -> open edit dialog
                            editingSupplement = supplement
                        }
                    )
                }
            }

            // FloatingActionButton in bottom-left corner for adding new supplements
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Supplement")
            }

            // Show the Add dialog if user taps the "+"
            if (isAddDialogOpen) {
                AddSupplementDialog(
                    onDismiss = { isAddDialogOpen = false },
                    onConfirm = { name, dose, measureUnit, remaining ->
                        val newSupplement = Supplement(
                            name = name,
                            dailyDose = dose,
                            measureUnit = measureUnit,
                            remainingQuantity = remaining
                        )
                        viewModel.addSupplement(newSupplement)
                        isAddDialogOpen = false
                    }
                )
            }

            // Show the Edit dialog if a supplement is selected
            editingSupplement?.let { supplement ->
                EditSupplementDialog(
                    oldSupplement = supplement,
                    onDismiss = { editingSupplement = null },
                    onConfirm = { name, dose, measureUnit, remaining ->
                        // call viewModel.updateSupplement(...)
                        viewModel.updateSupplement(
                            supplement.id,
                            name,
                            dose,
                            measureUnit,
                            remaining
                        )
                        editingSupplement = null
                    }
                )
            }
        }
    }
}

/**
 * Represents each supplement item in the list, showing its name, daily dose,
 * remaining quantity, etc. with an option to delete.
 *
 * Now we also pass an `onClick` callback so the entire row is clickable.
 */
@Composable
fun SupplementItem(
    supplement: Supplement,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Entire row is clickable
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(supplement.name, style = MaterialTheme.typography.titleMedium)
                Text("Daily Dose: ${supplement.dailyDose} ${supplement.measureUnit}")
                Text("Remaining: ${supplement.remainingQuantity} ${supplement.measureUnit}")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

/**
 * A dialog for adding a new supplement. (Same as before)
 * The user picks:
 * - Name
 * - Daily Dose
 * - Remaining Quantity
 * - "Pill" or "Powder" (with "mg"/"g" choice)
 */
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
                    onValueChange = { dose = it },
                    label = { Text("Daily Dose") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remaining,
                    onValueChange = { remaining = it },
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
            Button(onClick = {
                val doseInt = dose.toIntOrNull() ?: 1
                val remainingInt = remaining.toIntOrNull() ?: 0
                val measureUnit = if (formType == "Pill") "pill" else powderMeasure
                onConfirm(name, doseInt, measureUnit, remainingInt)
            }) {
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

/**
 * Similar to AddSupplementDialog, but pre-populates fields with the existing supplement data.
 * On confirm, it calls `onConfirm` with the updated info, so the parent can do viewModel.updateSupplement(...).
 */
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
                    onValueChange = { dose = it },
                    label = { Text("Daily Dose") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remaining,
                    onValueChange = { remaining = it },
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
            Button(onClick = {
                val doseInt = dose.toIntOrNull() ?: 1
                val remainingInt = remaining.toIntOrNull() ?: 0
                val measureUnit = if (formType == "Pill") "pill" else powderMeasure
                onConfirm(name, doseInt, measureUnit, remainingInt)
            }) {
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
