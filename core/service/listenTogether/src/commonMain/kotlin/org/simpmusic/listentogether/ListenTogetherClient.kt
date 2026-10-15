/*
 * The transport half of Listen Together, talking to Limusic's sync server (`crates/sync-server` in
 * the Limusic repo) so Android and desktop share rooms. Originally written against Metrolist's
 * server (GPL-3.0, the same licence as this project).
 */
package org.simpmusic.listentogether

import com.maxrave.ktorext.getEngine
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val TAG = "ListenTogetherClient"

/**
 * What the socket reports upward.
 *
 * Everything above this class reacts to these and never to frames, which is what lets the state
 * machine in the next layer stay platform-free and testable without a socket.
 */
sealed interface ListenTogetherEvent {
    /**
     * The handshake completed and the connection is usable.
     *
     * [resumed] means a `reconnect` was sent with a stored session token — the room may still be
     * the one we were in. It is NOT a promise: the server answers `reconnected` on success and
     * `error` when the token has expired, and only that answer settles it.
     */
    data class Connected(
        val serverVersion: String,
        val resumed: Boolean,
    ) : ListenTogetherEvent

    /** One decoded protocol message. [payload] is null for types this client does not model. */
    data class Message(
        val type: String,
        val payload: Any?,
    ) : ListenTogetherEvent

    /** The server clock is set, so [ListenTogetherClient.positionAt] now returns corrected values. */
    data object ClockReady : ListenTogetherEvent

    /**
     * The socket is down. [willRetry] false means this client has given up and the user has to
     * rejoin by hand — the retry budget is spent, or the server refused this client outright.
     */
    data class Disconnected(
        val reason: String?,
        val willRetry: Boolean,
    ) : ListenTogetherEvent
}

/**
 * Owns one WebSocket to a Listen Together server: the frame loop, the keepalive ping, and
 * reconnection with a stored session token.
 *
 * It deliberately holds no room state — that belongs to the layer above. The one piece of protocol
 * knowledge it does keep is the session token, because reconnection has to be transparent: it is
 * read out of `room_created` / `join_approved` as those pass through, and replayed on the next
 * connection.
 */
