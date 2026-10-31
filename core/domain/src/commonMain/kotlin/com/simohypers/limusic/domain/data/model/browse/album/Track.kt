package com.simohypers.limusic.domain.data.model.browse.album

import com.simohypers.limusic.domain.data.model.searchResult.songs.Album
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.data.model.searchResult.songs.FeedbackTokens
import com.simohypers.limusic.domain.data.model.searchResult.songs.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val album: Album?,
    val artists: List<Artist>?,
    val duration: String?,
    val durationSeconds: Int?,
    val isAvailable: Boolean,
    val isExplicit: Boolean,
    val likeStatus: String?,
    val thumbnails: List<Thumbnail>?,
    val title: String,
    val videoId: String,
    val videoType: String?,
    val category: String?,
    val feedbackTokens: FeedbackTokens?,
    val resultType: String?,
    val year: String? = null,
    /**
     * Localized view-count text ("432K views"), for the artist Videos shelf, or an artist's top
     * song's play count ("53M plays"). Carried here because
     * that shelf renders [Track]s; it used to ride in [videoType], which is why that column filled
     * up with view counts. Defaulted so existing persisted queue JSON still decodes.
     */
    val views: String? = null,
    /** Queued with Play next. Queue rows only, like the three below (desktop's SongItem flags). */
    val queued: Boolean = false,
    /** Queued with Add to queue. */
    val queuedEnd: Boolean = false,
    /** The album or playlist a queued block came from; shuffle keeps such a block together. */
    val queuedFrom: String? = null,
    /** Brought in by autoplay: drawn under its divider, and what switching Autoplay off removes. */
    val autoplay: Boolean = false,
    /** This row's entry in a YouTube playlist: what removes it, and what tells two copies apart. */
    val setVideoId: String? = null,
    /**
     * The same recording's other version, the music video for a song or the song for a video, when
     * YouTube paired them (signed in only). What the player's Song/Video switch plays.
     */
    val counterpart: Track? = null,
    /** Where this version's moments sit in [counterpart], sorted by [TwinSegment.startMs]. */
    val twinSegments: List<TwinSegment> = emptyList(),
)

/** [startMs] into one version of a recording is [twinStartMs] into the other, for [durationMs]. */
@Serializable
data class TwinSegment(
    val startMs: Long,
    val twinStartMs: Long,
    val durationMs: Long,
)