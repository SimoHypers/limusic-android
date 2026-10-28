package com.maxrave.data.listentogether

import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.LinkConnection as DomainConnection
import com.maxrave.domain.data.model.listentogether.LinkQueue
import com.maxrave.domain.data.model.listentogether.LinkQueueItem
import com.maxrave.domain.data.model.listentogether.LinkRemote
import com.maxrave.domain.data.model.listentogether.LinkedDevice
import com.maxrave.domain.data.model.listentogether.PairResult
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.DeviceLinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.simpmusic.listentogether.ClaimResult
import org.simpmusic.listentogether.DeviceLinkSession
import org.simpmusic.listentogether.LinkConnection
import org.simpmusic.listentogether.LinkEvent
import org.simpmusic.listentogether.LinkState
import org.simpmusic.listentogether.RemoteAction
import org.simpmusic.listentogether.TrackInfo
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/** The stored link. Never logged. */
@Serializable
private data class StoredLink(
    val secret: String,
    /** Empty means the default server. */
    val server: String,
)

/**
 * Owns the stored link and decides when the socket is open: while paired and (the app is in front
 * or playing). It closes 30 s after both stop; while it plays, the playback foreground service
 * keeps the process and the network up.
 */
class DeviceLinkRepositoryImpl(
    private val session: DeviceLinkSession,
    private val bridge: DeviceLinkBridge,
    private val dataStore: DataStoreManager,
    handler: MediaPlayerHandler,
    private val scope: CoroutineScope,
) : DeviceLinkRepository {
    private val foreground = MutableStateFlow(false)

    private val stored: StateFlow<StoredLink?> =
        dataStore
            .getString(KEY_LINK)
            .map { raw -> raw?.takeIf { it.isNotBlank() }?.let { runCatching { Json.decodeFromString<StoredLink>(it) }.getOrNull() } }
            .stateIn(scope, SharingStarted.Eagerly, null)

    override val link: StateFlow<DeviceLink> =
        combine(
            stored,
            session.state,
            handler.controlState.map { it.isPlaying }.distinctUntilChanged(),
            bridge.handingOver,
        ) { s, state, playingHere, handingOver ->
            state.toDomain(paired = s != null).let { it.copy(controlling = it.isRemoteFor(playingHere || handingOver)) }
        }.stateIn(scope, SharingStarted.Eagerly, DeviceLink())

    init {
        scope.launch {
            // Controlling the computer counts as playing: the notification is its remote, and it
            // keeps working with the app in the background.
            combine(stored, foreground, handler.controlState.map { it.isPlaying }, link.map { it.controlling }) { s, inFront, playing, controlling ->
                s.takeIf { inFront || playing || controlling }
            }.distinctUntilChanged()
                .collectLatest { wanted ->
                    if (wanted != null) {
                        session.open(wanted.server, wanted.secret, deviceId(), deviceName())
                    } else {
                        delay(CLOSE_AFTER)
                        // Not while pairing: a claim's socket has no room and reads as OFF.
                        if (session.state.value.connection != LinkConnection.OFF) session.close()
                    }
                }
        }
        scope.launch {
            session.events.collect { if (it is LinkEvent.Kicked) dataStore.putString(KEY_LINK, "") }
        }
    }

    override fun setForeground(foreground: Boolean) {
        this.foreground.value = foreground
    }

    override suspend fun pair(
        server: String,
        code: String,
    ): PairResult =
        when (val r = session.claim(server, code)) {
            is ClaimResult.Paired -> {
                dataStore.putString(KEY_LINK, Json.encodeToString(StoredLink.serializer(), StoredLink(r.secret, server)))
                session.open(server, r.secret, deviceId(), deviceName())
                PairResult.Linked
            }
            ClaimResult.InvalidCode -> PairResult.InvalidCode
            ClaimResult.Unsupported -> PairResult.Unsupported
            ClaimResult.Unreachable -> PairResult.Unreachable
        }

    override fun unlink(deviceId: String) = session.unlink(deviceId)

    override fun unlinkThisPhone() {
        session.leave()
        scope.launch { dataStore.putString(KEY_LINK, "") }
    }

    override fun remote(
        action: LinkRemote,
        to: String?,
    ) = session.remote(
        when (action) {
            LinkRemote.Play -> RemoteAction.Play
            LinkRemote.Pause -> RemoteAction.Pause
            LinkRemote.Next -> RemoteAction.Next
            LinkRemote.Previous -> RemoteAction.Previous
            is LinkRemote.Seek -> RemoteAction.Seek(action.positionMs)
            is LinkRemote.SetVolume -> RemoteAction.SetVolume(action.volume.coerceIn(0f, 1f).toDouble())
            is LinkRemote.PlayTrack -> RemoteAction.PlayTrack(action.track.toProtocol())
            is LinkRemote.PlayTracks ->
                RemoteAction.PlayTracks(action.tracks.map { it.toProtocol() }, action.start, action.sourceId, action.sourceName, action.shuffle)
            is LinkRemote.PlayIndex -> RemoteAction.PlayIndex(action.index, action.id)
            is LinkRemote.Remove -> RemoteAction.Remove(action.index, action.id)
            is LinkRemote.Move -> RemoteAction.Move(action.from, action.to)
            is LinkRemote.PlayNext -> RemoteAction.PlayNext(action.tracks.map { it.toProtocol() }, action.from)
            is LinkRemote.AddToQueue -> RemoteAction.AddToQueue(action.tracks.map { it.toProtocol() }, action.from)
            LinkRemote.ClearQueued -> RemoteAction.ClearQueued
            is LinkRemote.SetShuffle -> RemoteAction.SetShuffle(action.on)
            is LinkRemote.SetRepeat -> RemoteAction.SetRepeat(action.mode)
            LinkRemote.TakeOver -> RemoteAction.TakeOver
        },
        to,
    )

    override fun continueHere() = bridge.continueHere()

    /** Made once per install and kept for good. */
    private suspend fun deviceId(): String =
        dataStore.getString(KEY_DEVICE_ID).first()?.takeIf { it.isNotBlank() }
            ?: newDeviceId().also { dataStore.putString(KEY_DEVICE_ID, it) }

    private companion object {
        const val KEY_LINK = "device_link"
        const val KEY_DEVICE_ID = "device_id"
        val CLOSE_AFTER = 30.seconds
    }
}

