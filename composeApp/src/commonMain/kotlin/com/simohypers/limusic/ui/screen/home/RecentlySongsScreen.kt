package com.simohypers.limusic.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.expect.ui.PlatformBackHandler
import com.simohypers.limusic.expect.ui.isLyricsBlurSupported
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.Chip
import com.simohypers.limusic.ui.component.CollectionSearchRow
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.RoundAccentButton
import com.simohypers.limusic.ui.component.RoundIconButton
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.label
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.component.selection.TrackSelectionBar
import com.simohypers.limusic.ui.component.selection.rememberTrackSelection
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.History
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Pause
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.Shuffle
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.HistoryGroup
import com.simohypers.limusic.viewModel.HistorySource
import com.simohypers.limusic.viewModel.HistoryState
import com.simohypers.limusic.viewModel.RecentlySongsViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import kotlinx.coroutines.flow.map
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.history
import limusic.composeapp.generated.resources.history_empty_phone
import limusic.composeapp.generated.resources.history_empty_youtube
import limusic.composeapp.generated.resources.history_failed
import limusic.composeapp.generated.resources.history_matching
import limusic.composeapp.generated.resources.history_source_phone
import limusic.composeapp.generated.resources.history_source_youtube
import limusic.composeapp.generated.resources.history_subtitle_phone
import limusic.composeapp.generated.resources.history_subtitle_youtube
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.no_match_for
import limusic.composeapp.generated.resources.pause
import limusic.composeapp.generated.resources.play_all
import limusic.composeapp.generated.resources.retry
import limusic.composeapp.generated.resources.search
import limusic.composeapp.generated.resources.shuffle_all
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Desktop's History page (routes/history): a band of the last covers with Shuffle all and Play all,
 * then the history under pinned day headings. Two sources: the account's YouTube Music history and
 * what this phone played.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecentlySongsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: RecentlySongsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val source by viewModel.source.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val youtube by viewModel.youtube.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val isPlaying by remember { sharedViewModel.controllerState.map { it.isPlaying } }.collectAsStateWithLifecycle(false)
    // Read again each time the page shows, so a song played since is on top.
    LaunchedEffect(Unit) { viewModel.refreshPhone() }

    val state = if (source == HistorySource.YOUTUBE) youtube else if (source == null) HistoryState.Loading else phone
    val groups = (state as? HistoryState.Ready)?.groups.orEmpty()
    val all = remember(groups) { groups.flatMap { it.rows } }
    val tracks = remember(all) { all.map { it.second } }
    val indexOf = remember(all) { all.withIndex().associate { (i, row) -> row.first to i } }

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val filter = query.trim().takeIf { searching && it.isNotEmpty() }
    val shownGroups =
        remember(groups, filter) {
            if (filter == null) {
                groups
            } else {
                groups
                    .map { g -> g.copy(rows = g.rows.filter { (_, t) -> t.matches(filter) }) }
                    .filter { it.rows.isNotEmpty() }
            }
        }
    val shown = remember(shownGroups) { shownGroups.flatMap { it.rows } }
    val closeSearch = {
        searching = false
        query = ""
    }
    PlatformBackHandler(enabled = searching) { closeSearch() }

    val selection = rememberTrackSelection()
    // A pick is a row of one source's list; switching lists drops it.
    LaunchedEffect(source) { selection.exit() }
    var chosen by remember { mutableStateOf<Track?>(null) }
    val listState = rememberLazyListState()
    val today = remember { now().date }
    val historyTitle = stringResource(Res.string.history)
    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val historyPlaying = isPlaying && queueData?.data?.playlistId == null && queueData?.data?.playlistName == historyTitle

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(64.dp)) {
            Crossfade(searching, label = "historyBar") { open ->
                if (open) {
                    CollectionSearchRow(
                        query = query,
                        onQueryChange = { query = it },
                        onClose = closeSearch,
                        modifier = Modifier.padding(horizontal = 16.dp).height(64.dp),
                    )
                } else {
                    Row(Modifier.fillMaxSize().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        RippleIconButton(LimusicIcons.ArrowBackIosNew, Modifier.size(32.dp), true, MaterialTheme.colorScheme.onSurface) {
                            navController.navigateUp()
                        }
                        Text(
                            historyTitle,
                            style = typo().titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        IconButton(onClick = { searching = true }) {
                            Icon(LimusicIcons.Search, stringResource(Res.string.search), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (!searching) {
                item(key = "band", contentType = "band") {
                    HistoryBand(
                        covers = remember(tracks) { tracks.take(40).mapNotNull { it.thumbnails?.lastOrNull()?.url }.distinct().take(5) },
                        subtitle =
                            stringResource(
                                if (source == HistorySource.YOUTUBE) Res.string.history_subtitle_youtube else Res.string.history_subtitle_phone,
                            ),
                        enabled = tracks.isNotEmpty(),
                        playing = historyPlaying,
                        onShuffle = { viewModel.shuffle(tracks) },
                        onPlay = { if (historyPlaying) sharedViewModel.onUIEvent(UIEvent.PlayPause) else viewModel.play(tracks, 0) },
                    )
                }
            }
            if (signedIn) {
                item(key = "sources", contentType = "sources") {
                    // Well clear of the band's buttons, or the two rows of pills read as one cluster.
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Chip(isSelected = source == HistorySource.YOUTUBE, text = stringResource(Res.string.history_source_youtube)) {
                            viewModel.setSource(HistorySource.YOUTUBE)
                        }
                        Chip(isSelected = source == HistorySource.PHONE, text = stringResource(Res.string.history_source_phone)) {
                            viewModel.setSource(HistorySource.PHONE)
                        }
                    }
                }
            }
            if (filter != null && shown.isNotEmpty()) {
                item(key = "matching") {
                    Text(
                        pluralStringResource(Res.plurals.history_matching, shown.size, shown.size),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            when (state) {
                HistoryState.Loading ->
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            CenterLoadingBox(Modifier.size(80.dp))
                        }
                    }
                HistoryState.Failed ->
                    item(key = "failed") {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Message(stringResource(Res.string.history_failed))
                            TextButton(onClick = viewModel::retryYouTube) {
                                Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                is HistoryState.Ready ->
                    when {
                        groups.isEmpty() ->
                            item(key = "empty") {
                                Message(
                                    stringResource(
                                        if (source == HistorySource.YOUTUBE) Res.string.history_empty_youtube else Res.string.history_empty_phone,
                                    ),
                                    Modifier.padding(32.dp),
                                )
                            }
                        shownGroups.isEmpty() -> item(key = "no-match") { Message(stringResource(Res.string.no_match_for, filter.orEmpty()), Modifier.padding(32.dp)) }
                        else ->
                            shownGroups.forEachIndexed { i, group ->
                                stickyHeader(key = "day:$i:${group.title ?: group.bucket}", contentType = "day") {
                                    DayHeading(group.title ?: group.bucket?.label(today).orEmpty(), group.rows.size)
                                }
                                items(group.rows, key = { it.first }, contentType = { "song" }) { (key, track) ->
                                    // Wrapped: SongFullWidthItems puts its modifier on two nodes.
                                    Column(Modifier.animateItem()) {
                                        SongFullWidthItems(
                                            track = track,
                                            isPlaying = isPlaying && track.videoId == nowPlayingVideoId,
                                            onMoreClickListener = { chosen = track },
                                            onClickListener = { viewModel.play(tracks, indexOf[key] ?: 0) },
                                            onAddToQueue = { sharedViewModel.addListToQueue(arrayListOf(track)) },
                                            selectionMode = selection.active,
                                            isSelected = selection.has(key),
                                            onLongClick = { selection.start(key, track) },
                                            onSelectToggle = { selection.toggle(key, track) },
                                            modifier = Modifier,
                                            padding = PaddingValues(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        )
                                    }
                                }
                            }
                    }
            }
            item(key = "end") { EndOfPage() }
        }
    }
    // Opening and closing search both start from the top (the band comes and goes as item 0);
    // not on the first run, or coming back to the page would lose its scroll.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(searching) {
        if (settled) listState.scrollToItem(0)
        settled = true
    }

    TrackSelectionBar(
        selection = selection,
        rows = all,
        bottomPadding = innerPadding.calculateBottomPadding(),
        shown = shown,
        from = historyTitle,
    )
    chosen?.let { track ->
        NowPlayingBottomSheet(onDismiss = { chosen = null }, navController = navController, song = track.toSongEntity())
    }
}

private fun Track.matches(query: String) =
    title.contains(query, ignoreCase = true) || artists?.any { it.name.contains(query, ignoreCase = true) } == true

/**
 * Desktop's band: a blur of the last cover washed into the page colour from the left, the last
 * covers overlapping, and the two play buttons.
 */
@Composable
private fun HistoryBand(
    covers: List<String>,
    subtitle: String,
    enabled: Boolean,
    playing: Boolean,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val shape = rounded(20.dp)
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape),
    ) {
        // Below Android 12 there is no blur, and a sharp cover behind the text is just noise.
        if (covers.isNotEmpty() && isLyricsBlurSupported()) {
            AsyncImage(
                // Blurred this hard, 96px looks the same as the full cover.
                model = ImageRequest.Builder(LocalPlatformContext.current).data(covers.first()).size(96).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().scale(1.1f).blur(40.dp).alpha(0.6f),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(background, background.copy(alpha = 0.8f), background.copy(alpha = 0.4f)))),
        )
        Column(Modifier.padding(16.dp)) {
            if (covers.isNotEmpty()) {
                CoverStrip(covers)
            } else {
                Box(
                    Modifier.size(64.dp).clip(rounded(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LimusicIcons.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
            }
            // The album pages' round buttons rather than labelled pills: the source chips below are
            // pills, and two rows of them read as one cluster.
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subtitle,
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                if (enabled) {
                    RoundIconButton(LimusicIcons.Shuffle, stringResource(Res.string.shuffle_all), onShuffle)
                    Spacer(Modifier.width(4.dp))
                    RoundAccentButton(
                        if (playing) LimusicIcons.Pause else LimusicIcons.PlayArrow,
                        stringResource(if (playing) Res.string.pause else Res.string.play_all),
                        onPlay,
                    )
                }
            }
        }
    }
}

/** The last few sleeves, overlapping like a fanned stack: history has no artwork of its own. */
@Composable
private fun CoverStrip(covers: List<String>) {
    val ring = MaterialTheme.colorScheme.background
    val holder = rememberHolderPainter()
    Row(horizontalArrangement = Arrangement.spacedBy((-20).dp)) {
        covers.forEachIndexed { i, cover ->
            AsyncImage(
                model = cover,
                placeholder = holder,
                error = holder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .zIndex((covers.size - i).toFloat())
                        .size(64.dp)
                        .shadow(6.dp, rounded(12.dp))
                        .border(2.dp, ring, rounded(12.dp))
                        .clip(rounded(12.dp)),
            )
        }
    }
}

/** Pinned while its day scrolls by: the day, a hairline, and how many songs it holds. */
@Composable
private fun DayHeading(
    title: String,
    count: Int,
) {
    // Opaque and drawn over the rows: an animated row passing under it would otherwise show through.
    Row(
        Modifier
            .zIndex(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = typo().headlineMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.weight(1f).padding(horizontal = 12.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Text(
            pluralStringResource(Res.plurals.liked_songs_count, count, count),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Message(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = typo().bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
