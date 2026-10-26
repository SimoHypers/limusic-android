package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.YourYouTubePlaylistList
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.utils.PlaylistSort
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface PlaylistRepository {
    fun getAllPlaylists(limit: Int): Flow<List<PlaylistEntity>>

    fun getPlaylist(id: String): Flow<PlaylistEntity?>

    fun getLikedPlaylists(): Flow<List<PlaylistEntity>>

    suspend fun insertPlaylist(playlistEntity: PlaylistEntity)

    suspend fun insertAndReplacePlaylist(playlistEntity: PlaylistEntity)

    suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity)

    /** Save to library: on YouTube Music too while signed in, for Sync while signed out. False when YouTube refused. */
    suspend fun updatePlaylistLiked(
        playlistId: String,
        likeStatus: Int,
    ): Boolean

    /** Signed in, copy what the playlist page says about the account's library onto the local row. */
    suspend fun mirrorPlaylistSaved(
        playlistId: String,
        saved: Boolean,
    )

    suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    )

    suspend fun updatePlaylistDownloadState(
        playlistId: String,
        downloadState: Int,
    )

    fun getAllDownloadedPlaylist(): Flow<List<PlaylistType>>

    fun getRadio(
        radioId: String,
        defaultDescription: String,
        radioString: String,
        viewString: String,
        originalTrack: SongEntity? = null,
        artist: ArtistEntity? = null,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>>

    fun getRDATRadioData(
        radioId: String,
        viewString: String,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>>

    fun getFullPlaylistData(
        playlistId: String,
        viewString: String,
    ): Flow<Resource<PlaylistBrowse>>

    /** [sort] asks YouTube for that order; null gets the order the account already has it in. */
    fun getPlaylistData(
        playlistId: String,
        viewString: String,
        sort: PlaylistSort? = null,
        desc: Boolean = false,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>>

    /** Stores [sort] on a playlist you own, for every client on the account. False if it failed. */
    suspend fun setPlaylistSort(
        playlistId: String,
        sort: PlaylistSort,
    ): Boolean

    fun getLibraryPlaylist(): Flow<List<PlaylistsResult>?>

    /** A new, empty playlist on the YouTube account. Its id, or null when YouTube refused. */
    suspend fun createYouTubePlaylist(title: String): String?

    fun getMixedForYou(): Flow<List<PlaylistsResult>?>

    /** Edits a playlist you own. A null field is left as it is; [cover] is a square JPEG/PNG. */
    fun editYourYouTubePlaylist(
        playlistId: String,
        title: String?,
        description: String?,
        privacy: String?,
        cover: ByteArray?,
    ): Flow<Resource<String>>

    fun deleteYourYouTubePlaylist(playlistId: String): Flow<Resource<String>>

    suspend fun insertYourYouTubePlaylist(yourYouTubePlaylist: YourYouTubePlaylistList)

    /**
     * @param emailPageId = $email_$pageId
     */
    fun getYourYouTubePlaylistList(emailPageId: String): Flow<YourYouTubePlaylistList?>

    suspend fun deleteAllYourYouTubePlaylist()

    /**
     * @return Country Code -> YouTube Music Playlist ID
     */
}