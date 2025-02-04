package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifemaxx.model.Supplement
import com.example.lifemaxx.viewmodel.DoseTrackerViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseTrackerScreen(navController: NavController) {
    val viewModel: DoseTrackerViewModel = koinViewModel()

    // Collect the list of supplements from the ViewModel
    val supplements by viewModel.supplements.collectAsState()

    // Count how many are taken
    val takenCount = supplements.count { it.isTaken }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dose Tracker") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Buttons for Mark All
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.updateAllSupplementsTaken(true) }) {
                    Text("Mark All Taken")
                }
                Button(onClick = { viewModel.updateAllSupplementsTaken(false) }) {
                    Text("Mark All Untaken")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show how many are taken vs total
            Text("Supplements ($takenCount/${supplements.size} taken)")

            Spacer(modifier = Modifier.height(8.dp))

            // List each supplement with a Switch for isTaken
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(supplements) { supplement ->
                    SupplementDoseItem(
                        supplement = supplement,
                        onTakenChange = { newVal ->
                            viewModel.updateSupplementTaken(supplement.id, newVal)
                        }
                    )
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(supplement.name, style = MaterialTheme.typography.titleMedium)
                Text("Daily Dose: ${supplement.dailyDose}")
            }
            Row {
                Text(if (localIsTaken) "Taken" else "To be taken")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = localIsTaken,
                    onCheckedChange = {
                        localIsTaken = it
                        onTakenChange(it)
                    }
                )
            }
        }
    }
}