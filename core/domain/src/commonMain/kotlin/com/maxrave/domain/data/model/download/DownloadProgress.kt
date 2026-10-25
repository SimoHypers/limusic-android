package com.maxrave.domain.data.model.download

data class DownloadProgress(
    val audioDownloadProgress: Float = 0f, // 0.0 - 1.0
    val videoDownloadProgress: Float = 0f, // 0.0 - 1.0
    val downloadSpeed: Int = 0, // kb/s
    val errorMessage: String = "",
    val isMerging: Boolean = false,
    val isError: Boolean = false,
    val isDone: Boolean = false,
) {
    companion object {
        fun failed(message: String) = DownloadProgress(0f, 0f, 0, message, isMerging = false, isError = true, isDone = false)

        val AUDIO_DONE = DownloadProgress(1f, 0f, 0, "", isMerging = false, isError = false, isDone = true)
        val VIDEO_DONE = DownloadProgress(1f, 1f, 0, "", isMerging = false, isError = false, isDone = true)
        val MERGING = DownloadProgress(1f, 1f, 0, "", isMerging = true, isError = false, isDone = false)
        val INIT = DownloadProgress(0f, 0f, 0, "", isMerging = false, isError = false, isDone = false)
    }
}
/** How many of a list's songs are on the phone, and how many are still queued or downloading. */
data class DownloadCount(
    val downloaded: Int = 0,
    val active: Int = 0,
)

/**
 * A playlist or album's download: [kept] once the user asked for it, and how far its [total] songs
 * got. [refused] counts the songs YouTube would not play this run, which no retry will fetch.
 */
data class CollectionDownload(
    val kept: Boolean = false,
    val total: Int = 0,
    val downloaded: Int = 0,
    val active: Int = 0,
    val refused: Int = 0,
)

/** What the downloader is working through: [done] and [failed] since the queue last ran dry, [remaining] still to go. */
data class DownloadQueue(
    val done: Int = 0,
    val failed: Int = 0,
    val remaining: Int = 0,
    val paused: Boolean = false,
)
