package com.simohypers.limusic.ui.screen.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.discord.DiscordConfig
import com.simohypers.limusic.discord.DiscordOAuth
import com.simohypers.limusic.discord.DiscordTrack
import com.simohypers.limusic.discord.DiscordUser
import com.simohypers.limusic.discord.button
import com.simohypers.limusic.discord.line1Slot
import com.simohypers.limusic.discord.link
import com.simohypers.limusic.discord.text
import com.simohypers.limusic.ui.component.SettingSheet
import com.simohypers.limusic.ui.component.SettingGroup
import com.simohypers.limusic.ui.component.SettingItem
import com.simohypers.limusic.ui.icon.Discord
import com.simohypers.limusic.ui.icon.SimpIcons
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SettingsViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.baseline_music_note_24
import limusic.composeapp.generated.resources.circle_app_icon
import limusic.composeapp.generated.resources.discord_app_name
import limusic.composeapp.generated.resources.discord_app_name_hint
import limusic.composeapp.generated.resources.discord_artwork
import limusic.composeapp.generated.resources.discord_badge
import limusic.composeapp.generated.resources.discord_badge_hint
import limusic.composeapp.generated.resources.discord_button1
import limusic.composeapp.generated.resources.discord_button2
import limusic.composeapp.generated.resources.discord_button_album
import limusic.composeapp.generated.resources.discord_button_app
import limusic.composeapp.generated.resources.discord_button_artist
import limusic.composeapp.generated.resources.discord_button_listen
import limusic.composeapp.generated.resources.discord_button_off
import limusic.composeapp.generated.resources.discord_buttons
import limusic.composeapp.generated.resources.discord_card
import limusic.composeapp.generated.resources.discord_content_album
import limusic.composeapp.generated.resources.discord_content_artist
import limusic.composeapp.generated.resources.discord_content_artist_album
import limusic.composeapp.generated.resources.discord_content_off
import limusic.composeapp.generated.resources.discord_content_title
import limusic.composeapp.generated.resources.discord_done
import limusic.composeapp.generated.resources.discord_enable
import limusic.composeapp.generated.resources.discord_enable_hint
import limusic.composeapp.generated.resources.discord_hide_details
import limusic.composeapp.generated.resources.discord_hide_details_hint
import limusic.composeapp.generated.resources.discord_line1
import limusic.composeapp.generated.resources.discord_line2
import limusic.composeapp.generated.resources.discord_line3
import limusic.composeapp.generated.resources.discord_line3_hint
import limusic.composeapp.generated.resources.discord_link
import limusic.composeapp.generated.resources.discord_link_cover
import limusic.composeapp.generated.resources.discord_link_hint
import limusic.composeapp.generated.resources.discord_off
import limusic.composeapp.generated.resources.discord_on
import limusic.composeapp.generated.resources.discord_off_notice
import limusic.composeapp.generated.resources.discord_paused_notice
import limusic.composeapp.generated.resources.discord_preview_live
import limusic.composeapp.generated.resources.discord_preview_sample
import limusic.composeapp.generated.resources.discord_reset
import limusic.composeapp.generated.resources.discord_show_paused
import limusic.composeapp.generated.resources.discord_show_paused_hint
import limusic.composeapp.generated.resources.discord_sign_in
import limusic.composeapp.generated.resources.discord_sign_in_hint
import limusic.composeapp.generated.resources.discord_sign_out
import limusic.composeapp.generated.resources.discord_signed_in
import limusic.composeapp.generated.resources.discord_signed_out_notice
import limusic.composeapp.generated.resources.discord_status_app
import limusic.composeapp.generated.resources.discord_status_line
import limusic.composeapp.generated.resources.discord_status_line_hint
import limusic.composeapp.generated.resources.discord_tap_to_edit
import limusic.composeapp.generated.resources.discord_timeline
import limusic.composeapp.generated.resources.discord_timeline_hint
import limusic.composeapp.generated.resources.discord_you
import limusic.composeapp.generated.resources.log_out_from_discord

