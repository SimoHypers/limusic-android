package com.maxrave.domain.data.model.listentogether

/**
 * The device link as the app sees it: this phone paired with Limusic desktop, as a remote and for
 * Continue here. The wire protocol stays behind [com.maxrave.domain.repository.DeviceLinkRepository].
 */
data class DeviceLink(
    val paired: Boolean = false,
    /** This phone's name, as the other devices list it. */
    val selfName: String = "",
    val connection: LinkConnection = LinkConnection.Off,
    /** The other linked devices. */
    val devices: List<LinkedDevice> = emptyList(),
    /** Another device that is playing or played last, or null when this phone is the player. */
    val player: LinkedDevice? = null,
    /** What the player plays. A blank id is a local file on the desktop. */
    val track: RoomTrack? = null,
    val isPlaying: Boolean = false,
    /** The player's position at [updatedAtMs] (wall clock). */
    val positionMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    /** 0f..1f */
    val volume: Float = 1f,
    /** What the player plays after [track]. */
    val queue: List<RoomTrack> = emptyList(),
    /** The player's queue view, shuffle and repeat, once it has sent them. */
    val playerQueue: LinkQueue? = null,
    /**
     * This phone is the remote for [player]: the app's own player shows that device's playback and
     * its controls go there. True while the player is online, playing or paused only a little
     * while ago, and this phone isn't playing or taking over itself.
     */
    val controlling: Boolean = false,
    val error: String? = null,
) {
    /** The position now, extrapolated while playing. */
    fun livePositionMs(nowMs: Long): Long {
        val p = if (isPlaying && updatedAtMs > 0L) positionMs + (nowMs - updatedAtMs).coerceAtLeast(0L) else positionMs
        val duration = track?.durationMs ?: 0L
        return if (duration > 0L) p.coerceIn(0L, duration) else p.coerceAtLeast(0L)
    }
}

/** The player's queue as it sent it: a window of rows around the one playing. */
data class LinkQueue(
    val items: List<LinkQueueItem>,
    /** Where `items[0]` is in the player's whole queue; remote actions name rows by queue index. */
    val offset: Int,
    /** The playing row, in [items]. */
    val current: Int,
    val shuffle: Boolean,
    /** "off" | "all" | "one" */
    val repeat: String,
    /** The playlist or album it plays from. */
    val source: String?,
)

data class LinkQueueItem(
    val track: RoomTrack,
    /** Play next. */
    val queued: Boolean,
    /** Add to queue. */
    val queuedEnd: Boolean,
    val queuedFrom: String?,
    val autoplay: Boolean,
)

data class LinkedDevice(
    val id: String,
    val name: String,
    val online: Boolean,
    val playing: Boolean,
)

enum class LinkConnection { Off, Connecting, Connected, Unsupported }

/** A control for the device that is playing. */
sealed interface LinkRemote {
    data object Play : LinkRemote

    data object Pause : LinkRemote

    data object Next : LinkRemote

    data object Previous : LinkRemote

    data class Seek(
        val positionMs: Long,
    ) : LinkRemote

    /** 0f..1f */
    data class SetVolume(
        val volume: Float,
    ) : LinkRemote

    /** One song, with its radio behind it. */
    data class PlayTrack(
        val track: RoomTrack,
    ) : LinkRemote

    /** A playlist, album or radio; [start] is the tapped row, null = just play it. */
    data class PlayTracks(
        val tracks: List<RoomTrack>,
        val start: Int?,
        val sourceId: String?,
        val sourceName: String?,
        val shuffle: Boolean,
    ) : LinkRemote

    /** Queue rows by their index in the player's whole queue, with the track meant. */
    data class PlayIndex(
        val index: Int,
        val id: String,
    ) : LinkRemote

    data class Remove(
        val index: Int,
        val id: String,
    ) : LinkRemote

    data class Move(
        val from: Int,
        val to: Int,
    ) : LinkRemote

    data class PlayNext(
        val tracks: List<RoomTrack>,
        val from: String?,
    ) : LinkRemote

    data class AddToQueue(
        val tracks: List<RoomTrack>,
        val from: String?,
    ) : LinkRemote

    data object ClearQueued : LinkRemote

    data class SetShuffle(
        val on: Boolean,
    ) : LinkRemote

    /** "off" | "all" | "one" */
    data class SetRepeat(
        val mode: String,
    ) : LinkRemote

    /** Sent to a device that isn't playing: it plays what the player plays (this phone's music, to the computer). */
    data object TakeOver : LinkRemote
}

enum class PairResult { Linked, InvalidCode, Unsupported, Unreachable }
