package com.simohypers.limusic.domain.data.model.listentogether

/**
 * Listen Together, as the rest of the app sees it.
 *
 * These mirror the protocol types in `core/service/listenTogether` on purpose. The service module
 * is the wire format — its shapes are dictated by Metrolist's `.proto` and may not be changed —
 * while these belong to this app and can. Repeating them is what keeps the UI from importing the
 * service directly and pinning our screens to someone else's schema.
 */

/** Where the socket is, independent of whether a room has been joined. */
sealed interface RoomConnection {
    data object Disconnected : RoomConnection

    data object Connecting : RoomConnection

    data class Connected(val serverVersion: String) : RoomConnection

    /** Retries are exhausted or the server refused us; the user has to act. */
    data class Failed(val reason: String) : RoomConnection
}

data class RoomMember(
    val userId: String,
    val username: String,
    val isHost: Boolean,
    val isConnected: Boolean,
)

data class RoomJoinRequest(
    val userId: String,
    val username: String,
)

data class RoomTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val thumbnail: String = "",
)

data class RoomSuggestion(
    val suggestionId: String,
    val fromUsername: String,
    val track: RoomTrack,
)

data class ListenTogetherRoom(
    val connection: RoomConnection = RoomConnection.Disconnected,
    val roomCode: String? = null,
    val selfUserId: String = "",
    val isHost: Boolean = false,
    val members: List<RoomMember> = emptyList(),
    val joinRequests: List<RoomJoinRequest> = emptyList(),
    val suggestions: List<RoomSuggestion> = emptyList(),
    val currentTrack: RoomTrack? = null,
    val queue: List<RoomTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    /** Server time [position] was stated at; 0 when unknown. Changes with every sync, even an unchanged one. */
    val positionAt: Long = 0L,
    /** The code we asked to join and have not heard back about. */
    val pendingJoinCode: String? = null,
    val error: String? = null,
) {
    val inRoom: Boolean get() = roomCode != null
    val isConnected: Boolean get() = connection is RoomConnection.Connected
}
