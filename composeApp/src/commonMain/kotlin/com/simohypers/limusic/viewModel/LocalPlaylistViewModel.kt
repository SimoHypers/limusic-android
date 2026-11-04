package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.common.LOCAL_PLAYLIST_ID
import com.simohypers.limusic.domain.data.entities.DownloadState
import com.simohypers.limusic.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.simohypers.limusic.domain.data.entities.DownloadState.STATE_NOT_DOWNLOADED
import com.simohypers.limusic.domain.data.model.download.CollectionDownload
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PairSongLocalPlaylist
import com.simohypers.limusic.domain.data.entities.SetVideoIdEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.collectionDownload
import com.simohypers.limusic.domain.utils.collectLatestResource
import com.simohypers.limusic.domain.utils.collectResource
import com.simohypers.limusic.domain.utils.toArrayListTrack
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.logger.LogLevel
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.koin.core.component.inject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.add_to_queue
import limusic.composeapp.generated.resources.added_to_playlist
import limusic.composeapp.generated.resources.added_to_queue
import limusic.composeapp.generated.resources.added_to_youtube_playlist
import limusic.composeapp.generated.resources.can_t_add_to_youtube_playlist
import limusic.composeapp.generated.resources.can_t_delete_from_youtube_playlist
import limusic.composeapp.generated.resources.delete
import limusic.composeapp.generated.resources.delete_song_from_playlist
import limusic.composeapp.generated.resources.removed_from_this_playlist
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.playlist
import limusic.composeapp.generated.resources.playlist_is_empty
import limusic.composeapp.generated.resources.downloading
import limusic.composeapp.generated.resources.removed_download
import limusic.composeapp.generated.resources.removed_from_YouTube_playlist
import limusic.composeapp.generated.resources.suggest
import limusic.composeapp.generated.resources.updated
import limusic.composeapp.generated.resources.updated_to_youtube_playlist
import limusic.composeapp.generated.resources.updating
import limusic.composeapp.generated.resources.moving_to_youtube_music
import limusic.composeapp.generated.resources.moved_to_youtube_music
import limusic.composeapp.generated.resources.youtube_write_failed

