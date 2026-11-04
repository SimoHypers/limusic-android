@file:OptIn(ExperimentalTime::class)

package com.simohypers.limusic.domain.data.model.browse.artist

import com.simohypers.limusic.domain.data.entities.ArtistMotionEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Apple Music's animated artist artwork, in the two cuts AM actually publishes.
 *
 * AM ships three keys but only two distinct videos: `motionArtistFullscreen16x9` and
 * `motionArtistWide16x9` point at the same master. There is no tall/portrait cut at all — unlike
 * the album artwork, which is where the 3:4 rendition the Spotify canvas slot was shaped for
 * comes from — so a square header takes [squareVideoUrl] and a banner-shaped one takes
 * [wideVideoUrl].
 */
data class ArtistMotion(
    val squareVideoUrl: String?,
    val squareThumbUrl: String?,
    val wideVideoUrl: String?,
    val wideThumbUrl: String?,
) {
    val hasVideo: Boolean get() = squareVideoUrl != null || wideVideoUrl != null
}

/**
 * Everything one Apple Music artist lookup answers with.
 *
 * The logo and the motion travel together because they arrive in the *same* response — asking for
 * them separately would double a pair of requests against an API that is not ours. They are
 * independently nullable because they are independently present: Billie Eilish has motion and no
 * name logo, Taylor Swift has a name logo and no motion.
 */
data class ArtistEditorial(
    val logo: ArtistLogo?,
    val motion: ArtistMotion?,
)

/** How long a lookup is trusted — positive or negative — before the catalog is asked again. */
private const val ARTIST_MOTION_TTL_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Whether the cached answer may still be used.
 *
 * It gates the NEGATIVE answer as much as the positive one: without an expiry, an artist Apple
 * Music animates after the first lookup would never get their video, because the row saying they
 * had none would be believed forever.
 */
fun ArtistMotionEntity.isFresh(): Boolean =
    fetchedAt >= NAME_MATCH_SINCE_MS && Clock.System.now().toEpochMilliseconds() - fetchedAt < ARTIST_MOTION_TTL_MS

// 2026-11-04 12:30 UTC, when the name check below shipped. Lookups before it took Apple Music's first
// hit whatever its name (a podcast got a stranger's logo), so none of them is trusted.
private const val NAME_MATCH_SINCE_MS = 1793795400000L

/**
 * Whether an Apple Music artist is the YouTube one: the same name, ignoring case, spaces and
 * punctuation. The search ranks by relevance, so its first hit for a name it does not carry (a
 * podcast, a small channel) is somebody else.
 */
fun sameArtistName(
    youTube: String,
    appleMusic: String?,
): Boolean {
    fun String.key() = lowercase().filter { it.isLetterOrDigit() }
    val key = youTube.key()
    return key.isNotEmpty() && key == appleMusic?.key()
}

/** Null when the row records that this artist has no animated artwork. */
fun ArtistMotionEntity.toArtistMotion(): ArtistMotion? =
    ArtistMotion(
        squareVideoUrl = squareVideoUrl,
        squareThumbUrl = squareThumbUrl,
        wideVideoUrl = wideVideoUrl,
        wideThumbUrl = wideThumbUrl,
    ).takeIf { it.hasVideo }

/** A null receiver is the negative answer, and is stored as a row with no urls on it. */
fun ArtistMotion?.toEntity(channelId: String): ArtistMotionEntity =
    ArtistMotionEntity(
        channelId = channelId,
        squareVideoUrl = this?.squareVideoUrl,
        squareThumbUrl = this?.squareThumbUrl,
        wideVideoUrl = this?.wideVideoUrl,
        wideThumbUrl = this?.wideThumbUrl,
        fetchedAt = Clock.System.now().toEpochMilliseconds(),
    )
