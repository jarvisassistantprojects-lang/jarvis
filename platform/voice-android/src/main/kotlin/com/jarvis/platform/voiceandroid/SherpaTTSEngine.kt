package com.jarvis.platform.voiceandroid

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.jarvis.core.voice.AudioOwner
import com.jarvis.core.voice.AudioSessionCoordinator
import com.jarvis.core.voice.TTSEngine
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline, natural-sounding TTS using Piper's "amy-medium" voice via sherpa-onnx.
 * Replaces the robotic default Android TTS. Model files must be bundled at
 * app/src/main/assets/tts-voice/ (see setup instructions).
 */
class SherpaTTSEngine(
    private val context: Context,
    private val audioSession: AudioSessionCoordinator
) : TTSEngine {

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null

    private fun ensureLoaded(): OfflineTts {
        tts?.let { return it }
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "tts-voice/en_US-amy-medium.onnx",
                    tokens = "tts-voice/tokens.txt",
                    dataDir = "tts-voice/espeak-ng-data"
                ),
                numThreads = 2,
                debug = false
            )
        )
        val newTts = OfflineTts(assetManager = context.assets, config = config)
        tts = newTts
        return newTts
    }

    override suspend fun speak(text: String, rate: Float) {
        if (!audioSession.acquire(AudioOwner.TTS_PLAYBACK)) return
        try {
            withContext(Dispatchers.Default) {
                val engine = ensureLoaded()
                val audio: GeneratedAudio = engine.generate(text = text, sid = 0, speed = rate)
                playPcm(audio.samples, audio.sampleRate)
            }
        } finally {
            audioSession.release(AudioOwner.TTS_PLAYBACK)
        }
    }

    private fun playPcm(samples: FloatArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, samples.size * 4))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        track.play()

        val durationMs = (samples.size.toFloat() / sampleRate * 1000).toLong()
        Thread.sleep(durationMs)
        track.stop()
        track.release()
        audioTrack = null
    }

    override suspend fun stop() {
        audioTrack?.let {
            it.stop()
            it.release()
        }
        audioTrack = null
        audioSession.release(AudioOwner.TTS_PLAYBACK)
    }
}
