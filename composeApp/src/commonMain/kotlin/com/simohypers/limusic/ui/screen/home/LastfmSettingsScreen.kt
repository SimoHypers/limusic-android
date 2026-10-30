package com.simohypers.limusic.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import com.simohypers.limusic.ui.component.SettingSheet
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.Edit
import com.simohypers.limusic.lastfm.SCROBBLE_PRESETS
import com.simohypers.limusic.lastfm.ScrobblePreset
import com.simohypers.limusic.lastfm.ScrobbleRule
import com.simohypers.limusic.lastfm.patternError
import limusic.composeapp.generated.resources.lastfm_add_rule
import limusic.composeapp.generated.resources.lastfm_add_rule_hint
import limusic.composeapp.generated.resources.lastfm_blank_rule
import limusic.composeapp.generated.resources.lastfm_edit_rule
import limusic.composeapp.generated.resources.lastfm_field_album
import limusic.composeapp.generated.resources.lastfm_field_artist
import limusic.composeapp.generated.resources.lastfm_field_title
import limusic.composeapp.generated.resources.lastfm_find
import limusic.composeapp.generated.resources.lastfm_find_hint
import limusic.composeapp.generated.resources.lastfm_preset_explicit
import limusic.composeapp.generated.resources.lastfm_preset_remastered
import limusic.composeapp.generated.resources.lastfm_preset_topic
import limusic.composeapp.generated.resources.lastfm_preset_vevo
import limusic.composeapp.generated.resources.lastfm_preset_video_tags
import limusic.composeapp.generated.resources.lastfm_remove
import limusic.composeapp.generated.resources.lastfm_replace
import limusic.composeapp.generated.resources.lastfm_replace_nothing
import limusic.composeapp.generated.resources.lastfm_rule_applied
import limusic.composeapp.generated.resources.lastfm_rule_error
import limusic.composeapp.generated.resources.lastfm_rule_no_change
import limusic.composeapp.generated.resources.lastfm_rule_preview
import limusic.composeapp.generated.resources.lastfm_rules
import limusic.composeapp.generated.resources.lastfm_rules_hint
import limusic.composeapp.generated.resources.save
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.simohypers.limusic.domain.utils.MusicVideoType
import com.simohypers.limusic.ui.component.SettingGroup
import com.simohypers.limusic.ui.component.SettingItem
import com.simohypers.limusic.ui.icon.Lastfm
import com.simohypers.limusic.ui.icon.SimpIcons
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.SettingsViewModel
import com.simohypers.limusic.viewModel.SharedViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.simohypers.limusic.lastfm.LastfmProfile
import com.simohypers.limusic.lastfm.Resolved
import com.simohypers.limusic.lastfm.ScrobbleConfig
import com.simohypers.limusic.lastfm.ScrobbleTrack
import com.simohypers.limusic.lastfm.resolve
import com.simohypers.limusic.lastfm.scrobbleAt
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.baseline_music_note_24
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.discord_preview_live
import limusic.composeapp.generated.resources.discord_preview_sample
import limusic.composeapp.generated.resources.lastfm_approve_in_browser
import limusic.composeapp.generated.resources.lastfm_connect
import limusic.composeapp.generated.resources.lastfm_connect_hint
import limusic.composeapp.generated.resources.lastfm_disconnect
import limusic.composeapp.generated.resources.lastfm_from_youtube
import limusic.composeapp.generated.resources.lastfm_gets
import limusic.composeapp.generated.resources.lastfm_minutes
import limusic.composeapp.generated.resources.lastfm_minutes_value
import limusic.composeapp.generated.resources.lastfm_no_limit
import limusic.composeapp.generated.resources.lastfm_now_playing
import limusic.composeapp.generated.resources.lastfm_now_playing_hint
import limusic.composeapp.generated.resources.lastfm_now_scrobbling
import limusic.composeapp.generated.resources.lastfm_percent
import limusic.composeapp.generated.resources.lastfm_primary
import limusic.composeapp.generated.resources.lastfm_primary_hint
import limusic.composeapp.generated.resources.lastfm_scrobble
import limusic.composeapp.generated.resources.lastfm_scrobble_hint
import limusic.composeapp.generated.resources.lastfm_since
import limusic.composeapp.generated.resources.lastfm_split
import limusic.composeapp.generated.resources.lastfm_split_hint
import limusic.composeapp.generated.resources.lastfm_stat_artists
import limusic.composeapp.generated.resources.lastfm_stat_scrobbles
import limusic.composeapp.generated.resources.lastfm_stat_tracks
import limusic.composeapp.generated.resources.lastfm_status_at
import limusic.composeapp.generated.resources.lastfm_status_disconnected
import limusic.composeapp.generated.resources.lastfm_status_done
import limusic.composeapp.generated.resources.lastfm_status_incomplete
import limusic.composeapp.generated.resources.lastfm_status_paused
import limusic.composeapp.generated.resources.lastfm_status_short
import limusic.composeapp.generated.resources.lastfm_status_skip_edit
import limusic.composeapp.generated.resources.lastfm_strict
import limusic.composeapp.generated.resources.lastfm_strict_hint
import limusic.composeapp.generated.resources.lastfm_tag_edit
import limusic.composeapp.generated.resources.lastfm_tag_rule
import limusic.composeapp.generated.resources.lastfm_tag_split
import limusic.composeapp.generated.resources.lastfm_timing_hint
import limusic.composeapp.generated.resources.lastfm_timing_reset
import limusic.composeapp.generated.resources.lastfm_what
import limusic.composeapp.generated.resources.lastfm_when
import limusic.composeapp.generated.resources.log_out_from_lastfm
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Last.fm's red, for its badge and the scrobble mark. */
internal val LastfmRed = Color(0xFFD51007)

