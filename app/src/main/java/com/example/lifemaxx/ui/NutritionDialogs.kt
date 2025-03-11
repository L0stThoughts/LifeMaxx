package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lifemaxx.model.NutritionEntry

@Composable
fun AddNutritionEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (NutritionEntry) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("0") }
    var proteins by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var fats by remember { mutableStateOf("0") }
    var servingSize by remember { mutableStateOf("0") }
    var mealType by remember { mutableStateOf(NutritionEntry.MealType.LUNCH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Food Entry") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteins,
                        onValueChange = { proteins = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = servingSize,
                        onValueChange = { servingSize = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Serving (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Meal Type:")

                NutritionEntry.MealType.ALL.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mealType == type,
                            onClick = { mealType = type }
                        )

                        Text(type)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entry = NutritionEntry(
                        foodName = foodName,
                        calories = calories.toIntOrNull() ?: 0,
                        proteins = proteins.toDoubleOrNull() ?: 0.0,
                        carbs = carbs.toDoubleOrNull() ?: 0.0,
                        fats = fats.toDoubleOrNull() ?: 0.0,
                        servingSize = servingSize.toDoubleOrNull() ?: 0.0,
                        mealType = mealType
                    )
                    onConfirm(entry)
                },
                enabled = foodName.isNotBlank()
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
fun EditNutritionEntryDialog(
    entry: NutritionEntry,
    onDismiss: () -> Unit,
    onConfirm: (NutritionEntry) -> Unit
) {
    var foodName by remember { mutableStateOf(entry.foodName) }
    var calories by remember { mutableStateOf(entry.calories.toString()) }
    var proteins by remember { mutableStateOf(entry.proteins.toString()) }
    var carbs by remember { mutableStateOf(entry.carbs.toString()) }
    var fats by remember { mutableStateOf(entry.fats.toString()) }
    var servingSize by remember { mutableStateOf(entry.servingSize.toString()) }
    var mealType by remember { mutableStateOf(entry.mealType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Food Entry") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteins,
                        onValueChange = { proteins = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = servingSize,
                        onValueChange = { servingSize = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Serving (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Meal Type:")

                NutritionEntry.MealType.ALL.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mealType == type,
                            onClick = { mealType = type }
                        )

                        Text(type)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedEntry = entry.copy(
                        foodName = foodName,
                        calories = calories.toIntOrNull() ?: 0,
                        proteins = proteins.toDoubleOrNull() ?: 0.0,
                        carbs = carbs.toDoubleOrNull() ?: 0.0,
                        fats = fats.toDoubleOrNull() ?: 0.0,
                        servingSize = servingSize.toDoubleOrNull() ?: 0.0,
                        mealType = mealType
                    )
                    onConfirm(updatedEntry)
                },
                enabled = foodName.isNotBlank()
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