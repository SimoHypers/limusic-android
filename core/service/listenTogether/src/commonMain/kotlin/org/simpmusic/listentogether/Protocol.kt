/*
 * Listen Together's message model, ported from Metrolist (GPL-3.0, the same licence as this
 * project). Metrolist Project (C) 2026, Licensed under GPL-3.0 | See git history for contributors
 *
 * The app now talks to Limusic's server, whose JSON protocol shares this design; MessageCodec
 * translates between the two, so these classes are the session's model, not the wire format.
 */
package org.simpmusic.listentogether

import kotlinx.serialization.Serializable

/** Envelope `type` values. Client → server first, then server → client. */
object MessageTypes {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val KICK_USER = "kick_user"
    const val TRANSFER_HOST = "transfer_host"
    const val PING = "ping"
    const val REQUEST_SYNC = "request_sync"
    const val RECONNECT = "reconnect"
    const val SUGGEST_TRACK = "suggest_track"
    const val APPROVE_SUGGESTION = "approve_suggestion"
    const val REJECT_SUGGESTION = "reject_suggestion"

    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val ERROR = "error"
    const val PONG = "pong"
    const val HOST_CHANGED = "host_changed"
    const val KICKED = "kicked"
    const val SYNC_STATE = "sync_state"
    const val RECONNECTED = "reconnected"
    const val USER_RECONNECTED = "user_reconnected"
    const val USER_DISCONNECTED = "user_disconnected"
    const val SUGGESTION_RECEIVED = "suggestion_received"
    const val SUGGESTION_APPROVED = "suggestion_approved"
    const val SUGGESTION_REJECTED = "suggestion_rejected"

}

/** Values of [PlaybackActionPayload.action]. */
object PlaybackActions {
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val SEEK = "seek"
    const val CHANGE_TRACK = "change_track"
    const val SYNC_QUEUE = "sync_queue"
    const val SET_VOLUME = "set_volume"
}

/**
 * One track, as the room sees it.
 *
 * [id] is the YouTube videoId, which is why two different clients resolve the same row: both read
 * the same catalogue. Everything else is display metadata.
 */
@Serializable
data class TrackInfo(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    /** Milliseconds. */
    val duration: Long = 0L,
    val thumbnail: String = "",
    val suggestedBy: String = "",
)

@Serializable
data class UserInfo(
    val userId: String = "",
    val username: String = "",
    val isHost: Boolean = false,
    val isConnected: Boolean = false,
)

@Serializable
data class RoomState(
    val roomCode: String = "",
    val hostId: String = "",
    val users: List<UserInfo> = emptyList(),
    val currentTrack: TrackInfo? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val lastUpdate: Long = 0L,
    val volume: Float = 0f,
    val queue: List<TrackInfo> = emptyList(),
    val revision: Long = 0L,
)

// ───────────────────────────── client → server ─────────────────────────────

@Serializable
data class CreateRoomPayload(
    val username: String = "",
)

@Serializable
data class JoinRoomPayload(
    val roomCode: String = "",
    val username: String = "",
)

@Serializable
class LeaveRoomPayload

@Serializable
data class ApproveJoinPayload(
    val userId: String = "",
)

@Serializable
data class RejectJoinPayload(
    val userId: String = "",
    val reason: String = "",
)

/**
 * Every transport command, and the only message whose timing fields matter.
 *
 * [serverTime] and [capturedAtServerTime] are what let a late-arriving PLAY still land on the right
 * position: the receiver advances [position] by however long the frame spent in flight, measured on
 * the SERVER's clock rather than its own. See [ServerClock.positionAt].
 */
@Serializable
data class PlaybackActionPayload(
    val action: String = "",
    val trackId: String = "",
    /** Milliseconds. */
    val position: Long = 0L,
    val trackInfo: TrackInfo? = null,
    val insertNext: Boolean = false,
    val queue: List<TrackInfo> = emptyList(),
    val queueTitle: String = "",
    val volume: Float = 0f,
    val serverTime: Long = 0L,
    val revision: Long = 0L,
    val capturedAtServerTime: Long = 0L,
    /** change_track only: whether the new track should play. Limusic sends no PLAY after it. */
    val playing: Boolean? = null,
)

@Serializable
data class KickUserPayload(
    val userId: String = "",
    val reason: String = "",
)

@Serializable
data class TransferHostPayload(
    val newHostId: String = "",
)

@Serializable
data class SuggestTrackPayload(
    val trackInfo: TrackInfo? = null,
)

@Serializable
data class ApproveSuggestionPayload(
    val suggestionId: String = "",
)

@Serializable
data class RejectSuggestionPayload(
    val suggestionId: String = "",
    val reason: String = "",
)

@Serializable
data class ReconnectPayload(
    val sessionToken: String = "",
)

// ───────────────────────────── server → client ─────────────────────────────

@Serializable
data class RoomCreatedPayload(
    val roomCode: String = "",
    val userId: String = "",
    val sessionToken: String = "",
)

@Serializable
data class JoinRequestPayload(
    val userId: String = "",
    val username: String = "",
)

@Serializable
data class JoinApprovedPayload(
    val roomCode: String = "",
    val userId: String = "",
    val sessionToken: String = "",
    val state: RoomState? = null,
)

@Serializable
data class JoinRejectedPayload(
    val reason: String = "",
)

@Serializable
data class UserJoinedPayload(
    val userId: String = "",
    val username: String = "",
)

@Serializable
data class UserLeftPayload(
    val userId: String = "",
    val username: String = "",
)

@Serializable
data class ErrorPayload(
    val code: String = "",
    val message: String = "",
)

@Serializable
data class HostChangedPayload(
    val newHostId: String = "",
    val newHostName: String = "",
)

@Serializable
data class KickedPayload(
    val reason: String = "",
)

@Serializable
data class SyncStatePayload(
    val currentTrack: TrackInfo? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val lastUpdate: Long = 0L,
    val queue: List<TrackInfo> = emptyList(),
    val volume: Float = 0f,
    val revision: Long = 0L,
)

@Serializable
data class ReconnectedPayload(
    val roomCode: String = "",
    val userId: String = "",
    val state: RoomState? = null,
    val isHost: Boolean = false,
)

@Serializable
data class UserReconnectedPayload(
    val userId: String = "",
    val username: String = "",
)

@Serializable
data class UserDisconnectedPayload(
    val userId: String = "",
    val username: String = "",
)

@Serializable
data class SuggestionReceivedPayload(
    val suggestionId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val trackInfo: TrackInfo? = null,
)

@Serializable
data class SuggestionApprovedPayload(
    val suggestionId: String = "",
    val trackInfo: TrackInfo? = null,
)

@Serializable
data class SuggestionRejectedPayload(
    val suggestionId: String = "",
    val reason: String = "",
)

// ───────────────────────── capability negotiation ─────────────────────────

