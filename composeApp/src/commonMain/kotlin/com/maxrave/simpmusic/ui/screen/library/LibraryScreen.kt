package com.maxrave.simpmusic.ui.screen.library

import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.simpmusic.ui.component.LibrarySectionHeader
import com.maxrave.simpmusic.ui.component.PlaylistShelf
import com.maxrave.simpmusic.ui.component.RecentlyAddedList
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.GridView
import com.maxrave.simpmusic.ui.icon.AutoAwesome
import com.maxrave.simpmusic.ui.icon.Bookmark
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.Podcasts
import simpmusic.composeapp.generated.resources.all
import simpmusic.composeapp.generated.resources.playlists
import simpmusic.composeapp.generated.resources.library_mixes
import simpmusic.composeapp.generated.resources.library_saved
import simpmusic.composeapp.generated.resources.library_downloads
import simpmusic.composeapp.generated.resources.podcasts
import com.maxrave.simpmusic.ui.component.LimusicTabTopBar
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.History
import com.maxrave.simpmusic.ui.navigation.destination.home.RecentlySongsDestination
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.extension.ultraThinBarStyle
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.component.ListenTogetherIconButton
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create
import simpmusic.composeapp.generated.resources.library
import simpmusic.composeapp.generated.resources.no_favorite_playlists
import simpmusic.composeapp.generated.resources.no_favorite_podcasts
import simpmusic.composeapp.generated.resources.no_mixes_found
import simpmusic.composeapp.generated.resources.no_playlists_added
import simpmusic.composeapp.generated.resources.no_playlists_downloaded
import simpmusic.composeapp.generated.resources.playlist_name
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty

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
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val libraryOverview by viewModel.libraryOverview.collectAsStateWithLifecycle()

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }
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

    // Local first, then the account's: the shelf on All and the Playlists grid show both.
    val allPlaylists: List<PlaylistType> =
        remember(yourLocalPlaylist, youTubePlaylist, loggedIn) {
            yourLocalPlaylist.data.orEmpty() + if (loggedIn) youTubePlaylist.data.orEmpty() else emptyList()
        }
    val playlistsLoading = yourLocalPlaylist is LocalResource.Loading || (loggedIn && youTubePlaylist is LocalResource.Loading)
    val mixForYou by viewModel.youTubeMixForYou.collectAsStateWithLifecycle()
    val loadPlaylists = { force: Boolean ->
        viewModel.getLocalPlaylist()
        if (loggedIn && (force || youTubePlaylist.data.isNullOrEmpty())) viewModel.getYouTubePlaylist()
    }

    LaunchedEffect(currentFilter, loggedIn) {
        when (currentFilter) {
            // The account's playlists joined the Playlists chip. A saved choice or a link asking
            // for the old chip lands there instead.
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                viewModel.setCurrentScreen(LibraryChipType.LOCAL_PLAYLIST)
            }

            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                if (loggedIn && mixForYou.data.isNullOrEmpty()) viewModel.getYouTubeMixedForYou()
            }

            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getRecentlyAdded()
                loadPlaylists(false)
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                loadPlaylists(false)
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
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
                            LibraryTilingBox(navController, libraryOverview)
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
                                selectionState = selectionState,
                            )
                        }
                        item {
                            EndOfPage()
                        }
                    }
                }
            }

            // Nothing to draw: the effect above sends this filter on to Playlists.
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> Unit

            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    mixForYou,
                    emptyText = Res.string.no_mixes_found,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getYouTubeMixedForYou()
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

            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePlaylist,
                    emptyText = Res.string.no_favorite_playlists,
                    onScrolling = onScrolling,
                ) {
                    viewModel.getPlaylistFavorite()
                }
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    downloadedPlaylist,
                    emptyText = Res.string.no_playlists_downloaded,
                    onScrolling = onScrolling,
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
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf("") }
        val showAddSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showAddSheetState.hide()
                    showAddSheet = false
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = MaterialTheme.colorScheme.outline,
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.playlist_name))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                viewModel.makeToast(runBlocking { getString(Res.string.playlist_name_cannot_be_empty) })
                            } else {
                                viewModel.createPlaylist(newTitle)
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.create))
                    }
                }
            }
        }
    }
    Column(
        Modifier
            .background(Color.Transparent)
            .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()).onGloballyPositioned { coordinates ->
                topAppBarHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        LimusicTabTopBar(title = stringResource(Res.string.library)) {
            RippleIconButton(imageVector = SimpIcons.Add, tint = MaterialTheme.colorScheme.onBackground) {
                showAddSheet = true
            }
            RippleIconButton(imageVector = SimpIcons.History, tint = MaterialTheme.colorScheme.onBackground) {
                navController.navigate(RecentlySongsDestination)
            }
            ListenTogetherIconButton { navController.navigate(ListenTogetherDestination) }
        }
        AnimatedVisibility(visible = selectionState.isActive) {
            SongSelectionTopAppBar(
                state = selectionState,
                // Stacked BELOW the Library TopAppBar in the same Column, which already consumed
                // the status-bar inset — leaving the default here reserved it twice and opened a
                // status-bar-sized band of dead blur between the two bars. Same fix as Search;
                // the overlay-style call sites (Album, Artist, Recently…) keep the default because
                // they COVER their normal bar instead of standing under it.
                windowInsets = WindowInsets(0),
                onSelectAll = {
                    selectionState.toggleSelectAll(
                        (recentlyAdded.data ?: emptyList())
                            .filterIsInstance<SongEntity>()
                            .map { it.videoId },
                    )
                },
                onOpenActions = { showSelectionSheet = true },
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            )
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
                // Mixes come from the signed-in YouTube account.
                if (type == LibraryChipType.YOUTUBE_MIX_FOR_YOU && !loggedIn) {
                    return@forEach
                }
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
        if (showSelectionSheet) {
            val selectedIds = selectionState.selected.toList()
            SelectedSongsBottomSheet(
                count = selectedIds.size,
                onDismiss = { showSelectionSheet = false },
                onPlayNext = {
                    selectionViewModel.playNext(selectedIds)
                    selectionState.exit()
                },
                onAddToQueue = {
                    selectionViewModel.addToQueue(selectedIds)
                    selectionState.exit()
                },
                onAddToPlaylist = { showSelectionAddToPlaylist = true },
                onDownload = {
                    selectionViewModel.download(selectedIds)
                    selectionState.exit()
                },
                onAddToFavorite = {
                    selectionViewModel.addToFavorite(selectedIds)
                    selectionState.exit()
                },
            )
        }
        if (showSelectionAddToPlaylist) {
            val selectedIds = selectionState.selected.toList()
            val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
            AddToPlaylistModalBottomSheet(
                isBottomSheetVisible = true,
                listLocalPlaylist = localPlaylists,
                listYouTubePlaylist = emptyList(),
                onDismiss = { showSelectionAddToPlaylist = false },
                onClick = { playlist ->
                    selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                    selectionState.exit()
                },
                onYTPlaylistClick = {},
            )
        }
    }
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
        Triple(LibraryChipType.YOUTUBE_MIX_FOR_YOU, SimpIcons.AutoAwesome, Res.string.library_mixes),
        Triple(LibraryChipType.FAVORITE_PLAYLIST, SimpIcons.Bookmark, Res.string.library_saved),
        Triple(LibraryChipType.DOWNLOADED_PLAYLIST, SimpIcons.DownloadForOffline, Res.string.library_downloads),
        Triple(LibraryChipType.FAVORITE_PODCAST, SimpIcons.Podcasts, Res.string.podcasts),
    )
