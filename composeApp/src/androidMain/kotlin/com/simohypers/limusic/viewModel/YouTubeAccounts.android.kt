package com.simohypers.limusic.viewModel

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform.getKoin
import kotlin.time.Duration.Companion.seconds

/**
 * A detached WebView, never shown: it shares the sign-in WebView's cookie store, and YouTube's own
 * page load is what makes Google hand out the rotated cookies. Its default user agent, as the
 * sign-in WebView has, so the jar never sees a second browser.
 */
internal actual suspend fun freshYouTubeJar(current: String): String? =
    withContext(Dispatchers.Main) {
        val account = sapisid(current) ?: return@withContext null
        val context: Context = getKoin().get()
        val web =
            runCatching { WebView(context) }.getOrElse {
                Logger.w("Auth", "YouTube: no WebView for the session refresh")
                return@withContext null
            }
        try {
            val loaded = CompletableDeferred<Unit>()
            web.settings.javaScriptEnabled = true
            web.settings.domStorageEnabled = true
            web.webViewClient =
                object : WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?,
                    ) {
                        if (url?.startsWith(Config.YOUTUBE_MUSIC_MAIN_URL) == true) loaded.complete(Unit)
                    }
                }
            web.loadUrl(Config.YOUTUBE_MUSIC_MAIN_URL)
            if (withTimeoutOrNull(45.seconds) { loaded.await() } == null) return@withContext null
            // Landing on the page and having rotated are not the same instant.
            repeat(8) {
                val jar = CookieManager.getInstance().getCookie(Config.YOUTUBE_MUSIC_MAIN_URL).orEmpty()
                if (sapisid(jar) == account && !sameJar(jar, current)) return@withContext jar
                delay(500)
            }
            null
        } finally {
            web.stopLoading()
            web.destroy()
        }
    }
