package org.simpmusic.listentogether

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Speaks Limusic's Listen Together protocol (`crates/listen-protocol` in the Limusic repo): one JSON
 * object per text frame, tagged on `type`, snake_case fields.
 *
 * The layer above was written against Metrolist's message model, and the two protocols share their
 * design (host-led rooms, approval, suggestions, server-extrapolated position), so this translates
 * between Limusic's JSON and the payload classes in Protocol.kt instead of rewriting the session.
 */
internal class MessageCodec {
    /** One outgoing frame, or null for a message Limusic's server has no counterpart for. */
    fun encode(
        msgType: String,
        payload: Any?,
    ): String? {
        val obj =
            when (msgType) {
                MessageTypes.CREATE_ROOM ->
                    msg("create_room") { put("username", (payload as CreateRoomPayload).username) }
                MessageTypes.JOIN_ROOM -> {
                    val p = payload as JoinRoomPayload
                    msg("join_room") {
                        put("room_code", p.roomCode)
                        put("username", p.username)
                    }
                }
                MessageTypes.LEAVE_ROOM -> msg("leave_room")
                MessageTypes.APPROVE_JOIN -> msg("approve_join") { put("user_id", (payload as ApproveJoinPayload).userId) }
                MessageTypes.REJECT_JOIN -> msg("reject_join") { put("user_id", (payload as RejectJoinPayload).userId) }
                MessageTypes.KICK_USER -> msg("kick_user") { put("user_id", (payload as KickUserPayload).userId) }
                MessageTypes.TRANSFER_HOST -> msg("transfer_host") { put("user_id", (payload as TransferHostPayload).newHostId) }
                MessageTypes.PLAYBACK_ACTION -> encodePlayback(payload as PlaybackActionPayload) ?: return null
                MessageTypes.SUGGEST_TRACK -> {
                    val track = (payload as SuggestTrackPayload).trackInfo ?: return null
                    msg("suggest") { put("track", track.toJson()) }
                }
                MessageTypes.APPROVE_SUGGESTION ->
                    msg("approve_suggestion") { put("id", (payload as ApproveSuggestionPayload).suggestionId) }
                MessageTypes.REJECT_SUGGESTION ->
                    msg("reject_suggestion") { put("id", (payload as RejectSuggestionPayload).suggestionId) }
                MessageTypes.REQUEST_SYNC -> msg("request_sync")
                MessageTypes.RECONNECT -> msg("reconnect") { put("session_token", (payload as ReconnectPayload).sessionToken) }
                MessageTypes.PING -> msg("ping")
                MessageTypes.JOIN_LINK -> {
                    val p = payload as JoinLinkPayload
                    msg("join_link") {
                        put("secret", p.secret)
                        put("device_id", p.deviceId)
                        put("name", p.name)
                    }
                }
                MessageTypes.PAIR_START -> msg("pair_start")
                MessageTypes.PAIR_CLAIM -> msg("pair_claim") { put("code", (payload as PairClaimPayload).code) }
                MessageTypes.REMOTE -> {
                    val p = payload as RemotePayload
                    msg("remote") {
                        put("action", p.action.toJson())
                        p.to?.let { put("to", it) }
                    }
                }
                MessageTypes.LINK_STATUS -> msg("link_status") { put("status", (payload as LinkStatusPayload).status.toJson()) }
                // buffer_ready, chat and the rest have no counterpart: Limusic rooms do not wait for
                // everyone to buffer.
                else -> return null
            }
        return obj.toString()
    }

