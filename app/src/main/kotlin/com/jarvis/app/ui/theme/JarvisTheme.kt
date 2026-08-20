package com.jarvis.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = Color(0xFF4EA1FF),
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF0B0F1A),
    surface = Color(0xFF11162A),
    error = Color(0xFFFF5A5A)
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        content = content
    )
}
