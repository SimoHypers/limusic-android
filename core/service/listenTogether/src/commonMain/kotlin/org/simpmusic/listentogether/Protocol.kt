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

    // Device link (Limusic's sync server only): the phone as a remote for the desktop.
    const val JOIN_LINK = "join_link"
    const val PAIR_START = "pair_start"
    const val PAIR_CLAIM = "pair_claim"

    /** Both directions: a control sent to another device, and the same control forwarded to it. */
    const val REMOTE = "remote"

    /** Both directions: the player's queue, sent by it and passed on to the others. */
    const val LINK_STATUS = "link_status"
    const val LINK_JOINED = "link_joined"
    const val PAIR_CODE = "pair_code"
    const val PAIRED = "paired"
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

// ───────────────────────────── device link ─────────────────────────────

/** Join (or create) the link room keyed by [secret]. Holding the secret is membership. */
data class JoinLinkPayload(
    val secret: String,
    val deviceId: String,
    val name: String,
)

/** Trade a pairing code shown on the desktop for the link's secret. */
data class PairClaimPayload(
    val code: String,
)

/** The reply to [JoinLinkPayload]. `roomCode` is empty: the server never echoes the secret. */
data class LinkJoinedPayload(
    val state: RoomState,
    /** The player's last [LinkStatus]. */
    val status: LinkStatus? = null,
)

/**
 * The player's queue for the other devices' player views. Mirrors `LinkStatus` in
 * `crates/listen-protocol`: a window of rows around the playing one.
 */
data class LinkStatus(
    val items: List<LinkItem> = emptyList(),
    /** Where `items[0]` is in the player's whole queue: remote actions name rows by queue index. */
    val offset: Int = 0,
    /** The playing row, in [items]. */
    val current: Int = 0,
    val shuffle: Boolean = false,
    /** "off" | "all" | "one" */
    val repeat: String = "off",
    val source: String? = null,
)

/** A queue row with the marks the queue view draws. */
data class LinkItem(
    val track: TrackInfo,
    /** Play next. */
    val queued: Boolean = false,
    /** Add to queue. */
    val queuedEnd: Boolean = false,
    val queuedFrom: String? = null,
    val autoplay: Boolean = false,
)

data class LinkStatusPayload(
    val status: LinkStatus,
)

data class PairCodePayload(
    val code: String,
    val expiresInS: Int,
)

data class PairedPayload(
    val secret: String,
)

/**
 * A control for another device: the player, unless [to] names one. [from] is set on the way in:
 * the sender's device id.
 */
data class RemotePayload(
    val action: RemoteAction,
    val from: String = "",
    val to: String? = null,
)

/** What a remote can ask of the player. Mirrors `RemoteAction` in `crates/listen-protocol`. */
sealed interface RemoteAction {
    data object Play : RemoteAction

    data object Pause : RemoteAction

    data object Next : RemoteAction

    data object Previous : RemoteAction

    data class Seek(
        val positionMs: Long,
    ) : RemoteAction

    /** 0.0..1.0 */
    data class SetVolume(
        val volume: Double,
    ) : RemoteAction

    /** One song, with its radio behind it. */
    data class PlayTrack(
        val track: TrackInfo,
    ) : RemoteAction

    /** A playlist, album or radio; [start] is the tapped row, null = just play it. */
    data class PlayTracks(
        val tracks: List<TrackInfo>,
        val start: Int? = null,
        /** Lets autoplay carry on with the list's radio when it runs out. */
        val sourceId: String? = null,
        val sourceName: String? = null,
        val shuffle: Boolean = false,
    ) : RemoteAction

    /** The queue row at [index] in the player's whole queue; [id] is its track, in case the queue moved. */
    data class PlayIndex(
        val index: Int,
        val id: String,
    ) : RemoteAction

    data class Remove(
        val index: Int,
        val id: String,
    ) : RemoteAction

    data class Move(
        val from: Int,
        val to: Int,
    ) : RemoteAction

    data class PlayNext(
        val tracks: List<TrackInfo>,
        val from: String? = null,
    ) : RemoteAction

    data class AddToQueue(
        val tracks: List<TrackInfo>,
        val from: String? = null,
    ) : RemoteAction

    data object ClearQueued : RemoteAction

    data class SetShuffle(
        val on: Boolean,
    ) : RemoteAction

    /** "off" | "all" | "one" */
    data class SetRepeat(
        val mode: String,
    ) : RemoteAction

    /** To a device that isn't playing: play what the player plays, from where it is now. */
    data object TakeOver : RemoteAction
}
