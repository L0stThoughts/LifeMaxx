package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifemaxx.model.MedicalStudy
import com.example.lifemaxx.viewmodel.MedicalStudyFinderViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalStudyFinderScreen() {
    val viewModel: MedicalStudyFinderViewModel = koinViewModel()
    val medicalStudies by viewModel.medicalStudies.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Medical Study Finder") })
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(medicalStudies) { study ->
                StudyCard(study = study)
            }
        }
    }
}

@Composable
fun StudyCard(study: MedicalStudy) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(study.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(study.description, style = MaterialTheme.typography.bodyMedium)
            if (study.link.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Link: ${study.link}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