/** Shown while nothing plays: desktop's stand-in, a video upload with the artist in its title. */
private val SAMPLE = ScrobbleTrack("", "Nova Sky - Higher Ground (Official Video)", listOf("NovaSkyVEVO"), null, isVideo = true)

/**
 * Desktop's Scrobbling tab on a phone: the account as Last.fm's own profile header shows it, then a
 * live card showing what the playing track is sent as and when it counts, then the settings. The
 * card resolves with the same [resolve] the scrobbler sends from, so it can't disagree with it.
 */
@Composable
fun LastfmSettingsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val user by viewModel.lastfmUser.collectAsStateWithLifecycle()
    val profile by viewModel.lastfmProfile.collectAsStateWithLifecycle()
    val cfg by viewModel.lastfmConfig.collectAsStateWithLifecycle()
    val connecting by sharedViewModel.lastfmConnecting.collectAsStateWithLifecycle()
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val signedIn = user.isNotEmpty()
    fun set(c: ScrobbleConfig) = viewModel.setLastfmConfig(c)

    val song = nowPlaying?.songEntity
    val track =
        song?.let {
            ScrobbleTrack(it.videoId, it.title, it.artistName.orEmpty(), it.albumName, MusicVideoType.isVideoSong(it.videoType))
        } ?: SAMPLE
    val disconnectLabel = stringResource(Res.string.log_out_from_lastfm)
    var presetSheet by remember { mutableStateOf(false) }
    var ruleSheet by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        SettingTopBar(title = "Last.fm", onBack = { navController.navigateUp() }, windowInsets = WindowInsets(0))
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (signedIn) {
                Account(
                    user = user,
                    profile = profile,
                    onOpen = { profile?.url?.let { uriHandler.openUri(it) } },
                    onDisconnect = { viewModel.confirmLogOut(confirmLabel = disconnectLabel) { viewModel.logOutLastfm() } },
                )
            } else {
                Connect(connecting = connecting) {
                    if (connecting) sharedViewModel.cancelLastfmConnect() else sharedViewModel.connectLastfm { uriHandler.openUri(it) }
                }
            }

            NowScrobbling(
                track = track,
                thumbnail = song?.thumbnails,
                live = song != null,
                cfg = cfg,
                signedIn = signedIn,
                positionMs = if (song != null) timeline.current else 0,
                durationMs = if (song != null) timeline.total else 0,
            )

            if (signedIn) {
                SettingGroup(title = null) {
                    SettingItem(
                        title = stringResource(Res.string.lastfm_scrobble),
                        subtitle = stringResource(Res.string.lastfm_scrobble_hint),
                        smallSubtitle = true,
                        switch = cfg.enabled to { on: Boolean -> set(cfg.copy(enabled = on)) },
                    )
                    SettingItem(
                        title = stringResource(Res.string.lastfm_now_playing),
                        subtitle = stringResource(Res.string.lastfm_now_playing_hint),
                        smallSubtitle = true,
                        switch = cfg.nowPlaying to { on: Boolean -> set(cfg.copy(nowPlaying = on)) },
                    )
                }
            }

            val default = ScrobbleConfig()
            SettingGroup(
                title = stringResource(Res.string.lastfm_when),
                footer = {
                    Column {
                        Text(stringResource(Res.string.lastfm_timing_hint), style = typo().bodySmall)
                        if (cfg.percent != default.percent || cfg.minutes != default.minutes) {
                            TextButton(onClick = { set(cfg.copy(percent = default.percent, minutes = default.minutes)) }) {
                                Text(stringResource(Res.string.lastfm_timing_reset))
                            }
                        }
                    }
                },
            ) {
                SliderRow(
                    title = stringResource(Res.string.lastfm_percent),
                    value = cfg.percent,
                    range = 1..100,
                    label = { "$it%" },
                ) { set(cfg.copy(percent = it)) }
                SliderRow(
                    title = stringResource(Res.string.lastfm_minutes),
                    value = cfg.minutes,
                    range = 0..10,
                    label = { if (it == 0) stringResource(Res.string.lastfm_no_limit) else stringResource(Res.string.lastfm_minutes_value, it) },
                ) { set(cfg.copy(minutes = it)) }
            }

            SettingGroup(title = stringResource(Res.string.lastfm_what)) {
                SettingItem(
                    title = stringResource(Res.string.lastfm_split),
                    subtitle = stringResource(Res.string.lastfm_split_hint),
                    smallSubtitle = true,
                    switch = cfg.splitVideoTitles to { on: Boolean -> set(cfg.copy(splitVideoTitles = on)) },
                )
                SettingItem(
                    title = stringResource(Res.string.lastfm_primary),
                    subtitle = stringResource(Res.string.lastfm_primary_hint),
                    smallSubtitle = true,
                    switch = cfg.primaryArtist to { on: Boolean -> set(cfg.copy(primaryArtist = on)) },
                )
                if (cfg.primaryArtist) {
                    SettingItem(
                        title = stringResource(Res.string.lastfm_strict),
                        subtitle = stringResource(Res.string.lastfm_strict_hint),
                        smallSubtitle = true,
                        switch = cfg.primaryStrict to { on: Boolean -> set(cfg.copy(primaryStrict = on)) },
                    )
                }
            }
            Rules(cfg = cfg, track = track, onAdd = { presetSheet = true }, onEdit = { ruleSheet = it }, onChange = ::set)
            Spacer(Modifier.height(innerPadding.calculateBottomPadding() + 24.dp))
        }
    }
    if (presetSheet) {
        PresetSheet(
            cfg = cfg,
            onPick = { rule ->
                presetSheet = false
                set(cfg.copy(rules = cfg.rules + rule))
            },
            onBlank = {
                presetSheet = false
                ruleSheet = NEW_RULE
            },
            onDismiss = { presetSheet = false },
        )
    }
    ruleSheet?.let { i ->
        RuleSheet(
            rule = cfg.rules.getOrNull(i) ?: ScrobbleRule(),
            isNew = i == NEW_RULE,
            track = track,
            onSave = { rule ->
                ruleSheet = null
                set(cfg.copy(rules = if (i == NEW_RULE) cfg.rules + rule else cfg.rules.toMutableList().also { it[i] = rule }))
            },
            onRemove = {
                ruleSheet = null
                set(cfg.copy(rules = cfg.rules.toMutableList().also { it.removeAt(i) }))
            },
            onDismiss = { ruleSheet = null },
        )
    }
    SettingAlertDialog(viewModel)
}

