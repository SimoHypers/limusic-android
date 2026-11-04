package com.simohypers.limusic.ui.screen.library

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.download.DownloadQueue
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.type.ArtistType
import com.simohypers.limusic.domain.utils.LocalResource
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.extension.bytesToMB
import com.simohypers.limusic.extension.isScrollingUp
import com.simohypers.limusic.ui.component.ArtistFullWidthItems
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.icon.ArrowForwardIos
import com.simohypers.limusic.ui.icon.DownloadForOffline
import com.simohypers.limusic.ui.icon.Downloading
import com.simohypers.limusic.ui.icon.Error
import com.simohypers.limusic.ui.icon.LibraryMusic
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.theme.typo
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.checking_library
import limusic.composeapp.generated.resources.downloaded_songs
import limusic.composeapp.generated.resources.downloading_progress
import limusic.composeapp.generated.resources.downloads_blocked_at
import limusic.composeapp.generated.resources.downloads_failed_now
import limusic.composeapp.generated.resources.downloads_missing
import limusic.composeapp.generated.resources.downloads_paused_at
import limusic.composeapp.generated.resources.downloads_waiting_network
import limusic.composeapp.generated.resources.downloads_waiting_wifi
import limusic.composeapp.generated.resources.keep_library_downloaded
import limusic.composeapp.generated.resources.keep_library_downloaded_description
import limusic.composeapp.generated.resources.library_load_failed
import limusic.composeapp.generated.resources.library_subscriptions
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.no_library_artists
import limusic.composeapp.generated.resources.no_library_songs
import limusic.composeapp.generated.resources.no_saved_artists
import limusic.composeapp.generated.resources.no_subscriptions
import limusic.composeapp.generated.resources.pause
import limusic.composeapp.generated.resources.resume
import limusic.composeapp.generated.resources.retry

