package com.jarvis.app.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val microphoneGranted: Boolean,
    val notificationsGranted: Boolean
)

/** Read-only permission status checks. Actual runtime permission requests are launched from
 *  MainActivity via the standard ActivityResultContracts API (kept in the Activity because
 *  that API requires an Activity/ComponentActivity host). */
class PermissionCoordinator(private val context: Context) {

    fun status(): PermissionStatus = PermissionStatus(
        microphoneGranted = isGranted(Manifest.permission.RECORD_AUDIO),
        notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // not required pre-Android 13
        }
    )

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
