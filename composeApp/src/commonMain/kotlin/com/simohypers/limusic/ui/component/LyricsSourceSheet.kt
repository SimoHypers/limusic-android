package com.simohypers.limusic.ui.component

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.simohypers.limusic.domain.data.model.lyrics.LimusicException
import com.simohypers.limusic.domain.data.model.lyrics.LimusicRating
import com.simohypers.limusic.domain.data.model.lyrics.LIMUSIC_REPORT_REASONS
import com.simohypers.limusic.domain.data.model.lyrics.LIMUSIC_UNAVAILABLE
import com.simohypers.limusic.domain.data.model.lyrics.limusicProfileUrl
import com.simohypers.limusic.ui.icon.OutlinedFlag
import com.simohypers.limusic.ui.icon.ThumbDown
import com.simohypers.limusic.ui.icon.ThumbDownOutlined
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simohypers.limusic.domain.data.entities.LyricsEntity
import com.simohypers.limusic.domain.data.model.lyrics.LyricsSource
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.ArrowForwardIos
import com.simohypers.limusic.ui.icon.AvTimer
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.Remove
import com.simohypers.limusic.ui.icon.Search
import com.simohypers.limusic.ui.icon.Settings
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.OpenInNew
import com.simohypers.limusic.ui.icon.TipsAndUpdates
import com.simohypers.limusic.ui.icon.Undo
import com.simohypers.limusic.ui.screen.home.LyricsKind
import com.simohypers.limusic.ui.screen.home.LyricsKindBadge
import com.simohypers.limusic.ui.theme.typo
import com.simohypers.limusic.viewModel.NowPlayingScreenData
import com.simohypers.limusic.viewModel.SharedViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.lyrics_automatic
import limusic.composeapp.generated.resources.lyrics_automatic_hint
import limusic.composeapp.generated.resources.lyrics_automatic_showing
import limusic.composeapp.generated.resources.lyrics_check
import limusic.composeapp.generated.resources.lyrics_not_found
import limusic.composeapp.generated.resources.lyrics_off
import limusic.composeapp.generated.resources.lyrics_retry
import limusic.composeapp.generated.resources.lyrics_search_artist
import limusic.composeapp.generated.resources.lyrics_search_go
import limusic.composeapp.generated.resources.lyrics_search_open
import limusic.composeapp.generated.resources.lyrics_search_reset
import limusic.composeapp.generated.resources.lyrics_search_results
import limusic.composeapp.generated.resources.lyrics_search_title
import limusic.composeapp.generated.resources.lyrics_source_hint
import limusic.composeapp.generated.resources.lyrics_source_title
import limusic.composeapp.generated.resources.lyrics_timing
import limusic.composeapp.generated.resources.lyrics_timing_hint
import limusic.composeapp.generated.resources.lyrics_timing_reset
import limusic.composeapp.generated.resources.lyrics_timing_value
import limusic.composeapp.generated.resources.lyrics_unison_rate
import limusic.composeapp.generated.resources.lyrics_unreachable
import limusic.composeapp.generated.resources.cancel
import limusic.composeapp.generated.resources.lyrics_feedback_banned
import limusic.composeapp.generated.resources.lyrics_feedback_failed
import limusic.composeapp.generated.resources.lyrics_feedback_gone
import limusic.composeapp.generated.resources.lyrics_feedback_own
import limusic.composeapp.generated.resources.lyrics_feedback_rate_limited
import limusic.composeapp.generated.resources.lyrics_from
import limusic.composeapp.generated.resources.lyrics_names_and
import limusic.composeapp.generated.resources.lyrics_rate
import limusic.composeapp.generated.resources.lyrics_report
import limusic.composeapp.generated.resources.lyrics_report_abuse
import limusic.composeapp.generated.resources.lyrics_report_note
import limusic.composeapp.generated.resources.lyrics_report_send
import limusic.composeapp.generated.resources.lyrics_report_text
import limusic.composeapp.generated.resources.lyrics_report_timing
import limusic.composeapp.generated.resources.lyrics_report_title
import limusic.composeapp.generated.resources.lyrics_report_wrong_song
import limusic.composeapp.generated.resources.lyrics_reported
import limusic.composeapp.generated.resources.lyrics_synced_by
import limusic.composeapp.generated.resources.lyrics_vote_down
import limusic.composeapp.generated.resources.lyrics_vote_up
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
        val scroll = rememberScrollState()
        Column(Modifier.heightIn(max = 560.dp).scrollEdgeFade(scroll).verticalScroll(scroll)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.lyrics_source_hint), style = typo().bodySmall, color = colors.subtitle, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(LimusicIcons.Settings, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
                }
            }

            val q = query
            if (q != null) {
                Row(
                    Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(rounded(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LimusicIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                                .clip(rounded(10.dp))
                                .background(if (pinned) colors.disabled.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                LimusicIcons.TipsAndUpdates,
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
                                stringResource(Res.string.lyrics_automatic_showing, lyricsData.sourceName().orEmpty())
                            } else {
                                stringResource(Res.string.lyrics_automatic_hint)
                            },
                            style = typo().bodySmall,
                            color = colors.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!pinned) Icon(LimusicIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
                    below =
                        if (source == LyricsSource.LIMUSIC && showing && query == null) {
                            { LimusicFeedback(song.videoId, lyricsData, sharedViewModel, colors) }
                        } else {
                            null
                        },
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
                        answer is Answer.Loading -> LimusicLoader(Modifier.size(18.dp), color = colors.subtitle)
                        answer is Answer.Idle -> Status(stringResource(Res.string.lyrics_off), stringResource(Res.string.lyrics_check), colors)
                        answer is Answer.Failed -> Status(stringResource(Res.string.lyrics_unreachable), stringResource(Res.string.lyrics_retry), colors)
                        hit != null -> LyricsKindBadge(LyricsKind.of(hit.syncType))
                        else -> Text(stringResource(Res.string.lyrics_not_found), style = typo().bodySmall, color = colors.subtitle.copy(alpha = 0.7f))
                    }
                }
            }

            if (lyricsData?.lyricsProvider == LyricsSource.UNISON) {
                // Unison votes are signed with a Better Lyrics key, so they happen on its site.
                val uriHandler = LocalUriHandler.current
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.disabled.copy(alpha = 0.4f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(rounded(12.dp))
                        .clickable { uriHandler.openUri("https://unison.betterlyrics.org/song/${song.videoId}") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LimusicIcons.OpenInNew, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.lyrics_unison_rate), style = typo().bodySmall, color = colors.subtitle)
                }
            }

            if (lyricsData != null && lyricsData.lyrics.syncType != "UNSYNCED") {
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.disabled.copy(alpha = 0.4f))
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(LimusicIcons.AvTimer, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(20.dp))
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
                        Icon(LimusicIcons.Undo, contentDescription = stringResource(Res.string.lyrics_timing_reset), tint = colors.subtitle, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.disabled.copy(alpha = 0.4f))
            if (!searchOpen) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(rounded(12.dp))
                        .clickable { searchOpen = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LimusicIcons.Search, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.lyrics_search_open), style = typo().bodySmall, color = colors.subtitle, modifier = Modifier.weight(1f))
                    Icon(LimusicIcons.ArrowForwardIos, contentDescription = null, tint = colors.subtitle, modifier = Modifier.size(12.dp))
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

