package com.jarvis.core.voice

/** Result of a wake-word engine start attempt. Engines that need a licensed SDK (Porcupine)
 *  must be able to report [Unavailable] instead of silently no-op'ing, per section 1.5. */
sealed class WakeWordAvailability {
    data object Available : WakeWordAvailability()
    data class Unavailable(val reason: String) : WakeWordAvailability()
}

data class SpeechResult(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float? = null
)

sealed class SpeechFailure {
    data object NoMatch : SpeechFailure()
    data object Timeout : SpeechFailure()
    data object AudioError : SpeechFailure()
    data object PermissionMissing : SpeechFailure()
    data class Other(val message: String) : SpeechFailure()
}

/** Which logical consumer currently owns the microphone. [AudioSessionCoordinator]
 *  guarantees only one is active at a time (section "Voice" / AudioSessionCoordinator). */
enum class AudioOwner { NONE, WAKE_WORD, SPEECH_RECOGNITION, TTS_PLAYBACK }
