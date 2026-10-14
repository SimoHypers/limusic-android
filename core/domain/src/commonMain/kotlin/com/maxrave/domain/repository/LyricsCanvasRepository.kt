package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.LyricsEntity
import com.maxrave.domain.data.model.lyrics.LyricsSource
import com.maxrave.domain.data.model.browse.artist.ArtistEditorial
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface LyricsCanvasRepository {
    fun getSavedLyrics(videoId: String): Flow<LyricsEntity?>

    suspend fun insertLyrics(lyricsEntity: LyricsEntity)

    /**
     * The song's lyrics as Limusic finds them: cached, else asked down [order] (switched-off sources
     * skipped), the first synced answer winning over any plain one. Null when no source has any.
     */
    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Int,
        order: List<Pair<LyricsSource, Boolean>>,
    ): LyricsEntity?

    /**
     * Animated album artwork from the hidden AM catalog, returned as the same [CanvasResult] a
     * Spotify canvas produces and stored in the same columns. Needs no login.
     */
    fun getAMAnimatedArtwork(videoId: String): Flow<Resource<CanvasResult>>

    suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    )

    suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    )

    /**
     * Fetch everything the hidden catalog holds for an artist: the name-logo image with its
     * dominant color, and the animated artist artwork.
     *
     * One call, because both ride the same response — splitting them would double a pair of
     * requests against an API that is not ours. Either half may be null on a success.
     */
    fun getArtistEditorial(artistName: String): Flow<Resource<ArtistEditorial>>

}