private const val NEW_RULE = -1

private val FIELDS =
    mapOf(
        "title" to Res.string.lastfm_field_title,
        "artist" to Res.string.lastfm_field_artist,
        "album" to Res.string.lastfm_field_album,
    )

private val PRESET_LABELS =
    mapOf(
        "video_tags" to Res.string.lastfm_preset_video_tags,
        "remastered" to Res.string.lastfm_preset_remastered,
        "explicit" to Res.string.lastfm_preset_explicit,
        "topic" to Res.string.lastfm_preset_topic,
        "vevo" to Res.string.lastfm_preset_vevo,
    )

/** The preset a rule still is, if it was added from one and left alone. */
private fun presetOf(rule: ScrobbleRule): ScrobblePreset? = SCROBBLE_PRESETS.firstOrNull { it.rule.copy(enabled = rule.enabled) == rule }

/**
 * The rule list. Each row says what it does (a preset's name, or its pattern), whether it changed the
 * track playing right now, and whether its pattern is broken; the switch turns it off without
 * losing it.
 */
@Composable
private fun Rules(
    cfg: ScrobbleConfig,
    track: ScrobbleTrack,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onChange: (ScrobbleConfig) -> Unit,
) {
    val applied = remember(track, cfg) { resolve(track, cfg).rules }
    SettingGroup(
        title = stringResource(Res.string.lastfm_rules),
        footer = { Text(stringResource(Res.string.lastfm_rules_hint), style = typo().bodySmall) },
    ) {
        cfg.rules.forEachIndexed { i, rule ->
            val preset = presetOf(rule)
            val error = patternError(rule.find)
            SettingItem(
                title = preset?.let { stringResource(PRESET_LABELS.getValue(it.id)) } ?: "${i + 1}. ${stringResource(FIELDS[rule.field] ?: Res.string.lastfm_field_title)}",
                subtitle =
                    if (preset != null) {
                        stringResource(FIELDS[rule.field] ?: Res.string.lastfm_field_title)
                    } else {
                        "${rule.find.ifEmpty { "…" }}  →  ${rule.replace.ifEmpty { stringResource(Res.string.lastfm_replace_nothing) }}"
                    },
                smallSubtitle = true,
                switch = rule.enabled to { on: Boolean -> onChange(cfg.copy(rules = cfg.rules.toMutableList().also { it[i] = rule.copy(enabled = on) })) },
                otherView =
                    when {
                        error != null -> {
                            { Text(stringResource(Res.string.lastfm_rule_error, error), style = typo().bodySmall, color = MaterialTheme.colorScheme.error) }
                        }
                        i in applied -> {
                            { Pill(stringResource(Res.string.lastfm_rule_applied)) }
                        }
                        else -> null
                    },
                onClick = { onEdit(i) },
            )
        }
        SettingItem(
            title = stringResource(Res.string.lastfm_add_rule),
            subtitle = stringResource(Res.string.lastfm_add_rule_hint),
            smallSubtitle = true,
            leading = { Icon(SimpIcons.Add, contentDescription = null) },
            onClick = onAdd,
        )
    }
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        style = typo().labelSmall,
        color = LastfmRed,
        modifier = Modifier.clip(CircleShape).background(LastfmRed.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** "Add rule": desktop's presets, one tap each, or a blank rule to write. */
@Composable
private fun PresetSheet(
    cfg: ScrobbleConfig,
    onPick: (ScrobbleRule) -> Unit,
    onBlank: () -> Unit,
    onDismiss: () -> Unit,
) {
    SettingSheet(title = stringResource(Res.string.lastfm_add_rule), onDismiss = onDismiss) { colors ->
        Column {
            SCROBBLE_PRESETS.forEach { preset ->
                val added = cfg.rules.any { presetOf(it)?.id == preset.id }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !added) { onPick(preset.rule) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(PRESET_LABELS.getValue(preset.id)),
                        style = typo().bodyMedium,
                        color = if (added) colors.disabled else colors.content,
                        modifier = Modifier.weight(1f),
                    )
                    if (added) Icon(SimpIcons.Check, contentDescription = null, tint = colors.disabled, modifier = Modifier.size(18.dp))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBlank)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(SimpIcons.Edit, contentDescription = null, tint = colors.content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.lastfm_blank_rule), style = typo().bodyMedium, color = colors.content)
            }
        }
    }
}

