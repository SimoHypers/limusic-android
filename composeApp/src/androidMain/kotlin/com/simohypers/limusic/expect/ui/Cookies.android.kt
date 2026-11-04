package com.simohypers.limusic.expect.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.simohypers.limusic.logger.Logger
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.unavailable

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        // getCookie is null when no cookie matches this url, even if hasCookies() is true for others.
        override fun getCookie(url: String): String = CookieManager.getInstance().getCookie(url).orEmpty()

        override fun removeAllCookies(done: () -> Unit) {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                done()
            }
        }
    }

@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    val unavailable = stringResource(Res.string.unavailable)
    Box {
        AndroidView(
            factory = { ctx ->
                webViewOrFallback(ctx, unavailable) {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                url?.let {
                                    onPageFinished(it)
                                }
                            }
                        }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    loadUrl(initUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        aboveContent()
    }
}

// Devices without a working WebView provider (watches, a WebView mid-update) throw from the constructor.
private fun webViewOrFallback(
    context: Context,
    unavailable: String,
    setup: WebView.() -> Unit,
): View =
    runCatching { WebView(context).apply(setup) }.getOrElse { e ->
        Logger.e("PlatformWebView", "WebView unavailable", e)
        TextView(context).apply {
            text = unavailable
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
    }
