package com.example.lifemaxx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vibrant, modern colors
private val LightColors = lightColorScheme(
    primary = Color(0xFF4355B9),   // a bright indigo
    onPrimary = Color.White,
    secondary = Color(0xFFFFC107), // a bold amber accent
    onSecondary = Color.Black,
    tertiary = Color(0xFF00BFA5),  // a teal color
    background = Color(0xFFF6F6F6),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AA4F8),
    onPrimary = Color(0xFF1E1E1E),
    secondary = Color(0xFFFFCA28),
    onSecondary = Color(0xFF1E1E1E),
    tertiary = Color(0xFF26D7BE),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECECEC),
)

@Composable
fun LifeMaxxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // you can customize further
        shapes = Shapes(),
        content = content
    )
}


