package com.maxrave.discord

import com.maxrave.ktorext.getEngine
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random

private const val TAG = "Discord"
private const val GATEWAY = "wss://gateway.discord.gg/?v=10&encoding=json"

/** Discord rejected the token: refresh it before the next connect. */
internal const val CLOSE_AUTH_FAILED = 4004

private val client = HttpClient(getEngine()) { install(WebSockets) }
private val json = Json { ignoreUnknownKeys = true }

/**
 * One gateway session, identified with the OAuth token as the Social SDK does (Metrolist's route).
 *
 * It never reconnects itself. When the socket ends for any reason it says so through [onChange],
 * and the presence loop decides whether to open a new one; a reconnect is a fresh IDENTIFY, not a
 * RESUME, because the presence has to be re-sent either way.
 * ponytail: no RESUME and no missed-ACK check; add them if reconnects ever show up in the logs often.
 */
internal class DiscordGateway(
    private val scope: CoroutineScope,
    private val token: String,
    private val onChange: () -> Unit,
) {
    @kotlin.concurrent.Volatile
    var ready = false
        private set

    @kotlin.concurrent.Volatile
    var closed = false
        private set

    @kotlin.concurrent.Volatile
    var closeCode: Int? = null
        private set

    private var session: WebSocketSession? = null
    private var seq: Int? = null
    private var heartbeat: Job? = null

    private val job =
        scope.launch {
            try {
                client.webSocket(GATEWAY) {
                    session = this
                    for (frame in incoming) {
                        if (frame is Frame.Text) onText(frame.readText())
                    }
                    closeCode = closeReason.await()?.code?.toInt()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, "gateway failed: ${e.message}")
            } finally {
                heartbeat?.cancel()
                ready = false
                closed = true
                if (closeCode != null) Logger.w(TAG, "gateway closed: $closeCode")
                onChange()
            }
        }

    private suspend fun onText(text: String) {
        val o = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        o["s"]?.jsonPrimitive?.intOrNull?.let { seq = it }
        when (o["op"]?.jsonPrimitive?.intOrNull) {
            // HELLO: start beating, then identify.
            10 -> {
                val interval = o["d"]?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.long ?: 41_250L
                heartbeat?.cancel()
                heartbeat =
                    scope.launch {
                        delay((interval * Random.nextDouble()).toLong())
                        while (true) {
                            beat()
                            delay(interval)
                        }
                    }
                send(identify())
            }
            1 -> beat()
            0 ->
                if (o["t"]?.jsonPrimitive?.contentOrNull == "READY") {
                    ready = true
                    Logger.i(TAG, "gateway ready")
                    onChange()
                }
            // RECONNECT and INVALID_SESSION: end this session; the loop opens a fresh one.
            7, 9 -> session?.close(CloseReason(4000, "reconnect"))
        }
    }

    private suspend fun beat() {
        send(
            buildJsonObject {
                put("op", 1)
                put("d", seq?.let { JsonPrimitive(it) } ?: JsonNull)
            }.toString(),
        )
    }

    private fun identify(): String =
        buildJsonObject {
            put("op", 2)
            putJsonObject("d") {
                put("token", "Bearer $token")
                put("intents", 0)
                putJsonObject("properties") {
                    put("os", "android")
                    put("browser", "Discord Android")
                    put("device", APP_ID)
                }
                put("compress", false)
            }
        }.toString()

    /** Op 3 with [activity] as the only one, or none to clear. */
    suspend fun presence(activity: JsonElement?): Boolean =
        send(
            buildJsonObject {
                put("op", 3)
                putJsonObject("d") {
                    put("since", 0)
                    put("activities", kotlinx.serialization.json.JsonArray(listOfNotNull(activity)))
                    put("status", "online")
                    // afk keeps Discord's own app on the phone getting push notifications.
                    put("afk", true)
                }
            }.toString(),
        )

    private suspend fun send(text: String): Boolean =
        runCatching {
            val s = session ?: return false
            s.send(Frame.Text(text))
            true
        }.getOrElse {
            if (it is CancellationException) throw it
            false
        }

    /** A clean close, which takes this session's presence down with it. */
    suspend fun close() {
        runCatching { session?.close(CloseReason(CloseReason.Codes.NORMAL, "")) }
        job.cancel()
    }
}

