package com.simohypers.limusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.common.Config.REMOVED_SONG_DATE_TIME
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.expect.ui.PlatformBackHandler
import com.simohypers.limusic.extension.barBlurStyle
import com.simohypers.limusic.ui.component.AudioPlayingIndicator
import com.simohypers.limusic.ui.component.CollectionHero
import com.simohypers.limusic.ui.component.CollectionSearchRow
import com.simohypers.limusic.ui.component.dateBucket
import com.simohypers.limusic.ui.component.label
import com.simohypers.limusic.ui.component.CollectionTitle
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.PlaylistBottomSheet
import com.simohypers.limusic.ui.component.PlaylistSortSheet
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.SaveTracksSheet
import com.simohypers.limusic.ui.component.SearchBarEnter
import com.simohypers.limusic.ui.component.SearchBarExit
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.pillSurface
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.component.selection.TrackSelection
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.keyedRows
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.DownloadForOffline
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.PeopleAlt
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.Sort
import com.simohypers.limusic.ui.icon.TrendingUp
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.DOWNLOAD_SORTS
import com.simohypers.limusic.viewModel.LibraryDynamicPlaylistViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.artists_count
import limusic.composeapp.generated.resources.downloaded_songs
import limusic.composeapp.generated.resources.duration_hours_short
import limusic.composeapp.generated.resources.duration_minutes_short
import limusic.composeapp.generated.resources.empty_artist_liked_hint
import limusic.composeapp.generated.resources.empty_artist_liked_title
import limusic.composeapp.generated.resources.empty_downloads_hint
import limusic.composeapp.generated.resources.empty_downloads_title
import limusic.composeapp.generated.resources.empty_followed_hint
import limusic.composeapp.generated.resources.empty_followed_title
import limusic.composeapp.generated.resources.empty_liked_hint
import limusic.composeapp.generated.resources.empty_liked_title
import limusic.composeapp.generated.resources.empty_most_played_hint
import limusic.composeapp.generated.resources.empty_most_played_title
import limusic.composeapp.generated.resources.followed
import limusic.composeapp.generated.resources.kicker_collection
import limusic.composeapp.generated.resources.kicker_on_this_phone
import limusic.composeapp.generated.resources.kicker_your_top_songs
import limusic.composeapp.generated.resources.liked_songs
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.most_played
import limusic.composeapp.generated.resources.no_match_for
import limusic.composeapp.generated.resources.play_count
import limusic.composeapp.generated.resources.search
import limusic.composeapp.generated.resources.sort_by
import limusic.composeapp.generated.resources.sort_recently_downloaded
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Liked songs, Downloaded songs, Most played, Followed and one artist's liked songs: the album page's
 * hero over a mosaic of the list's newest covers, then the list. Most played reads as a chart,
 * liked songs fall under when they were liked, followed artists sit in a grid.
 */
@Composable
@ExperimentalMaterial3Api
fun LibraryDynamicPlaylistScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: String,
    viewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val listType = remember(type) { LibraryDynamicPlaylistType.toType(type) }
    LaunchedEffect(listType) { viewModel.open(listType) }
    if (listType == LibraryDynamicPlaylistType.Followed) {
        FollowedPage(navController, viewModel)
    } else {
        SongsPage(listType, innerPadding, navController, viewModel, sharedViewModel)
    }
}