    /**
     * One incoming frame as (MessageTypes constant, payload), or null when it is not JSON. A type
     * this client does not model comes back with a null payload, so a newer server cannot break it.
     */
    fun decode(text: String): Pair<String, Any?>? {
        val o = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        return when (val type = o.str("type")) {
            "room_created" ->
                MessageTypes.ROOM_CREATED to
                    RoomCreatedPayload(roomCode = o.str("room_code"), userId = o.str("user_id"), sessionToken = o.str("session_token"))
            "join_request" -> MessageTypes.JOIN_REQUEST to JoinRequestPayload(userId = o.str("user_id"), username = o.str("username"))
            "join_approved" -> {
                val state = o.obj("state")?.toRoomState()
                MessageTypes.JOIN_APPROVED to
                    JoinApprovedPayload(
                        roomCode = state?.roomCode.orEmpty(),
                        userId = o.str("user_id"),
                        sessionToken = o.str("session_token"),
                        state = state,
                    )
            }
            "join_rejected" -> MessageTypes.JOIN_REJECTED to JoinRejectedPayload(reason = o.str("reason"))
            "reconnected" -> {
                val state = o.obj("state")?.toRoomState()
                MessageTypes.RECONNECTED to
                    ReconnectedPayload(
                        roomCode = state?.roomCode.orEmpty(),
                        userId = o.str("user_id"),
                        state = state,
                        isHost = o.bool("is_host"),
                    )
            }
            "user_joined" -> {
                val user = o.obj("user")
                MessageTypes.USER_JOINED to UserJoinedPayload(userId = user?.str("user_id").orEmpty(), username = user?.str("username").orEmpty())
            }
            "user_left" -> MessageTypes.USER_LEFT to UserLeftPayload(userId = o.str("user_id"))
            "user_disconnected" -> MessageTypes.USER_DISCONNECTED to UserDisconnectedPayload(userId = o.str("user_id"))
            "user_reconnected" -> MessageTypes.USER_RECONNECTED to UserReconnectedPayload(userId = o.str("user_id"))
            "host_changed" -> MessageTypes.HOST_CHANGED to HostChangedPayload(newHostId = o.str("host_id"))
            "sync_playback" -> MessageTypes.SYNC_PLAYBACK to o.toPlayback()
            "sync_state" -> {
                val s = o.obj("state")?.toRoomState() ?: RoomState()
                MessageTypes.SYNC_STATE to
                    SyncStatePayload(
                        currentTrack = s.currentTrack,
                        isPlaying = s.isPlaying,
                        position = s.position,
                        lastUpdate = s.lastUpdate,
                        queue = s.queue,
                        volume = s.volume,
                    )
            }
            "suggestion_received" -> {
                val s = o.obj("suggestion")
                MessageTypes.SUGGESTION_RECEIVED to
                    SuggestionReceivedPayload(
                        suggestionId = s?.str("id").orEmpty(),
                        fromUserId = s?.str("from_user_id").orEmpty(),
                        fromUsername = s?.str("from_username").orEmpty(),
                        trackInfo = s?.obj("track")?.toTrack(),
                    )
            }
            "suggestion_approved" -> MessageTypes.SUGGESTION_APPROVED to SuggestionApprovedPayload(suggestionId = o.str("id"))
            "suggestion_rejected" ->
                MessageTypes.SUGGESTION_REJECTED to SuggestionRejectedPayload(suggestionId = o.str("id"), reason = o.str("reason"))
            "kicked" -> MessageTypes.KICKED to KickedPayload(reason = o.str("reason"))
            "error" -> MessageTypes.ERROR to ErrorPayload(code = o.str("code"), message = o.str("message"))
            "pong" -> MessageTypes.PONG to null
            "link_joined" ->
                MessageTypes.LINK_JOINED to
                    LinkJoinedPayload(state = o.obj("state")?.toRoomState() ?: RoomState(), status = o.obj("status")?.toLinkStatus())
            "link_status" -> MessageTypes.LINK_STATUS to o.obj("status")?.toLinkStatus()?.let { LinkStatusPayload(it) }
            "pair_code" -> MessageTypes.PAIR_CODE to PairCodePayload(code = o.str("code"), expiresInS = o.long("expires_in_s").toInt())
            "paired" -> MessageTypes.PAIRED to PairedPayload(secret = o.str("secret"))
            "remote" -> {
                val action = o.obj("action")?.toRemoteAction()
                MessageTypes.REMOTE to action?.let { RemotePayload(action = it, from = o.str("from")) }
            }
            else -> type to null
        }
    }

    private fun encodePlayback(p: PlaybackActionPayload): JsonObject? {
        val kind =
            when (p.action) {
                PlaybackActions.PLAY, PlaybackActions.PAUSE, PlaybackActions.SEEK, PlaybackActions.CHANGE_TRACK,
                PlaybackActions.SYNC_QUEUE, PlaybackActions.SET_VOLUME,
                -> p.action
                else -> return null
            }
        return msg("playback") {
            put("kind", kind)
            put("position_ms", p.position)
            put("server_time_ms", 0)
            p.trackInfo?.let { put("track", it.toJson()) }
            if (kind == PlaybackActions.CHANGE_TRACK || kind == PlaybackActions.SYNC_QUEUE) {
                put("queue", buildJsonArray { p.queue.forEach { add(it.toJson()) } })
            }
            // Limusic guests start or hold a new track from this flag alone; there is no PLAY after it.
            put("playing", p.playing ?: (kind == PlaybackActions.PLAY))
            put("volume", p.volume.toDouble())
        }
    }