class ListenTogetherClient(
    /** Application identity for the HTTP upgrade request, separate from protocol capabilities. */
    userAgent: String,
    /**
     * Read on every connection attempt rather than captured once, so changing the server in
     * settings takes effect on the next connect instead of needing the screen to be recreated.
     */
    private val serverUrl: () -> String = { DEFAULT_SERVER_URL },
    /**
     * MUST be monotonic — [ServerClock] compares it against itself. The default is Kotlin's own
     * monotonic source, which needs no expect/actual; a platform with a better one (Android's
     * `SystemClock.elapsedRealtime`, which also counts deep sleep) can pass it instead.
     */
    private val elapsedRealtime: () -> Long = {
        PROCESS_START.elapsedNow().inWholeMilliseconds
    },
    dispatcher: CoroutineContext = Dispatchers.Default,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatcher

    private val client: HttpClient =
        HttpClient(getEngine()) {
            install(UserAgent) { agent = userAgent }
            install(WebSockets)
        }

    private val serverClock = ServerClock(elapsedRealtime)

    // SUSPEND rather than a dropping overflow policy, and a buffer far larger than any burst the
    // server can produce: losing one protocol frame is a silent desync, which is strictly worse
    // than briefly slowing the reader. The server's own send buffer is 256.
    private val _events = MutableSharedFlow<ListenTogetherEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()

    private val codec = MessageCodec()
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private var sessionToken: String? = null
    private var reconnectDelay = INITIAL_RECONNECT_DELAY
    private var reconnectAttempts = 0
    /** Set when the server tells us retrying cannot help — see [MessageTypes.ERROR] handling. */
    private var fatal = false

    /** True while the socket is open. */
    val isConnected: Boolean
        get() = session?.isActive == true

    /** Server wall time now, or null before the first connection. */
    fun serverNow(): Long? = serverClock.now()

    /** See [ServerClock.positionAt]. Falls back to [position] whenever the clock cannot help. */
    fun positionAt(
        position: Long,
        effectiveAtServerTime: Long?,
        isPlaying: Boolean,
    ): Long = serverClock.positionAt(position, effectiveAtServerTime, isPlaying)

    /**
     * Opens the connection, or does nothing if one is already open or opening.
     *
     * Reconnection is automatic from here on; callers do not call this again after a drop.
     */
    fun connect() {
        // A deliberate start by the user, so the attempt budget begins again. The retry path
        // deliberately calls [startConnection] instead of this: routing a retry through here would
        // reset the counter every time and [MAX_RECONNECT_ATTEMPTS] would never be reached — the
        // limit would look implemented and quietly never fire.
        reconnectAttempts = 0
        reconnectDelay = INITIAL_RECONNECT_DELAY
        fatal = false
        startConnection()
    }

    private fun startConnection() {
        if (connectionJob?.isActive == true) {
            Logger.i(TAG, "Connect ignored — a connection is already active")
            return
        }
        connectionJob =
            launch {
                try {
                    openAndRun()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(TAG, "Connection error: ${e.stackTraceToString()}")
                }
                // Reached on every close, clean or not — openAndRun returns when the reader stops.
                scheduleReconnect()
            }
    }

    /**
     * Sends one message. Returns false when there is no live socket, which the caller must treat
     * as "not sent" rather than retrying blindly: the reconnect path replays nothing.
     */
    suspend fun send(
        msgType: String,
        payload: Any? = null,
    ): Boolean {
        val live = session ?: return false
        if (!live.isActive) return false
        // Nothing to send for a message the server has no counterpart for; that is not a failure.
        val frame = codec.encode(msgType, payload) ?: return true
        return runCatching {
            live.send(Frame.Text(frame))
            true
        }.getOrElse { e ->
            Logger.w(TAG, "Send failed for $msgType: ${e.message}")
            false
        }
    }

    /** Leaves for good: stops retrying, forgets the session token, closes the socket. */
    fun disconnect() {
        Logger.i(TAG, "Disconnecting by request")
        sessionToken = null
        reconnectAttempts = 0
        connectionJob?.cancel()
        connectionJob = null
        launch {
            runCatching { session?.close() }
            session = null
        }
    }

    /** Releases the HTTP client too. The instance is unusable afterwards. */
    fun release() {
        disconnect()
        runCatching { client.close() }
        cancel()
    }

    private suspend fun openAndRun() {
        val url = serverUrl().ifBlank { DEFAULT_SERVER_URL }
        Logger.i(TAG, "Connecting to $url")
        val live = client.webSocketSession(url)
        session = live
        // Limusic's server stamps times with its wall clock and its pong carries none, so the
        // device's own wall clock stands in for it, as on desktop.
        serverClock.assumeServerTime(Clock.System.now().toEpochMilliseconds())

        try {
            coroutineScope {
                val reader = launch { readFrames(live) }

                val token = sessionToken
                if (token != null) {
                    Logger.i(TAG, "Resuming with stored session token")
                    send(MessageTypes.RECONNECT, ReconnectPayload(sessionToken = token))
                }

                // An open socket is what "connected" means here; there is no handshake.
                reconnectDelay = INITIAL_RECONNECT_DELAY
                reconnectAttempts = 0
                _events.emit(ListenTogetherEvent.Connected(serverVersion = "", resumed = token != null))
                _events.emit(ListenTogetherEvent.ClockReady)

                val pinger = launch { pingLoop() }
                reader.join()
                pinger.cancel()
            }
        } finally {
            session = null
            // NonCancellable because this runs on the disconnect() path too, where the job has
            // already been cancelled — and close() is itself a suspend call, so without this it
            // would abort immediately and the close frame would never be sent.
            withContext(NonCancellable) { runCatching { live.close() } }
        }
    }

    private suspend fun readFrames(live: DefaultClientWebSocketSession) {
        for (frame in live.incoming) {
            // Limusic's server writes text frames only.
            if (frame !is Frame.Text) continue
            val text = frame.readText()
            val decoded = codec.decode(text)
            if (decoded == null) {
                Logger.w(TAG, "Undecodable frame dropped (${text.length} chars)")
                continue
            }
            handleMessage(decoded.first, decoded.second)
        }
        Logger.i(TAG, "Read loop ended; socket closed")
    }

    private suspend fun handleMessage(
        type: String,
        payload: Any?,
    ) {
        when (type) {
            // A keepalive answer and nothing else.
            MessageTypes.PONG -> return

            // The token is what makes a drop survivable, so it is captured in passing rather than
            // asked of the layer above — which may not have been listening yet.
            MessageTypes.ROOM_CREATED -> (payload as? RoomCreatedPayload)?.let { sessionToken = it.sessionToken }
            MessageTypes.JOIN_APPROVED -> (payload as? JoinApprovedPayload)?.let { sessionToken = it.sessionToken }

            MessageTypes.ERROR -> {
                val error = payload as? ErrorPayload
                Logger.w(TAG, "Server error: ${error?.code} ${error?.message}")
                // `error?.code in set` would not compile — Set<String>.contains takes a non-null
                // String, and a null code is not a rejection anyway.
                if (error != null && error.code in NON_RECOVERABLE_ERROR_CODES) {
                    Logger.e(TAG, "Server rejected this client (${error.code}) — not retrying")
                    fatal = true
                }
                // A token the server no longer knows cannot resume anything.
                if (error?.code == "session_expired") sessionToken = null
            }

            // A room we can no longer return to: the token is spent either way.
            MessageTypes.KICKED -> sessionToken = null
        }
        _events.emit(ListenTogetherEvent.Message(type, payload))
    }

    /** Keeps the socket alive: the server drops a connection that says nothing for 90s. */
    private suspend fun pingLoop() {
        while (send(MessageTypes.PING)) delay(PING_INTERVAL)
    }

    private suspend fun scheduleReconnect() {
        if (fatal) {
            _events.emit(ListenTogetherEvent.Disconnected("Server rejected this client", willRetry = false))
            return
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Logger.w(TAG, "Gave up after $MAX_RECONNECT_ATTEMPTS reconnection attempts")
            // The token is dropped with the attempts: a later connect() is a fresh join, not a
            // resume, and replaying a token the server may already have expired would be answered
            // with an error rather than a room.
            sessionToken = null
            _events.emit(
                ListenTogetherEvent.Disconnected(
                    reason = "Could not reconnect after $MAX_RECONNECT_ATTEMPTS attempts",
                    willRetry = false,
                ),
            )
            return
        }

        reconnectAttempts++
        Logger.i(TAG, "Reconnecting in $reconnectDelay (attempt $reconnectAttempts of $MAX_RECONNECT_ATTEMPTS)")
        _events.emit(ListenTogetherEvent.Disconnected("Connection lost", willRetry = true))
        delay(reconnectDelay)
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
        connectionJob = null
        startConnection()
    }

    companion object {
        /** Limusic desktop's default server, so Android and desktop land in the same rooms. */
        const val DEFAULT_SERVER_URL = "wss://fedora-1.tail9c4985.ts.net/ws"

        /** Anchors the default monotonic source; see [elapsedRealtime]. */
        private val PROCESS_START = TimeSource.Monotonic.markNow()

        /** Desktop pings every 25s; the server's idle limit is 90s. */
        private val PING_INTERVAL = 25.seconds

        private val INITIAL_RECONNECT_DELAY: Duration = 1.seconds
        private val MAX_RECONNECT_DELAY: Duration = 60.seconds

        /**
         * How many times a dropped connection is retried before the user is told, in so many words,
         * that it failed. The delay doubles each time — 1s, 2s, 4s, 8s, 16s — so five attempts span
         * a little over half a minute of genuinely bad network before giving up rather than
         * retrying into a flat battery.
         *
         * Counted attempts, not elapsed time: the budget refills when a socket opens.
         */
        private const val MAX_RECONNECT_ATTEMPTS = 5

        /** Errors where reconnecting repeats the same rejection. */
        private val NON_RECOVERABLE_ERROR_CODES = setOf("server_full")
    }
}
