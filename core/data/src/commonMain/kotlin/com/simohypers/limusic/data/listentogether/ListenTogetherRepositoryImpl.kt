package com.simohypers.limusic.data.listentogether

import com.simohypers.limusic.domain.data.model.listentogether.ListenTogetherRoom
import com.simohypers.limusic.domain.data.model.listentogether.RoomConnection
import com.simohypers.limusic.domain.data.model.listentogether.RoomJoinRequest
import com.simohypers.limusic.domain.data.model.listentogether.RoomMember
import com.simohypers.limusic.domain.data.model.listentogether.RoomSuggestion
import com.simohypers.limusic.domain.data.model.listentogether.RoomTrack
import com.simohypers.limusic.domain.repository.ListenTogetherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.simohypers.limusic.listentogether.ConnectionState
import com.simohypers.limusic.listentogether.ListenTogetherSession
import com.simohypers.limusic.listentogether.ListenTogetherState
import com.simohypers.limusic.listentogether.PendingJoin
import com.simohypers.limusic.listentogether.PendingSuggestion
import com.simohypers.limusic.listentogether.RoomMember as ProtocolMember
import com.simohypers.limusic.listentogether.TrackInfo

/**
 * The only place in the app that knows the Listen Together protocol exists.
 *
 * Everything above talks to [ListenTogetherRepository] in domain types; the protocol types stay
 * behind this boundary because they are not ours to change — they mirror Metrolist's `.proto`, and
 * a renamed field there is a client that cannot join a room.
 */
class ListenTogetherRepositoryImpl(
    private val session: ListenTogetherSession,
    scope: CoroutineScope,
) : ListenTogetherRepository {
    private val _room = MutableStateFlow(ListenTogetherRoom())
    override val room: StateFlow<ListenTogetherRoom> = _room.asStateFlow()

    init {
        scope.launch { session.state.collect { _room.value = it.toDomain() } }
    }

    override fun connect() = session.connect()

    override fun disconnect() = session.disconnect()

    override fun createRoom(username: String) {
        session.createRoom(username)
    }

    override fun joinRoom(
        roomCode: String,
        username: String,
    ) {
        session.joinRoom(roomCode, username)
    }

    override fun cancelJoin() = session.cancelJoin()

    override fun leaveRoom() {
        session.leaveRoom()
    }

    override fun approveJoin(userId: String) {
        session.approveJoin(userId)
    }

    override fun rejectJoin(userId: String) {
        session.rejectJoin(userId)
    }

    override fun approveSuggestion(suggestionId: String) {
        session.approveSuggestion(suggestionId)
    }

    override fun rejectSuggestion(suggestionId: String) {
        session.rejectSuggestion(suggestionId)
    }

    override fun kickUser(userId: String) {
        session.kickUser(userId)
    }

    override fun transferHost(userId: String) {
        session.transferHost(userId)
    }

    override fun suggestTracks(tracks: List<RoomTrack>) {
        session.suggestTracks(tracks.map { it.toProtocol() })
    }

    override fun requestSync() {
        session.requestSync()
    }

    override fun clearError() = session.clearError()
}

private fun ListenTogetherState.toDomain() =
    ListenTogetherRoom(
        connection = connection.toDomain(),
        roomCode = roomCode,
        selfUserId = selfUserId,
        isHost = isHost,
        members = members.map { it.toDomain() },
        joinRequests = joinRequests.map { it.toDomain() },
        suggestions = suggestions.map { it.toDomain() },
        currentTrack = currentTrack?.toDomain(),
        queue = queue.map { it.toDomain() },
        isPlaying = isPlaying,
        position = position,
        positionAt = lastActionServerTime,
        pendingJoinCode = pendingJoinCode,
        error = error,
    )

private fun ConnectionState.toDomain(): RoomConnection =
    when (this) {
        ConnectionState.Disconnected -> RoomConnection.Disconnected
        ConnectionState.Connecting -> RoomConnection.Connecting
        is ConnectionState.Connected -> RoomConnection.Connected(serverVersion)
        is ConnectionState.Failed -> RoomConnection.Failed(reason)
    }

private fun ProtocolMember.toDomain() =
    RoomMember(
        userId = userId,
        username = username,
        isHost = isHost,
        isConnected = isConnected,
    )

private fun PendingJoin.toDomain() = RoomJoinRequest(userId = userId, username = username)

private fun PendingSuggestion.toDomain() =
    RoomSuggestion(
        suggestionId = suggestionId,
        fromUsername = fromUsername,
        track = track.toDomain(),
    )

private fun TrackInfo.toDomain() =
    RoomTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = duration,
        thumbnail = thumbnail,
    )

private fun RoomTrack.toProtocol() =
    TrackInfo(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = durationMs,
        thumbnail = thumbnail,
    )
