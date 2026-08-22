package com.jarvis.app

import android.content.Context
import android.provider.Settings
import com.jarvis.core.llm.LLMProviderRouter
import com.jarvis.data.logging.EventLogger
import com.jarvis.data.logging.LocalEventLogger
import com.jarvis.data.security.AndroidKeystoreSecretStore
import com.jarvis.data.security.SecretStore
import com.jarvis.data.settings.DataStoreSettingsRepository
import com.jarvis.data.settings.JarvisSettingsRepository
import com.jarvis.platform.androidcontrol.apps.AppCatalog
import com.jarvis.platform.androidcontrol.apps.OpenAppExecutor
import com.jarvis.platform.voiceandroid.AndroidAudioSessionCoordinator
import com.jarvis.platform.voiceandroid.AndroidSpeechRecognitionEngine
import com.jarvis.platform.voiceandroid.SherpaTTSEngine
import com.jarvis.platform.voiceandroid.VoskWakeWordEngine
import com.jarvis.providers.localllm.LocalLLMProvider
import com.jarvis.providers.localllm.UnavailableLocalInferenceBackend
import com.jarvis.providers.remotellm.OpenAICompatibleAdapter
import com.jarvis.providers.remotellm.RemoteLLMConfig
import com.jarvis.providers.remotellm.RemoteLLMProvider
import com.jarvis.core.actions.execution.ActionEngine
import kotlinx.coroutines.flow.first

class JarvisAppContainer(private val context: Context) {

    val secretStore: SecretStore = AndroidKeystoreSecretStore(context)
    val settingsRepository: JarvisSettingsRepository = DataStoreSettingsRepository(context.jarvisDataStore)
    val eventLogger: EventLogger = LocalEventLogger()

    val audioSessionCoordinator = AndroidAudioSessionCoordinator(context)

    val ttsEngine = SherpaTTSEngine(context, audioSessionCoordinator)
    val speechRecognitionEngine = AndroidSpeechRecognitionEngine(context, audioSessionCoordinator)

    val wakeWordEngine = VoskWakeWordEngine(
        context = context,
        audioSession = audioSessionCoordinator
    )

    val appCatalog = AppCatalog(context)

    var isAppForeground: Boolean = false

    val openAppExecutor = OpenAppExecutor(
        context = context,
        appCatalog = appCatalog,
        isCallerForeground = { isAppForeground || Settings.canDrawOverlays(context) }
    )

    val localLLMProvider = LocalLLMProvider(UnavailableLocalInferenceBackend())

    val remoteLLMProvider = RemoteLLMProvider(
        configProvider = {
            val settings = settingsRepository.settings.first()
            val apiKey = secretStore.get(SecretStore.KEY_REMOTE_API_KEY)
            if (settings.remoteBaseUrl.isBlank() || apiKey.isNullOrBlank() || settings.remoteModel.isBlank()) {
                null
            } else {
                RemoteLLMConfig(
                    baseUrl = settings.remoteBaseUrl,
                    apiKey = apiKey,
                    model = settings.remoteModel,
                    timeoutMillis = settings.remoteTimeoutMillis
                )
            }
        },
        adapter = OpenAICompatibleAdapter()
    )

    val llmProviderRouter = LLMProviderRouter(localLLMProvider, remoteLLMProvider)

    val actionEngine = ActionEngine(executors = listOf(openAppExecutor))
}
