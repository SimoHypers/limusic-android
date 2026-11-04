package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.simohypers.limusic.domain.data.entities.GoogleAccountEntity
import com.simohypers.limusic.domain.data.model.account.AccountInfo
import com.simohypers.limusic.ui.component.ActionButton
import com.simohypers.limusic.ui.component.EndOfModalBottomSheet
import com.simohypers.limusic.ui.component.rememberSurfaceDarkColors
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.CheckCircle
import com.simohypers.limusic.ui.icon.Close
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.Logout
import com.simohypers.limusic.ui.icon.PeopleAlt
import com.simohypers.limusic.ui.theme.AppThemeContent
import com.simohypers.limusic.ui.theme.rounded
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.YouTubeAccounts
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.add_account
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.choose_channel
import limusic.composeapp.generated.resources.choose_channel_desc
import limusic.composeapp.generated.resources.no_channels
import limusic.composeapp.generated.resources.remove
import limusic.composeapp.generated.resources.remove_account
import limusic.composeapp.generated.resources.remove_account_desc
import limusic.composeapp.generated.resources.remove_account_title
import limusic.composeapp.generated.resources.retry
import limusic.composeapp.generated.resources.saved_accounts_hint
import limusic.composeapp.generated.resources.sign_in
import limusic.composeapp.generated.resources.sign_in_failed
import limusic.composeapp.generated.resources.sign_in_hint
import limusic.composeapp.generated.resources.sign_in_with_google
import limusic.composeapp.generated.resources.sign_out
import limusic.composeapp.generated.resources.switch_channel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Desktop Limusic's account menu (AccountMenu.svelte) as a bottom sheet, from Home's avatar and
 * Settings' top row. Signed in: you, the other saved accounts (tap to switch, x to remove), Add
 * account, Switch channel, Sign out. Signed out: Sign in with Google, and any saved account one
 * tap from coming back. [onSignIn] opens the sign-in screen; true is Add account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeAccountSheet(
    onSignIn: (addAccount: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val accounts = koinInject<YouTubeAccounts>()
    val saved by accounts.saved.collectAsStateWithLifecycle()
    val active by accounts.active.collectAsStateWithLifecycle()
    val others = saved.filter { it.email != active?.email }
    val scope = rememberCoroutineScope()
    // The row being switched to, so it can show the wait and the rest can't be tapped meanwhile.
    var switching by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<GoogleAccountEntity?>(null) }
    var pickingChannel by remember { mutableStateOf(false) }

    removing?.let { row ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(Res.string.remove_account_title)) },
            text = { Text(stringResource(Res.string.remove_account_desc, row.name)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    scope.launch { accounts.remove(row) }
                }) { Text(stringResource(Res.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }

    AppThemeContent {
        val c = rememberSurfaceDarkColors()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = rounded(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors().copy(containerColor = c.container),
            ) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(5.dp))
                    Card(
                        modifier = Modifier.align(Alignment.CenterHorizontally).width(60.dp).height(4.dp),
                        colors = CardDefaults.cardColors().copy(containerColor = c.handle),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(Modifier.height(14.dp))
                    val me = active
                    if (pickingChannel && me != null) {
                        ChannelSwitcher(accounts, me, onDone = onDismiss)
                    } else {
                        if (me != null) {
                            AccountRow(me, avatarSize = 52.dp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
                        } else {
                            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(Res.string.sign_in), style = typo().titleMedium, color = c.content)
                                Text(
                                    stringResource(if (others.isEmpty()) Res.string.sign_in_hint else Res.string.saved_accounts_hint),
                                    style = typo().bodySmall,
                                    color = c.subtitle,
                                )
                                Spacer(Modifier.height(6.dp))
                                // The WebView's own Google session is the fast way back into whichever
                                // account it last held; Add account is the way to a different one.
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onSignIn(false)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(stringResource(Res.string.sign_in_with_google)) }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        others.forEach { row ->
                            AccountRow(
                                row,
                                avatarSize = 40.dp,
                                modifier =
                                    Modifier
                                        .clickable(enabled = switching == null) {
                                            switching = row.email
                                            scope.launch {
                                                val ok = accounts.switchTo(row)
                                                switching = null
                                                if (ok) onDismiss()
                                            }
                                        }.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            ) {
                                if (switching == row.email) {
                                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = c.content, strokeWidth = 2.dp)
                                    }
                                } else {
                                    IconButton(onClick = { removing = row }, enabled = switching == null) {
                                        Icon(LimusicIcons.Close, stringResource(Res.string.remove_account), tint = c.subtitle, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        if (me != null || others.isNotEmpty()) {
                            ActionButton(icon = LimusicIcons.Add, text = Res.string.add_account) {
                                onDismiss()
                                onSignIn(true)
                            }
                        }
                        if (me != null) {
                            // Always offered, as on desktop: the list is fetched live when opened, so a
                            // one-channel account just shows its one channel.
                            ActionButton(icon = LimusicIcons.PeopleAlt, text = Res.string.switch_channel) { pickingChannel = true }
                            ActionButton(icon = LimusicIcons.Logout, text = Res.string.sign_out) {
                                scope.launch { accounts.signOut() }
                                onDismiss()
                            }
                        }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

/** Switch channel: the live jar's channels, fetched when opened; picking one switches and closes. */
@Composable
private fun ChannelSwitcher(
    accounts: YouTubeAccounts,
    me: GoogleAccountEntity,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var channels by remember { mutableStateOf<List<AccountInfo>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(attempt) {
        failed = false
        channels = accounts.channels()
        failed = channels == null
    }
    var switching by remember { mutableStateOf<AccountInfo?>(null) }
    ChannelPicker(
        channels = channels,
        failed = failed,
        isCurrent = { it.pageId == me.pageId && it.authUser == me.authUser },
        busy = switching,
        onRetry = { attempt++ },
        onPick = { channel ->
            if (channel.pageId == me.pageId && channel.authUser == me.authUser) {
                onDone()
            } else {
                switching = channel
                scope.launch {
                    accounts.switchChannel(channel)
                    onDone()
                }
            }
        },
    )
}

/**
 * Desktop's channel picker body (ChannelPicker.svelte): title, what the choice changes, then one
 * row per channel. The sign-in screen shows it too, when a new sign-in has several channels.
 */
@Composable
fun ChannelPicker(
    channels: List<AccountInfo>?,
    failed: Boolean,
    isCurrent: (AccountInfo) -> Boolean,
    busy: AccountInfo?,
    onRetry: () -> Unit,
    onPick: (AccountInfo) -> Unit,
) {
    val c = rememberSurfaceDarkColors()
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.choose_channel), style = typo().titleMedium, color = c.content)
            Text(stringResource(Res.string.choose_channel_desc), style = typo().bodySmall, color = c.subtitle)
        }
        Spacer(Modifier.height(8.dp))
        when {
            failed ->
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.sign_in_failed), style = typo().bodySmall, color = c.subtitle, textAlign = TextAlign.Center)
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
                }
            channels == null ->
                Box(Modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = c.content, strokeWidth = 2.dp)
                }
            channels.isEmpty() ->
                Text(
                    stringResource(Res.string.no_channels),
                    style = typo().bodySmall,
                    color = c.subtitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            else ->
                channels.forEach { channel ->
                    ChannelRow(
                        channel,
                        current = isCurrent(channel),
                        busy = busy == channel,
                        modifier =
                            Modifier
                                .clickable(enabled = busy == null) { onPick(channel) }
                                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: AccountInfo,
    current: Boolean,
    busy: Boolean,
    modifier: Modifier,
) {
    val c = rememberSurfaceDarkColors()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AccountAvatar(channel.thumbnails.lastOrNull()?.url?.let { if (it.startsWith("//")) "https:$it" else it }, channel.name, 40.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(channel.name, style = typo().labelMedium, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (channel.email.isNotBlank()) {
                Text(channel.email, style = typo().bodySmall, color = c.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        when {
            busy -> CircularProgressIndicator(Modifier.size(20.dp), color = c.content, strokeWidth = 2.dp)
            current -> Icon(LimusicIcons.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}

/** A saved account: photo, name, handle. [trailing] sits at the end (the remove x). */
@Composable
private fun AccountRow(
    row: GoogleAccountEntity,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val c = rememberSurfaceDarkColors()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AccountAvatar(row.thumbnailUrl, row.name, avatarSize)
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(row.name, style = typo().labelMedium, color = c.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The id is the handle when the channel has one; `ga-` ids are internal.
            if (row.email.startsWith("@")) {
                Text(row.email, style = typo().bodySmall, color = c.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

/** The photo, or the name's initial in the accent while there is none (as the artist picker does). */
@Composable
private fun AccountAvatar(
    url: String?,
    name: String,
    size: Dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        val trimmed = name.trim()
        // Two chars when the first is half of a surrogate pair, so an emoji stays whole.
        Text(trimmed.take(if (trimmed.firstOrNull()?.isHighSurrogate() == true) 2 else 1).uppercase(), style = typo().labelMedium, color = accent)
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
