@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.maxrave.domain.data.entities.DownloadState.STATE_NOT_DOWNLOADED
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.download.CollectionDownload
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.data.model.browse.playlist.Author
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.collectionDownload
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.isRadioPlaylistId
import com.maxrave.domain.utils.toListVideoId
import com.maxrave.domain.utils.toPlaylistEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Error
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Loading
import com.maxrave.simpmusic.viewModel.PlaylistUIState.Success
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.auto_created_by_youtube_music
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.playlist_deleted
import simpmusic.composeapp.generated.resources.playlist_saved
import simpmusic.composeapp.generated.resources.playlist_is_empty
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.removed_download
import simpmusic.composeapp.generated.resources.shuffle
import simpmusic.composeapp.generated.resources.view_count
import simpmusic.composeapp.generated.resources.youtube_write_failed

class PlaylistViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel() {
    val downloadUtils: DownloadHandler by inject<DownloadHandler>()
    private var _uiState = MutableStateFlow<PlaylistUIState>(Loading)
    val uiState: StateFlow<PlaylistUIState> = _uiState


    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private var _playlistEntity: MutableStateFlow<PlaylistEntity?> = MutableStateFlow(null)
    var playlistEntity: StateFlow<PlaylistEntity?> = _playlistEntity

    // Downloaded = asked for; how many of the songs stored with it have landed is the progress.
    @OptIn(ExperimentalCoroutinesApi::class)
    val download =
        _playlistEntity
            .map { (it?.downloadState == STATE_DOWNLOADED) to it?.tracks.orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { (kept, ids) -> songRepository.collectionDownload(kept, ids) }
            .stateIn(viewModelScope, WhileSubscribed(1000), CollectionDownload())
    val liked = _playlistEntity.map { it?.liked == true }.stateIn(viewModelScope, WhileSubscribed(1000), false)

    private var _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    private var _tracksListState = MutableStateFlow<ListState>(ListState.IDLE)
    val tracksListState: StateFlow<ListState> = _tracksListState

    private var playlistEntityJob: Job? = null

    private fun resetData() {
        _uiState.value = Loading
        _playlistEntity.value = null
    }

    fun getData(id: String) {
        resetData()
        viewModelScope.launch {
            // Check radio
            if (id.isRadioPlaylistId()) {
                playlistRepository
                    .getRadio(
                        id,
                        radioString = getString(Res.string.radio),
                        defaultDescription = getString(Res.string.auto_created_by_youtube_music),
                        viewString = getString(Res.string.view_count),
                    ).collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Radio data: $data")
                                // Tracks first: a menu waiting on Success reads them straight away.
                                _tracks.value = data.first.tracks
                                _continuation.value = data.second
                                if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                                _uiState.value =
                                    Success(
                                        data =
                                            PlaylistState(
                                                id = data.first.id,
                                                title = data.first.title,
                                                isRadio = true,
                                                author = data.first.author,
                                                thumbnail =
                                                    data.first.thumbnails
                                                        .lastOrNull()
                                                        ?.url,
                                                description = data.first.description,
                                                trackCount = data.first.trackCount,
                                                year = data.first.year,
                                            ),
                                    )
                                playlistRepository.insertRadioPlaylist(data.first.toPlaylistEntity())
                            }

                            else -> {
                                _uiState.value = Error(res.message ?: "Empty response")
                            }
                        }
                    }
            } else {
                // This is an online playlist
                playlistRepository
                    .getPlaylistData(id, getString(Res.string.view_count))
                    .collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Playlist data: $data")
                                log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                                // Tracks first: a menu waiting on Success reads them straight away.
                                _tracks.value = data.first.tracks
                                _continuation.value = data.second
                                if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                                _uiState.value =
                                    Success(
                                        data =
                                            PlaylistState(
                                                id = data.first.id,
                                                title = data.first.title,
                                                isRadio = false,
                                                author = data.first.author,
                                                thumbnail =
                                                    data.first.thumbnails
                                                        .lastOrNull()
                                                        ?.url,
                                                description = data.first.description,
                                                trackCount = data.first.trackCount,
                                                year = data.first.year,
                                                shuffleEndpoint = data.first.shuffleEndpoint,
                                                radioEndpoint = data.first.radioEndpoint,
                                                privacy = data.first.privacy,
                                                owned = data.first.owned,
                                                authorThumbnail = data.first.authorThumbnail,
                                                duration = data.first.duration,
                                            ),
                                    )
                                getPlaylistEntity(id = data.first.id, playlistBrowse = data.first)
                            }

