package com.jarvis.core.voice

import com.jarvis.core.domain.model.OperationResult

/**
 * Abstraction over speech-to-text. Milestone 1 backs this with Android's SpeechRecognizer;
 * it is intentionally a single-shot ("listen once, return one final result") contract because
 * that is what SpeechRecognizer reliably offers — no rolling PCM buffer, no guaranteed offline
 * mode (see section 1.3 / 4).
 */
interface SpeechRecognitionEngine {

    /** Starts listening for a single utterance and suspends until a final result, a timeout,
     *  or a failure. [onPartial] is invoked (best-effort — some OEMs never deliver partials)
     *  so the UI can show live partials while state is LISTENING. */
    suspend fun recognizeOnce(
        languageTag: String,
        timeoutMillis: Long,
        onPartial: (String) -> Unit = {}
    ): OperationResult<SpeechResult>

    /** Cancels an in-progress recognition immediately and releases the microphone. */
    suspend fun cancel()
}
