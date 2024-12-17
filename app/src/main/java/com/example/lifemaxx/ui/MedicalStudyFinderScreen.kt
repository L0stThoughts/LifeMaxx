package com.example.lifemaxx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifemaxx.model.Study

@Composable
fun MedicalStudyFinderScreen() {
    // Mock data for studies
    val studies = listOf(
        Study("Vitamin D and Bone Health", "Exploring how Vitamin D improves bone density."),
        Study("Omega-3 and Heart Health", "Research on Omega-3 fatty acids and cardiovascular health."),
        Study("Iron Deficiency Study", "Effects of iron supplements on anemia treatment.")
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Medical Study Finder") }) }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(studies) { study ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(study.title, style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(study.description, style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}
