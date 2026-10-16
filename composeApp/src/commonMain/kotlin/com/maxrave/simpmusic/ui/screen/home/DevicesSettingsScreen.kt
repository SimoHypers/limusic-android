package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.data.model.listentogether.LinkConnection
import com.maxrave.domain.data.model.listentogether.LinkedDevice
import com.maxrave.domain.data.model.listentogether.PairResult
import com.maxrave.simpmusic.ui.component.CodeInput
import com.maxrave.simpmusic.ui.component.SettingGroup
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.component.codeOf
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Computer
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.DeviceLinkViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.devices
import simpmusic.composeapp.generated.resources.devices_connecting
import simpmusic.composeapp.generated.resources.devices_intro
import simpmusic.composeapp.generated.resources.devices_link
import simpmusic.composeapp.generated.resources.devices_linked
import simpmusic.composeapp.generated.resources.devices_offline
import simpmusic.composeapp.generated.resources.devices_online
import simpmusic.composeapp.generated.resources.devices_playing
import simpmusic.composeapp.generated.resources.devices_unlink
import simpmusic.composeapp.generated.resources.devices_unlink_phone
import simpmusic.composeapp.generated.resources.devices_unlink_phone_hint
import simpmusic.composeapp.generated.resources.devices_unreachable
import simpmusic.composeapp.generated.resources.devices_unsupported
import simpmusic.composeapp.generated.resources.devices_wrong_code

private const val PAIR_CODE_LENGTH = 6

/**
 * Settings > Integrations > Devices. Not paired: the code from the desktop's Devices tab and Link.
 * Paired: the linked devices, Unlink for each, and Unlink this phone.
 */
@Composable
fun DevicesSettingsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: DeviceLinkViewModel = koinViewModel(),
) {
    val link by viewModel.link.collectAsStateWithLifecycle()
    val pairing by viewModel.pairing.collectAsStateWithLifecycle()
    val pairError by viewModel.pairError.collectAsStateWithLifecycle()
    var invite by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        SettingTopBar(title = stringResource(Res.string.devices), onBack = { navController.navigateUp() }, windowInsets = WindowInsets(0))
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (!link.paired) {
                SettingGroup(title = null) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            stringResource(Res.string.devices_intro),
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
            } else {
                SettingGroup(
                    title = stringResource(Res.string.devices_linked),
                    footer =
                        when {
                            link.connection == LinkConnection.Unsupported -> {
                                { Text(stringResource(Res.string.devices_unsupported), style = typo().bodySmall, color = MaterialTheme.colorScheme.error) }
                            }
                            link.connection != LinkConnection.Connected -> {
                                { Text(stringResource(Res.string.devices_connecting), style = typo().bodySmall) }
                            }
                            else -> null
                        },
                ) {
                    link.devices.forEach { DeviceRow(it) { viewModel.unlink(it.id) } }
                }
                SettingGroup(title = null) {
                    SettingItem(
                        title = stringResource(Res.string.devices_unlink_phone),
                        subtitle = stringResource(Res.string.devices_unlink_phone_hint),
                        smallSubtitle = true,
                        onClick = { viewModel.unlinkThisPhone() },
                    )
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }
}

@Composable
private fun DeviceRow(
    device: LinkedDevice,
    onUnlink: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(SimpIcons.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            val status = stringResource(if (device.online) Res.string.devices_online else Res.string.devices_offline)
            val playing = stringResource(Res.string.devices_playing)
            Text(if (device.playing) "$status · $playing" else status, style = typo().bodySmall)
        }
        TextButton(onClick = onUnlink) { Text(stringResource(Res.string.devices_unlink)) }
    }
}
