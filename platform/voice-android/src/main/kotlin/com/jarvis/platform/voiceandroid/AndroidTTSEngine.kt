package com.jarvis.platform.voiceandroid

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.core.voice.AudioOwner
import com.jarvis.core.voice.AudioSessionCoordinator
import com.jarvis.core.voice.TTSEngine
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class AndroidTTSEngine(
    context: Context,
    private val audioSession: AudioSessionCoordinator
) : TTSEngine {

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext

    private suspend fun ensureInitialized(): TextToSpeech = suspendCancellableCoroutine { cont ->
        if (tts != null) {
            cont.resume(tts!!)
            return@suspendCancellableCoroutine
        }
        var engine: TextToSpeech? = null
        engine = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts = engine
                cont.resume(engine!!)
            } else {
                cont.resume(engine!!) // caller will fail on speak() if broken; avoids crashing here
            }
        }
    }

    override suspend fun speak(text: String, rate: Float) {
        if (!audioSession.acquire(AudioOwner.TTS_PLAYBACK)) return
        try {
            val engine = ensureInitialized()
            engine.setSpeechRate(rate)
            val utteranceId = UUID.randomUUID().toString()
            suspendCancellableCoroutine<Unit> { cont ->
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        if (id == utteranceId && cont.isActive) cont.resume(Unit)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (id == utteranceId && cont.isActive) cont.resume(Unit)
                    }
                })
                val params = android.os.Bundle()
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                cont.invokeOnCancellation { engine.stop() }
            }
        } finally {
            audioSession.release(AudioOwner.TTS_PLAYBACK)
        }
    }

    override suspend fun stop() {
        tts?.stop()
        audioSession.release(AudioOwner.TTS_PLAYBACK)
    }
}
