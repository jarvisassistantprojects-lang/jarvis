package com.jarvis.platform.voiceandroid

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.jarvis.core.voice.AudioOwner
import com.jarvis.core.voice.AudioSessionCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Real AudioManager-backed implementation. All acquire/release calls go through a single
 * mutex so wake word, STT, and TTS can never simultaneously believe they own the mic —
 * directly addressing the "audio focus conflict" risk in section 4.
 */
class AndroidAudioSessionCoordinator(context: Context) : AudioSessionCoordinator {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutex = Mutex()
    private val _currentOwner = MutableStateFlow(AudioOwner.NONE)
    override val currentOwner: StateFlow<AudioOwner> = _currentOwner

    private var focusRequest: AudioFocusRequest? = null

    override suspend fun acquire(owner: AudioOwner): Boolean = mutex.withLock {
        if (_currentOwner.value != AudioOwner.NONE) {
            // Caller is expected to have released the previous owner already; refuse to
            // silently steal ownership.
            return@withLock false
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()
        val granted = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) {
            focusRequest = request
            _currentOwner.value = owner
        }
        granted
    }

    override suspend fun release(owner: AudioOwner) = mutex.withLock {
        if (_currentOwner.value != owner) return@withLock
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
        _currentOwner.value = AudioOwner.NONE
    }
}
