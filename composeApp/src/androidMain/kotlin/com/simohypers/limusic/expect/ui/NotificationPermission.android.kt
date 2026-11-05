package com.simohypers.limusic.expect.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

@Composable
actual fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): NotificationPermission {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { currentOnResult(it) }
    return remember(context, launcher) {
        object : NotificationPermission {
            override val granted get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

            // ponytail: after two refusals Android stops showing the dialog and this answers false at
            // once; the app's notification settings page is the way back if that ever matters
            override fun request() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    currentOnResult(granted)
                }
            }
        }
    }
}