    private fun JsonObject.toPlayback(): PlaybackActionPayload {
        val kind = str("kind")
        val track = obj("track")?.toTrack()
        val serverTime = long("server_time_ms")
        return PlaybackActionPayload(
            action = kind,
            trackId = track?.id.orEmpty(),
            position = long("position_ms"),
            trackInfo = track,
            queue = (this["queue"] as? JsonArray)?.map { it.jsonObject.toTrack() }.orEmpty(),
            volume = (this["volume"]?.jsonPrimitive?.let { runCatching { it.double }.getOrNull() } ?: 0.0).toFloat(),
            serverTime = serverTime,
            capturedAtServerTime = serverTime,
            // Only change_track carries a meaningful flag; play and pause say it with their kind.
            playing = if (kind == PlaybackActions.CHANGE_TRACK) bool("playing") else null,
        )
    }

    private fun RemoteAction.toJson() =
        buildJsonObject {
            when (val a = this@toJson) {
                RemoteAction.Play -> put("kind", "play")
                RemoteAction.Pause -> put("kind", "pause")
                RemoteAction.Next -> put("kind", "next")
                RemoteAction.Previous -> put("kind", "previous")
                is RemoteAction.Seek -> {
                    put("kind", "seek")
                    put("position_ms", a.positionMs)
                }
                is RemoteAction.SetVolume -> {
                    put("kind", "set_volume")
                    put("volume", a.volume)
                }
                is RemoteAction.PlayTrack -> {
                    put("kind", "play_track")
                    put("track", a.track.toJson())
                }
                is RemoteAction.PlayTracks -> {
                    put("kind", "play_tracks")
                    put("tracks", a.tracks.toJson())
                    a.start?.let { put("start", it) }
                    a.sourceId?.let { put("source_id", it) }
                    a.sourceName?.let { put("source_name", it) }
                    if (a.shuffle) put("shuffle", true)
                }
                is RemoteAction.PlayIndex -> {
                    put("kind", "play_index")
                    put("index", a.index)
                    put("id", a.id)
                }
                is RemoteAction.Remove -> {
                    put("kind", "remove")
                    put("index", a.index)
                    put("id", a.id)
                }
                is RemoteAction.Move -> {
                    put("kind", "move")
                    put("from", a.from)
                    put("to", a.to)
                }
                is RemoteAction.PlayNext -> {
                    put("kind", "play_next")
                    put("tracks", a.tracks.toJson())
                    a.from?.let { put("from", it) }
                }
                is RemoteAction.AddToQueue -> {
                    put("kind", "add_to_queue")
                    put("tracks", a.tracks.toJson())
                    a.from?.let { put("from", it) }
                }
                RemoteAction.ClearQueued -> put("kind", "clear_queued")
                is RemoteAction.SetShuffle -> {
                    put("kind", "set_shuffle")
                    put("on", a.on)
                }
                is RemoteAction.SetRepeat -> {
                    put("kind", "set_repeat")
                    put("mode", a.mode)
                }
                is RemoteAction.SetAutoplay -> {
                    put("kind", "set_autoplay")
                    put("on", a.on)
                }
                RemoteAction.TakeOver -> put("kind", "take_over")
            }
        }

    private fun List<TrackInfo>.toJson() = buildJsonArray { forEach { add(it.toJson()) } }

    private fun JsonObject.tracks(key: String) = (this[key] as? JsonArray)?.map { it.jsonObject.toTrack() }.orEmpty()

    private fun JsonObject.int(key: String) = long(key).toInt()

    private fun JsonObject.optStr(key: String) = this[key]?.jsonPrimitive?.contentOrNull

    private fun LinkStatus.toJson() =
        buildJsonObject {
            put(
                "items",
                buildJsonArray {
                    items.forEach { item ->
                        add(
                            buildJsonObject {
                                item.track.toJson().forEach { (k, v) -> put(k, v) }
                                if (item.queued) put("queued", true)
                                if (item.queuedEnd) put("queued_end", true)
                                item.queuedFrom?.let { put("queued_from", it) }
                                if (item.autoplay) put("autoplay", true)
                            },
                        )
                    }
                },
            )
            put("offset", offset)
            put("current", current)
            put("shuffle", shuffle)
            put("repeat", repeat)
            source?.let { put("source", it) }
            autoplay?.let { put("autoplay", it) }
        }

