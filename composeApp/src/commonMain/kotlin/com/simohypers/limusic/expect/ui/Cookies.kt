package com.simohypers.limusic.expect.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

interface WebViewCookieManager {
    fun getCookie(url: String): String

    /** Clears the WebView's whole cookie store; [done] runs once it is really gone. */
    fun removeAllCookies(done: () -> Unit = {})
}

expect fun createWebViewCookieManager(): WebViewCookieManager

sealed class WebViewState {
    data class Loading(
        val progress: Int,
    ) : WebViewState()

    object Finished : WebViewState()
}

@Composable
fun rememberWebViewState(): MutableState<WebViewState> =
    remember {
        mutableStateOf(WebViewState.Loading(0))
    }

@Composable
expect fun PlatformWebView(
    state: MutableState<WebViewState> = rememberWebViewState(),
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit) = {},
    onPageFinished: (String) -> Unit,
)
