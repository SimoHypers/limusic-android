package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.elapsedLabel
import com.maxrave.simpmusic.extension.lengthLabel
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UIEvent

private val ArtworkShape = RoundedCornerShape(10.dp)

/**
 * The Limusic full player: the artwork large, then title and artist with the like button, the seek
 * bar and the shuffle / previous / play / next / repeat row. Lyrics, queue and the rest sit behind
 * buttons below rather than in place of the artwork. The ground is the artwork's own colour fading
 * into the app background, as on the desktop app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingContentLimusic(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val background = MaterialTheme.colorScheme.background
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .background(
                Brush.verticalGradient(
                    0f to state.startColor.value.copy(alpha = 0.55f),
                    0.75f to background,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            PlayerTopRow(state, actions)
            Spacer(Modifier.weight(1f))
            HorizontalPager(
                state = state.artworkPagerState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                pageSpacing = 24.dp,
                userScrollEnabled = !isRepeatOne && state.artworkQueue.isNotEmpty(),
                key = { index -> "artwork_${state.artworkQueue.getOrNull(index)?.videoId.orEmpty()}_$index" },
            ) { page ->
                if (page == state.currentOrderIndex) {
                    CurrentArtwork(state, actions)
                } else {
                    val url = state.artworkQueue.getOrNull(page)?.thumbnails?.maxByOrNull { it.width * it.height }?.url
                    ArtworkImage(url = url, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.weight(1f))
            TitleRow(state, actions)
            Spacer(Modifier.height(12.dp))
            SeekBar(state, actions)
            Spacer(Modifier.height(8.dp))
            TransportRow(state, actions)
            Spacer(Modifier.weight(0.6f))
            SecondaryActions(actions)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PlayerTopRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onDismiss) {
            Icon(state.dismissIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = state.screenData.playlistName,
            style = typo().bodyMedium,
            maxLines = 1,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .basicMarquee(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = actions.onShowMoreSheet) {
            Icon(SimpIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** The track now playing: its artwork, or its video when one is playing. */
@Composable
private fun CurrentArtwork(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val showVideo = state.screenData.isVideo && state.shouldShowVideo
    // maxresdefault only exists for HD videos; hqdefault always does.
    var artworkUrl by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!showVideo) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(artworkUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artworkUrl + "BIGGER")
                        .crossfade(400)
                        .build(),
                contentDescription = null,
                onSuccess = { actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                onError = {
                    val fallback = artworkUrl?.replace("maxresdefault", "hqdefault")
                    if (fallback != null && fallback != artworkUrl) artworkUrl = fallback
                },
                contentScale = ContentScale.Crop,
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .shadow(16.dp, ArtworkShape, spotColor = state.spotShadowColor)
                        .clip(ArtworkShape),
            )
        }
        AnimatedVisibility(visible = showVideo) {
            Box(
                Modifier
                    .aspectRatio(state.videoAspectRatio)
                    .clip(ArtworkShape)
                    .background(Color.Black),
            ) {
                MediaPlayerViewWithSubtitle(
                    playerName = MAIN_PLAYER,
                    modifier = Modifier.align(Alignment.Center),
                    shouldShowSubtitle = false,
                    shouldPip = false,
                    shouldScaleDownSubtitle = true,
                    timelineState = state.timelineState,
                    lyricsData = state.screenData.lyricsData?.lyrics,
                    translatedLyricsData = state.screenData.lyricsData?.translatedLyrics?.first,
                    isInPipMode = state.isInPipMode,
                    mainTextStyle = typo().bodyLarge,
                    translatedTextStyle = typo().bodyMedium,
                )
                IconButton(onClick = actions.onEnterFullscreenVideo, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(SimpIcons.Fullscreen, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ArtworkImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(url)
                .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        modifier = modifier.clip(ArtworkShape),
    )
}

@Composable
private fun TitleRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typo().titleLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = state.screenData.artistName,
                style = typo().bodyLarge,
                maxLines = 1,
                modifier =
                    Modifier
                        .padding(top = 2.dp)
                        .basicMarquee()
                        .clickable(onClick = actions.onNavigateToArtist),
            )
        }
        IconButton(onClick = { actions.onUIEvent(UIEvent.ToggleLike) }) {
            Icon(
                imageVector = if (state.controllerState.isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                contentDescription = null,
                tint = if (state.controllerState.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val accent = MaterialTheme.colorScheme.primary
    val colors =
        SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
        )
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Slider(
            // A fraction, as the other styles do: sliderValue lives on a 0..100 scale.
            value = state.sliderValue / 100f,
            onValueChange = { actions.onSliderChange(it * 100f) },
            onValueChangeFinished = actions.onSliderChangeFinished,
            colors = colors,
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = colors,
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                )
            },
            thumb = {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(accent, CircleShape),
                )
            },
            modifier = Modifier.fillMaxWidth().height(24.dp),
        )
    }
    Row(Modifier.fillMaxWidth()) {
        Text(state.timelineState.elapsedLabel(state.sliderValue / 100f), style = typo().bodySmall, modifier = Modifier.weight(1f))
        Text(state.timelineState.lengthLabel(), style = typo().bodySmall)
    }
}

@Composable
private fun TransportRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val control = state.controllerState
    val onBackground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(SimpIcons.Shuffle, tint = if (control.isShuffle) accent else onBackground) {
            actions.onUIEvent(UIEvent.Shuffle)
        }
        TransportButton(SimpIcons.SkipPrevious, size = 36.dp, enabled = control.isPreviousAvailable) {
            actions.onUIEvent(UIEvent.Previous)
        }
        Box(
            Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable { actions.onUIEvent(UIEvent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (control.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
        TransportButton(SimpIcons.SkipNext, size = 36.dp, enabled = control.isNextAvailable) {
            actions.onUIEvent(UIEvent.Next)
        }
        TransportButton(
            if (control.repeatState is RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
            tint = if (control.repeatState is RepeatState.None) onBackground else accent,
        ) {
            actions.onUIEvent(UIEvent.Repeat)
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    size: Dp = 26.dp,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(52.dp)) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(size))
    }
}

/** What used to compete with the artwork for space: each opens its own sheet or page. */
@Composable
private fun SecondaryActions(actions: NowPlayingContentActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SecondaryButton(SimpIcons.Lyrics, actions.onShowFullscreenLyrics)
        SecondaryButton(SimpIcons.QueueMusic, actions.onShowQueue)
        SecondaryButton(SimpIcons.PlaylistAdd, actions.onShowAddToPlaylist)
        SecondaryButton(SimpIcons.Info, actions.onShowInfo)
    }
}

@Composable
private fun SecondaryButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .width(64.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
    }
}
