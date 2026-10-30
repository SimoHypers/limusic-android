package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.domain.data.entities.AlbumEntity
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PlaylistEntity
import com.simohypers.limusic.domain.data.entities.PodcastsEntity
import com.simohypers.limusic.domain.data.model.browse.playlist.Author
import com.simohypers.limusic.domain.data.model.browse.playlist.PlaylistState
import com.simohypers.limusic.domain.data.model.library.LibraryOverview
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.type.PlaylistType
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.domain.utils.isRadioPlaylistId
import com.simohypers.limusic.ui.component.AlbumMenu
import com.simohypers.limusic.ui.component.LocalPlaylistMenu
import com.simohypers.limusic.ui.component.PlaylistMenu
import com.simohypers.limusic.ui.component.SettingSheet
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.AddCircleOutline
import com.simohypers.limusic.ui.icon.CheckCircle
import com.simohypers.limusic.ui.icon.DownloadForOffline
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.GridView
import com.simohypers.limusic.ui.icon.PeopleAlt
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.TrendingUp
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.list.PodcastDestination
import com.simohypers.limusic.ui.screen.library.LibraryDynamicPlaylistType
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.AlbumViewModel
import com.simohypers.limusic.viewModel.HomeViewModel
import com.simohypers.limusic.viewModel.LibraryViewModel
import com.simohypers.limusic.viewModel.LocalPlaylistState
import com.simohypers.limusic.viewModel.LocalPlaylistViewModel
import com.simohypers.limusic.viewModel.PlaylistViewModel
import com.simohypers.limusic.viewModel.Shortcut
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.podcasts
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.add
import limusic.composeapp.generated.resources.add_shortcut
import limusic.composeapp.generated.resources.album
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.library
import limusic.composeapp.generated.resources.library_downloads
import limusic.composeapp.generated.resources.library_followed_count
import limusic.composeapp.generated.resources.liked_songs
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.most_played
import limusic.composeapp.generated.resources.playlist
import limusic.composeapp.generated.resources.playlists
import limusic.composeapp.generated.resources.remove_shortcut
import limusic.composeapp.generated.resources.shortcuts
import limusic.composeapp.generated.resources.shortcuts_desc
import limusic.composeapp.generated.resources.you

/**
 * Desktop's Shortcuts: what you pinned from your library, two wide tiles to a row. "Add" opens
 * [ShortcutPicker]; a long press opens [ShortcutMenu].
 */
