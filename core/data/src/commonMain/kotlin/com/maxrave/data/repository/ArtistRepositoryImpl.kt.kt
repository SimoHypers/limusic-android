package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.extension.getFullDataFromDB
import com.maxrave.data.parser.parseArtistData
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.ArtistMotionEntity
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult

internal class ArtistRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val librarySync: LibrarySync,
) : ArtistRepository {
    override fun getAllArtists(limit: Int): Flow<List<ArtistEntity>> =
        flow {
            emit(localDataSource.getAllArtists(limit))
        }.flowOn(Dispatchers.IO)

    override fun getArtistById(id: String): Flow<ArtistEntity?> =
        flow {
            emit(localDataSource.getArtist(id))
        }.flowOn(Dispatchers.IO)

    override suspend fun insertArtist(artistEntity: ArtistEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertArtist(artistEntity)
        }

    override suspend fun updateArtistImage(
        channelId: String,
        thumbnail: String,
    ) = withContext(
        Dispatchers.Main,
    ) {
        localDataSource.updateArtistImage(
            channelId,
            thumbnail,
        )
    }

    override suspend fun updateArtistNameLogo(
        channelId: String,
        nameLogoUrl: String?,
        nameLogoColor: String?,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateArtistNameLogo(channelId, nameLogoUrl, nameLogoColor)
    }

    override suspend fun getArtistMotion(channelId: String): ArtistMotionEntity? =
        withContext(Dispatchers.IO) {
            localDataSource.getArtistMotion(channelId)
        }

    override suspend fun upsertArtistMotion(motion: ArtistMotionEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.upsertArtistMotion(motion)
        }

    /**
     * Unfollowing also drops what only existed to serve the follow.
     *
     * Flipping the flag was all this ever did, so an artist's notifications and their new-releases
     * tracking row survived every unfollow, and once the unfollowed `artist` row is itself swept by
     * `SongRepository.clearHistoryAndOrphanedSongs`, they have nothing left to point back at.
     */
    override suspend fun updateFollowedStatus(
        channelId: String,
        followedStatus: Int,
        subscribeChannelId: String?,
    ): Boolean =
        withContext(Dispatchers.Main) {
            val wasFollowed = localDataSource.getArtist(channelId)?.followed == true
            localDataSource.updateFollowed(followedStatus, channelId)
            val key = LibrarySync.artist(channelId)
            if (librarySync.loggedIn()) {
                // The subscribe button's channel is not always the browseId, and unsubscribing
                // the browseId is refused, so the artist page is asked when the caller has no id.
                val target =
                    subscribeChannelId
                        ?: withContext(Dispatchers.IO) { youTube.artist(channelId) }.getOrNull()?.subscribeChannelId
                        ?: channelId
                val written =
                    withContext(Dispatchers.IO) {
                        if (followedStatus == 1) youTube.subscribeChannel(target) else youTube.unsubscribeChannel(target)
                    }
                if (written.isFailure) {
                    Logger.w("ArtistRepositoryImpl", "Subscription on YouTube failed: ${written.exceptionOrNull()?.message}")
                    localDataSource.updateFollowed(if (wasFollowed) 1 else 0, channelId)
                    return@withContext false
                }
                librarySync.mark(key, false)
            } else {
                librarySync.mark(key, followedStatus == 1)
            }
            if (followedStatus == 0) dropFollowData(channelId)
            true
        }

    override suspend fun getLibraryArtists(subscriptions: Boolean): List<ArtistsResult>? =
        withContext(Dispatchers.IO) {
            val page = youTube.wholeLibrary(if (subscriptions) LIBRARY_SUBSCRIPTIONS else LIBRARY_ARTISTS) ?: return@withContext null
            (page.rows.mapNotNull { it.toLibraryArtist() } + page.cards.mapNotNull { it.toLibraryArtist() }).distinctBy { it.browseId }
        }

    override suspend fun mirrorArtistSubscribed(
        channelId: String,
        subscribed: Boolean,
    ) = withContext(Dispatchers.Main) {
        if (!librarySync.loggedIn() || librarySync.isUnsynced(LibrarySync.artist(channelId))) return@withContext
        val artist = localDataSource.getArtist(channelId) ?: return@withContext
        if (artist.followed == subscribed) return@withContext
        localDataSource.updateFollowed(if (subscribed) 1 else 0, channelId)
        if (!subscribed) dropFollowData(channelId)
    }

    private suspend fun dropFollowData(channelId: String) {
        localDataSource.deleteNotificationsByChannelId(channelId)
        localDataSource.deleteFollowedArtistSingleAndAlbum(channelId)
    }

    override fun getFollowedArtists(): Flow<List<ArtistEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getFollowedArtists(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateArtistInLibrary(
            inLibrary,
            channelId,
        )
    }

    override fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> =
        flow {
            runCatching {
                youTube
                    .artist(channelId)
                    .onSuccess { result ->
                        emit(Resource.Success<ArtistBrowse>(parseArtistData(result)))
                    }.onFailure { e ->
                        Logger.d("Artist", "Error: ${e.message}")
                        emit(Resource.Error<ArtistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)
}