    private fun JsonObject.toLinkStatus() =
        LinkStatus(
            items =
                (this["items"] as? JsonArray)?.map {
                    val o = it.jsonObject
                    LinkItem(
                        track = o.toTrack(),
                        queued = o.bool("queued"),
                        queuedEnd = o.bool("queued_end"),
                        queuedFrom = o.optStr("queued_from"),
                        autoplay = o.bool("autoplay"),
                    )
                }.orEmpty(),
            offset = int("offset"),
            current = int("current"),
            shuffle = bool("shuffle"),
            repeat = str("repeat").ifBlank { "off" },
            source = optStr("source"),
            autoplay = this["autoplay"]?.jsonPrimitive?.booleanOrNull,
        )

    private fun JsonObject.toRemoteAction(): RemoteAction? =
        when (str("kind")) {
            "play" -> RemoteAction.Play
            "pause" -> RemoteAction.Pause
            "next" -> RemoteAction.Next
            "previous" -> RemoteAction.Previous
            "seek" -> RemoteAction.Seek(long("position_ms"))
            "set_volume" -> RemoteAction.SetVolume(this["volume"]?.jsonPrimitive?.let { runCatching { it.double }.getOrNull() } ?: 1.0)
            "play_track" -> obj("track")?.toTrack()?.let { RemoteAction.PlayTrack(it) }
            "play_tracks" ->
                RemoteAction.PlayTracks(
                    tracks = tracks("tracks"),
                    start = this["start"]?.let { int("start") },
                    sourceId = optStr("source_id"),
                    sourceName = optStr("source_name"),
                    shuffle = bool("shuffle"),
                )
            "play_index" -> RemoteAction.PlayIndex(int("index"), str("id"))
            "remove" -> RemoteAction.Remove(int("index"), str("id"))
            "move" -> RemoteAction.Move(int("from"), int("to"))
            "play_next" -> RemoteAction.PlayNext(tracks("tracks"), optStr("from"))
            "add_to_queue" -> RemoteAction.AddToQueue(tracks("tracks"), optStr("from"))
            "clear_queued" -> RemoteAction.ClearQueued
            "set_shuffle" -> RemoteAction.SetShuffle(bool("on"))
            "set_repeat" -> RemoteAction.SetRepeat(str("mode"))
            "set_autoplay" -> RemoteAction.SetAutoplay(bool("on"))
            "take_over" -> RemoteAction.TakeOver
            else -> null
        }

    private fun JsonObject.toRoomState() =
        RoomState(
            roomCode = str("room_code"),
            hostId = str("host_id"),
            users =
                (this["users"] as? JsonArray)?.map {
                    val u = it.jsonObject
                    UserInfo(userId = u.str("user_id"), username = u.str("username"), isHost = u.bool("is_host"), isConnected = u.bool("is_connected"))
                }.orEmpty(),
            currentTrack = obj("current_track")?.toTrack(),
            isPlaying = bool("is_playing"),
            position = long("position_ms"),
            lastUpdate = long("last_update_ms"),
            volume = (this["volume"]?.jsonPrimitive?.let { runCatching { it.double }.getOrNull() } ?: 0.0).toFloat(),
            queue = (this["queue"] as? JsonArray)?.map { it.jsonObject.toTrack() }.orEmpty(),
        )

    private fun JsonObject.toTrack() =
        TrackInfo(
            id = str("id"),
            title = str("title"),
            artist = str("artist"),
            duration = long("duration_ms"),
            thumbnail = str("thumbnail"),
            suggestedBy = str("queued_by"),
        )

    private fun TrackInfo.toJson() =
        buildJsonObject {
            put("id", id)
            put("title", title)
            put("artist", artist)
            if (thumbnail.isNotBlank()) put("thumbnail", thumbnail)
            put("duration_ms", duration)
            if (suggestedBy.isNotBlank()) put("queued_by", suggestedBy)
        }

    private fun msg(
        type: String,
        fields: JsonObjectBuilder.() -> Unit = {},
    ) = buildJsonObject {
        put("type", type)
        fields()
    }

    private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.long(key: String) = this[key]?.jsonPrimitive?.let { runCatching { it.long }.getOrNull() } ?: 0L

    private fun JsonObject.bool(key: String) = this[key]?.jsonPrimitive?.booleanOrNull ?: false

    private fun JsonObject.obj(key: String) = this[key] as? JsonObject
}