@Composable
fun ShortcutsSection(
    viewModel: HomeViewModel,
    navController: NavController,
) {
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val overview by viewModel.libraryOverview.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Shortcut?>(null) }
    var menuRequest by remember { mutableIntStateOf(0) }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                LimusicIcons.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.shortcuts),
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            // Adding is an action in the heading, not a tile, so a short last row stays short.
            if (shortcuts.isNotEmpty() && shortcuts.size < Shortcut.MAX) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { picking = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LimusicIcons.Add, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.add), style = typo().bodySmall, color = onSurfaceVariant)
                }
            }
        }
        if (shortcuts.isEmpty()) {
            EmptyShortcuts { picking = true }
        } else {
            shortcuts.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { shortcut ->
                        ShortcutTile(
                            shortcut = shortcut,
                            overview = overview,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.openShortcut(shortcut) },
                            onLongClick = {
                                menuRequest++
                                menuFor = shortcut
                            },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (picking) {
        ShortcutPicker(
            shortcuts = shortcuts,
            overview = overview,
            onToggle = viewModel::toggleShortcut,
            onDismiss = { picking = false },
        )
    }
    menuFor?.let { shortcut ->
        key(menuRequest) {
            ShortcutMenu(
                shortcut = shortcut,
                onRemove = { viewModel.removeShortcut(shortcut) },
                onClose = { menuFor = null },
            )
        }
    }
}

/**
 * A long press: the shortcut's own ⋮ menu, the one its page shows, plus Remove from shortcuts. It
 * opens at once from what the shortcut knows while the page's view model loads behind it (one per
 * shortcut, kept with Home); rows that need the tracks wait for them. A collection or a podcast
 * has no ⋮ menu, so it gets Remove alone.
 */
@Composable
private fun ShortcutMenu(
    shortcut: Shortcut,
    onRemove: () -> Unit,
    onClose: () -> Unit,
) {
    val vmKey = "shortcut:${shortcut.kind}:${shortcut.id}"
    when (shortcut.kind) {
        Shortcut.Kind.PLAYLIST, Shortcut.Kind.OWN_PLAYLIST -> {
            val viewModel: PlaylistViewModel = koinViewModel(key = vmKey)
            val id = shortcut.id.removePrefix("VL")
            // Also retries one that failed last time: the view model outlives the menu.
            LaunchedEffect(Unit) { if (viewModel.uiState.value.data == null) viewModel.getData(id) }
            PlaylistMenu(
                viewModel = viewModel,
                placeholder =
                    PlaylistState(
                        id = id,
                        title = shortcut.title,
                        isRadio = id.isRadioPlaylistId(),
                        author = Author(id = "", name = shortcut.subtitle.orEmpty()),
                        thumbnail = shortcut.thumbnail,
                        year = "",
                    ),
                isYourYouTubePlaylist = shortcut.kind == Shortcut.Kind.OWN_PLAYLIST,
                onClose = onClose,
                onDeleted = onRemove,
                onRemoveShortcut = onRemove,
            )
        }

        Shortcut.Kind.ALBUM -> {
            val viewModel: AlbumViewModel = koinViewModel(key = vmKey)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                if (viewModel.uiState.value.loadState != LocalPlaylistState.PlaylistLoadState.Success) viewModel.updateBrowseId(shortcut.id)
            }
            AlbumMenu(viewModel, uiState, shortcut.id, onClose = onClose, onRemoveShortcut = onRemove)
        }

        Shortcut.Kind.LOCAL_PLAYLIST -> {
            val viewModel: LocalPlaylistViewModel = koinViewModel(key = vmKey)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val id = shortcut.id.toLongOrNull()
            LaunchedEffect(Unit) { if (id != null && uiState.id != id) viewModel.updatePlaylistState(id) }
            // A local read, a few milliseconds: the menu waits for it rather than guess the Sync row.
            if (id != null && uiState.id == id) {
                LocalPlaylistMenu(viewModel, uiState, onClose = onClose, onDeleted = onRemove, onRemoveShortcut = onRemove)
            }
        }

        Shortcut.Kind.COLLECTION, Shortcut.Kind.PODCAST -> {
            SettingSheet(title = shortcut.displayTitle(), onDismiss = onClose) { colors ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onRemove()
                                onClose()
                            }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LimusicIcons.GridView, contentDescription = null, tint = colors.content)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.remove_shortcut), style = typo().labelSmall, color = colors.content)
                }
            }
        }
    }
}

/** One wide row instead of ghost tiles: this sits on Home for everyone who never pins anything. */
@Composable
private fun EmptyShortcuts(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .clickable(onClick = onClick)
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LimusicIcons.Add, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(Res.string.add_shortcut), style = typo().titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(Res.string.shortcuts_desc), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    overview: LibraryOverview?,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flush with the tile's leading edge, as on desktop.
        ShortcutArt(shortcut, Modifier.size(56.dp))
        ShortcutText(shortcut, overview, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f).padding(horizontal = 10.dp))
    }
}

