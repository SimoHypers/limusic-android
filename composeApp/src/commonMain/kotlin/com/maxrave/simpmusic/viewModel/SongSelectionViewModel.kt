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
 * The bulk actions that need the database or YouTube: Download and Like on
 * [com.maxrave.simpmusic.ui.component.selection.TrackSelectionBar], and the Save to playlist
 * picker it shares with the playlist and album menus.
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

    /**
     * Only starts songs that are not downloaded or in flight. Not a toggle like the song menu's: a
     * selection usually mixes both, and toggling would delete the ones already on disk. Rows a page
     * has not stored yet are saved first.
     */
    fun downloadTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val pending = stored(tracks).filter { it.downloadState == DownloadState.STATE_NOT_DOWNLOADED }
            if (pending.isEmpty()) return@launch
            downloadUtils.downloadTracks(pending)
            makeToast(getString(Res.string.downloading))
        }
    }

    /** One like at a time, as each is a write to the account when signed in. Toasts once all are in. */
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
        // One toast for the batch, not one per song.
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

    /** The database returns rows in its own order, so they are put back in the list's. */
    private suspend fun songsOf(videoIds: List<String>): List<SongEntity> {
        val order = videoIds.withIndex().associate { (i, id) -> id to i }
        // Chunked under SQLite's 999-variable limit on older Androids: Select all has no cap.
        return videoIds
            .chunked(900)
            .flatMap { songRepository.getSongsByListVideoId(it).firstOrNull().orEmpty() }
            .sortedBy { order[it.videoId] }
    }
}
