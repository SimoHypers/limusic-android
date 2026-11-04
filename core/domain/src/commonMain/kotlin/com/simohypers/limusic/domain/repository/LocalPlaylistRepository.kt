package com.simohypers.limusic.domain.repository

import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PairSongLocalPlaylist
import com.simohypers.limusic.domain.data.entities.SetVideoIdEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.utils.LocalResource
import kotlinx.coroutines.flow.Flow

interface LocalPlaylistRepository {
    fun getLocalPlaylist(id: Long): Flow<LocalResource<LocalPlaylistEntity?>>

    fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>>

    suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    )

    suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    )

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    )

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist)

    fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ): Flow<List<PairSongLocalPlaylist>?>

    fun getPlaylistPairOfSong(
        playlistId: Long,
        videoId: String,
    ): Flow<PairSongLocalPlaylist?>

    fun changePositionOfSongInPlaylist(
        playlistId: Long,
        videoId: String,
        newPosition: Int,
    ): Flow<String>

    /**
     * Move a song within a synced playlist: updates both YouTube (via API) and local DB positions.
     * @param playlistId Local playlist ID
     * @param fromIndex The current index of the item being moved (0-based, in CustomOrder)
     * @param toIndex The target index to move the item to (0-based, in CustomOrder)
     * @return Flow<LocalResource<String>> success/error message
     */
    fun moveItemInSyncedPlaylist(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int,
    ): Flow<LocalResource<String>>

    /**
     * Move a song within an unsynced local playlist: updates local DB positions only.
     * Items between the two indexes are shifted by one so the moved item lands at [toIndex]
     * without displacing anything else.
     * @param playlistId Local playlist ID
     * @param fromIndex The current index of the item being moved (0-based, in CustomOrder)
     * @param toIndex The target index to move the item to (0-based, in CustomOrder)
     * @return Flow<LocalResource<String>> success/error message
     */
    fun moveItemInLocalPlaylist(
        playlistId: Long,
        fromIndex: Int,
        toIndex: Int,
    ): Flow<LocalResource<String>>

    fun downloadStateFlow(id: Long): Flow<Int>

    fun listTrackFlow(id: Long): Flow<List<String>>

    /** Search one local playlist by song title or artist name, in stored order. */
    fun searchTracks(
        id: Long,
        query: String,
        limit: Int = 200,
    ): Flow<List<Pair<SongEntity, PairSongLocalPlaylist>>>

    /** Every row of the playlist in stored (Custom order) position, again on each add, removal or move. */
    fun playlistTracks(id: Long): Flow<List<Pair<SongEntity, PairSongLocalPlaylist>>>

    suspend fun getFullPlaylistTracks(id: Long): List<SongEntity>

    suspend fun getListTrackVideoId(id: Long): List<String>

    fun insertLocalPlaylist(
        localPlaylist: LocalPlaylistEntity,
        successMessage: String,
    ): Flow<LocalResource<String>>

    /** A new, empty playlist on the phone; returns its id. */
    suspend fun createLocalPlaylist(title: String): Long

    fun deleteLocalPlaylist(
        id: Long,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateTitleLocalPlaylist(
        id: Long,
        newTitle: String,
        updatedMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun updateThumbnailLocalPlaylist(
        id: Long,
        newThumbnail: String,
        successMessage: String,
    ): Flow<LocalResource<String>>

    fun updateDownloadState(
        id: Long,
        downloadState: Int,
        successMessage: String,
    ): Flow<LocalResource<String>>

    /** Put the playlist on the YouTube account, then delete it here. Answers the new playlist's id. */
    suspend fun moveLocalPlaylistToYouTube(id: Long): Result<String>

    fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
        successMessage: String,
        updatedYtMessage: String,
        errorMessage: String,
    ): Flow<LocalResource<String>>

    fun getSuggestionsTrackForPlaylist(id: Long): Flow<LocalResource<Pair<String?, List<Track>>>>

    fun reloadSuggestionPlaylist(reloadParams: String): Flow<LocalResource<Pair<String?, List<Track>>>>

    fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<List<SetVideoIdEntity>>

    fun addYouTubePlaylistItem(
        youtubePlaylistId: String,
        videoId: String,
    ): Flow<LocalResource<String>>
}