@Composable
private fun ShortcutText(
    shortcut: Shortcut,
    overview: LibraryOverview?,
    titleColor: Color,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(shortcut.displayTitle(), style = typo().titleSmall, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // A collection's count is read live; anything else shows what its card said when pinned.
        // ponytail: a pinned playlist's track count goes stale; overlay Library's rows if it shows.
        val subtitle = shortcut.collection()?.countText(overview) ?: shortcut.subtitle
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A collection draws its icon on an accent tint, like Library's tiles; anything else its cover. */
@Composable
private fun ShortcutArt(
    shortcut: Shortcut,
    modifier: Modifier,
) {
    val collection = shortcut.collection()
    if (collection != null) {
        val accent = MaterialTheme.colorScheme.primary
        Box(modifier.background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(collection.icon(), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        }
    } else {
        AsyncImage(
            model = shortcut.thumbnail,
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            fallback = rememberHolderPainter(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/**
 * Pin from the library without hunting for a menu. Stays open so several go in at once; a pinned
 * row shows a tick and a second tap takes it off.
 */
@Composable
private fun ShortcutPicker(
    shortcuts: List<Shortcut>,
    overview: LibraryOverview?,
    onToggle: (Shortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    val library: LibraryViewModel = koinViewModel()
    val loggedIn by library.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val local by library.yourLocalPlaylist.collectAsStateWithLifecycle()
    val youTube by library.youTubePlaylist.collectAsStateWithLifecycle()
    val saved by library.savedPlaylists.collectAsStateWithLifecycle()
    val albums by library.albums.collectAsStateWithLifecycle()
    val podcasts by library.favoritePodcasts.collectAsStateWithLifecycle()
    LaunchedEffect(loggedIn) {
        library.getLocalPlaylist()
        library.getSavedPlaylists()
        library.getAlbums()
        library.getFavoritePodcasts()
        if (loggedIn && youTube.data.isNullOrEmpty()) library.getYouTubePlaylist()
    }
    val groups =
        listOf(
            Res.string.library to Collections.map { it.toShortcut() },
            Res.string.playlists to
                (local.data.orEmpty() + saved + if (loggedIn) youTube.data.orEmpty() else emptyList()).mapNotNull { it.toShortcut() },
            Res.string.albums to albums.data.orEmpty().mapNotNull { it.toShortcut() },
            Res.string.podcasts to podcasts.data.orEmpty().mapNotNull { it.toShortcut() },
        ).filter { it.second.isNotEmpty() }
    val full = shortcuts.size >= Shortcut.MAX

    SettingSheet(title = stringResource(Res.string.add_shortcut), onDismiss = onDismiss) { colors ->
        // Capped so a long library scrolls inside the sheet instead of pushing it under the status bar.
        LazyColumn(Modifier.heightIn(max = 560.dp)) {
            groups.forEach { (label, items) ->
                item(key = label.key) {
                    Text(
                        stringResource(label),
                        style = typo().bodySmall,
                        color = colors.subtitle,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                items(items, key = { "${it.kind}:${it.id}" }) { shortcut ->
                    val pinned = shortcuts.any { it.sameAs(shortcut) }
                    val enabled = pinned || !full
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = enabled) { onToggle(shortcut) }
                                .padding(vertical = 6.dp)
                                .alpha(if (enabled) 1f else 0.4f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShortcutArt(shortcut, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                        ShortcutText(shortcut, overview, colors.content, Modifier.weight(1f).padding(horizontal = 12.dp))
                        Icon(
                            if (pinned) LimusicIcons.CheckCircle else LimusicIcons.AddCircleOutline,
                            contentDescription = null,
                            tint = if (pinned) MaterialTheme.colorScheme.primary else colors.subtitle,
                        )
                    }
                }
            }
        }
    }
}

fun NavController.openShortcut(shortcut: Shortcut) {
    when (shortcut.kind) {
        Shortcut.Kind.COLLECTION -> navigate(LibraryDynamicPlaylistDestination(type = shortcut.id))
        Shortcut.Kind.LOCAL_PLAYLIST -> shortcut.id.toLongOrNull()?.let { navigate(LocalPlaylistDestination(it)) }
        Shortcut.Kind.PLAYLIST -> navigate(PlaylistDestination(shortcut.id))
        Shortcut.Kind.OWN_PLAYLIST -> navigate(PlaylistDestination(shortcut.id, isYourYouTubePlaylist = true))
        Shortcut.Kind.ALBUM -> navigate(AlbumDestination(shortcut.id))
        Shortcut.Kind.PODCAST -> navigate(PodcastDestination(podcastId = shortcut.id))
    }
}

/** Library's four collections, in the order its tiles read. */
private val Collections =
    listOf(
        LibraryDynamicPlaylistType.Favorite,
        LibraryDynamicPlaylistType.Downloaded,
        LibraryDynamicPlaylistType.MostPlayed,
        LibraryDynamicPlaylistType.Followed,
    )

private fun Shortcut.collection(): LibraryDynamicPlaylistType? =
    if (kind == Shortcut.Kind.COLLECTION) Collections.firstOrNull { it.toStringParams() == id } else null

/** A collection's name follows the app's language; anything else keeps the title it was pinned with. */
@Composable
private fun Shortcut.displayTitle(): String = collection()?.let { stringResource(it.label()) } ?: title

private fun LibraryDynamicPlaylistType.label(): StringResource =
    when (this) {
        LibraryDynamicPlaylistType.Favorite -> Res.string.liked_songs
        LibraryDynamicPlaylistType.Downloaded -> Res.string.library_downloads
        LibraryDynamicPlaylistType.MostPlayed -> Res.string.most_played
        else -> Res.string.artists
    }

private fun LibraryDynamicPlaylistType.icon(): ImageVector =
    when (this) {
        LibraryDynamicPlaylistType.Favorite -> LimusicIcons.Favorite
        LibraryDynamicPlaylistType.Downloaded -> LimusicIcons.DownloadForOffline
        LibraryDynamicPlaylistType.MostPlayed -> LimusicIcons.TrendingUp
        else -> LimusicIcons.PeopleAlt
    }

@Composable
private fun LibraryDynamicPlaylistType.countText(overview: LibraryOverview?): String? {
    val count =
        when (this) {
            LibraryDynamicPlaylistType.Favorite -> overview?.favorite
            LibraryDynamicPlaylistType.Downloaded -> overview?.downloaded
            LibraryDynamicPlaylistType.MostPlayed -> overview?.mostPlayed
            else -> overview?.followed
        }?.count ?: return null
    return if (this == LibraryDynamicPlaylistType.Followed) {
        pluralStringResource(Res.plurals.library_followed_count, count, count)
    } else {
        pluralStringResource(Res.plurals.liked_songs_count, count, count)
    }
}

@Composable
private fun LibraryDynamicPlaylistType.toShortcut() = Shortcut(Shortcut.Kind.COLLECTION, toStringParams(), stringResource(label()))

/** What a Library card says about [this], as a Shortcut; the kinds match `openLibraryItem`. */
@Composable
private fun PlaylistType.toShortcut(): Shortcut? =
    when (this) {
        is LocalPlaylistEntity -> Shortcut(Shortcut.Kind.LOCAL_PLAYLIST, id.toString(), title, stringResource(Res.string.you), thumbnail)
        is PlaylistsResult -> Shortcut(Shortcut.Kind.OWN_PLAYLIST, browseId, title, author, thumbnails.lastOrNull()?.url)
        is AlbumEntity ->
            Shortcut(Shortcut.Kind.ALBUM, browseId, title, artistName?.connectArtists() ?: stringResource(Res.string.album), thumbnails)
        is AlbumsResult ->
            Shortcut(
                Shortcut.Kind.ALBUM,
                browseId,
                title,
                artists.joinToString { it.name }.ifEmpty { stringResource(Res.string.album) },
                thumbnails.lastOrNull()?.url,
            )
        is PlaylistEntity -> Shortcut(Shortcut.Kind.PLAYLIST, id, title, author ?: stringResource(Res.string.playlist), thumbnails)
        is PodcastsEntity -> Shortcut(Shortcut.Kind.PODCAST, podcastId, title, authorName, thumbnail)
        else -> null
    }
