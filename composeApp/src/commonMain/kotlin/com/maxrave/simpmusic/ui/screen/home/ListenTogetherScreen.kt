package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomConnection
import com.maxrave.domain.data.model.listentogether.RoomMember
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.ui.component.CodeInput
import com.maxrave.simpmusic.ui.component.SettingSheet
import com.maxrave.simpmusic.ui.component.codeOf
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.Edit
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PersonAdd
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Star
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.devices_paused
import simpmusic.composeapp.generated.resources.devices_playing
import simpmusic.composeapp.generated.resources.ltd_alone_hint
import simpmusic.composeapp.generated.resources.ltd_approve
import simpmusic.composeapp.generated.resources.ltd_connecting
import simpmusic.composeapp.generated.resources.ltd_copied
import simpmusic.composeapp.generated.resources.ltd_copy
import simpmusic.composeapp.generated.resources.ltd_decline
import simpmusic.composeapp.generated.resources.ltd_err_enter_name
import simpmusic.composeapp.generated.resources.ltd_err_paste_code
import simpmusic.composeapp.generated.resources.ltd_host
import simpmusic.composeapp.generated.resources.ltd_hosting
import simpmusic.composeapp.generated.resources.ltd_join
import simpmusic.composeapp.generated.resources.ltd_join_requests
import simpmusic.composeapp.generated.resources.ltd_join_room
import simpmusic.composeapp.generated.resources.ltd_join_room_hint
import simpmusic.composeapp.generated.resources.ltd_leave_hint_alone
import simpmusic.composeapp.generated.resources.ltd_leave_hint_guest
import simpmusic.composeapp.generated.resources.ltd_leave_hint_host
import simpmusic.composeapp.generated.resources.ltd_leave_room
import simpmusic.composeapp.generated.resources.ltd_listening
import simpmusic.composeapp.generated.resources.ltd_make_host
import simpmusic.composeapp.generated.resources.ltd_make_host_hint
import simpmusic.composeapp.generated.resources.ltd_offline
import simpmusic.composeapp.generated.resources.ltd_people
import simpmusic.composeapp.generated.resources.ltd_remove_from_room
import simpmusic.composeapp.generated.resources.ltd_remove_hint
import simpmusic.composeapp.generated.resources.ltd_resync
import simpmusic.composeapp.generated.resources.ltd_resync_hint
import simpmusic.composeapp.generated.resources.ltd_share_invite
import simpmusic.composeapp.generated.resources.ltd_share_text
import simpmusic.composeapp.generated.resources.ltd_start_room
import simpmusic.composeapp.generated.resources.ltd_start_room_hint
import simpmusic.composeapp.generated.resources.ltd_status_connected
import simpmusic.composeapp.generated.resources.ltd_status_connecting
import simpmusic.composeapp.generated.resources.ltd_status_disconnected
import simpmusic.composeapp.generated.resources.ltd_subtitle
import simpmusic.composeapp.generated.resources.ltd_sync_server
import simpmusic.composeapp.generated.resources.ltd_sync_server_hint
import simpmusic.composeapp.generated.resources.ltd_sync_server_placeholder
import simpmusic.composeapp.generated.resources.ltd_title
import simpmusic.composeapp.generated.resources.ltd_use_default
import simpmusic.composeapp.generated.resources.ltd_waiting_for_host
import simpmusic.composeapp.generated.resources.ltd_you
import simpmusic.composeapp.generated.resources.ltd_your_name
import simpmusic.composeapp.generated.resources.now_playing
import simpmusic.composeapp.generated.resources.save

