package com.simohypers.limusic.domain.data.model.lyrics

/** The votes on the lyric Limusic Lyrics has live for a song, this install's own among them. */
data class LimusicRating(
    val revision: Long,
    val up: Int,
    val down: Int,
    /** -1, 0 or 1. */
    val myVote: Int,
) {
    /** The counts as they will read once the service has [value], shown before it answers. */
    fun withVote(value: Int): LimusicRating =
        copy(
            up = (up - (if (myVote == 1) 1 else 0) + (if (value == 1) 1 else 0)).coerceAtLeast(0),
            down = (down - (if (myVote == -1) 1 else 0) + (if (value == -1) 1 else 0)).coerceAtLeast(0),
            myVote = value,
        )

    /** What a tap on a thumb asks for: the same thumb again takes the vote back. */
    fun next(clicked: Int): Int = if (myVote == clicked) 0 else clicked
}

/**
 * A refusal from Limusic Lyrics, by the service's error code (`own`, `not_found`, `rate_limited`,
 * `banned`, `unauthorized`...), or `unavailable` when it couldn't be reached.
 */
class LimusicException(
    val code: String,
) : Exception(code)

/** The reasons Limusic Lyrics takes a report for, in the order the report form lists them. */
val LIMUSIC_REPORT_REASONS = listOf("timing", "text", "wrong_song", "abuse")

/** The code for "Limusic Lyrics couldn't be reached". */
const val LIMUSIC_UNAVAILABLE = "unavailable"

/** Where a contributor's public profile is, on the website. Handles are `[A-Za-z0-9_]`. */
fun limusicProfileUrl(handle: String) = "https://limusic.app/lyrics/?u=$handle"