/**
 * Votes and reports on the lyric Limusic Lyrics has live, while it is the one on screen, and who
 * synced it. No sign-in: the first vote or report makes an anonymous account, silently. Votes go to
 * the live revision, so an older one still cached here offers none until the daily look updates it.
 */
@Composable
private fun LimusicFeedback(
    videoId: String,
    lyricsData: NowPlayingScreenData.LyricsData,
    sharedViewModel: SharedViewModel,
    colors: SurfaceDarkColors,
) {
    val scope = rememberCoroutineScope()
    val shown = lyricsData.revision
    var rating by remember(videoId, shown) { mutableStateOf<LimusicRating?>(null) }
    var loading by remember(videoId, shown) { mutableStateOf(true) }
    var busy by remember(videoId, shown) { mutableStateOf(false) }
    // Why the last vote or report didn't go through, as the service's code.
    var refusal by remember(videoId, shown) { mutableStateOf<String?>(null) }
    var reportOpen by remember(videoId, shown) { mutableStateOf(false) }
    var reason by remember(videoId, shown) { mutableStateOf<String?>(null) }
    var note by remember(videoId, shown) { mutableStateOf("") }
    // ponytail: lasts as long as the sheet; the service keeps one report per account and revision.
    var reported by remember(videoId, shown) { mutableStateOf(false) }

    LaunchedEffect(videoId, shown) {
        rating =
            try {
                sharedViewModel.limusicRating(videoId)?.takeIf { shown == null || it.revision == shown }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        loading = false
    }

    fun codeOf(e: Exception) = (e as? LimusicException)?.code ?: LIMUSIC_UNAVAILABLE

    fun vote(clicked: Int) {
        val before = rating ?: return
        if (busy) return
        val value = before.next(clicked)
        rating = before.withVote(value)
        busy = true
        refusal = null
        scope.launch {
            try {
                rating = sharedViewModel.limusicVote(videoId, value, before)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                rating = before
                refusal = codeOf(e)
            } finally {
                busy = false
            }
        }
    }

    fun send() {
        val r = reason ?: return
        reported = true
        reportOpen = false
        refusal = null
        scope.launch {
            try {
                sharedViewModel.limusicReport(videoId, r, note)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reported = false
                reportOpen = true
                refusal = codeOf(e)
            }
        }
    }

    val scheme = MaterialTheme.colorScheme
    val rated = loading || rating != null
    if (!rated && lyricsData.authors.isEmpty()) return
    // Lined up under the source's name: the Limusic row's second line, at the top of the sheet.
    Column(Modifier.fillMaxWidth().padding(start = 50.dp, end = 2.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (lyricsData.authors.isNotEmpty()) syncedByLinks(lyricsData.authors) else AnnotatedString(stringResource(Res.string.lyrics_rate)),
                style = typo().bodySmall,
                color = colors.subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (rated) {
                Spacer(Modifier.width(8.dp))
                val r = rating
                if (r == null) {
                    Box(Modifier.size(width = 96.dp, height = 32.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.06f)))
                } else {
                    Row(
                        Modifier.clip(CircleShape).border(1.dp, scheme.outlineVariant, CircleShape).background(scheme.onSurface.copy(alpha = 0.04f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Thumb(r.up, mine = r.myVote == 1, up = true, label = stringResource(Res.string.lyrics_vote_up)) { vote(1) }
                        Thumb(r.down, mine = r.myVote == -1, up = false, label = stringResource(Res.string.lyrics_vote_down)) { vote(-1) }
                    }
                    if (!reported) {
                        IconButton(onClick = { reportOpen = !reportOpen }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                LimusicIcons.OutlinedFlag,
                                contentDescription = stringResource(Res.string.lyrics_report),
                                tint = if (reportOpen) scheme.primary else colors.subtitle,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        refusal?.let {
            Text(stringResource(refusalText(it)), style = typo().bodySmall, color = scheme.error, modifier = Modifier.padding(top = 4.dp))
        }
        if (rating != null) {
            when {
                reportOpen -> ReportForm(reason, note, colors, onReason = { reason = it }, onNote = { note = it }, onCancel = { reportOpen = false }, onSend = ::send)
                reported ->
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(LimusicIcons.Check, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.lyrics_reported), style = typo().bodySmall, color = colors.subtitle)
                    }
            }
        }
    }
}

/** One half of the vote pill: the thumb and its count, filled and in the accent when it is mine. */
@Composable
private fun Thumb(
    count: Int,
    mine: Boolean,
    up: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (mine) scheme.primary else scheme.onSurfaceVariant
    Row(
        Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (mine) scheme.primary.copy(alpha = 0.14f) else Color.Transparent)
            .toggleable(value = mine, onValueChange = { onClick() })
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Material's thumbs are one shape turned over, so the up one is the down one at 180°.
        Icon(
            if (mine) LimusicIcons.ThumbDown else LimusicIcons.ThumbDownOutlined,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp).then(if (up) Modifier.rotate(180f) else Modifier),
        )
        Spacer(Modifier.width(6.dp))
        Text(count.toString(), style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold), color = tint)
    }
}

@Composable
private fun ReportForm(
    reason: String?,
    note: String,
    colors: SurfaceDarkColors,
    onReason: (String) -> Unit,
    onNote: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 8.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.lyrics_report_title), style = typo().labelMedium, color = colors.content)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LIMUSIC_REPORT_REASONS.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pair.forEach { id ->
                        val on = reason == id
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(rounded(10.dp))
                                .border(1.dp, if (on) scheme.primary.copy(alpha = 0.6f) else scheme.outlineVariant, rounded(10.dp))
                                .background(if (on) scheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .selectable(selected = on, role = Role.RadioButton, onClick = { onReason(id) })
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(14.dp).border(2.dp, if (on) scheme.primary else colors.subtitle.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (on) Box(Modifier.size(6.dp).clip(CircleShape).background(scheme.primary))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(reportReasonText(id)), style = typo().bodySmall, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { onNote(it.take(REPORT_NOTE_MAX)) },
            placeholder = { Text(stringResource(Res.string.lyrics_report_note), style = typo().bodySmall) },
            textStyle = typo().bodySmall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (reason != null) onSend() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onSend, enabled = reason != null) {
                Icon(LimusicIcons.OutlinedFlag, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.lyrics_report_send))
            }
        }
    }
}