/**
 * Whether the app's player should be the remote for the device that plays: it is online and
 * playing, or paused less than [STALE_PAUSE_MS] ago. Paused longer, opening the app on the phone
 * means this phone again.
 */
@OptIn(ExperimentalTime::class)
private fun DeviceLink.isRemoteFor(busyHere: Boolean): Boolean {
    if (busyHere || !paired || connection != DomainConnection.Connected) return false
    if (player?.online != true || track == null) return false
    return isPlaying || Clock.System.now().toEpochMilliseconds() - updatedAtMs < STALE_PAUSE_MS
}

private const val STALE_PAUSE_MS = 10 * 60_000L

/** A random version-4 UUID. */
private fun newDeviceId(): String {
    val b = Random.nextBytes(16)
    b[6] = ((b[6].toInt() and 0x0f) or 0x40).toByte()
    b[8] = ((b[8].toInt() and 0x3f) or 0x80).toByte()
    val h = b.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    return "${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}"
}

private fun LinkState.toDomain(paired: Boolean): DeviceLink {
    val devices =
        users
            .filter { it.userId != selfId }
            .map { LinkedDevice(id = it.userId, name = it.username, online = it.isConnected, playing = it.userId == hostId && isPlaying) }
    return DeviceLink(
        paired = paired,
        connection =
            when (connection) {
                LinkConnection.OFF -> DomainConnection.Off
                LinkConnection.CONNECTING -> DomainConnection.Connecting
                LinkConnection.CONNECTED -> DomainConnection.Connected
                LinkConnection.UNSUPPORTED -> DomainConnection.Unsupported
            },
        devices = devices,
        player = devices.firstOrNull { it.id == hostId },
        track = currentTrack?.toRoomTrack(),
        queue = queue.map { it.toRoomTrack() },
        playerQueue =
            status?.let { st ->
                LinkQueue(
                    items = st.items.map { LinkQueueItem(it.track.toRoomTrack(), it.queued, it.queuedEnd, it.queuedFrom, it.autoplay) },
                    offset = st.offset,
                    current = st.current,
                    shuffle = st.shuffle,
                    repeat = st.repeat,
                    source = st.source,
                )
            },
        isPlaying = isPlaying,
        positionMs = position,
        // The server's clock is taken to be this device's wall clock, as ServerClock does.
        updatedAtMs = lastUpdate,
        volume = volume,
        error = error,
    )
}

private fun TrackInfo.toRoomTrack() = RoomTrack(id, title, artist, album, duration, thumbnail)

private fun RoomTrack.toProtocol() =
    TrackInfo(id = id, title = title, artist = artist, album = album, duration = durationMs, thumbnail = thumbnail)
