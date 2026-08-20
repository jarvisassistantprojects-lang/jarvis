package com.jarvis.core.voice

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the wake-word detector so that core/coordinator code never depends on
 * Porcupine (or any other concrete SDK) directly — see "principle of dependency" in the
 * technical review. The Android-backed implementation lives in platform/voice-android.
 */
interface WakeWordEngine {

    /** Emits Unit each time the configured wake word ("Jarvis") is detected. The engine must
     *  stop listening internally as soon as it detects a wake word (it does not own the mic
     *  again until [resume] is called) so that SpeechRecognition/TTS can take over. */
    val detections: Flow<Unit>

    /** Attempts to start continuous detection. Returns [WakeWordAvailability.Unavailable] if
     *  the engine cannot run (missing AccessKey, missing keyword file, SDK init failure, no
     *  RECORD_AUDIO permission) rather than failing silently. */
    suspend fun start(): WakeWordAvailability

    /** Stops detection and releases the microphone. Must be safe to call multiple times. */
    suspend fun stop()

    /** Resumes detection after it was paused for TTS/STT (see AudioSessionCoordinator). */
    suspend fun resume(): WakeWordAvailability
}