/** One song of the list: its [key] for selection, and its place in the whole list, from 1. */
private class SongRow(
    val key: String,
    val song: SongEntity,
    val track: Track,
    val rank: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsPage(
    type: LibraryDynamicPlaylistType,
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LibraryDynamicPlaylistViewModel,
    sharedViewModel: SharedViewModel,
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val bytes by viewModel.downloadedBytes.collectAsStateWithLifecycle()
    val sort by viewModel.downloadSort.collectAsStateWithLifecycle()
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val isPlaying by remember { sharedViewModel.controllerState.map { it.isPlaying } }.collectAsStateWithLifecycle(false)
    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()

    val list = songs.orEmpty()
    val rows =
        remember(list) {
            keyedRows(list.map { it.toTrack() }).mapIndexed { i, (key, track) -> SongRow(key, list[i], track, i + 1) }
        }
    val chrome = rememberPageChrome()
    val shown =
        remember(rows, chrome.filter) {
            val q = chrome.filter ?: return@remember rows
            rows.filter { row -> row.song.title.contains(q, true) || row.song.artistName?.any { it.contains(q, true) } == true }
        }
    val selection = rememberTrackSelection()
    val allPairs = remember(rows) { rows.map { it.key to it.track } }
    val shownPairs = remember(shown) { shown.map { it.key to it.track } }

    val title = type.title(artist)
    val queueName = remember(type, artist?.name) { viewModel.queueName(type) }
    val thisPlaying = isPlaying && queueData?.data?.playlistId == null && queueData?.data?.playlistName == queueName
    var chosen by remember { mutableStateOf<SongEntity?>(null) }
    var menu by remember { mutableIntStateOf(0) }
    var sorting by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf<List<Track>?>(null) }

    val play = { videoId: String -> viewModel.playSong(videoId, type) }
    val rowContent: @Composable LazyItemScope.(SongRow) -> Unit = { row ->
        SongItem(
            row = row,
            playing = isPlaying && row.song.videoId == nowPlayingVideoId,
            selection = selection,
            ranked = type == LibraryDynamicPlaylistType.MostPlayed,
            onPlay = play,
            onMore = { chosen = row.song },
            onAddToQueue = { sharedViewModel.addListToQueue(arrayListOf(row.track)) },
        )
    }

    PageScaffold(
        title = title,
        chrome = chrome,
        onBack = { navController.navigateUp() },
    ) { hazeState ->
        LazyColumn(
            state = chrome.listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).hazeSource(hazeState),
        ) {
            if (!chrome.searching) {
                item(key = "hero", contentType = "hero") {
                    Column {
                        CollectionHero(
                            thumbnail = null,
                            onBack = { navController.navigateUp() },
                            art = {
                                val photo = artist?.thumbnails
                                if (type is LibraryDynamicPlaylistType.ArtistLiked && photo != null) {
                                    Cover(photo.sized(544), Modifier.fillMaxSize())
                                } else {
                                    HeroArt(
                                        // The newest downloads whatever the sort: a Title sort would
                                        // otherwise put whatever starts with a quote mark up there.
                                        covers =
                                            remember(list) {
                                                val newest =
                                                    if (type == LibraryDynamicPlaylistType.Downloaded) {
                                                        list.sortedByDescending { it.downloadedAt ?: REMOVED_SONG_DATE_TIME }
                                                    } else {
                                                        list
                                                    }
                                                newest.mapNotNull { it.thumbnails }.distinct().take(4)
                                            },
                                        icon = type.icon(),
                                        loading = songs == null,
                                    )
                                }
                            },
                            actions = {
                                ActionsPill(
                                    onSearch = chrome::openSearch,
                                    onSort = if (type == LibraryDynamicPlaylistType.Downloaded) ({ sorting = true }) else null,
                                    sorted = !sort.isDefault,
                                    onMore = if (list.isNotEmpty()) ({ menu++ }) else null,
                                )
                            },
                        ) {
                            CollectionTitle(
                                kicker = stringResource(type.kicker()),
                                title = title,
                                playing = thisPlaying,
                                onPlay =
                                    if (list.isEmpty()) {
                                        null
                                    } else {
                                        { if (thisPlaying) sharedViewModel.onUIEvent(UIEvent.PlayPause) else viewModel.playAll(type) }
                                    },
                                onShuffle = { viewModel.shuffle(type) },
                            )
                            MetaLine(
                                listOfNotNull(
                                    pluralStringResource(Res.plurals.liked_songs_count, list.size, list.size),
                                    if (type == LibraryDynamicPlaylistType.MostPlayed) {
                                        val plays = list.sumOf { it.totalPlayTime }.toInt()
                                        pluralStringResource(Res.plurals.play_count, plays, plays)
                                    } else {
                                        lengthText(list.sumOf { it.durationSeconds.toLong() })
                                    },
                                    if (type == LibraryDynamicPlaylistType.Downloaded && bytes > 0) bytes.sizeText() else null,
                                ),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            } else {
                item(key = "search-space") { SearchSpace(chrome) }
            }
            when {
                // Loading: the database answers in a frame or two; an empty state would flash.
                songs == null -> {}
                list.isEmpty() -> item(key = "empty") { EmptyState(type.emptyTitle(), type.emptyHint()) }
                shown.isEmpty() -> item(key = "no-match") { NoMatch(chrome.query) }
                type == LibraryDynamicPlaylistType.MostPlayed -> {
                    items(shown, key = { it.key }, contentType = { if (it.rank == 1) "top" else "song" }) { row ->
                        // The top card can't show a tick, so it becomes a row while picking.
                        if (row.rank == 1 && !selection.active) {
                            TopSongCard(
                                row = row,
                                playing = isPlaying && row.song.videoId == nowPlayingVideoId,
                                onPlay = { play(row.song.videoId) },
                                onLongClick = { selection.start(row.key, row.track) },
                                onMore = { chosen = row.song },
                                modifier = Modifier.animateItem(),
                            )
                        } else {
                            rowContent(row)
                        }
                    }
                }
                type == LibraryDynamicPlaylistType.Favorite || type is LibraryDynamicPlaylistType.ArtistLiked ->
                    likedSections(shown, rowContent)
                else -> items(shown, key = { it.key }, contentType = { "song" }) { rowContent(it) }
            }
            item(key = "end") { EndOfPage() }
        }
    }

    TrackSelectionBar(
        selection = selection,
        rows = allPairs,
        bottomPadding = innerPadding.calculateBottomPadding(),
        shown = shownPairs,
        from = title,
    )
    chosen?.let { song ->
        NowPlayingBottomSheet(onDismiss = { chosen = null }, navController = navController, song = song)
    }
    if (menu > 0) {
        val tracks = allPairs.map { it.second }
        PlaylistBottomSheet(
            onDismiss = { menu = 0 },
            shareLink = null,
            onPlayNext = { sharedViewModel.playListNext(tracks, title) },
            onAddToQueue = { sharedViewModel.addListToQueue(tracks, title) },
            onStartRadio = null,
            onSaveToPlaylist = { saving = tracks },
            onPlay = { viewModel.playAll(type) },
            onShufflePlay = { viewModel.shuffle(type) },
            onSelectTracks = selection::enter,
        )
    }
    saving?.let { SaveTracksSheet(it, onDismiss = { saving = null }) }
    if (sorting) {
        PlaylistSortSheet(
            sort = sort.sort,
            desc = sort.desc,
            onSort = {
                viewModel.setDownloadSort(sort.copy(sort = it))
                sorting = false
            },
            onReverse = { viewModel.setDownloadSort(sort.copy(desc = it)) },
            onDismiss = { sorting = false },
            options = DOWNLOAD_SORTS,
            defaultLabel = Res.string.sort_recently_downloaded,
        )
    }
}

/** Liked songs under "This week", "This month", then each month back, in liked order. */
private fun LazyListScope.likedSections(
    rows: List<SongRow>,
    rowContent: @Composable LazyItemScope.(SongRow) -> Unit,
) {
    val today = now().date
    var start = 0
    while (start < rows.size) {
        val bucket = dateBucket(rows[start].song.favoriteAt, today)
        var end = start + 1
        while (end < rows.size && dateBucket(rows[end].song.favoriteAt, today) == bucket) end++
        item(key = "when:$bucket", contentType = "label") { SectionLabel(bucket.label(today)) }
        items(rows.subList(start, end), key = { it.key }, contentType = { "song" }) { rowContent(it) }
        start = end
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = typo().bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
    )
}

@Composable
private fun LazyItemScope.SongItem(
    row: SongRow,
    playing: Boolean,
    selection: TrackSelection,
    ranked: Boolean,
    onPlay: (String) -> Unit,
    onMore: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    // Wrapped: SongFullWidthItems puts its modifier on two nodes.
    Column(Modifier.animateItem()) {
        SongFullWidthItems(
            songEntity = row.song,
            isPlaying = playing,
            onMoreClickListener = { onMore() },
            onClickListener = onPlay,
            onAddToQueue = { onAddToQueue() },
            selectionMode = selection.active,
            isSelected = selection.has(row.key),
            onLongClick = { selection.start(row.key, row.track) },
            onSelectToggle = { selection.toggle(row.key, row.track) },
            modifier = Modifier,
            padding = PaddingValues(start = if (ranked) 8.dp else 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            leadingView = if (ranked) ({ Rank(row.rank) }) else null,
            rightView =
                if (ranked) {
                    {
                        val plays = row.song.totalPlayTime.toInt()
                        Text(
                            pluralStringResource(Res.plurals.play_count, plays, plays),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                } else {
                    null
                },
        )
    }
}

/** A chart position: the top three in the accent, the rest quieter. */
@Composable
private fun Rank(rank: Int) {
    Text(
        text = rank.toString().padStart(2, '0'),
        style = typo().titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        color = if (rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(32.dp).padding(end = 4.dp),
    )
}

/** Most played's number one, as desktop's stats page shows its top artist. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopSongCard(
    row: SongRow,
    playing: Boolean,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val card = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier =
            modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clip(rounded(20.dp))
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.22f).compositeOver(card), card)))
                .combinedClickable(onClick = onPlay, onLongClick = onLongClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(104.dp).clip(rounded(14.dp))) {
            Cover(row.song.thumbnails, Modifier.fillMaxSize())
            if (playing) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
                    AudioPlayingIndicator(Modifier.fillMaxSize().padding(30.dp), color = Color.White)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "01",
                style = typo().titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = 1.sp),
                color = accent,
            )
            Text(
                row.song.title,
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.song.artistName?.connectArtists().orEmpty(),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val plays = row.song.totalPlayTime.toInt()
            Text(
                pluralStringResource(Res.plurals.play_count, plays, plays),
                style = typo().labelSmall,
                color = accent,
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        RippleIconButton(imageVector = LimusicIcons.MoreVert, fillMaxSize = false, tint = MaterialTheme.colorScheme.onSurface, onClick = onMore)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowedPage(
    navController: NavController,
    viewModel: LibraryDynamicPlaylistViewModel,
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val list = artists.orEmpty()
    val chrome = rememberPageChrome()
    val shown =
        remember(list, chrome.filter) {
            val q = chrome.filter ?: return@remember list
            list.filter { it.name.contains(q, true) }
        }
    val title = stringResource(Res.string.followed)
    PageScaffold(title = title, chrome = chrome, onBack = { navController.navigateUp() }) { hazeState ->
        LazyColumn(
            state = chrome.listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).hazeSource(hazeState),
        ) {
            if (!chrome.searching) {
                item(key = "hero", contentType = "hero") {
                    Column {
                        CollectionHero(
                            thumbnail = null,
                            onBack = { navController.navigateUp() },
                            art = {
                                HeroArt(
                                    covers = remember(list) { list.mapNotNull { it.thumbnails?.sized(544) }.distinct().take(4) },
                                    icon = LimusicIcons.PeopleAlt,
                                    loading = artists == null,
                                )
                            },
                            actions = { ActionsPill(onSearch = chrome::openSearch, onSort = null, sorted = false, onMore = null) },
                        ) {
                            CollectionTitle(kicker = stringResource(Res.string.artists), title = title, playing = false, onPlay = null, onShuffle = null)
                            MetaLine(listOf(pluralStringResource(Res.plurals.artists_count, list.size, list.size)))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            } else {
                item(key = "search-space") { SearchSpace(chrome) }
            }
            when {
                artists == null -> {}
                list.isEmpty() -> item(key = "empty") { EmptyState(Res.string.empty_followed_title, Res.string.empty_followed_hint) }
                shown.isEmpty() -> item(key = "no-match") { NoMatch(chrome.query) }
                // Rows of three rather than a grid, so the hero above stays edge to edge.
                else ->
                    items(shown.chunked(3), key = { it.first().channelId }, contentType = { "artists" }) { three ->
                        Row(
                            Modifier.animateItem().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            three.forEach { artist ->
                                ArtistCell(artist, Modifier.weight(1f)) { navController.navigate(ArtistDestination(channelId = artist.channelId)) }
                            }
                            repeat(3 - three.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
            }
            item(key = "end") { EndOfPage() }
        }
    }
}

@Composable
private fun ArtistCell(
    artist: ArtistEntity,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clip(rounded(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Cover(artist.thumbnails?.sized(360), Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape))
        Text(
            artist.name,
            style = typo().labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * The hero's picture: four newest covers in a square-ish mosaic, one cover when there are fewer,
 * and the list's icon over an accent wash while it is empty.
 */
@Composable
private fun HeroArt(
    covers: List<String>,
    icon: ImageVector,
    loading: Boolean,
) {
    when {
        loading -> {}
        covers.size >= 4 ->
            Column(Modifier.fillMaxSize()) {
                for (r in 0..1) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..1) Cover(covers[r * 2 + c], Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        covers.isNotEmpty() -> Cover(covers.first(), Modifier.fillMaxSize())
        else -> {
            val accent = MaterialTheme.colorScheme.primary
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent.copy(alpha = 0.55f), modifier = Modifier.offset(y = (-40).dp).size(120.dp))
            }
        }
    }
}

@Composable
private fun Cover(
    url: String?,
    modifier: Modifier,
) {
    val holder = rememberHolderPainter()
    AsyncImage(
        model = url,
        placeholder = holder,
        error = holder,
        fallback = holder,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** Search, sort (Downloaded songs only, in the accent while it isn't the default) and ⋮. */
@Composable
private fun ActionsPill(
    onSearch: () -> Unit,
    onSort: (() -> Unit)?,
    sorted: Boolean,
    onMore: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.height(48.dp).pillSurface(rounded(24.dp)).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = MaterialTheme.colorScheme.onSurface
        IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
            Icon(LimusicIcons.Search, stringResource(Res.string.search), tint = ink)
        }
        if (onSort != null) {
            IconButton(onClick = onSort, modifier = Modifier.size(40.dp)) {
                Icon(LimusicIcons.Sort, stringResource(Res.string.sort_by), tint = if (sorted) MaterialTheme.colorScheme.primary else ink)
            }
        }
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(40.dp)) {
                Icon(LimusicIcons.MoreVert, "More", tint = ink)
            }
        }
    }
}

@Composable
private fun MetaLine(parts: List<String>) {
    Text(
        text = parts.joinToString(" • "),
        style = typo().bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyState(
    title: StringResource,
    hint: StringResource,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = typo().titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Text(
            stringResource(hint),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun NoMatch(query: String) {
    Text(
        stringResource(Res.string.no_match_for, query.trim()),
        style = typo().bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}

@Composable
private fun lengthText(seconds: Long): String? {
    if (seconds <= 0) return null
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return listOfNotNull(
        if (h > 0) stringResource(Res.string.duration_hours_short, h) else null,
        if (m > 0 || h == 0) stringResource(Res.string.duration_minutes_short, m) else null,
    ).joinToString(" ")
}

/** A YouTube image URL asked for at [px] square, so a small stored thumbnail doesn't blur. */
private fun String.sized(px: Int) = replace(Regex("=w\\d+-h\\d+"), "=w$px-h$px")

// The page's top: the hero scrolls away into a blurred bar with the title, and search swaps both
// for a field. Same behaviour as the playlist page.

@Stable
private class PageChrome(
    val listState: LazyListState,
    searchingState: MutableState<Boolean>,
    queryState: MutableState<String>,
) {
    var searching by searchingState
    var query by queryState
    var searchHeightPx by mutableIntStateOf(0)

    /** The text to filter by, or null while nothing is typed. */
    val filter: String? get() = query.trim().takeIf { searching && it.isNotEmpty() }

    fun openSearch() {
        searching = true
    }

    fun closeSearch() {
        searching = false
        query = ""
    }
}

/** Saved, so the scroll and a search are still there when back comes from an artist or a song. */
@Composable
private fun rememberPageChrome(): PageChrome {
    val listState = rememberLazyListState()
    val searching = rememberSaveable { mutableStateOf(false) }
    val query = rememberSaveable { mutableStateOf("") }
    val chrome = remember { PageChrome(listState, searching, query) }
    // Opening and closing search both start from the top: the hero comes and goes as item 0, and
    // the list would otherwise hold on to whichever song row was first. Not on the first run, or
    // coming back to the page would lose its saved scroll.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(chrome.searching) {
        if (settled) chrome.listState.scrollToItem(0)
        settled = true
    }
    return chrome
}

@Composable
private fun SearchSpace(chrome: PageChrome) {
    Spacer(Modifier.height(with(LocalDensity.current) { chrome.searchHeightPx.toDp() }))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageScaffold(
    title: String,
    chrome: PageChrome,
    onBack: () -> Unit,
    content: @Composable (HazeState) -> Unit,
) {
    val hazeState = rememberHazeState()
    val background = MaterialTheme.colorScheme.background
    val ink = MaterialTheme.colorScheme.onSurface
    // The bar comes in once the hero's foot (the title) reaches it, not only after the whole hero
    // has gone: a title scrolling under the status bar with nothing behind it reads as broken.
    val density = LocalDensity.current
    // Status bar + the bar's 64dp + about the title block's height under the hero.
    val threshold = with(density) { WindowInsets.statusBars.getTop(this) + 160.dp.roundToPx() }
    val pastHero by remember(threshold) {
        derivedStateOf {
            val state = chrome.listState
            val hero = state.layoutInfo.visibleItemsInfo.firstOrNull()
            state.firstVisibleItemIndex > 0 || (hero != null && hero.size - state.firstVisibleItemScrollOffset < threshold)
        }
    }
    PlatformBackHandler(enabled = chrome.searching) { chrome.closeSearch() }

    Box(Modifier.fillMaxSize()) {
        content(hazeState)
        AnimatedVisibility(
            visible = pastHero && !chrome.searching,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.exclude(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start)),
                title = { Text(title, style = typo().titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(LimusicIcons.ArrowBackIosNew, Modifier.size(32.dp), true, ink, onClick = onBack)
                    }
                },
                actions = {
                    IconButton(onClick = chrome::openSearch) { Icon(LimusicIcons.Search, stringResource(Res.string.search), tint = ink) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
            )
        }
        AnimatedVisibility(visible = chrome.searching, enter = SearchBarEnter, exit = SearchBarExit) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { chrome.searchHeightPx = it.size.height }
                    .hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(background, 0.55f)),
            ) {
                CollectionSearchRow(
                    query = chrome.query,
                    onQueryChange = { chrome.query = it },
                    onClose = chrome::closeSearch,
                    modifier = Modifier.padding(16.dp).windowInsetsPadding(WindowInsets.statusBars),
                )
            }
        }
    }
}

sealed class LibraryDynamicPlaylistType {
    data object Favorite : LibraryDynamicPlaylistType()

    data object Followed : LibraryDynamicPlaylistType()

    data object MostPlayed : LibraryDynamicPlaylistType()

    data object Downloaded : LibraryDynamicPlaylistType()

    /**
     * Liked songs crediting one artist — opened from the "Liked songs" row on that artist's page
     * (issue #2524).
     *
     * Carries only the channel id: the header's artist name is looked up by the view model, since a
     * name can hold any character and would have to survive the route string.
     */
    data class ArtistLiked(
        val channelId: String,
    ) : LibraryDynamicPlaylistType()

    /** The fixed name, for the callers that resolve a resource without composition. */
    fun name(): StringResource =
        when (this) {
            Favorite -> Res.string.liked_songs
            Followed -> Res.string.followed
            MostPlayed -> Res.string.most_played
            Downloaded -> Res.string.downloaded_songs
            is ArtistLiked -> Res.string.liked_songs
        }

    /** The header's title: an artist's liked songs are titled by the artist. */
    @Composable
    fun title(artist: ArtistEntity? = null): String =
        if (this is ArtistLiked && artist != null) artist.name else stringResource(name())

    fun kicker(): StringResource =
        when (this) {
            Downloaded -> Res.string.kicker_on_this_phone
            MostPlayed -> Res.string.kicker_your_top_songs
            is ArtistLiked -> Res.string.liked_songs
            Followed -> Res.string.artists
            Favorite -> Res.string.kicker_collection
        }

    fun icon(): ImageVector =
        when (this) {
            Downloaded -> LimusicIcons.DownloadForOffline
            MostPlayed -> LimusicIcons.TrendingUp
            Followed -> LimusicIcons.PeopleAlt
            Favorite, is ArtistLiked -> LimusicIcons.Favorite
        }

    fun emptyTitle(): StringResource =
        when (this) {
            Downloaded -> Res.string.empty_downloads_title
            MostPlayed -> Res.string.empty_most_played_title
            is ArtistLiked -> Res.string.empty_artist_liked_title
            Followed -> Res.string.empty_followed_title
            Favorite -> Res.string.empty_liked_title
        }

    fun emptyHint(): StringResource =
        when (this) {
            Downloaded -> Res.string.empty_downloads_hint
            MostPlayed -> Res.string.empty_most_played_hint
            is ArtistLiked -> Res.string.empty_artist_liked_hint
            Followed -> Res.string.empty_followed_hint
            Favorite -> Res.string.empty_liked_hint
        }

    // For serialization and navigation
    fun toStringParams(): String =
        when (this) {
            Favorite -> "favorite"
            Followed -> "followed"
            MostPlayed -> "most_played"
            Downloaded -> "downloaded"
            is ArtistLiked -> ARTIST_LIKED_PREFIX + channelId
        }

    companion object {
        private const val ARTIST_LIKED_PREFIX = "artist_liked_"

        fun toType(input: String): LibraryDynamicPlaylistType =
            when (input) {
                "favorite" -> Favorite
                "followed" -> Followed
                "most_played" -> MostPlayed
                "downloaded" -> Downloaded
                else ->
                    parseArtistLiked(input)
                        ?: throw IllegalArgumentException("Unknown type: $input")
            }

        /**
         * `artist_liked_UC…` back into an [ArtistLiked], or null. The remainder is taken whole —
         * channel ids contain `_` and `-` — and only has to be non-blank: an id matching no song
         * just opens an empty list.
         */
        private fun parseArtistLiked(input: String): ArtistLiked? =
            input
                .takeIf { it.startsWith(ARTIST_LIKED_PREFIX) }
                ?.removePrefix(ARTIST_LIKED_PREFIX)
                ?.takeIf { it.isNotBlank() }
                ?.let(::ArtistLiked)
    }
}
