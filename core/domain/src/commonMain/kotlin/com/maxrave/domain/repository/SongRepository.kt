package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.QueueEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface SongRepository {
    fun getAllSongs(limit: Int): Flow<List<SongEntity>>

    suspend fun setInLibrary(
        videoId: String,
        inLibrary: LocalDateTime,
    )

    fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>>

    fun getDownloadedSongs(): Flow<List<SongEntity>?>

    fun getDownloadingSongs(): Flow<List<SongEntity>?>

    fun getPreparingSongs(): Flow<List<SongEntity>>

    fun getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId: List<String>): Flow<List<String>>

    fun getLikedSongs(): Flow<List<SongEntity>>

    /** Liked songs crediting [channelId] anywhere in their artist list; re-emits on every change. */
    fun getLikedSongsByArtist(channelId: String): Flow<List<SongEntity>>

    /**
     * Queue every liked song that is not already offline, and return how many were queued.
     *
     * Used when the user switches auto-download on: liking a song from then on downloads it, and
     * this catches up on everything they liked before. Songs already downloaded are skipped, so
     * turning the setting off and on again costs nothing.
     */
    suspend fun downloadAllLikedSongs(): Int

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

    /** Signed in, copy YouTube's like for [videoId] onto the local row the hearts read. */
    suspend fun refreshLikeFromYouTube(videoId: String)

    fun downloadToFile(
        track: Track,
        path: String,
        videoId: String,
        isVideo: Boolean,
    ): Flow<DownloadProgress>

    fun getRelatedData(videoId: String): Flow<Resource<Pair<List<Track>, String?>>>

    fun getRadioFromEndpoint(endpoint: YouTubeWatchEndpoint): Flow<Resource<Pair<List<Track>, String?>>>
}