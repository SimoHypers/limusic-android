package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.FollowedArtistSingleAndAlbum
import com.maxrave.domain.data.model.browse.album.AlbumBrowse
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface AlbumRepository {
    fun getAllAlbums(limit: Int): Flow<List<AlbumEntity>>

    fun getAlbum(id: String): Flow<AlbumEntity?>

    fun getAlbumAsFlow(id: String): Flow<AlbumEntity?>

    fun getLikedAlbums(): Flow<List<AlbumEntity>>

    fun insertAlbum(albumEntity: AlbumEntity): Flow<Long>

    /**
     * Save to library. Signed in it saves on YouTube Music too (a like on the album's audio
     * playlist); signed out it waits for Sync. False when YouTube refused, local row put back.
     */
    suspend fun updateAlbumLiked(
        albumId: String,
        likeStatus: Int,
    ): Boolean

    /** Signed in, copy what the album page says about the account's library onto the local row. */
    suspend fun mirrorAlbumSaved(
        albumId: String,
        saved: Boolean,
    )

    suspend fun updateAlbumInLibrary(
        inLibrary: LocalDateTime,
        albumId: String,
    )

    suspend fun updateAlbumDownloadState(
        albumId: String,
        downloadState: Int,
    )

    suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum)

    suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String)

    suspend fun getAllFollowedArtistSingleAndAlbums(): Flow<List<FollowedArtistSingleAndAlbum>?>

    suspend fun getFollowedArtistSingleAndAlbum(channelId: String): Flow<FollowedArtistSingleAndAlbum?>

    fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>>

    fun getAlbumMore(
        browseId: String,
        params: String,
    ): Flow<Pair<String, List<AlbumsResult>>?>
}