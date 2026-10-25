package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.type.ArtistType
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.library_load_failed
import simpmusic.composeapp.generated.resources.library_subscriptions
import simpmusic.composeapp.generated.resources.no_library_artists
import simpmusic.composeapp.generated.resources.no_library_songs
import simpmusic.composeapp.generated.resources.no_saved_artists
import simpmusic.composeapp.generated.resources.no_subscriptions

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
    PullToRefreshBox(
        isRefreshing = data is LocalResource.Loading,
        onRefresh = onReload,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(state = state, contentPadding = contentPadding) {
            content(list)
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
