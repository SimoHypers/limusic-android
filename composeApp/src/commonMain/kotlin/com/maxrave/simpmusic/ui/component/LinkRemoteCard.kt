package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.LinkRemote
import com.maxrave.domain.repository.DeviceLinkRepository
import com.maxrave.simpmusic.ui.icon.Computer
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.icon.Smartphone
import com.maxrave.simpmusic.ui.icon.VolumeDown
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.link_continue_here
import simpmusic.composeapp.generated.resources.link_last_played_on
import simpmusic.composeapp.generated.resources.link_local_file_on
import simpmusic.composeapp.generated.resources.link_paused_on
import simpmusic.composeapp.generated.resources.link_playing_on
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * "Playing on <desktop>" above the mini player, while another linked device is (or was last) the
 * player: play/pause and next as remote buttons, and Continue here. A tap opens the remote sheet.
 */
@Composable
fun LinkRemoteCard(
    modifier: Modifier = Modifier,
    repository: DeviceLinkRepository = koinInject(),
) {
    val link by repository.link.collectAsStateWithLifecycle()
    val player = link.player
    val track = link.track
    if (!link.paired || player == null || track == null) return
    var sheet by remember { mutableStateOf(false) }
    val local = track.id.isBlank()
    val online = player.online
    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { sheet = true }
            .height(58.dp)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(track.thumbnail, Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = typo().titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                status(link),
                style = typo().bodySmall,
                color = if (online && link.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { repository.remote(if (link.isPlaying) LinkRemote.Pause else LinkRemote.Play) }, enabled = online) {
            Icon(if (link.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow, contentDescription = null, tint = if (online) LocalContentColor.current else dim)
        }
        IconButton(onClick = { repository.remote(LinkRemote.Next) }, enabled = online) {
            Icon(SimpIcons.SkipNext, contentDescription = null, tint = if (online) LocalContentColor.current else dim)
        }
        IconButton(onClick = { repository.continueHere() }, enabled = !local) {
            Icon(
                SimpIcons.Smartphone,
                contentDescription = stringResource(Res.string.link_continue_here),
                tint = if (!local) LocalContentColor.current else dim,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    if (sheet) {
        LinkRemoteSheet(
            link = link,
            onRemote = repository::remote,
            onContinue = {
                sheet = false
                repository.continueHere()
            },
            onDismiss = { sheet = false },
        )
    }
}

@Composable
private fun status(link: DeviceLink): String {
    val name = link.player?.name.orEmpty()
    return when {
        link.track?.id.isNullOrBlank() -> stringResource(Res.string.link_local_file_on, name)
        link.player?.online != true -> stringResource(Res.string.link_last_played_on, name)
        link.isPlaying -> stringResource(Res.string.link_playing_on, name)
        else -> stringResource(Res.string.link_paused_on, name)
    }
}

@Composable
private fun Cover(
    url: String,
    modifier: Modifier,
) {
    if (url.isBlank()) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(SimpIcons.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

/** The remote: cover, a seek bar ticking locally, prev/play/next, volume and Continue here. */
@OptIn(ExperimentalTime::class)
@Composable
private fun LinkRemoteSheet(
    link: DeviceLink,
    onRemote: (LinkRemote) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val track = link.track ?: return
    val online = link.player?.online == true
    val local = track.id.isBlank()
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(link.isPlaying) {
        while (link.isPlaying) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(500)
        }
    }
    var seeking by remember { mutableStateOf<Float?>(null) }
    var volume by remember { mutableStateOf<Float?>(null) }

    SettingSheet(title = status(link), onDismiss = onDismiss) { colors ->
        Cover(track.thumbnail, Modifier.fillMaxWidth(0.6f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).align(Alignment.CenterHorizontally))
        Column(Modifier.fillMaxWidth()) {
            Text(track.title, style = typo().titleMedium, color = colors.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = typo().bodyMedium, color = colors.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val duration = track.durationMs
        if (duration > 0L) {
            val position = link.livePositionMs(now)
            Column {
                Slider(
                    value = seeking ?: (position.toFloat() / duration).coerceIn(0f, 1f),
                    onValueChange = { seeking = it },
                    onValueChangeFinished = {
                        seeking?.let { onRemote(LinkRemote.Seek((it * duration).toLong())) }
                        seeking = null
                    },
                    enabled = online && !local,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(clock(seeking?.let { (it * duration).toLong() } ?: position), style = typo().bodySmall, color = colors.subtitle)
                    Text(clock(duration), style = typo().bodySmall, color = colors.subtitle)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onRemote(LinkRemote.Previous) }, enabled = online) {
                Icon(SimpIcons.SkipPrevious, contentDescription = null, tint = if (online) colors.content else colors.disabled)
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (online) MaterialTheme.colorScheme.primary else colors.disabled)
                    .clickable(enabled = online) { onRemote(if (link.isPlaying) LinkRemote.Pause else LinkRemote.Play) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (link.isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = { onRemote(LinkRemote.Next) }, enabled = online) {
                Icon(SimpIcons.SkipNext, contentDescription = null, tint = if (online) colors.content else colors.disabled)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(SimpIcons.VolumeDown, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
            Slider(
                value = volume ?: link.volume.coerceIn(0f, 1f),
                onValueChange = {
                    volume = it
                    onRemote(LinkRemote.SetVolume(it))
                },
                onValueChangeFinished = { volume = null },
                enabled = online,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Icon(SimpIcons.VolumeUp, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
        }
        Button(onClick = onContinue, enabled = !local, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(Res.string.link_continue_here), color = LocalContentColor.current)
        }
    }
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
