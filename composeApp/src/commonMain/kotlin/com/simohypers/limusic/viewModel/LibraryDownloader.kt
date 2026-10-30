package com.simohypers.limusic.viewModel

import com.simohypers.limusic.domain.data.entities.DownloadState.STATE_DOWNLOADED
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.player.PlaybackRefusals
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import com.simohypers.limusic.domain.repository.AlbumRepository
import com.simohypers.limusic.domain.repository.LocalPlaylistRepository
import com.simohypers.limusic.domain.repository.PlaylistRepository
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.utils.isRadioPlaylistId
import com.simohypers.limusic.domain.utils.toAlbumEntity
import com.simohypers.limusic.domain.utils.toListVideoId
import com.simohypers.limusic.domain.utils.toPlaylistEntity
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.view_count
import kotlin.time.Clock

/**
 * Keep library downloaded: the liked songs, every library playlist and every saved album become
 * downloaded collections, and their songs queue. Signed in the lists come from the account, signed
 * out from the phone. Browses go one at a time, a second apart: a library is dozens of requests from
 * the listener's IP, and the songs' own stream lookups come on top. A bot check or rate limit
 * anywhere (PlaybackRefusals.blocked) ends the pass.
 */
internal class LibraryDownloader : KoinComponent {
    private val dataStoreManager: DataStoreManager by inject()
    private val playlistRepository: PlaylistRepository by inject()
    private val albumRepository: AlbumRepository by inject()
    private val localPlaylistRepository: LocalPlaylistRepository by inject()
    private val songRepository: SongRepository by inject()
    private val downloadHandler: DownloadHandler by inject()

    /** At most once a day while the switch is on, unless [force]d (the switch was just turned on). */
    suspend fun runIfDue(force: Boolean = false) {
        if (dataStoreManager.keepLibraryDownloaded.first() != DataStoreManager.TRUE) return
        val last = dataStoreManager.getString(LAST_RUN).first()?.toLongOrNull() ?: 0L
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force && now - last < DAY_MS) return
        // Recorded only when every list was read, so a run cut short tries again next launch.
        val complete =
            try {
                run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, "Library download pass failed: ${e.message}")
                false
            }
        if (complete) dataStoreManager.putString(LAST_RUN, now.toString())
    }

    private suspend fun run(): Boolean {
        var ok = true
        if (dataStoreManager.loggedIn.first() == DataStoreManager.TRUE) {
            val library = playlistRepository.getLibraryPlaylist().firstOrNull()
            if (library == null) ok = false
            // "SE" is Episodes for later: podcast episodes, not songs.
            val ids = listOf("LM") + library.orEmpty().map { it.browseId.removePrefix("VL") }.filter { it != "SE" }
            ids.distinct().forEach { ok = keepPlaylist(it) && ok }
            val albums = albumRepository.getLibraryAlbums()
            if (albums == null) ok = false
            albums.orEmpty().forEach { ok = keepAlbum(it.browseId) && ok }
        } else {
            downloadHandler.downloadTracks(songRepository.getLikedSongs().first())
            playlistRepository
                .getLikedPlaylists()
                .first()
                .filter { it.id.removePrefix("VL") != "LM" && !it.id.isRadioPlaylistId() }
                .forEach { ok = keepPlaylist(it.id) && ok }
            albumRepository.getLikedAlbums().first().forEach { ok = keepAlbum(it.browseId) && ok }
        }
        localPlaylistRepository.getAllLocalPlaylists().first().forEach { playlist ->
            if (playlist.downloadState != STATE_DOWNLOADED) localPlaylistRepository.updateLocalPlaylistDownloadState(STATE_DOWNLOADED, playlist.id)
            downloadHandler.downloadTracks(localPlaylistRepository.getFullPlaylistTracks(playlist.id))
        }
        Logger.i(TAG, "Library download pass done, complete: $ok")
        return ok
    }

    /** Reads the playlist to its last page, stores that list on its row and queues the songs. */
    private suspend fun keepPlaylist(id: String): Boolean {
        delay(GAP_MS)
        if (PlaybackRefusals.blocked.value != null) return false
        val (browse, firstToken) = playlistRepository.getPlaylistData(id, getString(Res.string.view_count)).firstOrNull()?.data ?: return false
        val tracks = ArrayList<Track>(browse.tracks)
        var token = firstToken
        var pages = 0
        while (!token.isNullOrEmpty() && pages++ < MAX_PAGES) {
            delay(GAP_MS)
            if (PlaybackRefusals.blocked.value != null) return false
            // A page that did not come back would store a short list; keep yesterday's instead.
            val (more, next) = songRepository.getContinueTrack(browse.id, token, fromPlaylist = true).firstOrNull() ?: return false
            more ?: return false
            tracks += more
            token = next
        }
        val existing = playlistRepository.getPlaylist(browse.id).first()
        playlistRepository.insertAndReplacePlaylist(
            (existing ?: browse.toPlaylistEntity()).copy(tracks = tracks.toListVideoId(), trackCount = tracks.size),
        )
        if (existing?.downloadState != STATE_DOWNLOADED) playlistRepository.updatePlaylistDownloadState(browse.id, STATE_DOWNLOADED)
        downloadHandler.downloadTracks(tracks.map { it.toSongEntity() })
        return true
    }

    /** An album's songs never change: one already kept is retried from its stored list, with no request. */
    private suspend fun keepAlbum(browseId: String): Boolean {
        val existing = albumRepository.getAlbum(browseId).first()
        val stored = existing?.tracks
        if (existing?.downloadState == STATE_DOWNLOADED && !stored.isNullOrEmpty()) {
            downloadHandler.downloadTracks(songRepository.getSongsByListVideoId(stored).first())
            return true
        }
        delay(GAP_MS)
        if (PlaybackRefusals.blocked.value != null) return false
        val album = albumRepository.getAlbumData(browseId).firstOrNull()?.data ?: return false
        if (existing == null) albumRepository.insertAlbum(album.toAlbumEntity(browseId)).first()
        albumRepository.updateAlbumDownloadState(browseId, STATE_DOWNLOADED)
        downloadHandler.downloadTracks(album.tracks.map { it.toSongEntity() })
        return true
    }

    private companion object {
        const val TAG = "LibraryDownloader"
        const val LAST_RUN = "library_downloaded_at"
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val GAP_MS = 1000L

        // 100 songs a page: 5000 songs is past any playlist anyone keeps.
        const val MAX_PAGES = 50
    }
}
