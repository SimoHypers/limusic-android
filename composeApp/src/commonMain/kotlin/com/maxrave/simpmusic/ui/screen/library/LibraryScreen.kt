package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.LibraryChipType
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.withoutYouTubeTwins
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.viewModel.trackIds
import simpmusic.composeapp.generated.resources.download_progress_short
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibrarySectionHeader
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.LimusicTabTopBar
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.NewPlaylistSheet
import com.maxrave.simpmusic.ui.component.PlaylistShelf
import com.maxrave.simpmusic.ui.component.RecentlyAddedList
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.selection.TrackSelectionBar
import com.maxrave.simpmusic.ui.component.selection.keyedRows
import com.maxrave.simpmusic.ui.component.selection.rememberTrackSelection
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.GridView
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.icon.Notifications
import com.maxrave.simpmusic.ui.icon.Podcasts
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Album
import com.maxrave.simpmusic.ui.icon.PeopleAlt
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Sync
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.no_saved_albums
import simpmusic.composeapp.generated.resources.songs
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.library
import simpmusic.composeapp.generated.resources.library_downloads
import simpmusic.composeapp.generated.resources.library_sync_to_youtube
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.no_playlists_downloaded
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.podcasts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val savedPlaylists by viewModel.savedPlaylists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val libraryOverview by viewModel.libraryOverview.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()

    val selection = rememberTrackSelection()
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()
    val hazeState =
        rememberHazeState()

    var topAppBarHeight by remember {
        mutableStateOf(0.dp)
    }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(nowPlaying) {
        Logger.d("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    val chipRowState = rememberScrollState()
    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()
    // A chip asked for by a deep link (simpmusic://library?tab=…), applied once and cleared.
    val libraryTabRequest by sharedViewModel.libraryTabRequest.collectAsStateWithLifecycle()
    LaunchedEffect(libraryTabRequest) {
        libraryTabRequest?.let {
            viewModel.setCurrentScreen(it)
            sharedViewModel.requestLibraryTab(null)
        }
    }

    val youTubePlaylistsVersion by sharedViewModel.youTubePlaylistsVersion.collectAsStateWithLifecycle()
    // Saveable so coming back to Library refetches once per change, not on every visit.
    var seenYouTubePlaylistsVersion by rememberSaveable { mutableIntStateOf(youTubePlaylistsVersion) }
    LaunchedEffect(youTubePlaylistsVersion) {
        if (youTubePlaylistsVersion != seenYouTubePlaylistsVersion) {
            seenYouTubePlaylistsVersion = youTubePlaylistsVersion
            viewModel.getYouTubePlaylist()
        }
    }

    val createdPlaylists by sharedViewModel.createdPlaylists.collectAsStateWithLifecycle()
    // Local first, then the account's: the shelf on All and the Playlists grid show both. A local
    // playlist synced back when Sync kept two copies shows once, as its YouTube one, and one just
    // made on YouTube shows until YouTube's own list catches up. Saved playlists the account's
    // list doesn't carry (saved signed out, waiting for Sync) sit in front of it, as on desktop.
    val allPlaylists: List<PlaylistType> =
        remember(yourLocalPlaylist, youTubePlaylist, createdPlaylists, savedPlaylists, loggedIn) {
            val youTube = if (loggedIn) youTubePlaylist.data.orEmpty() else emptyList()
            val listed = youTube.mapTo(HashSet()) { it.browseId.removePrefix("VL") }
            val created = if (loggedIn) createdPlaylists.filter { it.browseId.removePrefix("VL") !in listed } else emptyList()
            val saved = savedPlaylists.filter { it.id.removePrefix("VL") !in listed }
            yourLocalPlaylist.data.orEmpty().withoutYouTubeTwins(youTube) + created + saved + youTube
        }
    val playlistsLoading = yourLocalPlaylist is LocalResource.Loading || (loggedIn && youTubePlaylist is LocalResource.Loading)
    val loadPlaylists = { force: Boolean ->
        viewModel.getLocalPlaylist()
        viewModel.getSavedPlaylists()
        if (loggedIn && (force || youTubePlaylist.data.isNullOrEmpty())) viewModel.getYouTubePlaylist()
    }

    LaunchedEffect(currentFilter, loggedIn) {
        when (currentFilter) {
            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getRecentlyAdded()
                loadPlaylists(false)
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                loadPlaylists(false)
            }

            // Once per visit, not per chip switch: each is a walk through the account's library.
            LibraryChipType.ALBUMS -> {
                if (albums.data == null) viewModel.getAlbums()
            }

            LibraryChipType.ARTISTS -> {
                if (subscriptions.data == null) viewModel.getArtists()
            }

            LibraryChipType.SONGS -> {
                if (songs.data == null) viewModel.getSongs()
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }

        }
    }

    Crossfade(
        modifier = Modifier.hazeSource(hazeState),
        targetState = currentFilter,
    ) { filter ->
        when (filter) {
            LibraryChipType.YOUR_LIBRARY -> {
                val state = rememberLazyListState()
                val isScrollingUp by state.isScrollingUp()
                LaunchedEffect(state) {
                    snapshotFlow { state.firstVisibleItemIndex }
                        .collect {
                            if (it <= 1) {
                                onScrolling.invoke(true)
                            } else {
                                onScrolling.invoke(isScrollingUp)
                            }
                        }
                }
                // Desktop windows run wide; this tab keeps to the floating capsule player's width
                // so its cards line up with the capsule instead of stretching edge to edge. Done
                // as content padding rather than by narrowing the list, so the side gutters stay
                // part of it and the wheel still scrolls there.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val layoutDirection = LocalLayoutDirection.current
                    val innerStart = innerPadding.calculateStartPadding(layoutDirection)
                    val innerEnd = innerPadding.calculateEndPadding(layoutDirection)
                    val sideInset =
                        if (getPlatform() == Platform.Desktop) {
                            ((maxWidth - innerStart - innerEnd - YOUR_LIBRARY_MAX_WIDTH) / 2).coerceAtLeast(0.dp)
                        } else {
                            0.dp
                        }
                    LazyColumn(
                        contentPadding =
                            innerPadding.copy(
                                start = innerStart + sideInset,
                                top = topAppBarHeight,
                                end = innerEnd + sideInset,
                            ),
                        state = state,
                    ) {
                        item {
                            LibraryTilingBox(
                                navController,
                                libraryOverview,
                                signedIn = loggedIn,
                                onOpenArtists = { viewModel.setCurrentScreen(LibraryChipType.ARTISTS) },
                            )
                        }
                        item {
                            LibrarySectionHeader(
                                icon = SimpIcons.QueueMusic,
                                title = stringResource(Res.string.playlists),
                                modifier = Modifier.padding(top = 20.dp),
                                onMore = { viewModel.setCurrentScreen(LibraryChipType.LOCAL_PLAYLIST) },
                            )
                            PlaylistShelf(navController, allPlaylists, loading = playlistsLoading, onNewPlaylist = { showAddSheet = true })
                        }
                        item {
                            RecentlyAddedList(
                                data = recentlyAdded.data ?: emptyList(),
                                playingVideoId = nowPlaying,
                                isLoading = recentlyAdded is LocalResource.Loading,
                                navController = navController,
                                selection = selection,
                            )
                        }
                        item {
                            EndOfPage()
                        }
                    }
                }
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    if (playlistsLoading) LocalResource.Loading() else LocalResource.Success(allPlaylists),
                    onScrolling = onScrolling,
                    emptyText = Res.string.no_playlists_added,
                    createNewPlaylist = {
                        showAddSheet = true
                    },
                ) {
                    loadPlaylists(true)
                }
            }

            LibraryChipType.ALBUMS -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    albums,
                    emptyText = Res.string.no_saved_albums,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getAlbums()
                }
            }

            LibraryChipType.ARTISTS -> {
                LibraryArtistsTab(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    signedIn = loggedIn,
                    artists = artists,
                    subscriptions = subscriptions,
                    onScrolling = onScrolling,
                    onReload = viewModel::getArtists,
                )
            }

            LibraryChipType.SONGS -> {
                LibrarySongsTab(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    songs = songs,
                    playingVideoId = nowPlaying,
                    onPlay = viewModel::playSong,
                    onLoadMore = viewModel::getMoreSongs,
                    onScrolling = onScrolling,
                    onReload = viewModel::getSongs,
                )
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
                val checkingLibrary by sharedViewModel.checkingLibrary.collectAsStateWithLifecycle()
                // The library pass adds playlists and albums: show them once it is done.
                LaunchedEffect(checkingLibrary) { if (!checkingLibrary) viewModel.getDownloadedPlaylist() }
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    downloadedPlaylist,
                    emptyText = Res.string.no_playlists_downloaded,
                    onScrolling = onScrolling,
                    // Read in here, so a queue tick redraws these rows and not the whole screen.
                    header = {
                        val queue by viewModel.downloadQueue.collectAsStateWithLifecycle()
                        val bytes by viewModel.downloadedBytes.collectAsStateWithLifecycle()
                        val missing by viewModel.missingDownloads.collectAsStateWithLifecycle()
                        val keepLibrary by sharedViewModel.keepLibraryDownloaded.collectAsStateWithLifecycle()
                        DownloadsHeader(
                            keepLibrary = keepLibrary,
                            checkingLibrary = checkingLibrary,
                            onKeepLibrary = sharedViewModel::setKeepLibraryDownloaded,
                            queue = queue,
                            songCount = libraryOverview?.downloaded?.count ?: 0,
                            bytes = bytes,
                            missing = missing.size,
                            onPause = viewModel::setDownloadsPaused,
                            onOpenSongs = {
                                navController.navigate(LibraryDynamicPlaylistDestination(type = LibraryDynamicPlaylistType.Downloaded.toStringParams()))
                            },
                            onRetry = viewModel::retryMissingDownloads,
                        )
                    },
                    subtitleOf = { item ->
                        val ids = item.trackIds()
                        val done = remember(ids, downloadedIds) { ids.count { it in downloadedIds } }
                        if (done < ids.size) stringResource(Res.string.download_progress_short, done, ids.size) else null
                    },
                ) {
                    viewModel.getDownloadedPlaylist()
                }
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePodcasts,
                    emptyText = Res.string.no_favorite_podcasts,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getFavoritePodcasts()
                }
            }

        }
    }
    if (showAddSheet) {
        val onYouTube by sharedViewModel.newPlaylistOnYouTube.collectAsStateWithLifecycle()
        NewPlaylistSheet(
            signedIn = loggedIn,
            onYouTube = onYouTube,
            onDismiss = { showAddSheet = false },
            onCreate = { title, toYouTube, onResult ->
                sharedViewModel.createPlaylist(title, toYouTube) { created ->
                    onResult(created != null)
                    if (created != null) {
                        viewModel.playlistCreated(title, onPhone = created is LocalPlaylistEntity)
                        showAddSheet = false
                    }
                }
            },
        )
    }
    Column(
        Modifier
            .background(Color.Transparent)
            .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()).onGloballyPositioned { coordinates ->
                topAppBarHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        LimusicTabTopBar(title = stringResource(Res.string.library)) {
            // Only with something to push: what was liked, followed or saved while signed out
            // (or before likes went to YouTube) lives on the phone until this puts it on the account.
            if (unsyncedCount > 0) {
                IconButton(onClick = viewModel::syncToYouTube, enabled = !syncing) {
                    BadgedBox(
                        badge = {
                            // Badge's labelSmall comes from typo(), which bakes in a grey; the owner wants white.
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(unsyncedCount.toString(), color = Color.White)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = SimpIcons.Sync,
                            contentDescription = stringResource(Res.string.library_sync_to_youtube, unsyncedCount),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            RippleIconButton(imageVector = SimpIcons.Add, tint = MaterialTheme.colorScheme.onBackground) {
                showAddSheet = true
            }
            RippleIconButton(imageVector = SimpIcons.History, tint = MaterialTheme.colorScheme.onBackground) {
                navController.navigate(RecentlySongsDestination)
            }
            // New releases from the artists you follow; moved here from Home, next to where they live.
            RippleIconButton(imageVector = SimpIcons.Notifications, tint = MaterialTheme.colorScheme.onBackground) {
                navController.navigate(NotificationDestination)
            }
            ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
        }
        Row(
            modifier =
                Modifier
                    .horizontalScroll(chipRowState)
                    .padding(horizontal = 15.dp)
                    .padding(bottom = 8.dp)
                    .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LibraryChips.forEach { (type, icon, label) ->
                Chip(
                    isAnimated = false,
                    isSelected = type == currentFilter,
                    text = stringResource(label),
                    icon = icon,
                ) {
                    viewModel.setCurrentScreen(type)
                }
            }
        }
    }
    // Only All's Recently added list picks songs: another tab has nothing to pick, so it leaves.
    LaunchedEffect(currentFilter) { selection.exit() }
    val recentSongs = recentlyAdded.data
    val rows = remember(recentSongs) { keyedRows(recentSongs.orEmpty().filterIsInstance<SongEntity>().map { it.toTrack() }) }
    TrackSelectionBar(selection = selection, rows = rows, bottomPadding = innerPadding.calculateBottomPadding())
}

/**
 * How wide the Your Library tab may get on Desktop: the floating capsule player's own width plus
 * the 10dp gutter every row of this list already carries, so the cards' edges land on the
 * capsule's. The capsule has no width constant of its own — `MiniPlayer`'s Desktop row sums to
 * 16 + 200 (controls) + 29 (divider) + 300 (track) + 29 (divider) + 232 (heart cell 40 + four
 * 48dp icon buttons) + 16 = 822dp — so this must follow if that row changes.
 */
private val YOUR_LIBRARY_MAX_WIDTH = 842.dp

/** The chip row, in the order it reads. */
private val LibraryChips =
    listOf(
        Triple(LibraryChipType.YOUR_LIBRARY, SimpIcons.GridView, Res.string.all),
        Triple(LibraryChipType.LOCAL_PLAYLIST, SimpIcons.QueueMusic, Res.string.playlists),
        Triple(LibraryChipType.ALBUMS, SimpIcons.Album, Res.string.albums),
        Triple(LibraryChipType.ARTISTS, SimpIcons.PeopleAlt, Res.string.artists),
        Triple(LibraryChipType.SONGS, SimpIcons.LibraryMusic, Res.string.songs),
        Triple(LibraryChipType.DOWNLOADED_PLAYLIST, SimpIcons.DownloadForOffline, Res.string.library_downloads),
        Triple(LibraryChipType.FAVORITE_PODCAST, SimpIcons.Podcasts, Res.string.podcasts),
    )