                            else -> {
                                getPlaylistEntity(id)
                            }
                        }
                    }
            }
        }
    }

    fun getContinuationTrack(
        playlistId: String,
        continuation: String?,
    ) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _tracksListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                _tracksListState.value = ListState.PAGINATING
                songRepository
                    .getContinueTrack(
                        playlistId,
                        continuation,
                        fromPlaylist = true,
                    ).collectLatest { res ->
                        res.first?.forEach { track ->
                            songRepository
                                .insertSong(
                                    track
                                        .toSongEntity()
                                        .copy(
                                            inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                        ),
                                ).singleOrNull()
                                ?.let {
                                    log("Insert song: $it")
                                }
                        }
                        _tracks.update {
                            val newList = it.toMutableList()
                            newList.addAll(res.first ?: emptyList())
                            newList
                        }
                        if (res.second.isNullOrEmpty()) {
                            _continuation.value = null
                            _tracksListState.value = ListState.PAGINATION_EXHAUST
                        } else {
                            _continuation.value = res.second
                            _tracksListState.value = ListState.IDLE
                        }
                    }
            }
        }
    }

    private fun getPlaylistEntity(
        id: String,
        playlistBrowse: PlaylistBrowse? = null,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch {
                val playlistEntity = playlistRepository.getPlaylist(id).firstOrNull()
                if (playlistBrowse != null) {
                    if (playlistEntity == null) {
                        playlistRepository.insertAndReplacePlaylist(
                            playlistBrowse.toPlaylistEntity(),
                        )
                        delay(500)
                        playlistRepository.getPlaylist(id).collectLatest { playlist ->
                            _playlistEntity.value = playlist
                            playlistRepository.updatePlaylistInLibrary(
                                playlistId = id,
                                inLibrary = now(),
                            )
                        }
                    } else {
                        _playlistEntity.value = playlistEntity
                        playlistRepository.updatePlaylistInLibrary(
                            playlistId = id,
                            inLibrary = now(),
                        )
                    }
                    playlistBrowse.saved?.let { saved ->
                        playlistRepository.mirrorPlaylistSaved(id, saved)
                        playlistRepository.getPlaylist(id).firstOrNull()?.let { _playlistEntity.value = it }
                    }
                    playlistBrowse.tracks.forEach { tracks ->
                        songRepository
                            .insertSong(
                                tracks.toSongEntity().copy(
                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                ),
                            ).firstOrNull()
                            ?.let {
                                log("Insert song: $it")
                            }
                    }
                } else if (playlistEntity != null) {
                    _playlistEntity.value = playlistEntity
                    playlistRepository.updatePlaylistInLibrary(
                        playlistId = id,
                        inLibrary = now(),
                    )
                    // Before Success, so nothing waiting on it reads the track list half-built.
                    _tracksListState.value = ListState.LOADING
                    _uiState.value =
                        Success(
                            data =
                                PlaylistState(
                                    id = playlistEntity.id,
                                    title = playlistEntity.title,
                                    isRadio = false,
                                    author =
                                        Author(
                                            id = "",
                                            name = playlistEntity.author ?: "",
                                        ),
                                    thumbnail = playlistEntity.thumbnails,
                                    description = playlistEntity.description,
                                    trackCount = playlistEntity.trackCount,
                                    year = playlistEntity.year ?: now().year.toString(),
                                ),
                        )
                    playlistEntity.tracks?.let {
                        songRepository
                            .getSongsByListVideoId(it)
                            .singleOrNull()
                            ?.let { song ->
                                _tracks.value = song.map { it.toTrack() }
                            }
                    }
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                } else {
                    _uiState.value = Error("Empty response")
                }
            }
    }

    fun updatePlaylistLiked(
        liked: Boolean,
        id: String,
    ) {
        viewModelScope.launch {
            _playlistEntity.update { it?.copy(liked = liked) }
            if (!playlistRepository.updatePlaylistLiked(id, if (liked) 1 else 0)) {
                _playlistEntity.update { it?.copy(liked = !liked) }
                makeToast(getString(Res.string.youtube_write_failed))
                return@launch
            }
            getFullTracks { }
        }
    }

    fun onUIEvent(event: PlaylistUIEvent) {
        val data = uiState.value.data ?: return
        when (event) {
            is PlaylistUIEvent.ItemClick -> {
                val videoId = event.videoId
                val loadedList = tracks.value
                val clickedSong = loadedList.first { it.videoId == videoId }
                val index = loadedList.indexOf(clickedSong)
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = data.title,
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    index,
                )
            }

            PlaylistUIEvent.PlayAll -> {
                val loadedList = tracks.value
                if (loadedList.isEmpty()) {
                    makeToast(
                        getString(Res.string.playlist_is_empty),
                    )
                    return
                }
                val clickedSong = loadedList.first()
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = data.title,
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }

            PlaylistUIEvent.Shuffle -> {
                // The playlist's own tracks in random order, like desktop; the rest of a long one
                // is walked in the background and mixed in as it arrives.
                val loadedList = tracks.value
                if (loadedList.isEmpty()) {
                    makeToast(getString(Res.string.playlist_is_empty))
                    return
                }
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        playlistId = data.id,
                        playlistName = data.title,
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                        shuffle = true,
                    ),
                )
                loadMediaItem(loadedList.first(), Config.PLAYLIST_CLICK, null)
            }

            PlaylistUIEvent.StartRadio -> {
                // Without one from the page, the same radio Limusic desktop starts for a playlist.
                val radioEndpoint =
                    data.radioEndpoint ?: YouTubeWatchEndpoint(playlistId = "RDAMPL${data.id.removePrefix("VL")}")
                run {
                    viewModelScope.launch {
                        songRepository.getRadioFromEndpoint(radioEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Logger.d(tag, "Radio data: ${result.first.size}")
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = radioEndpoint.playlistId,
                                            playlistName = "${data.title} ${getString(Res.string.radio)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(
                                        res.message ?: getString(Res.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.Download -> {
                downloadFullPlaylist()
            }

            PlaylistUIEvent.Favorite -> {
                updatePlaylistLiked(!liked.value, data.id)
            }
        }
    }

    fun getFullTracks(callback: (List<Track>) -> Unit) {
        viewModelScope.launch {
            // A Home shortcut opens the menu before the playlist has loaded: wait for its first page.
            combine(uiState, tracksListState) { ui, list -> ui !is Loading && list != ListState.LOADING }.first { it }
            if (uiState.value.data == null) return@launch
            if (tracksListState.value == ListState.PAGINATION_EXHAUST) {
                _playlistEntity.value
                    ?.copy(
                        tracks = tracks.value.toListVideoId(),
                        trackCount = tracks.value.size,
                    )?.let {
                        playlistRepository.insertAndReplacePlaylist(it)
                    }
                callback(tracks.value)
            } else {
                val id = uiState.value.data?.id ?: return@launch
                tracksListState.collectLatest { state ->
                    if (state == ListState.PAGINATION_EXHAUST) {
                        _playlistEntity.value
                            ?.copy(
                                tracks = tracks.value.toListVideoId(),
                                trackCount = tracks.value.size,
                            )?.let {
                                playlistRepository.insertAndReplacePlaylist(it)
                            }
                        callback(tracks.value)
                    } else if (state != ListState.PAGINATING) {
                        getContinuationTrack(id, continuation.value)
                    }
                }
            }
        }
    }

    /** Runs [action] once the playlist has loaded; never, if it failed. */
    fun afterLoad(action: () -> Unit) {
        viewModelScope.launch {
            if (uiState.first { it !is Loading } is Success) action()
        }
    }

    fun downloadFullPlaylist() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            makeToast(getString(Res.string.downloading))
            // getFullTracks stores the full list on the playlist row first, which is what the
            // progress and the Downloads tab count against.
            getFullTracks { tracks ->
                viewModelScope.launch {
                    playlistRepository.updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                    _playlistEntity.update { it?.copy(downloadState = STATE_DOWNLOADED, tracks = tracks.toListVideoId()) }
                    downloadUtils.downloadTracks(tracks.map { it.toSongEntity() })
                }
            }
        }
    }

    /** Tapped again on a kept playlist: the songs it was holding go, unless another download keeps them. */
    fun removeDownload() {
        val entity = playlistEntity.value ?: return
        viewModelScope.launch {
            playlistRepository.updatePlaylistDownloadState(entity.id, STATE_NOT_DOWNLOADED)
            _playlistEntity.update { it?.copy(downloadState = STATE_NOT_DOWNLOADED) }
            downloadUtils.releaseDownloads(entity.tracks.orEmpty())
            makeToast(getString(Res.string.removed_download))
        }
    }

    /** Retries the songs of a kept playlist that are not on the phone. */
    fun downloadMissing() {
        val ids = playlistEntity.value?.tracks ?: return
        viewModelScope.launch {
            downloadUtils.downloadTracks(songRepository.getSongsByListVideoId(ids).first())
        }
    }

    /** Sends only what changed; null means untouched. [onResult] says whether it saved. */
    fun editPlaylist(
        title: String?,
        description: String?,
        privacy: String?,
        cover: ByteArray?,
        onResult: (Boolean) -> Unit,
    ) {
        val id = uiState.value.data?.id ?: return
        viewModelScope.launch {
            playlistRepository.editYourYouTubePlaylist(id, title, description, privacy, cover).collect {
                when (it) {
                    is Resource.Success -> {
                        getData(id)
                        makeToast(getString(Res.string.playlist_saved))
                        onResult(true)
                    }

                    is Resource.Error -> {
                        makeToast("${getString(Res.string.error)}: ${it.message}")
                        onResult(false)
                    }
                }
            }
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        val id = uiState.value.data?.id ?: return
        viewModelScope.launch {
            playlistRepository.deleteYourYouTubePlaylist(id).collect {
                when (it) {
                    is Resource.Success -> {
                        makeToast(getString(Res.string.playlist_deleted))
                        onDeleted()
                    }

                    is Resource.Error -> makeToast("${getString(Res.string.error)}: ${it.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playlistEntityJob?.cancel()
    }
}

sealed class PlaylistUIState(
    val data: PlaylistState? = null,
    val message: String? = null,
) {
    data object Loading : PlaylistUIState()

    class Success(
        data: PlaylistState,
    ) : PlaylistUIState(
            data = data,
        )

    class Error(
        message: String? = null,
    ) : PlaylistUIState(
            message = message,
        )
}

sealed class PlaylistUIEvent {
    data object PlayAll : PlaylistUIEvent()

    data object Shuffle : PlaylistUIEvent()

    data object StartRadio : PlaylistUIEvent()

    data class ItemClick(
        val videoId: String,
    ) : PlaylistUIEvent()

    data object Favorite : PlaylistUIEvent()

    data object Download : PlaylistUIEvent()
}

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    PAGINATION_EXHAUST,
}