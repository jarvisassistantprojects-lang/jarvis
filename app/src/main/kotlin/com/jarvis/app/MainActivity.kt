package com.jarvis.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.app.service.JarvisVoiceService
import com.jarvis.app.ui.JarvisRoot

/**
 * Section 1.2's Milestone 1 acceptance bar for open_app depends on this activity accurately
 * reporting when it is actually foreground-visible — see onStart/onStop below, which flip
 * [JarvisAppContainer.isAppForeground]. Without this flag correctly tracked, OpenAppExecutor
 * will (correctly) refuse to launch apps rather than risk a silently-dropped background
 * activity launch.
 */
class MainActivity : ComponentActivity() {

    private val container by lazy { (application as JarvisApplication).container }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* PermissionCoordinator.status() is re-read by the UI on next recomposition */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        setContent {
            JarvisRoot(
                container = container,
                onStartListening = { startVoiceService() },
                onStopListening = { stopVoiceService() }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        container.isAppForeground = true
    }

    override fun onStop() {
        container.isAppForeground = false
        super.onStop()
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startVoiceService() {
        val intent = Intent(this, JarvisVoiceService::class.java)
        startForegroundService(intent)
    }

    private fun stopVoiceService() {
        val intent = Intent(this, JarvisVoiceService::class.java).setAction(JarvisVoiceService.ACTION_STOP)
        startService(intent)
    }
}
