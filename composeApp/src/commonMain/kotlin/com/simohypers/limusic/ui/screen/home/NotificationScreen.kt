package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.combinedClickable
import com.simohypers.limusic.viewModel.Shortcut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.simohypers.limusic.domain.extension.now
import com.simohypers.limusic.expect.ui.isLyricsBlurSupported
import com.simohypers.limusic.extension.formatTimeAgo
import com.simohypers.limusic.extension.sizedThumbnail
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.PinnedHeading
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.SettingSheet
import com.simohypers.limusic.ui.component.dateBucket
import com.simohypers.limusic.ui.component.label
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Delete
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.navigation.destination.list.AlbumDestination
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.NotificationViewModel
import com.simohypers.limusic.viewModel.Release
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.album
import limusic.composeapp.generated.resources.clear_all
import limusic.composeapp.generated.resources.new_album
import limusic.composeapp.generated.resources.new_releases
import limusic.composeapp.generated.resources.new_single
import limusic.composeapp.generated.resources.new_tag
import limusic.composeapp.generated.resources.release_single
import limusic.composeapp.generated.resources.releases_count
import limusic.composeapp.generated.resources.releases_empty_hint
import limusic.composeapp.generated.resources.releases_empty_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * New releases: what the twice-daily release check found from the artists you follow. The newest
 * as a featured card, the rest as a cover grid under pinned date headings, with what you haven't
 * seen yet tagged New.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = koinViewModel(),
) {
    val releases by viewModel.releases.collectAsStateWithLifecycle()
    val seenBefore by viewModel.seenBefore.collectAsStateWithLifecycle()
    val list = releases.orEmpty()
    val today = remember { now().date }
    var menu by remember { mutableStateOf(false) }
    val isNew = { r: Release -> seenBefore.let { it == null || r.foundAt > it } }
    val openAlbum = { r: Release -> navController.navigate(AlbumDestination(browseId = r.browseId)) }
    val openArtist = { r: Release -> navController.navigate(ArtistDestination(channelId = r.channelId)) }
    var menuFor by remember { mutableStateOf<Release?>(null) }
    menuFor?.let { r ->
        ItemMenu(Shortcut(Shortcut.Kind.ALBUM, r.browseId, r.title, r.artist, r.thumbnail), onClose = { menuFor = null })
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RippleIconButton(LimusicIcons.ArrowBackIosNew, Modifier.size(32.dp), true, MaterialTheme.colorScheme.onSurface) {
                navController.navigateUp()
            }
            Text(
                stringResource(Res.string.new_releases),
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (list.isNotEmpty()) {
                IconButton(onClick = { menu = true }) {
                    Icon(LimusicIcons.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            when {
                releases == null ->
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            CenterLoadingBox(Modifier.size(80.dp))
                        }
                    }
                list.isEmpty() -> item(key = "empty") { Empty() }
                else -> {
                    val featured = list.first()
                    item(key = "featured", contentType = "featured") {
                        FeaturedRelease(
                            featured,
                            isNew(featured),
                            onOpen = { openAlbum(featured) },
                            onArtist = { openArtist(featured) },
                            onLongClick = { menuFor = featured },
                        )
                    }
                    val rest = list.drop(1)
                    var start = 0
                    while (start < rest.size) {
                        val bucket = dateBucket(rest[start].foundAt, today, days = true)
                        var end = start + 1
                        while (end < rest.size && dateBucket(rest[end].foundAt, today, days = true) == bucket) end++
                        val day = rest.subList(start, end)
                        stickyHeader(key = "day:$bucket", contentType = "day") {
                            PinnedHeading(bucket.label(today), pluralStringResource(Res.plurals.releases_count, day.size, day.size))
                        }
                        items(day.chunked(2), key = { it.first().browseId }, contentType = { "releases" }) { two ->
                            Row(
                                Modifier.animateItem().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                two.forEach { r -> ReleaseCell(r, isNew(r), Modifier.weight(1f), onLongClick = { menuFor = r }) { openAlbum(r) } }
                                if (two.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        start = end
                    }
                }
            }
            item(key = "end") { EndOfPage() }
        }
    }
    if (menu) {
        SettingSheet(title = stringResource(Res.string.new_releases), onDismiss = { menu = false }) {
            val danger = MaterialTheme.colorScheme.error
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(rounded(12.dp))
                    .clickable {
                        viewModel.clearAll()
                        menu = false
                    }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LimusicIcons.Delete, null, tint = danger)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(Res.string.clear_all), style = typo().bodyMedium, color = danger)
            }
        }
    }
}

/** The newest release, big: its cover blurred behind, the sharp cover, what it is and when. */
@Composable
private fun FeaturedRelease(
    release: Release,
    new: Boolean,
    onOpen: () -> Unit,
    onArtist: () -> Unit,
    onLongClick: () -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val shape = rounded(20.dp)
    val cover = release.thumbnail?.sizedThumbnail(544)
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape)
            .combinedClickable(onLongClick = onLongClick, onClick = onOpen),
    ) {
        // Below Android 12 there is no blur, and a sharp cover behind the text is just noise.
        if (cover != null && isLyricsBlurSupported()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(cover).size(96).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().scale(1.2f).blur(40.dp).alpha(0.7f),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(background.copy(alpha = 0.85f), background.copy(alpha = 0.55f)))),
        )
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Cover(cover, Modifier.size(120.dp).shadow(8.dp, rounded(14.dp)).clip(rounded(14.dp)))
                if (new) NewTag(Modifier.align(Alignment.TopStart).padding(6.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${stringResource(if (release.isAlbum) Res.string.new_album else Res.string.new_single)} • ${release.foundAt.formatTimeAgo()}"
                        .uppercase(),
                    style = typo().bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    release.title,
                    style = typo().headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onArtist)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(release.artistThumbnail?.sizedThumbnail(120), Modifier.size(24.dp).clip(CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(release.artist, style = typo().titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ReleaseCell(
    release: Release,
    new: Boolean,
    modifier: Modifier,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    Column(modifier.clip(rounded(16.dp)).combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(4.dp)) {
        Box {
            Cover(release.thumbnail?.sizedThumbnail(544), Modifier.fillMaxWidth().aspectRatio(1f).clip(rounded(14.dp)))
            if (new) NewTag(Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Text(
            release.title,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
        Text(
            "${stringResource(if (release.isAlbum) Res.string.album else Res.string.release_single)} • ${release.artist}",
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
        )
    }
}

/** On what you hadn't seen before this visit. The colour is set here, not taken from typo()'s grey. */
@Composable
private fun NewTag(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.new_tag).uppercase(),
        style = typo().bodySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onPrimary,
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
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

@Composable
private fun Empty() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(Res.string.releases_empty_title),
            style = typo().titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.releases_empty_hint),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
