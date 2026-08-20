package com.jarvis.core.domain.model

/**
 * The Milestone 1 state machine, per JARVIS technical review section 6 ("State Machine").
 *
 * IDLE -> PROMPTING -> LISTENING -> THINKING -> EXECUTING -> {SUCCESS|ERROR|CANCELLED} -> IDLE
 *
 * PROMPTING exists because TTS ("Yes?") must fully complete before SpeechRecognizer starts;
 * Android's SpeechRecognizer cannot reliably consume a rolling audio buffer, so the wake word
 * and the command can never be resolved from a single utterance in Milestone 1.
 */
sealed class JarvisState {
    data object Idle : JarvisState()
    data object Prompting : JarvisState()
    data class Listening(val partialTranscript: String = "") : JarvisState()
    data class Thinking(val transcript: String) : JarvisState()
    data class Executing(val action: String) : JarvisState()
    data class Success(val summary: String) : JarvisState()
    data class Error(val reason: String, val category: ErrorCategory) : JarvisState()
    data object Cancelled : JarvisState()
}

enum class ErrorCategory {
    WAKE_WORD_UNAVAILABLE,
    SPEECH_RECOGNITION_FAILED,
    SPEECH_TIMEOUT,
    PROVIDER_UNAVAILABLE,
    PROVIDER_TIMEOUT,
    INVALID_ACTION_RESPONSE,
    ACTION_NOT_REGISTERED,
    ACTION_VALIDATION_FAILED,
    ACTION_EXECUTION_FAILED,
    PERMISSION_MISSING,
    UNKNOWN
}
