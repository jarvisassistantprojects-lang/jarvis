package com.jarvis.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jarvis.app.JarvisAppContainer
import com.jarvis.app.navigation.JarvisRoute
import com.jarvis.app.ui.main.JarvisScreen
import com.jarvis.app.ui.main.JarvisViewModel
import com.jarvis.app.ui.settings.SettingsScreen
import com.jarvis.app.ui.settings.SettingsViewModel
import com.jarvis.app.ui.theme.JarvisTheme

/** Milestone 1 has exactly two screens, so a full navigation-compose dependency is not
 *  warranted yet — plain Compose state switching keeps the dependency surface small. */
@Composable
fun JarvisRoot(
    container: JarvisAppContainer,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    var currentRoute by remember { mutableStateOf<JarvisRoute>(JarvisRoute.Main) }

    JarvisTheme {
        when (currentRoute) {
            JarvisRoute.Main -> {
                val viewModel = remember { JarvisViewModel(container) }
                JarvisScreen(
                    viewModel = viewModel,
                    onStartListening = onStartListening,
                    onStopListening = onStopListening,
                    onOpenSettings = { currentRoute = JarvisRoute.Settings }
                )
            }
            JarvisRoute.Settings -> {
                val viewModel = remember { SettingsViewModel(container) }
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentRoute = JarvisRoute.Main }
                )
            }
        }
    }
}
