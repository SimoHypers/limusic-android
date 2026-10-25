package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.ArtistMotionEntity
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

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
     * [com.maxrave.domain.data.entities.ArtistMotionEntity].
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