class LocalPlaylistViewModel(
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject<DownloadHandler>()

    var loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _uiState: MutableStateFlow<LocalPlaylistState> = MutableStateFlow(LocalPlaylistState.initial())
    val uiState: StateFlow<LocalPlaylistState> get() = _uiState

    // Downloaded = asked for; how many of its songs have landed is the progress.
    @OptIn(ExperimentalCoroutinesApi::class)
    val download =
        _uiState
            .map { it.id to (it.downloadState == STATE_DOWNLOADED) }
            .distinctUntilChanged()
            .flatMapLatest { (id, kept) ->
                if (kept) {
                    localPlaylistRepository.listTrackFlow(id).flatMapLatest { ids -> songRepository.collectionDownload(true, ids) }
                } else {
                    flowOf(CollectionDownload())
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), CollectionDownload())

    // Each phone playlist keeps its own sort; YouTube playlists use playlist_sort_<id>.
    private fun sortKey(id: Long) = "playlist_sort_local_$id"

    fun setSort(sort: ListSort) {
        val id = uiState.value.id
        if (id == 0L || sort == uiState.value.sort) return
        _uiState.update { it.copy(sort = sort) }
        viewModelScope.launch { dataStoreManager.putString(sortKey(id), sort.stored()) }
    }

    /**
     * The whole playlist in the picked sort, read again on every add, removal or move. Null until
     * the first read lands. Only the page collects it, so a Home shortcut's menu reads nothing.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tracks: StateFlow<List<Pair<SongEntity, PairSongLocalPlaylist>>?> =
        combine(
            _uiState
                .map { it.id }
                .distinctUntilChanged()
                .flatMapLatest { id -> if (id == 0L) flowOf(null) else localPlaylistRepository.playlistTracks(id) },
            _uiState.map { it.sort }.distinctUntilChanged(),
        ) { rows, sort -> rows?.let { sortPhonePlaylist(it, sort) } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var newUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            // A song added to a downloaded playlist downloads too.
            val listTrackStringJob =
                launch {
                    uiState
                        .map { it.id to (it.downloadState == STATE_DOWNLOADED) }
                        .distinctUntilChanged()
                        .collectLatest { (id, kept) ->
                            if (!kept) return@collectLatest
                            localPlaylistRepository
                                .listTrackFlow(id)
                                .distinctUntilChanged()
                                .drop(1)
                                .collectLatest { downloadUtils.downloadTracks(localPlaylistRepository.getFullPlaylistTracks(id)) }
                        }
                }
            val resetSuggestions =
                launch {
                    uiState
                        .map {
                            it.id
                        }.distinctUntilChanged()
                        .collectLatest {
                            _uiState.update {
                                it.copy(
                                    suggestions = null,
                                )
                            }
                        }
                }
            listTrackStringJob.join()
            resetSuggestions.join()
        }
    }

    private val _searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val searchQuery: StateFlow<String> get() = _searchQuery

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Results for the search box, a database query matching title or artist. Below two characters
     * it emits nothing: a single letter matches most of a playlist.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Pair<SongEntity, PairSongLocalPlaylist>>> =
        _searchQuery
            .debounce(250)
            .distinctUntilChanged()
            .flatMapLatest { raw ->
                val query = raw.trim()
                if (query.length < 2) {
                    flowOf(emptyList())
                } else {
                    localPlaylistRepository.searchTracks(uiState.value.id, query)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun getSuggestions(playlistId: Long) {
        loading.value = true
        viewModelScope.launch {
            localPlaylistRepository.getSuggestionsTrackForPlaylist(playlistId).collectLatestResource(
                onSuccess = { res ->
                    val reloadParams = res?.first
                    val songs = res?.second
                    if (reloadParams != null && songs != null) {
                        _uiState.update {
                            it.copy(
                                suggestions =
                                    LocalPlaylistState.SuggestionSongs(
                                        reloadParams = reloadParams,
                                        songs = songs,
                                    ),
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                suggestions = null,
                            )
                        }
                    }
                    loading.value = false
                },
                onError = { e ->
                    makeToast(e)
                    loading.value = false
                    _uiState.update {
                        it.copy(
                            suggestions = null,
                        )
                    }
                },
            )
        }
    }

    fun reloadSuggestion() {
        loading.value = true
        viewModelScope.launch {
            val param = uiState.value.suggestions?.reloadParams
            if (param != null) {
                localPlaylistRepository.reloadSuggestionPlaylist(param).collectLatestResource(
                    onSuccess = { res ->
                        val reloadParams = res?.first
                        val songs = res?.second
                        if (reloadParams != null && songs != null) {
                            _uiState.update {
                                it.copy(
                                    suggestions =
                                        LocalPlaylistState.SuggestionSongs(
                                            reloadParams = reloadParams,
                                            songs = songs,
                                        ),
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    suggestions = null,
                                )
                            }
                        }
                        loading.value = false
                    },
                    onError = {
                        _uiState.update { it.copy(suggestions = null) }
                        makeToast(getString(Res.string.error))
                        loading.value = false
                    },
                )
            }
        }
    }

    fun updatePlaylistDownloadState(
        id: Long,
        state: Int,
    ) {
        viewModelScope.launch {
            localPlaylistRepository
                .updateDownloadState(
                    id,
                    state,
                    getString(Res.string.updated),
                ).collectLatestResource(
                    onSuccess = { mess ->
                        Logger.d(tag, "updatePlaylistDownloadState: $mess")
                        _uiState.update {
                            it.copy(
                                downloadState = state,
                            )
                        }
                    },
                )
        }
    }

    fun updatePlaylistTitle(
        title: String,
        id: Long,
    ) {
        viewModelScope.launch {
            showLoadingDialog(message = getString(Res.string.updating))
            localPlaylistRepository
                .updateTitleLocalPlaylist(
                    id,
                    title,
                    getString(Res.string.updated),
                    getString(Res.string.updated_to_youtube_playlist),
                    getString(Res.string.error),
                ).collectResource(
                    onSuccess = {
                        makeToast(it)
                        updatePlaylistState(id)
                        hideLoadingDialog()
                    },
                    onError = {
                        makeToast(it)
                        hideLoadingDialog()
                    },
                )
        }
    }

    fun deletePlaylist(id: Long) {
        showLoadingDialog(message = getString(Res.string.delete))
        viewModelScope.launch {
            _uiState.value = LocalPlaylistState.initial()
            localPlaylistRepository
                .deleteLocalPlaylist(id, getString(Res.string.delete))
                .collectLatestResource(
                    onSuccess = {
                        makeToast(it)
                        hideLoadingDialog()
                    },
                    onError = {
                        makeToast(it)
                        hideLoadingDialog()
                    },
                )
        }
    }

    fun updatePlaylistThumbnail(
        uri: String,
        id: Long,
    ) {
        showLoadingDialog(message = getString(Res.string.updating))
        viewModelScope.launch {
            localPlaylistRepository
                .updateThumbnailLocalPlaylist(id, uri, getString(Res.string.updated))
                .collectResource(
                    onSuccess = {
                        makeToast(it)
                        updatePlaylistState(id)
                        hideLoadingDialog()
                    },
                    onError = {
                        makeToast(it)
                        hideLoadingDialog()
                    },
                )
        }
    }

    fun deleteItem(
        id: Long,
        song: SongEntity,
    ) {
        viewModelScope.launch {
            localPlaylistRepository
                .removeTrackFromLocalPlaylist(
                    id,
                    song,
                    getString(Res.string.delete_song_from_playlist),
                    getString(Res.string.removed_from_YouTube_playlist),
                    getString(Res.string.can_t_delete_from_youtube_playlist),
                ).collectLatestResource(
                    onSuccess = {
                        makeToast(it)
                        updatePlaylistState(id)
                    },
                    onError = {
                        makeToast(it)
                    },
                )
        }
    }

    private var _listSetVideoId: MutableStateFlow<List<SetVideoIdEntity>?> =
        MutableStateFlow(null)
    val listSetVideoId: StateFlow<List<SetVideoIdEntity>?> = _listSetVideoId

    fun getSetVideoId(youtubePlaylistId: String) {
        viewModelScope.launch {
            localPlaylistRepository.getYouTubeSetVideoId(youtubePlaylistId).collect {
                _listSetVideoId.value = it
            }
        }
    }

    /**
     * Sync became a move, as desktop keeps no phone copy of a YouTube playlist. [onMoved] gets
     * the new playlist's id; the local one is gone by then.
     */
    fun moveToYouTube(
        id: Long,
        onMoved: (String) -> Unit,
    ) {
        showLoadingDialog(message = getString(Res.string.moving_to_youtube_music))
        viewModelScope.launch {
            localPlaylistRepository
                .moveLocalPlaylistToYouTube(id)
                .onSuccess {
                    makeToast(getString(Res.string.moved_to_youtube_music))
                    onMoved(it)
                }.onFailure { makeToast(getString(Res.string.youtube_write_failed)) }
            hideLoadingDialog()
        }
    }

    fun removeListSuggestion() {
        _uiState.update { it.copy(suggestions = null) }
    }

    fun addSuggestTrackToListTrack(track: Track) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    suggestions =
                        state.suggestions?.copy(
                            songs = state.suggestions.songs.filter { it.videoId != track.videoId },
                        ),
                )
            }
            _uiState.value.id.let { id ->
                localPlaylistRepository
                    .addTrackToLocalPlaylist(
                        id,
                        track.toSongEntity(),
                        getString(Res.string.added_to_playlist),
                        getString(Res.string.added_to_youtube_playlist),
                        getString(Res.string.can_t_add_to_youtube_playlist),
                    ).collectLatestResource(
                        onSuccess = { makeToast(it) },
                        onError = {
                            makeToast(it)
                        },
                    )
            }
        }
    }

    fun onUIEvent(ev: LocalPlaylistUIEvent) {
        when (ev) {
            is LocalPlaylistUIEvent.ItemClick -> {
                val list = tracks.value ?: return
                val index = list.indexOfFirst { it.first.videoId == ev.videoId }
                if (index >= 0) play(list.map { it.first.toTrack() }, index)
            }

            is LocalPlaylistUIEvent.SuggestionsItemClick -> {
                val suggestionsList = uiState.value.suggestions?.songs ?: return
                val clickedSong = suggestionsList.find { it.videoId == ev.videoId } ?: return

                setQueueData(
                    QueueData.Data(
                        listTracks = suggestionsList.toCollection(ArrayList()),
                        firstPlayedTrack = clickedSong,
                        playlistId = "RDAMVM${clickedSong.videoId}",
                        playlistName = "${
                            getString(
                                Res.string.playlist,
                            )
                        } \"${uiState.value.title}\" ${
                            getString(Res.string.suggest)
                        }",
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }

            is LocalPlaylistUIEvent.PlayClick -> {
                val list = tracks.value
                if (list != null) {
                    if (list.isNotEmpty()) play(list.map { it.first.toTrack() }, 0)
                } else {
                    // No page open (the ⋮ menu of a Home shortcut): every track, in stored order.
                    withFullTracks { play(it, 0) }
                }
            }

            is LocalPlaylistUIEvent.ShuffleClick -> {
                withFullTracks { tracks ->
                    setQueueData(
                        QueueData.Data(
                            listTracks = ArrayList(tracks),
                            playlistId = LOCAL_PLAYLIST_ID + uiState.value.id,
                            playlistName = uiState.value.title,
                            playlistType = PlaylistType.LOCAL_PLAYLIST,
                            shuffle = true,
                        ),
                    )
                    loadMediaItem(tracks.first(), Config.PLAYLIST_CLICK, null)
                }
            }
        }
    }

    /** The whole list from [index], in the order on screen. */
    private fun play(
        tracks: List<Track>,
        index: Int,
    ) {
        setQueueData(
            QueueData.Data(
                listTracks = ArrayList(tracks),
                firstPlayedTrack = tracks[index],
                playlistId = LOCAL_PLAYLIST_ID + uiState.value.id,
                playlistName = uiState.value.title,
                playlistType = PlaylistType.LOCAL_PLAYLIST,
                continuation = null,
            ),
        )
        loadMediaItem(tracks[index], Config.PLAYLIST_CLICK, index)
    }

    /** [refresh]: a newly opened playlist, which comes up in its own saved sort. */
    fun updatePlaylistState(
        id: Long,
        refresh: Boolean = false,
    ) {
        viewModelScope.launch {
            val saved = if (refresh) ListSort.parse(dataStoreManager.getString(sortKey(id)).first()) else null
            localPlaylistRepository.getLocalPlaylist(id).collectLatestResource(
                onSuccess = { pl ->
                    if (pl != null) {
                        _uiState.update {
                            it.copy(
                                id = pl.id,
                                title = pl.title,
                                thumbnail = pl.thumbnail,
                                inLibrary = pl.inLibrary,
                                downloadState = pl.downloadState,
                                syncState = pl.syncState,
                                ytPlaylistId = pl.youtubePlaylistId,
                                trackCount = pl.tracks?.size ?: 0,
                                sort = saved ?: it.sort,
                            )
                        }
                    }
                },
            )
        }
    }

    fun downloadFullPlaylist() {
        val id = uiState.value.id
        viewModelScope.launch {
            val tracks = localPlaylistRepository.getFullPlaylistTracks(id = id)
            if (tracks.isEmpty()) {
                makeToast(getString(Res.string.playlist_is_empty))
                return@launch
            }
            makeToast(getString(Res.string.downloading))
            updatePlaylistDownloadState(id, STATE_DOWNLOADED)
            downloadUtils.downloadTracks(tracks)
        }
    }

    /** Tapped again on a kept playlist: its songs go, unless another download keeps them. */
    fun removeDownload() {
        val id = uiState.value.id
        viewModelScope.launch {
            localPlaylistRepository.updateLocalPlaylistDownloadState(STATE_NOT_DOWNLOADED, id)
            _uiState.update { it.copy(downloadState = STATE_NOT_DOWNLOADED) }
            downloadUtils.releaseDownloads(localPlaylistRepository.getFullPlaylistTracks(id).map { it.videoId })
            makeToast(getString(Res.string.removed_download))
        }
    }

    /** Retries the songs of a kept playlist that are not on the phone. */
    fun downloadMissing() {
        val id = uiState.value.id
        viewModelScope.launch { downloadUtils.downloadTracks(localPlaylistRepository.getFullPlaylistTracks(id)) }
    }

    /**
     * Every track of the playlist, not just the loaded page, for the menu's whole-list actions. An
     * empty playlist toasts instead of calling back; [onDone] runs either way, after [callback].
     */
    fun withFullTracks(
        onDone: () -> Unit = {},
        callback: (List<Track>) -> Unit,
    ) {
        viewModelScope.launch {
            val tracks = localPlaylistRepository.getFullPlaylistTracks(id = uiState.value.id).toArrayListTrack()
            if (tracks.isEmpty()) makeToast(getString(Res.string.playlist_is_empty)) else callback(tracks)
            onDone()
        }
    }

    /** The selection bar's Remove: [deleteItem] for each, with one toast and one refresh. */
    fun deleteItems(
        tracks: List<Track>,
        onDone: () -> Unit,
    ) {
        val id = uiState.value.id
        viewModelScope.launch {
            var failed = false
            tracks.forEach { track ->
                val song = songRepository.getSongById(track.videoId).lastOrNull() ?: track.toSongEntity()
                localPlaylistRepository
                    .removeTrackFromLocalPlaylist(
                        id,
                        song,
                        getString(Res.string.delete_song_from_playlist),
                        getString(Res.string.removed_from_YouTube_playlist),
                        getString(Res.string.can_t_delete_from_youtube_playlist),
                    ).collectLatestResource(
                        onSuccess = {},
                        // Gone from the phone; only the synced YouTube copy refused it.
                        onError = { failed = true },
                    )
            }
            makeToast(getString(if (failed) Res.string.can_t_delete_from_youtube_playlist else Res.string.removed_from_this_playlist))
            updatePlaylistState(id)
            onDone()
        }
    }

    fun addAllToQueue() {
        viewModelScope.launch {
            showLoadingDialog(getString(Res.string.add_to_queue))
            val fullTracks = localPlaylistRepository.getFullPlaylistTracks(id = uiState.value.id)
            if (fullTracks.isNotEmpty()) {
                mediaPlayerHandler.addToQueue(fullTracks.toArrayListTrack(), from = uiState.value.title)
                makeToast(getString(Res.string.added_to_queue))
                hideLoadingDialog()
            } else {
                makeToast(getString(Res.string.playlist_is_empty))
                hideLoadingDialog()
            }
        }
    }

    suspend fun changeLocalPlaylistItemPosition(
        from: Int,
        to: Int,
    ) {
        val isSynced =
            uiState.value.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced
        if (isSynced) {
            // Synced playlist: show loading dialog to block user interaction during API call
            showLoadingDialog(message = getString(Res.string.updating))
            localPlaylistRepository
                .moveItemInSyncedPlaylist(
                    playlistId = uiState.value.id,
                    fromIndex = from,
                    toIndex = to,
                ).collectResource(
                    onLoading = {
                        log("changeLocalPlaylistItemPosition (synced): loading")
                    },
                    onSuccess = {
                        log("changeLocalPlaylistItemPosition (synced): success $it")
                        hideLoadingDialog()
                    },
                    onError = { message ->
                        log("changeLocalPlaylistItemPosition (synced): error $message", LogLevel.WARN)
                        makeToast(message ?: getString(Res.string.error))
                        hideLoadingDialog()
                    },
                )
        } else {
            // Unsynced playlist: local only, no loading dialog needed
            localPlaylistRepository
                .moveItemInLocalPlaylist(
                    playlistId = uiState.value.id,
                    fromIndex = from,
                    toIndex = to,
                ).collectResource(
                    onLoading = {
                        log("changeLocalPlaylistItemPosition (local): loading")
                    },
                    onSuccess = {
                        log("changeLocalPlaylistItemPosition (local): success $it")
                    },
                    onError = { message ->
                        log("changeLocalPlaylistItemPosition (local): error $message", LogLevel.WARN)
                        makeToast(message)
                    },
                )
        }
    }
}

