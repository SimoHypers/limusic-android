package com.simohypers.limusic.domain.repository

import com.simohypers.limusic.domain.data.entities.QueueEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.entities.SongInfoEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.download.CollectionDownload
import com.simohypers.limusic.domain.data.model.download.DownloadCount
import com.simohypers.limusic.domain.data.model.library.HistoryDay
import com.simohypers.limusic.domain.data.model.download.DownloadProgress
import com.simohypers.limusic.domain.data.model.streams.YouTubeWatchEndpoint
import com.simohypers.limusic.domain.data.player.PlaybackRefusals
import com.simohypers.limusic.domain.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime

interface SongRepository {
    fun getAllSongs(limit: Int): Flow<List<SongEntity>>

    suspend fun setInLibrary(
        videoId: String,
        inLibrary: LocalDateTime,
    )

    fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>>

    fun getDownloadedSongs(): Flow<List<SongEntity>?>

    /** Inserts the songs that have no row yet, in one transaction. */
    suspend fun insertSongs(songs: List<SongEntity>)

    /** One write for many songs; rows already in [downloadState] are left alone. */
    suspend fun updateDownloadStates(
        videoIds: List<String>,
        downloadState: Int,
    )

    /**
     * Startup: songs a previous run left queued or downloading that the download index no longer
     * has in flight ([active]) go back to not downloaded, and [completed] ones are marked downloaded.
     */
    suspend fun reconcileDownloads(
        active: List<String>,
        completed: List<String>,
    )

    /** After every download was removed: no song, playlist or album is downloaded any more. */
    suspend fun forgetAllDownloads()

    /** Every song a downloaded playlist, album or local playlist holds. */
    suspend fun keptVideoIds(): Set<String>

    fun downloadCount(videoIds: List<String>): Flow<DownloadCount>

    fun downloadedVideoIds(): Flow<Set<String>>

