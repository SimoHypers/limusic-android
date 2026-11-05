package com.simohypers.limusic

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
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
import kotlinx.coroutines.delay
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
import com.simohypers.limusic.ui.component.BusyPill
import com.simohypers.limusic.ui.component.UpdateBanner
import com.simohypers.limusic.ui.icon.ArrowForwardIos
import com.simohypers.limusic.ui.icon.LimusicIcons
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
import com.simohypers.limusic.ui.component.navigateToTab
import com.simohypers.limusic.ui.screen.MiniPlayer
import com.simohypers.limusic.ui.screen.player.NowPlayingScreen
import com.simohypers.limusic.ui.screen.player.NowPlayingScreenContent
import com.simohypers.limusic.ui.theme.AppTheme
import com.simohypers.limusic.ui.theme.desktopPanelDark
import com.simohypers.limusic.ui.theme.desktopWindowDark
import com.simohypers.limusic.ui.theme.desktopWindowLight
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.updater.Updater
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
import limusic.composeapp.generated.resources.good_night
import limusic.composeapp.generated.resources.notification
import limusic.composeapp.generated.resources.sleep_timer_off
import limusic.composeapp.generated.resources.this_link_is_not_supported
import limusic.composeapp.generated.resources.yes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalFoundationApi::class)
@Composable
fun App(
    viewModel: SharedViewModel = koinInject(),
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val navController = rememberNavController()
    val isDesktopShell = getPlatform() == Platform.Desktop

    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val intent by viewModel.intent.collectAsStateWithLifecycle()

    // Mix for you comes from the signed-in YouTube account, so its tab follows the session — the
    // same condition that used to hide the chip inside Library.
    val isYouTubeLoggedIn by viewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val showMixForYouTab = isYouTubeLoggedIn == TRUE

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val artworkColors by viewModel.artworkColors.collectAsStateWithLifecycle()
    val artworkColor =
        rememberArtworkColor(
            url = nowPlayingData?.songEntity?.thumbnails ?: nowPlayingData?.mediaItem?.metadata?.artworkUri,
            enabled = artworkColors == TRUE,
            saved = viewModel.artworkColor,
            onColor = viewModel::setArtworkColor,
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
            if (data.scheme == "limusic" && data.host == "notification") {
                viewModel.setIntent(null)
                navController.navigate(
                    NotificationDestination,
                )
            } else if (data.scheme == "limusic" && data.host == "update") {
                // The update notification: show the banner, even with the banner switch off.
                viewModel.setIntent(null)
                runCatching { Updater.checkNow() }
            } else if (data.scheme == "limusic" && data.host == "discord-auth") {
                // Discord's authorize page sends the user back here with ?code=&state= (or ?error=).
                viewModel.setIntent(null)
                viewModel.completeDiscordLogin(
                    data.getQueryParameter("code"),
                    data.getQueryParameter("state"),
                    data.getQueryParameter("error_description") ?: data.getQueryParameter("error"),
                )
            } else if (data.scheme == "limusic") {
                // limusic://watch?v=VIDEO_ID  (host="watch", no path)
                // limusic://playlist?list=PLAYLIST_ID
                // limusic://channel/CHANNEL_ID
                // The host IS the appPath.
                val appPath = data.host
                Logger.d("MainActivity", "App link, appPath: $appPath")
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
                        // limusic://channel/UCxxx → segments = ["UCxxx"]
                        data.pathSegments.firstOrNull()?.let {
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

                    // limusic://search → the search screen (the launcher shortcut)
                    "search" -> {
                        navController.navigateToTab(SearchDestination)
                    }

                    // limusic://library                     → the Library tab
                    // limusic://library?type=favorite       → one of its collections
                    // Added for the Playlists widget, whose shortcuts have to reach these
                    // screens from the home screen without the app already running.
                    "library" -> {
                        val type = data.getQueryParameter("type")
                        if (type.isNullOrBlank()) {
                            // limusic://library?tab=chart → the Library tab on one of its chips,
                            // named as LibraryChipType stores them.
                            val tab = libraryTabOf(data)
                            if (!isAppLinkAvailable(appPath)) {
                                viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                            } else {
                                viewModel.requestLibraryTab(tab)
                                navController.navigateToTab(LibraryDestination)
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
            navController.navigateToTab(HomeDestination)
        }
    }
    var isScrolledToTop by rememberSaveable {
        mutableStateOf(false)
    }
    val isTablet = windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isTabletLandscape = isTablet && currentOrientation() == Orientation.LANDSCAPE

    AppTheme(
        themeMode = themeMode,
        appearance = appearance,
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
                                            .clip(rounded(12.dp))
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
                                                rounded(12.dp),
                                            ).then(
                                                if (isDesktopShell) {
                                                    Modifier.background(desktopPanel)
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    ) {
                                        NowPlayingScreenContent(
                                            navController = navController,
                                            sharedViewModel = viewModel,
                                            isExpanded = true,
                                            dismissIcon = LimusicIcons.ArrowForwardIos,
                                        ) {
                                            isShowNowPlaylistScreen = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                    BusyPill(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = innerPadding.calculateBottomPadding() + 12.dp),
                    )
                    UpdateBanner(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp)
                            .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp),
                    )
                }

                if (isShowNowPlaylistScreen && !isTabletLandscape) {
                    NowPlayingScreen(
                        navController = navController,
                    ) {
                        isShowNowPlaylistScreen = false
                    }
                }

                // Desktop's quiet update check: on open, then every six hours while the app stays up.
                LaunchedEffect(Unit) {
                    while (true) {
                        Updater.checkQuiet()
                        delay(Updater.INTERVAL)
                    }
                }

                // The fade and the pause are the whole ending; this only resets the timer to off.
                LaunchedEffect(sleepTimerState.isDone) {
                    if (sleepTimerState.isDone) viewModel.stopSleepTimer()
                }
            },
        )
    }
}