/** The service takes a note of at most 300 bytes; 300 characters is the field's cap, and the client clips the rest. */
private const val REPORT_NOTE_MAX = 300

private fun reportReasonText(id: String) =
    when (id) {
        "timing" -> Res.string.lyrics_report_timing
        "text" -> Res.string.lyrics_report_text
        "wrong_song" -> Res.string.lyrics_report_wrong_song
        else -> Res.string.lyrics_report_abuse
    }

private fun refusalText(code: String) =
    when (code) {
        "own" -> Res.string.lyrics_feedback_own
        "not_found" -> Res.string.lyrics_feedback_gone
        "rate_limited" -> Res.string.lyrics_feedback_rate_limited
        "banned" -> Res.string.lyrics_feedback_banned
        else -> Res.string.lyrics_feedback_failed
    }

/** "Synced by carla and moss", each name a link to its profile on the website. */
@Composable
private fun syncedByLinks(authors: List<String>): AnnotatedString {
    val credit = stringResource(Res.string.lyrics_synced_by, names(authors))
    val link = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold))
    return remember(credit, authors, link) {
        buildAnnotatedString {
            append(credit)
            // Found in the sentence rather than placed by it, so a translation may order it any way.
            var from = 0
            authors.forEach { handle ->
                val at = credit.indexOf(handle, from).takeIf { it >= 0 } ?: return@forEach
                addLink(LinkAnnotation.Url(limusicProfileUrl(handle), link), at, at + handle.length)
                from = at + handle.length
            }
        }
    }
}

