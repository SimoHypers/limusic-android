package com.simohypers.limusic.ui.component

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.domain.data.model.library.LibraryOverview
import com.simohypers.limusic.domain.data.type.PlaylistType
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.ArrowForwardIos
import com.simohypers.limusic.ui.icon.DownloadForOffline
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.PeopleAlt
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.TrendingUp
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.simohypers.limusic.ui.screen.library.LibraryDynamicPlaylistType
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.library_downloads
import limusic.composeapp.generated.resources.library_followed_count
import limusic.composeapp.generated.resources.liked_songs
import limusic.composeapp.generated.resources.liked_songs_count
import limusic.composeapp.generated.resources.most_played
import limusic.composeapp.generated.resources.new_playlist
import limusic.composeapp.generated.resources.see_all

/**
 * The four collections at the top of the All tab, two by two: an icon that says what each holds,
 * its count, and the newest artwork fading in from the right. [overview] is null until the first
 * read lands, and the tiles then draw their titles alone.
 */
@Composable
fun LibraryTilingBox(
    navController: NavController,
    overview: LibraryOverview?,
    signedIn: Boolean,
    onOpenArtists: () -> Unit,
) {
    val open = { type: LibraryDynamicPlaylistType ->
        navController.navigate(LibraryDynamicPlaylistDestination(type = type.toStringParams()))
    }
    val songs = @Composable { count: Int? -> count?.let { pluralStringResource(Res.plurals.liked_songs_count, it, it) } }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(
                icon = LimusicIcons.Favorite,
                title = stringResource(Res.string.liked_songs),
                // Signed in this is YouTube's Liked Music, which the phone's count doesn't know.
                countText = if (signedIn) null else songs(overview?.favorite?.count),
                cover = overview?.favorite?.thumbnails?.firstOrNull(),
                modifier = Modifier.weight(1f),
            ) {
                if (signedIn) navController.navigate(PlaylistDestination("LM")) else open(LibraryDynamicPlaylistType.Favorite)
            }
            QuickTile(
                icon = LimusicIcons.DownloadForOffline,
                title = stringResource(Res.string.library_downloads),
                countText = songs(overview?.downloaded?.count),
                cover = overview?.downloaded?.thumbnails?.firstOrNull(),
                modifier = Modifier.weight(1f),
            ) { open(LibraryDynamicPlaylistType.Downloaded) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(
                icon = LimusicIcons.TrendingUp,
                title = stringResource(Res.string.most_played),
                countText = songs(overview?.mostPlayed?.count),
                cover = overview?.mostPlayed?.thumbnails?.firstOrNull(),
                modifier = Modifier.weight(1f),
            ) { open(LibraryDynamicPlaylistType.MostPlayed) }
            QuickTile(
                icon = LimusicIcons.PeopleAlt,
                title = stringResource(Res.string.artists),
                // Signed in the tab lists the account's subscriptions, which the phone's count doesn't know.
                countText =
                    if (signedIn) null else overview?.followed?.count?.let { pluralStringResource(Res.plurals.library_followed_count, it, it) },
                cover = overview?.followed?.thumbnails?.firstOrNull(),
                modifier = Modifier.weight(1f),
                onClick = onOpenArtists,
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    countText: String?,
    cover: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = rounded(18.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (cover != null) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(cover)
                            .crossfade(true)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            // The card's own colour washes over the art's left edge, so the
                            // title and count never sit on a busy cover.
                            .drawWithContent {
                                drawContent()
                                drawRect(Brush.horizontalGradient(listOf(container, container.copy(alpha = 0.55f), Color.Transparent)))
                            },
                )
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(rounded(12.dp)).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = typo().labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (countText != null) {
                        Text(
                            text = countText,
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A section title in the desktop app's style: the kind's icon (if any) in a softened accent, a bold
 * title, and "See all" at the far end when [onMore] is given (the title then opens it too).
 */
@Composable
fun LibrarySectionHeader(
    icon: ImageVector?,
    title: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    // Controls at the far end, before "See all" (desktop's SectionHeading children).
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        // The box takes the free width so "See all" lands at the far end; the title inside it
        // stays its own width, so only the words themselves are a second way to open it.
        Box(Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier,
            )
        }
        trailing?.invoke()
        if (onMore != null) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onMore).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.see_all),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    LimusicIcons.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).size(11.dp),
                )
            }
        }
    }
}

/** Your playlists in a row, local then YouTube, with a tile for a new one always first so an empty shelf still offers it. */
@Composable
fun PlaylistShelf(
    navController: NavController,
    playlists: List<PlaylistType>,
    // Something is still on its way (the account's playlists): skeleton tiles hold its place.
    loading: Boolean,
    onNewPlaylist: () -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 6.dp)) {
        item {
            NewPlaylistTile(onNewPlaylist, Modifier.width(ShelfThumb))
        }
        items(playlists) { item ->
            HomeItemContentPlaylist(
                onClick = { navController.openLibraryItem(item) },
                data = item,
                thumbSize = ShelfThumb,
            )
        }
        if (loading) {
            // As tall as a real card (its 70dp text floor plus padding), so the row does not grow
            // and push Recently added down when the cards land.
            items(4) { PlaylistTileSkeleton(Modifier.width(ShelfThumb).heightIn(min = ShelfThumb + 86.dp)) }
        }
    }
}

@Composable
internal fun NewPlaylistTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .clip(rounded(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 6.dp)
            .then(modifier),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(rounded(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LimusicIcons.Add, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
        }
        Text(
            text = stringResource(Res.string.new_playlist),
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private val ShelfThumb = 120.dp
