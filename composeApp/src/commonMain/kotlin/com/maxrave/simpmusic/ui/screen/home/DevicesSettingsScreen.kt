package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.listentogether.DeviceLink
import com.maxrave.domain.data.model.listentogether.LinkConnection
import com.maxrave.domain.data.model.listentogether.LinkedDevice
import com.maxrave.domain.data.model.listentogether.PairResult
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.simpmusic.ui.component.CodeInput
import com.maxrave.simpmusic.ui.component.SettingSheet
import com.maxrave.simpmusic.ui.component.codeOf
import com.maxrave.simpmusic.ui.icon.Computer
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Smartphone
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.DeviceLinkViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.devices
import simpmusic.composeapp.generated.resources.devices_connect_title
import simpmusic.composeapp.generated.resources.devices_connecting
import simpmusic.composeapp.generated.resources.devices_linked
import simpmusic.composeapp.generated.resources.devices_offline
import simpmusic.composeapp.generated.resources.devices_online
import simpmusic.composeapp.generated.resources.devices_paused
import simpmusic.composeapp.generated.resources.devices_play_here
import simpmusic.composeapp.generated.resources.devices_play_on_computer
import simpmusic.composeapp.generated.resources.devices_playing
import simpmusic.composeapp.generated.resources.devices_playing_here
import simpmusic.composeapp.generated.resources.devices_ready
import simpmusic.composeapp.generated.resources.devices_remote_for
import simpmusic.composeapp.generated.resources.devices_step_open
import simpmusic.composeapp.generated.resources.devices_step_type
import simpmusic.composeapp.generated.resources.devices_subtitle
import simpmusic.composeapp.generated.resources.devices_this_phone
import simpmusic.composeapp.generated.resources.devices_title
import simpmusic.composeapp.generated.resources.devices_link
import simpmusic.composeapp.generated.resources.devices_unlink
import simpmusic.composeapp.generated.resources.devices_unlink_hint
import simpmusic.composeapp.generated.resources.devices_unlink_phone
import simpmusic.composeapp.generated.resources.devices_unlink_phone_hint
import simpmusic.composeapp.generated.resources.devices_unreachable
import simpmusic.composeapp.generated.resources.devices_unsupported
import simpmusic.composeapp.generated.resources.devices_wrong_code

private const val PAIR_CODE_LENGTH = 6

/**
 * Settings > Integrations > Devices. Not paired: the two steps and the code from the computer's
 * Devices tab. Paired: device cards, this phone first, then each linked computer with what it
 * plays and a button to move the music there or back; Unlink in each card's ⋮ sheet.
 */
@Composable
fun DevicesSettingsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: DeviceLinkViewModel = koinViewModel(),
) {
    val link by viewModel.link.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<LinkedDevice?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        SettingTopBar(title = stringResource(Res.string.devices), onBack = { navController.navigateUp() }, windowInsets = WindowInsets(0))
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (!link.paired) {
                Pairing(viewModel)
            } else {
                Header(stringResource(Res.string.devices_title), stringResource(Res.string.devices_subtitle))
                SettingLabel(stringResource(Res.string.devices_this_phone))
                ThisPhoneCard(link, onPlayHere = { viewModel.playOn(null) })
                SettingLabel(stringResource(Res.string.devices_linked))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    link.devices.forEach { device ->
                        DeviceCard(link, device, onPlayOn = { viewModel.playOn(device.id) }, onMore = { menuFor = device })
                    }
                }
                when {
                    link.connection == LinkConnection.Unsupported ->
                        Note(stringResource(Res.string.devices_unsupported), MaterialTheme.colorScheme.error)
                    link.connection != LinkConnection.Connected ->
                        Note(stringResource(Res.string.devices_connecting), MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(28.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.unlinkThisPhone() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(Res.string.devices_unlink_phone), style = typo().labelMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(Res.string.devices_unlink_phone_hint), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }

    menuFor?.let { device ->
        SettingSheet(title = device.name, onDismiss = { menuFor = null }) { colors ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.unlink(device.id)
                        menuFor = null
                    }.padding(vertical = 10.dp),
            ) {
                Text(stringResource(Res.string.devices_unlink), style = typo().labelMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(Res.string.devices_unlink_hint, device.name), style = typo().bodySmall, color = colors.subtitle)
            }
        }
    }
}