/**
 * One rule, edited as a draft: nothing saves until Save, and the bottom of the sheet shows what this
 * rule alone makes of the playing track, so a pattern can be tried before it goes live.
 */
@Composable
private fun RuleSheet(
    rule: ScrobbleRule,
    isNew: Boolean,
    track: ScrobbleTrack,
    onSave: (ScrobbleRule) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var field by remember { mutableStateOf(rule.field) }
    var find by remember { mutableStateOf(rule.find) }
    var replace by remember { mutableStateOf(rule.replace) }
    val draft = rule.copy(field = field, find = find, replace = replace, enabled = true)
    val error = patternError(find)
    SettingSheet(title = stringResource(if (isNew) Res.string.lastfm_add_rule else Res.string.lastfm_edit_rule), onDismiss = onDismiss) { colors ->
        Choices(selected = field, options = FIELDS.map { (id, res) -> id to stringResource(res) }) { field = it }
        OutlinedTextField(
            value = find,
            onValueChange = { find = it },
            label = { Text(stringResource(Res.string.lastfm_find)) },
            textStyle = typo().bodyMedium.copy(fontFamily = FontFamily.Monospace, color = colors.content),
            isError = error != null,
            supportingText = { Text(error?.let { stringResource(Res.string.lastfm_rule_error, it) } ?: stringResource(Res.string.lastfm_find_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = replace,
            onValueChange = { replace = it },
            label = { Text(stringResource(Res.string.lastfm_replace)) },
            textStyle = typo().bodyMedium.copy(fontFamily = FontFamily.Monospace, color = colors.content),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (find.isNotEmpty() && error == null) {
            val before = mapOf("title" to track.title, "artist" to track.byline, "album" to track.album.orEmpty())
            val r = remember(draft, track) { resolve(track, ScrobbleConfig(rules = listOf(draft))) }
            val after = mapOf("title" to r.title, "artist" to r.artist, "album" to r.album)
            Column {
                Text(stringResource(Res.string.lastfm_rule_preview).uppercase(), style = typo().labelSmall, color = colors.subtitle)
                val changed = FIELDS.keys.filter { before[it] != after[it] }
                if (changed.isEmpty()) {
                    Text(stringResource(Res.string.lastfm_rule_no_change), style = typo().bodySmall, color = colors.subtitle)
                }
                changed.forEach { f ->
                    Text(before.getValue(f), style = typo().bodySmall, color = colors.subtitle, textDecoration = TextDecoration.LineThrough)
                    Text(after.getValue(f).ifEmpty { "…" }, style = typo().bodyMedium, color = LastfmRed)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!isNew) {
                TextButton(onClick = onRemove) { Text(stringResource(Res.string.lastfm_remove), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(draft.copy(enabled = rule.enabled || isNew)) }, enabled = find.isNotEmpty() && error == null) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}

@Composable
private fun LastfmBadge(size: Int = 40) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(LastfmRed), contentAlignment = Alignment.Center) {
        Icon(SimpIcons.Lastfm, contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
private fun Connect(
    connecting: Boolean,
    onClick: () -> Unit,
) {
    SettingGroup(title = null) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LastfmBadge(56)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(if (connecting) Res.string.lastfm_approve_in_browser else Res.string.lastfm_connect_hint),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (connecting) Res.string.cancel else Res.string.lastfm_connect))
            }
        }
    }
}

/** The account the way Last.fm's own profile header shows it: avatar, name, since when, three counts. */
@OptIn(ExperimentalTime::class)
@Composable
private fun Account(
    user: String,
    profile: LastfmProfile?,
    onOpen: () -> Unit,
    onDisconnect: () -> Unit,
) {
    SettingGroup(title = null) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile?.image != null) {
                    AsyncImage(
                        model = profile.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape).clickable(onClick = onOpen),
                    )
                } else {
                    LastfmBadge(56)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                    Text(user, style = typo().titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    profile?.since?.takeIf { it > 0 }?.let { since ->
                        val date = Instant.fromEpochSeconds(since).toLocalDateTime(TimeZone.currentSystemDefault())
                        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                        Text(stringResource(Res.string.lastfm_since, "$month ${date.year}"), style = typo().bodySmall)
                    }
                }
                TextButton(onClick = onDisconnect) { Text(stringResource(Res.string.lastfm_disconnect)) }
            }
            if (profile != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(profile.scrobbles, stringResource(Res.string.lastfm_stat_scrobbles), Modifier.weight(1f))
                    Stat(profile.artists, stringResource(Res.string.lastfm_stat_artists), Modifier.weight(1f))
                    Stat(profile.tracks, stringResource(Res.string.lastfm_stat_tracks), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Stat(
    value: Long,
    label: String,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(grouped(value), style = typo().titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(label, style = typo().bodySmall, maxLines = 1)
    }
}

/** 48213 as "48,213". */
private fun grouped(n: Long): String =
    n
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

/**
 * The playing track as YouTube has it, what Last.fm gets, why (tags), and when it counts: a bar with
 * a mark at the scrobble point that turns Last.fm red once the play has counted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NowScrobbling(
    track: ScrobbleTrack,
    thumbnail: String?,
    live: Boolean,
    cfg: ScrobbleConfig,
    signedIn: Boolean,
    positionMs: Long,
    durationMs: Long,
) {
    val r = remember(track, cfg) { resolve(track, cfg) }
    val at = if (live) scrobbleAt(durationMs / 1000.0, cfg) else null
    val done = at != null && positionMs / 1000.0 >= at
    SettingGroup(title = stringResource(Res.string.lastfm_now_scrobbling)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (thumbnail != null) {
                        AsyncImage(model = thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
                    } else {
                        Icon(painterResource(Res.drawable.baseline_music_note_24), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Caption(stringResource(Res.string.lastfm_from_youtube), Modifier.weight(1f))
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
                    }
                    Text(track.title, style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.byline, style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(46.dp).clip(CircleShape).background(if (r.skip == null) LastfmRed else MaterialTheme.colorScheme.outlineVariant))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Caption(stringResource(Res.string.lastfm_gets))
                    Text(r.title, style = typo().titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(r.artist, r.album).filter { it.isNotBlank() }.joinToString(" · "),
                        style = typo().bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Tags(r)
            if (live && at != null) {
                Spacer(Modifier.height(12.dp))
                ScrobbleBar(positionMs, durationMs, at, done)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = status(r, cfg, signedIn, live, at, done, durationMs),
                style = typo().bodySmall,
                color = if (done && signedIn && cfg.enabled) LastfmRed else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Caption(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text.uppercase(), style = typo().labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** Why the track scrobbles the way it does: the fix, the split, each rule that changed something. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(r: Resolved) {
    val tags =
        buildList {
            if (r.edit != null) add(stringResource(Res.string.lastfm_tag_edit))
            if (r.split) add(stringResource(Res.string.lastfm_tag_split))
            r.rules.forEach { add(stringResource(Res.string.lastfm_tag_rule, it + 1)) }
        }
    if (tags.isEmpty()) return
    FlowRow(Modifier.padding(start = 13.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.forEach {
            Text(
                it,
                style = typo().labelSmall,
                color = LastfmRed,
                modifier = Modifier.clip(CircleShape).background(LastfmRed.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ScrobbleBar(
    positionMs: Long,
    durationMs: Long,
    atSec: Double,
    done: Boolean,
) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val mark = if (durationMs > 0) (atSec * 1000 / durationMs).toFloat().coerceIn(0f, 1f) else 1f
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = if (done) LastfmRed else MaterialTheme.colorScheme.onSurfaceVariant
    BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp)) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp).clip(CircleShape).background(track))
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth(fraction).height(4.dp).clip(CircleShape).background(fill))
        // The scrobble point: where this play starts to count.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = maxWidth * mark - 1.dp)
                .width(2.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(LastfmRed),
        )
    }
}

/** One line saying whether this play counts and when. The first reason that applies wins. */
@Composable
private fun status(
    r: Resolved,
    cfg: ScrobbleConfig,
    signedIn: Boolean,
    live: Boolean,
    at: Double?,
    done: Boolean,
    durationMs: Long,
): String =
    when {
        !signedIn -> stringResource(Res.string.lastfm_status_disconnected)
        !cfg.enabled -> stringResource(Res.string.lastfm_status_paused)
        r.skip == "edit" -> stringResource(Res.string.lastfm_status_skip_edit)
        r.skip == "incomplete" -> stringResource(Res.string.lastfm_status_incomplete)
        !live -> ""
        at == null -> if (durationMs > 0) stringResource(Res.string.lastfm_status_short) else ""
        done -> stringResource(Res.string.lastfm_status_done, clockOf(at))
        else -> stringResource(Res.string.lastfm_status_at, clockOf(at))
    }

private fun clockOf(sec: Double): String {
    val s = sec.toLong()
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** A setting slider that saves when the finger lifts, not on every step of the drag. */
@Composable
private fun SliderRow(
    title: String,
    value: Int,
    range: IntRange,
    label: @Composable (Int) -> String,
    onSet: (Int) -> Unit,
) {
    var dragging by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(label(dragging.roundToInt()), style = typo().labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = dragging,
            onValueChange = { dragging = it },
            onValueChangeFinished = { onSet(dragging.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            // Ticks only where there are few enough to read; 1 to 100 would be a dotted line.
            steps = if (range.last - range.first <= 20) range.last - range.first - 1 else 0,
        )
    }
}