/**
 * The colour of the cover at [url], for "Adapt colors to artwork"; null when [url] is null. Picked
 * the way the M3 Expressive player seeds its scheme. The previous colour stays until the new cover
 * is decoded; a colourless cover, or one that fails to load, then drops back to the theme, as on
 * desktop (#522: keeping the last track's colour paints this one in a cover it doesn't have).
 * The mini player has already loaded the same cover, so this is a disk-cache read.
 */
@Composable
private fun rememberArtworkColor(
    url: String?,
    enabled: Boolean,
    // The last colour, kept by the view model: a cold start opens in it while the saved queue comes
    // back, and a recreated activity carries on in it instead of flashing the theme's accent.
    saved: Color?,
    onColor: (Color?) -> Unit,
): Color? {
    val context = LocalPlatformContext.current
    var color by remember { mutableStateOf(saved) }
    // Until the first track shows up, no track means the queue isn't restored yet, not nothing playing.
    // ponytail: a saved queue that fails to come back leaves the saved colour up until something plays.
    var sawTrack by remember { mutableStateOf(false) }
    LaunchedEffect(url, enabled) {
        if (!enabled || url == null) {
            if (!enabled || sawTrack) color = null
        } else {
            sawTrack = true
            val image =
                SingletonImageLoader
                    .get(context)
                    .execute(ImageRequest.Builder(context).data(url).size(ARTWORK_SEED_PX).build())
                    .image
            // ponytail: decoded on the track change, not one track early as desktop does; prewarm the
            // next cover if the colour visibly lands after the artwork.
            color =
                image?.let {
                    withContext(Dispatchers.Default) {
                        runCatching { it.toImageBitmap().toReadableBitmap(ARTWORK_SEED_PX, ARTWORK_SEED_PX).themeColorOrNull() }.getOrNull()
                    }
                }
        }
        onColor(color)
    }
    return color.takeIf { enabled }
}

private const val ARTWORK_SEED_PX = 112