/** Not paired yet: the icon pair, two numbered steps, the code boxes and Link. */
@Composable
private fun Pairing(viewModel: DeviceLinkViewModel) {
    val pairing by viewModel.pairing.collectAsStateWithLifecycle()
    val pairError by viewModel.pairError.collectAsStateWithLifecycle()
    var invite by rememberSaveable { mutableStateOf("") }
    Header(stringResource(Res.string.devices_connect_title), subtitle = null, phoneFirst = true)
    SettingCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Step(1, stringResource(Res.string.devices_step_open))
            Step(2, stringResource(Res.string.devices_step_type))
            CodeInput(invite, length = PAIR_CODE_LENGTH) { invite = it }
            pairError?.let {
                Text(
                    stringResource(
                        when (it) {
                            PairResult.Unsupported -> Res.string.devices_unsupported
                            PairResult.Unreachable -> Res.string.devices_unreachable
                            else -> Res.string.devices_wrong_code
                        },
                    ),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { viewModel.pair(invite) },
                enabled = !pairing && codeOf(invite, PAIR_CODE_LENGTH).length == PAIR_CODE_LENGTH,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(if (pairing) Res.string.devices_connecting else Res.string.devices_link), color = LocalContentColor.current)
            }
        }
    }
}

@Composable
private fun ThisPhoneCard(
    link: DeviceLink,
    onPlayHere: () -> Unit,
) {
    // The app's player: while this phone is a remote it shows the computer, so the phone's own song
    // only counts when it isn't.
    val handler = koinInject<MediaPlayerHandler>()
    val control by handler.controlState.collectAsStateWithLifecycle()
    val nowPlaying by handler.nowPlayingState.collectAsStateWithLifecycle()
    val song = nowPlaying.songEntity.takeIf { !link.controlling && nowPlaying.isNotEmpty() }
    val remoteFor = link.player?.takeIf { link.controlling }
    SettingCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeviceIcon(SimpIcons.Smartphone, active = song != null && control.isPlaying)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(link.selfName, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when {
                            remoteFor != null -> stringResource(Res.string.devices_remote_for, remoteFor.name)
                            song == null -> stringResource(Res.string.devices_ready)
                            control.isPlaying -> stringResource(Res.string.devices_playing_here)
                            else -> stringResource(Res.string.devices_paused)
                        },
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (remoteFor != null) {
                    Button(onClick = onPlayHere) { Text(stringResource(Res.string.devices_play_here), color = LocalContentColor.current) }
                }
            }
            song?.let {
                NowPlayingRow(
                    cover = it.thumbnails,
                    title = it.title,
                    artist = it.artistName?.joinToString(", ").orEmpty(),
                    state = stringResource(if (control.isPlaying) Res.string.devices_playing else Res.string.devices_paused),
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    link: DeviceLink,
    device: LinkedDevice,
    onPlayOn: () -> Unit,
    onMore: () -> Unit,
) {
    val isPlayer = link.player?.id == device.id
    val track = link.track?.takeIf { isPlayer }
    SettingCard {
        Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 16.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeviceIcon(SimpIcons.Computer, active = isPlayer && link.isPlaying && device.online)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                when {
                                    !device.online -> Res.string.devices_offline
                                    track != null && link.isPlaying -> Res.string.devices_playing
                                    track != null -> Res.string.devices_paused
                                    else -> Res.string.devices_online
                                },
                            ),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (device.online && !(link.controlling && isPlayer)) {
                    FilledTonalButton(onClick = onPlayOn) {
                        Text(stringResource(Res.string.devices_play_on_computer), color = LocalContentColor.current)
                    }
                }
                IconButton(onClick = onMore) {
                    Icon(SimpIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            track?.let {
                Box(Modifier.padding(end = 12.dp)) {
                    NowPlayingRow(
                        cover = it.thumbnail.takeIf { url -> url.isNotBlank() },
                        title = it.title,
                        artist = it.artist,
                        state = stringResource(if (link.isPlaying) Res.string.devices_playing else Res.string.devices_paused),
                    )
                }
            }
        }
    }
}

// A card on the settings page's card colour, 20dp corners: the SettingGroup look, one device each.
// Listen Together's page is built of the same.
@Composable
internal fun SettingCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)),
    ) { content() }
}

@Composable
private fun DeviceIcon(
    icon: ImageVector,
    active: Boolean,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NowPlayingRow(
    cover: String?,
    title: String,
    artist: String,
    state: String,
) {
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$artist · $state", style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The icon pair, the title and one line, over the page. */
@Composable
private fun Header(
    title: String,
    subtitle: String?,
    phoneFirst: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderIcon(if (phoneFirst) SimpIcons.Smartphone else SimpIcons.Computer)
            HorizontalDivider(Modifier.width(28.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            HeaderIcon(if (phoneFirst) SimpIcons.Computer else SimpIcons.Smartphone)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = typo().titleMedium, color = MaterialTheme.colorScheme.onSurface)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun HeaderIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Step(
    number: Int,
    text: String,
) {
    Row {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A group's small header, as SettingGroup draws it. */
@Composable
internal fun SettingLabel(text: String) {
    Text(
        text.toUpperCase(Locale.current),
        style = typo().labelMedium.copy(fontWeight = FontWeight.Normal),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun Note(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(text, style = typo().bodySmall, color = color, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
}
