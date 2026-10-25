package com.maxrave.media3.service.download

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.download.DownloadQueue
import com.maxrave.domain.data.player.PlaybackRefusals
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.logger.Logger
import com.maxrave.media3.R
import com.maxrave.media3.extension.isFullyCached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.util.concurrent.Executor

@UnstableApi
internal class DownloadUtils(
    private val context: Context,
    private val playerCache: SimpleCache,
    private val downloadCache: SimpleCache,
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val songRepository: SongRepository,
    databaseProvider: DatabaseProvider,
) : DownloadHandler {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val resolveLock = Mutex()

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient
                            .Builder()
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            Logger.d("Stream", mediaId)
            Logger.d("Stream", mediaId.startsWith(MERGING_DATA_TYPE.VIDEO).toString())
            // Already downloaded in full: hand the DataSpec back untouched so the enclosing
            // CacheDataSource reads straight off disk. Safe precisely because the whole track
            // is there — it never needs the upstream, which is the only thing the bare media id
            // in the URI cannot survive. Re-resolving here would re-fetch a track the user
            // already has.
            if (downloadCache.isFullyCached(mediaId, dataSpec.position)) {
                Logger.d("Stream", "Already downloaded $mediaId")
                return@Factory dataSpec
            }
            // Anything short of a full copy must resolve a real URL, and must never hand back
            // the incoming DataSpec: its URI is the bare media id. Skipping the bytes already on
            // disk is not this resolver's job — CacheWriter does that via cache.getCachedLength(),
            // and the CacheDataSource wrapping this resolver still serves whatever playerCache
            // holds.
            //
            // The upstream here is OkHttpDataSource (not DefaultDataSource, as on the playback
            // side), so a scheme-less URI dies in HttpUrl.parse as
            // HttpDataSourceException("Malformed URL", ERROR_CODE_FAILED_RUNTIME_CHECK) — a
            // misleading error for what is really "we could not get a stream URL". Fail loudly
            // instead, and let DownloadManager mark the download failed for the true reason.
            // Refused once this run: the downloader's retries would only ask again for the same no.
            PlaybackRefusals.reasonFor(mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO))?.let {
                throw java.io.IOException("YouTube refused $mediaId: $it")
            }
            var dataSpecReturn: DataSpec = dataSpec
            var resolved = false
            runBlocking(Dispatchers.IO) {
                // One lookup at a time: three downloads in flight never mean three /player calls at once.
                resolveLock.withLock {
                    if (mediaId.contains(MERGING_DATA_TYPE.VIDEO)) {
                        val id = mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                        streamRepository.getNewFormat(id).lastOrNull()?.let {
                            val videoUrl = it.videoUrl
                            if (videoUrl != null && it.expiredTime > now()) {
                                Logger.d("Stream", videoUrl)
                                Logger.d("Stream", "Video from format")
                                val is403Url = streamRepository.is403Url(videoUrl).firstOrNull() != false
                                if (!is403Url) {
                                    dataSpecReturn = dataSpec.withUri(videoUrl.toUri())
                                    resolved = true
                                    return@withLock
                                }
                            }
                        }
                        streamRepository
                            .getStream(
                                dataStoreManager,
                                id,
                                isDownloading = true,
                                true,
                            ).lastOrNull()
                            ?.let {
                                dataSpecReturn = dataSpec.withUri(it.toUri())
                                resolved = true
                            }
                    } else {
                        streamRepository.getNewFormat(mediaId).lastOrNull()?.let {
                            val audioUrl = it.audioUrl
                            if (audioUrl != null && it.expiredTime > now()) {
                                Logger.d("Stream", audioUrl)
                                Logger.d("Stream", "Audio from format")
                                val is403Url = streamRepository.is403Url(audioUrl).firstOrNull() != false
                                if (!is403Url) {
                                    dataSpecReturn = dataSpec.withUri(audioUrl.toUri())
                                    resolved = true
                                    return@withLock
                                }
                            }
                        }
                        streamRepository
                            .getStream(
                                dataStoreManager,
                                mediaId,
                                isDownloading = true,
                                isVideo = false,
                            ).lastOrNull()
                            ?.let {
                                if (it.contains("MPD")) {
                                    DashManifestParser().parse(
                                        it.toUri(),
                                        ByteArrayInputStream(it.toByteArray()),
                                    )
                                }
                                dataSpecReturn = dataSpec.withUri(it.toUri())
                                resolved = true
                            }
                    }
                }
            }
            if (!resolved) {
                Logger.e("Stream", "Failed to resolve download stream URL for $mediaId")
                throw java.io.IOException("Failed to resolve stream URL for $mediaId")
            }
            return@Factory dataSpecReturn
        }
    val downloadNotificationHelper =
        DownloadNotificationHelper(
            context,
            MusicDownloadService.Companion.CHANNEL_ID,
        )
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run),
        ).apply {
            maxParallelDownloads = 3
            minRetryCount = 3
        }

    // Main thread only, like every DownloadManager callback: request id (video parts carry
    // MERGING_DATA_TYPE.VIDEO) to its Media3 state.
    private val states = HashMap<String, Int>()

    // Song states waiting for the next batched write.
    private val pending = HashMap<String, Int>()
    private var flushJob: Job? = null

    private val _queue = MutableStateFlow(DownloadQueue())
    override val queue: StateFlow<DownloadQueue> = _queue.asStateFlow()

    /** The video stream is fetched only while "play video instead of audio" is on: playback merges it in for nothing else. */
    override suspend fun downloadTracks(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        // A download's state lives on its song row, and a playlist page may not have saved every one.
        songRepository.insertSongs(songs)
        val wanted =
            withContext(Dispatchers.Main) {
                songs.distinctBy { it.videoId }.filter {
                    songState(it.videoId) == DownloadState.STATE_NOT_DOWNLOADED && PlaybackRefusals.reasonFor(it.videoId) == null
                }
            }
        if (wanted.isEmpty()) return
        val withVideo = dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.Values.TRUE
        val requests =
            wanted.flatMap { song ->
                listOfNotNull(
                    request(song.videoId, song.title),
                    if (withVideo && isVideo(song.thumbnails)) request(MERGING_DATA_TYPE.VIDEO + song.videoId, "Video ${song.title}") else null,
                )
            }
        withContext(Dispatchers.Main) { requests.forEach(downloadManager::addDownload) }
        // Added straight to the manager; the service only has to be up to keep them going in the background.
        DownloadService.start(context, MusicDownloadService::class.java)
    }

    private fun request(
        id: String,
        title: String,
    ) = DownloadRequest
        .Builder(id, id.toUri())
        .setData(title.toByteArray())
        .setCustomCacheKey(id)
        .build()

    // Videos have wide covers, songs square ones.
    private suspend fun isVideo(thumbnail: String?): Boolean {
        if (thumbnail.isNullOrEmpty()) return false
        val image =
            SingletonImageLoader
                .get(context)
                .execute(
                    ImageRequest
                        .Builder(context)
                        .data(thumbnail)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                ).image ?: return false
        return image.height != image.width
    }

    override fun removeDownload(videoId: String) {
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            videoId,
            false,
        )
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            MERGING_DATA_TYPE.VIDEO + videoId,
            false,
        )
    }

    override suspend fun releaseDownloads(videoIds: List<String>) {
        val kept = songRepository.keptVideoIds()
        val released = videoIds.filterNot { it in kept }
        withContext(Dispatchers.Main) {
            released.forEach {
                downloadManager.removeDownload(it)
                downloadManager.removeDownload(MERGING_DATA_TYPE.VIDEO + it)
            }
        }
    }

    override fun removeAllDownloads() {
        downloadManager.removeAllDownloads()
    }

    override fun setPaused(paused: Boolean) {
        if (paused) {
            DownloadService.sendPauseDownloads(context, MusicDownloadService::class.java, false)
        } else {
            DownloadService.sendResumeDownloads(context, MusicDownloadService::class.java, false)
        }
    }

    private fun updateQueue() {
        val remaining =
            states.count { (id, state) ->
                !id.startsWith(MERGING_DATA_TYPE.VIDEO) &&
                    state != Download.STATE_COMPLETED && state != Download.STATE_FAILED && state != Download.STATE_REMOVING
            }
        _queue.update { it.copy(remaining = remaining, paused = downloadManager.downloadsPaused) }
    }

    /** The song's state in [DownloadState] terms, from its audio download and its video part if any. */
    private fun songState(songId: String): Int {
        val audio = states[songId] ?: return DownloadState.STATE_NOT_DOWNLOADED
        val video = states[MERGING_DATA_TYPE.VIDEO + songId]
        return when {
            audio == Download.STATE_FAILED || video == Download.STATE_FAILED -> DownloadState.STATE_NOT_DOWNLOADED
            audio == Download.STATE_COMPLETED && (video == null || video == Download.STATE_COMPLETED) -> DownloadState.STATE_DOWNLOADED
            audio == Download.STATE_DOWNLOADING || video == Download.STATE_DOWNLOADING -> DownloadState.STATE_DOWNLOADING
            else -> DownloadState.STATE_PREPARING
        }
    }

    /**
     * Collects song states and writes them in batches. A 500-song playlist changes state about
     * 1500 times; one write each would make every open song list re-query 1500 times.
     */
    private fun write(songId: String) {
        pending[songId] = songState(songId)
        if (flushJob?.isActive == true) return
        flushJob =
            mainScope.launch {
                while (pending.isNotEmpty()) {
                    delay(500)
                    val batch = pending.toMap()
                    pending.clear()
                    batch.entries
                        .groupBy({ it.value }, { it.key })
                        .forEach { (state, ids) -> songRepository.updateDownloadStates(ids, state) }
                }
            }
    }

    // Once the queue runs dry, not when a pause idles it.
    private fun notifyFinished() {
        val (finished, failed, remaining) = queue.value
        if (remaining > 0 || finished + failed == 0) return
        val text =
            listOfNotNull(
                finished.takeIf { it > 0 }?.let { context.resources.getQuantityString(R.plurals.downloads_finished, it, it) },
                failed.takeIf { it > 0 }?.let { context.resources.getQuantityString(R.plurals.downloads_failed, it, it) },
            ).joinToString(" · ")
        val notification =
            if (finished > 0) {
                downloadNotificationHelper.buildDownloadCompletedNotification(context, R.drawable.baseline_downloaded, null, text)
            } else {
                downloadNotificationHelper.buildDownloadFailedNotification(context, R.drawable.baseline_error_outline_24, null, text)
            }
        NotificationUtil.setNotification(context, MusicDownloadService.NOTIFICATION_ID + 1, notification)
        _queue.update { it.copy(done = 0, failed = 0) }
    }

    init {
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) states[cursor.download.request.id] = cursor.download.state
        }
        updateQueue()
        val songIds = states.keys.filterNot { it.startsWith(MERGING_DATA_TYPE.VIDEO) }
        val bySong = songIds.groupBy { songState(it) }
        coroutineScope.launch {
            songRepository.reconcileDownloads(
                active = bySong[DownloadState.STATE_PREPARING].orEmpty() + bySong[DownloadState.STATE_DOWNLOADING].orEmpty(),
                completed = bySong[DownloadState.STATE_DOWNLOADED].orEmpty(),
            )
        }
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    val id = download.request.id
                    states[id] = download.state
                    val isAudio = !id.startsWith(MERGING_DATA_TYPE.VIDEO)
                    when (download.state) {
                        Download.STATE_COMPLETED -> {
                            coroutineScope.launch { playerCache.removeResource(id) }
                            if (isAudio) _queue.update { it.copy(done = it.done + 1) }
                        }
                        Download.STATE_FAILED -> if (isAudio) _queue.update { it.copy(failed = it.failed + 1) }
                    }
                    updateQueue()
                    write(id.removePrefix(MERGING_DATA_TYPE.VIDEO))
                }

                override fun onDownloadRemoved(
                    downloadManager: DownloadManager,
                    download: Download,
                ) {
                    val id = download.request.id
                    states.remove(id)
                    updateQueue()
                    write(id.removePrefix(MERGING_DATA_TYPE.VIDEO))
                }

                override fun onIdle(downloadManager: DownloadManager) = notifyFinished()

                override fun onDownloadsPausedChanged(
                    downloadManager: DownloadManager,
                    downloadsPaused: Boolean,
                ) = updateQueue()
            },
        )
    }
}
