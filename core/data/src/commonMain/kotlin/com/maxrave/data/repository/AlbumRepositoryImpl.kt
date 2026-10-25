package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.extension.getFullDataFromDB
import com.maxrave.data.mapping.toAlbumsResult
import com.maxrave.data.parser.parseAlbumData
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.FollowedArtistSingleAndAlbum
import com.maxrave.domain.data.model.browse.album.AlbumBrowse
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime

private const val TAG = "AlbumRepositoryImpl"

internal class AlbumRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val librarySync: LibrarySync,
) : AlbumRepository {
    override fun getAllAlbums(limit: Int): Flow<List<AlbumEntity>> =
        flow {
            emit(localDataSource.getAllAlbums(limit))
        }.flowOn(Dispatchers.IO)

    override fun getAlbum(id: String): Flow<AlbumEntity?> =
        flow {
            emit(localDataSource.getAlbum(id))
        }.flowOn(Dispatchers.IO)

    override fun getAlbumAsFlow(id: String) = localDataSource.getAlbumAsFlow(id)

    override fun getLikedAlbums(): Flow<List<AlbumEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getLikedAlbums(
                        limit,
                        offset,
                    )
                },
            )
        }.flowOn(Dispatchers.IO)

    override fun insertAlbum(albumEntity: AlbumEntity) =
        flow {
            emit(localDataSource.insertAlbum(albumEntity))
        }.flowOn(Dispatchers.IO)

    override suspend fun updateAlbumLiked(
        albumId: String,
        likeStatus: Int,
    ): Boolean =
        withContext(Dispatchers.Main) {
            val album = localDataSource.getAlbum(albumId)
            localDataSource.updateAlbumLiked(likeStatus, albumId)
            val key = LibrarySync.album(albumId)
            if (!librarySync.loggedIn()) {
                librarySync.mark(key, likeStatus == 1)
                return@withContext true
            }
            val written =
                album?.audioPlaylistId?.takeIf { it.isNotEmpty() }?.let { playlistId ->
                    withContext(Dispatchers.IO) { youTube.likePlaylist(playlistId, likeStatus == 1) }
                }
            if (written?.isSuccess != true) {
                Logger.w(TAG, "Album save on YouTube failed: ${written?.exceptionOrNull()?.message ?: "no audio playlist"}")
                localDataSource.updateAlbumLiked(if (album?.liked == true) 1 else 0, albumId)
                return@withContext false
            }
            librarySync.mark(key, false)
            true
        }

    override suspend fun getLibraryAlbums(): List<AlbumsResult>? =
        withContext(Dispatchers.IO) {
            youTube.wholeLibrary(LIBRARY_ALBUMS)?.cards?.mapNotNull { it.toLibraryAlbum() }?.distinctBy { it.browseId }
        }

    override suspend fun mirrorAlbumSaved(
        albumId: String,
        saved: Boolean,
    ) = withContext(Dispatchers.Main) {
        if (!librarySync.loggedIn() || librarySync.isUnsynced(LibrarySync.album(albumId))) return@withContext
        val album = localDataSource.getAlbum(albumId) ?: return@withContext
        if (album.liked != saved) localDataSource.updateAlbumLiked(if (saved) 1 else 0, albumId)
    }

    override suspend fun updateAlbumInLibrary(
        inLibrary: LocalDateTime,
        albumId: String,
    ) = withContext(
        Dispatchers.Main,
    ) { localDataSource.updateAlbumInLibrary(inLibrary, albumId) }

    override suspend fun updateAlbumDownloadState(
        albumId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateAlbumDownloadState(
            downloadState,
            albumId,
        )
    }

    override suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum) =
        withContext(Dispatchers.IO) {
            localDataSource.insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum)
        }

    override suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteFollowedArtistSingleAndAlbum(channelId)
        }

    override suspend fun getAllFollowedArtistSingleAndAlbums(): Flow<List<FollowedArtistSingleAndAlbum>?> =
        flow {
            val list =
                getFullDataFromDB { limit, offset ->
                    localDataSource.getAllFollowedArtistSingleAndAlbums(limit, offset)
                }
            emit(list)
        }.flowOn(Dispatchers.IO)

    override suspend fun getFollowedArtistSingleAndAlbum(channelId: String): Flow<FollowedArtistSingleAndAlbum?> =
        flow {
            emit(localDataSource.getFollowedArtistSingleAndAlbum(channelId))
        }.flowOn(Dispatchers.IO)

    override fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>> =
        flow {
            runCatching {
                youTube
                    .album(browseId, withSongs = true)
                    .onSuccess { result ->
                        emit(Resource.Success(parseAlbumData(result)))
                    }.onFailure { e ->
                        Logger.d(TAG, "getAlbumData -> error: ${e.message}")
                        emit(Resource.Error(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getAlbumMore(
        browseId: String,
        params: String,
    ): Flow<Pair<String, List<AlbumsResult>>?> =
        flow {
            runCatching {
                youTube
                    .browse(browseId = browseId, params = params)
                    .onSuccess { data ->
                        Logger.d(TAG, "getAlbumMore -> result: $data")
                        val items =
                            (data.items.firstOrNull()?.items ?: emptyList()).mapNotNull { item ->
                                item as? AlbumItem
                            }
                        emit(
                            (data.title ?: "") to (
                                items.map {
                                    it.toAlbumsResult()
                                }
                            ),
                        )
                    }.onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
            }
        }.flowOn(Dispatchers.IO)
}