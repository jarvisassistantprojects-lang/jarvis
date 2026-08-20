package com.jarvis.core.domain.model

/** Events that drive transitions in [JarvisState]. Emitted by the voice pipeline, the LLM
 *  router, the action engine, or the user (cancel). The coordinator is the single place
 *  that folds these into state transitions. */
sealed class JarvisEvent {
    data object WakeDetected : JarvisEvent()
    data object PromptCompleted : JarvisEvent()
    data class PartialTranscript(val text: String) : JarvisEvent()
    data class FinalTranscript(val text: String) : JarvisEvent()
    data class CancelPhraseDetected(val phrase: String) : JarvisEvent()
    data class ListeningTimedOut(val reason: String) : JarvisEvent()
    data class ActionResolved(val actionType: String) : JarvisEvent()
    data class ActionSucceeded(val summary: String) : JarvisEvent()
    data class ActionFailed(val reason: String, val category: ErrorCategory) : JarvisEvent()
    data object UserCancelled : JarvisEvent()
    data object ResetToIdle : JarvisEvent()
}