// Discord's own colours, not the theme's: the preview is a picture of another app, and following
// the Limusic theme would make it less true to what friends see. Desktop's DiscordCard does the same.
private val DcCard = Color(0xFF232428)
private val DcSunken = Color(0xFF1E1F22)
private val DcMuted = Color(0xFFB5BAC1)
private val DcTitle = Color(0xFFF2F3F5)
private val DcText = Color(0xFFDBDEE1)
private val DcButton = Color(0xFF4E5058)
private val DcMemberRow = Color(0xFF2B2D31)
private val Blurple = Color(0xFF5865F2)
private val DcPink = Color(0xFFEB459E)

/** The parts of the card a tap can open. */
private enum class CardPart { HEADER, STATUS, LINE1, LINE2, LINE3, ARTWORK, BAR, BUTTONS }

/** Shown while nothing is playing: a video upload's worth of slots, so every option has something to change. */
private val SAMPLE =
    DiscordTrack(
        videoId = "dQw4w9WgXcQ",
        title = "Higher Ground",
        artists = "Nova Sky",
        artistId = "UCsample",
        album = "Night Signals",
        albumId = "MPREsample",
    )

private val CONTENT =
    mapOf(
        "title" to Res.string.discord_content_title,
        "artist" to Res.string.discord_content_artist,
        "album" to Res.string.discord_content_album,
        "artist_album" to Res.string.discord_content_artist_album,
        "off" to Res.string.discord_content_off,
    )
private val BUTTONS =
    mapOf(
        "listen" to Res.string.discord_button_listen,
        "album" to Res.string.discord_button_album,
        "artist" to Res.string.discord_button_artist,
        "app" to Res.string.discord_button_app,
        "off" to Res.string.discord_button_off,
    )

/**
 * Desktop's Discord settings tab on a phone: the card pinned at the top, live, and every part of it
 * a button that opens its own choices. The same options sit in the list below for anyone who
 * doesn't think to tap the picture.
 */
