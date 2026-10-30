@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.simohypers.limusic.domain.data.entities.DownloadState.STATE_NOT_DOWNLOADED
import com.simohypers.limusic.domain.data.entities.PlaylistEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.download.CollectionDownload
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import com.simohypers.limusic.domain.data.model.browse.playlist.Author
import com.simohypers.limusic.domain.data.model.browse.playlist.PlaylistBrowse
import com.simohypers.limusic.domain.data.model.browse.playlist.PlaylistState
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.PlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.collectionDownload
import com.simohypers.limusic.domain.utils.PlaylistSort
import com.simohypers.limusic.domain.utils.PlaylistSortMenu
import com.simohypers.limusic.domain.utils.Resource
import com.simohypers.limusic.domain.utils.isRadioPlaylistId
import com.simohypers.limusic.domain.utils.toListVideoId
import com.simohypers.limusic.domain.utils.toPlaylistEntity
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.viewModel.PlaylistUIState.Error
import com.simohypers.limusic.viewModel.PlaylistUIState.Loading
import com.simohypers.limusic.viewModel.PlaylistUIState.Success
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.auto_created_by_youtube_music
import limusic.composeapp.generated.resources.downloading
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.playlist
import limusic.composeapp.generated.resources.playlist_deleted
import limusic.composeapp.generated.resources.playlist_saved
import limusic.composeapp.generated.resources.playlist_is_empty
import limusic.composeapp.generated.resources.radio
import limusic.composeapp.generated.resources.removed_download
import limusic.composeapp.generated.resources.removed_from_liked_music
import limusic.composeapp.generated.resources.removed_from_this_playlist
import limusic.composeapp.generated.resources.shuffle
import limusic.composeapp.generated.resources.sort_failed
import limusic.composeapp.generated.resources.sort_not_saved
import limusic.composeapp.generated.resources.view_count
import limusic.composeapp.generated.resources.youtube_write_failed

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
    private var continuationJob: Job? = null
    private var sortJob: Job? = null

    // --- sorting (PlaylistSorting.kt, desktop's playlist page) ---
    private val dataStoreManager: DataStoreManager by inject<DataStoreManager>()
    private var playlistId = ""

    private val _sortState = MutableStateFlow(PlaylistSortState())
    val sortState: StateFlow<PlaylistSortState> = _sortState

    /** The rows in the order on screen. Search filters these. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sortedTracks: StateFlow<List<Track>> =
        combine(_tracks, _sortState.map { it.copy(resorting = false) }.distinctUntilChanged()) { t, s -> t to s }
            .mapLatest { (t, s) -> ordered(t, s) }
            .stateIn(viewModelScope, Eagerly, emptyList())

    private suspend fun ordered(
        items: List<Track>,
        s: PlaylistSortState = _sortState.value,
    ): List<Track> {
        // The rows came back from YouTube in order. The one order it has no params for is a
        // reversed manual one, so that reverse stays here.
        if (s.serverSorted) return if (s.sort == PlaylistSort.DEFAULT && s.desc) items.reversed() else items
        val plays =
            if (s.sort == PlaylistSort.PLAYS) {
                // Chunked under SQLite's 999-variable limit on older Androids.
                items.map { it.videoId }.chunked(900).flatMap { songRepository.getSongsByListVideoId(it).first() }
                    .associate { it.videoId to it.totalPlayTime }
            } else {
                emptyMap()
            }
        // Liked Music is the one playlist YouTube hands back newest addition first.
        return sortTracks(items, s.sort, playlistId == "LM", s.desc, plays)
    }

    // Only what YouTube will not keep goes here: Most played, a reversed Title/Artist/Album, and any
    // sort on a list it stores none for (someone else's playlist). The rest is read back off the page.
    private fun sortKey(id: String) = "playlist_sort_$id"

    private suspend fun readSort(id: String): PlaylistSortState? {
        val (name, dir) = dataStoreManager.getString(sortKey(id)).first()?.split(":")?.let { it[0] to it.getOrNull(1) } ?: return null
        return PlaylistSort.entries.firstOrNull { it.name == name }?.let { PlaylistSortState(sort = it, desc = dir == "desc") }
    }

    private suspend fun rememberSort(
        id: String,
        s: PlaylistSortState,
    ) {
        val keptByYouTube = s.menu != null && (s.menu.editable || id == "LM") && storedExactly(s.sort, s.desc)
        dataStoreManager.putString(sortKey(id), if (keptByYouTube) "" else s.sort.name + if (s.desc) ":desc" else "")
    }

    /**
     * A sort change is a new request: YouTube owns the order, so the rows come back from it. On a
     * playlist you own the choice is stored first, so it outlives the visit and shows up in YouTube
     * Music. Most played and a reversed manual order are still sorted here, over every page.
     */
    fun setSort(
        sort: PlaylistSort,
        desc: Boolean,
    ) {
        val id = playlistId
        val s = _sortState.value.copy(sort = sort, desc = desc)
        if (s == _sortState.value || id.isEmpty()) return
        _sortState.value = s
        sortJob?.cancel()
        sortJob =
            viewModelScope.launch {
                rememberSort(id, s)
                val menu = s.menu
                if (menu != null) {
                    persistedSort(sort, desc)?.takeIf { menu.editable }?.let { store ->
                        // The order still applies here; only the carry-over to other clients is lost.
                        if (!playlistRepository.setPlaylistSort(id, store)) makeToast(getString(Res.string.sort_not_saved))
                    }
                    fetchSorted(id, s)
                }
                // Inside this job, so the next pick stops a walk this order no longer needs.
                if (s.localSorting) walkAll()
            }
    }

    /** Page one of the list in the new order. Its token pages on in that same order. */
    private suspend fun fetchSorted(
        id: String,
        s: PlaylistSortState,
    ) {
        continuationJob?.cancel()
        _sortState.update { it.copy(resorting = true) }
        val res = playlistRepository.getPlaylistData(id, getString(Res.string.view_count), fetchSort(s.sort), s.desc).firstOrNull()
        val data = res?.data
        if (res is Resource.Success && data != null) {
            data.first.tracks.forEach {
                songRepository.insertSong(it.toSongEntity().copy(inLibrary = Config.REMOVED_SONG_DATE_TIME)).firstOrNull()
            }
            _tracks.value = data.first.tracks
            _continuation.value = data.second
            _tracksListState.value = if (data.second.isNullOrEmpty()) ListState.PAGINATION_EXHAUST else ListState.IDLE
        } else {
            makeToast(getString(Res.string.sort_failed))
        }
        _sortState.update { it.copy(resorting = false) }
    }

    /** Hands [play] the rows in the order on screen, with the token for the pages behind them. */
    private fun withQueueTracks(play: (List<Track>, String?) -> Unit) {
        // Ordered here, so the queue needs every page first, and it is a snapshot with no token.
        if (_sortState.value.localSorting) getFullTracks { play(it, null) } else play(tracks.value, continuation.value)
    }

    private fun resetData() {
        _uiState.value = Loading
        _playlistEntity.value = null
        _removed.value = 0
    }

    fun getData(id: String) {
        resetData()
        playlistId = id
        sortJob?.cancel()
        _sortState.value = PlaylistSortState()
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
                // This is an online playlist. Nothing saved means asking for no order at all: what
                // comes back is the order the account has it in, the one YouTube Music would show.
                val saved = readSort(id)
                _sortState.value = saved ?: PlaylistSortState()
                playlistRepository
                    .getPlaylistData(id, getString(Res.string.view_count), saved?.let { fetchSort(it.sort) }, saved?.desc == true)
                    .collect { res ->
                        val data = res.data
                        when (res) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Playlist data: $data")
                                log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                                val menu = data.first.sortMenu
                                _sortState.update { it.copy(menu = menu, sort = if (saved == null) menu?.selected ?: PlaylistSort.DEFAULT else it.sort) }
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
                                if (_sortState.value.localSorting) sortJob = viewModelScope.launch { walkAll() }
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
        // Kept so a re-sort can drop a page of the old order that is still on its way.
        continuationJob = viewModelScope.launch {
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
            is PlaylistUIEvent.ItemClick -> withQueueTracks { loadedList, token ->
                val videoId = event.videoId
                val clickedSong = loadedList.firstOrNull { it.videoId == videoId } ?: return@withQueueTracks
                val index = loadedList.indexOf(clickedSong)
                setQueueData(
                    QueueData.Data(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = data.title,
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = token,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    index,
                )
            }

            PlaylistUIEvent.PlayAll -> {
                if (tracks.value.isEmpty()) {
                    makeToast(
                        getString(Res.string.playlist_is_empty),
                    )
                    return
                }
                withQueueTracks { loadedList, token ->
                    val clickedSong = loadedList.firstOrNull() ?: return@withQueueTracks
                    setQueueData(
                        QueueData.Data(
                            listTracks = loadedList.toCollection(arrayListOf<Track>()),
                            firstPlayedTrack = clickedSong,
                            playlistId = data.id,
                            playlistName = data.title,
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = token,
                        ),
                    )
                    loadMediaItem(
                        clickedSong,
                        Config.PLAYLIST_CLICK,
                        0,
                    )
                }
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

    /** Walks in every page, then calls [callback] once with the whole list, in the order on screen. */
    fun getFullTracks(callback: (List<Track>) -> Unit) {
        viewModelScope.launch {
            if (walkAll()) callback(ordered(tracks.value))
        }
    }

    /** Pulls in every page of the list; false if it never loaded. */
    private suspend fun walkAll(): Boolean {
        // A Home shortcut opens the menu before the playlist has loaded: wait for its first page.
        combine(uiState, tracksListState) { ui, list -> ui !is Loading && list != ListState.LOADING }.first { it }
        val id = uiState.value.data?.id ?: return false
        // Ends at the last page: a callback that queues must not fire again when a re-sort walks
        // the list a second time.
        tracksListState.first { state ->
            if (state != ListState.PAGINATION_EXHAUST && state != ListState.PAGINATING) {
                getContinuationTrack(id, continuation.value)
            }
            state == ListState.PAGINATION_EXHAUST
        }
        _playlistEntity.value
            ?.copy(
                tracks = tracks.value.toListVideoId(),
                trackCount = tracks.value.size,
            )?.let {
                playlistRepository.insertAndReplacePlaylist(it)
            }
        return true
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

    private val _removed = MutableStateFlow(0)

    /** Rows taken out with [removeTracks] this visit, off the header's song count. */
    val removed: StateFlow<Int> = _removed

    /**
     * The selection bar's Remove, like desktop: one request for a playlist you own. Liked Music has
     * only per-song unlike, so it goes song by song and a row leaves as YouTube takes it. Rows go
     * only once YouTube has them, so a page that lands meanwhile is never put back over.
     */
    fun removeTracks(
        tracks: List<Track>,
        onDone: () -> Unit,
    ) {
        val id = playlistId
        viewModelScope.launch {
            if (id.removePrefix("VL") == "LM") {
                var failed = 0
                tracks.forEach { track ->
                    if (songRepository.updateLikeStatus(track.videoId, 0)) {
                        _tracks.update { rows -> rows.filterNot { it.videoId == track.videoId } }
                        _removed.update { it + 1 }
                    } else {
                        failed++
                    }
                }
                makeToast(getString(if (failed > 0) Res.string.youtube_write_failed else Res.string.removed_from_liked_music))
            } else {
                val targets = tracks.mapNotNull { track -> track.setVideoId?.let { track.videoId to it } }
                if (targets.isEmpty()) return@launch
                if (playlistRepository.removePlaylistItems(id, targets)) {
                    val gone = targets.mapTo(HashSet()) { it.second }
                    _tracks.update { rows -> rows.filterNot { it.setVideoId in gone } }
                    _removed.update { it + targets.size }
                    makeToast(getString(Res.string.removed_from_this_playlist))
                } else {
                    makeToast(getString(Res.string.youtube_write_failed))
                }
            }
            onDone()
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

data class PlaylistSortState(
    val sort: PlaylistSort = PlaylistSort.DEFAULT,
    val desc: Boolean = false,
    /** YouTube's sort menu for this list; null on lists it will not reorder, or offline. */
    val menu: PlaylistSortMenu? = null,
    /** A YouTube sort is a round trip: the rows on screen are the old order until it lands. */
    val resorting: Boolean = false,
) {
    /** YouTube does the ordering: all of it but Most played, which is this phone's own counts. */
    val serverSorted get() = menu != null && sort != PlaylistSort.PLAYS

    /** Rows are ordered here, which is only honest once every page is in (desktop's `sorting`). */
    val localSorting get() = if (serverSorted) sort == PlaylistSort.DEFAULT && desc else sort != PlaylistSort.DEFAULT || desc
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