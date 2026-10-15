package com.maxrave.domain.repository

import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomTrack
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's view of a Listen Together room.
 *
 * The implementation owns the socket and the protocol; nothing above this interface knows either
 * exists, so the wire format (Limusic's server protocol) never reaches the ViewModels and screens.
 */
interface ListenTogetherRepository {
    val room: StateFlow<ListenTogetherRoom>

    fun connect()

    fun disconnect()

    fun createRoom(username: String)

    fun joinRoom(
        roomCode: String,
        username: String,
    )

    /** Gives up on a join that has not been answered. Local only. */
    fun cancelJoin()

    fun leaveRoom()

    fun approveJoin(userId: String)

    fun rejectJoin(userId: String)

    fun approveSuggestion(suggestionId: String)

    fun rejectSuggestion(suggestionId: String)

    fun kickUser(userId: String)

    fun transferHost(userId: String)

    fun suggestTrack(track: RoomTrack)

    /** Re-syncs a guest with the room after it has driven its own transport. */
    fun requestSync()

    fun clearError()
}
