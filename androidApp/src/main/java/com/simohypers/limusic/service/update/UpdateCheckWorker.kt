package com.simohypers.limusic.service.update

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.simohypers.limusic.MainActivity
import com.simohypers.limusic.R
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.updater.Updater
import com.simohypers.limusic.utils.ComposeResUtils
import com.simohypers.limusic.utils.ComposeResUtils.StringType
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val TAG = "UpdateCheckWorker"
private const val CHANNEL_ID = "app_updates"
private const val NOTIFICATION_ID = 7001

/**
 * The update check for when Limusic is closed: every six hours while online, a notification for
 * each new version, once. An app in front has the banner, so it is left alone. The tap opens
 * limusic://update, which shows the banner even with the banner switch off.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val dataStoreManager: DataStoreManager by inject()

    override suspend fun doWork(): Result {
        if (dataStoreManager.updateNotification.first() == DataStoreManager.FALSE) return Result.success()
        val notifications = NotificationManagerCompat.from(applicationContext)
        if (!notifications.areNotificationsEnabled()) return Result.success()
        val process = ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState)
        if (process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) return Result.success()
        val update =
            runCatching { Updater.findUpdate() }
                .onFailure { Logger.w(TAG, "Update check failed: ${it.message}") }
                .getOrNull() ?: return Result.success()
        if (dataStoreManager.notifiedUpdate.first() == update.version) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    ComposeResUtils.getResString(StringType.UPDATE_NOTIFICATION_CHANNEL),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open =
            PendingIntent.getActivity(
                applicationContext,
                NOTIFICATION_ID,
                Intent(applicationContext, MainActivity::class.java)
                    .setData("limusic://update".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(ComposeResUtils.getResString(StringType.UPDATE_NOTIFICATION_TITLE, update.version))
                .setContentText(ComposeResUtils.getResString(StringType.UPDATE_NOTIFICATION_TEXT))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        try {
            notifications.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission taken away between the check above and here.
            return Result.success()
        }
        dataStoreManager.setNotifiedUpdate(update.version)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(6L, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("Update check", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
