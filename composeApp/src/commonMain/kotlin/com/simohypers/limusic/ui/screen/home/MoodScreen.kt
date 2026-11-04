package com.simohypers.limusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
import com.simohypers.limusic.common.Config
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.mood.moodmoments.Content
import com.simohypers.limusic.domain.data.model.searchResult.songs.Artist
import com.simohypers.limusic.domain.mediaservice.handler.PlaylistType
import com.simohypers.limusic.domain.mediaservice.handler.QueueData
import com.simohypers.limusic.expect.ui.isLyricsBlurSupported
import com.simohypers.limusic.extension.artworkScrimBrush
import com.simohypers.limusic.extension.sizedThumbnail
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.EndOfPage
import com.simohypers.limusic.ui.component.LibrarySectionHeader
import com.simohypers.limusic.ui.component.PillIconButton
import com.simohypers.limusic.ui.component.RippleIconButton
import com.simohypers.limusic.ui.component.rememberHolderPainter
import com.simohypers.limusic.ui.icon.ArrowBackIosNew
import com.simohypers.limusic.ui.icon.Explicit
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.navigation.destination.list.PlaylistDestination
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.MoodViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.mood_failed
import limusic.composeapp.generated.resources.playlists_count
import limusic.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * A Mood or Genre page, opened from a Search tile: the tile's colour and artwork wash the top, then
 * each of YouTube's sections ("Featured playlists", "Community playlists") as a cover grid.
 * [color], [artwork] and [kicker] are the tile's; a caller without them gets the accent and the
 * page's first cover.
 */
@Composable
fun MoodScreen(
    navController: NavController,
    viewModel: MoodViewModel = koinViewModel(),
    params: String?,
    color: Long? = null,
    artwork: String? = null,
    kicker: String? = null,
) {
    val moodData by viewModel.moodsMomentObject.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    LaunchedEffect(params) { if (params != null) viewModel.getMood(params) }

    val sections = moodData?.items.orEmpty().filter { it.contents.isNotEmpty() }
    val title = moodData?.header.orEmpty()
    val listState = rememberLazyListState()
    val open = { content: Content -> openMoodItem(content, navController, viewModel) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "hero", contentType = "hero") {
                val playlists = sections.sumOf { s -> s.contents.count { it.videoId == null } }
                MoodHero(
                    color = color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                    artwork = artwork ?: sections.firstOrNull()?.contents?.firstOrNull()?.cover(),
                    kicker = kicker,
                    title = title,
                    count = if (playlists > 0) pluralStringResource(Res.plurals.playlists_count, playlists, playlists) else null,
                )
            }
            when {
                loading && moodData == null ->
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CenterLoadingBox(Modifier.size(80.dp))
                        }
                    }
                moodData == null ->
                    item(key = "failed") {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.mood_failed),
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = { params?.let(viewModel::getMood) }) {
                                Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                else ->
                    sections.forEachIndexed { s, section ->
                        item(key = "section:$s", contentType = "section") {
                            LibrarySectionHeader(icon = null, title = section.header, modifier = Modifier.padding(top = if (s == 0) 4.dp else 16.dp))
                        }
                        items(section.contents.chunked(2), key = { "s$s:${it.first().id()}" }, contentType = { "cards" }) { two ->
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                two.forEach { content ->
                                    CoverCard(content.cover(), content.title, content.subtitle, Modifier.weight(1f)) { open(content) }
                                }
                                if (two.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
            }
            item(key = "end") { EndOfPage() }
        }

        HeroTopBar(listState, title) { navController.navigateUp() }
    }
}

internal val HERO = 300.dp

