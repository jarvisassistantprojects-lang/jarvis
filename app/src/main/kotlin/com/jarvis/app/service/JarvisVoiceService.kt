package com.jarvis.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.app.JarvisApplication
import com.jarvis.app.MainActivity
import com.jarvis.app.R
import com.jarvis.app.coordinator.JarvisCoordinator
import com.jarvis.core.domain.model.JarvisState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Foreground microphone service, per section 9/13/15: exported=false, type="microphone",
 * only ever started by an explicit user action from the visible JARVIS screen (never from a
 * boot receiver in Milestone 1), and carries a persistent notification with a Stop action
 * that fully releases wake word / STT / TTS / the current job (section 14).
 *
 * The notification text is now driven by coordinator.state (see observeState()) instead of a
 * static string, so real failures (e.g. the Vosk wake-word model failing to load) are visible
 * to the user in the notification tray even without a debugger attached.
 */
class JarvisVoiceService : Service() {

    companion object {
        const val ACTION_STOP = "com.jarvis.app.action.STOP"
        private const val CHANNEL_ID = "jarvis_voice_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob())
    private lateinit var coordinator: JarvisCoordinator

    override fun onCreate() {
        super.onCreate()
        val container = (application as JarvisApplication).container
        coordinator = JarvisCoordinator(container, serviceScope)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfCompletely()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(JarvisState.Idle))
        observeState()
        coordinator.start()
        return START_NOT_STICKY // section 9: no auto-restart-from-boot semantics in Milestone 1
    }

    private fun observeState() {
        coordinator.state
            .onEach { state -> updateNotification(state) }
            .launchIn(serviceScope)
    }

    private fun updateNotification(state: JarvisState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun stopSelfCompletely() {
        coordinator.stop()
        serviceScope.launch {
            // give in-flight cancellation a moment to release the mic cleanly
            kotlinx.coroutines.delay(100)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        coordinator.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun statusTextFor(state: JarvisState): String = when (state) {
        is JarvisState.Idle -> getString(R.string.notification_listening)
        is JarvisState.Prompting -> "Yes?"
        is JarvisState.Listening ->
            if (state.partialTranscript.isBlank()) "Listening..." else "Listening: ${state.partialTranscript}"
        is JarvisState.Thinking -> "Thinking: ${state.transcript}"
        is JarvisState.Executing -> "Running: ${state.action}"
        is JarvisState.Success -> state.summary
        is JarvisState.Error -> "Error (${state.category}): ${state.reason}"
        is JarvisState.Cancelled -> "Cancelled"
    }

    private fun buildNotification(state: JarvisState): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, JarvisVoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusTextFor(state))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_stop_action), stopIntent)
            .build()
    }
}