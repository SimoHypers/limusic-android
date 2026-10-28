package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.data.player.AudioOutputKind
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.elapsedLabel
import com.maxrave.simpmusic.extension.lengthLabel
import com.maxrave.simpmusic.ui.component.LyricText
import com.maxrave.simpmusic.ui.component.SleepTimerBottomSheet
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AccessAlarm
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Headphones
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AudioOutputSheetHost
import com.maxrave.simpmusic.ui.screen.player.content.expressive.WavySeekBar
import com.maxrave.simpmusic.ui.theme.LocalArtworkColors
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.now_playing
import simpmusic.composeapp.generated.resources.share
import simpmusic.composeapp.generated.resources.sleep_timer_warning
import simpmusic.composeapp.generated.resources.yes

private val ArtworkShape = RoundedCornerShape(16.dp)
private val TonalShape = RoundedCornerShape(16.dp)
private val SmallShape = RoundedCornerShape(12.dp)
private val FROST_BLUR = 80.dp
private const val FROST_DECODE_PX = 256

/**
 * The Classic full player, the default: a large cover with the current lyric line under it, title
 * and artist with share / like / more, the wavy seek bar, a row of rounded transport buttons, and
 * Queue / sleep timer / Lyrics at the bottom, the audio output top right. Colours come from the cover (the M3
 * Expressive style's artwork scheme) over the cover itself, frosted.
 */
@Composable
fun NowPlayingContentLimusic(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    // With "Adapt colors to artwork" on, the player takes the same cover colours as the rest of
    // the app, so opening it doesn't change the accent.
    if (LocalArtworkColors.current) {
        ClassicLayout(state, actions)
    } else {
        NowPlayingExpressiveTheme(state = state) {
            ClassicLayout(state, actions)
        }
    }
}

@Composable
private fun ClassicLayout(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val isRepeatOne = state.controllerState.repeatState is RepeatState.One
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize().background(background)) {
        // Modifier.blur does nothing before Android 12; the cover-coloured gradient stands in there.
        if (isLyricsBlurSupported()) {
            FrostedCover(state.screenData.thumbnailURL, wash = background)
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to state.startColor.value.copy(alpha = 0.55f), 0.75f to background)))
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            TopRow(state, actions)
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
            // Cover, lyric line and title stay one group at fixed gaps; the leftover height goes
            // above the cover and above the bottom row instead of opening a hole under the line.
            Spacer(Modifier.height(12.dp))
            LyricLine(state)
            Spacer(Modifier.height(14.dp))
            TitleRow(state, actions)
            Spacer(Modifier.height(16.dp))
            WavySeekBar(
                progressFraction = state.sliderValue / 100f,
                isPlaying = state.controllerState.isPlaying,
                activeColor = if (state.timelineState.isCrossfading) state.sliderTrackColor else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                thumbColor = MaterialTheme.colorScheme.primary,
                onSliderChange = actions.onSliderChange,
                onSliderChangeFinished = actions.onSliderChangeFinished,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(state.timelineState.elapsedLabel(state.sliderValue / 100f), style = typo().bodySmall, modifier = Modifier.weight(1f))
                Text(state.timelineState.lengthLabel(), style = typo().bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            ClassicTransportRow(state, actions)
            Spacer(Modifier.weight(1f))
            BottomRow(state, actions)
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * [thumbnail] blurred to fill its box, under a wash of [wash] so text over it stays readable. The
 * blur needs Android 12 ([isLyricsBlurSupported]); callers check before drawing this.
 */
@Composable
internal fun FrostedCover(
    thumbnail: String?,
    wash: Color,
    washFrom: Float = 0.5f,
    washTo: Float = 0.75f,
) {
    Crossfade(targetState = thumbnail, animationSpec = tween(600), label = "frostedCover") { thumbnail ->
        var url by remember(thumbnail) { mutableStateOf(thumbnail) }
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    // Blurred this hard, a small decode looks the same and spares a full-screen bitmap.
                    .size(FROST_DECODE_PX)
                    .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = {
                val fallback = url?.replace("maxresdefault", "hqdefault")
                if (fallback != null && fallback != url) url = fallback
            },
            modifier = Modifier.fillMaxSize().blur(FROST_BLUR, BlurredEdgeTreatment.Rectangle),
        )
    }
    // The wash's alpha top to bottom; picked by eye on the phone.
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to wash.copy(alpha = washFrom), 1f to wash.copy(alpha = washTo))))
}

@Composable
private fun TopRow(
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
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.now_playing), style = typo().bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = state.screenData.playlistName,
                style = typo().bodyMedium,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.basicMarquee(),
            )
        }
        OutputButton(state)
    }
}

