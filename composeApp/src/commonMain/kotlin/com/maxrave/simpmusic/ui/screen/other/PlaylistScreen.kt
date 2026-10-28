package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.material3.MaterialTheme
import org.jetbrains.compose.resources.pluralStringResource
import com.maxrave.simpmusic.ui.component.CollectionAuthorLine
import com.maxrave.simpmusic.ui.component.CollectionHero
import com.maxrave.simpmusic.ui.component.CollectionTitle
import simpmusic.composeapp.generated.resources.liked_songs_count
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.PlaylistSort
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.barBlurStyle
import com.maxrave.simpmusic.ui.component.CollectionDownloadButton
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistMenu
import com.maxrave.simpmusic.ui.component.PlaylistSortSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.pillSurface
import com.maxrave.simpmusic.ui.component.selection.TrackSelectionBar
import com.maxrave.simpmusic.ui.component.selection.keyedRows
import com.maxrave.simpmusic.ui.component.selection.rememberTrackSelection
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Sort
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.PlaylistUIEvent
import com.maxrave.simpmusic.viewModel.PlaylistUIState
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.sort_by
import simpmusic.composeapp.generated.resources.unlimited

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    playlistId: String,
    isYourYouTubePlaylist: Boolean,
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
) {
    // Home shelves navigate with the browseEndpoint id, which is "VL" + the playlist id
    // (HomeParser reads title.runs[0].navigationEndpoint.browseEndpoint.browseId). Every radio
    // prefix check and the watch endpoint expect the bare id, so normalise once on the way in
    // rather than stripping "VL" again at each consumer.
    val id = playlistId.removePrefix("VL")
    val tag = "PlaylistScreen"

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val download by viewModel.download.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val tracks by viewModel.sortedTracks.collectAsStateWithLifecycle()
    val sortState by viewModel.sortState.collectAsStateWithLifecycle()
    var showSortSheet by remember { mutableStateOf(false) }
    val tracksListState by viewModel.tracksListState.collectAsStateWithLifecycle()
    val removed by viewModel.removed.collectAsStateWithLifecycle()

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var searchBarHeightPx by remember { mutableStateOf(0) }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val selection = rememberTrackSelection()
    val rows = remember(tracks) { keyedRows(tracks) }
    // Filtered as keyed rows, so a pick the search hides is still that row.
    val shownRows =
        remember(rows, query, showSearchBar) {
            if (query.isEmpty() || !showSearchBar) {
                rows
            } else {
                rows.filter { (_, it) ->
                    it.title.contains(query, ignoreCase = true) ||
                        it.artists?.joinToString(", ")?.contains(query, ignoreCase = true) == true
                }
            }
        }

    LaunchedEffect(uiState) {
        Logger.d(tag, "uiState hash: ${uiState.hashCode()}")
        Logger.d(tag, "uiState data: ${uiState.data}")
    }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            viewModel.getFullTracks {}
            lazyState.animateScrollToItem(0)
        }
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                tracksListState != ListState.PAGINATION_EXHAUST &&
                    (
                        lazyState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (lazyState.layoutInfo.totalItemsCount - 6)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d(tag, "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d(tag, "tracksListState: $tracksListState")
        Logger.d(tag, "Continuation: $continuation")
        if (shouldStartPaginate.value && tracksListState == ListState.IDLE) {
            viewModel.getContinuationTrack(
                id,
                continuation,
            )
        }
    }

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    var currentItem by remember {
        mutableStateOf<Track?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var menuRequest by remember { mutableIntStateOf(0) }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            PlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = tracks.firstOrNull { it.videoId == videoId }
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        menuRequest++
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.data?.id) {
            Logger.d(tag, "new id: $id")
            viewModel.getData(id)
        }
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val background = MaterialTheme.colorScheme.background

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
    Crossfade(
        targetState = uiState,
    ) { state ->
        Logger.d(tag, "State hash: ${state.hashCode()}")
        when (state) {
            is PlaylistUIState.Success -> {
                val data = state.data
                Logger.d(tag, "data: $data")
                if (data == null) return@Crossfade
                val hazeState =
                    rememberHazeState()
                val isThisPlaying = isPlaying && playingPlaylistId == data.id
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(background)
                            .hazeSource(hazeState),
                    state = lazyState,
                ) {
                    if (!showSearchBar) {
                        item(contentType = "header") {
                            Column(Modifier.animateItem()) {
                                CollectionHero(
                                    thumbnail = data.thumbnail,
                                    onBack = { navController.navigateUp() },
                                    actions = {
                                        Row(
                                            modifier = Modifier.height(48.dp).pillSurface(RoundedCornerShape(24.dp)).padding(horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            IconButton(onClick = { showSearchBar = true }, modifier = Modifier.size(40.dp)) {
                                                Icon(SimpIcons.Search, stringResource(Res.string.search), tint = MaterialTheme.colorScheme.onSurface)
                                            }
                                            if (!data.isRadio) {
                                                IconButton(onClick = { showSortSheet = true }, modifier = Modifier.size(40.dp)) {
                                                    // Accent while the list is in anything but its own order.
                                                    val sorted = sortState.sort != PlaylistSort.DEFAULT || sortState.desc
                                                    Icon(
                                                        SimpIcons.Sort,
                                                        stringResource(Res.string.sort_by),
                                                        tint = if (sorted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }
                                                CollectionDownloadButton(
                                                    download = download,
                                                    onDownload = { viewModel.onUIEvent(PlaylistUIEvent.Download) },
                                                    onRemove = viewModel::removeDownload,
                                                    onDownloadMissing = viewModel::downloadMissing,
                                                )
                                            }
                                            // Nothing to save: your own playlists and Liked Music are yours already.
                                            if (!data.isRadio && !data.owned && data.id != "LM") {
                                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                    HeartCheckBox(
                                                        size = 28,
                                                        checked = liked,
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        onStateChange = { viewModel.onUIEvent(PlaylistUIEvent.Favorite) },
                                                    )
                                                }
                                            }
                                            IconButton(onClick = onPlaylistMoreClick, modifier = Modifier.size(40.dp)) {
                                                Icon(SimpIcons.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    },
                                ) {
                                    CollectionTitle(
                                        // The year is the subtitle's last run, which is not always a year.
                                        kicker =
                                            listOfNotNull(
                                                stringResource(if (data.isRadio) Res.string.radio else Res.string.playlist),
                                                data.year.takeIf { year -> year.toIntOrNull() != null },
                                            ).joinToString(" • "),
                                        title = data.title,
                                        playing = isThisPlaying,
                                        onPlay = {
                                            if (isThisPlaying) {
                                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                            } else {
                                                viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                            }
                                        },
                                        onShuffle = if (data.isRadio) null else ({ viewModel.onUIEvent(PlaylistUIEvent.Shuffle) }),
                                    )
                                    CollectionAuthorLine(
                                        name = data.author.name,
                                        thumbnail = data.authorThumbnail,
                                        meta =
                                            listOf(
                                                if (data.isRadio) {
                                                    stringResource(Res.string.unlimited)
                                                } else {
                                                    val count = data.trackCount - removed
                                                    pluralStringResource(Res.plurals.liked_songs_count, count, count)
                                                },
                                                data.duration,
                                            ),
                                        onClick =
                                            data.author.id.takeIf { it.isNotEmpty() }?.let { channelId ->
                                                { navController.navigate(ArtistDestination(channelId)) }
                                            },
                                    )
                                }
                                data.description?.takeIf { text -> text.isNotBlank() }?.let { text ->
                                    val uriHandler = LocalUriHandler.current
                                    DescriptionView(
                                        text = text,
                                        limitLine = 2,
                                        onTimeClicked = {},
                                        onURLClicked = { url -> uriHandler.openUri(url) },
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    } else {
                        item {
                            val density = LocalDensity.current
                            Spacer(
                                Modifier.height(
                                    with(density) { searchBarHeightPx.toDp() },
                                ),
                            )
                        }
                    }
                    items(count = shownRows.size, key = { index ->
                        val item = shownRows.getOrNull(index)?.second
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val row = shownRows.getOrNull(index)
                        if (row != null) {
                            val (key, item) = row
                            // Wrapped: SongFullWidthItems puts its modifier on two nodes. Dimmed while
                            // YouTube re-sorts, or picking a sort looks like it did nothing.
                            Column(Modifier.animateItem().alpha(if (sortState.resorting) 0.5f else 1f)) {
                                SongFullWidthItems(
                                    isPlaying = playingTrack?.videoId == item.videoId && isPlaying,
                                    track = item,
                                    onMoreClickListener = { onItemMoreClick(it) },
                                    onClickListener = { onPlaylistItemClick(it) },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(
                                            arrayListOf(item),
                                        )
                                    },
                                    selectionMode = selection.active,
                                    isSelected = selection.has(key),
                                    onLongClick = { selection.start(key, item) },
                                    onSelectToggle = { selection.toggle(key, item) },
                                    modifier = Modifier,
                                    padding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                )
                            }
                        }
                    }
                    when (tracksListState) {
                        ListState.IDLE -> {
                            // DO NOTHING
                            item {
                                EndOfPage()
                            }
                        }

                        ListState.LOADING, ListState.PAGINATING -> {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CenterLoadingBox(
                                        modifier = Modifier.size(80.dp),
                                    )
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }

                        ListState.ERROR -> {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.error),
                                        style = typo().bodyMedium,
                                    )
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }

                        ListState.PAGINATION_EXHAUST -> {
                            item {
                                EndOfPage()
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = SearchBarEnter,
                    exit = SearchBarExit,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { searchBarHeightPx = it.size.height }
                            .hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .windowInsetsPadding(WindowInsets.statusBars),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RippleIconButton(
                                imageVector = SimpIcons.ArrowBackIosNew,
                                tint = MaterialTheme.colorScheme.onSurface,
                            ) {
                                navController.navigateUp()
                            }
                            SearchBar(
                                modifier =
                                    Modifier
                                        .height(50.dp)
                                        .padding(horizontal = 12.dp)
                                        .weight(1f),
                                colors =
                                    SearchBarDefaults.colors().copy(
                                        containerColor = Color.Transparent,
                                    ),
                                inputField = {
                                    CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                                        SearchBarDefaults.InputField(
                                            query = query,
                                            onQueryChange = { query = it },
                                            onSearch = { showSearchBar = false },
                                            expanded = showSearchBar,
                                            onExpandedChange = { showSearchBar = it },
                                            placeholder = {
                                                Text(
                                                    stringResource(Res.string.search),
                                                    style = typo().bodyMedium,
                                                )
                                            },
                                        )
                                    }
                                },
                                expanded = false,
                                onExpandedChange = {},
                                windowInsets = WindowInsets(0, 0, 0, 0),
                            ) {
                            }
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
                            ) {
                                Icon(SimpIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Box(
                    Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding() + 8.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Liked Music is emptied by unliking; any other playlist only when it is yours.
                    val canRemove = data.id.removePrefix("VL") == "LM" || ((data.owned || isYourYouTubePlaylist) && !data.isRadio)
                    TrackSelectionBar(
                        selection = selection,
                        rows = rows,
                        shown = shownRows,
                        // A radio pages on without end: what has loaded is all there is to pick.
                        complete = data.isRadio || (tracksListState == ListState.PAGINATION_EXHAUST && !sortState.resorting),
                        total = data.trackCount - removed,
                        loadRest = { viewModel.getFullTracks {} },
                        from = data.title,
                        onRemove =
                            if (canRemove) {
                                { picked, done ->
                                    viewModel.removeTracks(picked) {
                                        sharedViewModel.youTubePlaylistsChanged()
                                        done()
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }
                if (itemBottomSheetShow && currentItem != null) {
                    val track = currentItem?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = {
                            itemBottomSheetShow = false
                            currentItem = null
                        },
                        navController = navController,
                        song = track,
                    )
                }
                if (showSortSheet) {
                    PlaylistSortSheet(
                        sort = sortState.sort,
                        desc = sortState.desc,
                        onSort = {
                            viewModel.setSort(it, sortState.desc)
                            showSortSheet = false
                        },
                        onReverse = { viewModel.setSort(sortState.sort, it) },
                        onDismiss = { showSortSheet = false },
                    )
                }
                if (playlistBottomSheetShow) {
                    key(menuRequest) {
                        PlaylistMenu(
                            viewModel = viewModel,
                            placeholder = data,
                            isYourYouTubePlaylist = isYourYouTubePlaylist,
                            onClose = { playlistBottomSheetShow = false },
                            onDeleted = { navController.navigateUp() },
                            onSelectTracks = selection::enter,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar && !showSearchBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        windowInsets =
                            TopAppBarDefaults.windowInsets.exclude(
                                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                            ),
                        title = {
                            Text(
                                text = data.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(
                                            align = Alignment.CenterVertically,
                                        ).basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        },
                        navigationIcon = {
                            Box(Modifier.padding(horizontal = 5.dp)) {
                                RippleIconButton(
                                    SimpIcons.ArrowBackIosNew,
                                    Modifier
                                        .size(32.dp),
                                    true,
                                    MaterialTheme.colorScheme.onSurface,
                                ) {
                                    navController.navigateUp()
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
                            ) {
                                Icon(SimpIcons.Search, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        modifier =
                            Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
                    )
                }
            }

            is PlaylistUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier = Modifier.size(80.dp),
                    )
                }
            }

            is PlaylistUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                navController.navigateUp()
            }
        }
    }
}