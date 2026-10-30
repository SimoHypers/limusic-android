package com.simohypers.limusic.domain.mediaservice.handler

import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.download.DownloadQueue
import kotlinx.coroutines.flow.StateFlow

/**
 * The song rows are the only download state the UI reads: the handler writes each song's
 * `downloadState` as its download moves, and a playlist or album is downloaded once the user asked
 * for it (its own `downloadState`), however many of its songs have landed so far.
 */
interface DownloadHandler {
    /** Queues the songs that are neither downloaded nor in flight, as one batch. */
    suspend fun downloadTracks(songs: List<SongEntity>)

    fun removeDownload(videoId: String)

    /**
     * Removes these songs' downloads, except the ones a downloaded playlist, album or local
     * playlist still holds. Clear the collection's own state first, or it keeps them all.
     */
    suspend fun releaseDownloads(videoIds: List<String>)

    fun removeAllDownloads()

    val queue: StateFlow<DownloadQueue>

    /** A pause lasts until resumed, or until the app's process ends. */
    fun setPaused(paused: Boolean)

    /** Restarts what an earlier run of the app left queued. Call it with the app in front. */
    fun resumeLeftovers()
}
