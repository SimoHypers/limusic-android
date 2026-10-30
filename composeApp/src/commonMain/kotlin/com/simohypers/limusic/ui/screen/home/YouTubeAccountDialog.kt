package com.simohypers.limusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.domain.utils.LocalResource
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.ui.component.ActionButton
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.PeopleAlt
import com.simohypers.limusic.ui.icon.PlaylistAdd
import com.simohypers.limusic.ui.icon.Settings
import com.simohypers.limusic.ui.icon.SimpIcons
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SettingsViewModel
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.add_an_account
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.crossfade
import limusic.composeapp.generated.resources.guest
import limusic.composeapp.generated.resources.log_out
import limusic.composeapp.generated.resources.log_out_warning
import limusic.composeapp.generated.resources.no_account
import limusic.composeapp.generated.resources.signed_in
import limusic.composeapp.generated.resources.warning
import limusic.composeapp.generated.resources.youtube_account

/**
 * The YouTube account picker: switch to a saved account, carry on as a guest, log out of all of
 * them, or add one. Settings' account card opens it, and so does Home's avatar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeAccountDialog(
    viewModel: SettingsViewModel,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.getAllGoogleAccount() }
    var confirmLogOut by remember { mutableStateOf(false) }
    if (confirmLogOut) {
        AlertDialog(
            onDismissRequest = { confirmLogOut = false },
            title = { Text(stringResource(Res.string.warning), style = typo().titleSmall) },
            text = { Text(stringResource(Res.string.log_out_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logOutAllYouTube()
                    confirmLogOut = false
                    onDismiss()
                }) { Text(stringResource(Res.string.log_out)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogOut = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.wrapContentSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            shadowElevation = 1.dp,
        ) {
            val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                minActiveState = Lifecycle.State.RESUMED,
            )
            LaunchedEffect(googleAccounts) {
                Logger.d(
                    "SettingScreen",
                    "LaunchedEffect: ${
                        googleAccounts.data?.map {
                            it.name to it.isUsed
                        }
                    }",
                )
            }
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                    ) {
                        IconButton(
                            onClick = { onDismiss() },
                            colors =
                                IconButtonDefaults.iconButtonColors().copy(
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxHeight(),
                        ) {
                            Icon(SimpIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            stringResource(Res.string.youtube_account),
                            style = typo().titleMedium,
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                    .wrapContentWidth(),
                        )
                    }
                }
                if (googleAccounts is LocalResource.Success) {
                    val data = googleAccounts.data
                    if (data.isNullOrEmpty()) {
                        item {
                            Text(
                                stringResource(Res.string.no_account),
                                style = typo().bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier =
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                            )
                        }
                    } else {
                        items(data) {
                            Row(
                                modifier =
                                    Modifier
                                        .padding(vertical = 8.dp)
                                        .clickable {
                                            viewModel.setUsedAccount(it)
                                        },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(24.dp))
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(it.thumbnailUrl)
                                            .crossfade(550)
                                            .build(),
                                    placeholder = rememberVectorPainter(SimpIcons.PeopleAlt),
                                    error = rememberVectorPainter(SimpIcons.PeopleAlt),
                                    contentDescription = it.name,
                                    modifier =
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(it.name, style = typo().labelMedium, color = MaterialTheme.colorScheme.onBackground)
                                    Text(it.email, style = typo().bodySmall)
                                }
                                Spacer(Modifier.width(12.dp))
                                AnimatedVisibility(it.isUsed) {
                                    Text(
                                        stringResource(Res.string.signed_in),
                                        style = typo().bodySmall,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.widthIn(0.dp, 64.dp),
                                    )
                                }
                                Spacer(Modifier.width(24.dp))
                            }
                        }
                    }
                } else {
                    item {
                        CenterLoadingBox(
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                        )
                    }
                }
                item {
                    Column {
                        ActionButton(
                            icon = SimpIcons.PeopleAlt,
                            text = Res.string.guest,
                        ) {
                            viewModel.setUsedAccount(null)
                            onDismiss()
                        }
                        ActionButton(
                            icon = SimpIcons.Close,
                            text = Res.string.log_out,
                        ) { confirmLogOut = true }
                        ActionButton(
                            icon = SimpIcons.PlaylistAdd,
                            text = Res.string.add_an_account,
                        ) {
                            onDismiss()
                            onAddAccount()
                        }
                    }
                }
            }
        }
    }
}
