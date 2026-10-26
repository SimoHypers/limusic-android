package com.maxrave.kotlinytmusicscraper.extractor

import com.maxrave.kotlinytmusicscraper.cipher.FaradayCipherEngine
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress

expect class Extractor() {
    fun init()

    fun logIn(cookie: String?)

    fun mergeAudioVideoDownload(filePath: String): DownloadProgress

    fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem,
    ): DownloadProgress

    /** Solves signature and `n` challenges on the device, for the stream chain in YouTube.player. */
    val cipher: FaradayCipherEngine

    /** BotGuard's tokens for WEB_REMIX, or null when they cannot be minted right now. */
    suspend fun webPoTokens(
        videoId: String,
        visitorData: String,
    ): WebPoTokens?

    /**
     * The HLS playlist of a video that is broadcasting live right now, or null when no extractor
     * produced one.
     */
    fun liveHlsUrl(videoId: String): String?
}
/** [session] goes in the `/player` body, [video] on the stream URL as `&pot=` (desktop Limusic's pair). */
data class WebPoTokens(
    val session: String,
    val video: String,
)
