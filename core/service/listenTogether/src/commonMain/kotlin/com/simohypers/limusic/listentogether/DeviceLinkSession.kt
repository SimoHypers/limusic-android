package com.simohypers.limusic.listentogether

import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private const val TAG = "DeviceLink"

/** What the send rule does with one playback message. */
enum class LinkSend { SENT, TAKEOVER, DROPPED }

/**
 * The send rule, the same as desktop's `devicelink::send_rule`. Only the player tells the room what
 * is happening; a device that isn't playing may only take over, by starting a track or resuming. A
 * resume comes back as [LinkSend.TAKEOVER]: the caller sends a change_track built from what it
 * plays instead.
 */
fun linkSendRule(
    isPlayer: Boolean,
    action: String,
    playing: Boolean?,
): LinkSend =
    when {
        isPlayer -> LinkSend.SENT
        action == PlaybackActions.CHANGE_TRACK && playing == true -> LinkSend.SENT
        action == PlaybackActions.PLAY -> LinkSend.TAKEOVER
        else -> LinkSend.DROPPED
    }

enum class LinkConnection { OFF, CONNECTING, CONNECTED, UNSUPPORTED }

/** The link room as the server last described it. [lastUpdate] is server wall time in ms. */
data class LinkState(
    val connection: LinkConnection = LinkConnection.OFF,
    val selfId: String = "",
    val users: List<UserInfo> = emptyList(),
    val hostId: String = "",
    val currentTrack: TrackInfo? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val lastUpdate: Long = 0L,
    val volume: Float = 1f,
    val queue: List<TrackInfo> = emptyList(),
    /** The player's queue, shuffle and repeat, when another device plays and has sent them. */
    val status: LinkStatus? = null,
    /**
     * The socket dropped moments ago and is coming back: the room above is the last one heard, still
     * shown for up to [RECONNECT_GRACE] so a blip on mobile data doesn't flip the player view.
     */
    val reconnecting: Boolean = false,
    val error: String? = null,
) {
    val isPlayer: Boolean get() = hostId.isNotEmpty() && hostId == selfId
}

sealed interface LinkEvent {
    /** In the room (again). */
    data object Joined : LinkEvent

    data class HostChanged(
        val hostId: String,
    ) : LinkEvent

    /** Unlinked from another device. */
    data object Kicked : LinkEvent

    /** A control from another device: for this phone as the player, or [RemoteAction.TakeOver] while it isn't. */
    data class Remote(
        val action: RemoteAction,
    ) : LinkEvent
}

sealed interface ClaimResult {
    data class Paired(
        val secret: String,
    ) : ClaimResult

    data object InvalidCode : ClaimResult

    /** Connected, but the server never answered: it predates linking. */
    data object Unsupported : ClaimResult

    data object Unreachable : ClaimResult
}

/**
 * The device link's protocol half: a second [ListenTogetherClient] in a link room (see
 * `limusic-docs/plans/device-link.md` and desktop's `devicelink.rs`). It knows nothing about the
 * player; the bridge in core/data publishes through [publish] and reacts to [events].
 */
