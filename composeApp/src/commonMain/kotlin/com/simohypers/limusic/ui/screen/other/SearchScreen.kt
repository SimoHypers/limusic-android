package com.simohypers.limusic.ui.screen.other

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.intent.GenericIntent
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.model.searchResult.songs.SongsResult
import com.simohypers.limusic.domain.data.model.searchResult.videos.VideosResult
import com.simohypers.limusic.domain.data.type.SearchResultType
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.Platform
import com.simohypers.limusic.expect.ui.PlatformBackHandler
import com.simohypers.limusic.extension.getScreenSizeInfo
import com.simohypers.limusic.extension.ultraThinBarStyle
import com.simohypers.limusic.getPlatform
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.AmbientThemeGlow
import com.simohypers.limusic.ui.component.ForYouMoodTile
import com.simohypers.limusic.ui.component.GenreMoodTile
import com.simohypers.limusic.ui.component.LibrarySectionHeader
import com.simohypers.limusic.ui.component.MoodsMoodTile
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.extension.toAppDeepLinkOrNull
import com.simohypers.limusic.ui.component.Chip
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.ShimmerSearchItem
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.ArrowOutward
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.home.MoodDestination
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.search.SearchDestination
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SearchScreenUIState
import com.simohypers.limusic.viewModel.SearchType
import com.simohypers.limusic.viewModel.SearchViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.toStringRes
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.clear
import limusic.composeapp.generated.resources.recent_searches
import limusic.composeapp.generated.resources.search
import limusic.composeapp.generated.resources.error_occurred
import limusic.composeapp.generated.resources.no_results_found
import limusic.composeapp.generated.resources.playlists
import limusic.composeapp.generated.resources.podcasts
import limusic.composeapp.generated.resources.retry
import limusic.composeapp.generated.resources.search_for
import limusic.composeapp.generated.resources.song
import limusic.composeapp.generated.resources.videos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(),
) {
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchScreenState by searchViewModel.searchScreenState.collectAsStateWithLifecycle()
    val uiState by searchViewModel.searchScreenUIState.collectAsStateWithLifecycle()
    val searchHistory by searchViewModel.searchHistory.collectAsStateWithLifecycle()
    val moodAndGenres by searchViewModel.moodAndGenres.collectAsStateWithLifecycle()
    val moodArtwork by searchViewModel.moodArtwork.collectAsStateWithLifecycle()

    var searchUIType by rememberSaveable { mutableStateOf(SearchUIType.EMPTY) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchSubmitted by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var isFocused by rememberSaveable { mutableStateOf(false) }

    val isSearchPanelVisible = searchUIType == SearchUIType.SEARCH_SUGGESTIONS
    val dismissSearchPanel: () -> Unit = {
        isExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    PlatformBackHandler(enabled = isSearchPanelVisible, onBack = dismissSearchPanel)

    // The bar floats OVER the content (a Box, not a Column) so there is something behind it to
    // blur — same arrangement HomeScreen uses. Each branch owns a scroll state, hoisted here so
    // the bar can tell whether the branch currently on screen is scrolled away from the top.
    // Two columns only on a phone held upright. Anywhere wider — tablet, landscape, desktop — two
    // columns stretch each tile to half the window, and since the tile keeps a 2:1 ratio it grows
    // absurdly tall with it.
    val screenInfo = getScreenSizeInfo()
    val isMobilePortrait = getPlatform() == Platform.Android && screenInfo.wDP < screenInfo.hDP
    val moodGridColumns = if (isMobilePortrait) 2 else 4

    val hazeState = rememberHazeState()
    val suggestionsState = rememberLazyListState()
    val moodGridState = rememberLazyGridState()
    val resultsState = rememberLazyGridState()
    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val searchBarHeight = with(density) { searchBarHeightPx.toDp() }
    // The "Search" title scrolls away like desktop's while the pill stays. Each list opens with a
    // spacer exactly the title's height (item 0), so how far item 0 has scrolled IS how far the
    // title has gone, with no jump when the list moves on to item 1.
    val titleHeightPx = with(density) { SearchTitleHeight.toPx() }
    val scrolledPx by remember {
        derivedStateOf {
            val (index, offset) =
                when (searchUIType) {
                    SearchUIType.EMPTY -> moodGridState.firstVisibleItemIndex to moodGridState.firstVisibleItemScrollOffset
                    SearchUIType.SEARCH_SUGGESTIONS -> suggestionsState.firstVisibleItemIndex to suggestionsState.firstVisibleItemScrollOffset
                    SearchUIType.SEARCH_RESULTS -> resultsState.firstVisibleItemIndex to resultsState.firstVisibleItemScrollOffset
                }
            if (index == 0) offset else Int.MAX_VALUE
        }
    }
    // Content only reaches the pill once the title has gone (until then the spacer sits under
    // it), so the frosted backing waits for that; earlier it hid the glow behind the title.
    val isBarFrosted by remember { derivedStateOf { scrolledPx > titleHeightPx } }
    // Read only inside offset/graphicsLayer lambdas, so scrolling moves the title without recomposing.
    val titleCollapse = { minOf(scrolledPx.toFloat(), titleHeightPx) }
    // The lists' top padding: the bar minus the title, whose height the spacer item supplies.
    val listTop = (searchBarHeight - SearchTitleHeight).coerceAtLeast(0.dp)

    val searchForString = stringResource(Res.string.search_for)
    val songString = stringResource(Res.string.song).lowercase()
    val artistString = stringResource(Res.string.artists).lowercase()
    val albumString = stringResource(Res.string.albums).lowercase()
    val playlistString = stringResource(Res.string.playlists).lowercase()
    val videoString = stringResource(Res.string.videos).lowercase()
    val podcastString = stringResource(Res.string.podcasts).lowercase()

    // Animated Placeholder
    val placeholderTexts =
        remember {
            listOf(
                "$searchForString $songString...",
                "$searchForString $artistString...",
                "$searchForString $albumString...",
                "$searchForString $playlistString...",
                "$searchForString $videoString...",
                "$searchForString $podcastString...",
            )
        }

    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }

    // Animate placeholder - pause when focused
    LaunchedEffect(isFocused) {
        while (!isFocused) {
            delay(3000) // Change every 3 seconds
            currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholderTexts.size
        }
    }

    var sheetSong by remember { mutableStateOf<SongEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val selection = rememberTrackSelection()
    val currentVideoId by searchViewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val chipRowState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()

    val onMoreClick: (SongEntity) -> Unit = { song ->
        sheetSong = song
        showBottomSheet = true
    }

    val runSearch: (String) -> Unit = { query ->
        searchText = query
        focusManager.clearFocus()
        isSearchSubmitted = true
        searchViewModel.insertSearchHistory(query)
        searchViewModel.search(query)
        // A new answer starts at its top.
        resultsState.requestScrollToItem(0)
    }
    // A chip or a "See all": the list starts at its top, like desktop's pickCat.
    val showChip: (SearchType) -> Unit = { type ->
        searchViewModel.setSearchType(type)
        resultsState.requestScrollToItem(0)
    }

    LaunchedEffect(searchText) {
        if (isFocused) {
            isSearchSubmitted = false
            isExpanded = true
        }
        if (searchText.isNotEmpty() && isFocused) {
            searchViewModel.suggestQuery(searchText)
        }
    }

    LaunchedEffect(isSearchSubmitted) {
        if (isSearchSubmitted) {
            isExpanded = false
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            isExpanded = true
        }
    }

    LaunchedEffect(isExpanded, searchText, isFocused) {
        // An empty field, focused or not, is the browse page: recent searches live on it.
        searchUIType =
            if (searchText.isNotEmpty() && isExpanded) {
                SearchUIType.SEARCH_SUGGESTIONS
            } else if (searchText.isEmpty()) {
                SearchUIType.EMPTY
            } else {
                SearchUIType.SEARCH_RESULTS
            }
    }

    //On search icon click while on search screen, open keyboard. Android only feature
    if (getPlatform() == Platform.Android) {
        val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
        LaunchedEffect(reloadDestination) {
            if (reloadDestination == SearchDestination::class) {
                if (!selection.active && searchUIType == SearchUIType.EMPTY) {
                    isExpanded = true
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                sharedViewModel.reloadDestinationDone()
            }
        }
    }

    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                sheetSong = null
            },
            navController = navController,
            song = sheetSong,
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                // A tab now: stop above the mini player and the tab bar.
                .padding(bottom = innerPadding.calculateBottomPadding()),
    ) {
        // Settings' accent glow behind the title (desktop's wash), leaving with it.
        AmbientThemeGlow(
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer { alpha = 1f - titleCollapse() / titleHeightPx },
        )
        // Content scrolls under the bar (it is the haze source), so it needs top padding
        // equal to the bar's measured height to keep its first item clear of it.
        Crossfade(
            targetState = searchUIType,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
        ) {
            when (it) {
                SearchUIType.SEARCH_SUGGESTIONS -> {
                    LazyColumn(
                        Modifier.padding(horizontal = 16.dp),
                        state = suggestionsState,
                        contentPadding = PaddingValues(top = listTop, bottom = 10.dp),
                    ) {
                        item { Spacer(Modifier.height(SearchTitleHeight)) }
                        items(searchScreenState.suggestYTItems) { item ->
                            SuggestItemRow(
                                searchResult = item,
                                onItemClick = { item ->
                                    when (item) {
                                        is SongsResult, is VideosResult -> {
                                            val firstTrack: Track = (item as? SongsResult)?.toTrack() ?: (item as VideosResult).toTrack()
                                            searchViewModel.playFromSearch(firstTrack, searchText)
                                        }

                                        is ArtistsResult -> {
                                            navController.navigate(
                                                ArtistDestination(item.browseId),
                                            )
                                        }

                                        is AlbumsResult -> {
                                            navController.navigate(
                                                AlbumDestination(item.browseId),
                                            )
                                        }

                                        is PlaylistsResult -> {
                                            navController.navigate(
                                                PlaylistDestination(
                                                    item.browseId,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        items(searchScreenState.suggestQueries) { suggestion ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(),
                                            onClick = { runSearch(suggestion) },
                                        ).padding(horizontal = 12.dp, vertical = 2.dp)
                                        .clip(rounded(8.dp)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = suggestion,
                                    style = typo().bodyMedium,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        searchText = suggestion
                                        focusRequester.requestFocus()
                                    },
                                ) {
                                    Icon(
                                        imageVector = LimusicIcons.ArrowOutward,
                                        contentDescription = "Search suggestion",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                        item {
                            EndOfPage(
                                withoutCredit = true,
                            )
                        }
                    }
                }

                SearchUIType.EMPTY -> {
                    val mood = moodAndGenres
                    // Capped and centred: on a wide desktop window the grid would otherwise
                    // span the whole width, stretching four tiles into long bars. 1100.dp
                    // keeps a tile near 250.dp, which is its natural size.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        // No side padding on the grid: the sideways rows run to the screen's edges,
                        // and the tiles take their 16.dp margin from their column (tileMargin).
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(moodGridColumns),
                            modifier = Modifier.fillMaxHeight().widthIn(max = 1100.dp),
                            state = moodGridState,
                            contentPadding = PaddingValues(top = listTop, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(SearchTitleHeight)) }
                            if (searchHistory.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "recent") {
                                    RecentSearches(
                                        queries = searchHistory,
                                        onSearch = runSearch,
                                        onForget = searchViewModel::forgetSearch,
                                        onClear = searchViewModel::deleteSearchHistory,
                                    )
                                }
                            }
                            if (mood == null) {
                                // First run only: the repository serves its cached copy before
                                // hitting the network, so this is never seen after the first fetch.
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    CenterLoadingBox(Modifier.fillMaxWidth().height(240.dp))
                                }
                            } else {
                                mood.sections.forEachIndexed { index, section ->
                                    val kind = moodKind(index, mood.sections.size)
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        // Section titles come from YouTube already localised.
                                        LibrarySectionHeader(icon = null, title = section.title, modifier = Modifier.padding(top = 8.dp))
                                    }
                                    if (kind == MoodKind.FOR_YOU) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                items(section.items, key = { it.params }) { item ->
                                                    // A LazyRow composes only the tiles on screen,
                                                    // so asking here IS the laziness.
                                                    LaunchedEffect(item.params) { searchViewModel.loadMoodArtwork(item.params) }
                                                    ForYouMoodTile(
                                                        title = item.title,
                                                        color = Color(item.stripeColor),
                                                        artworkUrl = moodArtwork[item.params],
                                                        modifier = Modifier.width(300.dp),
                                                    ) { navController.navigate(MoodDestination(item.params)) }
                                                }
                                            }
                                        }
                                    } else {
                                        // Key must include the section: every section lives in this
                                        // ONE grid, and "For you" repeats categories that also appear
                                        // under Moods or Genres, so params alone collides.
                                        itemsIndexed(section.items, key = { _, it -> "${section.title}/${it.params}" }) { i, item ->
                                            // LazyVerticalGrid only composes tiles inside the viewport,
                                            // so a category never scrolled to never costs a browse.
                                            LaunchedEffect(item.params) { searchViewModel.loadMoodArtwork(item.params) }
                                            val column = i % moodGridColumns
                                            val tileMargin =
                                                Modifier.padding(
                                                    start = if (column == 0) 16.dp else 0.dp,
                                                    end = if (column == moodGridColumns - 1) 16.dp else 0.dp,
                                                )
                                            val open = { navController.navigate(MoodDestination(item.params)) }
                                            if (kind == MoodKind.MOODS) {
                                                MoodsMoodTile(item.title, Color(item.stripeColor), moodArtwork[item.params], tileMargin, open)
                                            } else {
                                                GenreMoodTile(item.title, Color(item.stripeColor), moodArtwork[item.params], tileMargin, open)
                                            }
                                        }
                                    }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EndOfPage()
                            }
                        }
                    }
                }

                SearchUIType.SEARCH_RESULTS -> {
                    // Content area: the chips live in the blurred bar block above.
                    PullToRefreshBox(
                        modifier = Modifier.fillMaxSize(),
                        state = pullToRefreshState,
                        onRefresh = { searchText.trim().takeIf { it.isNotEmpty() }?.let(runSearch) },
                        isRefreshing = uiState is SearchScreenUIState.Loading,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullToRefreshState,
                                isRefreshing = uiState is SearchScreenUIState.Loading,
                                // Anchored to the top of the box, which starts under the bar:
                                // without this offset the spinner sits behind the bar.
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = searchBarHeight),
                                containerColor = PullToRefreshDefaults.indicatorContainerColor,
                                color = PullToRefreshDefaults.indicatorColor,
                                maxDistance = PullToRefreshDefaults.PositionalThreshold - 5.dp,
                            )
                        },
                    ) {
                        Crossfade(targetState = uiState) { uiState ->
                            when {
                                uiState is SearchScreenUIState.Loading -> {
                                    // Same top inset as the results, or the first row hides behind the bar.
                                    LazyColumn(contentPadding = PaddingValues(top = searchBarHeight, bottom = 10.dp)) {
                                        items(10) { ShimmerSearchItem() }
                                    }
                                }

                                uiState is SearchScreenUIState.Error -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.error_occurred),
                                            style = typo().titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(onClick = { if (searchText.isNotEmpty()) searchViewModel.search(searchText) }) {
                                            Text(text = stringResource(Res.string.retry))
                                        }
                                    }
                                }

                                uiState is SearchScreenUIState.Success && searchScreenState.hasResults() -> {
                                    val columns = searchScreenState.searchType.gridColumns(wide = !isMobilePortrait)
                                    // A card grid keeps Home's 6dp edge (its cards carry 10dp more); rows and sections bring their own 16dp.
                                    val side = if (columns > 1) 6.dp else 0.dp
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columns),
                                        state = resultsState,
                                        contentPadding = PaddingValues(start = side, end = side, top = listTop, bottom = 24.dp),
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(SearchTitleHeight)) }
                                        searchResults(
                                            state = searchScreenState,
                                            query = searchText,
                                            playingVideoId = currentVideoId,
                                            selection = selection,
                                            navController = navController,
                                            viewModel = searchViewModel,
                                            sharedViewModel = sharedViewModel,
                                            onSongMenu = { onMoreClick(it.toSongEntity()) },
                                            onSeeAll = showChip,
                                        )
                                    }
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.no_results_found),
                                            style = typo().titleMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, -titleCollapse().roundToInt()) }
                    .onGloballyPositioned { searchBarHeightPx = it.size.height },
        ) {
            // Animate only the background so scrolling keeps the search input and its focus.
            AnimatedVisibility(
                visible = isBarFrosted,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                label = "search_bar_scrim",
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle()),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(bottom = 10.dp),
            ) {
                Text(
                    text = stringResource(Res.string.search),
                    style = typo().headlineLarge.copy(fontSize = 30.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier =
                        Modifier
                            .height(SearchTitleHeight)
                            .padding(start = 16.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .graphicsLayer { alpha = 1f - titleCollapse() / titleHeightPx },
                )
        // Search Bar with Animated Placeholder
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = { newText ->
                        searchText = newText
                    },
                    onSearch = { query ->
                        // A pasted YouTube link is a destination, not a query. Translating it into
                        // the app's own deep link hands it to the same intent flow that handles
                        // shared links, so it plays or opens straight away instead of being
                        // searched for as text. Anything else falls through to a normal search.
                        val deepLink = query.toAppDeepLinkOrNull()
                        if (deepLink != null) {
                            focusManager.clearFocus()
                            sharedViewModel.setIntent(GenericIntent(data = deepLink))
                        } else if (query.isNotEmpty()) {
                            runSearch(query)
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    enabled = true,
                    placeholder = {
                        // Animated placeholder text
                        AnimatedContent(
                            targetState = currentPlaceholderIndex,
                            transitionSpec = {
                                (
                                    fadeIn(animationSpec = tween(500)) +
                                        slideInVertically { height -> height }
                                ).togetherWith(
                                    fadeOut(animationSpec = tween(500)) +
                                        slideOutVertically { height -> -height },
                                )
                            },
                            label = "placeholder_animation",
                        ) { index ->
                            Text(
                                text = placeholderTexts[index],
                                style = typo().labelMedium,
                            )
                        }
                    },
                    leadingIcon = {
                        Crossfade(
                            targetState = isSearchPanelVisible,
                            modifier = Modifier.size(48.dp),
                            animationSpec = tween(200),
                            label = "search_back_icon",
                        ) { showBack ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showBack) {
                                    IconButton(
                                        onClick = dismissSearchPanel,
                                        enabled = isSearchPanelVisible,
                                    ) {
                                        Icon(
                                            imageVector = LimusicIcons.ArrowBackIosNew,
                                            contentDescription = "Back",
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = LimusicIcons.Search,
                                        contentDescription = "Search",
                                    )
                                }
                            }
                        }
                    },
                    trailingIcon = {
                        // X button only shows when there's text
                        if (searchText.isNotEmpty()) {
                            IconButton(
                                modifier = Modifier.clip(CircleShape),
                                onClick = {
                                    searchText = ""
                                    isSearchSubmitted = false
                                },
                            ) {
                                Icon(
                                    imageVector = LimusicIcons.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }.padding(horizontal = 16.dp),
            // Limusic's pill, on the card colour like desktop's search field.
            shape = CircleShape,
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            // The Column above owns the status-bar inset; a second one here reserves it twice.
            windowInsets = WindowInsets(0),
            content = {},
        )
                // Filter chips ride along inside the blurred block instead of sitting in the
                // results branch. That way searchBarHeight covers them too, results scroll
                // underneath the whole thing, and the glass has something to blur.
                AnimatedVisibility(visible = searchUIType == SearchUIType.SEARCH_RESULTS) {
                    Row(
                        modifier =
                            Modifier
                                .horizontalScroll(chipRowState)
                                .padding(top = 10.dp)
                                .padding(horizontal = 12.dp),
                    ) {
                        SearchType.entries.forEach { id ->
                            val isSelected = id == searchScreenState.searchType
                            Spacer(modifier = Modifier.width(4.dp))
                            Chip(
                                isAnimated = uiState is SearchScreenUIState.Loading,
                                isSelected = isSelected,
                                text = stringResource(id.toStringRes()),
                            ) {
                                showChip(id)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
    // Picks belong to the results on screen: typing, or leaving the results, ends select mode.
    LaunchedEffect(searchUIType) { if (searchUIType != SearchUIType.SEARCH_RESULTS) selection.exit() }
    val results = searchScreenState.rowTracks()
    // A song can show twice under All (top result and Songs); it is one pick.
    val rows = remember(results) { keyedRows(results.distinctBy { it.videoId }) }
    // After the results, so the bar is drawn over them and takes its own taps.
    TrackSelectionBar(selection = selection, rows = rows, bottomPadding = innerPadding.calculateBottomPadding())
}

@Composable
fun SuggestItemRow(
    searchResult: SearchResultType,
    onItemClick: (SearchResultType) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onItemClick(searchResult) }
                .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val url =
            when (searchResult) {
                is SongsResult -> {
                    searchResult.thumbnails?.lastOrNull()?.url
                }

                is AlbumsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is ArtistsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is PlaylistsResult -> {
                    searchResult.thumbnails.lastOrNull()?.url
                }

                is VideosResult -> {
                    searchResult.thumbnails?.lastOrNull()?.url
                }

                else -> {
                    null
                }
            }

        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(rounded(4.dp)),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(url)
                        .crossfade(true)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            if (searchResult is ArtistsResult) {
                                CircleShape
                            } else {
                                rounded(4.dp)
                            },
                        ),
            )
        }

        Spacer(modifier = Modifier.padding(horizontal = 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val title =
                when (searchResult) {
                    is SongsResult -> {
                        searchResult.title
                    }

                    is AlbumsResult -> {
                        searchResult.title
                    }

                    is ArtistsResult -> {
                        searchResult.artist
                    }

                    is PlaylistsResult -> {
                        searchResult.title
                    }

                    is VideosResult -> {
                        searchResult.title
                    }

                    else -> {
                        null
                    }
                } ?: "Unknown"

            Text(
                text = title,
                style = typo().labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))

            val subtitle =
                when (searchResult) {
                    is SongsResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    is AlbumsResult -> searchResult.artists.map { it.name }.connectArtists()
                    is PlaylistsResult -> searchResult.author.ifEmpty { "YouTube Music" }
                    is ArtistsResult -> stringResource(Res.string.artists)
                    is VideosResult -> searchResult.artists?.map { it.name }?.connectArtists()
                    else -> null
                } ?: "Unknown"

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Desktop's recent searches: one sideways row of chips, newest first, each forgettable. */
@Composable
private fun RecentSearches(
    queries: List<String>,
    onSearch: (String) -> Unit,
    onForget: (String) -> Unit,
    onClear: () -> Unit,
) {
    // Desktop keeps one entry per spelling-insensitive query; the table is keyed on the exact text.
    val shown = remember(queries) { queries.distinctBy { it.lowercase() }.take(20) }
    Column {
        LibrarySectionHeader(
            icon = null,
            title = stringResource(Res.string.recent_searches),
            trailing = {
                Text(
                    text = stringResource(Res.string.clear),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onClear).padding(horizontal = 8.dp, vertical = 6.dp),
                )
            },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shown, key = { it }) { query ->
                Row(
                    modifier =
                        Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().clickable { onSearch(query) }.padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(LimusicIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = query,
                            style = typo().labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.padding(end = 4.dp).size(28.dp).clip(CircleShape).clickable { onForget(query) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(LimusicIcons.Close, contentDescription = stringResource(Res.string.clear), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private enum class MoodKind { FOR_YOU, MOODS, GENRES }

/**
 * Which section is which, by position like desktop: the titles are in the user's language and
 * "For you" only exists signed in. YouTube ends on Moods & moments, then Genres.
 */
private fun moodKind(
    index: Int,
    count: Int,
) = when (index) {
    count - 1 -> MoodKind.GENRES
    count - 2 -> MoodKind.MOODS
    else -> MoodKind.FOR_YOU
}

private val SearchTitleHeight = 56.dp

enum class SearchUIType {
    EMPTY,
    SEARCH_SUGGESTIONS,
    SEARCH_RESULTS,
}
