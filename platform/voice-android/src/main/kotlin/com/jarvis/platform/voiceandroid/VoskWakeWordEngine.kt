package com.jarvis.platform.voiceandroid

import android.content.Context
import android.util.Log
import com.jarvis.core.voice.AudioOwner
import com.jarvis.core.voice.AudioSessionCoordinator
import com.jarvis.core.voice.WakeWordAvailability
import com.jarvis.core.voice.WakeWordEngine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Free, offline, no-account wake word detection using Vosk (replaces Porcupine, which now
 * requires a company email to sign up). A tiny grammar constrains recognition to
 * "jarvis" / anything-else; any hypothesis containing "jarvis" counts as a wake detection.
 * Requires a Vosk model bundled at app/src/main/assets/model (see setup instructions).
 */
class VoskWakeWordEngine(
    private val context: Context,
    private val audioSession: AudioSessionCoordinator
) : WakeWordEngine {

    private var speechService: SpeechService? = null
    private var pendingSend: ((Unit) -> Unit)? = null

    override val detections: Flow<Unit> = callbackFlow {
        pendingSend = { trySend(Unit) }
        awaitClose { pendingSend = null }
    }

    override suspend fun start(): WakeWordAvailability {
        if (!audioSession.acquire(AudioOwner.WAKE_WORD)) {
            return WakeWordAvailability.Unavailable("Microphone unavailable (another owner active)")
        }
        return try {
            val model = loadModel()
            val recognizer = Recognizer(model, 16000.0f, "[\"jarvis\", \"[unk]\"]")
            val service = SpeechService(recognizer, 16000.0f)
            speechService = service
            service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) = checkHypothesis(hypothesis)
                override fun onResult(hypothesis: String?) = checkHypothesis(hypothesis)
                override fun onFinalResult(hypothesis: String?) = checkHypothesis(hypothesis)
                override fun onError(exception: Exception?) {}
                override fun onTimeout() {}
            })
            WakeWordAvailability.Available
        } catch (e: Exception) {
            Log.e("JarvisVosk", "Vosk init failed", e)
            audioSession.release(AudioOwner.WAKE_WORD)
            WakeWordAvailability.Unavailable("Vosk init failed: ${e.message}")
        }
    }

    private fun checkHypothesis(hypothesis: String?) {
        if (hypothesis == null) return
        val text = try {
            JSONObject(hypothesis).optString("text", "") + JSONObject(hypothesis).optString("partial", "")
        } catch (e: Exception) {
            ""
        }
        if (text.contains("jarvis", ignoreCase = true)) {
            pendingSend?.invoke(Unit)
        }
    }

    private suspend fun loadModel(): Model = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        StorageService.unpack(
            context, "model", "model",
            { model -> cont.resume(model) {} },
            { exception -> cont.resumeWith(Result.failure(exception)) }
        )
    }

    override suspend fun stop() {
        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (_: Exception) {
        } finally {
            speechService = null
            audioSession.release(AudioOwner.WAKE_WORD)
        }
    }

    override suspend fun resume(): WakeWordAvailability = start()
}