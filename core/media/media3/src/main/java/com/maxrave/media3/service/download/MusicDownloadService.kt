package com.maxrave.media3.service.download

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.maxrave.media3.R
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import org.koin.android.ext.android.inject

@UnstableApi
internal class MusicDownloadService :
    DownloadService(
        NOTIFICATION_ID,
        1000L,
        CHANNEL_ID,
        R.string.download,
        0,
    ) {
    private val downloadUtil: DownloadHandler by inject<DownloadHandler>()

    override fun getDownloadManager() = (downloadUtil as DownloadUtils).downloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    // Progress through the whole queue (Media3's own helper averages the songs in flight, so its
    // bar refills with every song), and the song downloading under it.
    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val queue = (downloadUtil as DownloadUtils).queue.value
        val total = queue.done + queue.failed + queue.remaining
        val percent = (queue.progress * 100).toInt()
        val current = downloads.firstOrNull { it.state == Download.STATE_DOWNLOADING }?.let { Util.fromUtf8Bytes(it.request.data) }
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.mono)
            .setContentTitle(getString(R.string.downloads_progress, queue.done, total) + " · $percent%")
            .setContentText(
                when {
                    queue.waitingForWifi -> getString(R.string.downloads_waiting_wifi)
                    queue.offline -> getString(R.string.downloads_waiting_network)
                    else -> current
                },
            ).setSubText(queue.failed.takeIf { it > 0 }?.let { resources.getQuantityString(R.plurals.downloads_failed, it, it) })
            .setProgress(100, percent, total == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "download"
        const val NOTIFICATION_ID = 1000
        const val JOB_ID = 1000
    }
}