/** Back floats on a [MoodHero]; once the name has gone under the top, a bar with the name takes over. */
@Composable
internal fun HeroTopBar(
    listState: LazyListState,
    title: String,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    // The bar fills in once the name has scrolled up to it.
    val barPx = with(density) { WindowInsets.statusBars.getTop(this) + 64.dp.roundToPx() }
    val heroPx = with(density) { HERO.roundToPx() }
    val pastHero by remember(barPx, heroPx) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > heroPx - barPx - 40 }
    }
    AnimatedVisibility(visible = pastHero, enter = fadeIn(), exit = fadeOut()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(64.dp)
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RippleIconButton(LimusicIcons.ArrowBackIosNew, Modifier.size(32.dp), true, MaterialTheme.colorScheme.onSurface, onClick = onBack)
            Text(
                title,
                style = typo().titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (!pastHero) {
        PillIconButton(
            LimusicIcons.ArrowBackIosNew,
            Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(12.dp).size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onBack,
        )
    }
}

/** The tile, grown: its colour as a wash, its artwork faint in it, the page colour rising from below. */
@Composable
internal fun MoodHero(
    color: Color,
    artwork: String?,
    kicker: String?,
    title: String,
    count: String?,
    // The artwork as itself, fading into the page, instead of a blurred texture.
    sharp: Boolean = false,
) {
    val background = MaterialTheme.colorScheme.background
    // Clipped: a blur draws past its bounds, and the overflow sat as a strip under the first heading.
    Box(Modifier.fillMaxWidth().height(HERO).clipToBounds()) {
        Box(Modifier.fillMaxSize().background(color.copy(alpha = 0.55f)))
        // Blurred to a texture: covers carry their own big lettering, which fought the name over it.
        // Below Android 12 there is no blur, so the colour carries the hero alone.
        if (artwork != null && sharp) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (artwork != null && isLyricsBlurSupported()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(artwork).size(96).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(1.2f).blur(36.dp, BlurredEdgeTreatment.Rectangle).alpha(0.5f),
            )
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.8f).align(Alignment.BottomCenter).background(artworkScrimBrush(background)))
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (kicker != null) {
                Text(
                    kicker.uppercase(),
                    style = typo().bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                title,
                style = typo().headlineLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) {
                Text(
                    count,
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** A cover over its title and line, filling a grid cell; round and centred for an artist. [explicit] puts an E before the line. */
@Composable
internal fun CoverCard(
    cover: String?,
    title: String,
    subtitle: String,
    modifier: Modifier,
    ratio: Float = 1f,
    round: Boolean = false,
    explicit: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val holder = rememberHolderPainter()
    val align = if (round) TextAlign.Center else TextAlign.Start
    Column(modifier.clip(rounded(16.dp)).combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(4.dp)) {
        AsyncImage(
            model = cover,
            placeholder = holder,
            error = holder,
            fallback = holder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(ratio).clip(if (round) CircleShape else rounded(14.dp)),
        )
        Text(
            title,
            style = typo().titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
        if (subtitle.isNotBlank() || explicit) {
            Row(Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (explicit) {
                    Icon(
                        LimusicIcons.Explicit,
                        contentDescription = "Explicit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp).size(14.dp),
                    )
                }
                Text(
                    subtitle,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = align,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun Content.cover() = thumbnails?.lastOrNull()?.url?.sizedThumbnail(544)

private fun Content.id() = videoId ?: playlistBrowseId

/** A song (from a "Songs" section) starts its radio; anything else opens its playlist. */
private fun openMoodItem(
    content: Content,
    navController: NavController,
    viewModel: MoodViewModel,
) {
    val videoId = content.videoId
    if (videoId == null) {
        navController.navigate(PlaylistDestination(playlistId = content.playlistBrowseId))
        return
    }
    val track =
        Track(
            album = null,
            artists = listOf(Artist(id = null, name = content.subtitle)),
            duration = null,
            durationSeconds = null,
            isAvailable = true,
            isExplicit = false,
            likeStatus = null,
            thumbnails = content.thumbnails,
            title = content.title,
            videoId = videoId,
            videoType = null,
            category = null,
            feedbackTokens = null,
            resultType = null,
        )
    viewModel.setQueueData(
        QueueData.Data(
            listTracks = arrayListOf(track),
            firstPlayedTrack = track,
            playlistId = "RDAMVM$videoId",
            playlistName = "\"${content.title}\" Radio",
            playlistType = PlaylistType.RADIO,
            continuation = null,
        ),
    )
    viewModel.loadMediaItem(track, type = Config.SONG_CLICK)
}
