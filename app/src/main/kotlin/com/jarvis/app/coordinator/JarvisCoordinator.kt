package com.jarvis.app.coordinator

import com.jarvis.app.JarvisAppContainer
import com.jarvis.core.actions.validation.AppCandidate
import com.jarvis.core.domain.cancellation.OperationController
import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.JarvisState
import com.jarvis.core.domain.model.OperationResult
import com.jarvis.core.llm.LLMCandidate
import com.jarvis.core.llm.LLMMode
import com.jarvis.core.llm.LLMRequest
import com.jarvis.core.voice.WakeWordAvailability
import com.jarvis.data.logging.JarvisEventRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private val CANCEL_PHRASES = setOf("cancel", "stop", "never mind", "nevermind")
private const val FOLLOW_UP_TIMEOUT_MS = 8_000L

/**
 * Implements the IDLE -> PROMPTING -> LISTENING -> THINKING -> EXECUTING -> {SUCCESS|ERROR|
 * CANCELLED} -> IDLE state machine, plus a "continuous conversation" follow-up: after any
 * completed command, instead of resetting to IDLE, the coordinator listens again directly
 * (no wake word needed) for a short window so multiple commands can be given back-to-back,
 * similar to Bixby/Assistant follow-up mode.
 */
class JarvisCoordinator(
    private val container: JarvisAppContainer,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<JarvisState>(JarvisState.Idle)
    val state: StateFlow<JarvisState> = _state

    private val operationController = OperationController(scope)

    fun start() {
        container.wakeWordEngine.detections
            .onEach { onWakeDetected() }
            .launchIn(scope)

        launchWakeWord()
    }

    private fun launchWakeWord() {
        operationController.launchExclusive { isCurrent ->
            val availability = container.wakeWordEngine.start()
            if (!isCurrent()) return@launchExclusive
            if (availability is WakeWordAvailability.Unavailable) {
                _state.value = JarvisState.Error(availability.reason, ErrorCategory.WAKE_WORD_UNAVAILABLE)
            }
        }
    }

    fun stop() {
        operationController.cancelCurrent()
        scope.launch { container.wakeWordEngine.stop() }
        _state.value = JarvisState.Idle
    }

    fun cancelCurrentOperation() {
        operationController.cancelCurrent()
        scope.launch {
            container.speechRecognitionEngine.cancel()
            container.ttsEngine.stop()
        }
        _state.value = JarvisState.Cancelled
        scope.launch { resetToIdleAfterDelay() }
    }

    private fun onWakeDetected() {
        operationController.launchExclusive { isCurrent ->
            container.wakeWordEngine.stop() // release mic so TTS/STT can take over
            if (!isCurrent()) return@launchExclusive

            _state.value = JarvisState.Prompting
            container.ttsEngine.speak("Yes?")
            if (!isCurrent()) return@launchExclusive

            val settings = container.settingsRepository.settings.first()
            _state.value = JarvisState.Listening()
            val speechResult = container.speechRecognitionEngine.recognizeOnce(
                languageTag = settings.speechLanguageTag,
                timeoutMillis = 8_000,
                onPartial = { partial -> if (isCurrent()) _state.value = JarvisState.Listening(partial) }
            )
            if (!isCurrent()) return@launchExclusive

            when (speechResult) {
                is OperationResult.Success -> handleTranscript(speechResult.value.text, isCurrent)
                is OperationResult.Failure -> {
                    _state.value = JarvisState.Error(speechResult.message, speechResult.category)
                    logResult("LISTENING->ERROR", null, null, speechResult.message, speechResult.category)
                    resetToIdleAfterDelay()
                }
                is OperationResult.Cancelled -> { /* cancelCurrentOperation already set state */ }
            }

            // Only resume wake-word listening once the whole conversation (including any
            // follow-up turns) has ended and we're back to idle.
            container.wakeWordEngine.resume()
        }
    }

    private suspend fun handleTranscript(transcript: String, isCurrent: () -> Boolean) {
        val normalized = transcript.trim().lowercase()
        if (CANCEL_PHRASES.any { normalized == it || normalized.startsWith("$it ") }) {
            _state.value = JarvisState.Cancelled
            resetToIdleAfterDelay()
            return
        }

        _state.value = JarvisState.Thinking(transcript)

        // Anti-hallucination candidate narrowing (section 13): pull the likely app name out
        // of the transcript and resolve it to a short, real candidate list BEFORE calling
        // the LLM. We never send the full installed-app list.
        val spokenAppName = extractAppName(transcript)
        val candidates = container.appCatalog.findCandidates(spokenAppName)
        if (!isCurrent()) return

        if (candidates.isEmpty()) {
            _state.value = JarvisState.Error("No installed app matches \"$spokenAppName\"", ErrorCategory.ACTION_VALIDATION_FAILED)
            container.ttsEngine.speak("I couldn't find an app called $spokenAppName.")
            continueConversation(isCurrent)
            return
        }

        val settings = container.settingsRepository.settings.first()
        val mode = LLMMode.entries.firstOrNull { it.name == settings.llmMode } ?: LLMMode.AUTO

        val llmResult = container.llmProviderRouter.complete(
            request = LLMRequest(
                transcript = transcript,
                candidates = candidates.map { LLMCandidate(it.packageName, it.label) },
                systemPrompt = SystemPromptBuilder.build(),
                timeoutMillis = settings.remoteTimeoutMillis
            ),
            mode = mode,
            allowAutoRemoteFallback = settings.allowAutoRemoteFallback
        )
        if (!isCurrent()) return

        when (llmResult) {
            is OperationResult.Success -> executeAction(llmResult.value.rawJson, candidates, llmResult.value.providerId.name, isCurrent)
            is OperationResult.Failure -> {
                _state.value = JarvisState.Error(llmResult.message, llmResult.category)
                logResult("THINKING->ERROR", null, null, llmResult.message, llmResult.category)
                container.ttsEngine.speak("Sorry, something went wrong.")
                continueConversation(isCurrent)
            }
            is OperationResult.Cancelled -> { /* handled by cancelCurrentOperation */ }
        }
    }

    private suspend fun executeAction(
        rawJson: String,
        candidates: List<com.jarvis.platform.androidcontrol.apps.LaunchableApp>,
        providerUsed: String,
        isCurrent: () -> Boolean
    ) {
        _state.value = JarvisState.Executing("open_app")
        val appCandidates = candidates.map { AppCandidate(it.packageName, it.label) }
        val result = container.actionEngine.run(rawJson, appCandidates)
        if (!isCurrent()) return

        when (result) {
            is OperationResult.Success -> {
                _state.value = JarvisState.Success(result.value)
                container.ttsEngine.speak(result.value)
                logResult("EXECUTING->SUCCESS", providerUsed, "open_app", result.value, null)
            }
            is OperationResult.Failure -> {
                _state.value = JarvisState.Error(result.message, result.category)
                container.ttsEngine.speak("Sorry, that didn't work.")
                logResult("EXECUTING->ERROR", providerUsed, "open_app", result.message, result.category)
            }
            is OperationResult.Cancelled -> return
        }
        if (!isCurrent()) return
        continueConversation(isCurrent)
    }

    /**
     * Continuous-conversation follow-up: instead of resetting to IDLE after a command, listen
     * again directly (no "Yes?" prompt, no wake word needed) for a short window so the user
     * can chain another command immediately. Falls back to IDLE on silence/timeout.
     */
    private suspend fun continueConversation(isCurrent: () -> Boolean) {
        if (!isCurrent()) return
        val settings = container.settingsRepository.settings.first()
        _state.value = JarvisState.Listening()
        val result = container.speechRecognitionEngine.recognizeOnce(
            languageTag = settings.speechLanguageTag,
            timeoutMillis = FOLLOW_UP_TIMEOUT_MS,
            onPartial = { partial -> if (isCurrent()) _state.value = JarvisState.Listening(partial) }
        )
        if (!isCurrent()) return
        when (result) {
            is OperationResult.Success -> handleTranscript(result.value.text, isCurrent)
            is OperationResult.Failure -> resetToIdleAfterDelay()
            is OperationResult.Cancelled -> { /* handled by cancelCurrentOperation */ }
        }
    }

    private val PERSIAN_OPEN_SUFFIXES = listOf(
        " را باز کن", " رو باز کن", " را باز کن.", " رو باز کن.", " باز کن"
    )

    private fun extractAppName(transcript: String): String {
        val prefixes = listOf("open ", "launch ", "start ")
        val trimmed = transcript.trim()
        for (prefix in prefixes) {
            if (trimmed.lowercase().startsWith(prefix)) return trimmed.substring(prefix.length).trim()
        }
        for (suffix in PERSIAN_OPEN_SUFFIXES) {
            if (trimmed.endsWith(suffix)) return trimmed.removeSuffix(suffix).trim()
        }
        return trimmed
    }

    private suspend fun resetToIdleAfterDelay() {
        kotlinx.coroutines.delay(1_500)
        _state.value = JarvisState.Idle
    }

    private fun logResult(
        transition: String,
        provider: String?,
        actionType: String?,
        result: String,
        category: ErrorCategory?
    ) {
        scope.launch {
            container.eventLogger.log(
                JarvisEventRecord(
                    timestampMillis = System.currentTimeMillis(),
                    stateTransition = transition,
                    providerUsed = provider,
                    actionType = actionType,
                    result = result,
                    latencyMillis = null,
                    errorCategory = category?.name
                )
            )
        }
    }
}