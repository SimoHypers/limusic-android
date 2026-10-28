package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.utils.toSongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.collectResource
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.liked_n_songs
import simpmusic.composeapp.generated.resources.added_to_playlist
import simpmusic.composeapp.generated.resources.adding_to_playlist
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.delete_song_from_playlist
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_from_YouTube_playlist
import simpmusic.composeapp.generated.resources.youtube_write_failed

/**
 * Runs the bulk actions offered by
 * [com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet].
 *
 * Shared by every screen that offers multi-selection: the actions only ever need videoIds, so
 * they do not depend on which screen the selection was made on. The one exception is removing
 * from a local playlist, which takes the playlist id from the screen.
 */
class SongSelectionViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject()
    private val playlistRepository: PlaylistRepository by inject()

    val listLocalPlaylist: StateFlow<List<LocalPlaylistEntity>> =
        localPlaylistRepository
            .getAllLocalPlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playNext(videoIds: List<String>) {
        viewModelScope.launch {
            val songs = songsOf(videoIds)
            if (songs.isEmpty()) {
                makeToast(getString(Res.string.error_occurred))
                return@launch
            }
            mediaPlayerHandler.playNext(songs.map { it.toTrack() })
            makeToast(getString(Res.string.play_next))
        }
    }

    fun addToQueue(videoIds: List<String>) {
        viewModelScope.launch {
            val songs = songsOf(videoIds)
            if (songs.isEmpty()) {
                makeToast(getString(Res.string.error_occurred))
                return@launch
            }
            mediaPlayerHandler.addToQueue(songs.map { it.toTrack() })
            makeToast(getString(Res.string.added_to_queue))
        }
    }

    /**
     * Only starts songs that are not already downloaded or in flight — unlike the single-song
     * menu, this is not a toggle: a selection of 25 will usually mix downloaded and not, and
     * toggling would silently delete the ones already on disk.
     */
    fun download(videoIds: List<String>) {
        viewModelScope.launch {
            val pending =
                songsOf(videoIds).filter {
                    it.downloadState == DownloadState.STATE_NOT_DOWNLOADED
                }
            if (pending.isEmpty()) return@launch
            downloadUtils.downloadTracks(pending)
            makeToast(getString(Res.string.downloading))
        }
    }

    fun addToFavorite(videoIds: List<String>) {
        viewModelScope.launch {
            // One at a time: signed in each is a write to the YouTube account.
            val failed =
                songsOf(videoIds)
                    .filterNot { it.liked }
                    .count { !songRepository.updateLikeStatus(it.videoId, 1) }
            if (failed > 0) makeToast(getString(Res.string.youtube_write_failed))
        }
    }

    /** [download] for rows a page may not have stored yet: they are saved first. */
    fun downloadTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val pending = stored(tracks).filter { it.downloadState == DownloadState.STATE_NOT_DOWNLOADED }
            if (pending.isEmpty()) return@launch
            downloadUtils.downloadTracks(pending)
            makeToast(getString(Res.string.downloading))
        }
    }

    /** [addToFavorite] for rows a page may not have stored yet. Toasts once YouTube has them all. */
    fun likeTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val songs = stored(tracks)
            val failed = songs.filterNot { it.liked }.count { !songRepository.updateLikeStatus(it.videoId, 1) }
            makeToast(
                if (failed > 0) {
                    getString(Res.string.youtube_write_failed)
                } else {
                    getPluralString(Res.plurals.liked_n_songs, songs.size, songs.size)
                },
            )
        }
    }

    private suspend fun stored(tracks: List<Track>): List<SongEntity> {
        // Ignored where the row exists, so nothing already saved is overwritten.
        tracks.forEach { songRepository.insertSong(it.toSongEntity().copy(inLibrary = Config.REMOVED_SONG_DATE_TIME)).firstOrNull() }
        return songsOf(tracks.map { it.videoId })
    }

    fun addToPlaylist(
        playlistId: Long,
        videoIds: List<String>,
    ) {
        viewModelScope.launch { addSongsToPlaylist(playlistId, songsOf(videoIds)) }
    }

    /** Save to playlist from a playlist or album menu: the tracks need not be in the database yet. */
    fun addTracksToPlaylist(
        playlistId: Long,
        tracks: List<Track>,
    ) {
        viewModelScope.launch { addSongsToPlaylist(playlistId, tracks.map { it.toSongEntity() }) }
    }

    private suspend fun addSongsToPlaylist(
        playlistId: Long,
        songs: List<SongEntity>,
    ) {
        val playlist = localPlaylistRepository.getAllLocalPlaylists().firstOrNull()?.find { it.id == playlistId }
        val already = playlist?.tracks.orEmpty()
        val toAdd = songs.filterNot { already.contains(it.videoId) }
        if (toAdd.isEmpty()) return
        var added = 0
        toAdd.forEach { song ->
            localPlaylistRepository
                .addTrackToLocalPlaylist(
                    id = playlistId,
                    song = song,
                    successMessage = getString(Res.string.added_to_playlist),
                    updatedYtMessage = getString(Res.string.added_to_playlist),
                    errorMessage = getString(Res.string.error),
                ).collectResource(
                    onSuccess = { added++ },
                    onError = { },
                )
        }
        // One toast for the batch: 25 songs would otherwise stack 25 toasts.
        makeToast(
            if (added > 0) getString(Res.string.added_to_playlist) else getString(Res.string.error),
        )
    }

    private val _youTubePlaylists = MutableStateFlow<List<PlaylistsResult>>(emptyList())

    /** Your YouTube playlists for the Save to playlist sheet, fetched when it opens. */
    val youTubePlaylists: StateFlow<List<PlaylistsResult>> = _youTubePlaylists

    fun loadYouTubePlaylists() {
        viewModelScope.launch {
            playlistRepository.getLibraryPlaylist().collect { list ->
                // Liked Music is filled by liking, not by adding.
                _youTubePlaylists.value = list?.filter { it.browseId != "VLLM" }.orEmpty()
            }
        }
    }

    /**
     * One request per song, in order, like Limusic desktop: YouTube refuses a song the playlist
     * already holds, and a refusal only skips that song.
     */
    // ponytail: runs in this screen's scope, so leaving the page mid-save stops it; move it to an
    // app-wide scope if long playlists get saved and left often.
    fun addTracksToYouTubePlaylist(
        browseId: String,
        tracks: List<Track>,
    ) {
        viewModelScope.launch {
            makeToast(getString(Res.string.adding_to_playlist))
            var added = 0
            tracks.forEach { track ->
                localPlaylistRepository
                    .addYouTubePlaylistItem(youtubePlaylistId = browseId, videoId = track.videoId)
                    .collectResource(onSuccess = { added++ }, onError = { })
            }
            makeToast(
                if (added > 0) getString(Res.string.added_to_playlist) else getString(Res.string.error),
            )
        }
    }

    fun removeFromLocalPlaylist(
        playlistId: Long,
        videoIds: List<String>,
    ) {
        viewModelScope.launch {
            val songs = songsOf(videoIds)
            if (songs.isEmpty()) {
                makeToast(getString(Res.string.error_occurred))
                return@launch
            }
            songs.forEach { song ->
                localPlaylistRepository
                    .removeTrackFromLocalPlaylist(
                        id = playlistId,
                        song = song,
                        successMessage = getString(Res.string.delete_song_from_playlist),
                        updatedYtMessage = getString(Res.string.removed_from_YouTube_playlist),
                        errorMessage = getString(Res.string.error_occurred),
                    ).collectResource(
                        onSuccess = { },
                        onError = { },
                    )
            }
            makeToast(getString(Res.string.delete_song_from_playlist))
        }
    }

    /**
     * The database returns rows in its own order, so the result is re-sorted back into the order
     * the user picked them in — which is what makes "play next" land in the expected sequence.
     */
    private suspend fun songsOf(videoIds: List<String>): List<SongEntity> {
        val order = videoIds.withIndex().associate { (i, id) -> id to i }
        // Chunked under SQLite's 999-variable limit on older Androids: Select all has no cap.
        return videoIds
            .chunked(900)
            .flatMap { songRepository.getSongsByListVideoId(it).firstOrNull().orEmpty() }
            .sortedBy { order[it.videoId] }
    }
}
