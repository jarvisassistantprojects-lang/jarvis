package com.jarvis.core.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * Guarantees exactly one of {wake word, speech recognition, TTS} owns the microphone / audio
 * focus at a time, and mediates the handoffs described in section 1.3 and section 4:
 * wake word must fully release the mic before TTS speaks, TTS must fully finish before
 * SpeechRecognizer starts, and the wake-word detector must stay paused during both so it does
 * not self-trigger on JARVIS's own voice.
 */
interface AudioSessionCoordinator {

    val currentOwner: StateFlow<AudioOwner>

    /** Requests ownership for [owner]. Suspends until any current owner has released.
     *  Returns false if ownership could not be acquired (e.g. audio focus denied by system). */
    suspend fun acquire(owner: AudioOwner): Boolean

    /** Releases ownership if [owner] currently holds it; no-op otherwise. */
    suspend fun release(owner: AudioOwner)
}
