package com.jarvis.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jarvis.app.permission.PermissionCoordinator
import com.jarvis.core.domain.model.JarvisState
import com.jarvis.app.ui.components.JarvisOrb

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var isListening by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionCoordinator = remember { PermissionCoordinator(context) }
    val permissionStatus = remember(isListening) { permissionCoordinator.status() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("JARVIS", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            JarvisOrb(state = JarvisState.Idle)

            Spacer(Modifier.height(24.dp))
            Text(if (isListening) "Voice service running" else "Voice service stopped")
            Text("LLM mode: ${settings.llmMode}", style = MaterialTheme.typography.bodySmall)

            if (!permissionStatus.microphoneGranted) {
                Text(
                    "Microphone permission is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                if (isListening) onStopListening() else onStartListening()
                isListening = !isListening
            }) {
                Text(if (isListening) "Stop Listening" else "Start Listening")
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onOpenSettings) {
                Text("Settings")
            }
        }
    }
}
