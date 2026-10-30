package com.simohypers.limusic.kotlinytmusicscraper.extractor

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import kotlin.coroutines.resume

private const val TAG = "PoToken"

/**
 * PoTokens from YouTube's BotGuard, run in a hidden WebView. A port of NewPipe's PoTokenWebView
 * and po_token.html (GPL-3.0, TeamNewPipe), the recipe Metrolist uses too.
 *
 * Desktop Limusic measured every real browser engine into the integrity-token class googlevideo
 * refuses for an anonymous session (its KNOWN-ISSUES KI-1), and mints in V8 instead. This WebView
 * (153, on the owner's phone) landed in the accepted class, 63 bytes, on all four bootstraps of
 * 2026-10-26, and its tokens made WEB_REMIX play signed in where none did without. Each bootstrap
 * logs its class, so a change there shows up in the log.
 *
 * ponytail: no re-roll when a bootstrap lands in the rejected class (desktop tries up to 8), and
 * no reset when googlevideo refuses a token early; the token simply lives its 12 hours. Add both
 * if the log ever shows "rejected class" or WEB_REMIX failing with a PoToken.
 */
internal class WebPoTokenMinter(
    private val context: Context,
) {
    private val lock = Mutex()
    private val http = OkHttpClient()
    private var webView: WebView? = null
    private var sessionId: String? = null
    private var sessionToken: String? = null
    private var expiresAtMs = 0L
    private var integrityTokenSize = 0

    /**
     * The session token for the `/player` body and [videoId]'s token for `&pot=`, or null when
     * BotGuard cannot be run right now. Never throws: playback goes on without them.
     */
    suspend fun tokens(
        videoId: String,
        visitorData: String,
    ): WebPoTokens? =
        lock.withLock {
            try {
                withTimeout(BUDGET_MS) {
                    if (webView == null || sessionId != visitorData || System.currentTimeMillis() >= expiresAtMs) {
                        bootstrap(visitorData)
                    }
                    WebPoTokens(session = sessionToken!!, video = mint(videoId))
                }
            } catch (e: Exception) {
                // A timeout is ours to handle; any other cancellation belongs to the caller.
                if (e is kotlinx.coroutines.CancellationException && e !is TimeoutCancellationException) throw e
                Logger.w(TAG, "no PoToken: ${e.message}")
                close()
                null
            }
        }

    private suspend fun bootstrap(visitorData: String) {
        close()
        val bridge = Bridge()
        val view = withContext(Dispatchers.Main) { newWebView(bridge) }
        webView = view
        bridge.pageReady.await()

        val challenge = parseChallenge(post(CREATE_URL, "[\"$REQUEST_KEY\"]"))
        evaluate(
            view,
            """try {
                runBotGuard($challenge).then(function (r) {
                    window.webPoSignalOutput = r.webPoSignalOutput
                    $BRIDGE.onBotguard(r.botguardResponse)
                }, function (e) { $BRIDGE.onError(String(e)) })
            } catch (e) { $BRIDGE.onError(String(e)) }""",
        )
        val botguardResponse = bridge.botguard.await()

        val generated = Json.parseToJsonElement(post(GENERATE_IT_URL, "[\"$REQUEST_KEY\",\"$botguardResponse\"]")).jsonArray
        val integrityToken = youtubeBase64(generated[0].jsonPrimitive.content)
        val ttlSeconds = generated[1].jsonPrimitive.long
        // Desktop's ACCEPTED_MAX_IT: googlevideo takes an anonymous session's pots only when this is <= 64 bytes.
        val kind = if (integrityToken.size <= 64) "accepted" else "rejected"
        Logger.i(TAG, "BotGuard ready: integrity token ${integrityToken.size} bytes ($kind class when signed out), valid ${ttlSeconds}s")
        // One minter for the life of the integrity token, as bgutils builds it. NewPipe's harness
        // built a new one per token, and each build made every later token longer by about the
        // integrity token's size, which googlevideo then refused (measured 2026-10-26: 156, 219,
        // 285 bytes for 11-character ids).
        val made =
            evaluate(
                view,
                "(function(){try{window.poMinter = webPoSignalOutput[0](${uint8Array(integrityToken)});" +
                    "return typeof poMinter}catch(e){return 'error: '+e}})()",
            )
        check(made == "\"function\"") { "no minter: $made" }
        integrityTokenSize = integrityToken.size

        // Ten minutes of margin, like NewPipe.
        expiresAtMs = System.currentTimeMillis() + (ttlSeconds - 600) * 1000
        sessionId = visitorData
        sessionToken = mint(visitorData)
    }

    private suspend fun mint(identifier: String): String {
        val view = checkNotNull(webView) { "BotGuard is not running" }
        val identifierBytes = identifier.encodeToByteArray()
        val bytes =
            evaluate(
                view,
                "(function(){try{var t = poMinter(${uint8Array(identifierBytes)});" +
                    "if (!(t instanceof Uint8Array)) return 'error: not a Uint8Array';" +
                    "return Array.from(t).join(',')}catch(e){return 'error: '+e}})()",
            )?.removeSurrounding("\"")
        check(bytes != null && bytes.isNotEmpty() && !bytes.startsWith("error")) { "minting: $bytes" }
        val token = bytes.split(',').map { it.toInt().toByte() }.toByteArray()
        // Desktop's arithmetic: a pot is its identifier, the integrity token and 14 bytes. Not for
        // visitorData, whose percent escapes put it a byte adrift (desktop saw the same).
        val expected = identifierBytes.size + integrityTokenSize + 14
        if ('%' !in identifier && token.size != expected) Logger.w(TAG, "token is ${token.size} bytes, expected $expected")
        return Base64.getUrlEncoder().encodeToString(token)
    }

    private suspend fun close() {
        val view = webView ?: return
        webView = null
        sessionToken = null
        withContext(Dispatchers.Main) {
            view.loadUrl("about:blank")
            view.destroy()
        }
    }

    private class Bridge {
        val pageReady = CompletableDeferred<Unit>()
        val botguard = CompletableDeferred<String>()

        @JavascriptInterface
        fun onPageReady() {
            pageReady.complete(Unit)
        }

        @JavascriptInterface
        fun onBotguard(response: String) {
            botguard.complete(response)
        }

        @JavascriptInterface
        fun onError(error: String) {
            val e = IllegalStateException("BotGuard: $error")
            pageReady.completeExceptionally(e)
            botguard.completeExceptionally(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun newWebView(bridge: Bridge): WebView =
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.userAgentString = USER_AGENT
            // BotGuard's program arrives through Kotlin; the page itself never needs the network.
            settings.blockNetworkLoads = true
            settings.safeBrowsingEnabled = false
            addJavascriptInterface(bridge, BRIDGE)
            webChromeClient =
                object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        // Everything that can fail is caught in JS, so an uncaught error means this
                        // WebView cannot run the harness at all.
                        if (message.message().contains("Uncaught")) bridge.onError(message.message())
                        return super.onConsoleMessage(message)
                    }
                }
            loadDataWithBaseURL(
                "https://www.youtube.com",
                HARNESS.replaceFirst("</script>", "\n$BRIDGE.onPageReady()</script>"),
                "text/html",
                "utf-8",
                null,
            )
        }

    private suspend fun evaluate(
        view: WebView,
        js: String,
    ): String? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation -> view.evaluateJavascript(js) { continuation.resume(it) } }
        }

    private suspend fun post(
        url: String,
        body: String,
    ): String =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("x-goog-api-key", GOOGLE_API_KEY)
                    .header("x-user-agent", "grpc-web-javascript/0.1")
                    .post(body.toRequestBody("application/json+protobuf".toMediaType()))
                    .build()
            http.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "${url.substringAfterLast('/')} answered ${response.code}" }
                response.body.string()
            }
        }

    private companion object {
        const val BUDGET_MS = 20_000L
        const val BRIDGE = "LimusicBotGuard"
        const val CREATE_URL = "https://www.youtube.com/api/jnn/v1/Create"
        const val GENERATE_IT_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"

        // BotGuard's public key and request key, read off YouTube's own BotGuard requests (NewPipe).
        const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"

        /** Create's challenge, descrambled when it comes scrambled, as the object runBotGuard takes. */
        fun parseChallenge(raw: String): String {
            val scrambled = Json.parseToJsonElement(raw).jsonArray
            val data =
                if (scrambled.size > 1 && (scrambled[1] as? JsonPrimitive)?.isString == true) {
                    val descrambled = youtubeBase64(scrambled[1].jsonPrimitive.content).map { (it + 97).toByte() }.toByteArray()
                    Json.parseToJsonElement(descrambled.decodeToString()).jsonArray
                } else {
                    scrambled[0].jsonArray
                }

            fun text(i: Int) = (data.getOrNull(i) as? JsonPrimitive)?.contentOrNull

            fun firstText(i: Int) =
                (data.getOrNull(i) as? JsonArray)
                    ?.firstOrNull { it is JsonPrimitive && it.isString }
                    ?.jsonPrimitive
                    ?.content
            return buildJsonObject {
                put("messageId", text(0))
                putJsonObject("interpreterJavascript") {
                    put("privateDoNotAccessOrElseSafeScriptWrappedValue", firstText(1))
                    put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue", firstText(2))
                }
                put("interpreterHash", text(3))
                put("program", text(4))
                put("globalName", text(5))
                put("clientExperimentsStateBlob", text(7))
            }.toString()
        }

        /** YouTube's base64: URL-safe, with `.` for padding. */
        fun youtubeBase64(text: String): ByteArray =
            Base64.getDecoder().decode(text.replace('-', '+').replace('_', '/').replace('.', '='))

        fun uint8Array(bytes: ByteArray) = bytes.joinToString(",", "new Uint8Array([", "])") { it.toUByte().toString() }

        /** NewPipe's po_token.html, minus obtainPoToken: BotGuard's client in a page, as BgUtils runs it. */
        const val HARNESS = """<!DOCTYPE html>
<html lang="en"><head><title></title><script>
function loadBotGuard(challengeData) {
  this.vm = this[challengeData.globalName];
  this.program = challengeData.program;
  this.vmFunctions = {};
  this.syncSnapshotFunction = null;

  if (!this.vm)
    throw new Error('[BotGuardClient]: VM not found in the global object');

  if (!this.vm.a)
    throw new Error('[BotGuardClient]: Could not load program');

  const vmFunctionsCallback = function (
    asyncSnapshotFunction,
    shutdownFunction,
    passEventFunction,
    checkCameraFunction
  ) {
    this.vmFunctions = {
      asyncSnapshotFunction: asyncSnapshotFunction,
      shutdownFunction: shutdownFunction,
      passEventFunction: passEventFunction,
      checkCameraFunction: checkCameraFunction
    };
  };

  this.syncSnapshotFunction = this.vm.a(this.program, vmFunctionsCallback, true, this.userInteractionElement, function () {/** no-op */ }, [ [], [] ])[0]

  // The VM finishes in the background and eventually calls vmFunctionsCallback; polling every
  // millisecond hands control to it.
  return new Promise(function (resolve, reject) {
    i = 0
    refreshIntervalId = setInterval(function () {
      if (!!this.vmFunctions.asyncSnapshotFunction) {
        resolve(this)
        clearInterval(refreshIntervalId);
      }
      if (i >= 10000) {
        reject("asyncSnapshotFunction is null even after 10 seconds")
        clearInterval(refreshIntervalId);
      }
      i += 1;
    }, 1);
  })
}

function snapshot(args) {
  return new Promise(function (resolve, reject) {
    if (!this.vmFunctions.asyncSnapshotFunction)
      return reject(new Error('[BotGuardClient]: Async snapshot function not found'));

    this.vmFunctions.asyncSnapshotFunction(function (response) { resolve(response) }, [
      args.contentBinding,
      args.signedTimestamp,
      args.webPoSignalOutput,
      args.skipPrivacyBuffer
    ]);
  });
}

function runBotGuard(challengeData) {
  const interpreterJavascript = challengeData.interpreterJavascript.privateDoNotAccessOrElseSafeScriptWrappedValue;

  if (interpreterJavascript) {
    new Function(interpreterJavascript)();
  } else throw new Error('Could not load VM');

  const webPoSignalOutput = [];
  return loadBotGuard({
    globalName: challengeData.globalName,
    globalObj: this,
    program: challengeData.program
  }).then(function (botguard) {
    return botguard.snapshot({ webPoSignalOutput: webPoSignalOutput })
  }).then(function (botguardResponse) {
    return { webPoSignalOutput: webPoSignalOutput, botguardResponse: botguardResponse }
  })
}
</script></head><body></body></html>"""
    }
}
