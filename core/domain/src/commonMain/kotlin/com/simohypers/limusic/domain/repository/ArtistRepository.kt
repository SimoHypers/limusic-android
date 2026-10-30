package com.simohypers.limusic.domain.repository

import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.ArtistMotionEntity
import com.simohypers.limusic.domain.data.model.browse.artist.ArtistBrowse
import com.simohypers.limusic.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult

interface ArtistRepository {
    fun getAllArtists(limit: Int): Flow<List<ArtistEntity>>

    fun getArtistById(id: String): Flow<ArtistEntity?>

    suspend fun insertArtist(artistEntity: ArtistEntity)

    suspend fun updateArtistImage(
        channelId: String,
        thumbnail: String,
    )

    suspend fun updateArtistNameLogo(
        channelId: String,
        nameLogoUrl: String?,
        nameLogoColor: String?,
    )

    /**
     * The cached Apple Music animated artwork for this artist, or null when it has never been
     * looked up. A row whose urls are all null means "looked up, has none" — see
     * [com.simohypers.limusic.domain.data.entities.ArtistMotionEntity].
     */
    suspend fun getArtistMotion(channelId: String): ArtistMotionEntity?

    suspend fun upsertArtistMotion(motion: ArtistMotionEntity)

    /**
     * Follow. Signed in it is a YouTube subscription, kept locally too because new-release
     * notifications read the local follow; signed out it waits for Sync. [subscribeChannelId] is
     * the artist page's subscribe target, looked up when not given. False when YouTube refused,
     * with the local follow put back.
     */
    suspend fun updateFollowedStatus(
        channelId: String,
        followedStatus: Int,
        subscribeChannelId: String? = null,
    ): Boolean

    /**
     * The artists behind the songs in your library, or with [subscriptions] the ones you subscribe
     * to, paged to the end. Null when YouTube could not be asked.
     */
    suspend fun getLibraryArtists(subscriptions: Boolean): List<ArtistsResult>?

    /** Signed in, copy the artist page's subscription onto the local follow. */
    suspend fun mirrorArtistSubscribed(
        channelId: String,
        subscribed: Boolean,
    )

    fun getFollowedArtists(): Flow<List<ArtistEntity>>

    suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    )

    fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>>
}