package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.listentogether.LinkedDevice
import com.maxrave.domain.repository.DeviceLinkRepository
import com.maxrave.simpmusic.ui.icon.Computer
import com.maxrave.simpmusic.ui.icon.SimpIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.link_paused_on
import simpmusic.composeapp.generated.resources.link_playing_on

/** The linked device the app's player is the remote for, or null while it is this phone's own. */
@Composable
fun rememberRemoteDevice(): LinkedDevice? {
    val link by koinInject<DeviceLinkRepository>().link.collectAsStateWithLifecycle()
    return link.player?.takeIf { link.controlling }
}

/** "Playing on <device>" in the accent colour, where the player names the device it controls. */
@Composable
fun PlayingOnLabel(
    device: LinkedDevice,
    playing: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(SimpIcons.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(if (playing) Res.string.link_playing_on else Res.string.link_paused_on, device.name),
            style = style,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
