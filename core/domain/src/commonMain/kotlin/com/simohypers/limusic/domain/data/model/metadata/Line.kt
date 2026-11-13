package com.simohypers.limusic.domain.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Line(
    val endTimeMs: String,
    val startTimeMs: String,
    val syllables: List<String>? = null,
    val words: String,
    /** Which singer the line is (`v1`, `v2`; `v1000` is all of them). Limusic Lyrics only. */
    val agent: String? = null,
    /** The line's background vocals, timed like [words] and left out of it. Limusic Lyrics only. */
    val background: String? = null,
)