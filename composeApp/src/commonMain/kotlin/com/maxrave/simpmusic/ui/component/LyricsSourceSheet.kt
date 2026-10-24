package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.entities.LyricsEntity
import com.maxrave.domain.data.model.lyrics.LyricsSource
import com.maxrave.simpmusic.ui.icon.Add
import com.maxrave.simpmusic.ui.icon.ArrowForwardIos
import com.maxrave.simpmusic.ui.icon.AvTimer
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Settings
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.TipsAndUpdates
import com.maxrave.simpmusic.ui.icon.Undo
import com.maxrave.simpmusic.ui.screen.home.LyricsKind
import com.maxrave.simpmusic.ui.screen.home.LyricsKindBadge
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.lyrics_automatic
import simpmusic.composeapp.generated.resources.lyrics_automatic_hint
import simpmusic.composeapp.generated.resources.lyrics_automatic_showing
import simpmusic.composeapp.generated.resources.lyrics_check
import simpmusic.composeapp.generated.resources.lyrics_not_found
import simpmusic.composeapp.generated.resources.lyrics_off
import simpmusic.composeapp.generated.resources.lyrics_retry
import simpmusic.composeapp.generated.resources.lyrics_search_artist
import simpmusic.composeapp.generated.resources.lyrics_search_go
import simpmusic.composeapp.generated.resources.lyrics_search_open
import simpmusic.composeapp.generated.resources.lyrics_search_reset
import simpmusic.composeapp.generated.resources.lyrics_search_results
import simpmusic.composeapp.generated.resources.lyrics_search_title
import simpmusic.composeapp.generated.resources.lyrics_source_hint
import simpmusic.composeapp.generated.resources.lyrics_source_title
import simpmusic.composeapp.generated.resources.lyrics_timing
import simpmusic.composeapp.generated.resources.lyrics_timing_hint
import simpmusic.composeapp.generated.resources.lyrics_timing_reset
import simpmusic.composeapp.generated.resources.lyrics_timing_value
import simpmusic.composeapp.generated.resources.lyrics_unreachable
import kotlin.math.abs

/** One step of the timing nudge. A quarter second is under what anyone hears as early. */
private const val STEP_MS = 250
private const val MAX_OFFSET_MS = 15_000

private sealed interface Answer {
    data object Idle : Answer

    data object Loading : Answer

    data object Failed : Answer

    data class Done(
        val lyrics: LyricsEntity?,
    ) : Answer
}

/**
 * Desktop's lyrics source switch (Limusic #23) as a sheet: every provider's answer for this song
 * side by side, one tap to keep one, a timing nudge, and a search for when YouTube's title is what
 * went wrong. A pick lands on the lyrics page behind the sheet straight away.
 */
