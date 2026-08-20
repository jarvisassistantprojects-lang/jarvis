package com.jarvis.core.voice

/** Abstraction over text-to-speech. [speak] must suspend until playback actually completes
 *  (via the platform's utterance-completion callback), never a fixed delay — the PROMPTING
 *  -> LISTENING transition depends on this being accurate so SpeechRecognizer isn't started
 *  while TTS audio is still playing (which would otherwise re-trigger the wake word). */
interface TTSEngine {
    suspend fun speak(text: String, rate: Float = 1.0f)
    suspend fun stop()
}
