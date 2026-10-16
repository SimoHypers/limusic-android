package com.maxrave.domain.data.model.listentogether

/**
 * The device link as the app sees it: this phone paired with Limusic desktop, as a remote and for
 * Continue here. The wire protocol stays behind [com.maxrave.domain.repository.DeviceLinkRepository].
 */
data class DeviceLink(
    val paired: Boolean = false,
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
    val error: String? = null,
) {
    /** The position now, extrapolated while playing. */
    fun livePositionMs(nowMs: Long): Long {
        val p = if (isPlaying && updatedAtMs > 0L) positionMs + (nowMs - updatedAtMs).coerceAtLeast(0L) else positionMs
        val duration = track?.durationMs ?: 0L
        return if (duration > 0L) p.coerceIn(0L, duration) else p.coerceAtLeast(0L)
    }
}

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

    data class PlayTrack(
        val track: RoomTrack,
    ) : LinkRemote
}

enum class PairResult { Linked, InvalidCode, Unsupported, Unreachable }