/**
 * Listen Together, built like the Devices page: the start page (your name, Start a room, Join a
 * room), the wait for the host, and the room (its code, what plays, the people, Leave). The top
 * bar's ⋮ holds the sync server before a room, and a guest's Re-sync in one.
 */
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requesting by viewModel.requesting.collectAsStateWithLifecycle()
    val waiting = !state.inRoom && (requesting || state.pendingJoinCode != null)
    var menu by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        SettingTopBar(
            title = stringResource(Res.string.ltd_title),
            onBack = { navController.navigateUp() },
            windowInsets = WindowInsets(0),
            actions = {
                if (!waiting && !state.isHost) {
                    IconButton(onClick = { menu = true }) {
                        Icon(SimpIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
        Column(Modifier.padding(horizontal = 16.dp).animateContentSize()) {
            when {
                waiting -> Waiting(state.connection, viewModel::cancel)
                !state.inRoom -> Start(state, viewModel)
                else -> Room(state, viewModel)
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }

    if (menu) {
        val close = { menu = false }
        if (state.inRoom) {
            SettingSheet(title = stringResource(Res.string.ltd_title), onDismiss = close) { colors ->
                SheetRow(stringResource(Res.string.ltd_resync), stringResource(Res.string.ltd_resync_hint), colors.content, colors.subtitle) {
                    viewModel.resync()
                    close()
                }
            }
        } else {
            ServerSheet(viewModel, close)
        }
    }
}

@Composable
private fun Waiting(
    connection: RoomConnection,
    onCancel: () -> Unit,
) {
    Spacer(Modifier.height(20.dp))
    SettingCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(Modifier.size(36.dp), strokeWidth = 3.dp)
            Text(
                stringResource(if (connection is RoomConnection.Connected) Res.string.ltd_waiting_for_host else Res.string.ltd_connecting),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onCancel) { Text(stringResource(Res.string.cancel), color = LocalContentColor.current) }
        }
    }
}

/** Not in a room: who you are, then start one or join one. */
@Composable
private fun Start(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
) {
    val savedName by viewModel.savedName.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var editName by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<StringResource?>(null) }
    LaunchedEffect(savedName) { savedName?.takeIf { it.isNotEmpty() }?.let { name = it } }

    val go: (hosting: Boolean) -> Unit = { hosting ->
        localError = null
        viewModel.clearError()
        val n = name.trim()
        when {
            n.isEmpty() -> localError = Res.string.ltd_err_enter_name
            hosting -> viewModel.host(n)
            !viewModel.join(n, invite) -> localError = Res.string.ltd_err_paste_code
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HeaderIcon(SimpIcons.Groups)
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(Res.string.ltd_subtitle),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The name is typed here once; after that it is a row, edited in a sheet.
        when (savedName) {
            null -> Unit
            "" ->
                SettingCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(Res.string.ltd_your_name), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(10.dp))
                        NameField(name) { name = it }
                    }
                }
            else ->
                SettingCard {
                    Row(
                        Modifier.fillMaxWidth().clickable { editName = true }.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.ltd_your_name), style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(name, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { editName = true }) {
                            Icon(SimpIcons.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
        }

        SettingCard {
            Row(
                Modifier.fillMaxWidth().clickable { go(true) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(SimpIcons.Add)
                Spacer(Modifier.width(12.dp))
                TitleAndLine(stringResource(Res.string.ltd_start_room), stringResource(Res.string.ltd_start_room_hint), Modifier.weight(1f))
                Icon(SimpIcons.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }

        SettingCard {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(SimpIcons.PersonAdd)
                    Spacer(Modifier.width(12.dp))
                    TitleAndLine(stringResource(Res.string.ltd_join_room), stringResource(Res.string.ltd_join_room_hint), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                CodeInput(invite, length = ROOM_CODE_LENGTH) { invite = it }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { go(false) },
                    enabled = codeOf(invite, ROOM_CODE_LENGTH).length == ROOM_CODE_LENGTH,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text(stringResource(Res.string.ltd_join), color = LocalContentColor.current) }
            }
        }
    }

    (localError?.let { stringResource(it) } ?: state.error)?.let {
        Text(it, style = typo().bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 10.dp))
    }

    if (editName) {
        var draft by rememberSaveable { mutableStateOf(name) }
        val close = { editName = false }
        SettingSheet(title = stringResource(Res.string.ltd_your_name), onDismiss = close) {
            NameField(draft) { draft = it }
            Button(
                onClick = {
                    name = draft.trim()
                    viewModel.saveName(name)
                    close()
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(stringResource(Res.string.save), color = LocalContentColor.current) }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(ListenTogetherViewModel.MAX_NAME_LENGTH)) },
        singleLine = true,
        placeholder = { Text(stringResource(Res.string.ltd_your_name)) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** In a room: the code to hand out, what plays, who is here, and Leave. */
@Composable
private fun Room(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
) {
    var managing by remember { mutableStateOf<RoomMember?>(null) }
    val others = state.members.filter { it.userId != state.selfUserId }

    Spacer(Modifier.height(8.dp))
    CodeCard(state, viewModel, alone = state.isHost && others.isEmpty() && state.joinRequests.isEmpty())

    if (state.isHost && state.joinRequests.isNotEmpty()) {
        SettingLabel(stringResource(Res.string.ltd_join_requests))
        SettingCard {
            Column(Modifier.padding(vertical = 6.dp)) {
                state.joinRequests.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(r.username, highlight = true)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            r.username,
                            style = typo().labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.rejectJoin(r.userId) }) {
                            Text(stringResource(Res.string.ltd_decline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { viewModel.approveJoin(r.userId) }, contentPadding = PaddingValues(horizontal = 16.dp)) {
                            Text(stringResource(Res.string.ltd_approve), color = LocalContentColor.current)
                        }
                    }
                }
            }
        }
    }

    state.currentTrack?.let { track ->
        SettingLabel(stringResource(Res.string.now_playing))
        SettingCard {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = track.thumbnail.ifBlank { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.width(12.dp))
                TitleAndLine(
                    track.title,
                    listOf(track.artist, stringResource(if (state.isPlaying) Res.string.devices_playing else Res.string.devices_paused))
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    Modifier.weight(1f),
                )
            }
        }
    }

    SettingLabel(stringResource(Res.string.ltd_people, state.members.size))
    SettingCard {
        Column(Modifier.padding(vertical = 6.dp)) {
            state.members.forEach { u ->
                Person(
                    u,
                    isSelf = u.userId == state.selfUserId,
                    onManage = { managing = u }.takeIf { state.isHost && u.userId != state.selfUserId },
                )
            }
        }
    }

    // Leaving, as Devices puts Unlink: a quiet row at the bottom, not a red button.
    Spacer(Modifier.height(28.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = viewModel::leave)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(stringResource(Res.string.ltd_leave_room), style = typo().labelMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(2.dp))
        val host = state.members.firstOrNull { it.isHost }?.username.orEmpty()
        Text(
            when {
                !state.isHost -> stringResource(Res.string.ltd_leave_hint_guest, host)
                others.any { it.isConnected } -> stringResource(Res.string.ltd_leave_hint_host)
                else -> stringResource(Res.string.ltd_leave_hint_alone)
            },
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    managing?.let { u ->
        val close = { managing = null }
        SettingSheet(title = u.username, onDismiss = close) { colors ->
            SheetRow(stringResource(Res.string.ltd_make_host), stringResource(Res.string.ltd_make_host_hint, u.username), colors.content, colors.subtitle) {
                viewModel.transferHost(u.userId)
                close()
            }
            SheetRow(stringResource(Res.string.ltd_remove_from_room), stringResource(Res.string.ltd_remove_hint, u.username), MaterialTheme.colorScheme.error, colors.subtitle) {
                viewModel.kick(u.userId)
                close()
            }
        }
    }
}

/** The room's code, big enough to read out, with Share and Copy under it. */
@Composable
private fun CodeCard(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
    alone: Boolean,
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val code = state.roomCode.orEmpty()
    val invite = ListenTogetherViewModel.makeInvite(serverUrl, code)
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val shareTitle = stringResource(Res.string.ltd_title)
    val shareText = stringResource(Res.string.ltd_share_text, invite)

    SettingCard {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val connected = state.connection is RoomConnection.Connected
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.width(6.dp))
                val status =
                    when (state.connection) {
                        is RoomConnection.Connected -> Res.string.ltd_status_connected
                        RoomConnection.Connecting -> Res.string.ltd_status_connecting
                        else -> Res.string.ltd_status_disconnected
                    }
                Text(
                    stringResource(status) + " · " + stringResource(if (state.isHost) Res.string.ltd_hosting else Res.string.ltd_listening),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            // In two halves, the way it gets read out and typed.
            Text(
                code.chunked(4).joinToString(" "),
                style = typo().headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (serverUrl.isNotEmpty()) {
                Text(
                    invite,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { shareUrl(shareTitle, shareText) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(SimpIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.ltd_share_invite), color = LocalContentColor.current)
                }
                FilledTonalButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(invite))
                        copied = true
                        scope.launch {
                            delay(1600)
                            copied = false
                        }
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(if (copied) SimpIcons.Check else SimpIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (copied) Res.string.ltd_copied else Res.string.ltd_copy), color = LocalContentColor.current)
                }
            }
            if (alone) {
                Text(
                    stringResource(Res.string.ltd_alone_hint),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Person(
    u: RoomMember,
    isSelf: Boolean,
    onManage: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = if (onManage != null) 4.dp else 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(u.username, highlight = u.isHost, modifier = Modifier.alpha(if (u.isConnected) 1f else 0.5f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(u.username, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!u.isConnected) {
                Text(stringResource(Res.string.ltd_offline), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val tags = listOfNotNull(stringResource(Res.string.ltd_host).takeIf { u.isHost }, stringResource(Res.string.ltd_you).takeIf { isSelf })
        if (tags.isNotEmpty()) {
            if (u.isHost) {
                Icon(SimpIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                tags.joinToString(" · "),
                style = typo().bodySmall,
                color = if (u.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onManage != null) {
            IconButton(onClick = onManage) {
                Icon(SimpIcons.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The first letter in a circle, tinted for the host or someone asking in. */
@Composable
private fun Avatar(
    name: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = typo().labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoundIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TitleAndLine(
    title: String,
    line: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(line, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A sheet row as Devices draws its Unlink: a title and one line under it. */
@Composable
private fun SheetRow(
    title: String,
    line: String,
    titleColor: Color,
    lineColor: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(title, style = typo().labelMedium, color = titleColor)
        Spacer(Modifier.height(2.dp))
        Text(line, style = typo().bodySmall, color = lineColor)
    }
}

/** The sync server, for the few who run their own. Applies from the next room. */
@Composable
private fun ServerSheet(
    viewModel: ListenTogetherViewModel,
    onDismiss: () -> Unit,
) {
    val saved by viewModel.serverUrl.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf(saved.ifEmpty { ListenTogetherViewModel.DEFAULT_SERVER_URL }) }
    SettingSheet(title = stringResource(Res.string.ltd_sync_server), onDismiss = onDismiss) { colors ->
        Text(stringResource(Res.string.ltd_sync_server_hint), style = typo().bodySmall, color = colors.subtitle)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.ltd_sync_server_placeholder)) },
            textStyle = typo().bodyMedium,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (draft.trim() != ListenTogetherViewModel.DEFAULT_SERVER_URL) {
                TextButton(onClick = { draft = ListenTogetherViewModel.DEFAULT_SERVER_URL }) {
                    Text(stringResource(Res.string.ltd_use_default), color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.setServer(draft)
                    onDismiss()
                },
                enabled = draft.isNotBlank(),
            ) { Text(stringResource(Res.string.save), color = LocalContentColor.current) }
        }
    }
}

private const val ROOM_CODE_LENGTH = 8
