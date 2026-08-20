package com.jarvis.data.settings

import kotlinx.coroutines.flow.Flow

/** Section 15 settings list, minus secrets (API key / Porcupine AccessKey live in
 *  data:security's SecretStore instead — never in DataStore Preferences in plaintext). */
data class JarvisSettings(
    val llmMode: String = "AUTO", // mirrors core.llm.LLMMode name to avoid a module dependency here
    val allowAutoRemoteFallback: Boolean = false,
    val remoteBaseUrl: String = "",
    val remoteModel: String = "",
    val remoteTimeoutMillis: Long = 60_000,
    val speechLanguageTag: String = "en-US",
    val ttsRate: Float = 1.0f,
    val eventLoggingEnabled: Boolean = true,
    val transcriptLoggingEnabled: Boolean = false // opt-in only, per section 16
)

interface JarvisSettingsRepository {
    val settings: Flow<JarvisSettings>
    suspend fun update(transform: (JarvisSettings) -> JarvisSettings)
}