    fun getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId: List<String>): Flow<List<String>>

    fun getLikedSongs(): Flow<List<SongEntity>>

    /** Liked songs crediting [channelId] anywhere in their artist list; re-emits on every change. */
    fun getLikedSongsByArtist(channelId: String): Flow<List<SongEntity>>

    /**
     * Erase the local listening history and drop every song row nothing refers to any more.
     *
     * A song is kept when its id appears anywhere else at all: in an album, a YouTube playlist, a
     * local playlist, the saved queue, a podcast, or `set_video_id`. Everything left is a row the
     * app accumulated just by seeing the track once, and it takes its lyrics, translations, cached
     * format and extra info with it.
     *
     * @return how many songs were removed.
     */
    suspend fun clearHistoryAndOrphanedSongs(): Int

    fun getCanvasSong(max: Int): Flow<List<SongEntity>>

    fun getSongById(id: String): Flow<SongEntity?>

    fun getSongAsFlow(id: String): Flow<SongEntity?>

    fun insertSong(songEntity: SongEntity): Flow<Long>

    fun updateThumbnailsSongEntity(
        thumbnail: String,
        videoId: String,
    ): Flow<Int>

    /**
     * Corrects `song.videoType` for a row that predates, or was written before, the parsers started
     * carrying YouTube's real `MUSIC_VIDEO_TYPE_*`. Callers must pass a value they actually know —
     * writing an unknown over a correct one loses information.
     */
    fun updateVideoTypeSongEntity(
        videoType: String,
        videoId: String,
    ): Flow<Int>

    suspend fun updateListenCount(videoId: String)

    suspend fun resetTotalPlayTime(videoId: String)

    /**
     * The heart. Signed in it is a YouTube Music like, mirrored into the local row so every heart
     * agrees; signed out it stays on the phone until Library's Sync button. False when YouTube
     * refused, with the local row put back.
     */
    suspend fun updateLikeStatus(
        videoId: String,
        likeStatus: Int,
    ): Boolean

    /**
     * YouTube Music's dislike, signed in only. YouTube's ratings exclude each other, so a dislike
     * also clears the heart. Written to YouTube first, then the row; false when YouTube refused.
     */
    suspend fun updateDislike(
        videoId: String,
        disliked: Boolean,
    ): Boolean

    fun updateSongInLibrary(
        inLibrary: LocalDateTime,
        videoId: String,
    ): Flow<Int>

    suspend fun updateDurationSeconds(
        durationSeconds: Int,
        videoId: String,
    )

    fun getMostPlayedSongs(): Flow<List<SongEntity>>

    suspend fun updateDownloadState(
        videoId: String,
        downloadState: Int,
    )

    suspend fun getRecentSong(
        limit: Int,
        offset: Int,
    ): List<SongEntity>

    /** Songs this phone played, the last played first, each once. */
    suspend fun getPlayedSongs(limit: Int): List<SongEntity>

    /** The account's YouTube Music history in YouTube's day buckets, or null when it can't be read. */
    suspend fun getHistory(): List<HistoryDay>?

    suspend fun insertSongInfo(songInfo: SongInfoEntity)

    suspend fun getSongInfoEntity(videoId: String): Flow<SongInfoEntity?>

    suspend fun recoverQueue(temp: List<Track>)

    suspend fun removeQueue()

    suspend fun getSavedQueue(): Flow<List<QueueEntity>?>

    fun getContinueTrack(
        playlistId: String,
        continuation: String,
        fromPlaylist: Boolean = false,
    ): Flow<Pair<ArrayList<Track>?, String?>>

    fun getSongInfo(videoId: String): Flow<SongInfoEntity?>

    /** One page of Library ▸ Songs, or the page after [continuation]; null when YouTube refused. */
    suspend fun getLibrarySongs(continuation: String?): Pair<List<Track>, String?>?

    /**
     * Signed in, copy YouTube's like for [videoId] onto the local row the hearts read. Returns the
     * track's own queue row from the same answer, which names its other version (song or video).
     */
    suspend fun refreshLikeFromYouTube(videoId: String): Track?

    /** Signed in, pick up likes made on other devices from Liked Music. At most every 30 minutes. */
    suspend fun syncLikedMusic()

    /**
     * The stored song names no linked artist or no album (a card sent its artists as plain text):
     * fill them from the song's own queue row on YouTube. One request, once per song per run.
     */
    suspend fun fillMissingLinks(videoId: String)

    fun downloadToFile(
        track: Track,
        path: String,
        videoId: String,
        isVideo: Boolean,
    ): Flow<DownloadProgress>

    fun getRelatedData(videoId: String): Flow<Resource<Pair<List<Track>, String?>>>

    fun getRadioFromEndpoint(endpoint: YouTubeWatchEndpoint): Flow<Resource<Pair<List<Track>, String?>>>
}
@OptIn(ExperimentalCoroutinesApi::class)
fun SongRepository.collectionDownload(
    kept: Boolean,
    list: List<String>,
): Flow<CollectionDownload> {
    // A song in a playlist twice is one file: counted twice, "191 of 192" never reaches the end.
    val videoIds = list.distinct()
    return if (!kept || videoIds.isEmpty()) {
        flowOf(CollectionDownload(kept, videoIds.size))
    } else {
        // ponytail: a refused song is taken as not downloaded; only getStream records refusals, and a downloaded song never goes through it.
        // The rows are read only when the refused set changes, not on every progress tick.
        val refused =
            PlaybackRefusals.refusedIds
                .map { refusals -> videoIds.filter { it in refusals } }
                .distinctUntilChanged()
                .flatMapLatest { ids -> if (ids.isEmpty()) flowOf(ids to emptyList()) else getSongsByListVideoId(ids).map { ids to it } }
        combine(downloadCount(videoIds), refused) { count, (ids, songs) ->
            CollectionDownload(true, videoIds.size, count.downloaded, count.active, ids.size, songs)
        }
    }
}
