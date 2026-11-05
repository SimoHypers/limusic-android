package com.simohypers.limusic.expect.ui

import androidx.compose.runtime.Composable

interface NotificationPermission {
    /** Notifications reach the user: the Android 13+ permission and the app's system switch. Read fresh each time. */
    val granted: Boolean

    /** Android's permission dialog; its answer goes to the callback the permission was built with. */
    fun request()
}

@Composable
expect fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): NotificationPermission
