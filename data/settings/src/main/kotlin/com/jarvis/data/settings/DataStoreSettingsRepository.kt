package com.jarvis.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/** Backs [JarvisSettingsRepository] with Preferences DataStore. Only non-sensitive settings
 *  live here (section 15/16) — API keys and the Porcupine AccessKey are stored via
 *  data:security's AndroidKeystoreSecretStore, never in these preferences. */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : JarvisSettingsRepository {

    private object Keys {
        val LLM_MODE = stringPreferencesKey("llm_mode")
        val ALLOW_AUTO_FALLBACK = booleanPreferencesKey("allow_auto_remote_fallback")
        val REMOTE_BASE_URL = stringPreferencesKey("remote_base_url")
        val REMOTE_MODEL = stringPreferencesKey("remote_model")
        val REMOTE_TIMEOUT_MS = longPreferencesKey("remote_timeout_ms")
        val SPEECH_LANGUAGE = stringPreferencesKey("speech_language_tag")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val EVENT_LOGGING = booleanPreferencesKey("event_logging_enabled")
        val TRANSCRIPT_LOGGING = booleanPreferencesKey("transcript_logging_enabled")
    }

    override val settings: Flow<JarvisSettings> = dataStore.data.map { prefs ->
        JarvisSettings(
            llmMode = prefs[Keys.LLM_MODE] ?: "AUTO",
            allowAutoRemoteFallback = prefs[Keys.ALLOW_AUTO_FALLBACK] ?: false,
            remoteBaseUrl = prefs[Keys.REMOTE_BASE_URL] ?: "",
            remoteModel = prefs[Keys.REMOTE_MODEL] ?: "",
            remoteTimeoutMillis = prefs[Keys.REMOTE_TIMEOUT_MS] ?: 60_000L,
            speechLanguageTag = prefs[Keys.SPEECH_LANGUAGE] ?: "en-US",
            ttsRate = prefs[Keys.TTS_RATE] ?: 1.0f,
            eventLoggingEnabled = prefs[Keys.EVENT_LOGGING] ?: true,
            transcriptLoggingEnabled = prefs[Keys.TRANSCRIPT_LOGGING] ?: false
        )
    }

    override suspend fun update(transform: (JarvisSettings) -> JarvisSettings) {
        val current = settings.first()
        val next = transform(current)
        dataStore.edit { prefs ->
            prefs[Keys.LLM_MODE] = next.llmMode
            prefs[Keys.ALLOW_AUTO_FALLBACK] = next.allowAutoRemoteFallback
            prefs[Keys.REMOTE_BASE_URL] = next.remoteBaseUrl
            prefs[Keys.REMOTE_MODEL] = next.remoteModel
            prefs[Keys.REMOTE_TIMEOUT_MS] = next.remoteTimeoutMillis
            prefs[Keys.SPEECH_LANGUAGE] = next.speechLanguageTag
            prefs[Keys.TTS_RATE] = next.ttsRate
            prefs[Keys.EVENT_LOGGING] = next.eventLoggingEnabled
            prefs[Keys.TRANSCRIPT_LOGGING] = next.transcriptLoggingEnabled
        }
    }
}
