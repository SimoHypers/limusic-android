package com.simohypers.limusic

import com.simohypers.limusic.domain.repository.DeviceLinkRepository
import android.Manifest
import kotlinx.coroutines.flow.map
import com.simohypers.limusic.domain.repository.ArtistRepository
import com.simohypers.limusic.domain.mediaservice.handler.DownloadHandler
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eygraber.uri.toKmpUriOrNull
import com.simohypers.limusic.common.FIRST_TIME_MIGRATION
import com.simohypers.limusic.common.SELECTED_LANGUAGE
import com.simohypers.limusic.common.STATUS_DONE
import com.simohypers.limusic.common.SUPPORTED_LANGUAGE
import com.simohypers.limusic.common.SUPPORTED_LOCATION
import com.simohypers.limusic.domain.data.model.intent.GenericIntent
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.mediaservice.handler.MediaPlayerHandler
import com.simohypers.limusic.domain.mediaservice.handler.ToastType
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.media3.di.setServiceActivitySession
import com.simohypers.limusic.di.viewModelModule
import com.simohypers.limusic.service.test.notification.NotifyWork
import com.simohypers.limusic.service.update.UpdateCheckWorker
import com.simohypers.limusic.utils.ComposeResUtils
import com.simohypers.limusic.viewModel.SharedViewModel
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import com.simohypers.limusic.crashlytics.pushPlayerError
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    val viewModel: SharedViewModel by inject()
    val mediaPlayerHandler by inject<MediaPlayerHandler>()
    val dataStoreManager: DataStoreManager by inject()

    /** The device link's socket is open while paired and this activity is started (or playing). */
    private val deviceLink: DeviceLinkRepository by inject()
    private val downloadHandler: DownloadHandler by inject()
    private val artistRepository: ArtistRepository by inject()

    private var mBound = false
    private var shouldUnbind = false
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
//                mediaPlayerHandler.setActivitySession(this@MainActivity, MainActivity::class.java, service)
                setServiceActivitySession(this@MainActivity, MainActivity::class.java, service)
                Logger.d("MainActivity", "onServiceConnected: ")
                mBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Logger.d("MainActivity", "onServiceDisconnected: ")
                mBound = false
            }
        }

    override fun onStart() {
        super.onStart()
        startMusicService()
        deviceLink.setForeground(true)
        viewModel.syncLikedMusic()
    }

    override fun onStop() {
        super.onStop()
        deviceLink.setForeground(false)
        if (shouldUnbind) {
            unbindService(serviceConnection)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("MainActivity", "onNewIntent: $intent")
        viewModel.setIntent(
            GenericIntent(
                action = intent.action,
                data = (intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri())?.toKmpUriOrNull(),
                type = intent.type,
            ),
        )
    }

    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadKoinModules(
            module {
                single<AppCompatActivity> { this@MainActivity }
            },
        )
        // Recreate view model to fix the issue of view model not getting data from the service
        unloadKoinModules(viewModelModule)
        loadKoinModules(viewModelModule)
        if (viewModel.recreateActivity.value || viewModel.isServiceRunning) {
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
        Logger.d("MainActivity", "onCreate: ")
        val data = (intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri())?.toKmpUriOrNull()
        if (data != null) {
            viewModel.setIntent(
                GenericIntent(
                    action = intent.action,
                    data = data,
                    type = intent.type,
                ),
            )
        }
        Logger.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Logger.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Logger.d(
                    "Contains",
                    "onCreate: ${
                        SUPPORTED_LANGUAGE.codes.contains(
                            Locale.getDefault().toLanguageTag(),
                        )
                    }",
                )
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
            }
            // The content region follows the phone's country whatever its language, so an en-GB phone
            // gets GB rather than the stored default.
            putString(
                "location",
                Locale.getDefault().country.takeIf { it in SUPPORTED_LOCATION.items } ?: "US",
            )
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                Logger.d("Locale Key", "getString: $it")
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            getString(
                SELECTED_LANGUAGE,
            )
        ) {
            Logger.d(
                "Locale Key",
                "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}",
            )
            putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }

        enableEdgeToEdge(
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
        )
        viewModel.checkIsRestoring()
        val request =
            PeriodicWorkRequestBuilder<NotifyWork>(
                12L,
                TimeUnit.HOURS,
            ).addTag("Worker Test")
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "Artist Worker",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        UpdateCheckWorker.schedule(this)
        viewModel.getLocation()

        setContent {
            App(viewModel)
            AskForNotificationsWhenNeeded()
        }
    }

    /**
     * Android's notification prompt, asked the first time something needs it rather than at launch:
     * the first download (its progress) and the first followed artist (new-release alerts). The
     * player's notification is a media session's, which Android exempts. Once per reason, and never
     * for an install an older version already asked at launch.
     */
    @Composable
    private fun AskForNotificationsWhenNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        val downloading by remember { downloadHandler.queue.map { it.remaining > 0 } }.collectAsState(false)
        val following by remember { artistRepository.getFollowedArtists().map { it.isNotEmpty() } }.collectAsState(false)
        for ((reason, needed) in listOf("download" to downloading, "follow" to following)) {
            LaunchedEffect(reason, needed) {
                if (!needed || getString("notification_permission_asked") == "true") return@LaunchedEffect
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return@LaunchedEffect
                val key = "notification_asked_$reason"
                if (getString(key) == "true") return@LaunchedEffect
                putString(key, "true")
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        val shouldStopMusicService = viewModel.shouldStopMusicService()
        Logger.d("MainActivity", "onDestroy: Should stop service $shouldStopMusicService")

        // Always unbind service if it was bound to prevent MusicBinder leak
        if (shouldStopMusicService && shouldUnbind && isFinishing) {
            viewModel.isServiceRunning = false
        }
        // No unloadKoinModules here: onCreate already reloads the module, and a launcher shortcut
        // starts the new activity before this one is destroyed, so unloading here would pull the
        // view models out from under the new one.
        super.onDestroy()
        Logger.d("MainActivity", "onDestroy: ")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.activityRecreate()
    }

    private fun startMusicService() {
//        mediaPlayerHandler.startMediaService(this, serviceConnection)
        com.simohypers.limusic.media3.di
            .startService(this@MainActivity, serviceConnection)
        mediaPlayerHandler.pushPlayerError = { it ->
            pushPlayerError(it)
        }
        mediaPlayerHandler.showToast = { type ->
            viewModel.makeToast(
                when (type) {
                    is ToastType.ExplicitContent -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.EXPLICIT_CONTENT_BLOCKED) }
                    }

                    is ToastType.PlayerError -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.TIME_OUT_ERROR, type.error) }
                    }

                    is ToastType.PlaybackRefused -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.PLAYBACK_REFUSED, type.reason) }
                    }

                    is ToastType.YouTubeWriteFailed -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.YOUTUBE_WRITE_FAILED) }
                    }

                    is ToastType.HostControlsPlayback -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.HOST_CONTROLS_PLAYBACK) }
                    }

                    is ToastType.SentToHost -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.ADDED_TO_QUEUE) }
                    }

                    is ToastType.GuestAdded -> {
                        runBlocking { ComposeResUtils.getResString(ComposeResUtils.StringType.GUEST_ADDED, type.username, type.title) }
                    }

                },
            )
        }
        viewModel.isServiceRunning = true
        shouldUnbind = true
        Logger.d("Service", "Service started")
    }

    private fun putString(
        key: String,
        value: String,
    ) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? = viewModel.getString(key)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.activityRecreate()
    }
}