/** Where the sound goes, lit while it isn't the phone's own speaker. */
@Composable
private fun OutputButton(state: NowPlayingContentState) {
    val outputs by koinInject<MediaPlayerHandler>().player.audioOutputs.collectAsStateWithLifecycle()
    val active = outputs.firstOrNull { it.isActive }
    val away = state.castState.isRemote || (active != null && active.kind != AudioOutputKind.DEVICE_SPEAKER)
    var choosing by remember { mutableStateOf(false) }
    IconButton(onClick = { choosing = true }) {
        Icon(
            SimpIcons.Headphones,
            contentDescription = null,
            tint = if (away) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
    if (choosing) {
        AudioOutputSheetHost(castState = state.castState, onDismiss = { choosing = false })
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
                        // A fixed memory key, used as the placeholder too: when a queue reorder
                        // (shuffle) rebuilds the pager page, the cover redraws from memory at once
                        // instead of flashing the placeholder.
                        .memoryCacheKey(artworkUrl + "BIGGER")
                        .placeholderMemoryCacheKey(artworkUrl + "BIGGER")
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
                .memoryCacheKey(url)
                .placeholderMemoryCacheKey(url)
                .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        modifier = modifier.clip(ArtworkShape),
    )
}

/** The synced line playing now, under the cover, as the M3 Expressive style shows it. */
@Composable
private fun LyricLine(state: NowPlayingContentState) {
    val lyrics = state.screenData.lyricsData?.lyrics
    val synced = lyrics != null && lyrics.syncType != null && lyrics.syncType != "UNSYNCED" && lyrics.lines != null
    val line =
        if (!synced || state.currentLyricLineIndex < 0) {
            ""
        } else {
            lyrics
                ?.lines
                ?.getOrNull(state.currentLyricLineIndex)
                ?.words
                ?.stripRichSyncTimestamps()
                .orEmpty()
        }
    // Fixed height, so a song without synced lyrics keeps the same layout.
    Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) {
        Crossfade(targetState = line, animationSpec = tween(300), label = "classicLyricLine") { text ->
            LyricText(
                text = text,
                style = typo().labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately),
            )
        }
    }
}

@Composable
private fun TitleRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val liked = state.controllerState.isLiked
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.screenData.nowPlayingTitle,
                style = typo().titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
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
        FilledButton(if (liked) SimpIcons.Favorite else SimpIcons.FavoriteBorder, Modifier.size(52.dp), TonalShape) {
            actions.onUIEvent(UIEvent.ToggleLike)
        }
    }
}

/** Previous, play / pause, next: three pills in the cover's colour, play the widest. */
@Composable
internal fun ClassicTransportRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val control = state.controllerState
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledButton(SimpIcons.SkipPrevious, Modifier.width(76.dp).height(58.dp), CircleShape, iconSize = 30.dp, enabled = control.isPreviousAvailable) {
            actions.onUIEvent(UIEvent.Previous)
        }
        FilledButton(
            if (control.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
            Modifier.width(116.dp).height(72.dp),
            CircleShape,
            iconSize = 38.dp,
        ) { actions.onUIEvent(UIEvent.PlayPause) }
        FilledButton(SimpIcons.SkipNext, Modifier.width(76.dp).height(58.dp), CircleShape, iconSize = 30.dp, enabled = control.isNextAvailable) {
            actions.onUIEvent(UIEvent.Next)
        }
    }
}

/** Queue, sleep timer, Lyrics, shuffle, repeat as outlined squares; the song menu on the right. */
@Composable
private fun BottomRow(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
) {
    val handler = koinInject<MediaPlayerHandler>()
    val sleepTimer by handler.sleepTimerState.collectAsStateWithLifecycle()
    val timerOn = sleepTimer.timeRemaining > 0 || sleepTimer.timeRemaining == -1
    var picking by remember { mutableStateOf(false) }
    var confirmingOff by remember { mutableStateOf(false) }
    val control = state.controllerState

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(SimpIcons.QueueMusic, onClick = actions.onShowQueue)
        OutlinedButton(SimpIcons.AccessAlarm, active = timerOn) { if (timerOn) confirmingOff = true else picking = true }
        OutlinedButton(SimpIcons.Lyrics, onClick = actions.onShowFullscreenLyrics)
        OutlinedButton(SimpIcons.Shuffle, active = control.isShuffle) { actions.onUIEvent(UIEvent.Shuffle) }
        OutlinedButton(
            if (control.repeatState is RepeatState.One) SimpIcons.RepeatOne else SimpIcons.Repeat,
            active = control.repeatState !is RepeatState.None,
        ) { actions.onUIEvent(UIEvent.Repeat) }
        Spacer(Modifier.weight(1f))
        FilledButton(SimpIcons.MoreVert, Modifier.size(46.dp), CircleShape, onClick = actions.onShowMoreSheet)
    }

    if (picking) {
        SleepTimerBottomSheet(
            onDismiss = { picking = false },
            accent = MaterialTheme.colorScheme.primary,
        ) { minutes -> handler.sleepStart(minutes) }
    }
    if (confirmingOff) {
        AlertDialog(
            onDismissRequest = { confirmingOff = false },
            text = { Text(stringResource(Res.string.sleep_timer_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    handler.sleepStop()
                    confirmingOff = false
                }) { Text(stringResource(Res.string.yes)) }
            },
            dismissButton = { TextButton(onClick = { confirmingOff = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

/** Filled with the cover's colour: like, the transport pills, the song menu. */
@Composable
private fun FilledButton(
    icon: ImageVector,
    modifier: Modifier,
    shape: Shape,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(iconSize))
    }
}

/** A small outlined square; [active] lights it in the cover's colour (shuffle on, a timer running). */
@Composable
private fun OutlinedButton(
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(44.dp)
            .clip(SmallShape)
            .border(1.dp, color.copy(alpha = if (active) 0.9f else 0.3f), SmallShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}
