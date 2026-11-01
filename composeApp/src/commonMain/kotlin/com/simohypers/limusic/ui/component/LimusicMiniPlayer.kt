package com.simohypers.limusic.ui.component

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.ui.icon.Favorite
import com.simohypers.limusic.ui.icon.FavoriteBorder
import com.simohypers.limusic.ui.icon.Pause
import com.simohypers.limusic.ui.icon.PlayArrow
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.SkipNext
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SharedViewModel
import com.simohypers.limusic.viewModel.UIEvent
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** How far a drag has to travel, in px, before it counts as a swipe. */
private const val SWIPE_THRESHOLD = 120f

/**
 * The bar above the tabs while something plays. A tap or an upward swipe opens the full player; a
 * sideways swipe skips to the next or previous track.
 */
@Composable
fun LimusicMiniPlayer(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val control by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val song = nowPlaying?.songEntity
    val remoteDevice = rememberRemoteDevice()
    val progress = if (timeline.total > 0L && timeline.current >= 0L) timeline.current.toFloat() / timeline.total else 0f
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val onBackground = MaterialTheme.colorScheme.onSurface

    Column(
        modifier
            .fillMaxWidth()
            .clip(rounded(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                    },
                    onDragEnd = { if (dragged < -SWIPE_THRESHOLD / 2) onOpen() },
                )
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                scope.launch { offsetX.snapTo(offsetX.value + amount) }
                            },
                            onDragEnd = {
                                scope.launch {
                                    when {
                                        offsetX.value > SWIPE_THRESHOLD -> sharedViewModel.onUIEvent(UIEvent.Previous)
                                        offsetX.value < -SWIPE_THRESHOLD -> sharedViewModel.onUIEvent(UIEvent.Next)
                                    }
                                    offsetX.animateTo(0f)
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(song?.thumbnails)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(song?.thumbnails)
                            .crossfade(300)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(rounded(6.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song?.title.orEmpty(),
                        style = typo().titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (remoteDevice != null) {
                        PlayingOnLabel(remoteDevice, playing = control.isPlaying, style = typo().bodySmall)
                    } else {
                        Text(
                            text = song?.artistName?.joinToString(", ").orEmpty(),
                            style = typo().bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(onClick = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) }) {
                Icon(
                    imageVector = if (control.isLiked) LimusicIcons.Favorite else LimusicIcons.FavoriteBorder,
                    contentDescription = null,
                    tint = if (control.isLiked) MaterialTheme.colorScheme.primary else onBackground,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (control.isPlaying) LimusicIcons.Pause else LimusicIcons.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = { sharedViewModel.onUIEvent(UIEvent.Next) }, enabled = control.isNextAvailable) {
                Icon(
                    LimusicIcons.SkipNext,
                    contentDescription = null,
                    tint = if (control.isNextAvailable) onBackground else onBackground.copy(alpha = 0.35f),
                )
            }
        }
        // Thin progress line along the bottom edge, as on the desktop player bar.
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(onBackground.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
