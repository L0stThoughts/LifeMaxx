package com.example.lifemaxx.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.lifemaxx.ui.theme.LifeMaxxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparkTopAppBar() {
    TopAppBar(
        title = {
            SparkTitle()
        },
        // Optionally customize the bar colors:
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * A gradient text composable for "LifeMaxx" title.
 */
@Composable
fun SparkTitle() {
    // Define a gradient brush (left-to-right)
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    // Use an AnnotatedString + SpanStyle(brush=...) to render gradient text
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(brush = gradientBrush)) {
                append("LifeMaxx")
            }
        },
        style = MaterialTheme.typography.titleLarge
    )
}
