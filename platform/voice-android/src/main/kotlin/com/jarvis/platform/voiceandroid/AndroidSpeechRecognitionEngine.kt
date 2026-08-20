package com.jarvis.platform.voiceandroid

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult
import com.jarvis.core.voice.AudioOwner
import com.jarvis.core.voice.AudioSessionCoordinator
import com.jarvis.core.voice.SpeechFailure
import com.jarvis.core.voice.SpeechRecognitionEngine
import com.jarvis.core.voice.SpeechResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Wraps Android's single-shot SpeechRecognizer. Per section 1.3/4, EXTRA_PREFER_OFFLINE is
 * requested but not guaranteed by every OEM, and there is no supported way to feed it a
 * pre-recorded rolling buffer — this can only ever recognize what is spoken after it starts.
 */
class AndroidSpeechRecognitionEngine(
    private val context: Context,
    private val audioSession: AudioSessionCoordinator
) : SpeechRecognitionEngine {

    private var recognizer: SpeechRecognizer? = null

    override suspend fun recognizeOnce(
        languageTag: String,
        timeoutMillis: Long,
        onPartial: (String) -> Unit
    ): OperationResult<SpeechResult> {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return OperationResult.Failure("No speech recognition service available on this device", ErrorCategory.SPEECH_RECOGNITION_FAILED)
        }
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return OperationResult.Failure("RECORD_AUDIO permission missing", ErrorCategory.PERMISSION_MISSING)
        }
        if (!audioSession.acquire(AudioOwner.SPEECH_RECOGNITION)) {
            return OperationResult.Failure("Could not acquire microphone", ErrorCategory.SPEECH_RECOGNITION_FAILED)
        }

        return try {
            withTimeout(timeoutMillis) {
                listenOnce(languageTag, onPartial)
            }
        } catch (e: TimeoutCancellationException) {
            OperationResult.Failure("Speech recognition timed out", ErrorCategory.SPEECH_TIMEOUT)
        } finally {
            audioSession.release(AudioOwner.SPEECH_RECOGNITION)
            withContext(Dispatchers.Main.immediate) { recognizer?.destroy() }
            recognizer = null
        }
    }

    private suspend fun listenOnce(
        languageTag: String,
        onPartial: (String) -> Unit
    ): OperationResult<SpeechResult> = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle) {
                val text = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) onPartial(text)
            }

            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (cont.isActive) {
                    if (text.isNullOrBlank()) {
                        cont.resume(OperationResult.Failure("No speech recognized", ErrorCategory.SPEECH_RECOGNITION_FAILED))
                    } else {
                        cont.resume(OperationResult.Success(SpeechResult(text, isFinal = true)))
                    }
                }
            }

            override fun onError(error: Int) {
                if (!cont.isActive) return
                val failure = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> SpeechFailure.NoMatch
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechFailure.Timeout
                    SpeechRecognizer.ERROR_AUDIO -> SpeechFailure.AudioError
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechFailure.PermissionMissing
                    else -> SpeechFailure.Other("SpeechRecognizer error code $error")
                }
                cont.resume(OperationResult.Failure(describe(failure), ErrorCategory.SPEECH_RECOGNITION_FAILED))
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
 
        }
        sr.startListening(intent)

        cont.invokeOnCancellation { sr.cancel() }
         }
    }

    override suspend fun cancel() {
        withContext(Dispatchers.Main.immediate) { recognizer?.cancel() }
        audioSession.release(AudioOwner.SPEECH_RECOGNITION)
    }

    private fun describe(failure: SpeechFailure): String = when (failure) {
        SpeechFailure.NoMatch -> "No speech match"
        SpeechFailure.Timeout -> "Speech timeout"
        SpeechFailure.AudioError -> "Audio recording error"
        SpeechFailure.PermissionMissing -> "Microphone permission missing"
        is SpeechFailure.Other -> failure.message
    }
}