@Composable
fun DiscordSettingsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val page by viewModel.discordPage.collectAsStateWithLifecycle()
    val user by viewModel.discordUser.collectAsStateWithLifecycle()
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val control by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var editing by rememberSaveable { mutableStateOf<CardPart?>(null) }

    val cfg = page.config
    fun set(c: DiscordConfig) = viewModel.setDiscordConfig(c)

    val song = nowPlaying?.songEntity
    val live = song != null
    val track =
        song?.let {
            DiscordTrack(
                videoId = it.videoId,
                title = it.title,
                artists = it.artistName?.joinToString(", ").orEmpty(),
                artistId = it.artistId?.firstOrNull(),
                album = it.albumName,
                albumId = it.albumId,
                thumbnail = it.thumbnails,
            )
        } ?: SAMPLE
    val paused = live && !control.isPlaying
    val offAir = !page.signedIn || !page.enabled || (paused && !cfg.showPaused)

    Column(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
        SettingTopBar(title = "Discord", onBack = { navController.navigateUp() }, windowInsets = WindowInsets(0))

        // Pinned: the options scroll under it, so a change shows up while the finger is still there.
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (live) Res.string.discord_preview_live else Res.string.discord_preview_sample).uppercase(),
                    style = typo().labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.discord_tap_to_edit),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.alpha(if (offAir) 0.5f else 1f).animateContentSize()) {
                DiscordCardPreview(
                    cfg = cfg,
                    track = track,
                    paused = paused,
                    positionMs = if (live) timeline.current else 66_000,
                    durationMs = if (live) timeline.total else 204_000,
                    onEdit = { editing = it },
                )
                Spacer(Modifier.height(6.dp))
                MemberRow(cfg = cfg, track = track, user = user, onClick = { editing = CardPart.STATUS })
            }
            if (offAir) {
                Text(
                    text =
                        stringResource(
                            when {
                                !page.signedIn -> Res.string.discord_signed_out_notice
                                !page.enabled -> Res.string.discord_off_notice
                                else -> Res.string.discord_paused_notice
                            },
                        ),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            val signOutLabel = stringResource(Res.string.log_out_from_discord)
            SettingGroup(title = null) {
                if (page.signedIn) {
                    AccountRow(user = user, onSignOut = {
                        viewModel.confirmLogOut(confirmLabel = signOutLabel) { viewModel.logOutDiscord() }
                    })
                } else {
                    SettingItem(
                        title = stringResource(Res.string.discord_sign_in),
                        subtitle = stringResource(Res.string.discord_sign_in_hint),
                        smallSubtitle = true,
                        leading = { DiscordBadge() },
                        navigates = true,
                        onClick = { uriHandler.openUri(DiscordOAuth.authorizeUrl()) },
                    )
                }
            }
            SettingGroup(title = null) {
                SettingItem(
                    title = stringResource(Res.string.discord_enable),
                    subtitle = stringResource(Res.string.discord_enable_hint),
                    smallSubtitle = true,
                    switch = page.enabled to { on: Boolean -> viewModel.setDiscordRichPresenceEnabled(on) },
                    isEnable = page.signedIn,
                )
                SettingItem(
                    title = stringResource(Res.string.discord_show_paused),
                    subtitle = stringResource(Res.string.discord_show_paused_hint),
                    smallSubtitle = true,
                    switch = cfg.showPaused to { on: Boolean -> set(cfg.copy(showPaused = on)) },
                )
                SettingItem(
                    title = stringResource(Res.string.discord_hide_details),
                    subtitle = stringResource(Res.string.discord_hide_details_hint),
                    smallSubtitle = true,
                    switch = cfg.hideDetails to { on: Boolean -> set(cfg.copy(hideDetails = on)) },
                )
            }
            // Under "hide details" none of this reaches Discord, so none of it is offered.
            if (!cfg.hideDetails) {
                SettingGroup(title = stringResource(Res.string.discord_card)) {
                    PartRow(Res.string.discord_status_line, statusLabel(cfg)) { editing = CardPart.STATUS }
                    PartRow(Res.string.discord_app_name, cfg.name) { editing = CardPart.HEADER }
                    PartRow(Res.string.discord_line1, stringResource(CONTENT.getValue(cfg.line1))) { editing = CardPart.LINE1 }
                    PartRow(Res.string.discord_line2, stringResource(CONTENT[cfg.line2] ?: Res.string.discord_content_off)) {
                        editing = CardPart.LINE2
                    }
                    PartRow(Res.string.discord_artwork, onOff(cfg.cover)) { editing = CardPart.ARTWORK }
                    if (cfg.cover) {
                        PartRow(Res.string.discord_line3, stringResource(CONTENT[cfg.line3] ?: Res.string.discord_content_off)) {
                            editing = CardPart.LINE3
                        }
                    }
                    PartRow(Res.string.discord_timeline, onOff(cfg.timestamps)) { editing = CardPart.BAR }
                    PartRow(
                        Res.string.discord_buttons,
                        listOf(cfg.button1, cfg.button2)
                            .filter { it != "off" }
                            .map { stringResource(BUTTONS.getValue(it)) }
                            .ifEmpty { listOf(stringResource(Res.string.discord_button_off)) }
                            .joinToString(", "),
                    ) { editing = CardPart.BUTTONS }
                }
            }
            if (cfg != DiscordConfig()) {
                TextButton(onClick = { set(DiscordConfig()) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(Res.string.discord_reset))
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }

    editing?.let { part ->
        PartSheet(part = part, cfg = cfg, onChange = ::set, onDismiss = { editing = null })
    }
    SettingAlertDialog(viewModel)
}

@Composable
private fun onOff(on: Boolean): String = stringResource(if (on) Res.string.discord_on else Res.string.discord_off)

@Composable
private fun statusLabel(cfg: DiscordConfig): String =
    when (cfg.statusLine) {
        "app" -> stringResource(Res.string.discord_status_app)
        "line1" -> "${stringResource(Res.string.discord_line1)} · ${stringResource(CONTENT.getValue(cfg.line1))}"
        else -> "${stringResource(Res.string.discord_line2)} · ${stringResource(CONTENT[cfg.line2] ?: Res.string.discord_content_off)}"
    }

@Composable
private fun PartRow(
    title: StringResource,
    value: String,
    onClick: () -> Unit,
) {
    SettingItem(title = stringResource(title), subtitle = value, smallSubtitle = true, navigates = true, onClick = onClick)
}

@Composable
private fun DiscordBadge() {
    Box(Modifier.size(40.dp).clip(CircleShape).background(Blurple), contentAlignment = Alignment.Center) {
        Icon(SimpIcons.Discord, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AccountRow(
    user: DiscordUser?,
    onSignOut: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp)) {
            if (user?.avatar != null) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
            } else {
                DiscordBadge()
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = user?.name ?: stringResource(Res.string.discord_signed_in),
                style = typo().labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = user?.let { "@${it.username}" } ?: "Discord",
                style = typo().bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onSignOut) { Text(stringResource(Res.string.discord_sign_out)) }
    }
}

/**
 * A faithful mock of Discord's "Listening to" card, drawn from the same config and the same slot
 * rules ([text], [link], [button]) the presence loop sends from, so the two can't disagree.
 * Slots that are off show as a faint dashed outline, so there is something to tap to turn them on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscordCardPreview(
    cfg: DiscordConfig,
    track: DiscordTrack,
    paused: Boolean,
    positionMs: Long,
    durationMs: Long,
    onEdit: (CardPart) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DcCard)
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { onEdit(CardPart.HEADER) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Discord's own wording, untranslated: it renders this in the viewer's language anyway.
            Text(
                text = "Listening to ${cfg.name}",
                color = DcMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = "···", color = DcMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (cfg.hideDetails) return@Column
        Spacer(Modifier.height(8.dp))
        Row {
            Artwork(cfg = cfg, track = track, onClick = { onEdit(CardPart.ARTWORK) })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                val line1Slot = track.line1Slot(cfg)
                CardLine(
                    text = track.text(line1Slot) ?: track.title,
                    color = DcTitle,
                    bold = true,
                    linked = cfg.linkLine1 && track.link(line1Slot) != null,
                    onClick = { onEdit(CardPart.LINE1) },
                )
                val line2 = track.text(cfg.line2)
                if (line2 != null) {
                    CardLine(line2, DcText, linked = cfg.linkLine2 && track.link(cfg.line2) != null) { onEdit(CardPart.LINE2) }
                } else {
                    Ghost(stringResource(Res.string.discord_line2)) { onEdit(CardPart.LINE2) }
                }
                if (cfg.cover) {
                    val line3 = track.text(cfg.line3)
                    if (line3 != null) {
                        CardLine(line3, DcMuted) { onEdit(CardPart.LINE3) }
                    } else {
                        Ghost(stringResource(Res.string.discord_line3)) { onEdit(CardPart.LINE3) }
                    }
                }
                // No bar on a paused card: Discord has no paused state, so the real one isn't sent either.
                if (cfg.timestamps && !(paused && cfg.showPaused)) {
                    Bar(positionMs, durationMs) { onEdit(CardPart.BAR) }
                } else if (!cfg.timestamps) {
                    Ghost(stringResource(Res.string.discord_timeline)) { onEdit(CardPart.BAR) }
                }
            }
        }
        val buttons = listOf(cfg.button1, cfg.button2).mapNotNull { track.button(it)?.first }
        Spacer(Modifier.height(10.dp))
        if (buttons.isEmpty()) {
            Ghost(stringResource(Res.string.discord_buttons), Modifier.fillMaxWidth()) { onEdit(CardPart.BUTTONS) }
        } else {
            Column(
                Modifier.clip(RoundedCornerShape(4.dp)).clickable { onEdit(CardPart.BUTTONS) },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                buttons.forEach { label ->
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(DcButton)
                                .padding(vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Artwork(
    cfg: DiscordConfig,
    track: DiscordTrack,
    onClick: () -> Unit,
) {
    if (!cfg.cover) {
        Box(
            Modifier.size(60.dp).dashed().clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = DcMuted.copy(alpha = 0.7f), fontSize = 22.sp)
        }
        return
    }
    Box(Modifier.size(60.dp).clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(listOf(Blurple.copy(alpha = 0.5f), DcPink.copy(alpha = 0.4f)))),
            contentAlignment = Alignment.Center,
        ) {
            if (track.thumbnail != null) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(60.dp),
                )
            } else {
                Icon(painterResource(Res.drawable.baseline_music_note_24), null, tint = Color.White.copy(alpha = 0.7f))
            }
        }
        // Discord draws small_image as a circle clipped over the artwork's bottom-right corner.
        if (cfg.badge) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = 5.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(DcCard)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(DcSunken),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painterResource(Res.drawable.circle_app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CardLine(
    text: String,
    color: Color,
    bold: Boolean = false,
    linked: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = color,
        fontSize = if (bold) 14.sp else 13.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        textDecoration = if (linked) TextDecoration.Underline else null,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).clickable(onClick = onClick),
    )
}

/** An off slot: a faint dashed outline naming it, so it can be tapped back on. */
@Composable
private fun Ghost(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = "+ $label",
        color = DcMuted.copy(alpha = 0.6f),
        fontSize = 12.sp,
        maxLines = 1,
        modifier =
            modifier
                .padding(vertical = 2.dp)
                .dashed()
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun Modifier.dashed(): Modifier =
    drawBehind {
        drawRoundRect(
            color = DcMuted.copy(alpha = 0.35f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
        )
    }

@Composable
private fun Bar(
    positionMs: Long,
    durationMs: Long,
    onClick: () -> Unit,
) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(3.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(clock(positionMs), color = DcMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
        ) {
            Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(CircleShape).background(DcText))
        }
        Text(clock(durationMs), color = DcMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** The member-list row: the only place the Status setting shows, which is why it gets its own mock. */
@Composable
private fun MemberRow(
    cfg: DiscordConfig,
    track: DiscordTrack,
    user: DiscordUser?,
    onClick: () -> Unit,
) {
    val line1 = track.text(track.line1Slot(cfg)) ?: track.title
    val status =
        when {
            cfg.hideDetails || cfg.statusLine == "app" -> cfg.name
            cfg.statusLine == "line1" -> line1
            else -> track.text(cfg.line2) ?: cfg.name
        }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DcMemberRow)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (user?.avatar != null) {
            AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.size(30.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blurple, DcPink))))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = user?.name ?: stringResource(Res.string.discord_you),
                color = DcText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.baseline_music_note_24), null, tint = DcMuted, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(status, color = DcMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** One part's choices, as a sheet. Every change saves at once and the pinned card above follows. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PartSheet(
    part: CardPart,
    cfg: DiscordConfig,
    onChange: (DiscordConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    // The app name saves when the sheet closes, not per keystroke: each save re-pushes the card.
    var appName by remember { mutableStateOf(cfg.appName) }
    val close = {
        if (part == CardPart.HEADER && appName.trim() != cfg.appName) onChange(cfg.copy(appName = appName.trim()))
        onDismiss()
    }
    val title =
        when (part) {
            CardPart.HEADER -> Res.string.discord_app_name
            CardPart.STATUS -> Res.string.discord_status_line
            CardPart.LINE1 -> Res.string.discord_line1
            CardPart.LINE2 -> Res.string.discord_line2
            CardPart.LINE3 -> Res.string.discord_line3
            CardPart.ARTWORK -> Res.string.discord_artwork
            CardPart.BAR -> Res.string.discord_timeline
            CardPart.BUTTONS -> Res.string.discord_buttons
        }
    SettingSheet(title = stringResource(title), onDismiss = close) { colors ->
        when (part) {
            CardPart.HEADER -> {
                Hint(stringResource(Res.string.discord_app_name_hint), colors.subtitle)
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it.take(64) },
                    placeholder = { Text("Limusic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = close, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(Res.string.discord_done))
                }
            }
            CardPart.STATUS -> {
                Hint(stringResource(Res.string.discord_status_line_hint), colors.subtitle)
                Choices(
                    selected = cfg.statusLine,
                    options =
                        listOf(
                            "app" to stringResource(Res.string.discord_status_app),
                            "line1" to "${stringResource(Res.string.discord_line1)} · ${stringResource(CONTENT.getValue(cfg.line1))}",
                            "line2" to
                                "${stringResource(Res.string.discord_line2)} · ${stringResource(CONTENT[cfg.line2] ?: Res.string.discord_content_off)}",
                        ),
                ) { onChange(cfg.copy(statusLine = it)) }
            }
            CardPart.LINE1 -> {
                Choices(cfg.line1, contentOptions("title", "artist", "album")) { onChange(cfg.copy(line1 = it)) }
                SwitchRow(stringResource(Res.string.discord_link), stringResource(Res.string.discord_link_hint), cfg.linkLine1, colors) {
                    onChange(cfg.copy(linkLine1 = it))
                }
            }
            CardPart.LINE2 -> {
                Choices(cfg.line2, contentOptions("artist", "artist_album", "album", "title", "off")) { onChange(cfg.copy(line2 = it)) }
                if (cfg.line2 != "off") {
                    SwitchRow(stringResource(Res.string.discord_link), stringResource(Res.string.discord_link_hint), cfg.linkLine2, colors) {
                        onChange(cfg.copy(linkLine2 = it))
                    }
                }
            }
            CardPart.LINE3 -> {
                Hint(stringResource(Res.string.discord_line3_hint), colors.subtitle)
                Choices(cfg.line3, contentOptions("album", "artist", "title", "off")) { onChange(cfg.copy(line3 = it)) }
            }
            CardPart.ARTWORK -> {
                SwitchRow(stringResource(Res.string.discord_artwork), null, cfg.cover, colors) { onChange(cfg.copy(cover = it)) }
                if (cfg.cover) {
                    SwitchRow(stringResource(Res.string.discord_badge), stringResource(Res.string.discord_badge_hint), cfg.badge, colors) {
                        onChange(cfg.copy(badge = it))
                    }
                    SwitchRow(stringResource(Res.string.discord_link_cover), null, cfg.linkCover, colors) {
                        onChange(cfg.copy(linkCover = it))
                    }
                }
            }
            CardPart.BAR ->
                SwitchRow(stringResource(Res.string.discord_timeline), stringResource(Res.string.discord_timeline_hint), cfg.timestamps, colors) {
                    onChange(cfg.copy(timestamps = it))
                }
            CardPart.BUTTONS -> {
                val options = listOf("listen", "album", "artist", "app", "off").map { it to stringResource(BUTTONS.getValue(it)) }
                Text(stringResource(Res.string.discord_button1), style = typo().labelMedium, color = colors.content)
                Choices(cfg.button1, options) { onChange(cfg.copy(button1 = it)) }
                Text(stringResource(Res.string.discord_button2), style = typo().labelMedium, color = colors.content)
                Choices(cfg.button2, options) { onChange(cfg.copy(button2 = it)) }
            }
        }
    }
}

@Composable
private fun contentOptions(vararg slots: String): List<Pair<String, String>> = slots.map { it to stringResource(CONTENT.getValue(it)) }

@Composable
private fun Hint(
    text: String,
    color: Color,
) {
    Text(text, style = typo().bodySmall, color = color)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Choices(
    selected: String,
    options: List<Pair<String, String>>,
    onPick: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            FilterChip(selected = id == selected, onClick = { onPick(id) }, label = { Text(label) })
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String?,
    checked: Boolean,
    colors: com.simohypers.limusic.ui.component.SurfaceDarkColors,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().bodyMedium, color = colors.content)
            if (hint != null) Text(hint, style = typo().bodySmall, color = colors.subtitle)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
