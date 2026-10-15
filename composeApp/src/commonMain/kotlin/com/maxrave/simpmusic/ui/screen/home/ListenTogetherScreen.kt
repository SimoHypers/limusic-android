package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomConnection
import com.maxrave.domain.data.model.listentogether.RoomMember
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.ContentCopy
import com.maxrave.simpmusic.ui.icon.Groups
import com.maxrave.simpmusic.ui.icon.Logout
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Star
import com.maxrave.simpmusic.ui.icon.StarBorder
import com.maxrave.simpmusic.ui.icon.Sync
import com.maxrave.simpmusic.ui.icon.Tune
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.ltd_alone_hint
import simpmusic.composeapp.generated.resources.ltd_approve
import simpmusic.composeapp.generated.resources.ltd_change_server
import simpmusic.composeapp.generated.resources.ltd_connecting
import simpmusic.composeapp.generated.resources.ltd_copied
import simpmusic.composeapp.generated.resources.ltd_copy_code
import simpmusic.composeapp.generated.resources.ltd_copy_invite
import simpmusic.composeapp.generated.resources.ltd_custom_server
import simpmusic.composeapp.generated.resources.ltd_decline
import simpmusic.composeapp.generated.resources.ltd_dismiss
import simpmusic.composeapp.generated.resources.ltd_err_enter_name
import simpmusic.composeapp.generated.resources.ltd_err_paste_code
import simpmusic.composeapp.generated.resources.ltd_from_user
import simpmusic.composeapp.generated.resources.ltd_host_blurb
import simpmusic.composeapp.generated.resources.ltd_host_tab
import simpmusic.composeapp.generated.resources.ltd_hosting
import simpmusic.composeapp.generated.resources.ltd_in_room
import simpmusic.composeapp.generated.resources.ltd_invite_code
import simpmusic.composeapp.generated.resources.ltd_invite_hint
import simpmusic.composeapp.generated.resources.ltd_join_button
import simpmusic.composeapp.generated.resources.ltd_join_requests
import simpmusic.composeapp.generated.resources.ltd_join_tab
import simpmusic.composeapp.generated.resources.ltd_leave
import simpmusic.composeapp.generated.resources.ltd_listening
import simpmusic.composeapp.generated.resources.ltd_make_host
import simpmusic.composeapp.generated.resources.ltd_queue_it
import simpmusic.composeapp.generated.resources.ltd_remove
import simpmusic.composeapp.generated.resources.ltd_resync
import simpmusic.composeapp.generated.resources.ltd_share_hint
import simpmusic.composeapp.generated.resources.ltd_start_button
import simpmusic.composeapp.generated.resources.ltd_status_connected
import simpmusic.composeapp.generated.resources.ltd_status_connecting
import simpmusic.composeapp.generated.resources.ltd_status_disconnected
import simpmusic.composeapp.generated.resources.ltd_subtitle
import simpmusic.composeapp.generated.resources.ltd_suggestions
import simpmusic.composeapp.generated.resources.ltd_sync_server_hint
import simpmusic.composeapp.generated.resources.ltd_sync_server_placeholder
import simpmusic.composeapp.generated.resources.ltd_title
import simpmusic.composeapp.generated.resources.ltd_use_default
import simpmusic.composeapp.generated.resources.ltd_waiting_for_host
import simpmusic.composeapp.generated.resources.ltd_you
import simpmusic.composeapp.generated.resources.ltd_your_name

// Desktop's Tailwind green-500 and amber-500, for the connection and host marks.
private val Green = Color(0xFF22C55E)
private val Amber = Color(0xFFF59E0B)

