package com.simohypers.limusic.ui.screen.other

import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.simohypers.limusic.viewModel.Shortcut
import com.simohypers.limusic.ui.screen.home.menuShortcut
import com.simohypers.limusic.ui.screen.home.ItemMenu
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.extension.sizedThumbnail
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.MoreAlbumsDestination
import com.simohypers.limusic.ui.screen.home.CoverCard
import com.simohypers.limusic.ui.screen.home.HeroTopBar
import com.simohypers.limusic.ui.screen.home.MoodHero
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.MoreAlbumsUIState
import com.simohypers.limusic.viewModel.MoreAlbumsViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.albums_count
import limusic.composeapp.generated.resources.mood_failed
import limusic.composeapp.generated.resources.retry
import limusic.composeapp.generated.resources.singles
import limusic.composeapp.generated.resources.singles_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * An artist's albums or singles, from the artist page's "More": the Mood page's hero built from the
 * artist (their photo in the accent, their name over "Albums") over a two-column cover grid in
 * YouTube's order, newest first.
 */
@Composable
fun MoreAlbumsScreen(
    navController: NavController,
    id: String?,
    type: String?,
    artist: String?,
    artwork: String?,
    viewModel: MoreAlbumsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val singles = type == MoreAlbumsDestination.SINGLE_TYPE
    val load = {
        if (id != null) {
            if (singles) viewModel.getSingleMore(id) else viewModel.getAlbumMore(id)
        }
    }
    LaunchedEffect(id, type) { load() }

    val albums = (uiState as? MoreAlbumsUIState.Success)?.albumItems.orEmpty()
    val title = stringResource(if (singles) Res.string.singles else Res.string.albums)
    val listState = rememberLazyListState()
    var menuFor by remember { mutableStateOf<Shortcut?>(null) }
    menuFor?.let { ItemMenu(it, onClose = { menuFor = null }) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "hero", contentType = "hero") {
                MoodHero(
                    color = MaterialTheme.colorScheme.primary,
                    artwork = artwork ?: albums.firstOrNull()?.thumbnails?.lastOrNull()?.url,
                    kicker = artist,
                    title = title,
                    sharp = artwork != null,
                    count =
                        if (albums.isEmpty()) {
                            null
                        } else {
                            pluralStringResource(if (singles) Res.plurals.singles_count else Res.plurals.albums_count, albums.size, albums.size)
                        },
                )
            }
            when (uiState) {
                MoreAlbumsUIState.Loading ->
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CenterLoadingBox(Modifier.size(80.dp))
                        }
                    }

                is MoreAlbumsUIState.Error ->
                    item(key = "failed") {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.mood_failed),
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = load) {
                                Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                is MoreAlbumsUIState.Success ->
                    items(albums.chunked(2), contentType = { "cards" }) { two ->
                        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            two.forEach { album ->
                                val menu = album.menuShortcut()
                                CoverCard(
                                    cover = album.thumbnails.lastOrNull()?.url?.sizedThumbnail(544),
                                    title = album.title,
                                    subtitle = album.year,
                                    modifier = Modifier.weight(1f),
                                    explicit = album.isExplicit,
                                    onLongClick = menu?.let { { menuFor = it } },
                                ) { navController.navigate(AlbumDestination(browseId = album.browseId)) }
                            }
                            if (two.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
            }
            item(key = "end") { EndOfPage() }
        }
        HeroTopBar(listState, title) { navController.navigateUp() }
    }
}
