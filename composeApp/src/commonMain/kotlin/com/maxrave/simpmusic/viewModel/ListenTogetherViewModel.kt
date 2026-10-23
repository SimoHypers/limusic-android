package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.listentogether.ListenTogetherPlaybackBridge
import com.maxrave.data.listentogether.ListenTogetherPrefs
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomConnection
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.ListenTogetherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Listen Together, laid out and behaving like Limusic desktop's dialog: no separate connect step
 * (Start or Join connects first), the server lives in the Host tab, invites are `CODE@host` or a
 * bare code on the default server.
 */
class ListenTogetherViewModel(
    private val repository: ListenTogetherRepository,
    private val dataStore: DataStoreManager,
    bridge: ListenTogetherPlaybackBridge,
) : ViewModel() {
    // Above init: the collector there runs inside the constructor (Main.immediate, and a StateFlow
    // replays at once), so while in a room it writes this before anything declared below init
    // exists. Declared below, it was null there and the screen crashed.
    private val _requesting = MutableStateFlow(false)

    init {
        // The bridge is a singleton that outlives this screen; starting it here is simply the
        // first moment anything asks for it. Koin's `createdAtStart` does not fire for modules
        // added with loadKoinModules, so without an explicit injection it is never constructed
        // at all, and then a room syncs membership while no audio follows anyone.
        bridge.start()
        viewModelScope.launch {
            repository.room.collect {
                if (it.inRoom || it.error != null || it.connection is RoomConnection.Failed) _requesting.value = false
            }
        }
    }

    val state: StateFlow<ListenTogetherRoom> =
        repository.room.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.room.value)

    /** The custom server, or empty on the default one (stored that way, as on desktop). */
    val serverUrl: StateFlow<String> =
        dataStore
            .getString(ListenTogetherPrefs.SERVER_URL)
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** The name used last time, to seed the field. */
    val savedName: StateFlow<String> =
        dataStore
            .getString(KEY_NAME)
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Between pressing Start or Join and being in the room. */
    val requesting: StateFlow<Boolean> = _requesting.asStateFlow()

    fun host(
        name: String,
        server: String,
    ) = start(name, normalize(server)) { repository.createRoom(name) }

    /** False when [invite] holds no room code. */
    fun join(
        name: String,
        invite: String,
    ): Boolean {
        val (server, code) = parseInvite(invite) ?: return false
        if (code.isEmpty()) return false
        // A bare code is what a host on the default server sends, so it means the default.
        start(name, normalize(server)) { repository.joinRoom(code, name) }
        return true
    }

    private fun start(
        name: String,
        server: String,
        send: () -> Unit,
    ) = viewModelScope.launch {
        _requesting.value = true
        dataStore.putString(KEY_NAME, name)
        if (server != dataStore.getString(ListenTogetherPrefs.SERVER_URL).first().orEmpty()) {
            dataStore.putString(ListenTogetherPrefs.SERVER_URL, server)
            repository.disconnect()
        }
        if (!repository.room.value.isConnected) {
            repository.connect()
            val connected =
                withTimeoutOrNull(CONNECT_TIMEOUT) {
                    repository.room.first { it.isConnected || it.connection is RoomConnection.Failed }
                }?.isConnected == true
            if (!connected) {
                _requesting.value = false
                return@launch
            }
        }
        send()
    }

    /** Gives up on a room not yet entered and closes the connection. */
    fun cancel() {
        _requesting.value = false
        repository.cancelJoin()
        repository.disconnect()
    }

    fun leave() = repository.leaveRoom()

    fun approveJoin(userId: String) = repository.approveJoin(userId)

    fun rejectJoin(userId: String) = repository.rejectJoin(userId)

    fun approveSuggestion(id: String) = repository.approveSuggestion(id)

    fun rejectSuggestion(id: String) = repository.rejectSuggestion(id)

    fun kick(userId: String) = repository.kickUser(userId)

    fun transferHost(userId: String) = repository.transferHost(userId)

    fun resync() = repository.requestSync()

    fun clearError() = repository.clearError()

    /** The default is stored as empty, so typing it in by hand still mints bare invites. */
    private fun normalize(url: String): String = url.trim().let { if (it == DEFAULT_SERVER_URL) "" else it }

    companion object {
        const val DEFAULT_SERVER_URL = ListenTogetherPrefs.DEFAULT_SERVER_URL
        private const val KEY_NAME = "lt_name"
        const val MAX_NAME_LENGTH = 24
        private val CONNECT_TIMEOUT = 15.seconds

        /** What a guest has to be sent: the bare code on the default server, else `CODE@host`. */
        fun makeInvite(
            server: String,
            code: String,
        ): String = if (server.isEmpty()) code else code + "@" + server.removePrefix("wss://").removeSuffix("/ws")

        /** (server, code) from an invite; server is empty for a bare code. Desktop's parser. */
        fun parseInvite(raw: String): Pair<String, String>? {
            val s = raw.trim()
            if (s.isEmpty()) return null
            val at = s.lastIndexOf('@')
            if (at < 0) return "" to s.uppercase()
            var server = s.substring(at + 1)
            if (!server.startsWith("wss://") && !server.startsWith("ws://")) server = "wss://$server"
            if ('/' !in server.substringAfter("://")) server += "/ws"
            return server to s.substring(0, at).uppercase()
        }
    }
}
