package com.jarvis.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.core.llm.LLMMode

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val testResult by viewModel.testConnectionResult.collectAsState()

    var apiKeyInput by remember { mutableStateOf("") }
    var accessKeyInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AI mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LLMMode.entries.forEach { mode ->
                    Button(onClick = { viewModel.update { it.copy(llmMode = mode.name) } }) {
                        Text(if (settings.llmMode == mode.name) "[${mode.name}]" else mode.name)
                    }
                }
            }

            Row {
                Checkbox(
                    checked = settings.allowAutoRemoteFallback,
                    onCheckedChange = { checked -> viewModel.update { it.copy(allowAutoRemoteFallback = checked) } }
                )
                Text("Allow AUTO to fall back to Remote", modifier = Modifier.padding(top = 12.dp))
            }

            Text("Remote provider", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = settings.remoteBaseUrl,
                onValueChange = { viewModel.update { s -> s.copy(remoteBaseUrl = it) } },
                label = { Text("Base URL (https://...)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = settings.remoteModel,
                onValueChange = { viewModel.update { s -> s.copy(remoteModel = it) } },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.saveRemoteApiKey(apiKeyInput) }) { Text("Save key") }
                Button(onClick = { viewModel.testConnection(apiKeyInput.ifBlank { null }) }) { Text("Test Connection") }
            }
            testResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            Text("Wake word", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = accessKeyInput,
                onValueChange = { accessKeyInput = it },
                label = { Text("Porcupine AccessKey") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { viewModel.savePorcupineAccessKey(accessKeyInput) }) { Text("Save AccessKey") }

            Text("Speech", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = settings.speechLanguageTag,
                onValueChange = { viewModel.update { s -> s.copy(speechLanguageTag = it) } },
                label = { Text("Speech language (e.g. en-US)") },
                modifier = Modifier.fillMaxWidth()
            )
Text("Background app launching", style = MaterialTheme.typography.titleMedium)
            val context = LocalContext.current
            var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        overlayGranted = Settings.canDrawOverlays(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            Text(
                if (overlayGranted) "Granted — JARVIS can open apps even while its own screen is closed"
                else "Not granted — JARVIS can currently only open apps while its own screen is on top",
                style = MaterialTheme.typography.bodySmall
            )
            if (!overlayGranted) {
                Button(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }) { Text("Grant \"Display over other apps\"") }
            }
            Text("Local model", style = MaterialTheme.typography.titleMedium)
            Text("Status: Not installed", style = MaterialTheme.typography.bodySmall)

            Text("Logging", style = MaterialTheme.typography.titleMedium)
            Row {
                Checkbox(
                    checked = settings.eventLoggingEnabled,
                    onCheckedChange = { checked -> viewModel.update { it.copy(eventLoggingEnabled = checked) } }
                )
                Text("Event logging", modifier = Modifier.padding(top = 12.dp))
            }
            Row {
                Checkbox(
                    checked = settings.transcriptLoggingEnabled,
                    onCheckedChange = { checked -> viewModel.update { it.copy(transcriptLoggingEnabled = checked) } }
                )
                Text("Transcript logging (opt-in)", modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
