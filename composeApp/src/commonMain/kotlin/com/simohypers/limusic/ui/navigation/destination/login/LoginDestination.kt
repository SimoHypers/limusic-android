package com.simohypers.limusic.ui.navigation.destination.login

import kotlinx.serialization.Serializable

/** [addAccount] signs the WebView out of Google first, so a different account can be entered. */
@Serializable
data class LoginDestination(
    val addAccount: Boolean = false,
)