/** "carla", "carla and moss", "a, b and c". */
@Composable
private fun names(list: List<String>): String =
    if (list.size < 2) list.firstOrNull().orEmpty() else stringResource(Res.string.lyrics_names_and, list.dropLast(1).joinToString(", "), list.last())

/** The source as the credit names it: "Limusic (Unison)" for a lyric Limusic mirrors from Unison. */
internal fun NowPlayingScreenData.LyricsData.sourceName(): String? {
    val p = lyricsProvider ?: return null
    // No English connective in it: the credit wraps it in "Lyrics from %1$s".
    val mirrored = LyricsSource.fromId(origin)?.takeIf { p == LyricsSource.LIMUSIC && it != LyricsSource.LIMUSIC }
    return if (mirrored != null) "${p.displayName} (${mirrored.displayName})" else p.displayName
}

/** The footer's credit: "Synced by carla and moss" for a community lyric, else "Lyrics from <source>". */
@Composable
internal fun NowPlayingScreenData.LyricsData.credit(): String? =
    if (lyricsProvider == LyricsSource.LIMUSIC && authors.isNotEmpty()) {
        stringResource(Res.string.lyrics_synced_by, names(authors))
    } else {
        sourceName()?.let { stringResource(Res.string.lyrics_from, it) }
    }

/** The credit where the caption is a bare source name (Apple Music, the M3 lyrics card). */
@Composable
internal fun NowPlayingScreenData.LyricsData.creditName(): String =
    if (lyricsProvider == LyricsSource.LIMUSIC && authors.isNotEmpty()) stringResource(Res.string.lyrics_synced_by, names(authors)) else sourceName().orEmpty()

/**
 * Fades the edge the content goes on past, so a list the sheet cuts off reads as one that scrolls:
 * the votes and Timing below nine sources were easy to miss.
 */
private fun Modifier.scrollEdgeFade(
    state: ScrollState,
    edge: Dp = 48.dp,
) = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
    drawContent()
    val px = edge.toPx()
    if (state.canScrollForward) {
        drawRect(Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - px, endY = size.height), blendMode = BlendMode.DstIn)
    }
    if (state.canScrollBackward) {
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = px), blendMode = BlendMode.DstIn)
    }
}

@Composable
private fun SourceRow(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    // A second line that belongs to the row (Limusic's votes), inside its box but not its tap.
    below: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(rounded(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else Color.Transparent),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(10.dp))
            content()
        }
        below?.invoke()
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
            Icon(LimusicIcons.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(
            label,
            style = typo().labelSmall,
            color = if (highlighted) scheme.primary else scheme.onSurface,
            modifier = Modifier.width(58.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(34.dp)) {
            Icon(LimusicIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/** "+0.25", "-1.50", "0.00": signed, two decimals, like desktop's stepper. */
internal fun formatSeconds(ms: Int): String {
    val sign = if (ms > 0) "+" else if (ms < 0) "-" else ""
    val a = abs(ms)
    return "$sign${a / 1000}.${(a % 1000 / 10).toString().padStart(2, '0')}"
}
