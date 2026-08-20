package com.jarvis.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.app.JarvisAppContainer
import com.jarvis.data.security.SecretStore
import com.jarvis.data.settings.JarvisSettings
import com.jarvis.providers.remotellm.OpenAICompatibleAdapter
import com.jarvis.providers.remotellm.RemoteLLMConfig
import com.jarvis.core.domain.model.OperationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: JarvisAppContainer) : ViewModel() {

    val settings: StateFlow<JarvisSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JarvisSettings())

    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult

    fun update(transform: (JarvisSettings) -> JarvisSettings) {
        viewModelScope.launch { container.settingsRepository.update(transform) }
    }

    fun saveRemoteApiKey(apiKey: String) {
        viewModelScope.launch { container.secretStore.put(SecretStore.KEY_REMOTE_API_KEY, apiKey) }
    }

    fun savePorcupineAccessKey(accessKey: String) {
        viewModelScope.launch { container.secretStore.put(SecretStore.KEY_PORCUPINE_ACCESS_KEY, accessKey) }
    }

    fun testConnection(apiKeyOverride: String?) {
        viewModelScope.launch {
            val current = settings.value
            val apiKey = apiKeyOverride ?: container.secretStore.get(SecretStore.KEY_REMOTE_API_KEY)
            if (apiKey.isNullOrBlank() || current.remoteBaseUrl.isBlank() || current.remoteModel.isBlank()) {
                _testConnectionResult.value = "Missing base URL, API key, or model"
                return@launch
            }
            val config = RemoteLLMConfig(
                baseUrl = current.remoteBaseUrl,
                apiKey = apiKey,
                model = current.remoteModel,
                timeoutMillis = current.remoteTimeoutMillis
            )
            when (val result = OpenAICompatibleAdapter().testConnection(config)) {
                is OperationResult.Success -> _testConnectionResult.value = "Connected"
                is OperationResult.Failure -> _testConnectionResult.value = "Failed: ${result.message}"
                is OperationResult.Cancelled -> _testConnectionResult.value = "Cancelled"
            }
        }
    }
}
