package com.simohypers.limusic.viewModel

import androidx.lifecycle.viewModelScope
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.entities.DownloadState
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.model.searchResult.songs.Album
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import com.simohypers.limusic.domain.mediaservice.handler.PlayerEvent
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.domain.mediaservice.handler.SleepTimerState
import com.simohypers.limusic.domain.repository.ArtistRepository
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.PlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.utils.Resource
import com.simohypers.limusic.domain.utils.collectLatestResource
import com.simohypers.limusic.domain.utils.collectResource
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.expect.shareUrl
import com.simohypers.limusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.starting_radio
import org.koin.core.component.inject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.added_to_playlist
import limusic.composeapp.generated.resources.added_to_queue
import limusic.composeapp.generated.resources.added_to_youtube_playlist
import limusic.composeapp.generated.resources.delete_song_from_playlist
import limusic.composeapp.generated.resources.downloading
import limusic.composeapp.generated.resources.error
import limusic.composeapp.generated.resources.error_occurred
import limusic.composeapp.generated.resources.play_next
import limusic.composeapp.generated.resources.removed_download
import limusic.composeapp.generated.resources.removed_from_YouTube_playlist
import limusic.composeapp.generated.resources.share_url
import limusic.composeapp.generated.resources.sleep_timer_off_done
import limusic.composeapp.generated.resources.youtube_write_failed