@Composable
fun LyricsSourceSheet(
    lyricsData: NowPlayingScreenData.LyricsData?,
    sharedViewModel: SharedViewModel,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nowPlaying by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val song = nowPlaying?.songEntity ?: return
    val setting by sharedViewModel.getLyricsProviders().collectAsStateWithLifecycle("")
    val providers = remember(setting) { LyricsSource.order(setting) }
    val scope = rememberCoroutineScope()

    // A title and artist typed by hand, in place of the song's own.
    var query by remember(song.videoId) { mutableStateOf<Pair<String, String>?>(null) }
    // Opened on a song nothing was found for, every provider has already said no: the search is
    // the useful part, so it opens with it.
    var searchOpen by remember(song.videoId) { mutableStateOf(lyricsData == null) }
    var draftTitle by remember(song.videoId) { mutableStateOf(song.title) }
    var draftArtist by remember(song.videoId) { mutableStateOf(song.artistName?.firstOrNull().orEmpty()) }
    val results = remember(song.videoId, query) { mutableStateMapOf<LyricsSource, Answer>() }

    fun check(source: LyricsSource) {
        results[source] = Answer.Loading
        scope.launch {
            results[source] =
                try {
                    Answer.Done(sharedViewModel.previewLyrics(source, query?.first, query?.second))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Answer.Failed
                }
        }
    }

    // Ask every source that's on, once per song and query. The one on screen already answered.
    LaunchedEffect(song.videoId, query) {
        val showing = if (query == null) lyricsData?.lyricsProvider else null
        val saved = showing?.let { sharedViewModel.savedLyrics(song.videoId) }
        LyricsSource.order(sharedViewModel.getLyricsProviders().first()).forEach { (source, on) ->
            when {
                source == showing && saved != null -> results[source] = Answer.Done(saved)
                on -> check(source)
                else -> results[source] = Answer.Idle
            }
        }
    }

    val pinned = lyricsData?.pinned == true
    val offset = lyricsData?.offsetMs ?: 0

    SettingSheet(title = stringResource(Res.string.lyrics_source_title), onDismiss = onDismiss) { colors ->
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.lyrics_source_hint), style = typo().bodySmall, color = colors.subtitle, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(SimpIcons.Settings, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
                }
            }

            val q = query
            if (q != null) {
                Row(
                    Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(SimpIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.lyrics_search_results, if (q.second.isBlank()) q.first else "${q.first} · ${q.second}"),
                        style = typo().bodySmall,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(Res.string.lyrics_search_reset),
                        style = typo().labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(CircleShape).clickable { query = null }.padding(4.dp),
                    )
                }
            } else {
                SourceRow(
                    selected = !pinned,
                    onClick = { if (pinned) sharedViewModel.chooseLyrics(null) },
                    leading = {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (pinned) colors.disabled.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                SimpIcons.TipsAndUpdates,
                                contentDescription = null,
                                tint = if (pinned) colors.subtitle else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.lyrics_automatic), style = typo().labelMedium, color = colors.content)
                        Text(
                            if (!pinned && lyricsData?.lyricsProvider != null) {
                                stringResource(Res.string.lyrics_automatic_showing, lyricsData.lyricsProvider.displayName)
                            } else {
                                stringResource(Res.string.lyrics_automatic_hint)
                            },
                            style = typo().bodySmall,
                            color = colors.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!pinned) Icon(SimpIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.disabled.copy(alpha = 0.4f))
            }

            providers.forEach { (source, on) ->
                val answer = results[source] ?: Answer.Idle
                val hit = (answer as? Answer.Done)?.lyrics
                val showing = lyricsData?.lyricsProvider == source
                val dead = answer is Answer.Done && hit == null
                SourceRow(
                    selected = showing && pinned,
                    onClick = {
                        when {
                            answer is Answer.Idle || answer is Answer.Failed -> check(source)
                            hit != null && !(showing && pinned && query == null) -> sharedViewModel.chooseLyrics(hit)
                        }
                    },
                    enabled = !dead && answer !is Answer.Loading,
                    leading = {
                        // The dot is what's on screen, whether Automatic put it there or you did.
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .border(
                                        2.dp,
                                        when {
                                            showing -> MaterialTheme.colorScheme.primary
                                            dead -> colors.disabled.copy(alpha = 0.4f)
                                            else -> colors.subtitle.copy(alpha = 0.6f)
                                        },
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showing) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    },
                ) {
                    Text(
                        source.displayName,
                        style = typo().labelMedium.copy(fontWeight = if (showing) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (dead || !on) colors.subtitle else colors.content,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        answer is Answer.Loading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.subtitle)
                        answer is Answer.Idle -> Status(stringResource(Res.string.lyrics_off), stringResource(Res.string.lyrics_check), colors)
                        answer is Answer.Failed -> Status(stringResource(Res.string.lyrics_unreachable), stringResource(Res.string.lyrics_retry), colors)
                        hit != null -> LyricsKindBadge(LyricsKind.of(hit.syncType))
                        else -> Text(stringResource(Res.string.lyrics_not_found), style = typo().bodySmall, color = colors.subtitle.copy(alpha = 0.7f))
                    }
                }
            }

            if (lyricsData != null && lyricsData.lyrics.syncType != "UNSYNCED") {
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.disabled.copy(alpha = 0.4f))
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(SimpIcons.AvTimer, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.lyrics_timing), style = typo().labelMedium, color = colors.content)
                        Text(stringResource(Res.string.lyrics_timing_hint), style = typo().bodySmall, color = colors.subtitle)
                    }
                    Spacer(Modifier.width(8.dp))
                    Stepper(
                        label = stringResource(Res.string.lyrics_timing_value, formatSeconds(offset)),
                        highlighted = offset != 0,
                        canDown = offset > -MAX_OFFSET_MS,
                        canUp = offset < MAX_OFFSET_MS,
                        onDown = { sharedViewModel.setSongLyricsOffset(offset - STEP_MS) },
                        onUp = { sharedViewModel.setSongLyricsOffset(offset + STEP_MS) },
                    )
                    // Held in place when there's nothing to reset, so the stepper doesn't jump.
                    IconButton(onClick = { sharedViewModel.setSongLyricsOffset(0) }, enabled = offset != 0, modifier = Modifier.alpha(if (offset != 0) 1f else 0f)) {
                        Icon(SimpIcons.Undo, contentDescription = stringResource(Res.string.lyrics_timing_reset), tint = colors.subtitle, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.disabled.copy(alpha = 0.4f))
            if (!searchOpen) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { searchOpen = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(SimpIcons.Search, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.lyrics_search_open), style = typo().bodySmall, color = colors.subtitle, modifier = Modifier.weight(1f))
                    Icon(SimpIcons.ArrowForwardIos, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(12.dp))
                }
            }
            AnimatedVisibility(searchOpen) {
                fun search() {
                    val title = draftTitle.trim()
                    if (title.isEmpty()) return
                    query = title to draftArtist.trim()
                    searchOpen = false
                }
                Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text(stringResource(Res.string.lyrics_search_title)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = draftArtist,
                            onValueChange = { draftArtist = it },
                            label = { Text(stringResource(Res.string.lyrics_search_artist)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { search() }),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(onClick = { search() }, enabled = draftTitle.isNotBlank(), modifier = Modifier.padding(top = 6.dp)) {
                            Text(stringResource(Res.string.lyrics_search_go))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SourceRow(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leading: @Composable () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(10.dp))
        content()
    }
}

@Composable
private fun Status(
    state: String,
    action: String,
    colors: SurfaceDarkColors,
) {
    Row {
        Text("$state · ", style = typo().bodySmall, color = colors.subtitle)
        Text(action, style = typo().bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
    }
}

/** `− value +` in a pill; the ends go dead rather than wrapping. Shared with the text size control. */
@Composable
internal fun Stepper(
    label: String,
    highlighted: Boolean,
    canDown: Boolean,
    canUp: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.04f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDown, enabled = canDown, modifier = Modifier.size(34.dp)) {
            Icon(SimpIcons.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(
            label,
            style = typo().labelSmall,
            color = if (highlighted) scheme.primary else scheme.onSurface,
            modifier = Modifier.width(58.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(34.dp)) {
            Icon(SimpIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/** "+0.25", "-1.50", "0.00": signed, two decimals, like desktop's stepper. */
internal fun formatSeconds(ms: Int): String {
    val sign = if (ms > 0) "+" else if (ms < 0) "-" else ""
    val a = abs(ms)
    return "$sign${a / 1000}.${(a % 1000 / 10).toString().padStart(2, '0')}"
}
