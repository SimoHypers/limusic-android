package com.simohypers.limusic.data.repository

import com.simohypers.limusic.data.db.datasource.LocalDataSource
import com.simohypers.limusic.data.extension.getFullDataFromDB
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.manager.DataStoreManager.Values.TRUE
import com.simohypers.limusic.domain.utils.isRadioPlaylistId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Likes, follows and saves go to YouTube Music while signed in and stay on the phone while signed
 * out, like desktop's library saves. This holds what the phone has that the account doesn't yet,
 * as `kind:id` keys (song, album, playlist, artist), for Library's Sync button to push.
 *
 * Only additions are tracked: taking something out while signed out drops its key and leaves the
 * account alone, as desktop does.
 */
internal class LibrarySync(
    private val dataStoreManager: DataStoreManager,
    private val localDataSource: LocalDataSource,
) {
    private val mutex = Mutex()

    suspend fun loggedIn() = dataStoreManager.loggedIn.first() == TRUE

    suspend fun isUnsynced(key: String) = mutex.withLock { read() }.contains(key)

    suspend fun mark(
        key: String,
        unsynced: Boolean,
    ) = mutex.withLock {
        val keys = read()
        val next = if (unsynced) keys + key else keys - key
        if (next != keys) dataStoreManager.putString(KEY, next.joinToString("\n"))
    }

    /**
     * Signed in, a playlist, album, history or Liked songs page states the account's like on each
     * row, which is fresher than the phone's copy, so the songs already stored take it. Search and
     * radio rows state nothing and are left alone. A like still waiting for Sync keeps the phone's.
     * Rows not stored yet take theirs as they are stored (toSongEntity); reading the set here
     * first seeds it before any of those land, so the seed only holds the phone's own likes.
     *
     * ponytail: no guard against a heart tapped while the page was loading; the page's older
     * answer can win that race, and the next play's check (refreshLikeFromYouTube) corrects it.
     */
    suspend fun adoptLikes(tracks: List<Track>) {
        val (liked, notLiked) = tracks.filter { it.likeStatus != null }.partition { it.likeStatus == "LIKE" }
        adopt(liked.map { it.videoId }, notLiked.map { it.videoId })
    }

    /** [adoptLikes] by id: the account likes [liked] and not [notLiked]. */
    suspend fun adopt(
        liked: List<String>,
        notLiked: List<String>,
    ) {
        if (!loggedIn()) return
        val unsynced = mutex.withLock { read() }
        localDataSource.adoptLiked(liked, true)
        localDataSource.adoptLiked(notLiked.filterNot { song(it) in unsynced }, false)
    }

    fun keys(): Flow<Set<String>> =
        dataStoreManager
            .getString(KEY)
            .onStart { mutex.withLock { read() } }
            .map { it.toKeys() }

    /**
     * The first read seeds the set with everything this phone saved before likes and follows went
     * to YouTube: back then they never left the phone. Unset and empty differ, so it seeds once.
     */
    private suspend fun read(): Set<String> {
        dataStoreManager.getString(KEY).first()?.let { return it.toKeys() }
        val seed =
            buildSet {
                getFullDataFromDB { limit, offset -> localDataSource.getLikedSongs(limit, offset) }
                    .forEach { add(song(it.videoId)) }
                getFullDataFromDB { limit, offset -> localDataSource.getFollowedArtists(limit, offset) }
                    .forEach { add(artist(it.channelId)) }
                getFullDataFromDB { limit, offset -> localDataSource.getLikedAlbums(limit, offset) }
                    .forEach { add(album(it.browseId)) }
                getFullDataFromDB { limit, offset -> localDataSource.getLikedPlaylists(limit, offset) }
                    .filter { it.id.removePrefix("VL") != "LM" && !it.id.isRadioPlaylistId() }
                    .forEach { add(playlist(it.id)) }
            }
        dataStoreManager.putString(KEY, seed.joinToString("\n"))
        return seed
    }

    private fun String?.toKeys() = this?.split('\n')?.filter { it.isNotEmpty() }?.toSet().orEmpty()

    companion object {
        private const val KEY = "library_unsynced"

        fun song(videoId: String) = "song:$videoId"

        fun album(browseId: String) = "album:$browseId"

        fun playlist(id: String) = "playlist:${id.removePrefix("VL")}"

        fun artist(channelId: String) = "artist:$channelId"
    }
}