class NowPlayingBottomSheetViewModel(
    private val dataStoreManager: DataStoreManager,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject()
    private val artistRepository: ArtistRepository by inject()
    private val _uiState: MutableStateFlow<NowPlayingBottomSheetUIState> =
        MutableStateFlow(
            NowPlayingBottomSheetUIState(
                listLocalPlaylist = emptyList(),
                listYouTubePlaylist = null,
                sleepTimer =
                    SleepTimerState(
                        false,
                        0,
                    ),
            ),
        )
    val uiState: StateFlow<NowPlayingBottomSheetUIState> get() = _uiState.asStateFlow()

    private var getSongAsFlow: Job? = null

    init {
        viewModelScope.launch {
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest { sl ->
                        _uiState.update { it.copy(sleepTimer = sl) }
                    }
                }
            val listLocalPlaylistJob =
                launch {
                    localPlaylistRepository.getAllLocalPlaylists().collectLatest { list ->
                        _uiState.update { it.copy(listLocalPlaylist = list) }
                    }
                }
            val listYouTubePlaylistJob =
                launch {
                    playlistRepository.getLibraryPlaylist().collect { data ->
                        _uiState.update { state ->
                            state.copy(
                                listYouTubePlaylist =
                                    data?.filter {
                                        it.browseId != "VLLM"
                                    } ?: emptyList(),
                            )
                        }
                    }
                }
            val loggedInJob =
                launch {
                    dataStoreManager.loggedIn.collectLatest { v ->
                        _uiState.update { it.copy(loggedIn = v == DataStoreManager.TRUE) }
                    }
                }
            sleepTimerJob.join()
            loggedInJob.join()
            listLocalPlaylistJob.join()
            listYouTubePlaylistJob.join()
        }
    }

    /** The photos the phone already has for these artists (page opened, or followed); nothing is fetched. */
    suspend fun localArtistPhotos(ids: List<String>): Map<String, String?> =
        ids.associateWith { artistRepository.getArtistById(it).firstOrNull()?.thumbnails }

    fun resetPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collectLatest { list ->
                _uiState.update { it.copy(listLocalPlaylist = list) }
            }
        }
        viewModelScope.launch {
            playlistRepository.getLibraryPlaylist().collect { data ->
                _uiState.update { state ->
                    state.copy(
                        listYouTubePlaylist =
                            data?.filter {
                                it.browseId != "VLLM"
                            } ?: emptyList(),
                    )
                }
            }
        }
    }

    fun setSongEntity(songEntity: SongEntity?) {
        val songOrNowPlaying = songEntity ?: (mediaPlayerHandler.nowPlayingState.value.songEntity ?: return)
        viewModelScope.launch {
            songOrNowPlaying.videoId.let {
                _uiState.update { state ->
                    state.copy(
                        songUIState =
                            state.songUIState.copy(
                                isAddedToYouTubeLiked = false,
                            ),
                    )
                }
                songRepository.getSongById(it).lastOrNull().let { song ->
                    if (song != null) {
                        getSongEntityFlow(videoId = song.videoId)
                    } else {
                        songRepository.insertSong(songOrNowPlaying).singleOrNull()?.let {
                            getSongEntityFlow(videoId = songOrNowPlaying.videoId)
                        }
                    }
                }
                // A song stored from a card that named its artists in plain text gains its links
                // here; the flow above redraws the menu with Go to artist / Go to album.
                songRepository.fillMissingLinks(it)
            }
        }
    }

    private fun getSongEntityFlow(videoId: String) {
        getSongAsFlow?.cancel()
        if (videoId.isEmpty()) return
        getSongAsFlow =
            viewModelScope.launch {
                songRepository.getSongAsFlow(videoId).collectLatest { song ->
                    log("getSongEntityFlow: $song")
                    if (song != null) {
                        _uiState.update { state ->
                            state.copy(
                                songUIState =
                                    NowPlayingBottomSheetUIState.SongUIState(
                                        videoId = song.videoId,
                                        title = song.title,
                                        listArtists =
                                            song.artistName?.mapIndexed { i, name ->
                                                Artist(name = name, id = song.artistId?.getOrNull(i) ?: "")
                                            } ?: emptyList(),
                                        thumbnails = song.thumbnails,
                                        liked = song.liked,
                                        disliked = song.likeStatus == "DISLIKE",
                                        downloadState = song.downloadState,
                                        album =
                                            song.albumName?.takeIf { it.isNotEmpty() }?.let { name ->
                                                Album(name = name, id = song.albumId ?: "")
                                            },
                                    ),
                            )
                        }
                    }
                }
            }
    }

    fun onUIEvent(ev: NowPlayingBottomSheetUIEvent) {
        val songUIState = uiState.value.songUIState
        if (songUIState.videoId.isEmpty()) return
        viewModelScope.launch {
            when (ev) {
                is NowPlayingBottomSheetUIEvent.DeleteFromPlaylist -> {
                    localPlaylistRepository
                        .removeTrackFromLocalPlaylist(
                            id = ev.playlistId,
                            song =
                                songRepository.getSongById(ev.videoId).lastOrNull() ?: run {
                                    makeToast(getString(Res.string.error_occurred))
                                    return@launch
                                },
                            successMessage = getString(Res.string.delete_song_from_playlist),
                            updatedYtMessage = getString(Res.string.removed_from_YouTube_playlist),
                            errorMessage = getString(Res.string.error_occurred),
                        ).collectResource(
                            onSuccess = {
                                makeToast(it ?: getString(Res.string.delete_song_from_playlist))
                            },
                            onError = {
                                makeToast(it)
                            },
                        )
                }

                is NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist -> {
                    localPlaylistRepository
                        .addYouTubePlaylistItem(
                            youtubePlaylistId = ev.browseId,
                            videoId = songUIState.videoId,
                        ).collectLatestResource(
                            // Both carry YouTube's raw status ("STATUS_SUCCEEDED" / "FAILED"), not words.
                            onSuccess = {
                                makeToast(getString(Res.string.added_to_playlist))
                            },
                            onError = {
                                makeToast(getString(Res.string.error))
                            },
                        )
                }

                is NowPlayingBottomSheetUIEvent.ToggleLike -> {
                    val written =
                        songRepository.updateLikeStatus(
                            songUIState.videoId,
                            if (songUIState.liked) 0 else 1,
                        )
                    if (!written) makeToast(getString(Res.string.youtube_write_failed))
                }

                is NowPlayingBottomSheetUIEvent.ToggleDislike -> {
                    val id = songUIState.videoId
                    val disliking = !songUIState.disliked
                    if (!songRepository.updateDislike(id, disliking)) {
                        makeToast(getString(Res.string.youtube_write_failed))
                    } else if (disliking) {
                        // Like desktop: a disliked song shouldn't play, or wait to. Upcoming copies
                        // go back to front so the indices stay valid; played ones are history.
                        val queue = mediaPlayerHandler.queueData.value?.data?.listTracks.orEmpty()
                        val current = mediaPlayerHandler.currentOrderIndex()
                        for (i in queue.indices.reversed()) {
                            if (i > current && queue[i].videoId == id) mediaPlayerHandler.removeMediaItem(i)
                        }
                        if (mediaPlayerHandler.nowPlayingState.value.songEntity?.videoId == id) {
                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                        }
                    }
                }

                is NowPlayingBottomSheetUIEvent.Download -> {
                    // The handler writes the song's state as the download moves (or is removed).
                    if (songUIState.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                        songRepository.getSongById(songUIState.videoId).first()?.let { downloadUtils.downloadTracks(listOf(it)) }
                        makeToast(getString(Res.string.downloading))
                    } else {
                        downloadUtils.removeDownload(songUIState.videoId)
                        makeToast(getString(Res.string.removed_download))
                    }
                }

                is NowPlayingBottomSheetUIEvent.AddToPlaylist -> {
                    // One New playlist just made may not have reached this list yet; it holds nothing then.
                    val tracks = uiState.value.listLocalPlaylist.find { it.id == ev.playlistId }?.tracks.orEmpty()
                    if (tracks.contains(songUIState.videoId)) {
                        return@launch
                    } else {
                        val songEntity = songRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                        localPlaylistRepository
                            .addTrackToLocalPlaylist(
                                id = ev.playlistId,
                                song = songEntity,
                                successMessage = getString(Res.string.added_to_playlist),
                                updatedYtMessage = getString(Res.string.added_to_youtube_playlist),
                                errorMessage = getString(Res.string.error),
                            ).collectLatestResource(
                                onSuccess = {
                                    makeToast(it ?: getString(Res.string.added_to_playlist))
                                },
                                onError = {
                                    makeToast(it)
                                },
                            )
                    }
                }

                is NowPlayingBottomSheetUIEvent.PlayNext -> {
                    val songEntity = songRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    mediaPlayerHandler.playNext(listOf(songEntity.toTrack()))
                    makeToast(getString(Res.string.play_next))
                }

                is NowPlayingBottomSheetUIEvent.AddToQueue -> {
                    val songEntity = songRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    mediaPlayerHandler.addToQueue(listOf(songEntity.toTrack()))
                    makeToast(getString(Res.string.added_to_queue))
                }

                is NowPlayingBottomSheetUIEvent.SetSleepTimer -> {
                    if (ev.cancel) {
                        mediaPlayerHandler.sleepStop()
                        makeToast(getString(Res.string.sleep_timer_off_done))
                    } else if (ev.minutes > 0) {
                        mediaPlayerHandler.sleepStart(ev.minutes)
                    }
                }

                is NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch -> {
                    dataStoreManager.setPlaybackSpeed(ev.speed)
                    dataStoreManager.setPitch(ev.pitch)
                }

                is NowPlayingBottomSheetUIEvent.Share -> {
                    val url = "https://music.youtube.com/watch?v=${songUIState.videoId}"
                    shareUrl(
                        title = getString(Res.string.share_url),
                        url,
                    )
                }

                is NowPlayingBottomSheetUIEvent.StartRadio -> {
                    songRepository
                        .getRadioFromEndpoint(
                            YouTubeWatchEndpoint(
                                videoId = ev.videoId,
                                playlistId = "RDAMVM${ev.videoId}",
                            ),
                        ).busy(Res.string.starting_radio)
                        .collectLatest { res ->
                            val data = res.data
                            when (res) {
                                is Resource.Success if (data != null && data.first.isNotEmpty()) -> {
                                    setQueueData(
                                        QueueData.Data(
                                            listTracks = data.first,
                                            firstPlayedTrack = data.first.first(),
                                            playlistId = "RDAMVM${ev.videoId}",
                                            playlistName = ev.name,
                                            playlistType = PlaylistType.RADIO,
                                            continuation = data.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        data.first.first(),
                                        Config.PLAYLIST_CLICK,
                                        0,
                                    )
                                }

                                else -> {
                                    makeToast(res.message ?: getString(Res.string.error))
                                }
                            }
                        }
                }
            }
        }
    }
}

