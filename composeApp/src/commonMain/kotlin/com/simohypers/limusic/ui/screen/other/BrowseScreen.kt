package com.simohypers.limusic.ui.screen.other

import com.simohypers.limusic.ui.screen.home.ContentMenu
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.home.Content
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.domain.utils.toListName
import com.simohypers.limusic.domain.utils.toSongEntity
import com.simohypers.limusic.domain.utils.toTrack
import com.simohypers.limusic.extension.sizedThumbnail
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.MoodMomentAndGenreHomeItem
import com.simohypers.limusic.ui.component.NowPlayingBottomSheet
import com.simohypers.limusic.ui.component.homeCardAspectRatio
import com.simohypers.limusic.ui.component.openHomeItem
import com.simohypers.limusic.ui.component.pageDestination
import com.simohypers.limusic.ui.navigation.destination.home.MoodDestination
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.screen.home.CoverCard
import com.simohypers.limusic.ui.screen.home.HeroTopBar
import com.simohypers.limusic.ui.screen.home.MoodHero
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.BrowseUIState
import com.simohypers.limusic.viewModel.BrowseViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums_count
import limusic.composeapp.generated.resources.artists_count
import limusic.composeapp.generated.resources.items_count
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.mood_failed
import limusic.composeapp.generated.resources.playlists_count
import limusic.composeapp.generated.resources.retry
import limusic.composeapp.generated.resources.videos_count
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The page a home section's "More" endpoint opens when YouTube names no album, playlist, artist or
 * podcast behind it: the Mood page's hero over a two-column cover grid of every shelf, flattened.
 * An item opens and plays as it does on Home.
 */
@Composable
fun BrowseScreen(
    navController: NavController,
    browseId: String,
    params: String?,
    title: String?,
    kicker: String?,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(browseId, params) { viewModel.getBrowsePage(browseId, params) }

    val page = (uiState as? BrowseUIState.Success)?.page
    val contents = page?.contents.orEmpty()
    val name = page?.title ?: title.orEmpty()
    // A page that is mostly videos keeps their 16:9 shape; every other page has square covers.
    val ratio = if (contents.count { it.homeCardAspectRatio() != 1f } * 2 > contents.size) 16f / 9f else 1f
    val listState = rememberLazyListState()
    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    var cardMenu by remember { mutableStateOf<Content?>(null) }

    sheetTrack?.let {
        NowPlayingBottomSheet(onDismiss = { sheetTrack = null }, song = it.toSongEntity(), navController = navController)
    }
    cardMenu?.let { ContentMenu(it, onClose = { cardMenu = null }) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "hero", contentType = "hero") {
                MoodHero(
                    color = MaterialTheme.colorScheme.primary,
                    artwork = contents.firstOrNull()?.cover(),
                    kicker = kicker,
                    title = name,
                    count = if (contents.isEmpty()) null else pluralStringResource(contents.countPlural(), contents.size, contents.size),
                )
            }
            when (val state = uiState) {
                BrowseUIState.Loading ->
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CenterLoadingBox(Modifier.size(80.dp))
                        }
                    }

                is BrowseUIState.Error ->
                    item(key = "failed") {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.mood_failed),
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = { viewModel.getBrowsePage(browseId, params) }) {
                                Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                is BrowseUIState.Success -> {
                    // Only Moods & genres holds these.
                    items(state.page.moods.chunked(2), contentType = { "moods" }) { two ->
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            two.forEach { mood ->
                                Box(Modifier.weight(1f)) {
                                    MoodMomentAndGenreHomeItem(mood.title, mood.stripeColor, fillMaxWidth = true) {
                                        navController.navigate(MoodDestination(mood.params, mood.stripeColor))
                                    }
                                }
                            }
                            if (two.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    items(contents.chunked(2), contentType = { "cards" }) { two ->
                        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            two.forEach { content ->
                                val destination = content.pageDestination()
                                val round = destination is ArtistDestination
                                CoverCard(
                                    cover = content.cover(),
                                    title = content.title,
                                    subtitle = content.description?.takeIf { it.isNotBlank() } ?: content.artists.toListName().connectArtists(),
                                    modifier = Modifier.weight(1f),
                                    ratio = if (round) 1f else ratio,
                                    round = round,
                                    // A song or video has the song menu, as on Home; the rest their own.
                                    onLongClick = if (destination == null) ({ sheetTrack = content.toTrack() }) else ({ cardMenu = content }),
                                ) { content.openHomeItem(navController, viewModel) }
                            }
                            if (two.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item(key = "end") { EndOfPage() }
        }
        HeroTopBar(listState, name) { navController.navigateUp() }
    }
}

private fun Content.cover() = thumbnails.lastOrNull()?.url?.sizedThumbnail(544)

/** "12 playlists" when the page holds one kind of thing, "12 items" when it mixes them. */
private fun List<Content>.countPlural(): PluralStringResource =
    map {
        when (it.pageDestination()) {
            is ArtistDestination -> Res.plurals.artists_count
            is PlaylistDestination -> Res.plurals.playlists_count
            is AlbumDestination -> Res.plurals.albums_count
            null -> if (it.homeCardAspectRatio() != 1f) Res.plurals.videos_count else Res.plurals.liked_songs_count
            else -> Res.plurals.items_count
        }
    }.distinct().singleOrNull() ?: Res.plurals.items_count