class DeviceLinkSession(
    userAgent: String,
) {
    /** Read by the client on every connection attempt. Empty means its default server. */
    private var server = ""

    private val client = ListenTogetherClient(userAgent = userAgent, serverUrl = { server })
    private val scope: CoroutineScope = client

    private val _state = MutableStateFlow(LinkState())
    val state: StateFlow<LinkState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LinkEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<LinkEvent> = _events.asSharedFlow()

    private var join: JoinLinkPayload? = null

    /** The last queue sent as the player; an unchanged one isn't sent again. */
    private var sentStatus: LinkStatus? = null
    private var claim: Pair<String, CompletableDeferred<ClaimResult>>? = null
    private var joinDeadline: Job? = null
    private var retry: Job? = null
    private var grace: Job? = null

    init {
        scope.launch { client.events.collect { onEvent(it) } }
    }

    /** Join the link room on [server], or do nothing if that is already under way. */
    fun open(
        server: String,
        secret: String,
        deviceId: String,
        name: String,
    ) {
        val payload = JoinLinkPayload(secret = secret, deviceId = deviceId, name = name)
        if (payload == join && this.server == server && _state.value.connection != LinkConnection.OFF) return
        if (this.server != server && client.isConnected) client.disconnect()
        this.server = server
        join = payload
        _state.update { it.copy(connection = LinkConnection.CONNECTING, selfId = deviceId, error = null) }
        if (client.isConnected) sendJoin() else client.connect()
    }

    /** Close the socket. The room keeps us listed as offline. */
    fun close() {
        join = null
        grace?.cancel()
        retry?.cancel()
        joinDeadline?.cancel()
        client.disconnect()
        _state.value = LinkState()
    }

    /**
     * The room as last heard before the app was closed, shown until the join confirms or corrects
     * it (at most [RECONNECT_GRACE]): a cold start shows the computer's song at once instead of
     * this phone's own for a moment. Ignored once joined.
     */
    fun restore(last: LinkState) {
        if (_state.value.connection == LinkConnection.CONNECTED) return
        _state.update { it.copy(selfId = it.selfId.ifEmpty { last.selfId }, users = last.users, hostId = last.hostId, currentTrack = last.currentTrack, isPlaying = last.isPlaying, position = last.position, lastUpdate = last.lastUpdate, volume = last.volume, queue = last.queue, reconnecting = true) }
        grace?.cancel()
        grace =
            scope.launch {
                delay(RECONNECT_GRACE)
                _state.update { it.copy(reconnecting = false) }
            }
    }

    /** Leave the room for good (Unlink this phone). */
    fun leave() {
        scope.launch {
            client.send(MessageTypes.LEAVE_ROOM, LeaveRoomPayload())
            close()
        }
    }

    /** Trade a code for the secret, on a socket that is in no room yet. */
    suspend fun claim(
        server: String,
        code: String,
    ): ClaimResult {
        // Only when something is open: a disconnect right before a connect can close the new
        // socket (the client clears its session asynchronously).
        if (join != null || client.isConnected) close()
        this.server = server
        val result = CompletableDeferred<ClaimResult>()
        claim = code to result
        client.connect()
        val answer =
            withTimeoutOrNull(CLAIM_TIMEOUT) { result.await() }
                ?: if (client.isConnected) ClaimResult.Unsupported else ClaimResult.Unreachable
        claim = null
        // A failed claim leaves the socket for the next attempt to replace: the server allows three
        // claims per connection, and each attempt here gets a fresh one.
        if (answer !is ClaimResult.Paired) client.disconnect()
        return answer
    }

    /**
     * Sends a playback change through [linkSendRule]. A [LinkSend.TAKEOVER] is not sent; the caller
     * sends a change_track instead.
     */
    fun publish(p: PlaybackActionPayload): LinkSend {
        val s = _state.value
        if (s.connection != LinkConnection.CONNECTED) return LinkSend.DROPPED
        val decision = linkSendRule(s.isPlayer, p.action, p.playing)
        if (decision == LinkSend.SENT) {
            scope.launch { client.send(MessageTypes.PLAYBACK_ACTION, p) }
            // Ours from now on, not from when host_changed comes back.
            if (p.action == PlaybackActions.CHANGE_TRACK && p.playing == true) {
                _state.update { it.copy(hostId = it.selfId) }
            }
            _state.update { it.applied(p, client.serverNow() ?: 0L) }
        }
        return decision
    }

    /** A control for the player, or for the device [to] names. */
    /** This phone's queue for the other devices' player views. Sent only while it is the player. */
    fun publishStatus(status: LinkStatus) {
        val s = _state.value
        if (s.connection != LinkConnection.CONNECTED || !s.isPlayer || status == sentStatus) return
        sentStatus = status
        scope.launch { client.send(MessageTypes.LINK_STATUS, LinkStatusPayload(status)) }
    }

    fun remote(
        action: RemoteAction,
        to: String? = null,
    ) {
        scope.launch { client.send(MessageTypes.REMOTE, RemotePayload(action, to = to)) }
    }

    fun unlink(deviceId: String) {
        scope.launch { client.send(MessageTypes.KICK_USER, KickUserPayload(userId = deviceId)) }
        _state.update { s -> s.copy(users = s.users.filter { it.userId != deviceId }) }
    }

    /** Where the player is now, extrapolated from its last report. */
    fun positionNow(): Long {
        val s = _state.value
        return client.positionAt(s.position, s.lastUpdate, s.isPlaying)
    }

    /** Server wall time now; the device's own clock stands in for it, as on desktop. */
    fun serverNow(): Long? = client.serverNow()

    private fun sendJoin() {
        val payload = join ?: return
        scope.launch { client.send(MessageTypes.JOIN_LINK, payload) }
        joinDeadline?.cancel()
        joinDeadline =
            scope.launch {
                delay(REPLY_TIMEOUT)
                if (_state.value.connection != LinkConnection.CONNECTED) {
                    Logger.w(TAG, "The server doesn't answer join_link")
                    join = null
                    client.disconnect()
                    _state.update { it.copy(connection = LinkConnection.UNSUPPORTED) }
                }
            }
    }

    private suspend fun onEvent(event: ListenTogetherEvent) {
        when (event) {
            is ListenTogetherEvent.Connected -> {
                val pending = claim
                if (pending != null) {
                    client.send(MessageTypes.PAIR_CLAIM, PairClaimPayload(pending.first))
                } else {
                    sendJoin()
                }
            }
            is ListenTogetherEvent.Disconnected -> {
                if (join == null) return
                _state.update { it.copy(connection = LinkConnection.CONNECTING, reconnecting = it.connection == LinkConnection.CONNECTED || it.reconnecting) }
                if (grace?.isActive != true) {
                    grace =
                        scope.launch {
                            delay(RECONNECT_GRACE)
                            _state.update { it.copy(reconnecting = false) }
                        }
                }
                // The client gives up after a few attempts; the link is the user's devices, so it
                // tries again later for as long as it is wanted.
                if (!event.willRetry) {
                    retry?.cancel()
                    retry =
                        scope.launch {
                            delay(RETRY_AFTER)
                            if (join != null) client.connect()
                        }
                }
            }
            is ListenTogetherEvent.Message -> onMessage(event.type, event.payload)
        }
    }

    private suspend fun onMessage(
        type: String,
        payload: Any?,
    ) {
        when (type) {
            MessageTypes.LINK_JOINED -> {
                val room = (payload as? LinkJoinedPayload)?.state ?: return
                joinDeadline?.cancel()
                // A restarted server has none of ours.
                sentStatus = null
                _state.update {
                    it.copy(
                        connection = LinkConnection.CONNECTED,
                        users = room.users,
                        hostId = room.hostId,
                        currentTrack = room.currentTrack,
                        isPlaying = room.isPlaying,
                        position = room.position,
                        lastUpdate = room.lastUpdate,
                        volume = room.volume,
                        queue = room.queue,
                        status = (payload as LinkJoinedPayload).status,
                        reconnecting = false,
                        error = null,
                    )
                }
                grace?.cancel()
                _events.emit(LinkEvent.Joined)
            }
            MessageTypes.PAIRED -> (payload as? PairedPayload)?.let { claim?.second?.complete(ClaimResult.Paired(it.secret)) }
            MessageTypes.USER_JOINED -> {
                val p = payload as? UserJoinedPayload ?: return
                val user = UserInfo(userId = p.userId, username = p.username, isConnected = true)
                _state.update { s -> s.copy(users = s.users.filter { it.userId != p.userId } + user) }
            }
            MessageTypes.USER_LEFT -> {
                val id = (payload as? UserLeftPayload)?.userId ?: return
                _state.update { s ->
                    val wasHost = s.hostId == id
                    s.copy(
                        users = s.users.filter { it.userId != id },
                        hostId = if (wasHost) "" else s.hostId,
                        isPlaying = if (wasHost) false else s.isPlaying,
                    )
                }
            }
            MessageTypes.USER_DISCONNECTED -> setOnline((payload as? UserDisconnectedPayload)?.userId, false)
            MessageTypes.USER_RECONNECTED -> setOnline((payload as? UserReconnectedPayload)?.userId, true)
            MessageTypes.HOST_CHANGED -> {
                val id = (payload as? HostChangedPayload)?.newHostId ?: return
                // The new player's queue follows from it; ours goes out again if we play again.
                sentStatus = null
                _state.update { s -> s.copy(hostId = id, users = s.users.map { it.copy(isHost = it.userId == id) }, status = null) }
                _events.emit(LinkEvent.HostChanged(id))
            }
            MessageTypes.LINK_STATUS -> (payload as? LinkStatusPayload)?.let { p -> _state.update { it.copy(status = p.status) } }
            MessageTypes.REMOTE -> {
                val action = (payload as? RemotePayload)?.action ?: return
                // Take over only while not the player; everything else only while it is.
                if ((action == RemoteAction.TakeOver) != _state.value.isPlayer) _events.emit(LinkEvent.Remote(action))
            }
            MessageTypes.SYNC_PLAYBACK -> {
                val p = payload as? PlaybackActionPayload ?: return
                _state.update { it.applied(p, client.serverNow() ?: 0L) }
            }
            MessageTypes.KICKED -> {
                close()
                _events.emit(LinkEvent.Kicked)
            }
            MessageTypes.ERROR -> {
                val e = payload as? ErrorPayload ?: return
                when (e.code) {
                    "pair_code_invalid" -> claim?.second?.complete(ClaimResult.InvalidCode)
                    "bad_secret", "room_full", "server_full" -> {
                        join = null
                        client.disconnect()
                        _state.update { it.copy(connection = LinkConnection.OFF, error = e.message) }
                    }
                }
            }
        }
    }

    private fun setOnline(
        id: String?,
        online: Boolean,
    ) {
        if (id == null) return
        _state.update { s -> s.copy(users = s.users.map { if (it.userId == id) it.copy(isConnected = online) else it }) }
    }

    private companion object {
        val REPLY_TIMEOUT = 5.seconds

        /** Connecting plus [REPLY_TIMEOUT]. */
        val CLAIM_TIMEOUT = 10.seconds
        val RETRY_AFTER = 30.seconds
        val RECONNECT_GRACE = 10.seconds
    }
}

/** One playback change folded into the room the way the server does it. */
internal fun LinkState.applied(
    p: PlaybackActionPayload,
    now: Long,
): LinkState =
    when (p.action) {
        PlaybackActions.PLAY -> copy(isPlaying = true, position = p.position, lastUpdate = now)
        PlaybackActions.PAUSE -> copy(isPlaying = false, position = p.position, lastUpdate = now)
        PlaybackActions.SEEK -> copy(position = p.position, lastUpdate = now)
        PlaybackActions.CHANGE_TRACK ->
            copy(
                currentTrack = p.trackInfo,
                position = p.position,
                isPlaying = p.playing == true,
                lastUpdate = now,
                queue = p.queue,
            )
        PlaybackActions.SYNC_QUEUE -> copy(queue = p.queue)
        PlaybackActions.SET_VOLUME -> copy(volume = p.volume)
        else -> this
    }