data class NowPlayingBottomSheetUIState(
    val songUIState: SongUIState = SongUIState(),
    val listLocalPlaylist: List<LocalPlaylistEntity>,
    // Null until the account's playlists have come back.
    val listYouTubePlaylist: List<PlaylistsResult>?,
    val sleepTimer: SleepTimerState,
    val loggedIn: Boolean = false,
) {
    data class SongUIState(
        val videoId: String = "",
        val title: String = "",
        val listArtists: List<Artist> = emptyList(),
        val thumbnails: String? = null,
        val liked: Boolean = false,
        val disliked: Boolean = false,
        val isAddedToYouTubeLiked: Boolean = false,
        val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
        val album: Album? = null,
    )
}

sealed class NowPlayingBottomSheetUIEvent {
    data class DeleteFromPlaylist(
        val videoId: String,
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data object ToggleLike : NowPlayingBottomSheetUIEvent()

    data object ToggleDislike : NowPlayingBottomSheetUIEvent()

    data object Download : NowPlayingBottomSheetUIEvent()

    data class AddToPlaylist(
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data class AddToYouTubePlaylist(
        val browseId: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object PlayNext : NowPlayingBottomSheetUIEvent()

    data object AddToQueue : NowPlayingBottomSheetUIEvent()

    data class SetSleepTimer(
        val cancel: Boolean = false,
        val minutes: Int = 0,
    ) : NowPlayingBottomSheetUIEvent()

    data class ChangePlaybackSpeedPitch(
        val speed: Float,
        val pitch: Int,
    ) : NowPlayingBottomSheetUIEvent()

    data class StartRadio(
        val videoId: String,
        val name: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object Share : NowPlayingBottomSheetUIEvent()
}