/** Listen Together, laid out like Limusic desktop's dialog: setup, waiting, or the room. */
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requesting by viewModel.requesting.collectAsStateWithLifecycle()
    val waiting = !state.inRoom && (requesting || state.pendingJoinCode != null)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 24.dp)
                .animateContentSize(),
    ) {
        Box(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            RippleIconButton(SimpIcons.ArrowBackIosNew, Modifier.size(32.dp), true, tint = MaterialTheme.colorScheme.onSurface) {
                navController.navigateUp()
            }
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(SimpIcons.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text(stringResource(Res.string.ltd_title), style = typo().titleLarge)
            }
            if (!state.inRoom && !waiting) {
                Text(
                    stringResource(Res.string.ltd_subtitle),
                    style = typo().bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            when {
                waiting -> Waiting(state.connection, viewModel::cancel)
                !state.inRoom -> Setup(state, viewModel)
                else -> Room(state, viewModel)
            }
        }
    }
}

@Composable
private fun Waiting(
    connection: RoomConnection,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
        Text(
            stringResource(if (connection is RoomConnection.Connected) Res.string.ltd_waiting_for_host else Res.string.ltd_connecting),
            style = typo().bodyMedium,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel), color = LocalContentColor.current) }
    }
}

@Composable
private fun Setup(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
) {
    val savedName by viewModel.savedName.collectAsStateWithLifecycle()
    val savedServer by viewModel.serverUrl.collectAsStateWithLifecycle()
    var hostMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf("") }
    var showServer by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<StringResource?>(null) }
    LaunchedEffect(savedName) { if (name.isEmpty()) name = savedName }
    LaunchedEffect(savedServer) { if (!showServer) server = savedServer }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Segmented(
            left = stringResource(Res.string.ltd_join_tab),
            right = stringResource(Res.string.ltd_host_tab),
            rightSelected = hostMode,
            onSelect = { hostMode = it },
        )

        // Both paths need a name, so it is asked once, above the split.
        Field(stringResource(Res.string.ltd_your_name)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(ListenTogetherViewModel.MAX_NAME_LENGTH) },
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.ltd_your_name)) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val error = localError?.let { stringResource(it) } ?: state.error
        if (error != null) {
            Text(error, style = typo().bodySmall, color = MaterialTheme.colorScheme.error)
        }

        val start: () -> Unit = {
            localError = null
            viewModel.clearError()
            when {
                name.isBlank() -> localError = Res.string.ltd_err_enter_name
                hostMode -> viewModel.host(name.trim(), if (showServer) server else savedServer)
                !viewModel.join(name.trim(), invite) -> localError = Res.string.ltd_err_paste_code
            }
        }

        if (!hostMode) {
            Field(stringResource(Res.string.ltd_invite_code), hint = stringResource(Res.string.ltd_invite_hint)) {
                CodeInput(invite) { invite = it }
            }
            FullButton(stringResource(Res.string.ltd_join_button), onClick = start)
        } else {
            Note(SimpIcons.Add, stringResource(Res.string.ltd_host_blurb))
            FullButton(stringResource(Res.string.ltd_start_button), onClick = start)

            // The server address lives behind this, and nowhere else. Almost nobody runs one, so
            // the default path never shows a URL at all.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            showServer = !showServer
                            if (showServer && server.isEmpty()) server = ListenTogetherViewModel.DEFAULT_SERVER_URL
                        }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(SimpIcons.Tune, null, modifier = Modifier.size(16.dp))
                Text(stringResource(Res.string.ltd_change_server), style = typo().bodySmall)
                if (savedServer.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.ltd_custom_server),
                        style = typo().bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            AnimatedVisibility(showServer) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(Res.string.ltd_sync_server_placeholder)) },
                        textStyle = typo().bodySmall.copy(fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(Res.string.ltd_sync_server_hint), style = typo().bodySmall, modifier = Modifier.weight(1f))
                        if (server.trim().let { it.isNotEmpty() && it != ListenTogetherViewModel.DEFAULT_SERVER_URL }) {
                            Text(
                                stringResource(Res.string.ltd_use_default),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { server = ListenTogetherViewModel.DEFAULT_SERVER_URL },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Room(
    state: ListenTogetherRoom,
    viewModel: ListenTogetherViewModel,
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val onCustomServer = serverUrl.isNotEmpty()
    val code = state.roomCode.orEmpty()
    val invite = ListenTogetherViewModel.makeInvite(serverUrl, code)
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // The invite, which is the only thing a host has to act on.
        Card(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(state.connection)
                val status =
                    when (state.connection) {
                        is RoomConnection.Connected -> Res.string.ltd_status_connected
                        RoomConnection.Connecting -> Res.string.ltd_status_connecting
                        else -> Res.string.ltd_status_disconnected
                    }
                Text(
                    stringResource(status) + " · " + stringResource(if (state.isHost) Res.string.ltd_hosting else Res.string.ltd_listening),
                    style = typo().bodySmall.copy(fontWeight = FontWeight.Medium),
                )
            }
            // Spaced out because this gets read aloud and typed by hand.
            Text(
                code,
                style =
                    typo().titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.em,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (onCustomServer) {
                Text(
                    invite,
                    style = typo().bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(invite))
                    copied = true
                    scope.launch {
                        delay(1600)
                        copied = false
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Icon(if (copied) SimpIcons.Check else SimpIcons.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        when {
                            copied -> Res.string.ltd_copied
                            onCustomServer -> Res.string.ltd_copy_invite
                            else -> Res.string.ltd_copy_code
                        },
                    ),
                    color = LocalContentColor.current,
                )
            }
            Text(stringResource(Res.string.ltd_share_hint), style = typo().bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        state.currentTrack?.let { track ->
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (track.thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = track.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (state.isHost && state.joinRequests.isNotEmpty()) {
            Section(stringResource(Res.string.ltd_join_requests)) {
                state.joinRequests.forEach { p ->
                    Card(Modifier.fillMaxWidth().padding(8.dp), small = true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Initial(p.username, highlight = true, connected = true)
                            Text(p.username, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            SmallButton(stringResource(Res.string.ltd_approve)) { viewModel.approveJoin(p.userId) }
                            TextButton(onClick = { viewModel.rejectJoin(p.userId) }) { Text(stringResource(Res.string.ltd_decline), color = LocalContentColor.current) }
                        }
                    }
                }
            }
        }

        Section(stringResource(Res.string.ltd_in_room, state.members.size)) {
            state.members.forEach { u -> Participant(u, isSelf = u.userId == state.selfUserId, canManage = state.isHost, viewModel) }
            val others = state.members.count { it.userId != state.selfUserId }
            if (state.isHost && others == 0 && state.joinRequests.isEmpty()) {
                Text(stringResource(Res.string.ltd_alone_hint), style = typo().bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (state.isHost && state.suggestions.isNotEmpty()) {
            Section(stringResource(Res.string.ltd_suggestions)) {
                state.suggestions.forEach { s ->
                    Card(Modifier.fillMaxWidth().padding(8.dp), small = true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(s.track.title, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    s.track.artist + " · " + stringResource(Res.string.ltd_from_user, s.fromUsername),
                                    style = typo().bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SmallButton(stringResource(Res.string.ltd_queue_it)) { viewModel.approveSuggestion(s.suggestionId) }
                            IconButton(onClick = { viewModel.rejectSuggestion(s.suggestionId) }) {
                                Icon(SimpIcons.Close, stringResource(Res.string.ltd_dismiss), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!state.isHost) {
                TextButton(onClick = viewModel::resync) {
                    Icon(SimpIcons.Sync, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.ltd_resync), color = LocalContentColor.current)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = viewModel::leave,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(SimpIcons.Logout, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.ltd_leave), color = LocalContentColor.current)
            }
        }
    }
}

@Composable
private fun Participant(
    u: RoomMember,
    isSelf: Boolean,
    canManage: Boolean,
    viewModel: ListenTogetherViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Initial(u.username, highlight = u.isHost, connected = u.isConnected)
        Text(
            u.username + if (isSelf) " " + stringResource(Res.string.ltd_you) else "",
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).alpha(if (u.isConnected) 1f else 0.5f),
        )
        if (u.isHost) Icon(SimpIcons.Star, null, tint = Amber, modifier = Modifier.size(16.dp))
        // Desktop reveals these on hover; a phone has no hover, so they are simply shown.
        if (canManage && !isSelf) {
            IconButton(onClick = { viewModel.transferHost(u.userId) }, modifier = Modifier.size(36.dp)) {
                Icon(SimpIcons.StarBorder, stringResource(Res.string.ltd_make_host), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { viewModel.kick(u.userId) }, modifier = Modifier.size(36.dp)) {
                Icon(SimpIcons.Close, stringResource(Res.string.ltd_remove), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** First letter in a circle: tinted for the host (or a join request), with a connected dot. */
@Composable
private fun Initial(
    name: String,
    highlight: Boolean,
    connected: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .alpha(if (connected) 1f else 0.4f)
                    .clip(CircleShape)
                    .background(if (highlight) primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = typo().bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (highlight) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (connected) Green else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun StatusDot(connection: RoomConnection) {
    val pulse = rememberInfiniteTransition(label = "ltStatus")
    val alpha by pulse.animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "ltStatusAlpha")
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                when (connection) {
                    is RoomConnection.Connected -> Green
                    RoomConnection.Connecting -> Amber.copy(alpha = alpha)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            ),
    )
}

/** Join | Host, a pill with the chosen half lifted onto the page colour. */
@Composable
private fun Segmented(
    left: String,
    right: String,
    rightSelected: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        SegmentedHalf(left, selected = !rightSelected) { onSelect(false) }
        SegmentedHalf(right, selected = rightSelected) { onSelect(true) }
    }
}

@Composable
private fun RowScope.SegmentedHalf(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text,
        style = typo().bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.background else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    )
}

@Composable
private fun Field(
    label: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = typo().bodySmall.copy(fontWeight = FontWeight.Medium))
        content()
        if (hint != null) Text(hint, style = typo().bodySmall)
    }
}

/** The rounded, bordered, faintly filled box desktop uses for the invite and list rows. */
@Composable
private fun Card(
    contentPadding: Modifier,
    small: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(if (small) 12.dp else 16.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .then(contentPadding),
        horizontalAlignment = horizontalAlignment,
    ) { content() }
}

@Composable
private fun Note(
    icon: ImageVector,
    text: String,
) {
    Card(Modifier.padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(text, style = typo().bodySmall)
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = typo().bodySmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

@Composable
private fun FullButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(text, color = LocalContentColor.current)
    }
}

@Composable
private fun SmallButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
    ) { Text(text, style = typo().bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onPrimary) }
}

private const val ROOM_CODE_LENGTH = 8

/** The room code's part of an invite, as the boxes show it. */
private fun codeOf(invite: String) = invite.substringBefore('@').uppercase().filter { it.isLetterOrDigit() }.take(ROOM_CODE_LENGTH)

/**
 * One box per character of the room code. Typing keeps letters and digits; a pasted desktop invite
 * (`CODE@host`) is kept whole so its server still applies, with the boxes showing its code.
 */
@Composable
private fun CodeInput(
    invite: String,
    onInviteChange: (String) -> Unit,
) {
    val code = codeOf(invite)
    val focusRequester = remember { FocusRequester() }
    // TextFieldValue, not String: a plain String re-places the caret at index 0 on every
    // externally-driven recomposition, and backspace at index 0 deletes nothing.
    val fieldValue = TextFieldValue(text = code, selection = TextRange(code.length))

    BasicTextField(
        value = fieldValue,
        onValueChange = { onInviteChange(if ('@' in it.text) it.text.trim() else codeOf(it.text)) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            // Zero-sized but composed: without it nothing holds the cursor and the field takes no input.
            Box(Modifier.size(0.dp)) { innerTextField() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(ROOM_CODE_LENGTH) { index ->
                    // The caret is drawn by hand because the field that owns it is zero-sized.
                    val isCaret = index == code.length
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                .then(
                                    if (isCaret) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier,
                                ).clickable { focusRequester.requestFocus() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = code.getOrNull(index)?.toString().orEmpty(),
                            style = typo().titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
    )
}
