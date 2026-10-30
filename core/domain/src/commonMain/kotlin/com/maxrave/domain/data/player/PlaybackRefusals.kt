package com.maxrave.domain.data.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    fun record(
        videoId: String,
        reason: String,
    ) = reasons.update { it + (videoId to reason) }

    fun clear(videoId: String) = reasons.update { it - videoId }

    fun reasonFor(videoId: String): String? = reasons.value[videoId]

    private val downloads = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Songs a download was refused for (Premium only, taken down), which the downloader keeps
     * across runs: such a refusal rarely lifts, and asking every launch is a request per song from
     * the listener's IP. Playback still asks each run; only a download retry forgets these.
     */
    val refusedDownloads: StateFlow<Set<String>> = downloads.asStateFlow()

    fun rememberDownloadRefusals(videoIds: Collection<String>) = downloads.update { it + videoIds }

    /** Forgets every refused download, this run's refusals of them included, so a retry asks again. Returns them. */
    fun forgetDownloadRefusals(): Set<String> {
        val forgotten = downloads.value
        downloads.update { it - forgotten }
        reasons.update { it - forgotten }
        return forgotten
    }

    /** Songs refused this run or remembered from a download: the ones a download count leaves out. */
    val refusedIds: Flow<Set<String>> = combine(reasons, downloads) { now, remembered -> now.keys + remembered }

    private val _blocked = MutableStateFlow<String?>(null)

    /**
     * Set when YouTube pushed back on the IP itself (a bot check, a rate limit), with what it said.
     * Downloads stop on it and ask nothing more until the user resumes them.
     */
    val blocked: StateFlow<String?> = _blocked.asStateFlow()

    /** What [blocked] holds for an HTTP 429, which comes without a sentence. */
    const val RATE_LIMITED = "Too many requests"

    fun block(reason: String) {
        _blocked.value = reason
    }

    /** The user resumed: forget the push-back, and the songs it was recorded against. */
    fun unblock() {
        val reason = _blocked.value ?: return
        _blocked.value = null
        reasons.update { it.filterValues { r -> r != reason } }
    }
}
