package com.simohypers.limusic.discord

import com.simohypers.limusic.ktorext.getEngine
import com.simohypers.limusic.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/** Limusic's Discord application, shared with desktop: its name is what follows "Listening to". */
const val APP_ID = "1525891596804161727"

private const val TAG = "Discord"

/** Reconnect backoff while a card is wanted but the socket is down. */
private const val CONNECT_RETRY_MIN = 1_000L
private const val CONNECT_RETRY_MAX = 60_000L

/** How long to park when nothing is pending. Any message wakes the loop anyway. */
private const val IDLE_TICK = 15_000L

/** With no card up for this long, the socket closes. Desktop holds it open; a phone pays for every
 *  heartbeat, and the next card reconnects within the duration grace. */
private const val IDLE_CLOSE = 5 * 60_000L

/** Discord rejects asset URLs longer than this. Better a card without art than a rejected one. */
private const val MAX_ASSET_URL = 256

private val mono = TimeSource.Monotonic.markNow()

private fun monoNow(): Long = mono.elapsedNow().inWholeMilliseconds

@OptIn(ExperimentalTime::class)
private fun wallNow(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Discord Rich Presence, ported from desktop Limusic's discord.rs onto the gateway.
 *
 * Desktop talks to the local Discord client over IPC. A phone has no such client, so this signs in
 * as the user with an OAuth token ([DiscordOAuth]) and pushes PRESENCE_UPDATE itself. Everything
 * else follows desktop: one loop owns the socket and the card, fed by messages; [Planner] decides
 * when a push may go out; a card shows only while playing unless "keep while paused" is on.
 *
 * @param token a valid access token, refreshed when `force` is set or it is near expiry; null when
 * signed out.
 */
class DiscordPresence(
    private val scope: CoroutineScope,
    private val token: suspend (force: Boolean) -> String?,
) {
    private val inbox = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val planner = Planner(DiscordConfig(), ::monoNow, ::wallNow)
    private var enabled = false
    private var gateway: DiscordGateway? = null
    private var accessToken: String? = null
    private var lastConnectTry: Long? = null
    private var backoff = CONNECT_RETRY_MIN
    private var authFailures = 0
    private var lastShownAt = 0L

    /** Asset URL to Discord's `mp:` path. Owned by the loop, so no lock. */
    private val assets = LinkedHashMap<String, String>()

    init {
        scope.launch {
            try {
                var wait = sync()
                while (true) {
                    val action = withTimeoutOrNull(wait) { inbox.receive() }
                    // Drain the burst behind it (track + length + play state) so it becomes one push.
                    if (action != null) {
                        action()
                        while (true) (inbox.tryReceive().getOrNull() ?: break)()
                    }
                    wait = sync()
                }
            } finally {
                withContext(NonCancellable) { disconnect() }
            }
        }
    }

    private fun post(action: suspend () -> Unit) {
        inbox.trySend(action)
    }

    /** Presence on and signed in. Off takes the card down and closes the socket. */
    fun setEnabled(on: Boolean) =
        post {
            if (enabled != on) {
                enabled = on
                authFailures = 0
                if (!on) disconnect()
            }
        }

    fun setConfig(cfg: DiscordConfig) = post { planner.setConfig(cfg) }

    /** The player moved to [track] (or restarted it): its clock starts over. */
    fun setTrack(track: DiscordTrack) = post { planner.setTrack(track) }

    /** Better metadata for the playing track, without touching its clock. */
    fun updateTrack(track: DiscordTrack) = post { planner.updateTrack(track) }

    fun setDuration(ms: Long) = post { planner.setDuration(ms) }

    fun setPosition(ms: Long) = post { planner.setPosition(ms) }

    /**
     * [on]: the user wants playback, which includes loading the next track and a buffering stall;
     * off takes the card down. [moving]: audio is actually playing, which is what runs the bar.
     */
    fun setPlaying(
        on: Boolean,
        moving: Boolean,
    ) = post { planner.setPlaying(on, moving) }

    fun setSpeed(speed: Float) = post { planner.setSpeed(speed) }

    /** Reconcile Discord with what should be showing; returns how long to sleep before re-deciding. */
    private suspend fun sync(): Long {
        if (!enabled) return IDLE_TICK
        gateway?.let { g ->
            if (g.closed) {
                gateway = null
                planner.forgetSent()
                if (g.closeCode == CLOSE_AUTH_FAILED) authFailures++
            } else if (g.ready) {
                backoff = CONNECT_RETRY_MIN
                authFailures = 0
            }
        }
        return when (val act = planner.plan()) {
            Act.Push -> {
                val g = socket()
                if (g != null) {
                    push(g)
                    IDLE_TICK
                } else {
                    retryIn()
                }
            }
            Act.Clear -> {
                clear()
                IDLE_TICK
            }
            is Act.Wait -> {
                // Warm the socket while the grace runs, so the first card is not a connect late.
                if (planner.wantsCard() && socket() == null) minOf(act.ms, retryIn()) else act.ms
            }
            Act.Idle -> {
                closeIfIdle()
                IDLE_TICK
            }
        }
    }

    /** The ready socket, or null while one is opening (or can't be: signed out, backing off). */
    private suspend fun socket(): DiscordGateway? {
        gateway?.let { return it.takeIf { g -> g.ready } }
        // Two rejected tokens in a row, even after a refresh: stop until the switch or account changes.
        if (authFailures >= 2) return null
        lastConnectTry?.let { if (monoNow() - it < backoff) return null }
        lastConnectTry = monoNow()
        backoff = (backoff * 2).coerceAtMost(CONNECT_RETRY_MAX)
        val tok = token(authFailures > 0) ?: return null
        accessToken = tok
        lastShownAt = monoNow()
        gateway = DiscordGateway(scope, tok) { post {} }
        return null
    }

    private fun retryIn(): Long {
        val since = lastConnectTry ?: return MIN_WAIT
        return (backoff - (monoNow() - since)).coerceIn(MIN_WAIT, IDLE_TICK)
    }

    private suspend fun push(g: DiscordGateway) {
        val t = planner.track ?: return
        val cfg = planner.cfg
        val (art, badge) = if (!cfg.hideDetails && cfg.cover) images(t, cfg.badge) else null to null
        // Taken after the artwork lookup, which can take a moment, so the bar starts where playback is.
        val (start, end) = planner.timeline()
        planner.noteFrame()
        if (g.presence(activity(t, cfg, planner.playing, start, end, art, badge))) {
            planner.pushed(start)
            Logger.i(TAG, "presence pushed: ${t.videoId} playing=${planner.playing} at=${(wallNow() - start) / 1000}s bar=${end?.let { (it - start) / 1000 }}s")
        } else {
            planner.forgetSent()
        }
        lastShownAt = monoNow()
    }

    private suspend fun clear() {
        if (planner.sent == null) return
        gateway?.takeIf { it.ready }?.let {
            planner.noteFrame()
            it.presence(null)
        }
        planner.cleared()
        lastShownAt = monoNow()
        Logger.i(TAG, "presence cleared")
    }

    private suspend fun closeIfIdle() {
        val g = gateway ?: return
        if (planner.sent == null && monoNow() - lastShownAt > IDLE_CLOSE) {
            Logger.i(TAG, "closing the idle gateway")
            g.close()
            gateway = null
        }
    }

    private suspend fun disconnect() {
        clear()
        gateway?.close()
        gateway = null
        planner.forgetSent()
        lastConnectTry = null
        backoff = CONNECT_RETRY_MIN
    }

    /** The artwork and badge as Discord asset paths. Best-effort: a miss is a card without art. */
    private suspend fun images(
        t: DiscordTrack,
        withBadge: Boolean,
    ): Pair<String?, String?> {
        val art = t.thumbnail?.takeIf { it.startsWith("http") && it.length <= MAX_ASSET_URL } ?: return null to null
        val urls = listOfNotNull(art, BADGE_URL.takeIf { withBadge })
        val missing = urls.filter { it !in assets }
        val tok = accessToken
        if (missing.isNotEmpty() && tok != null) {
            externalAssets(tok, missing).forEachIndexed { i, path -> path?.let { assets[missing[i]] = it } }
            while (assets.size > 64) assets.remove(assets.keys.first())
        }
        return assets[art] to (if (withBadge) assets[BADGE_URL] else null)
    }
}

/** The activity for [t] under [cfg]: the Rust `push_card`, field for field. */
internal fun activity(
    t: DiscordTrack,
    cfg: DiscordConfig,
    playing: Boolean,
    start: Long,
    end: Long?,
    art: String?,
    badge: String?,
): JsonObject =
    buildJsonObject {
        put("name", field(cfg.name))
        put("type", 2)
        // "Hide details": nothing about the track leaves the phone, only "Listening to Limusic".
        if (cfg.hideDetails) {
            put("status_display_type", 0)
            return@buildJsonObject
        }
        val line1Slot = t.line1Slot(cfg)
        put("details", field(t.text(line1Slot) ?: t.title))
        if (cfg.linkLine1) t.link(line1Slot)?.let { put("details_url", it) }
        t.text(cfg.line2)?.let { line2 ->
            put("state", field(line2))
            if (cfg.linkLine2) t.link(cfg.line2)?.let { put("state_url", it) }
        }
        put(
            "status_display_type",
            when (cfg.statusLine) {
                "app" -> 0
                "line1" -> 2
                else -> 1
            },
        )
        // No bar on a paused card: Discord has no paused state, so it would keep running.
        if (cfg.timestamps && playing) {
            putJsonObject("timestamps") {
                put("start", start)
                end?.let { put("end", it) }
            }
        }
        if (cfg.cover && art != null) {
            putJsonObject("assets") {
                put("large_image", art)
                t.text(cfg.line3)?.let { put("large_text", field(it)) }
                // The artwork stands for the release: the album when there is one, else the song.
                if (cfg.linkCover) (t.link("album") ?: t.link("title"))?.let { put("large_url", it) }
                if (cfg.badge && badge != null) {
                    put("small_image", badge)
                    put("small_text", field(cfg.name))
                }
            }
        }
        val buttons = listOf(cfg.button1, cfg.button2).mapNotNull { t.button(it) }
        if (buttons.isNotEmpty()) {
            putJsonArray("buttons") { buttons.forEach { add(JsonPrimitive(it.first)) } }
            putJsonObject("metadata") {
                putJsonArray("button_urls") { buttons.forEach { add(JsonPrimitive(it.second)) } }
            }
        }
    }

private val http =
    HttpClient(getEngine()) {
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }

private val lenient = Json { ignoreUnknownKeys = true }

/**
 * Discord proxies outside images through `external-assets`, which answers `mp:` paths in the order
 * asked. It wants to see the Discord Android client, as Metrolist found.
 */
@OptIn(ExperimentalEncodingApi::class)
private suspend fun externalAssets(
    token: String,
    urls: List<String>,
): List<String?> =
    runCatching {
        val superProps =
            buildJsonObject {
                put("os", "Android")
                put("browser", "Discord Android")
                put("client_build_number", 314013)
                put("client_version", "314.13 - Stable")
                put("release_channel", "googleRelease")
            }.toString()
        val resp =
            http.post("https://discord.com/api/v9/applications/$APP_ID/external-assets") {
                header("Authorization", "Bearer $token")
                header("User-Agent", "Discord-Android/314013;RNA")
                header("X-Super-Properties", Base64.encode(superProps.encodeToByteArray()))
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { putJsonArray("urls") { urls.forEach { add(JsonPrimitive(it)) } } }.toString())
            }
        val body = resp.bodyAsText()
        if (!resp.status.isSuccess()) {
            Logger.w(TAG, "external-assets ${resp.status.value}: ${body.take(200)}")
            return@runCatching urls.map { null }
        }
        val list = lenient.parseToJsonElement(body).jsonArray
        urls.indices.map { i ->
            list.getOrNull(i)?.jsonObject?.get("external_asset_path")?.jsonPrimitive?.contentOrNull?.let { "mp:$it" }
        }
    }.getOrElse {
        Logger.w(TAG, "external-assets failed: ${it.message}")
        urls.map { null }
    }