/**
 * Library's Artists tab, split like YouTube Music's: the artists behind your songs, or the ones you
 * subscribe to. Signed out there is only the second kind, the follows on this phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsTab(
    navController: NavController,
    contentPadding: PaddingValues,
    signedIn: Boolean,
    artists: LocalResource<List<ArtistType>>,
    subscriptions: LocalResource<List<ArtistType>>,
    onScrolling: (onTop: Boolean) -> Unit,
    onReload: () -> Unit,
) {
    var subscribed by rememberSaveable { mutableStateOf(false) }
    val showing = if (signedIn && !subscribed) artists else subscriptions
    val empty =
        when {
            !signedIn -> Res.string.no_saved_artists
            subscribed -> Res.string.no_subscriptions
            else -> Res.string.no_library_artists
        }
    LibraryList(contentPadding, showing, empty, onScrolling, onReload) { list ->
        if (signedIn) {
            item {
                SecondaryTabRow(
                    selectedTabIndex = if (subscribed) 1 else 0,
                    containerColor = Color.Transparent,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
                ) {
                    listOf(Res.string.artists, Res.string.library_subscriptions).forEachIndexed { i, label ->
                        Tab(
                            selected = subscribed == (i == 1),
                            onClick = { subscribed = i == 1 },
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            // typo() styles carry a colour, so the tab's own colours are passed on.
                            text = { Text(stringResource(label), style = typo().titleSmall, color = LocalContentColor.current) },
                        )
                    }
                }
            }
        }
        items(list, key = { it.channelId() }) { artist ->
            ArtistFullWidthItems(
                artist,
                subtitle = (artist as? ArtistsResult)?.category?.takeIf { it.isNotEmpty() },
                onClickListener = { navController.navigate(ArtistDestination(channelId = artist.channelId())) },
            )
        }
    }
}

private fun ArtistType.channelId() =
    when (this) {
        is ArtistEntity -> channelId
        is ArtistsResult -> browseId
        else -> ""
    }

/** Library ▸ Songs signed in (paged as it scrolls), the phone's liked songs signed out. */
@Composable
fun LibrarySongsTab(
    navController: NavController,
    contentPadding: PaddingValues,
    songs: LocalResource<List<Track>>,
    playingVideoId: String?,
    onPlay: (videoId: String) -> Unit,
    onLoadMore: () -> Unit,
    onScrolling: (onTop: Boolean) -> Unit,
    onReload: () -> Unit,
) {
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val state = rememberLazyListState()
    LaunchedEffect(state) {
        // With the count in the key, a page that lands while the end is still on screen asks again.
        snapshotFlow {
            state.layoutInfo.let { ((it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 5) to it.totalItemsCount }
        }.distinctUntilChanged()
            .collect { (nearEnd, count) -> if (nearEnd && count > 0) onLoadMore() }
    }
    LibraryList(contentPadding, songs, Res.string.no_library_songs, onScrolling, onReload, state) { list ->
        items(list, key = { it.videoId }) { track ->
            SongFullWidthItems(
                track = track,
                isPlaying = track.videoId == playingVideoId,
                modifier = Modifier.fillMaxWidth(),
                onMoreClickListener = { chosen = it },
                onClickListener = onPlay,
            )
        }
    }
    chosen?.let { videoId ->
        NowPlayingBottomSheet(
            onDismiss = { chosen = null },
            navController = navController,
            song = songs.data?.find { it.videoId == videoId }?.toSongEntity(),
        )
    }
}

/**
 * The list both tabs draw: pull to refresh, with the last list kept on screen while a refresh
 * runs, as the Library grids do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> LibraryList(
    contentPadding: PaddingValues,
    data: LocalResource<List<T>>,
    emptyText: StringResource,
    onScrolling: (onTop: Boolean) -> Unit,
    onReload: () -> Unit,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.(List<T>) -> Unit,
) {
    var last by remember { mutableStateOf<List<T>?>(null) }
    LaunchedEffect(data) { if (data is LocalResource.Success) last = data.data }
    val list = (if (data is LocalResource.Success) data.data else last).orEmpty()
    val isScrollingUp by state.isScrollingUp()
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { onScrolling(it <= 1 || isScrollingUp) }
    }
    // The loader stands in until the first list lands; after that a refresh keeps the list and
    // shows the pull indicator, below the top bar like the Library grids.
    val firstLoad = last == null && data is LocalResource.Loading
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = data is LocalResource.Loading && !firstLoad,
        onRefresh = onReload,
        modifier = Modifier.fillMaxSize(),
        state = refreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = data is LocalResource.Loading && !firstLoad,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = contentPadding.calculateTopPadding()),
            )
        },
    ) {
        LazyColumn(state = state, contentPadding = contentPadding) {
            content(list)
            if (firstLoad) {
                item { CenterLoadingBox(Modifier.fillMaxWidth().padding(32.dp)) }
            }
            if (data !is LocalResource.Loading && list.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(if (data is LocalResource.Error) Res.string.library_load_failed else emptyText),
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { EndOfPage() }
        }
    }
}

/** The rows above the Downloads grid: what is downloading, the downloaded songs, what failed. */
@Composable
fun DownloadsHeader(
    keepLibrary: Boolean,
    checkingLibrary: Boolean,
    onKeepLibrary: (Boolean) -> Unit,
    queue: DownloadQueue,
    songCount: Int,
    bytes: Long,
    missing: Int,
    onPause: (Boolean) -> Unit,
    onOpenSongs: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DownloadsRow(
            icon = LimusicIcons.LibraryMusic,
            title = stringResource(Res.string.keep_library_downloaded),
            subtitle = stringResource(if (checkingLibrary) Res.string.checking_library else Res.string.keep_library_downloaded_description),
            subtitleLines = 2,
            trailing = { Switch(checked = keepLibrary, onCheckedChange = onKeepLibrary) },
        )
        if (queue.remaining > 0) {
            val total = queue.done + queue.failed + queue.remaining
            val title =
                when {
                    queue.blocked != null -> Res.string.downloads_blocked_at
                    queue.paused -> Res.string.downloads_paused_at
                    queue.waitingForWifi -> Res.string.downloads_waiting_wifi
                    queue.offline -> Res.string.downloads_waiting_network
                    else -> Res.string.downloading_progress
                }
            val failed = queue.failed.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.downloads_failed_now, it, it) }
            DownloadsRow(
                icon = LimusicIcons.Downloading,
                title = stringResource(title, queue.done, total) + " · ${(queue.progress * 100).toInt()}%",
                subtitle = listOfNotNull(failed, queue.blocked).joinToString(" · ").ifEmpty { null },
                subtitleLines = 2,
                progress = queue.progress,
                trailing = {
                    TextButton(onClick = { onPause(!queue.paused) }) {
                        Text(stringResource(if (queue.paused) Res.string.resume else Res.string.pause), color = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        DownloadsRow(
            icon = LimusicIcons.DownloadForOffline,
            title = stringResource(Res.string.downloaded_songs),
            subtitle = pluralStringResource(Res.plurals.liked_songs_count, songCount, songCount) + " · " + bytes.sizeText(),
            onClick = onOpenSongs,
        )
        // Only once nothing is queued: until then a missing song may just be next in line.
        if (missing > 0 && queue.remaining == 0) {
            DownloadsRow(
                icon = LimusicIcons.Error,
                title = pluralStringResource(Res.plurals.downloads_missing, missing, missing),
                trailing = { TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary) } },
            )
        }
    }
}

internal fun Long.sizeText(): String {
    val mb = bytesToMB()
    return if (mb < 1024) "$mb MB" else "${(mb * 10 / 1024) / 10.0} GB"
}

@Composable
private fun DownloadsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleLines: Int = 1,
    progress: Float? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = rounded(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(rounded(12.dp)).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(title, style = typo().labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = subtitleLines)
                }
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.16f),
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    LimusicIcons.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp).size(14.dp),
                )
            }
        }
    }
}
