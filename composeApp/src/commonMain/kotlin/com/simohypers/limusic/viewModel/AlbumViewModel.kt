package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.entities.DownloadState
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.browse.artist.ResultAlbum
import com.simohypers.limusic.domain.data.model.download.CollectionDownload
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.repository.AlbumRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.repository.collectionDownload
import com.simohypers.limusic.domain.utils.Resource
import com.simohypers.limusic.domain.utils.toAlbumEntity
import com.simohypers.limusic.domain.utils.toArrayListTrack
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.logger.LogLevel
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import limusic.composeapp.generated.resources.loading_album
import org.koin.core.component.inject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.album
import limusic.composeapp.generated.resources.downloading
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.playlist_is_empty
import limusic.composeapp.generated.resources.radio
import limusic.composeapp.generated.resources.radio_not_available
import limusic.composeapp.generated.resources.removed_download
import limusic.composeapp.generated.resources.youtube_write_failed

class AlbumViewModel(
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject<DownloadHandler>()
    private val _uiState: MutableStateFlow<AlbumUIState> = MutableStateFlow(AlbumUIState.initial())
    val uiState: StateFlow<AlbumUIState> = _uiState

    private var job: Job? = null

    // Downloaded = asked for; how many of its songs have landed is the progress.
    @OptIn(ExperimentalCoroutinesApi::class)
    val download =
        _uiState
            .map { (it.downloadState == DownloadState.STATE_DOWNLOADED) to it.listTrack.map { track -> track.videoId } }
            .distinctUntilChanged()
            .flatMapLatest { (kept, ids) -> songRepository.collectionDownload(kept, ids) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), CollectionDownload())

    fun updateBrowseId(browseId: String) {
        viewModelScope.launch {
            // Loading again, so a retry after an error reads as a new request until it lands.
            _uiState.update { it.copy(browseId = browseId, loadState = LocalPlaylistState.PlaylistLoadState.Loading) }
            albumRepository.getAlbumData(browseId).collectLatest { res ->
                when (res) {
                    is Resource.Success -> {
                        val data = res.data
                        if (data != null) {
                            _uiState.update {
                                it.copy(
                                    browseId = browseId,
                                    audioPlaylistId = data.audioPlaylistId,
                                    title = data.title,
                                    thumbnail = data.thumbnails?.lastOrNull()?.url,
                                    kind = data.type,
                                    artistThumbnail = data.artistThumbnail,
                                    artist =
                                        data.artists.firstOrNull() ?: Artist(
                                            id = null,
                                            name = "",
                                        ),
                                    year = data.year ?: now().year.toString(),
                                    trackCount = data.trackCount,
                                    description = data.description,
                                    length = data.duration ?: "",
                                    listTrack = data.tracks,
                                    otherVersion = data.otherVersion,
                                    loadState = LocalPlaylistState.PlaylistLoadState.Success,
                                )
                            }
                            val localAlbum = albumRepository.getAlbum(browseId).lastOrNull()
                            if (localAlbum != null) {
                                _uiState.update {
                                    it.copy(
                                        downloadState = localAlbum.downloadState,
                                        liked = localAlbum.liked,
                                    )
                                }
                                albumRepository.updateAlbumInLibrary(now(), browseId)
                            } else {
                                albumRepository.insertAlbum(data.toAlbumEntity(browseId)).singleOrNull().let {
                                    log("Insert Album $it")
                                    data.tracks.forEach { track ->
                                        songRepository
                                            .insertSong(
                                                track.toSongEntity().copy(
                                                    inLibrary = Config.REMOVED_SONG_DATE_TIME,
                                                ),
                                            ).singleOrNull()
                                            ?.let {
                                                log("Insert Song $it")
                                            }
                                    }
                                }
                            }
                            data.inLibrary?.let { albumRepository.mirrorAlbumSaved(browseId, it) }
                            getAlbumFlow(browseId)
                        } else {
                            makeToast(getString(Res.string.error) + ": Null data")
                            _uiState.update {
                                it.copy(
                                    loadState = LocalPlaylistState.PlaylistLoadState.Error,
                                )
                            }
                        }
                    }

                    is Resource.Error -> {
                        albumRepository.getAlbum(browseId).singleOrNull().let { albumEntity ->
                            if (albumEntity != null) {
                                _uiState.update {
                                    it.copy(
                                        browseId = browseId,
                                        title = albumEntity.title,
                                        thumbnail = albumEntity.thumbnails,
                                        kind = albumEntity.type,
                                        artist =
                                            Artist(
                                                id = albumEntity.artistId?.firstOrNull(),
                                                name = albumEntity.artistName?.firstOrNull() ?: "",
                                            ),
                                        year = albumEntity.year ?: now().year.toString(),
                                        trackCount = albumEntity.trackCount,
                                        description = albumEntity.description,
                                        length = albumEntity.duration ?: "",
                                        listTrack =
                                            (
                                                songRepository
                                                    .getSongsByListVideoId(albumEntity.tracks ?: emptyList())
                                                    .singleOrNull() ?: emptyList()
                                            ).toArrayListTrack(),
                                        loadState = LocalPlaylistState.PlaylistLoadState.Success,
                                    )
                                }
                            } else {
                                log("Error: ${res.message}", LogLevel.ERROR)
                                makeToast(getString(Res.string.error) + ": ${res.message}")
                                _uiState.update {
                                    it.copy(
                                        loadState = LocalPlaylistState.PlaylistLoadState.Error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // The heart follows the row through getAlbumFlow, refusal included.
    fun setAlbumLike() {
        viewModelScope.launch {
            if (!albumRepository.updateAlbumLiked(uiState.value.browseId, if (!uiState.value.liked) 1 else 0)) {
                makeToast(getString(Res.string.youtube_write_failed))
            }
        }
    }

    private fun getAlbumFlow(browseId: String) {
        job?.cancel()
        job =
            viewModelScope.launch {
                albumRepository.getAlbumAsFlow(browseId).collectLatest { album ->
                    if (album != null) {
                        _uiState.update {
                            it.copy(
                                downloadState = album.downloadState,
                                liked = album.liked,
                            )
                        }
                    }
                }
            }
    }

    fun playTrack(track: Track) {
        setQueueData(
            QueueData.Data(
                listTracks = uiState.value.listTrack.toCollection(ArrayList()),
                firstPlayedTrack = track,
                // The audio playlist, not the MPRE browseId: autoplay's radio keys off it.
                playlistId = uiState.value.audioPlaylistId.ifEmpty { null },
                playlistName = uiState.value.title,
                playlistType = PlaylistType.ALBUM,
                continuation = null,
            ),
        )
        val index = uiState.value.listTrack.indexOf(track)
        loadMediaItem(track, Config.ALBUM_CLICK, if (index == -1) 0 else index)
    }

    fun shuffle() {
        if (uiState.value.listTrack.isEmpty()) {
            makeToast(getString(Res.string.playlist_is_empty))
            return
        }
        val list = uiState.value.listTrack
        setQueueData(
            QueueData.Data(
                listTracks = list.toCollection(ArrayList()),
                playlistId = uiState.value.audioPlaylistId.ifEmpty { null },
                playlistName = uiState.value.title,
                playlistType = PlaylistType.ALBUM,
                shuffle = true,
            ),
        )
        loadMediaItem(list.first(), Config.ALBUM_CLICK, null)
    }

    /** Runs [action] with the album once it has loaded ([onDone] either way, after it). */
    fun afterLoad(
        onDone: () -> Unit = {},
        action: (AlbumUIState) -> Unit,
    ) {
        viewModelScope.launch {
            val state = busy(Res.string.loading_album) { uiState.first { it.loadState != LocalPlaylistState.PlaylistLoadState.Loading } }
            if (state.loadState == LocalPlaylistState.PlaylistLoadState.Success) action(state)
            onDone()
        }
    }

    fun startRadio() {
        val id = uiState.value.audioPlaylistId
        if (id.isEmpty()) {
            makeToast(getString(Res.string.radio_not_available))
            return
        }
        playFromEndpoint(
            YouTubeWatchEndpoint(playlistId = "RDAMPL$id"),
            "${uiState.value.title} ${getString(Res.string.radio)}",
        )
    }

    fun downloadFullAlbum() {
        val tracks = uiState.value.listTrack
        viewModelScope.launch {
            if (tracks.isEmpty()) {
                makeToast(getString(Res.string.playlist_is_empty))
                return@launch
            }
            makeToast(getString(Res.string.downloading))
            albumRepository.updateAlbumDownloadState(uiState.value.browseId, DownloadState.STATE_DOWNLOADED)
            downloadUtils.downloadTracks(tracks.map { it.toSongEntity() })
        }
    }

    /** Tapped again on a kept album: its songs go, unless another download keeps them. */
    fun removeDownload() {
        val state = uiState.value
        viewModelScope.launch {
            albumRepository.updateAlbumDownloadState(state.browseId, DownloadState.STATE_NOT_DOWNLOADED)
            downloadUtils.releaseDownloads(state.listTrack.map { it.videoId })
            makeToast(getString(Res.string.removed_download))
        }
    }

    /** Retries the songs of a kept album that are not on the phone. */
    fun downloadMissing() {
        val tracks = uiState.value.listTrack
        viewModelScope.launch { downloadUtils.downloadTracks(tracks.map { it.toSongEntity() }) }
    }
}

data class AlbumUIState(
    val browseId: String = "",
    /** The album's `OLAK5uy_…` playlist: its radio and its share link key off this, not [browseId]. */
    val audioPlaylistId: String = "",
    val title: String = "",
    val thumbnail: String? = null,
    /** "Album", "Single", "EP": the page's kicker. */
    val kind: String? = null,
    val artistThumbnail: String? = null,
    val artist: Artist =
        Artist(
            id = null,
            name = "",
        ),
    val year: String = now().year.toString(),
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
    val liked: Boolean = false,
    val trackCount: Int = 0,
    val description: String? = null,
    val length: String = "",
    val listTrack: List<Track> = emptyList(),
    val otherVersion: List<ResultAlbum> = emptyList(),
    val loadState: LocalPlaylistState.PlaylistLoadState = LocalPlaylistState.PlaylistLoadState.Loading,
) {
    companion object {
        fun initial(): AlbumUIState = AlbumUIState()
    }
}