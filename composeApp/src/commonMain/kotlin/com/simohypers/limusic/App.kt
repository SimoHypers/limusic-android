package com.simohypers.limusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.materialkolor.ktx.themeColorOrNull
import com.simohypers.limusic.expect.ui.toImageBitmap
import com.simohypers.limusic.expect.ui.toReadableBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.simohypers.limusic.common.LibraryChipType
import com.simohypers.limusic.domain.data.player.GenericMediaItem
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.domain.manager.DataStoreManager.Values.TRUE
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.expect.Orientation
import com.simohypers.limusic.expect.currentOrientation
import com.simohypers.limusic.extension.copy
import com.simohypers.limusic.ui.component.AppNavigationRail
import com.simohypers.limusic.ui.icon.ArrowForwardIos
import com.simohypers.limusic.ui.icon.SimpIcons
import com.simohypers.limusic.ui.navigation.destination.home.HomeDestination
import com.simohypers.limusic.ui.navigation.destination.home.NotificationDestination
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDestination
import com.simohypers.limusic.ui.navigation.destination.search.SearchDestination
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.library.MixForYouDestination
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.player.FullscreenDestination
import com.simohypers.limusic.ui.navigation.graph.AppNavigationGraph
import com.simohypers.limusic.ui.component.LimusicMiniPlayer
import com.simohypers.limusic.ui.component.LimusicNavigationBar
import com.simohypers.limusic.ui.screen.MiniPlayer
import com.simohypers.limusic.ui.screen.player.NowPlayingScreen
import com.simohypers.limusic.ui.screen.player.NowPlayingScreenContent
import com.simohypers.limusic.ui.theme.AppTheme
import com.simohypers.limusic.ui.theme.ForceDarkContent
import com.simohypers.limusic.ui.theme.desktopPanelDark
import com.simohypers.limusic.ui.theme.desktopWindowDark
import com.simohypers.limusic.ui.theme.desktopWindowLight
import com.simohypers.limusic.ui.theme.parseThemeColorHex
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.utils.isAppLinkAvailable
import com.simohypers.limusic.utils.libraryTabOf
import com.simohypers.limusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.do_not_show_again
import limusic.composeapp.generated.resources.good_night
import limusic.composeapp.generated.resources.notification
import limusic.composeapp.generated.resources.settings
import limusic.composeapp.generated.resources.sleep_timer_off
import limusic.composeapp.generated.resources.this_app_needs_to_access_your_notification
import limusic.composeapp.generated.resources.this_link_is_not_supported
import limusic.composeapp.generated.resources.yes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalFoundationApi::class)
@Composable
fun App(
    viewModel: SharedViewModel = koinInject(),
    showDesktopNotificationPermissionDialog: Boolean = false,
    onDismissDesktopNotificationPermissionDialog: (doNotShowAgain: Boolean) -> Unit = {},
    onOpenDesktopNotificationSettings: (doNotShowAgain: Boolean) -> Unit = {},
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val navController = rememberNavController()
    val isDesktopShell = getPlatform() == Platform.Desktop

    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val intent by viewModel.intent.collectAsStateWithLifecycle()
    val showNotificationPermissionDialog by viewModel.showNotificationPermissionDialog.collectAsStateWithLifecycle()

    // Mix for you comes from the signed-in YouTube account, so its tab follows the session — the
    // same condition that used to hide the chip inside Library.
    val isYouTubeLoggedIn by viewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val showMixForYouTab = isYouTubeLoggedIn == TRUE

    val themeMode by viewModel.getThemeMode().collectAsStateWithLifecycle(DataStoreManager.THEME_MODE_DARK)
    val themeColorSource by viewModel.getThemeColorSource().collectAsStateWithLifecycle(DataStoreManager.THEME_COLOR_DEFAULT)
    val customThemeColorHex by viewModel.getCustomThemeColor().collectAsStateWithLifecycle(DataStoreManager.DEFAULT_THEME_COLOR_HEX)
    val artworkColors by viewModel.getArtworkColors().collectAsStateWithLifecycle(DataStoreManager.TRUE)
    val artworkColor =
        rememberArtworkColor(
            url = (nowPlayingData?.songEntity?.thumbnails ?: nowPlayingData?.mediaItem?.metadata?.artworkUri).takeIf { artworkColors == TRUE },
        )
    // MiniPlayer visibility: derived, never stored.
    //
    // This used to be a rememberSaveable Boolean written by a LaunchedEffect. Two things went
    // wrong with that. The effect only runs AFTER the first composition, so the first frame drew
    // whatever the initial value said — and rememberSaveable RESTORES a previously saved value,
    // so flipping that initial value from true to false changed nothing on a process that had
    // already saved true. The bar therefore showed, hid, and showed again on every start.
    //
    // Reading it straight from nowPlayingData removes both failure modes: there is no first-frame
    // guess to be wrong, and no saved copy to disagree with the source.
    val isShowMiniPlayer by remember {
        derivedStateOf {
            val item = nowPlayingData?.mediaItem
            item != null && item != GenericMediaItem.EMPTY
        }
    }

    // Now playing screen
    var isShowNowPlaylistScreen by rememberSaveable {
        mutableStateOf(false)
    }

    // Fullscreen
    var isInFullscreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isNavBarVisible by rememberSaveable {
        mutableStateOf(true)
    }

    val hazeState =
        rememberHazeState()

    LaunchedEffect(intent) {
        val intent = intent ?: return@LaunchedEffect
        val data = intent.data
        Logger.d("MainActivity", "onCreate: $data")
        if (data != null) {
            if (data.scheme == "simpmusic" && data.host == "notification") {
                viewModel.setIntent(null)
                navController.navigate(
                    NotificationDestination,
                )
            } else if (data.scheme == "limusic" && data.host == "discord-auth") {
                // Discord's authorize page sends the user back here with ?code=&state= (or ?error=).
                viewModel.setIntent(null)
                viewModel.completeDiscordLogin(
                    data.getQueryParameter("code"),
                    data.getQueryParameter("state"),
                    data.getQueryParameter("error_description") ?: data.getQueryParameter("error"),
                )
            } else if (data.host == "simpmusic.org" || data.scheme == "simpmusic") {
                // https://simpmusic.org/app/watch?v=VIDEO_ID
                // https://simpmusic.org/app/playlist?list=PLAYLIST_ID
                // https://simpmusic.org/app/channel/CHANNEL_ID
                // simpmusic://watch?v=VIDEO_ID  (host="watch", no path)
                // simpmusic://playlist?list=PLAYLIST_ID
                // simpmusic://channel/CHANNEL_ID
                val segments = data.pathSegments
                // For simpmusic.org: segments = ["app", "watch"] → appPath = segments[1]
                // For simpmusic://: host IS the appPath (e.g. host="watch"), segments = []
                val appPath =
                    if (data.scheme == "simpmusic") {
                        data.host
                    } else {
                        segments.getOrNull(1)
                    }
                Logger.d("MainActivity", "simpmusic.org deep link, appPath: $appPath")
                // A path added to the when below must also go into APP_LINK_PATHS in AppLinks.kt.
                viewModel.setIntent(null)
                when (appPath) {
                    "watch" -> {
                        data.getQueryParameter("v")?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                    }

                    "playlist" -> {
                        data.getQueryParameter("list")?.let { playlistId ->
                            if (playlistId.startsWith("OLAK5uy_")) {
                                navController.navigate(AlbumDestination(browseId = playlistId))
                            } else if (playlistId.startsWith("VL")) {
                                navController.navigate(PlaylistDestination(playlistId = playlistId))
                            } else {
                                navController.navigate(PlaylistDestination(playlistId = "VL$playlistId"))
                            }
                        }
                    }

                    "channel", "c" -> {
                        // simpmusic://channel/UCxxx → segments = ["UCxxx"]
                        // simpmusic.org/app/channel/UCxxx → segments = ["app", "channel", "UCxxx"]
                        val artistId =
                            if (data.scheme == "simpmusic") {
                                segments.firstOrNull()
                            } else {
                                segments.getOrNull(2)
                            }
                        artistId?.let {
                            if (it.startsWith("UC")) {
                                navController.navigate(ArtistDestination(channelId = it))
                            } else {
                                viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                            }
                        }
                    }

                    "album" -> {
                        data.getQueryParameter("id")?.let { albumId ->
                            navController.navigate(AlbumDestination(browseId = albumId))
                        }
                    }

                    // simpmusic://search → the search screen (the launcher shortcut)
                    "search" -> {
                        navController.navigate(SearchDestination)
                    }

                    // simpmusic://library                     → the Library tab
                    // simpmusic://library?type=favorite       → one of its collections
                    // Added for the Playlists widget, whose shortcuts have to reach these
                    // screens from the home screen without the app already running.
                    "library" -> {
                        val type = data.getQueryParameter("type")
                        if (type.isNullOrBlank()) {
                            // simpmusic://library?tab=chart → the Library tab on one of its chips,
                            // named as LibraryChipType stores them.
                            val tab = libraryTabOf(data)
                            if (!isAppLinkAvailable(appPath)) {
                                viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                            } else {
                                viewModel.requestLibraryTab(tab)
                                navController.navigate(LibraryDestination)
                            }
                        } else {
                            navController.navigate(LibraryDynamicPlaylistDestination(type = type))
                        }
                    }

                    else -> {
                        viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                    }
                }
            } else {
                Logger.d("MainActivity", "onCreate: $data")
                when (val path = data.pathSegments.firstOrNull()) {
                    "playlist" -> {
                        data
                            .getQueryParameter("list")
                            ?.let { playlistId ->
                                viewModel.setIntent(null)
                                if (playlistId.startsWith("OLAK5uy_")) {
                                    navController.navigate(
                                        AlbumDestination(
                                            browseId = playlistId,
                                        ),
                                    )
                                } else if (playlistId.startsWith("VL")) {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = playlistId,
                                        ),
                                    )
                                } else {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = "VL$playlistId",
                                        ),
                                    )
                                }
                            }
                    }

                    "channel", "c" -> {
                        data.lastPathSegment?.let { artistId ->
                            if (artistId.startsWith("UC")) {
                                viewModel.setIntent(null)
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = artistId,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(
                                    getString(
                                        Res.string.this_link_is_not_supported,
                                    ),
                                )
                            }
                        }
                    }

                    else -> {
                        when {
                            path == "watch" -> data.getQueryParameter("v")
                            data.host == "youtu.be" -> path
                            else -> null
                        }?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                    }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        Logger.d("MainActivity", "Current destination: ${navBackStackEntry?.destination?.route}")
        if (navBackStackEntry?.destination?.route?.contains("FullscreenDestination") == true) {
            isShowNowPlaylistScreen = false
        }
        isInFullscreen = navBackStackEntry?.destination?.hierarchy?.any {
            it.hasRoute(FullscreenDestination::class)
        } == true
    }
    LaunchedEffect(showMixForYouTab) {
        // Same for signing out of YouTube: the Mix for you tab goes away, so nobody may be left
        // standing on a screen that has no mixes to show and no tab pointing at it.
        if (!showMixForYouTab &&
            navBackStackEntry?.destination?.hierarchy?.any {
                it.hasRoute(MixForYouDestination::class)
            } == true
        ) {
            navController.navigate(HomeDestination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    var isScrolledToTop by rememberSaveable {
        mutableStateOf(false)
    }
    val isTablet = windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isTabletLandscape = isTablet && currentOrientation() == Orientation.LANDSCAPE

    AppTheme(
        themeMode = themeMode,
        themeColorSource = themeColorSource,
        customThemeColor = parseThemeColorHex(customThemeColorHex),
        artworkColor = artworkColor,
    ) {
        // Read inside AppTheme so MaterialTheme reflects the resolved scheme (light background is #FFFFFF).
        val isLightScheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

        // The desktop shell is a window colour with panels floating on it. The two schemes mirror
        // each other: the window takes the extreme (pure black / pure white) and the panel steps
        // one shade back towards the middle, so the panels read as raised either way.
        val desktopWindow = if (isLightScheme) desktopWindowLight else desktopWindowDark
        val desktopPanel =
            if (isLightScheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        Scaffold(
            containerColor =
                if (isDesktopShell) desktopWindow else MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!isTablet) {
                    AnimatedVisibility(
                        isNavBarVisible,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut(),
                    ) {
                        // The mini player sits right above the tabs; both disappear together when a
                        // screen hides the bar (the fullscreen video player, for one).
                        Column {
                            AnimatedVisibility(
                                isShowMiniPlayer,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut(),
                            ) {
                                LimusicMiniPlayer(
                                    onOpen = { isShowNowPlaylistScreen = true },
                                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 6.dp),
                                )
                            }
                            LimusicNavigationBar(navController = navController) { klass ->
                                viewModel.reloadDestination(klass)
                            }
                        }
                    }
                }
            },
            content = { innerPadding ->
                Box(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxSize(),
                    ) {
                        if (isTablet && !isInFullscreen) {
                            AppNavigationRail(
                                navController = navController,
                                showMixForYouTab = showMixForYouTab,
                            ) { klass ->
                                viewModel.reloadDestination(klass)
                            }
                        }
                        // Desktop only: the content sits in its own rounded panel floating on a
                        // pure black window, Spotify style, while the rail stays flat black
                        // outside it. Phones keep one continuous surface — the inset only reads
                        // as deliberate when there is a window frame around it.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .then(
                                    if (isDesktopShell) {
                                        Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(desktopPanel)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .hazeSource(hazeState),
                            ) {
                                AppNavigationGraph(
                                    innerPadding = innerPadding,
                                    navController = navController,
                                    hideNavBar = {
                                        isNavBarVisible = false
                                    },
                                    showNavBar = {
                                        isNavBarVisible = true
                                    },
                                    showNowPlayingSheet = {
                                        isShowNowPlaylistScreen = true
                                    },
                                    onScrolling = {
                                        isScrolledToTop = it
                                    },
                                )
                            }
                            this@Row.AnimatedVisibility(
                                modifier =
                                    Modifier
                                        .padding(innerPadding)
                                        .align(Alignment.BottomCenter),
                                visible = isShowMiniPlayer && isTablet && !isInFullscreen,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut(),
                            ) {
                                MiniPlayer(
                                    if (getPlatform() == Platform.Android) {
                                        Modifier
                                            .height(60.dp)
                                            .fillMaxWidth(0.8f)
                                            .padding(
                                                horizontal = 12.dp,
                                            ).padding(
                                                bottom = 4.dp,
                                            )
                                    } else {
                                        // Floating capsule, Apple Music style: a fixed size so it never
                                        // stretches to the window width, and no haze at all — the capsule
                                        // paints its own liquid glass. Layering haze underneath blurs the
                                        // same pixels twice and reads as a dark smear, not glass.
                                        // padding BEFORE height: the other way round the bottom margin
                                        // eats into the 72dp and the capsule ends up 52dp tall.
                                        // 60dp is the floor for this layout: the content row is centred
                                        // on the capsule's axis and the 16dp progress box hangs off the
                                        // bottom, so the height has to cover the taller of the artwork
                                        // (32dp) and the two text lines (~33dp), plus that 16dp, plus a
                                        // gap. Going lower means shrinking the artwork again.
                                        Modifier
                                            .wrapContentWidth()
                                            .padding(bottom = 20.dp)
                                            .height(60.dp)
                                    },
                                    navController = navController,
                                    onClick = {
                                        isShowNowPlaylistScreen = true
                                    },
                                    onClose = {
                                        viewModel.stopPlayer()
                                        viewModel.isServiceRunning = false
                                    },
                                    // The page lives in the Now Playing panel, so the panel opens with it.
                                    onOpenFullscreenLyrics = {
                                        viewModel.requestFullscreenLyrics()
                                        isShowNowPlaylistScreen = true
                                    },
                                )
                            }
                        }
                        if (isTablet && isTabletLandscape && !isInFullscreen) {
                            AnimatedVisibility(
                                isShowNowPlaylistScreen,
                                enter = expandHorizontally() + fadeIn(),
                                exit = fadeOut() + shrinkHorizontally(),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.35f),
                                ) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .padding(
                                                innerPadding.copy(
                                                    start = 0.dp,
                                                    top = 0.dp,
                                                    bottom = 0.dp,
                                                ),
                                            ).then(
                                                // Matches the inset of the content panel so the two
                                                // read as a pair of floating cards, not one panel
                                                // with a seam down the middle.
                                                if (isDesktopShell) {
                                                    Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
                                                } else {
                                                    Modifier
                                                },
                                            ).clip(
                                                RoundedCornerShape(12.dp),
                                            ).then(
                                                if (isDesktopShell) {
                                                    Modifier.background(desktopPanel)
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    ) {
                                        ForceDarkContent {
                                            NowPlayingScreenContent(
                                                navController = navController,
                                                sharedViewModel = viewModel,
                                                isExpanded = true,
                                                dismissIcon = SimpIcons.ArrowForwardIos,
                                            ) {
                                                isShowNowPlaylistScreen = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isShowNowPlaylistScreen && !isTabletLandscape) {
                    ForceDarkContent {
                        NowPlayingScreen(
                            navController = navController,
                        ) {
                            isShowNowPlaylistScreen = false
                        }
                    }
                }

                if (sleepTimerState.isDone) {
                    Logger.d("MainActivity", "Sleep Timer Done: $sleepTimerState")
                    AlertDialog(
                        properties =
                            DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                            ),
                        onDismissRequest = {
                            viewModel.stopSleepTimer()
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.stopSleepTimer()
                            }) {
                                Text(
                                    stringResource(Res.string.yes),
                                    style = typo().bodySmall,
                                )
                            }
                        },
                        text = {
                            Text(
                                stringResource(Res.string.sleep_timer_off),
                                style = typo().labelSmall,
                            )
                        },
                        title = {
                            Text(
                                stringResource(Res.string.good_night),
                                style = typo().bodySmall,
                            )
                        },
                    )
                }

                if (showNotificationPermissionDialog || showDesktopNotificationPermissionDialog) {
                    var doNotShowAgain by remember { mutableStateOf(false) }
                    val dismissPermissionDialog = {
                        if (showDesktopNotificationPermissionDialog) {
                            onDismissDesktopNotificationPermissionDialog(doNotShowAgain)
                        } else {
                            viewModel.dismissNotificationPermissionDialog(doNotShowAgain)
                        }
                    }
                    AlertDialog(
                        onDismissRequest = {
                            dismissPermissionDialog()
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (showDesktopNotificationPermissionDialog) {
                                        onOpenDesktopNotificationSettings(doNotShowAgain)
                                    } else {
                                        viewModel.dismissNotificationPermissionDialog(doNotShowAgain)
                                    }
                                },
                            ) {
                                Text(
                                    stringResource(
                                        if (showDesktopNotificationPermissionDialog) {
                                            Res.string.settings
                                        } else {
                                            Res.string.yes
                                        },
                                    ),
                                    style = typo().bodySmall,
                                )
                            }
                        },
                        dismissButton =
                            if (showDesktopNotificationPermissionDialog) {
                                {
                                    TextButton(onClick = dismissPermissionDialog) {
                                        Text(
                                            stringResource(Res.string.cancel),
                                            style = typo().bodySmall,
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        title = {
                            Text(
                                stringResource(Res.string.notification),
                                style = typo().labelSmall,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    stringResource(Res.string.this_app_needs_to_access_your_notification),
                                    style = typo().bodySmall,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier
                                            .clickable { doNotShowAgain = !doNotShowAgain }
                                            .fillMaxWidth(),
                                ) {
                                    Checkbox(
                                        checked = doNotShowAgain,
                                        onCheckedChange = { doNotShowAgain = it },
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        stringResource(Res.string.do_not_show_again),
                                        style = typo().bodySmall,
                                    )
                                }
                            }
                        },
                    )
                }
            },
        )
    }
}

/**
 * The colour of the cover at [url], for "Adapt colors to artwork"; null when [url] is null. Picked
 * the way the M3 Expressive player seeds its scheme. A colourless cover, or one that fails to load,
 * keeps the previous colour instead of flashing back to the theme between tracks, as on desktop.
 * The mini player has already loaded the same cover, so this is a disk-cache read.
 */
@Composable
private fun rememberArtworkColor(url: String?): Color? {
    val context = LocalPlatformContext.current
    var color by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(url) {
        if (url == null) {
            color = null
            return@LaunchedEffect
        }
        val image =
            SingletonImageLoader
                .get(context)
                .execute(ImageRequest.Builder(context).data(url).size(ARTWORK_SEED_PX).build())
                .image ?: return@LaunchedEffect
        // ponytail: decoded on the track change, not one track early as desktop does; prewarm the
        // next cover if the colour visibly lands after the artwork.
        withContext(Dispatchers.Default) {
            runCatching { image.toImageBitmap().toReadableBitmap(ARTWORK_SEED_PX, ARTWORK_SEED_PX).themeColorOrNull() }.getOrNull()
        }?.let { color = it }
    }
    return color
}

private const val ARTWORK_SEED_PX = 112