sealed class LocalPlaylistUIEvent {
    data class ItemClick(
        val videoId: String,
    ) : LocalPlaylistUIEvent()

    data class SuggestionsItemClick(
        val videoId: String,
    ) : LocalPlaylistUIEvent()

    data object PlayClick : LocalPlaylistUIEvent()

    data object ShuffleClick : LocalPlaylistUIEvent()
}

data class LocalPlaylistState(
    val id: Long,
    val title: String,
    val thumbnail: String? = null,
    val inLibrary: LocalDateTime? = null,
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
    val syncState: Int = LocalPlaylistEntity.YouTubeSyncState.NotSynced,
    val ytPlaylistId: String? = null,
    val trackCount: Int = 0,
    val page: Int = 0,
    val isLoadedFull: Boolean = false,
    val loadState: PlaylistLoadState = PlaylistLoadState.Loading,
    val sort: ListSort = ListSort(),
    val suggestions: SuggestionSongs? = null,
) {
    sealed class SuggestionState {
        data object Loading : SuggestionState()

        data object Error : SuggestionState()

        data class Success(
            val suggestSongs: SuggestionSongs,
        ) : SuggestionState()
    }

    sealed class PlaylistLoadState {
        data object Loading : PlaylistLoadState()

        data object Error : PlaylistLoadState()

        data object Success : PlaylistLoadState()
    }

    data class SuggestionSongs(
        val reloadParams: String,
        val songs: List<Track>,
    )

    companion object {
        fun initial(): LocalPlaylistState =
            LocalPlaylistState(
                id = 0,
                title = "",
                thumbnail = null,
                inLibrary = null,
                downloadState = 0,
                syncState = 0,
                trackCount = 0,
            )
    }
}