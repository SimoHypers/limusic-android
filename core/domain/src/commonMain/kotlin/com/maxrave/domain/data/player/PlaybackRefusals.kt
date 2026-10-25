package com.maxrave.domain.data.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Videos YouTube's player refused in this run of the app, with the reason it gave ("Video
 * unavailable", a region block, a bot check). The stream repository records a refusal and clears
 * it on the next successful resolve; the stream resolver reads it to fail the track once instead
 * of letting the player retry an answer that will not change, which is a burst of requests from
 * the listener's IP for nothing.
 *
 * In memory on purpose, like [LiveStreamRegistry]: a refusal can lift, and the next run asks again.
 */
object PlaybackRefusals {
    private val reasons = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Video id to reason, for a download page saying why its missing songs are missing. */
    val all: StateFlow<Map<String, String>> = reasons.asStateFlow()

    fun record(
        videoId: String,
        reason: String,
    ) = reasons.update { it + (videoId to reason) }

    fun clear(videoId: String) = reasons.update { it - videoId }

    fun reasonFor(videoId: String): String? = reasons.value[videoId]
}
